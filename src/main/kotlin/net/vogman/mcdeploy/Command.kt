package net.vogman.mcdeploy

sealed interface Command {
    suspend fun run(args: Array<String>): Result
}