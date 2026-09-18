package io.github.paulsnuff.betternightlight.shizuku

import android.content.Context
import androidx.annotation.Keep
import kotlin.system.exitProcess

@Keep
class ShizukuPermissionWorker : IShizukuWorker.Stub {
    @Keep
    constructor() : super()

    @Keep
    @Suppress("unused")
    constructor(context: Context?) : super()

    override fun exec(cmd: Array<String>): Int =
        runCatching {
            val process =
                ProcessBuilder(*cmd)
                    .redirectErrorStream(true)
                    .start()
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
        }.getOrDefault(-1)

    override fun destroy() {
        exitProcess(0)
    }
}
