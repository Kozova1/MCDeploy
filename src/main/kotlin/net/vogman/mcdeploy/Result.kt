package net.vogman.mcdeploy

sealed class Result<T, E> {
    data class Ok<T, E>(val ok: T) : Result<T, E>()
    data class Err<T, E>(val error: E) : Result<T, E>()
}

enum class Error {
    User,
    Server,
    Hash,
    Filesystem,
}

fun <T> Result<T, Error>.toInt(): Int =
    when (this) {
        is Result.Ok -> 0
        is Result.Err ->
            when (this.error) {
                Error.User -> 1
                Error.Server -> 2
                Error.Hash -> 4
                Error.Filesystem -> 8
            }
    }

fun <T, E, R> Result<T, E>.mapErr(lambda: (E) -> R): Result<T, R> {
    return when (this) {
        is Result.Err -> Result.Err(lambda(this.error))
        is Result.Ok -> Result.Ok(this.ok)
    }
}

fun <T, E, R> Result<T, E>.map(lambda: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Err -> Result.Err(this.error)
        is Result.Ok -> Result.Ok(lambda(this.ok))
    }
}