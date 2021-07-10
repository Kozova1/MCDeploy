package net.vogman.mcdeploy

import arrow.core.Either

interface Fetcher {
    suspend fun fetch(config: Config): Either<Error, ByteArray>
}