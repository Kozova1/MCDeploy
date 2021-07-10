package net.vogman.mcdeploy

import arrow.core.Either
import arrow.core.Validated
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

object NewTemplate : Command {
    override suspend fun run(args: Array<String>): Either<Error, Unit> {
        if (args.size != 1) {
            return Either.Left(Error.WrongArguments(1, args.size))
        }

        val target = args.first()

        if (runCatching {
                Path(target).createDirectories()
                Path(target, "mcdeploy.toml").createFile()
            }.isFailure) {
            return Either.Left(Error.TemplateNotEmpty(Path(target)))
        }
        logOk("Server template created in $target!")
        return Either.Right(Unit)
    }
}