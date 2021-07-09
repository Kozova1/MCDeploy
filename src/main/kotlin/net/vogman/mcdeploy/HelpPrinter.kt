package net.vogman.mcdeploy

object HelpPrinter : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        println(this.javaClass.getResource("/Help.txt"))
        return Result.Ok(Unit)
    }
}