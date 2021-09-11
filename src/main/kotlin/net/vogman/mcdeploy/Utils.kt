package net.vogman.mcdeploy

import arrow.core.*
import arrow.typeclasses.Semigroup
import com.sksamuel.hoplite.fp.nel
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.security.MessageDigest
import java.time.temporal.ChronoUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

fun File.writeNewBytes(bytes: ByteArray): Either<Error, Unit> {
    if (exists()) {
        return Either.Left(Error.AlreadyExists(toPath()))
    }
    writeBytes(bytes)
    return Either.Right(Unit)
}

fun File.writeNewText(text: String, charset: Charset): Either<Error, Unit> {
    if (exists()) {
        return Either.Left(Error.AlreadyExists(toPath()))
    }
    writeText(text, charset)
    return Either.Right(Unit)
}

fun logErr(msg: String) {
    val reset = "\u001B[0m"
    val bold = "\u001B[1m"
    val colorRed = "\u001B[31m"
    System.err.println("[${colorRed}${bold}X${reset}]${colorRed} $msg${reset}")
}

fun logOk(msg: String) {
    val reset = "\u001B[0m"
    val bold = "\u001B[1m"
    val colorGreen = "\u001B[32m"
    println("[${colorGreen}${bold}✓${reset}]${colorGreen} $msg${reset}")
}

fun sha1sum(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(bytes)
    val sb = StringBuilder()
    for (byte in digest) {
        sb.append("%02x".format(byte))
    }
    return sb.toString()
}

suspend fun <A, B> Iterable<A>.parMap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

suspend fun <A, B> Nel<A>.parMap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}


suspend fun List<ExternalFile>.fetchAll(targetDir: Path, overwrite: Boolean = false): Either<Error, Unit> {
    if (targetDir.exists() && !overwrite) {
        return Either.Left(Error.AlreadyExists(targetDir))
    }
    if (isNotEmpty()) {
        val ret = runCatching {
            targetDir.createDirectories()
        }
        if (ret.isFailure) {
            return Either.Left(Error.FilesystemError(ret.exceptionOrNull()!!.localizedMessage))
        }

        val progressBars: List<ProgressBar> = this.map {
            ProgressBarBuilder()
                .setStyle(ProgressBarStyle.ASCII)
                .setInitialMax(0)
                .setUnit("KiB", 1024)
                .setSpeedUnit(ChronoUnit.SECONDS)
                .showSpeed()
                .setTaskName(it.FileName)
                .build()
        }

        val results = this.zip(progressBars).parMap { (it, progressBar) ->
            it.fetchWrite(targetDir, progressBar)
        }.separateValidated()

        return if (results.first.isEmpty()) {
            Validated.Valid(Unit)
        } else {
            Validated.Invalid(Error.Combined(results.first))
        }.toEither()
    }
    return Either.Right(Unit)
}


suspend fun HttpClient.downloadWithProgressBar(url: String, progressBarName: String, unitName: String, unitSize: Long): HttpResponse {
    return ProgressBarBuilder()
        .setStyle(ProgressBarStyle.ASCII)
        .setInitialMax(0)
        .setUnit(unitName, unitSize)
        .setSpeedUnit(ChronoUnit.SECONDS)
        .showSpeed()
        .setTaskName(progressBarName)
        .build().use { pbar ->
            get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    pbar.maxHint(contentLength)
                    pbar.stepTo(bytesSentTotal)
                }
            }
        }
}


suspend fun HttpClient.downloadWithProgressBar(url: URL, progressBarName: String, unitName: String, unitSize: Long): HttpResponse =
    ProgressBarBuilder()
        .setStyle(ProgressBarStyle.ASCII)
        .setInitialMax(0)
        .setUnit(unitName, unitSize)
        .setSpeedUnit(ChronoUnit.SECONDS)
        .showSpeed()
        .setTaskName(progressBarName)
        .build().use { pbar ->
            get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    pbar.maxHint(contentLength)
                    pbar.stepTo(bytesSentTotal)
                }
            }
        }