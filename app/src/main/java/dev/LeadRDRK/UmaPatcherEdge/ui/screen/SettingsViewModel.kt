package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.core.PrefKey
import dev.LeadRDRK.UmaPatcherEdge.core.dataStore
import dev.LeadRDRK.UmaPatcherEdge.core.getPrefValue
import dev.LeadRDRK.UmaPatcherEdge.utils.ksFile
import dev.LeadRDRK.UmaPatcherEdge.utils.showToast
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    var checkForUpdates by mutableStateOf(false)
    var appLibsVersion by mutableStateOf("")
    var hachimiRepo by mutableStateOf("")
    var configRead by mutableStateOf(false)

    fun loadConfig(context: Context) {
        viewModelScope.launch {
            checkForUpdates = context.getPrefValue(PrefKey.CHECK_FOR_UPDATES) as Boolean
            appLibsVersion = context.getPrefValue(PrefKey.APP_LIBS_VERSION) as String
            hachimiRepo = context.getPrefValue(PrefKey.HACHIMI_REPO) as String
            configRead = true
        }
    }

    fun updateCheckForUpdates(context: Context, value: Boolean) {
        checkForUpdates = value
        viewModelScope.launch {
            context.dataStore.edit { it[PrefKey.CHECK_FOR_UPDATES] = value }
        }
    }

    fun updateHachimiRepo(context: Context, value: String) {
        hachimiRepo = value
        viewModelScope.launch {
            context.dataStore.edit { it[PrefKey.HACHIMI_REPO] = value }
        }
    }

    fun forceRedownloadMod() {
        appLibsVersion = ""
        // This will be saved to DataStore via the LaunchedEffect in UI or we can do it here
    }

    fun saveAppLibsVersion(context: Context, value: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[PrefKey.APP_LIBS_VERSION] = value }
        }
    }

    fun exportKeystore(context: Context, uri: Uri) {
        val ksFile = context.ksFile
        if (!ksFile.exists()) return

        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    ksFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                context.showToast(context.getString(R.string.file_saved), Toast.LENGTH_SHORT)
            } catch (e: Exception) {
                context.showToast(context.getString(R.string.failed_to_save_file), Toast.LENGTH_SHORT)
            }
        }
    }

    fun importKeystore(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    context.ksFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                context.showToast(context.getString(R.string.keystore_imported), Toast.LENGTH_SHORT)
            } catch (e: Exception) {
                context.showToast(context.getString(R.string.failed_to_import_keystore), Toast.LENGTH_SHORT)
            }
        }
    }
}
