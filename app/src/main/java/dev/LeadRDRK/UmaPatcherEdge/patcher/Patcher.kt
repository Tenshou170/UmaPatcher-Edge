package dev.LeadRDRK.UmaPatcherEdge.patcher

import android.content.Context
import android.util.Log
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
import dev.LeadRDRK.UmaPatcherEdge.utils.copyTo
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Patcher(
    var onLog: (String) -> Unit = {},
    var onProgress: (Float) -> Unit = {},
    var onTask: (String) -> Unit = {},
    var onSaveFile: (String, File, (Boolean) -> Unit) -> Unit = { _: String, _: File, _: (Boolean) -> Unit -> }
) {
    var isCancelled = false

    var progress: Float = 0f
        set(value) {
            field = value
            onProgress(value)
        }

    var task: String = ""
        set(value) {
            if (field == value) return
            field = value
            onTask(value)
        }

    fun setCallbacks(
        onLog: (String) -> Unit = {},
        onProgress: (Float) -> Unit = {},
        onTask: (String) -> Unit = {},
        onSaveFile: (String, File, (Boolean) -> Unit) -> Unit
    ) {
        this.onLog = onLog
        this.onProgress = onProgress
        this.onTask = onTask
        this.onSaveFile = onSaveFile
    }

    fun ensureActive() {
        if (isCancelled) throw kotlinx.coroutines.CancellationException("Patching cancelled by user")
    }

    abstract suspend fun run(context: Context): Boolean

    suspend fun safeRun(context: Context): Boolean {
        return try {
            run(context)
        } catch (ex: kotlinx.coroutines.CancellationException) {
            false
        } catch (ex: Exception) {
            logException(ex)
            false
        }
    }

    fun log(line: String) {
        onLog(line)
    }

    protected fun logAll(lines: Iterable<String>) {
        lines.forEach(onLog)
    }

    fun logException(ex: Exception) {
        val sw = StringWriter()
        PrintWriter(sw).use {
            ex.printStackTrace(it)
        }
        log(sw.toString())
        Log.e("UmaPatcher", "logException", ex)
    }

    protected suspend fun saveFile(filename: String, file: File): Boolean {
        return suspendCoroutine { cont ->
            onSaveFile(filename, file) { cont.resume(it) }
        }
    }

    protected fun copyFileProgress(srcFile: File, dstFile: File) {
        srcFile.inputStream().use { input ->
            dstFile.outputStream().use { output ->
                copyStreamProgress(input, output, srcFile.length())
            }
        }
    }

    protected fun copyStreamProgress(input: InputStream, output: OutputStream, length: Long) {
        val lengthFloat = length.toFloat()
        input.copyTo(output) { current ->
            ensureActive()
            progress = current / lengthFloat
        }
    }

    protected fun isDirectInstallAllowed(context: Context): Boolean {
        if (!GameChecker.isPackageInstalled(context.packageManager)) {
            log(context.getString(R.string.direct_install_unavailable))
            return false
        }
        if (Shell.isAppGrantedRoot() != true) {
            log(context.getString(R.string.root_required))
            return false
        }
        return true
    }
}