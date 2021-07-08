package net.vogman.mcdeploy

import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

object NewTemplate : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        if (args.size != 1) {
            logErr("'new' subcommand accepts exactly one argument")
            return Result.Err(Error.User)
        }

        val target = args.first()

        if (runCatching {
                Path(target).createDirectories()
                Path(target, "mcdeploy.toml").createFile()
            }.isFailure) {
            logErr("Directory $target already exists and is not empty.")
            return Result.Err(Error.User)
        }
        logOk("Server template created in $target!")
        return Result.Ok(Unit)
    }
}