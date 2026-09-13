package dev.LeadRDRK.UmaPatcherEdge.core

import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import dev.LeadRDRK.UmaPatcherEdge.BuildConfig
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UpdateChecker {
    private const val CHECK_TIMEOUT = 300000 // 5 minutes

    private val releases = GitHubReleases("Tenshou170/UmaPatcher-Edge")
    private val currentVersion = parseVersion(BuildConfig.VERSION_NAME)
    private val scope = CoroutineScope(Dispatchers.IO)

    private var running = false
    var callback: (String) -> Unit = {}

    fun init(context: Context) {
        scope.launch {
            if (context.getPrefValue(PrefKey.CHECK_FOR_UPDATES) as Boolean) {
                val lastUpdateCheck = context.getPrefValue(PrefKey.LAST_UPDATE_CHECK) as Long
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateCheck < CHECK_TIMEOUT)
                    return@launch

                context.dataStore.edit { preferences ->
                    preferences[PrefKey.LAST_UPDATE_CHECK] = currentTime
                }

                try {
                    running = true
                    rawRun()
                }
                catch (_: Exception) {}
                running = false
            }
        }
    }

    fun run(context: Context? = null) {
        if (running) return
        running = true

        scope.launch {
            try {
                if (!rawRun()) {
                    context?.showToast(
                        context.getString(R.string.no_updates_available),
                        Toast.LENGTH_SHORT
                    )
                }
            }
            catch (_: Exception) {
                context?.showToast(
                    context.getString(R.string.failed_to_check_for_updates),
                    Toast.LENGTH_SHORT
                )
            }
            running = false
        }
    }

    private fun rawRun(): Boolean {
        val release = releases.fetchLatest()
        val tagName = release["tag_name"] as String
        val latestVersion = parseVersion(tagName.removePrefix("v"))

        return if (latestVersion != null && currentVersion != null && isNewer(latestVersion, currentVersion)) {
            callback(tagName)
            true
        } else false
    }

    fun getReleaseUrl(tagName: String) = releases.getReleaseUrl(tagName)

    /**
     * Returns true if [a] is strictly greater than [b] as a semver triple.
     */
    private fun isNewer(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Boolean {
        if (a.first != b.first) return a.first > b.first
        if (a.second != b.second) return a.second > b.second
        return a.third > b.third
    }

    /**
     * Parses a semver string of the form [v]MAJOR.MINOR.PATCH into a
     * comparable Triple. Returns null for malformed strings so callers
     * can treat them as "not newer".
     */
    private fun parseVersion(s: String): Triple<Int, Int, Int>? {
        val parts = s.removePrefix("v").split(".")
        if (parts.size < 3) return null
        return try {
            Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (_: NumberFormatException) {
            null
        }
    }
}