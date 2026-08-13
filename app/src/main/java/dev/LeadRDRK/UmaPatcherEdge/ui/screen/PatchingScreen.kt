package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Intent
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.ui.component.BackButton
import dev.LeadRDRK.UmaPatcherEdge.ui.component.TopBar
import dev.LeadRDRK.UmaPatcherEdge.ui.component.bottomControlsPadding
import dev.LeadRDRK.UmaPatcherEdge.ui.patcher.PatcherLauncher
import dev.LeadRDRK.UmaPatcherEdge.utils.copyTo
import dev.LeadRDRK.UmaPatcherEdge.utils.safeNavigate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.io.File

@Destination
@Composable
fun PatchingScreen(
    navigator: DestinationsNavigator,
    viewModel: PatchingViewModel = viewModel()
) {
    val workingStr = stringResource(R.string.working)
    val completedStr = stringResource(R.string.completed)

    val context = LocalContext.current

    val patchSuccessMsg = stringResource(R.string.patch_success_msg)
    val patchFailedMsg = stringResource(R.string.patch_failed_msg)
    val patchCancelledMsg = stringResource(R.string.patching_cancelled_by_user)

    val sfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.handleSaveFileResult(context, it.data?.data, completedStr)
    }

    LaunchedEffect(viewModel.sfFile) {
        val file = viewModel.sfFile
        if (file != null) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                }
            sfLauncher.launch(intent)
        }
    }

    val lazyListState = rememberLazyListState()
    LaunchedEffect(viewModel.log.size) {
        if (viewModel.log.isNotEmpty()) {
            lazyListState.animateScrollToItem(viewModel.log.size - 1)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler {
        if (viewModel.completed && viewModel.sfFile == null) {
            safeNavigate(lifecycleOwner) {
                navigator.popBackStack()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.init(context, patchSuccessMsg, patchFailedMsg, patchCancelledMsg)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (viewModel.completed) completedStr else workingStr,
                navigationIcon = { BackButton(navigator, enabled = viewModel.completed && viewModel.sfFile == null) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(viewModel.log) { line ->
                    Text(
                        text = line,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = TextUnit(1.4f, TextUnitType.Em),
                            fontSize = TextUnit(12f, TextUnitType.Sp)
                        )
                    )
                }
            }
            Column(
                modifier = Modifier
                    .bottomControlsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.currentTask,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (viewModel.progress < 0) "¯\\_(ツ)_/¯" else "${(viewModel.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (viewModel.progress < 0) {
                    // Indeterminate
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else {
                    LinearProgressIndicator(
                        progress = { viewModel.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!viewModel.completed) {
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
                    } else if (viewModel.sfFile == null) {
                        ElevatedButton(
                            onClick = {
                                val targetPackage = GameChecker.currentPackageName ?: "jp.co.cygames.umamusume"
                                val intent = context.packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    android.widget.Toast.makeText(context, R.string.game_not_installed, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.isSuccess
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
