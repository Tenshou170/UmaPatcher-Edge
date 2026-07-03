package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import dev.LeadRDRK.UmaPatcherEdge.core.UpdateChecker

class AboutViewModel : ViewModel() {
    fun checkForUpdates(context: Context) {
        UpdateChecker.run(context)
    }
}
