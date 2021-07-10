package net.vogman.mcdeploy

import arrow.core.Either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.io.path.Path

suspend fun ServerJarFetcher.DownloadURL.fetchImpl(config: Config): Either<Error, ByteArray> =
    try {
        HttpClient().use { client ->
            assert(config.Server.JarSource is ServerJarFetcher.DownloadURL)
            println("Downloading server.jar from $ServerJarURL")
            val response: HttpResponse = client.downloadWithProgressBar(ServerJarURL, "server.jar")
            val serverJar: ByteArray = response.receive()

            val actualHash = sha1sum(serverJar)
            return if (actualHash != Sha1Sum) {
                Either.Left(Error.HashMismatch(Path("server.jar"), Sha1Sum, actualHash))
            } else {
                logOk("SHA-1 Matches. Continuing")
                return Either.Right(serverJar)
            }
        }
    } catch (e: ClientRequestException) {
        Either.Left(Error.RequestFailed("${e.response.status.value} (${e.response.status.description})"))
    }
