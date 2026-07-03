package dev.LeadRDRK.UmaPatcherEdge.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.LeadRDRK.UmaPatcherEdge.R
import dev.LeadRDRK.UmaPatcherEdge.core.GameChecker
import dev.LeadRDRK.UmaPatcherEdge.ui.component.BackButton
import dev.LeadRDRK.UmaPatcherEdge.ui.component.BottomBarScrollSpacer
import dev.LeadRDRK.UmaPatcherEdge.ui.component.TopBar
import dev.LeadRDRK.UmaPatcherEdge.utils.safeNavigate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun AppSelectScreen(
    navigator: DestinationsNavigator,
    viewModel: AppSelectViewModel = viewModel()
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.loadApps(pm)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.select_an_app),
                navigationIcon = { BackButton(navigator) }
            )
        }
    ) { innerPadding ->
        val apps by viewModel.apps.collectAsState()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            for (packageInfo in apps) {
                val appInfo = pm.getApplicationInfo(packageInfo.packageName, 0)
                AppEntry(
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    packageName = packageInfo.packageName,
                    version = packageInfo.versionName ?: "Unknown",
                    icon = appInfo.loadIcon(pm).toBitmap().asImageBitmap(),
                    onClick = {
                        GameChecker.currentPackageName = packageInfo.packageName
                        safeNavigate(lifecycleOwner) {
                            navigator.popBackStack()
                        }
                    }
                )
            }
            BottomBarScrollSpacer()
        }
    }
}

@Composable
fun AppEntry(
    appName: String,
    packageName: String,
    version: String,
    icon: ImageBitmap,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.package_name_prefix) + packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.version_name_prefix) + version,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()
    }
}
