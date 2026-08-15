package dev.LeadRDRK.UmaPatcherEdge.patcher

import android.content.Context
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
import com.topjohnwu.superuser.Shell

object RootUtils {
    private fun shellArg(path: String): String = "'${path.replace("'", "'\"'\"'")}'"

    fun isRootOperationAllowed(context: Context): Boolean {
        return Shell.isAppGrantedRoot() == true && GameChecker.isPackageInstalled(context.packageManager)
    }

    fun testFile(path: String): Boolean {
        return Shell.cmd("test -f ${shellArg(path)}").exec().isSuccess
    }

    fun testDirectory(path: String): Boolean {
        return Shell.cmd("test -d ${shellArg(path)}").exec().isSuccess
    }

    fun createDirectory(path: String): Shell.Result {
        return Shell.cmd(
            "mkdir -p ${shellArg(path)}"
        ).exec()
    }

    fun removeDirectory(path: String): Shell.Result {
        return Shell.cmd(
            "rm -rf ${shellArg(path)}"
        ).exec()
    }

    fun moveFile(src: String, dest: String): Shell.Result {
        return Shell.cmd(
            "mv ${shellArg(src)} ${shellArg(dest)}"
        ).exec()
    }

    fun chmod(path: String, perm: String): Shell.Result {
        return Shell.cmd(
            "chmod $perm ${shellArg(path)}"
        ).exec()
    }

    fun chown(path: String, owner: String): Shell.Result {
        return Shell.cmd(
            "chown $owner ${shellArg(path)}"
        ).exec()
    }

    fun copyGameLibrary(src: String, dest: String): Shell.Result {
        return Shell.cmd(
            "cp ${shellArg(src)} ${shellArg(dest)}",
            "chown system:system ${shellArg(dest)}",
            "chmod 755 ${shellArg(dest)}"
        ).exec()
    }

    fun moveGameLibrary(src: String, dest: String): Shell.Result {
        return Shell.cmd(
            "mv ${shellArg(src)} ${shellArg(dest)}",
            "chown system:system ${shellArg(dest)}",
            "chmod 755 ${shellArg(dest)}"
        ).exec()
    }
}