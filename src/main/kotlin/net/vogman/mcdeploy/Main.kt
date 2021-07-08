package net.vogman.mcdeploy

import kotlin.system.exitProcess

suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        exitProcess(HelpPrinter.run(arrayOf()).toInt())
    }

    val command: Command = when (args.first()) {
        "new" -> NewTemplate
        "deploy" -> DeployServer
        "update" -> UpdateServer
        "help" -> HelpPrinter
        else -> ErrorOut("Unknown subcommand '${args.first()}'. For usage, run with the 'help' subcommand", Error.User)
    }

    exitProcess(command.run(args.sliceArray(1 until args.size)).toInt())
}