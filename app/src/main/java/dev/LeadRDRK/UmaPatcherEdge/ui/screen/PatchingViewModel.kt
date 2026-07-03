package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.ui.patcher.PatcherLauncher
import dev.LeadRDRK.UmaPatcherEdge.utils.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PatchingViewModel : ViewModel() {
    companion object {
        private const val MAX_LOG_LINES = 200
    }

    val log = mutableStateListOf<String>()
    var currentTask by mutableStateOf("")
    var progress by mutableFloatStateOf(-1f)
    var completed by mutableStateOf(false)
    var isSuccess by mutableStateOf(false)
    
    var sfFile by mutableStateOf<File?>(null)
    var sfCallback: (Boolean) -> Unit = {}

    fun init(context: Context, patchSuccessMsg: String, patchFailedMsg: String, patchCancelledMsg: String) {
        if (PatcherLauncher.patching) return
        
        val workingStr = context.getString(R.string.working)
        if (currentTask.isEmpty()) currentTask = workingStr

        val patcher = PatcherLauncher.patcher ?: return
        patcher.setCallbacks(
            onLog = { line ->
                log.add(line)
                if (log.size > MAX_LOG_LINES)
                    log.removeRange(0, log.size - MAX_LOG_LINES)
            },
            onProgress = { p -> progress = p },
            onTask = { task ->
                currentTask = task
                log.add("-- $task")
            },
            onSaveFile = { filename, file, callback ->
                sfFile = file
                sfCallback = callback
                // This will be handled by the UI layer as it needs to launch an Intent
            }
        )

        viewModelScope.launch {
            PatcherLauncher.runPatcher(context) { success ->
                completed = true
                isSuccess = success && !patcher.isCancelled
                if (patcher.isCancelled) {
                    log.add(patchCancelledMsg)
                    progress = 1f
                } else {
                    log.add(if (success) patchSuccessMsg else patchFailedMsg)
                    progress = 1f
                }
                currentTask = context.getString(R.string.completed)
            }
        }
    }

    fun handleSaveFileResult(context: Context, uri: Uri?, completedStr: String) {
        if (uri == null) {
            sfCallback(false)
            sfFile = null
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri).use { output ->
                if (output == null) {
                    sfCallback(false)
                    sfFile = null
                    return@launch
                }

                val file = sfFile!!
                val length = file.length().toFloat()
                
                // We need to update UI state from IO thread safely if using mutableStateOf, 
                // but Compose handles it if it's not a background thread... 
                // actually it's better to switch back to Main for state updates.
                
                viewModelScope.launch(Dispatchers.Main) {
                    currentTask = context.getString(R.string.copying_file_name).format(file.name)
                    progress = 0f
                }
                
                file.inputStream().use { input ->
                    input.copyTo(output) { current ->
                        progress = current / length
                    }
                }
            }
            
            viewModelScope.launch(Dispatchers.Main) {
                sfCallback(true)
                currentTask = completedStr
                sfFile = null
            }
        }
    }
}
