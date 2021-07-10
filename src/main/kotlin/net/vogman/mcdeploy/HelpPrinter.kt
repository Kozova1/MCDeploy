package net.vogman.mcdeploy

import arrow.core.Either

object HelpPrinter : Command {
    override suspend fun run(args: Array<String>): Either<Error, Unit> {
        logOk("MCDeploy version ${VersionPrinter.VERSION}")
        println(this.javaClass.getResource("/Help.txt")!!.readText(Charsets.UTF_8))
        return Either.Right(Unit)
    }
}