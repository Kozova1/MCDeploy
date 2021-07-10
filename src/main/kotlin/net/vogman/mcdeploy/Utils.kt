package net.vogman.mcdeploy

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.flatten
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
    val colorReset = "\u001B[0m"
    val colorRed = "\u001B[31m"
    println("[${colorRed}X${colorReset}] $msg")
}

fun logOk(msg: String) {
    val colorReset = "\u001B[0m"
    val colorGreen = "\u001B[32m"
    println("[${colorGreen}✓${colorReset}] $msg")
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
                .setUnit("KiB", 1)
                .setSpeedUnit(ChronoUnit.SECONDS)
                .showSpeed()
                .setTaskName(it.FileName)
                .build()
        }

        val resultList: List<Either<Error, Unit>> =
            this.zip(progressBars).parMap { (it, progressBar) ->
                Either.catch {
                    HttpClient().use { client ->
                        it.fetchWrite(client, targetDir, progressBar)
                    }
                }.mapLeft {
                    val status = (it as ClientRequestException).response.status
                    Error.RequestFailed("${status.value} (${status.description})")
                }.flatten()
            }

        return resultList.fold(Either.Right(Unit) as Either<Error, Unit>) { folded, toFold ->
            when (folded) {
                is Either.Left -> folded
                is Either.Right -> toFold.map { }
            }
        }
    }
    return Either.Right(Unit)
}


suspend fun HttpClient.downloadWithProgressBar(url: String, progressBarName: String): HttpResponse {
    return ProgressBarBuilder()
        .setStyle(ProgressBarStyle.ASCII)
        .setInitialMax(0)
        .setUnit("KiB", 1)
        .setSpeedUnit(ChronoUnit.SECONDS)
        .showSpeed()
        .setTaskName(progressBarName)
        .build().use { pbar ->
            get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    pbar.maxHint(contentLength / 1024)
                    pbar.stepTo(bytesSentTotal / 1024)
                }
            }
        }
}


suspend fun HttpClient.downloadWithProgressBar(url: URL, progressBarName: String): HttpResponse =
    ProgressBarBuilder()
        .setStyle(ProgressBarStyle.ASCII)
        .setInitialMax(0)
        .setUnit("KiB", 1)
        .setSpeedUnit(ChronoUnit.SECONDS)
        .showSpeed()
        .setTaskName(progressBarName)
        .build().use { pbar ->
            get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    pbar.maxHint(contentLength / 1024)
                    pbar.stepTo(bytesSentTotal / 1024)
                }
            }
        }