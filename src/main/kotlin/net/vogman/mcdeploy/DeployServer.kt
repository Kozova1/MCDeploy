package net.vogman.mcdeploy

import java.io.File
import kotlin.io.path.Path

object DeployServer : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        if (args.isNotEmpty()) {
            logErr("'deploy' subcommand accepts exactly zero arguments. Use the 'help' subcommand to see usage.")
            return Result.Err(Error.User)
        }


        val configFile = File("mcdeploy.toml")
        if (!configFile.exists()) {
            logErr("Config file 'mcdeploy.toml' does not exist. Please create it.")
            return Result.Err(Error.User)
        }

        val contentsExceptConfig = File("").listFiles { _, name -> name != "mcdeploy.toml" }
        if (contentsExceptConfig != null && contentsExceptConfig.isNotEmpty()) {
            logErr("Current directory contains files except for 'mcdeploy.toml'. Delete them and run again or use the 'update' subcommand instead")
        }

        val configResult = Config.loadConfig(configFile)
        when (configResult) {
            is Result.Err -> return configResult.map {}
            is Result.Ok -> {
            }
        }
        val config = configResult.ok


        if (!config.Server.AgreeToEULA) {
            logErr("To host a Minecraft server, you must first agree to the EULA: https://account.mojang.com/documents/minecraft_eula")
            println("\tWhen you have read the EULA and agreed to it, please add the following line to mcdeploy.toml under the [Server] section")
            println("AgreeToEULA = true")
            return Result.Err(Error.User)
        }

        val serverJarResult = config.Server.JarSource.fetch(config)
        if (serverJarResult is Result.Err) {
            return serverJarResult.map { }
        }
        val serverJar = (serverJarResult as Result.Ok).ok

        when (val ret = File("server.jar").writeNewBytes(serverJar)) {
            is Result.Err -> return ret.map {}
            is Result.Ok -> {
            }
        }
        logOk("Written server.jar")

        when (val ret = File("eula.txt").writeNewText("eula = true\n", Charsets.UTF_8)) {
            is Result.Err -> return ret.map {}
            is Result.Ok -> {
            }
        }
        logOk("Written eula.txt")

        when (val ret = File("run.sh").writeNewText(config.genRunScript(), Charsets.UTF_8)) {
            is Result.Err -> return ret.map {}
            is Result.Ok -> {
            }
        }
        logOk("Written run.sh")

        when (val ret = File("server.properties").writeNewText(config.genServerProperties(), Charsets.UTF_8)) {
            is Result.Err -> return ret.map {}
            is Result.Ok -> {
            }
        }
        logOk("written server.properties")

        if (config.Datapacks != null) {
            println("Starting to fetch datapacks (${config.Datapacks.Files.size})")
            val path = config.Datapacks.TargetDir ?: Path("world", "datapacks")

            when (val ret = config.Datapacks.Files.fetchAll(path, overwrite = false)) {
                is Result.Err -> return ret.map {}
                is Result.Ok -> {
                }
            }
            logOk("Finished fetching datapacks")
        }

        if (config.Plugins != null) {
            println("Starting to fetch plugins (${config.Plugins.Files.size})")
            val path = config.Plugins.TargetDir ?: Path("plugins")

            when (val ret = config.Plugins.Files.fetchAll(path, overwrite = false)) {
                is Result.Err -> return ret.map {}
                is Result.Ok -> {
                }
            }

            logOk("Finished fetching datapacks")
        }

        logOk("Done! Please run the server now.")
        return Result.Ok(Unit)
    }
}