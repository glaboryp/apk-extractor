package dev.gloria.apkextractor.install

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gloria.apkextractor.R

@Composable
fun InstallScreen(viewModel: InstallViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(viewModel.canInstall()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = viewModel.canInstall() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onFilePicked)
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!allowed) {
            Text(stringResource(R.string.install_permission_needed), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()),
                )
            }) { Text(stringResource(R.string.install_grant_permission)) }
            return@Column
        }

        when (val current = state) {
            InstallState.Idle -> {
                Text(stringResource(R.string.install_intro), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { picker.launch("*/*") }) { Text(stringResource(R.string.install_pick_file)) }
            }
            InstallState.Reading -> CircularProgressIndicator()
            InstallState.Installing -> {
                CircularProgressIndicator()
                Button(onClick = viewModel::cancelInstall) { Text(stringResource(R.string.cancel)) }
            }
            is InstallState.Ready -> {
                val count = current.contents.entries.size
                Text(current.fileName, style = MaterialTheme.typography.titleMedium)
                Text(pluralStringResource(R.plurals.install_apk_count, count, count))
                if (current.contents.hasObb) Text(stringResource(R.string.install_obb_notice))
                Button(onClick = viewModel::install) { Text(stringResource(R.string.install_action)) }
                Button(onClick = viewModel::reset) { Text(stringResource(R.string.cancel)) }
            }
            is InstallState.Invalid -> {
                Text(current.reason, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = viewModel::reset) { Text(stringResource(R.string.close)) }
            }
            is InstallState.Done -> {
                val text = when (val result = current.result) {
                    InstallResult.Success -> stringResource(R.string.install_success)
                    is InstallResult.Failure -> stringResource(R.string.install_failed, result.message)
                }
                Text(text, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = viewModel::reset) { Text(stringResource(R.string.close)) }
            }
        }
    }
}
