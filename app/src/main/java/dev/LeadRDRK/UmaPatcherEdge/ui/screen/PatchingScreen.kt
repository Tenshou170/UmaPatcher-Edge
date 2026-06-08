package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.ui.component.BackButton
import dev.LeadRDRK.UmaPatcherEdge.ui.component.TopBar
import dev.LeadRDRK.UmaPatcherEdge.ui.component.bottomControlsPadding
import dev.LeadRDRK.UmaPatcherEdge.ui.patcher.PatcherLauncher
import dev.LeadRDRK.UmaPatcherEdge.utils.copyTo
import dev.LeadRDRK.UmaPatcherEdge.utils.safeNavigate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_LOG_LINES = 200

@Destination
@Composable
fun PatchingScreen(navigator: DestinationsNavigator) {
    val workingStr = stringResource(R.string.working)
    val completedStr = stringResource(R.string.completed)

    val log = remember { mutableStateListOf<String>() }
    var currentTask by remember { mutableStateOf(workingStr) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var completed by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Patcher callbacks
    fun onLog(line: String) {
        log.add(line)
        if (log.size > MAX_LOG_LINES)
            log.removeRange(0, log.size - MAX_LOG_LINES)
    }
    fun onProgress(p: Float) { progress = p }
    fun onTask(task: String) {
        currentTask = task
        log.add("-- $task")
    }

    val coroutineScope = rememberCoroutineScope()
    var sfFile by remember { mutableStateOf<File?>(null) }
    var sfCallback: (Boolean) -> Unit by remember { mutableStateOf({}) }
    val sfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data
        if (uri == null) {
            sfCallback(false)
            sfFile = null
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri).use { output ->
                if (output == null) {
                    sfCallback(false)
                    sfFile = null
                    return@launch
                }

                val file = sfFile!!
                val length = file.length().toFloat()
                onTask(context.getString(R.string.copying_file_name).format(file.name))
                progress = 0f
                file.inputStream().use { input ->
                    input.copyTo(output) { current ->
                        progress = current / length
                    }
                }
            }
            sfCallback(true)
            currentTask = completedStr
            sfFile = null
        }
    }

    fun onSaveFile(filename: String, file: File, callback: (Boolean) -> Unit = {}) {
        sfFile = file
        sfCallback = callback
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_TITLE, filename)
            }
        sfLauncher.launch(intent)
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(log.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler {
        if (completed && sfFile == null) {
            safeNavigate(lifecycleOwner) {
                navigator.popBackStack()
            }
        }
    }

    val patchSuccessMsg = stringResource(R.string.patch_success_msg)
    val patchFailedMsg = stringResource(R.string.patch_failed_msg)

    val patchCancelledMsg = stringResource(R.string.patching_cancelled_by_user)

    LaunchedEffect(true) {
        if (PatcherLauncher.patching) return@LaunchedEffect
        val patcher = PatcherLauncher.patcher!!
        patcher.setCallbacks(::onLog, ::onProgress, ::onTask, ::onSaveFile)
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
        }
    }

    LaunchedEffect(completed) {
        if (completed) {
            currentTask = completedStr
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (completed) completedStr else workingStr,
                navigationIcon = { BackButton(navigator, enabled = completed && sfFile == null) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            TextField(
                value = log.joinToString("\n"),
                onValueChange = {},
                readOnly = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = TextUnit(1.4f, TextUnitType.Em)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Unspecified,
                    unfocusedIndicatorColor = Color.Unspecified
                )
            )
            Column(
                modifier = Modifier
                    .bottomControlsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = currentTask,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (progress < 0) "¯\\_(ツ)_/¯" else "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (progress < 0) {
                    // Indeterminate
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!completed) {
                        ElevatedButton(
                            onClick = {
                                PatcherLauncher.cancelPatcher()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = stringResource(R.string.stop_patching_and_cleanup))
                        }
                    } else if (sfFile == null) {
                        ElevatedButton(
                            onClick = {
                                val intent = context.packageManager.getLaunchIntentForPackage("jp.co.cygames.umamusume")
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    android.widget.Toast.makeText(context, R.string.game_not_installed, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isSuccess
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = stringResource(R.string.launch_game))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
