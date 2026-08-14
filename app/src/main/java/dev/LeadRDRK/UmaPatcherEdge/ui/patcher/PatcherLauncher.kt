package dev.LeadRDRK.UmaPatcherEdge.ui.patcher

import android.content.Context
import dev.LeadRDRK.UmaPatcherEdge.MainActivity
import dev.LeadRDRK.UmaPatcherEdge.patcher.Patcher
import dev.LeadRDRK.UmaPatcherEdge.ui.screen.destinations.PatchingScreenDestination
import dev.LeadRDRK.UmaPatcherEdge.utils.getActivity
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PatcherLauncher {
    @Volatile var patcher: Patcher? = null
    @Volatile var patching = false

    fun launch(navigator: DestinationsNavigator, patcher: Patcher) {
        if (patching) return

        this.patcher = patcher
        navigator.navigate(PatchingScreenDestination)
    }

    fun cancelPatcher() {
        patcher?.isCancelled = true
    }
    
    suspend fun runPatcher(context: Context, callback: (Boolean) -> Unit) {
        val patcher = this.patcher
        if (patcher == null || patching) return

        val activity = context.getActivity() as MainActivity
        withContext(Dispatchers.IO) {
            activity.useKeepScreenOn {
                patching = true
                val result = patcher.safeRun(context)
                patching = false
                this@PatcherLauncher.patcher = null
                callback(result)
            }
        }
    }
}