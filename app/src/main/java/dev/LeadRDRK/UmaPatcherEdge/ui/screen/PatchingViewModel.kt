package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Context
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val logChannel = Channel<String>(Channel.UNLIMITED)

    init {
        viewModelScope.launch(Dispatchers.Main) {
            logChannel.receiveAsFlow().collect { line ->
                log.add(line)
                while (log.size > MAX_LOG_LINES) {
                    log.removeAt(0)
                }
            }
        }
    }

    fun init(context: Context, patchSuccessMsg: String, patchFailedMsg: String, patchCancelledMsg: String) {
        if (completed || PatcherLauncher.patching) return
        
        val workingStr = context.getString(R.string.working)
        if (currentTask.isEmpty()) currentTask = workingStr

        val patcher = PatcherLauncher.patcher ?: return
        
        patcher.setCallbacks(
            onLog = { line -> logChannel.trySend(line) },
            onProgress = { p ->
                viewModelScope.launch(Dispatchers.Main) {
                    progress = p
                }
            },
            onTask = { task ->
                viewModelScope.launch(Dispatchers.Main) {
                    currentTask = task
                }
                logChannel.trySend("-- $task")
            },
            onSaveFile = { _, file, callback ->
                viewModelScope.launch(Dispatchers.Main) {
                    sfFile = file
                    sfCallback = callback
                }
            }
        )

        viewModelScope.launch {
            PatcherLauncher.runPatcher(context) { success ->
                viewModelScope.launch(Dispatchers.Main) {
                    completed = true
                    isSuccess = success && !patcher.isCancelled
                    if (patcher.isCancelled) {
                        logChannel.trySend(patchCancelledMsg)
                        progress = 1f
                    } else {
                        logChannel.trySend(if (success) patchSuccessMsg else patchFailedMsg)
                        progress = 1f
                    }
                    currentTask = context.getString(R.string.completed)
                }
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
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    val file = sfFile ?: return@launch
                    val length = file.length().toFloat()
                    
                    viewModelScope.launch(Dispatchers.Main) {
                        currentTask = context.getString(R.string.copying_file_name).format(file.name)
                        progress = 0f
                    }
                    
                    file.inputStream().use { input ->
                        input.copyTo(output) { current ->
                            viewModelScope.launch(Dispatchers.Main) {
                                progress = current / length
                            }
                        }
                    }
                }
                
                viewModelScope.launch(Dispatchers.Main) {
                    sfCallback(true)
                    currentTask = completedStr
                    sfFile = null
                }
            } catch (e: Exception) {
                Log.e("PatchingViewModel", "Failed to save file", e)
                viewModelScope.launch(Dispatchers.Main) {
                    sfCallback(false)
                    sfFile = null
                }
            }
        }
    }
}
