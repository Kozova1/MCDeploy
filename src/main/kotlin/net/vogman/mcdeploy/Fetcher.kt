package net.vogman.mcdeploy

interface Fetcher {
    suspend fun fetch(config: Config): Result<ByteArray, Error>
}