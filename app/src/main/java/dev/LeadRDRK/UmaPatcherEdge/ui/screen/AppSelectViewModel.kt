package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSelectViewModel : ViewModel() {
    private val _apps = MutableStateFlow<List<PackageInfo>>(emptyList())
    val apps: StateFlow<List<PackageInfo>> = _apps.asStateFlow()

    fun loadApps(pm: PackageManager) {
        _apps.value = GameChecker.getAllPackageInfo(pm).filterNotNull()
    }
}
