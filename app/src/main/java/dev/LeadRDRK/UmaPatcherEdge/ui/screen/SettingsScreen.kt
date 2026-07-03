package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.core.defaultValues
import dev.LeadRDRK.UmaPatcherEdge.ui.component.BooleanOption
import dev.LeadRDRK.UmaPatcherEdge.ui.component.BottomBarScrollSpacer
import dev.LeadRDRK.UmaPatcherEdge.ui.component.OptionBase
import dev.LeadRDRK.UmaPatcherEdge.ui.component.StringOption
import dev.LeadRDRK.UmaPatcherEdge.ui.component.TopBar
import dev.LeadRDRK.UmaPatcherEdge.utils.ksFile
import dev.LeadRDRK.UmaPatcherEdge.utils.showToast
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    val exportKsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data ?: return@rememberLauncherForActivityResult
        viewModel.exportKeystore(context, uri)
    }

    val importKsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data ?: return@rememberLauncherForActivityResult
        viewModel.importKeystore(context, uri)
    }

    LaunchedEffect(viewModel.appLibsVersion) {
        if (!viewModel.configRead) return@LaunchedEffect
        viewModel.saveAppLibsVersion(context, viewModel.appLibsVersion)
    }

    LaunchedEffect(Unit) {
        viewModel.loadConfig(context)
    }

    Scaffold(
        topBar = {
            TopBar(stringResource(R.string.settings))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (!viewModel.configRead) return@Column

            BooleanOption(
                title = stringResource(R.string.check_for_updates),
                desc = stringResource(R.string.check_for_updates_desc),
                value = viewModel.checkForUpdates,
                onCheckedChange = { viewModel.updateCheckForUpdates(context, it) }
            )

            StringOption(
                title = stringResource(R.string.hachimi_repo),
                value = viewModel.hachimiRepo,
                placeholder = "LeadRDRK/Hachimi-Edge", // Updated default placeholder if needed
                onValueChange = { viewModel.updateHachimiRepo(context, it) }
            )

            OptionBase(
                title = stringResource(R.string.export_signing_key),
                desc = stringResource(R.string.export_signing_key_desc),
                onClick = {
                    if (context.ksFile.exists()) {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_TITLE, "UmaPatcher.bks")
                            }
                        exportKsLauncher.launch(intent)
                    }
                    else {
                        context.showToast(
                            context.getString(R.string.no_keystore_to_export),
                            Toast.LENGTH_SHORT
                        )
                    }
                }
            ) {
            }

            OptionBase(
                title = stringResource(R.string.import_signing_key),
                desc = stringResource(R.string.import_signing_key_desc),
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                    importKsLauncher.launch(intent)
                }
            ) {
            }

            OptionBase(
                title = stringResource(R.string.force_redownload_mod),
                desc = stringResource(R.string.force_redownload_mod_desc),
                onClick = {
                    context.showToast(
                        context.getString(R.string.force_redownload_mod_notice),
                        Toast.LENGTH_SHORT
                    )
                    viewModel.forceRedownloadMod()
                }
            ) {
            }

            BottomBarScrollSpacer()
        }
    }
}
