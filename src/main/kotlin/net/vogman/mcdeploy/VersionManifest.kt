package net.vogman.mcdeploy

import kotlinx.serialization.Serializable


@Serializable
data class LatestVersion(val release: String, val snapshot: String)

@Serializable
data class Version(
    val id: String, // Version
    val url: String,
)

@Serializable
data class Versions(val versions: List<Version>, val latest: LatestVersion) {
    fun findURI(version: String): String? = versions.find {
        it.id == version
    }?.url
}

@Serializable
data class VersionManifest(val downloads: VersionDownloads)

@Serializable
data class VersionDownloads(val server: ServerJarEntry?)

@Serializable
data class ServerJarEntry(val sha1: String, val size: Long, val url: String)
