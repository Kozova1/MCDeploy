package net.vogman.mcdeploy

data class EnvironmentConfig(
    val JavaArgs: List<String>,
    val PreLaunchCommands: List<String>,
    val PostExitCommands: List<String>
)

data class ServerConfig(val JsonManifestUrl: String, val Version: String, val AgreeToEULA: Boolean)

data class Config(val Environment: EnvironmentConfig, val Server: ServerConfig, val Properties: Map<String, String>) {
    fun genServerProperties(): String =
        Properties
            .map { "${it.key} = ${it.value}" }
            .joinToString(separator = "\n") + "\n"

    fun genRunScript(): String {
        val javaArgs = Environment.JavaArgs.joinToString(separator = " ")
        val preLaunchCommands = Environment.PreLaunchCommands.joinToString(separator = "\n")
        val postExitCommands = Environment.PostExitCommands.joinToString(separator = "\n")
        return listOf(
            preLaunchCommands,
            "java $javaArgs -jar server.jar nogui",
            postExitCommands,
            "" // To ensure final newline
        ).joinToString(separator = "\n")
    }
}