package net.vogman.mcdeploy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

suspend fun ServerJarFetcher.DownloadURL.fetchImpl(config: Config): Result<ByteArray, Error> =
    try {
        HttpClient().use { client ->
            assert(config.Server.JarSource is ServerJarFetcher.DownloadURL)
            println("Downloading server.jar from $ServerJarURL")
            val response: HttpResponse = client.get(ServerJarURL)
            val serverJar: ByteArray = response.receive()
            return Result.Ok(serverJar)
        }
    } catch (e: ClientRequestException) {
        logErr("Download failed: ${e.response.status.value} (${e.response.status.description})")
        Result.Err(Error.Server)
    }
