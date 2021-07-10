package net.vogman.mcdeploy

import arrow.core.Either

sealed interface Command {
    suspend fun run(args: Array<String>): Either<Error, Unit>
}