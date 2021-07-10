package net.vogman.mcdeploy

import arrow.core.Either
import com.sksamuel.hoplite.ConfigFailure
import java.nio.file.Path

sealed class Error {
    object MissingConfig : Error()
    object NoEULA : Error()
    data class TemplateNotEmpty(val target: Path) : Error()
    data class AlreadyExists(val file: Path) : Error()
    data class WrongArguments(val expected: Int, val received: Int) : Error()
    data class HashMismatch(val file: Path, val expected: String, val actual: String) : Error()
    data class NoSuchFile(val expected: Path) : Error()
    data class ConfigParse(val message: ConfigFailure) : Error()
    data class RequestFailed(val message: String) : Error()
    data class FilesystemError(val message: String) : Error()
    data class ManifestMissingValue(val missing: String) : Error()
}

fun <T> Either<Error, T>.toInt(): Int =
    when (this) {
        is Either.Right -> 0
        is Either.Left -> 1
    }
