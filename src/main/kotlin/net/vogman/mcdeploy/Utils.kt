package net.vogman.mcdeploy

import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.system.exitProcess

fun writeEula() = File("./eula.txt").writeText("eula = true\n", Charsets.UTF_8)

fun File.writeNewBytes(bytes: ByteArray) {
    if (exists()) {
        logErr("File $this already exists.")
        exitProcess(1)
    }
    writeBytes(bytes)
}

fun File.writeNewText(text: String, charset: Charset) {
    if (exists()) {
        logErr("File $this already exists.")
        exitProcess(1)
    }
    writeText(text, charset)
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