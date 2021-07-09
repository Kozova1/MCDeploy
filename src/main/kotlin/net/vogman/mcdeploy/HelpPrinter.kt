package net.vogman.mcdeploy

object HelpPrinter : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        logOk("MCDeploy version ${VersionPrinter.VERSION}")
        println(this.javaClass.getResource("/Help.txt")!!.readText(Charsets.UTF_8))
        return Result.Ok(Unit)
    }
}