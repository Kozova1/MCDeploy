package net.vogman.mcdeploy

const val DEFAULT_CONFIG: String = """[Server]
JsonManifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
Version = "latest-release"
AgreeToEULA = false

[Environment]
JavaArgs = []
PreLaunchCommands = [ "echo Server Starting..." ]
PostExitCommands = [ "echo Server Stopped" ]

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

const val HELP: String = """USAGE
    java -jar MCDeploy.jar [new DIRECTORY | help | deploy]

COMMANDS
    new:
        Create the template project in DIRECTORY
    help:
        Display this screen
    deploy:
        Deploy the configuration specified in ./mcdeploy.toml

CONFIG
    Uses the TOML format (https://github.com/toml-lang/toml)
    SECTIONS
        [Server]
        Version - String that contains the version of the server.jar to download
                  Can be either a version, latest-release, or latest-snapshot
            EXAMPLE: Version = "1.17.1"
            DEFAULT: Version = "latest-release"
        JsonManifestUrl - String that contains the URL to the minecraft launcher manifest.
                          DO NOT MESS WITH THIS IF YOU DON'T KNOW WHAT YOU'RE DOING
            EXAMPLE: JsonManifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
            DEFAULT: JsonManifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
       
        [Environment]
        JavaArgs - List of strings that contains arguments passed to java -jar when running server.jar
            EXAMPLE: JavaArgs = [ "-Xmx4G", "-Xms2G" ]
            DEFAULT: JavaArgs = []
        PreLaunchCommands - List of strings that contains shell commands that will run before server.jar starts
            EXAMPLE: PreLaunchCommands = [ "echo Hello World", "echo Server Starting..." ]
            DEFAULT: PreLaunchCommands = [ "echo Server Starting..." ]
        PostLaunchCommands - List of strings that contain shell commands that will run after server.jar stops
            EXAMPLE: PreLaunchCommands = [ "echo Server Stopped", "echo closing...", "exit" ]
            DEFAULT: PreLaunchCommands = [ "echo Server Stopped" ]            
        
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
    2 - Download error
    Other - Java / Kotlin error, please submit bug report
"""