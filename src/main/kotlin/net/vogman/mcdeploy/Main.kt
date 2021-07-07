package net.vogman.mcdeploy

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.fp.getOrElse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.system.exitProcess

suspend fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        if (args[0] == "-h" || args[0] == "--help" || args[0] == "-help" || args[0] == "help") {
            println(HELP)
            return
        } else if (args.size >= 2 && args[0] == "new") {
            if (Path(args[1]).exists()) {
                logErr("Directory ${args[1]} already exists")
                exitProcess(1)
            }
            runCatching {
                Path(args[1]).createDirectory()
                Path(args[1], "mcdeploy.toml").createFile()
            }
            logOk("Server template created!")
            return
        } else if (args[0] != "deploy") {
            logErr("Unknown command '${args[0]}'. Run 'MCDeploy help' to learn how to use this tool")
            exitProcess(1)
        }
    }

    val configFile = File("./mcdeploy.toml")
    if (!configFile.exists()) {
        logErr("Config file ./mcdeploy.toml does not exist. Please create it.")
        exitProcess(1)
    }

    val config = ConfigLoader.Builder()
        .addSource(PropertySource.file(configFile))
        .addSource(PropertySource.string(DEFAULT_CONFIG, "toml"))
        .build()
        .loadConfig<Config>()
        .getOrElse {
            println(it.description())
            exitProcess(1)
        }

    if (!config.Server.AgreeToEULA) {
        logErr("To host a Minecraft server, you must first agree to the EULA: https://account.mojang.com/documents/minecraft_eula")
        println("\tWhen you have read the EULA and agreed to it, please add the following line to mcdeploy.toml under the [Server] section")
        println("AgreeToEULA = true")
        exitProcess(1)
    }

    val client = HttpClient()
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
        logErr("No such version exists!")
        exitProcess(1)
    }

    println("")

    val responseManifest: HttpResponse = client.get(versionUrl)
    val manifest: VersionManifest = Json { ignoreUnknownKeys = true }.decodeFromString(responseManifest.receive())
    logOk("Received manifest for version ${config.Server.Version}")

    if (manifest.downloads.server == null) {
        logErr("No server jar for version ${config.Server.Version}")
        exitProcess(1)
    }

    // Download server.jar for the selected version
    println("Downloading server.jar...")
    val serverResponse: HttpResponse = client.get(manifest.downloads.server.url)
    val serverJar: ByteArray = serverResponse.receive()
    logOk("Downloaded server.jar")
    println("Verifying server.jar...")

    val serverJarHash = hash(serverJar)
    println("Downloaded: $serverJarHash")
    println("Manifest:   ${manifest.downloads.server.sha1}")
    assertHashesMatch(manifest.downloads.server.sha1, serverJarHash)


    if (config.Datapacks != null) {
        val numDatapacks = config.Datapacks.size
        // make sure directory exists
        runCatching {
            Path("world", "datapacks").createDirectories()
        }
        println("Starting datapack downloads ($numDatapacks)...")
        println()
        config.Datapacks.forEachIndexed { idx, datapack ->
            println("[${idx + 1}/$numDatapacks] Starting download from ${datapack.URL}")
            val bytes = datapack.fetch(client)
            logOk("Downloaded ${datapack.FileName}")
            println("Verifying ${datapack.FileName}")
            val hashed = hash(bytes)
            println("Downloaded: $hashed")
            println("Configured: ${datapack.Sha1Sum}")
            assertHashesMatch(datapack.Sha1Sum, hashed)
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
}