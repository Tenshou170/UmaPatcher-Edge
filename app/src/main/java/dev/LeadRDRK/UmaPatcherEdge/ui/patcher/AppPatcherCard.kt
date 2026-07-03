package dev.LeadRDRK.UmaPatcherEdge.ui.patcher

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.MainActivity
import dev.LeadRDRK.UmaPatcherEdge.MainViewModel
import dev.LeadRDRK.UmaPatcherEdge.patcher.AppPatcher
import dev.LeadRDRK.UmaPatcherEdge.shizuku.ShizukuState
import dev.LeadRDRK.UmaPatcherEdge.ui.component.RadioGroupOption
import dev.LeadRDRK.UmaPatcherEdge.ui.component.SimpleOkCancelDialog
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import rikka.shizuku.Shizuku
import com.topjohnwu.superuser.Shell

private enum class InstallMethod {
    SAVE,
    NORMAL,
    DIRECT,
    SHIZUKU
}

@Composable
fun AppPatcherCard(navigator: DestinationsNavigator) {
    var showShizukuRationaleDialog by remember { mutableStateOf(false) }
    var showShizukuNotAvailableDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel(context as MainActivity)
    val isRootAvailable = mainViewModel.isRooted.value
    val isShizukuAvailable by ShizukuState.isAvailable

    val availableMethods = remember(isRootAvailable) {
        mutableListOf(InstallMethod.SAVE, InstallMethod.NORMAL).apply {
            if (isRootAvailable) add(InstallMethod.DIRECT)
            add(InstallMethod.SHIZUKU)
        }
    }

    val selectedMethodIndex = rememberSaveable { mutableIntStateOf(1) }
    val currentMethod = availableMethods.getOrElse(selectedMethodIndex.intValue) { InstallMethod.NORMAL }

    var fileUris by rememberSaveable { mutableStateOf<Array<Uri>>(arrayOf()) }
    val fileSelectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        fileUris = uris.toTypedArray()
    }

    LaunchedEffect(navigator, currentMethod, fileUris) {
        MainActivity.onShizukuPermissionResult = { grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                PatcherLauncher.launch(
                    navigator,
                    AppPatcher(
                        fileUris = fileUris,
                        install = true,
                        directInstall = false,
                        shizukuInstall = true
                    )
                )
            }
        }
    }

    if(showShizukuRationaleDialog) {
        SimpleOkCancelDialog(
            title = stringResource(R.string.shizuku_permission_required),
            onClose = { ok ->
                showShizukuRationaleDialog = false
                if (ok) {
                    Shizuku.requestPermission(MainActivity.SHIZUKU_PERMISSION_REQUEST_CODE)
                }
            }
        ) {
            Text(stringResource(R.string.shizuku_permission_required))
        }
    }

    val uriHandler = LocalUriHandler.current
    if(showShizukuNotAvailableDialog) {
        SimpleOkCancelDialog(
            title = stringResource(R.string.shizuku_unavailable),
            onClose = { ok ->
                showShizukuNotAvailableDialog = false
                if (ok) {
                    uriHandler.openUri("https://shizuku.rikka.app/download")
                }
            }
        ) {
            Text(stringResource(R.string.shizuku_unavailable_info))
        }
    }

    PatcherCard(
        label = stringResource(R.string.app_patcher_label),
        icon = { Icon(painterResource(R.drawable.ic_apk_install), null) },
        buttons = {
            val isShizukuOptionSelected = currentMethod == InstallMethod.SHIZUKU
            val isButtonEnabled = when (currentMethod) {
                InstallMethod.DIRECT -> true
                else -> fileUris.isNotEmpty()
            }

            Button(
                enabled = isButtonEnabled,
                onClick = {
                    if(!isShizukuAvailable && isShizukuOptionSelected) {
                        showShizukuNotAvailableDialog = true
                        return@Button
                    }

                    if(isShizukuOptionSelected) {
                        if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                            PatcherLauncher.launch(
                                navigator,
                                AppPatcher(fileUris, install = true, directInstall = false, shizukuInstall = true)
                            )
                        }else if (Shizuku.shouldShowRequestPermissionRationale()) {
                            showShizukuRationaleDialog = true
                        }else {
                            Shizuku.requestPermission(MainActivity.SHIZUKU_PERMISSION_REQUEST_CODE)
                        }
                    }else {
                        PatcherLauncher.launch(
                            navigator,
                            AppPatcher(
                                fileUris = if (currentMethod == InstallMethod.DIRECT) arrayOf() else fileUris,
                                install = currentMethod == InstallMethod.NORMAL,
                                directInstall = currentMethod == InstallMethod.DIRECT,
                                shizukuInstall = false
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.patch))
            }
        }
    ) {
        val shizukuStatusText = if (isShizukuAvailable) stringResource(R.string.shizuku_install_available) else stringResource(R.string.shizuku_install_unavailable)
        val shizukuStatusColor = if (isShizukuAvailable) Color(0xFF388E3C) else MaterialTheme.colorScheme.error

        RadioGroupOption(
            title = stringResource(R.string.install_method),
            desc = stringResource(R.string.install_method_desc),
            choices = availableMethods.map { method ->
                when (method) {
                    InstallMethod.SAVE -> stringResource(R.string.save_patched_file)
                    InstallMethod.NORMAL -> stringResource(R.string.normal_install)
                    InstallMethod.DIRECT -> stringResource(R.string.direct_install)
                    InstallMethod.SHIZUKU -> stringResource(R.string.shizuku_install)
                }
            }.toTypedArray(),
            state = selectedMethodIndex,
            choiceContent = { index, text ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if(availableMethods[index] == InstallMethod.SHIZUKU) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = shizukuStatusText,
                            color = shizukuStatusColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        )
        if (currentMethod != InstallMethod.DIRECT) {
            Spacer(Modifier.height(16.dp))
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .clickable {
                        fileSelectLauncher.launch(arrayOf("*/*"))
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_file_open), null)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.tap_to_select_file),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.n_files_selected).format(fileUris.size),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.app_patcher_supported_files),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
