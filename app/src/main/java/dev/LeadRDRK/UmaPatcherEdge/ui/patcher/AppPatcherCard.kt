package dev.LeadRDRK.UmaPatcherEdge.ui.patcher

import android.content.Context
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.MainActivity
import dev.LeadRDRK.UmaPatcherEdge.MainViewModel
import dev.LeadRDRK.UmaPatcherEdge.core.PrefKey
import dev.LeadRDRK.UmaPatcherEdge.core.dataStore
import dev.LeadRDRK.UmaPatcherEdge.core.getPrefValue
import dev.LeadRDRK.UmaPatcherEdge.patcher.AppPatcher
import dev.LeadRDRK.UmaPatcherEdge.shizuku.ShizukuState
import dev.LeadRDRK.UmaPatcherEdge.ui.component.RadioGroupOption
import dev.LeadRDRK.UmaPatcherEdge.ui.component.SimpleOkCancelDialog
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import com.topjohnwu.superuser.Shell

private enum class InstallMethod {
    SAVE,
    NORMAL,
    DIRECT,
    SHIZUKU,
    LEGACY
}

@Composable
fun AppPatcherCard(navigator: DestinationsNavigator) {
    var showShizukuRationaleDialog by remember { mutableStateOf(false) }
    var showShizukuNotAvailableDialog by remember { mutableStateOf(false) }
    var staleFilesError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mainViewModel: MainViewModel = viewModel(context as MainActivity)
    val isRootAvailable = mainViewModel.isRooted.value
    val isShizukuAvailable by ShizukuState.isAvailable

    val availableMethods = remember(isRootAvailable) {
        mutableListOf(InstallMethod.SAVE, InstallMethod.NORMAL).apply {
            if (isRootAvailable) add(InstallMethod.DIRECT)
            add(InstallMethod.SHIZUKU)
            add(InstallMethod.LEGACY)
        }
    }

    val selectedMethodIndex = rememberSaveable { mutableIntStateOf(1) }
    val currentMethod = availableMethods.getOrElse(selectedMethodIndex.intValue) { InstallMethod.NORMAL }

    var fileUris by rememberSaveable { mutableStateOf<Array<Uri>>(arrayOf()) }
    var stateLoaded by remember { mutableStateOf(false) }
    var installMethodLoaded by rememberSaveable { mutableStateOf(false) }

    val fileSelectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val newFileUris = uris.toTypedArray()
        for (uri in fileUris) releasePersistableUriPermission(context, uri)
        for (uri in newFileUris) tryTakePersistableUriPermission(context, uri)
        fileUris = newFileUris
        coroutineScope.launch { saveFileUris(context, newFileUris) }
    }

    // Restore the persisted file URIs and install method from preferences
    LaunchedEffect(true) {
        if (!installMethodLoaded) {
            val savedInstallMethod = context.getPrefValue(PrefKey.INSTALL_METHOD) as Int
            if (selectedMethodIndex.intValue == 1) selectedMethodIndex.intValue = savedInstallMethod
            installMethodLoaded = true
        }

        if (fileUris.isEmpty()) {
            val savedFileUris = (context.getPrefValue(PrefKey.FILE_URIS) as String)
                .split('\n')
                .filter { it.isNotEmpty() }
                .map { uri -> Uri.parse(uri) }
            val existingFileUris = mutableListOf<Uri>()
            for (uri in savedFileUris) {
                if (canOpenUri(context, uri)) {
                    existingFileUris.add(uri)
                } else {
                    releasePersistableUriPermission(context, uri)
                }
            }
            if (existingFileUris.size != savedFileUris.size)
                saveFileUris(context, existingFileUris.toTypedArray())
            if (existingFileUris.isNotEmpty())
                fileUris = existingFileUris.toTypedArray()
        }

        stateLoaded = true
    }

    // Persist install method changes (debounced)
    LaunchedEffect(installMethodLoaded) {
        if (!installMethodLoaded) return@LaunchedEffect
        snapshotFlow { selectedMethodIndex.intValue }
            .drop(1)
            .collectLatest { method ->
                saveInstallMethod(context, method)
            }
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
                        shizukuInstall = true,
                        legacyInstall = false
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

    if (staleFilesError != null) {
        SimpleOkCancelDialog(
            title = stringResource(R.string.patch),
            onClose = { staleFilesError = null }
        ) {
            Text(staleFilesError!!)
        }
    }

    // umapatcher-edge://update-hachimi deeplink: start patching with last selected files/method
    val pendingUpdateDeepLink by mainViewModel.pendingUpdateDeepLink
    LaunchedEffect(pendingUpdateDeepLink) {
        if (!pendingUpdateDeepLink) return@LaunchedEffect
        mainViewModel.pendingUpdateDeepLink.value = false

        val needsFiles = currentMethod != InstallMethod.DIRECT
        if (needsFiles && fileUris.isEmpty()) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.deep_link_missing_apks),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return@LaunchedEffect
        }

        PatcherLauncher.launch(
            navigator,
            AppPatcher(
                fileUris = if (needsFiles) fileUris else arrayOf(),
                install = true,
                directInstall = currentMethod == InstallMethod.DIRECT,
                shizukuInstall = currentMethod == InstallMethod.SHIZUKU,
                legacyInstall = currentMethod == InstallMethod.LEGACY
            )
        )
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
                    // Drop stale URIs whose grants were revoked before patching
                    if (currentMethod != InstallMethod.DIRECT && fileUris.isNotEmpty() && stateLoaded) {
                        val (aliveUris, deadUris) = fileUris.partition { canOpenUri(context, it) }
                        if (deadUris.isNotEmpty()) {
                            for (uri in deadUris) releasePersistableUriPermission(context, uri)
                            fileUris = aliveUris.toTypedArray()
                            coroutineScope.launch { saveFileUris(context, aliveUris.toTypedArray()) }
                            staleFilesError = context.getString(R.string.selected_files_no_longer_available)
                            return@Button
                        }
                    }

                    if(!isShizukuAvailable && isShizukuOptionSelected) {
                        showShizukuNotAvailableDialog = true
                        return@Button
                    }

                    if(isShizukuOptionSelected) {
                        if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                            PatcherLauncher.launch(
                                navigator,
                                AppPatcher(fileUris, install = true, directInstall = false, shizukuInstall = true, legacyInstall = false)
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
                                shizukuInstall = false,
                                legacyInstall = currentMethod == InstallMethod.LEGACY
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
                    InstallMethod.LEGACY -> stringResource(R.string.legacy_install)
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

private fun canOpenUri(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.read() } != null
    } catch (_: Exception) {
        false
    }
}

private fun tryTakePersistableUriPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // No persistable grant was offered for this URI
    }
}

private fun releasePersistableUriPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // No persisted permission was held for this URI
    }
}

private suspend fun saveFileUris(context: Context, fileUris: Array<Uri>) {
    context.dataStore.edit {
        it[PrefKey.FILE_URIS] = fileUris.joinToString("\n") { uri -> uri.toString() }
    }
}

private suspend fun saveInstallMethod(context: Context, installMethod: Int) {
    context.dataStore.edit {
        it[PrefKey.INSTALL_METHOD] = installMethod
    }
}
