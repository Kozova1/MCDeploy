package net.vogman.mcdeploy

import arrow.core.Either
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object DeployServer : Command {
    override suspend fun run(args: Array<String>): Either<Error, Unit> {
        if (args.isNotEmpty()) {
            logErr("'deploy' subcommand accepts exactly zero arguments. Use the 'help' subcommand to see usage.")
            return Either.Left(Error.WrongArguments(0, args.size))
        }


        val configFile = File("mcdeploy.toml")
        if (!configFile.exists()) {
            logErr("Config file 'mcdeploy.toml' does not exist. Please create it.")
            return Either.Left(Error.MissingConfig)
        }

        val config = when (val res = Config.loadConfig(configFile)) {
            is Either.Left -> return res.map {}
            is Either.Right -> res.value
        }

        if (!config.Server.AgreeToEULA) {
            return Either.Left(Error.NoEULA)
        }

        val serverJarFile = Path("server.jar")
        if (serverJarFile.exists()) {
            return Either.Left(Error.AlreadyExists(serverJarFile))
        }

        val serverJar = when (val res = config.Server.JarSource.fetch(config)) {
            is Either.Left -> return res.map {  }
            is Either.Right -> res.value
        }

        when (val ret = serverJarFile.toFile().writeNewBytes(serverJar)) {
            is Either.Left -> return ret.map {}
            is Either.Right -> logOk("Written server.jar")
        }

        when (val ret = File("eula.txt").writeNewText("eula = true\n", Charsets.UTF_8)) {
            is Either.Left -> return ret.map {}
            is Either.Right -> logOk("Written eula.txt")
        }

        when (val ret = File("run.sh").writeNewText(config.genRunScript(), Charsets.UTF_8)) {
            is Either.Left -> return ret.map {}
            is Either.Right -> logOk("Written run.sh")
        }

        when (val ret = File("server.properties").writeNewText(config.genServerProperties(), Charsets.UTF_8)) {
            is Either.Left -> return ret.map {}
            is Either.Right -> logOk("written server.properties")
        }

        if (config.Datapacks != null) {
            println("Starting to fetch datapacks (${config.Datapacks.Files.size})")
            val path = config.Datapacks.TargetDir ?: Path("world", "datapacks")

            when (val ret = config.Datapacks.Files.fetchAll(path, overwrite = false)) {
                is Either.Left -> return ret.map {}
                is Either.Right -> logOk("Finished fetching datapacks")
            }
        }

        if (config.Plugins != null) {
            println("Starting to fetch plugins (${config.Plugins.Files.size})")
            val path = config.Plugins.TargetDir ?: Path("plugins")

            when (val ret = config.Plugins.Files.fetchAll(path, overwrite = false)) {
                is Either.Left -> return ret.map {}
                is Either.Right -> logOk("Finished fetching plugins")
            }
        }

        logOk("Done! Please run the server now.")
        return Either.Right(Unit)
    }
}