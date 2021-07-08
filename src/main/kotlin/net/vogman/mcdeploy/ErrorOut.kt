package net.vogman.mcdeploy

class ErrorOut(private val message: String, private val error: Error) : Command {
    override suspend fun run(args: Array<String>): Result<Unit, Error> {
        logErr(message)
        return Result.Err(error)
    }
}