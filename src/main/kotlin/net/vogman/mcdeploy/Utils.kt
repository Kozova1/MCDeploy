package net.vogman.mcdeploy

import io.ktor.client.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

fun File.writeNewBytes(bytes: ByteArray): Result<Unit, Error> {
    if (exists()) {
        logErr("File $this already exists.")
        return Result.Err(Error.Filesystem)
    }
    writeBytes(bytes)
    return Result.Ok(Unit)
}

fun File.writeNewText(text: String, charset: Charset): Result<Unit, Error> {
    if (exists()) {
        logErr("File $this already exists.")
        return Result.Err(Error.Filesystem)
    }
    writeText(text, charset)
    return Result.Ok(Unit)
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

suspend fun List<ExternalFile>.fetchAll(targetDir: Path, overwrite: Boolean = false): Result<Unit, Error> =
    HttpClient().use { client ->
        if (targetDir.exists() && overwrite) {
            logErr("Directory $targetDir")
            return Result.Err(Error.Filesystem)
        }
        if (isNotEmpty()) {
            val ret = runCatching {
                targetDir.createDirectories()
            }
            if (ret.isFailure) {
                logErr("Failed to create directory $targetDir: ${ret.exceptionOrNull()?.localizedMessage}")
                return Result.Err(Error.User)
            }
            forEachIndexed { index, file ->
                println("[${index + 1}/$size] Starting download of ${targetDir.resolve(file.FileName)} from ${file.URL}")
                val bytes = when (val fRet = file.fetch(client)) {
                    is Result.Err -> return fRet.map {}
                    is Result.Ok -> fRet.ok
                }
                logOk("Downloaded ${file.FileName}")

                println("Verifying ${file.FileName}")
                val hashed = sha1sum(bytes)
                println("Downloaded: $hashed")
                println("Configured: ${file.Sha1Sum}")
                if (file.Sha1Sum == hashed) {
                    logOk("SHA-1 Match! Continuing")
                } else {
                    logErr("SHA-1 Mismatch! Exiting")
                    return Result.Err(Error.Hash)
                }
                logOk("SHA-1 Match! Continuing")
                if (overwrite) {
                    targetDir.resolve(file.FileName).toFile().writeBytes(bytes)
                } else {
                    when (val retFile = targetDir.resolve(file.FileName).toFile().writeNewBytes(bytes)) {
                        is Result.Err -> return retFile.map { }
                        is Result.Ok -> {
                        }
                    }
                }
                println("[${index + 1}/$size] Done.")
                println()
            }
        }
        return Result.Ok(Unit)
    }