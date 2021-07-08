package net.vogman.mcdeploy

fun ServerJarFetcher.CopyFile.fetchImpl(config: Config): Result<ByteArray, Error> {
    assert(config.Server.JarSource is ServerJarFetcher.CopyFile)
    if (!CopyFrom.exists()) {
        logErr("File ${CopyFrom.canonicalPath} does not exist!")
        return Result.Err(Error.User)
    }
    println("Copying server.jar from ${CopyFrom.canonicalPath}")
    val bytes = CopyFrom.readBytes()
    logOk("server.jar read successfully")
    return Result.Ok(bytes)
}