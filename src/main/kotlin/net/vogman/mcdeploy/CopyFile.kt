package net.vogman.mcdeploy

import arrow.core.Either

fun ServerJarFetcher.CopyFile.fetchImpl(config: Config): Either<Error, ByteArray> {
    assert(config.Server.JarSource is ServerJarFetcher.CopyFile)
    if (!CopyFrom.exists()) {
        logErr("File ${CopyFrom.canonicalPath} does not exist!")
        return Either.Left(Error.NoSuchFile(CopyFrom.toPath()))
    }
    println("Copying server.jar from ${CopyFrom.canonicalPath}")
    val bytes = CopyFrom.readBytes()
    logOk("server.jar read successfully")
    return Either.Right(bytes)
}