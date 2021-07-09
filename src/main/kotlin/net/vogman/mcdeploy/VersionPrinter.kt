package net.vogman.mcdeploy

object VersionPrinter : Command{
    const val VERSION: String = "v3.1"
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        logOk("Version: $VERSION")
        return Result.Ok(Unit)
    }
}