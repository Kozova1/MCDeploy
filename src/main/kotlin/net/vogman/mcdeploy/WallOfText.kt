package net.vogman.mcdeploy

// TODO: Make sure this is not stale
const val DEFAULT_CONFIG: String = """[Server]
AgreeToEULA = false

[Server.JarSource]
Fetcher = "LauncherManifest"
LauncherManifestURL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
Version = "latest-release"

[Environment]
JavaArgs = []
PreLaunchCommands.txt = [ "echo Server Starting..." ]
PostExitCommands.txt = [ "echo Server Stopped" ]

[Properties]
"spawn-protection"="16"
"max-tick-time"="60000"
"query.port"="25565"
"generator-settings"=""
"sync-chunk-writes"="true"
"force-gamemode"="false"
"allow-nether"="true"
"enforce-whitelist"="false"
"gamemode"="survival"
"broadcast-console-to-ops"="true"
"enable-query"="false"
"player-idle-timeout"="0"
"difficulty"="easy"
"spawn-monsters"="true"
"broadcast-rcon-to-ops"="true"
"op-permission-level"="4"
"pvp"="true"
"entity-broadcast-range-percentage"="100"
"snooper-enabled"="true"
"level-type"="default"
"hardcore"="false"
"enable-status"="true"
"enable-command-block"="false"
"max-players"="20"
"network-compression-threshold"="256"
"resource-pack-sha1"=""
"max-world-size"="29999984"
"function-permission-level"="2"
"rcon.port"="25575"
"server-port"="25565"
"debug"="false"
"server-ip"=""
"spawn-npcs"="true"
"allow-flight"="false"
"level-name"="world"
"view-distance"="10"
"resource-pack"=""
"spawn-animals"="true"
"white-list"="false"
"rcon.password"=""
"generate-structures"="true"
"max-build-height"="256"
"online-mode"="true"
"level-seed"=""
"use-native-transport"="true"
"prevent-proxy-connections"="false"
"enable-jmx-monitoring"="false"
"enable-rcon"="false"
"motd"="A Minecraft Server"
"""

// TODO: Update help
const val HELP: String = """USAGE
    java -jar MCDeploy.jar [help | deploy | update | new DIRECTORY]

COMMANDS
    help:
        Display this screen.
    new:
        Create the template project in DIRECTORY. The directory must either not exist or be empty.
    deploy:
        Deploy the configuration specified in ./mcdeploy.toml
    update:
        Update the deployed server in the current directory according to the configuration in ./mcdeploy.toml
        The main difference between this and the 'deploy' command is that this command overwrites files,
        and warns the user before doing so.

CONFIG
    Uses the TOML format (https://github.com/toml-lang/toml)
    SECTIONS
        [Server]
        AgreeToEULA - Boolean (true | false) that signifies whether you agree with the Minecraft EULA

        [Server.JarSource]
        The way that your server will get its server.jar file
        Fetcher - Either of "LauncherManifest", "DownloadURL", "CopyFile"

        JarSource: LauncherManifest
            Downloads the server.jar from Mojang servers
            This is probably what you want, unless you want to use a custom server.jar
            LauncherManifestURL - DO NOT MESS WITH THIS UNLESS YOU'RE SURE WHAT IT DOES
                DEFAULT: LauncherManifestURL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
            Version - The version of minecraft which to download.
                Can be any version, or "latest-release" or "latest-snapshot"
                DEFAULT: Version = "latest-release"
        EXAMPLE:
            [Server]
            AgreeToEULA = true

            [Server.JarSource]
            Fetcher = "LauncherManifest"
            Version = "1.17.1"

        JarSource: CopyFile
            Copies server.jar from your own PC
            CopyFrom - Path from which to copy the server.jar
        EXAMPLE:
            [Server]
            AgreeToEULA = true

            [Server.JarSource]
            Fetcher = "CopyFile"
            CopyFrom = "~/Downloads/spigotmc.jar"

        JarSource: DownloadURL
            Downloads the server.jar from the internet
            ServerJarURL - URL from which to download the server.jar
            Sha1Sum - A string containing the SHA-1 hash of the server.jar file
        EXAMPLE:
            [Server]
            AgreeToEULA = true

            [Server.JarSource]
            Fetcher = "DownloadURL"
            ServerJarURL = "https://github.com/blah/blah/asdas/spigot.jar"
            Sha1Sum = "389e19f5481e921c47a9cfeb6b3e645e54e49cc6"

        [Environment]
        JavaArgs - List of strings that contains arguments passed to java -jar when running server.jar
            EXAMPLE: JavaArgs = [ "-Xmx4G", "-Xms2G" ]
            DEFAULT: JavaArgs = []
        PreLaunchCommands.txt - List of strings that contains shell commands that will run before server.jar starts
            EXAMPLE: PreLaunchCommands.txt = [ "echo Hello World", "echo Server Starting..." ]
            DEFAULT: PreLaunchCommands.txt = [ "echo Server Starting..." ]
        PostLaunchCommands - List of strings that contain shell commands that will run after server.jar stops
            EXAMPLE: PreLaunchCommands.txt = [ "echo Server Stopped", "echo closing...", "exit" ]
            DEFAULT: PreLaunchCommands.txt = [ "echo Server Stopped" ]            

        [[Datapacks]]
        This section contains a list of datapacks, each of which has the following properties:
        NOTE: if your datapack is a zip within a zip, you'll have to extract it manually.
        NOTE: These have no default values - you *must* supply all of them for every datapack.
        URL - URL from which to download the datapack
            EXAMPLE: URL = "https://example.com/datapacks/datapack.zip"
        Sha1Sum - A string containing the SHA-1 hash of the datapack file
            EXAMPLE: Sha1Sum = "648a6a6ffffdaa0badb23b8baf90b6168dd16b3a"
        FileName - A string containing the file name under which the datapack will be saved, relative to ./world/datapacks/
            EXAMPLE: FileName = "ChocolateEdits.zip"
        EXAMPLE:
            [[Datapacks]]
            URL = "https://example.com/datapacks/datapack.zip"
            Sha1Sum = "648a6a6ffffdaa0badb23b8baf90b6168dd16b3a"
            FileName = "ChocolateEdits.zip"


        [Properties]
        This section contains the contents of server.properties.
        All property names and values must be enclosed by double quotes ("), see EXAMPLE section.
        DEFAULT: The default server.properties
        EXAMPLE:
            [Properties]
            "white-list" = "true"
            "spawn-protection" = "32"
            "query.port" = "25565"
RETURN VALUE
    0 - Success
    1 - User error
    2 - Server error
    3 - Hash mismatch
    Other - Java / Kotlin error, please submit bug report
"""