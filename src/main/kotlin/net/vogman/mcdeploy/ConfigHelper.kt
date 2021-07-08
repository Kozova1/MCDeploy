package net.vogman.mcdeploy

import kotlin.text.Charsets.UTF_8

object ConfigHelper : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        if (args.isEmpty()) {
            logErr("'describe' subcommand expects at least one argument. Use the 'help' subcommand to see usage.")
            return Result.Err(Error.User)
        }

        for ((index, arg) in args.withIndex()) {
            val resUrl = this.javaClass.getResource("/docs/" + arg.replace('.', '/') + ".txt")
            if (resUrl == null) {
                logErr("No config with path '${args.first()}' exists.")
                return Result.Err(Error.User)
            }
            val text = resUrl.readText(UTF_8)
            println("Help for @${arg}")
            println()
            println(text)
            if (index != args.size - 1) {
                println()
            }
        }

        return Result.Ok(Unit)
    }
}