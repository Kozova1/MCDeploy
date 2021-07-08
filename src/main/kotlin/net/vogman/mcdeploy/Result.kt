package net.vogman.mcdeploy

sealed class Result {
    object Ok: Result()
    data class Err(val error: Error) : Result()
}

enum class Error {
    User,
    Server,
    Hash,
}

fun Result.toInt(): Int =
    when (this) {
        is Result.Ok -> 0
        is Result.Err ->
            when (this.error) {
                Error.User -> 1
                Error.Server -> 2
                Error.Hash -> 4
            }
    }