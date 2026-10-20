package dev.gloria.apkextractor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.gloria.apkextractor.apps.AppListScreen
import dev.gloria.apkextractor.apps.AppListViewModel
import dev.gloria.apkextractor.apps.PackageManagerAppRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = viewModelFactory {
            initializer { AppListViewModel(PackageManagerAppRepository(packageManager)) }
        }
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppListScreen(viewModel(factory = factory), Modifier.safeDrawingPadding())
                }
            }
        }
    }
}
