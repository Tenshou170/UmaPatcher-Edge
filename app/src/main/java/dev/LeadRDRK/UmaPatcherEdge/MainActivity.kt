package dev.LeadRDRK.UmaPatcherEdge

import android.os.Bundle
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import dev.LeadRDRK.UmaPatcherEdge.core.UpdateChecker
import dev.LeadRDRK.UmaPatcherEdge.ui.component.SimpleOkCancelDialog
import dev.LeadRDRK.UmaPatcherEdge.ui.screen.BottomBarDestination
import dev.LeadRDRK.UmaPatcherEdge.ui.screen.NavGraphs
import dev.LeadRDRK.UmaPatcherEdge.ui.screen.destinations.PatchingScreenDestination
import dev.LeadRDRK.UmaPatcherEdge.ui.theme.UmaPatcherTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import rikka.shizuku.Shizuku
import com.topjohnwu.superuser.Shell

class MainActivity : ComponentActivity() {
    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 9975
        var onShizukuPermissionResult: ((grantResult: Int) -> Unit)? = null
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
            )
        }
    }

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                onShizukuPermissionResult?.invoke(grantResult)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            
            LaunchedEffect(Unit) {
                mainViewModel.init(this@MainActivity)
            }

            UmaPatcherTheme {
                MainContent(mainViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        onShizukuPermissionResult = null
    }

    suspend fun useKeepScreenOn(callback: suspend () -> Unit) {
        withContext(Dispatchers.Main) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        try {
            callback()
        } finally {
            withContext(Dispatchers.Main) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun MainContent(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val navHostEngine = rememberAnimatedNavHostEngine(
        rootDefaultAnimations = navAnimations
    )

    val bottomBarState = rememberSaveable { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    when (navBackStackEntry?.destination?.route) {
        PatchingScreenDestination.route -> bottomBarState.value = false
        else -> bottomBarState.value = true
    }

    val uriHandler = LocalUriHandler.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomBar(navController, bottomBarState) }
    ) {
        if (viewModel.rootInitialized.value) {
            DestinationsNavHost(
                modifier = Modifier,
                navGraph = NavGraphs.root,
                navController = navController,
                engine = navHostEngine
            )
        }
        if (viewModel.openUpdateDialog.value) {
            SimpleOkCancelDialog(
                title = stringResource(R.string.update_available),
                onClose = { ok ->
                    viewModel.openUpdateDialog.value = false
                    if (ok) uriHandler.openUri(UpdateChecker.getReleaseUrl(viewModel.updateTagName.value))
                }
            ) {
                Text(
                    stringResource(R.string.update_available_desc).format(viewModel.updateTagName.value)
                )
            }
        }
    }
}

private val navAnimations = RootNavGraphDefaultAnimations(
    enterTransition = {
        scaleIn(
            animationSpec = tween(200),
            initialScale = 0.9f
        ) + fadeIn(
            animationSpec = tween(200)
        )
    },
    exitTransition = {
        scaleOut(
            animationSpec = tween(200),
            targetScale = 0.9f
        ) + fadeOut(
            animationSpec = tween(200)
        )
    },
    popExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
            animationSpec = tween(200)
        )
    }
)

@Composable
private fun BottomBar(navController: NavHostController, bottomBarState: MutableState<Boolean>) {
    AnimatedVisibility(
        visible = bottomBarState.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NavigationBar(tonalElevation = 8.dp) {
            BottomBarDestination.entries.forEach { destination ->
                val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                NavigationBarItem(
                    selected = isCurrentDestOnBackStack,
                    onClick = {
                        if (isCurrentDestOnBackStack) {
                            navController.popBackStack(destination.direction, false)
                        }

                        navController.navigate(destination.direction.route) {
                            popUpTo(NavGraphs.root.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        if (isCurrentDestOnBackStack) {
                            Icon(destination.iconSelected, stringResource(destination.label))
                        } else {
                            Icon(destination.iconNotSelected, stringResource(destination.label))
                        }
                    },
                    label = { Text(stringResource(destination.label)) },
                    alwaysShowLabel = false
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainContent()
}
