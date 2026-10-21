package dev.gloria.apkextractor.apps

import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.gloria.apkextractor.R
import dev.gloria.apkextractor.extract.ExtractHost
import dev.gloria.apkextractor.extract.ExtractWorker
import android.content.pm.PackageManager as Pm
import java.util.UUID

@Composable
fun AppListScreen(viewModel: AppListViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val workId by viewModel.activeExtraction.collectAsStateWithLifecycle()
    var pendingPackage by rememberSaveable { mutableStateOf<String?>(null) }

    fun startExtraction(packageName: String) {
        val request = OneTimeWorkRequestBuilder<ExtractWorker>()
            .setInputData(workDataOf(ExtractWorker.KEY_PACKAGE to packageName))
            .build()
        WorkManager.getInstance(context).enqueue(request)
        viewModel.setActiveExtraction(request.id.toString())
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingPackage?.let(::startExtraction)
        pendingPackage = null
    }

    fun onAppClick(app: InstalledApp) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != Pm.PERMISSION_GRANTED
        if (needsPermission) {
            pendingPackage = app.packageName
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startExtraction(app.packageName)
        }
    }

    workId?.let { id -> ExtractHost(UUID.fromString(id), onDismiss = { viewModel.setActiveExtraction(null) }) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            label = { Text(stringResource(R.string.search_apps)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.show_system_apps))
            Switch(checked = state.showSystem, onCheckedChange = viewModel::setShowSystem)
        }
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn {
                items(state.apps, key = { it.packageName }) { app -> AppRow(app, onClick = { onAppClick(app) }) }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    val packageManager = LocalContext.current.packageManager
    val icon = remember(app.packageName) { loadIcon(packageManager, app.packageName) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(40.dp)) }
        Column {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun loadIcon(packageManager: PackageManager, packageName: String) =
    runCatching { packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
