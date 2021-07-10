package net.vogman.mcdeploy

import arrow.core.Either
import java.io.File
import kotlin.io.path.Path

object UpdateServer : Command {
    override suspend fun run(args: Array<String>): Either<Error, Unit> {
        if (args.isNotEmpty()) {
            logErr("'update' subcommand accepts exactly zero arguments. Use the 'help' subcommand to see usage.")
            return Either.Left(Error.WrongArguments(0, args.size))
        }

        val configFile = File("./mcdeploy.toml")
        if (!configFile.exists()) {
            logErr("Config file ./mcdeploy.toml does not exist. Please create it.")
            return Either.Left(Error.MissingConfig)
        }

        val config = when (val res = Config.loadConfig(configFile)) {
            is Either.Left -> return res.map {}
            is Either.Right -> res.value
        }

        if (!config.Server.AgreeToEULA) {
            logErr("To host a Minecraft server, you must first agree to the EULA: https://account.mojang.com/documents/minecraft_eula")
            println("\tWhen you have read the EULA and agreed to it, please add the following line to mcdeploy.toml under the [Server] section")
            println("AgreeToEULA = true")
            return Either.Left(Error.NoEULA)
        }

        println("About to start updating the server")
        println("WARNING: This *will* overwrite your datapacks, server.properties, run.sh, and server.jar files")
        while (true) {
            print("Do you want to continue? [y/n] ")
            val response = readLine() ?: continue
            when (response) {
                "y", "Y" -> {
                    logOk("Updating server...")
                    break
                }
                "n", "N" -> {
                    logOk("Server update cancelled. Exiting...")
                    return Either.Right(Unit)
                }
            }
        }

        val serverJar = when (val res = config.Server.JarSource.fetch(config)) {
            is Either.Left -> return res.map {  }
            is Either.Right -> res.value
        }
        logOk("Downloaded and verified server.jar")

        File("server.jar").writeBytes(serverJar)
        logOk("Written server.jar")

        File("eula.txt").writeText("eula = true\n")
        logOk("Written EULA.txt")

        File("run.sh").writeText(config.genRunScript(), Charsets.UTF_8)
        logOk("Written run.sh")

        File("server.properties").writeText(config.genServerProperties(), Charsets.UTF_8)
        logOk("written server.properties")

        if (config.Datapacks != null) {
            println("Starting to fetch datapacks (${config.Datapacks.Files.size})")
            val path = config.Datapacks.TargetDir ?: Path("world", "datapacks")

            when (val ret = config.Datapacks.Files.fetchAll(path, overwrite = true)) {
                is Either.Left -> return ret.map {}
                is Either.Right -> logOk("Finished fetching datapacks")
            }
        }

        if (config.Plugins != null) {
            println("Starting to fetch plugins (${config.Plugins.Files.size})")
            val path = config.Plugins.TargetDir ?: Path("plugins")

            when (val ret = config.Plugins.Files.fetchAll(path, overwrite = true)) {
                is Either.Left -> return ret.map {}
                is Either.Right -> logOk("Finished fetching plugins")
            }

        }

        logOk("Done! Please run the server now.")
        return Either.Right(Unit)
    }
}