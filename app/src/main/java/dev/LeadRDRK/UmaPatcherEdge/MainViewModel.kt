package dev.LeadRDRK.UmaPatcherEdge

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
import dev.LeadRDRK.UmaPatcherEdge.core.UpdateChecker
import dev.LeadRDRK.UmaPatcherEdge.utils.deleteRecursive
import dev.LeadRDRK.UmaPatcherEdge.utils.repoDir
import dev.LeadRDRK.UmaPatcherEdge.utils.workDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val rootInitialized = mutableStateOf(false)
    val isRooted = mutableStateOf(false)
    val openUpdateDialog = mutableStateOf(false)
    val updateTagName = mutableStateOf("")

    fun init(context: Context) {
        GameChecker.init(context.packageManager)

        // Init work directory
        context.workDir.mkdir()
        deleteRecursive(context.workDir, deleteRoot = false)

        // Remove legacy repo directory (if it exists)
        deleteRecursive(context.repoDir, deleteRoot = true)

        // Request root permissions
        Shell.getShell { shell ->
            isRooted.value = shell.isRoot
            rootInitialized.value = true
        }

        UpdateChecker.init(context)
        UpdateChecker.callback = { name ->
            updateTagName.value = name
            openUpdateDialog.value = true
        }
    }
}
