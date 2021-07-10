package net.vogman.mcdeploy

import arrow.core.Either
import kotlin.text.Charsets.UTF_8

object ConfigHelper : Command {
    override suspend fun run(args: Array<String>): Either<Error, Unit> {
        if (args.isEmpty()) {
            logErr("'describe' subcommand expects at least one argument. Use the 'help' subcommand to see usage.")
            return Either.Left(Error.WrongArguments(1, 0))
        }

        for ((index, arg) in args.withIndex()) {
            val resUrl = this.javaClass.getResource("/docs/" + arg.replace('.', '/') + ".txt")
            if (resUrl == null) {
                logErr("No config with path '${args.first()}' exists.")
                return Either.Left(Error.MissingConfig)
            }
            val text = resUrl.readText(UTF_8)
            println("Help for @${arg}")
            println()
            println(text)
            if (index != args.size - 1) {
                println()
            }
        }

        return Either.Right(Unit)
    }
}