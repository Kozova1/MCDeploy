package net.vogman.mcdeploy

import arrow.core.Either

object VersionPrinter : Command{
    const val VERSION: String = "v4.1"
    override suspend fun run(args: Array<String>): Either<Error, Unit> {
        logOk("Version: $VERSION")
        return Either.Right(Unit)
    }
}