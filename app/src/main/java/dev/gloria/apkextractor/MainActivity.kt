package dev.gloria.apkextractor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.gloria.apkextractor.apps.AppListScreen
import dev.gloria.apkextractor.apps.AppListViewModel
import dev.gloria.apkextractor.apps.PackageManagerAppRepository
import dev.gloria.apkextractor.install.ApkInstaller
import dev.gloria.apkextractor.install.InstallScreen
import dev.gloria.apkextractor.install.InstallViewModel

class MainActivity : ComponentActivity() {

    private val appListViewModel: AppListViewModel by viewModels {
        viewModelFactory { initializer { AppListViewModel(PackageManagerAppRepository(packageManager)) } }
    }

    private val installViewModel: InstallViewModel by viewModels {
        viewModelFactory { initializer { InstallViewModel(contentResolver, ApkInstaller(applicationContext)) } }
    }

    private var openedWithFile by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (savedInstanceState == null) handleViewIntent(intent)
        setContent {
            MaterialTheme {
                var tab by rememberSaveable { mutableIntStateOf(if (openedWithFile > 0) 1 else 0) }
                if (openedWithFile > 0) tab = 1
                Surface(Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = tab == 0,
                                    onClick = { tab = 0; openedWithFile = 0 },
                                    icon = { Icon(painterResource(android.R.drawable.ic_menu_agenda), null) },
                                    label = { Text(stringResource(R.string.tab_apps)) },
                                )
                                NavigationBarItem(
                                    selected = tab == 1,
                                    onClick = { tab = 1 },
                                    icon = { Icon(painterResource(android.R.drawable.ic_menu_add), null) },
                                    label = { Text(stringResource(R.string.tab_install)) },
                                )
                            }
                        },
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            if (tab == 0) AppListScreen(appListViewModel) else InstallScreen(installViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent) {
        val uri = intent.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        installViewModel.onFilePicked(uri)
        openedWithFile++
    }
}
