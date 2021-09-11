package net.vogman.mcdeploy

import arrow.core.Either
import kotlin.system.exitProcess

fun printErrorMessage(args: Array<String>, returnCode: Either<Error, Unit>): Unit = when (val ret = (returnCode as Either.Left).value) {
    is Error.MissingConfig -> logErr("You need to provide a 'mcdeploy.toml' config file. See 'MCDeploy help' for more info.")
    is Error.NoEULA -> {
        logErr("To use MCDeploy you must agree to the Minecraft EULA: https://account.mojang.com/documents/minecraft_eula")
        println("When you have agreed to it, add a key to 'mcdeploy.toml' under the [Server] section with the name AgreeToEULA and the value true")
    }
    is Error.TemplateNotEmpty -> logErr("Directory ${ret.target} is not empty (or contains files other than 'mcdeploy.toml')")
    is Error.AlreadyExists -> logErr("File ${ret.file} already exists")
    is Error.WrongArguments -> {
        logErr("Wrong amount of arguments for subcommand '${args.first()}'")
        logErr("Expected ${ret.expected} but got ${ret.received}")
    }
    is Error.HashMismatch -> {
        logErr("SHA-1 Mismatch for file ${ret.file}!")
        logErr("Expected: ${ret.expected}")
        logErr("Received: ${ret.actual}")
    }
    is Error.NoSuchFile -> logErr("File ${ret.expected} does not exist")
    is Error.ConfigParse -> logErr("Config is invalid: ${ret.message.description()}")
    is Error.RequestFailed -> logErr("Download failed: ${ret.message}")
    is Error.FilesystemError -> logErr("Write or Create failed: ${ret.message}")
    is Error.ManifestMissingValue -> logErr(ret.missing)
    is Error.Combined -> ret.errors.map { Either.Left(it) }.forEach {
        printErrorMessage(args, it)
        println()
    }
}

suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        exitProcess(HelpPrinter.run(arrayOf()).toInt())
    }

    val command: Command = when (args.first()) {
        "new" -> NewTemplate
        "deploy" -> DeployServer
        "update" -> UpdateServer
        "write-systemd-service" -> WriteService
        "version" -> VersionPrinter
        "describe" -> ConfigHelper
        "help" -> HelpPrinter
        else -> HelpPrinter
    }

    val returnCode = command.run(args.sliceArray(1 until args.size))
    if (returnCode.isLeft()) {
        printErrorMessage(args, returnCode)
    }

    exitProcess(returnCode.toInt())
}