package net.vogman.mcdeploy

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.fp.getOrElse
import com.sksamuel.hoplite.fp.onInvalid
import com.sksamuel.hoplite.fp.onValid
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.File
import java.net.URL
import java.nio.file.Path

data class EnvironmentConfig(
    val JavaArgs: List<String>,
    val PreLaunchCommands: List<String>,
    val PostExitCommands: List<String>
)

sealed class ServerJarFetcher : Fetcher {
    enum class LauncherManifestFetcher { LauncherManifest }
    data class LauncherManifest(
        val Fetcher: LauncherManifestFetcher,
        val Version: String,
        val LauncherManifestURL: URL
    ) : ServerJarFetcher() {
        override suspend fun fetch(config: Config): Result<ByteArray, Error> = fetchImpl(config)
    }

    enum class DownloadURLFetcher { DownloadURL }
    data class DownloadURL(val Fetcher: DownloadURLFetcher, val ServerJarURL: URL, val Sha1Sum: String) :
        ServerJarFetcher() {
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

data class ExternalFile(val URL: URL, val FileName: String, val Sha1Sum: String) {
    suspend fun fetch(client: HttpClient): Result<ByteArray, Error> {
        return try {
            val responseData: HttpResponse = client.get(URL)
            Result.Ok(responseData.receive())
        } catch (e: ClientRequestException) {
            logErr("Download failed: ${e.response.status.value} (${e.response.status.description})")
            Result.Err(Error.Server)
        }
    }
}

data class ExtraFilesConf(val TargetDir: Path?, val Files: List<ExternalFile>)

data class Config(
    val Server: ServerConfig,
    val Environment: EnvironmentConfig,
    val Datapacks: ExtraFilesConf?,
    val Plugins: ExtraFilesConf?,
    val Properties: Map<String, String>
) {
    companion object {
        fun loadConfig(configFile: File): Result<Config, Error> {
            val configMaybeInvalid = ConfigLoader.Builder()
                .addSource(PropertySource.file(configFile))
                .addSource(PropertySource.resource("/default-config.toml"))
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
            return Result.Ok(configMaybeInvalid.getOrElse {
                throw IllegalStateException("Got invalid after verification of validness")
            })
        }
    }

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