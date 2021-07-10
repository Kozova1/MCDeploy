package net.vogman.mcdeploy

import arrow.core.Either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import java.net.URL
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path

suspend fun ServerJarFetcher.LauncherManifest.fetchImpl(config: Config): Either<Error, ByteArray> =
    try {
        HttpClient().use { client ->
            assert(config.Server.JarSource is ServerJarFetcher.LauncherManifest)
            println("Downloading launcher manifest...")
            val versionResponse: HttpResponse = client.downloadWithProgressBar(LauncherManifestURL, "Launcher Manifest")

            val versions: Versions = Json { ignoreUnknownKeys = true }.decodeFromString(versionResponse.receive())
            val versionUrl = versions.findURI(
                when {
                    Version.equals("latest-release", ignoreCase = true) -> versions.latest.release
                    Version.equals("latest-snapshot", ignoreCase = true) -> versions.latest.snapshot
                    else -> Version
                }
            ) ?: return Either.Left(
                Error.ManifestMissingValue(
                    "No manifest URL found for version $Version (Are you sure this is the correct version?)"
                )
            )


            println("Downloading manifest for selected version...")
            val responseManifest: HttpResponse = client.downloadWithProgressBar(versionUrl, "Version Manifest")
            val manifest: VersionManifest =
                Json { ignoreUnknownKeys = true }.decodeFromString(responseManifest.receive())
            logOk("Received manifest for version $Version")

            if (manifest.downloads.server == null) {
                return Either.Left(Error.ManifestMissingValue("No server.jar for version $Version"))
            }

            // Download server.jar for the selected version
            println("Downloading server.jar...")
            val serverResponse: HttpResponse = client.downloadWithProgressBar(manifest.downloads.server.url, "server.jar")
            val serverJar: ByteArray = serverResponse.receive()
            logOk("Downloaded server.jar")
            println("Verifying server.jar...")

            val serverJarHash = sha1sum(serverJar)
            return if (manifest.downloads.server.sha1 == serverJarHash) {
                logOk("SHA-1 Match! Continuing")
                Either.Right(serverJar)
            } else {
                Either.Left(Error.HashMismatch(Path("server.jar"), manifest.downloads.server.sha1, serverJarHash))
            }
        }
    } catch (e: ClientRequestException) {
        Either.Left(Error.RequestFailed("${e.response.status.value} (${e.response.status.description})"))
    }