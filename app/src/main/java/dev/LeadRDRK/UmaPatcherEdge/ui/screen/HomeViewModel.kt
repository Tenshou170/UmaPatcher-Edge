package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
import dev.LeadRDRK.UmaPatcherEdge.core.PrefKey
import dev.LeadRDRK.UmaPatcherEdge.core.PluginEntry
import dev.LeadRDRK.UmaPatcherEdge.core.PluginManager
import dev.LeadRDRK.UmaPatcherEdge.core.dataStore
import dev.LeadRDRK.UmaPatcherEdge.core.getPrefValue
import dev.LeadRDRK.UmaPatcherEdge.utils.showToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _packageInfo = MutableStateFlow<PackageInfo?>(null)
    val packageInfo: StateFlow<PackageInfo?> = _packageInfo.asStateFlow()

    private val _plugins = MutableStateFlow<List<PluginEntry>>(emptyList())
    val plugins: StateFlow<List<PluginEntry>> = _plugins.asStateFlow()

    // Preferences handled in ViewModel
    var modSource by mutableStateOf("github")
    var customSoUri by mutableStateOf("")
    var customSoName by mutableStateOf("")

    fun refreshStatus(context: Context) {
        viewModelScope.launch {
            _packageInfo.value = GameChecker.getPackageInfo(context.packageManager)
            _plugins.value = PluginManager.listPlugins(context)
            
            modSource = context.getPrefValue(PrefKey.HACHIMI_MOD_SOURCE) as String
            customSoUri = context.getPrefValue(PrefKey.CUSTOM_MOD_SO_URI) as String
            customSoName = context.getPrefValue(PrefKey.CUSTOM_MOD_SO_NAME) as String
        }
    }

    fun updateModSource(context: Context, source: String) {
        modSource = source
        viewModelScope.launch {
            context.dataStore.edit { it[PrefKey.HACHIMI_MOD_SOURCE] = source }
        }
    }

    fun updateCustomSo(context: Context, uri: Uri, name: String) {
        customSoUri = uri.toString()
        customSoName = name
        viewModelScope.launch {
            context.dataStore.edit {
                it[PrefKey.CUSTOM_MOD_SO_URI] = uri.toString()
                it[PrefKey.CUSTOM_MOD_SO_NAME] = name
            }
        }
    }

    fun addPlugin(context: Context, uri: Uri) {
        viewModelScope.launch {
            val added = PluginManager.addPlugin(context, uri)
            if (added == null) {
                context.showToast(context.getString(R.string.failed_to_add_plugin), Toast.LENGTH_SHORT)
            } else {
                context.showToast(context.getString(R.string.plugin_added), Toast.LENGTH_SHORT)
            }
            _plugins.value = PluginManager.listPlugins(context)
        }
    }

    fun setPluginEnabled(context: Context, fileName: String, enabled: Boolean) {
        PluginManager.setEnabled(context, fileName, enabled)
        _plugins.value = PluginManager.listPlugins(context)
    }

    fun removePlugin(context: Context, fileName: String) {
        PluginManager.removePlugin(context, fileName)
        _plugins.value = PluginManager.listPlugins(context)
        context.showToast(context.getString(R.string.plugin_removed), Toast.LENGTH_SHORT)
    }
}
