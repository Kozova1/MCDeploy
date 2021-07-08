package net.vogman.mcdeploy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.File
import java.net.URL

data class EnvironmentConfig(
    val JavaArgs: List<String>,
    val PreLaunchCommands: List<String>,
    val PostExitCommands: List<String>
)

sealed class ServerJarFetcher : Fetcher {
    enum class LauncherManifestFetcher { LauncherManifest }
    data class LauncherManifest(val Fetcher: LauncherManifestFetcher, val Version: String, val LauncherManifestURL: URL) : ServerJarFetcher() {
        override suspend fun fetch(config: Config): Result<ByteArray, Error> = fetchImpl(config)
    }
    enum class DownloadURLFetcher { DownloadURL }
    data class DownloadURL(val Fetcher: DownloadURLFetcher, val ServerJarURL: URL, val Sha1Sum: String) : ServerJarFetcher() {
        override suspend fun fetch(config: Config): Result<ByteArray, Error> = fetchImpl(config)
    }

    enum class CopyFileFetcher { CopyFile }
    data class CopyFile(val Fetcher: CopyFileFetcher, val CopyFrom: File) : ServerJarFetcher() {
        override suspend fun fetch(config: Config): Result<ByteArray, Error> = fetchImpl(config)
    }
}

data class ServerConfig(
    val JarSource: ServerJarFetcher,
    val AgreeToEULA: Boolean,
)

data class Datapack(val URL: URL, val FileName: String, val Sha1Sum: String) {
    suspend fun fetch(client: HttpClient): ByteArray {
        val responseData: HttpResponse = client.get(URL)
        return responseData.receive()
    }
}

data class Config(
    val Server: ServerConfig,
    val Environment: EnvironmentConfig,
    val Datapacks: List<Datapack>?,
    val Properties: Map<String, String>
) {
    fun genServerProperties(): String =
        Properties
            .map { "${it.key} = ${it.value}" }
            .joinToString(separator = "\n") + "\n"

    fun genRunScript(): String {
        val javaArgs = Environment.JavaArgs.joinToString(separator = " ")
        val preLaunchCommands = Environment.PreLaunchCommands.joinToString(separator = "\n")
        val postExitCommands = Environment.PostExitCommands.joinToString(separator = "\n")
        return listOf(
            preLaunchCommands,
            "java $javaArgs -jar server.jar nogui",
            postExitCommands,
            "" // To ensure final newline
        ).joinToString(separator = "\n")
    }
}