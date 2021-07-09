package net.vogman.mcdeploy

import java.io.File
import kotlin.io.path.Path

object WriteService : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        println("Creating service file...")
        val systemdService = File("minecraft-server.service")
        val serviceContent = this.javaClass.getResource("/systemd-template.service")!!.readText(Charsets.UTF_8) + """
            WorkingDirectory = ${Path("").toAbsolutePath()}
            
            ExecStart = /usr/bin/screen -DmS mc-server ./run.sh
            
            ExecStop = /usr/bin/screen -p 0 -S mc-server -X eval 'stuff "/title @a title {\"text\":\"WARNING: SERVER WILL SHUT DOWN IN 15 SECONDS!\",\"bold\":true,\"underlined\":true,\"color\":\"dark_red\"}"\015'
            ExecStop = /bin/sleep 15
            ExecStop = /usr/bin/screen -p 0 -S mc-server -X eval 'stuff "save-all"\015'
            ExecStop = /usr/bin/screen -p 0 -S mc-server -X eval 'stuff "stop"\015'
        """.trimIndent()
        systemdService.writeText(serviceContent)
        logOk("Written service file")
        println("Please change the Description in the service file to match your server,")
        println("As well as the 'mc-server' parts to some unique name. USE THE SAME NAME FOR ALL OF THEM!")
        logOk("Done.")
        return Result.Ok(Unit)
    }
}