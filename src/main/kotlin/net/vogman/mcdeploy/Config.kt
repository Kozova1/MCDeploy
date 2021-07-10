package net.vogman.mcdeploy

import arrow.core.Either
import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.fp.fold
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.tongfei.progressbar.ProgressBar
import java.io.File
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes

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
        override suspend fun fetch(config: Config): Either<Error, ByteArray> = fetchImpl(config)
    }

    enum class DownloadURLFetcher { DownloadURL }
    data class DownloadURL(val Fetcher: DownloadURLFetcher, val ServerJarURL: URL, val Sha1Sum: String) :
        ServerJarFetcher() {
        override suspend fun fetch(config: Config): Either<Error, ByteArray> = fetchImpl(config)
    }

    enum class CopyFileFetcher { CopyFile }
    data class CopyFile(val Fetcher: CopyFileFetcher, val CopyFrom: File) : ServerJarFetcher() {
        override suspend fun fetch(config: Config): Either<Error, ByteArray> = fetchImpl(config)
    }
}

data class ServerConfig(
    val JarSource: ServerJarFetcher,
    val AgreeToEULA: Boolean,
)

data class ExternalFile(val URL: URL, val FileName: String, val Sha1Sum: String) {
    suspend fun fetchWrite(client: HttpClient, targetPath: Path, progressBar: ProgressBar): Either<Error, Unit> {
        val file = targetPath.resolve(FileName)
        when (val cnfRes = Either.catch { file.createFile() }.mapLeft {
            Error.FilesystemError(it.localizedMessage)
        }) {
            is Either.Left -> return cnfRes.map { }
        }

        val bytes: ByteArray = when (val ret: Either<Error, ByteArray> = Either.catch<ByteArray> {
            HttpClient().use { client ->
                val res: HttpResponse = client.get(URL) {
                    onDownload { bytesSentTotal, contentLength ->
                        progressBar.maxHint(contentLength / 1024)
                        progressBar.stepTo(bytesSentTotal / 1024)
                    }
                }
                res.receive()
            }
        }.mapLeft {
            val exnStatus = (it as ClientRequestException).response.status
            Error.RequestFailed("${exnStatus.value} (${exnStatus.description})")
        }) {
            is Either.Left -> return ret.map { }
            is Either.Right -> ret.value
        }
        val hashActual = sha1sum(bytes)
        if (hashActual != Sha1Sum) {
            return Either.Left(Error.HashMismatch(file, Sha1Sum, hashActual))
        }

        Either.catch { file.writeBytes(bytes) }.mapLeft {
            Error.FilesystemError(it.localizedMessage)
        }

        return Either.Right(Unit)
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
        fun loadConfig(configFile: File): Either<Error, Config> {
            fun ifValid(conf: Config): Either<Error, Config> = Either.Right(conf)
            fun ifInvalid(err: ConfigFailure): Either<Error, Config> = Either.Left(Error.ConfigParse(err))
            return ConfigLoader.Builder()
                .addSource(PropertySource.file(configFile))
                .addSource(PropertySource.resource("/default-config.toml"))
                .build()
                .loadConfig<Config>()
                .fold(::ifInvalid, ::ifValid)
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