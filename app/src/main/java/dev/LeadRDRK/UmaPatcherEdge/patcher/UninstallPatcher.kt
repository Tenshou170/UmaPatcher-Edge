package dev.LeadRDRK.UmaPatcherEdge.patcher

import android.content.Context
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
import java.io.File

/**
 * Removes Hachimi from the installed game without uninstalling the game itself:
 * unmounts any legacy APK mounts, restores the original libmain, removes
 * Hachimi plugin libraries from the game's lib dir, and deletes the
 * hachimi_internal_files marker. Save data and downloaded game data are kept.
 */
class UninstallPatcher : Patcher() {
    override val successMessageRes: Int = R.string.uninstall_success_msg
    override val failureMessageRes: Int = R.string.uninstall_failed_msg

    override suspend fun run(context: Context): Boolean {
        val packageInfo = GameChecker.getPackageInfo(context.packageManager) ?: run {
            log(context.getString(R.string.game_not_installed))
            return false
        }
        val appInfo = packageInfo.applicationInfo ?: return false

        // Remove the legacy mount script and unmount legacy mounts first, so
        // restored files are not shadowed and nothing remounts the APK later
        if (RootUtils.testDirectory(LEGACY_MOUNT_SCRIPT_DIR)) {
            task = context.getString(R.string.removing_legacy_files)
            progress = -1f

            if (AppPatcher.isApkMounted(context)) AppPatcher.unmountApk(context)
            RootUtils.removeDirectory(LEGACY_MOUNT_SCRIPT_DIR)
        }

        if (AppPatcher.isApkMounted(context)) {
            task = context.getString(R.string.uninstall_unmounting)
            progress = -1f

            if (!AppPatcher.unmountApk(context)) {
                log(context.getString(R.string.uninstall_unmount_failed))
                return false
            }
        }

        val apkDir = File(appInfo.publicSourceDir ?: return false).parentFile
        // Direct install patches lib/arm64, repacked installs keep lib/arm64-v8a
        val libDirs = listOf("lib/arm64", "lib/arm64-v8a")
            .mapNotNull { dir -> apkDir?.resolve(dir)?.takeIf { RootUtils.testDirectory(it.path) } }

        for (libDir in libDirs) {
            task = context.getString(R.string.uninstall_restoring_lib)
            progress = -1f

            val lib = libDir.resolve("libmain.so")
            val origLib = libDir.resolve("libmain_orig.so")
            if (RootUtils.testFile(origLib.path)) {
                if (!RootUtils.moveGameLibrary(origLib.path, lib.path).isSuccess) {
                    log(context.getString(R.string.uninstall_restore_failed))
                    return false
                }
                log(context.getString(R.string.uninstall_lib_restored))
            } else {
                log(context.getString(R.string.uninstall_lib_not_patched))
            }

            task = context.getString(R.string.uninstall_removing_plugins)
            progress = -1f

            val res = RootUtils.removeGamePlugins(libDir.path)
            if (res.isSuccess) {
                log(context.getString(R.string.uninstall_plugins_removed))
            } else {
                log(res.out.orEmpty().joinToString("\n"))
                log(res.err.orEmpty().joinToString("\n"))
            }
        }

        if (libDirs.isEmpty()) {
            log(context.getString(R.string.app_lib_dir_not_found))
        }

        // Remove the internal files dir marker so a stale one does not affect
        // future installs
        val dataDir = appInfo.dataDir
        if (dataDir != null) {
            val marker = File(dataDir, "files").resolve(ExtensionsPatcher.INTERNAL_FILES_MARKER_NAME)
            if (RootUtils.testFile(marker.path) && RootUtils.removeFile(marker.path).isSuccess) {
                log(context.getString(R.string.uninstall_marker_removed))
            }
        }

        return true
    }
}
