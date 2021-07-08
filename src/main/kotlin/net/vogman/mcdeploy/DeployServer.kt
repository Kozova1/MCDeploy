package net.vogman.mcdeploy

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.fp.getOrElse
import com.sksamuel.hoplite.fp.onInvalid
import com.sksamuel.hoplite.fp.onValid
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.IllegalStateException
import kotlin.io.path.Path
import kotlin.io.path.createDirectory

object DeployServer : Command {
    override suspend fun run(args: Array<String>): Result {
        if (args.isNotEmpty()) {
            logErr("'deploy' subcommand accepts exactly zero arguments")
            return Result.Err(Error.User)
        }

        val configFile = File("./mcdeploy.toml")
        if (!configFile.exists()) {
            logErr("Config file ./mcdeploy.toml does not exist. Please create it.")
            return Result.Err(Error.User)
        }

        val configMaybeInvalid = ConfigLoader.Builder()
            .addSource(PropertySource.file(configFile))
            .addSource(PropertySource.string(DEFAULT_CONFIG, "toml"))
            .build()
            .loadConfig<Config>()
        if (configMaybeInvalid.isInvalid()) {
            configMaybeInvalid.onInvalid {
                logErr(it.description())
            }
            configMaybeInvalid.onValid {
                throw IllegalStateException("Got valid when isInvalid() true")
            }
            return Result.Err(Error.User)
        }
        val config = configMaybeInvalid.getOrElse {
            throw IllegalStateException("Got invalid after verification of validness")
        }

        if (!config.Server.AgreeToEULA) {
            logErr("To host a Minecraft server, you must first agree to the EULA: https://account.mojang.com/documents/minecraft_eula")
            println("\tWhen you have read the EULA and agreed to it, please add the following line to mcdeploy.toml under the [Server] section")
            println("AgreeToEULA = true")
            return Result.Err(Error.User)
        }

        val client = HttpClient()

        println("Downloading launcher manifest...")
        val versionResponse: HttpResponse = client.get(config.Server.JsonManifestUrl)
        val versions: Versions = Json { ignoreUnknownKeys = true }.decodeFromString(versionResponse.receive())
        val versionUrl = versions.findURI(
            when {
                config.Server.Version.equals("latest-release", ignoreCase = true) -> versions.latest.release
                config.Server.Version.equals("latest-snapshot", ignoreCase = true) -> versions.latest.snapshot
                else -> config.Server.Version
            }
        )

        if (versionUrl == null) {
            logErr("No manifest URL found for version ${config.Server.Version} (Are you sure this is the correct version?)")
            return Result.Err(Error.Server)
        }

        println()

        println("Downloading manifest for selected version...")
        val responseManifest: HttpResponse = client.get(versionUrl)
        val manifest: VersionManifest = Json { ignoreUnknownKeys = true }.decodeFromString(responseManifest.receive())
        logOk("Received manifest for version ${config.Server.Version}")

        if (manifest.downloads.server == null) {
            logErr("No server jar for version ${config.Server.Version}")
            return Result.Err(Error.Server)
        }

        // Download server.jar for the selected version
        println("Downloading server.jar...")
        val serverResponse: HttpResponse = client.get(manifest.downloads.server.url)
        val serverJar: ByteArray = serverResponse.receive()
        logOk("Downloaded server.jar")
        println("Verifying server.jar...")

        val serverJarHash = sha1sum(serverJar)
        println("Downloaded: $serverJarHash")
        println("Manifest:   ${manifest.downloads.server.sha1}")
        if (manifest.downloads.server.sha1 == serverJarHash) {
            logOk("SHA-1 Match! Continuing")
        } else {
            logErr("SHA-1 Mismatch! Exiting")
            return Result.Err(Error.Hash)
        }

        if (config.Datapacks != null) {
            val numDatapacks = config.Datapacks.size
            // make sure directory exists (but do NOT overwrite)
            runCatching {
                Path("world").createDirectory()
                Path("world", "datapacks").createDirectory()
            }
            println("Starting datapack downloads ($numDatapacks)...")
            println()
            config.Datapacks.forEachIndexed { idx, datapack ->
                println("[${idx + 1}/$numDatapacks] Starting download from ${datapack.URL}")
                val bytes = datapack.fetch(client)
                logOk("Downloaded ${datapack.FileName}")

                println("Verifying ${datapack.FileName}")
                val hashed = sha1sum(bytes)
                println("Downloaded: $hashed")
                println("Configured: ${datapack.Sha1Sum}")
                if (datapack.Sha1Sum == hashed) {
                    logOk("SHA-1 Match! Continuing")
                } else {
                    logErr("SHA-1 Mismatch! Exiting")
                    return Result.Err(Error.Hash)
                }

                logOk("SHA-1 Match! Continuing")
                Path("world", "datapacks", datapack.FileName).toFile().writeNewBytes(bytes)
                println("[${idx + 1}/$numDatapacks] Done.")
                println()
            }
            logOk("Downloaded all datapacks")
        }

        File("./server.jar").writeNewBytes(serverJar)
        logOk("Written server.jar")

        writeEula()
        logOk("Written EULA.txt")

        File("./run.sh").writeNewText(config.genRunScript(), Charsets.UTF_8)
        logOk("Written run.sh")

        File("./server.properties").writeNewText(config.genServerProperties(), Charsets.UTF_8)
        logOk("written server.properties")

        logOk("Done! Please run the server now.")
        return Result.Ok
    }
}