package net.vogman.mcdeploy

object HelpPrinter : Command {
    override suspend fun run(args: Array<String>): Result {
        if (args.isNotEmpty()) {
            logErr("'help' subcommand accepts exactly zero arguments")
            return Result.Err(Error.User)
        }
        println(HELP)
        return Result.Ok
    }
}