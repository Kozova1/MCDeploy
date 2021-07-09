package net.vogman.mcdeploy

import java.io.File
import kotlin.io.path.Path

object UpdateServer : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        if (args.isNotEmpty()) {
            logErr("'update' subcommand accepts exactly zero arguments. Use the 'help' subcommand to see usage.")
            return Result.Err(Error.User)
        }

        val configFile = File("./mcdeploy.toml")
        if (!configFile.exists()) {
            logErr("Config file ./mcdeploy.toml does not exist. Please create it.")
            return Result.Err(Error.User)
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

        println("About to start updating the server")
        println("WARNING: This *will* overwrite your datapacks, server.properties, run.sh, and server.jar files")
        while (true) {
            print("Do you want to continue? [y/n] ")
            val response = readLine() ?: continue
            when (response) {
                "y", "Y" -> {
                    println("Starting server update...")
                    break
                }
                "n", "N" -> {
                    println("Server update cancelled. Exiting...")
                    return Result.Ok(Unit)
                }
            }
        }

        val serverJarResult = config.Server.JarSource.fetch(config)
        if (serverJarResult is Result.Err) {
            return serverJarResult.map { }
        }
        val serverJar = (serverJarResult as Result.Ok).ok

        File("./server.jar").writeBytes(serverJar)
        logOk("Written server.jar")

        File("eula.txt").writeText("eula = true\n")
        logOk("Written EULA.txt")

        File("./run.sh").writeText(config.genRunScript(), Charsets.UTF_8)
        logOk("Written run.sh")

        File("./server.properties").writeText(config.genServerProperties(), Charsets.UTF_8)
        logOk("written server.properties")

        if (config.Datapacks != null) {
            println("Starting to fetch datapacks (${config.Datapacks.Files.size})")
            val path = config.Datapacks.TargetDir ?: Path("world", "datapacks")

            when (val ret = config.Datapacks.Files.fetchAll(path, overwrite = true)) {
                is Result.Err -> return ret.map {}
                is Result.Ok -> {
                }
            }
            logOk("Finished fetching datapacks")
        }

        if (config.Plugins != null) {
            println("Starting to fetch plugins (${config.Plugins.Files.size})")
            val path = config.Plugins.TargetDir ?: Path("plugins")

            when (val ret = config.Plugins.Files.fetchAll(path, overwrite = true)) {
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