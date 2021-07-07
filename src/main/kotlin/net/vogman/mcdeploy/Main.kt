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
import java.lang.StringBuilder
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.system.exitProcess

fun writeEula() = File("./eula.txt").overwrite("eula = true\n", Charsets.UTF_8)

fun File.overwrite(content: String, charset: Charset) {
    if (exists()) {
        delete()
    }
    writeText(content, charset)
}

fun File.overwrite(content: ByteArray) {
    if (exists()) {
        delete()
    }
    writeBytes(content)
}

fun logErr(msg: String) {
    val colorReset = "\u001B[0m"
    val colorRed = "\u001B[31m"
    println("[${colorRed}X${colorReset}] $msg")
}

fun logOk(msg: String) {
    val colorReset = "\u001B[0m"
    val colorGreen = "\u001B[32m"
    println("[${colorGreen}✓${colorReset}] $msg")
}

fun hash(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(bytes)
    val sb = StringBuilder()
    for (byte in digest) {
        sb.append("%02x".format(byte))
    }
    return sb.toString()
}

suspend fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        if (args[0] == "-h" || args[0] == "--help" || args[0] == "-help" || args[0] == "help") {
            println(HELP)
            return
        } else if (args.size >= 2 && args[0] == "new") {
            runCatching {
                Path(args[1]).createDirectories()
                File(args[1] + File.separator + "mcdeploy.toml").createNewFile()
            }
            logOk("Server template created!")
            return
        } else if (args[0] != "deploy") {
            logErr("Unknown command '${args[0]}'")
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
    if (serverJarHash == manifest.downloads.server.sha1) {
        logOk("SHA-1 Match! Continuing")
    } else {
        logErr("SHA-1 Mismatch! Exiting")
        exitProcess(2)
    }

    // make sure directory exists
    runCatching {
        Path("./datapacks").createDirectories()
    }
    println("Starting to download datapacks...")
    config.Datapacks?.forEach { datapack ->
        println("Starting download from ${datapack.URL}")
        val bytes = datapack.fetch(client)
        logOk("Downloaded!")
        val hashed = hash(bytes)
        if (datapack.Sha1Sum == hashed) {
            logOk("SHA-1 Match! ${datapack.Sha1Sum}")
        } else {
            logErr("SHA-1 Mismatch! Expected ${datapack.Sha1Sum} but got $hashed")
            exitProcess(2)
        }
        File("datapacks${File.separator}${datapack.FileName}").overwrite(bytes)
    }
    logOk("Downloaded all datapacks")

    File("./server.jar").overwrite(serverJar)
    logOk("Written server.jar")

    writeEula()
    logOk("Written EULA.txt")

    File("./run.sh").overwrite(config.genRunScript(), Charsets.UTF_8)
    logOk("Written run.sh")

    File("./server.properties").overwrite(config.genServerProperties(), Charsets.UTF_8)
    logOk("written server.properties")

    logOk("Done! Please run the server now.")
}