package net.vogman.mcdeploy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

suspend fun ServerJarFetcher.LauncherManifest.fetchImpl(config: Config): Result<ByteArray, Error> =
    HttpClient().use { client ->
        assert(config.Server.JarSource is ServerJarFetcher.LauncherManifest)
        println("Downloading launcher manifest...")
        val versionResponse: HttpResponse = client.get(LauncherManifestURL)
        val versions: Versions = Json { ignoreUnknownKeys = true }.decodeFromString(versionResponse.receive())
        val versionUrl = versions.findURI(
            when {
                Version.equals("latest-release", ignoreCase = true) -> versions.latest.release
                Version.equals("latest-snapshot", ignoreCase = true) -> versions.latest.snapshot
                else -> Version
            }
        )

        if (versionUrl == null) {
            logErr("No manifest URL found for version $Version (Are you sure this is the correct version?)")
            return Result.Err(Error.Server)
        }

        println()

        println("Downloading manifest for selected version...")
        val responseManifest: HttpResponse = client.get(versionUrl)
        val manifest: VersionManifest =
            Json { ignoreUnknownKeys = true }.decodeFromString(responseManifest.receive())
        logOk("Received manifest for version $Version")

        if (manifest.downloads.server == null) {
            logErr("No server jar for version $Version")
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
        return if (manifest.downloads.server.sha1 == serverJarHash) {
            logOk("SHA-1 Match! Continuing")
            Result.Ok(serverJar)
        } else {
            logErr("SHA-1 Mismatch! Exiting")
            Result.Err(Error.Hash)
        }
    }