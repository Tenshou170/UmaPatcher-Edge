package dev.LeadRDRK.UmaPatcherEdge.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.LeadRDRK.UmaPatcherEdge.utils.safeNavigate
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun BackButton(navigator: DestinationsNavigator, enabled: Boolean = true) {
    val lifecycleOwner = LocalLifecycleOwner.current
    IconButton(
        onClick = {
            safeNavigate(lifecycleOwner) {
                navigator.popBackStack()
            }
        },
        enabled = enabled
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
    }
}