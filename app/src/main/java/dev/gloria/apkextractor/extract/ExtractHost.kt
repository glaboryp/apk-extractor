package dev.gloria.apkextractor.extract

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.gloria.apkextractor.R
import dev.gloria.apkextractor.output.OutputStore
import java.util.UUID

@Composable
fun ExtractHost(workId: UUID, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    val info by workManager.getWorkInfoByIdFlow(workId).collectAsStateWithLifecycle(null)
    val current = info ?: return
    val chooserTitle = stringResource(R.string.share_chooser)

    when (current.state) {
        WorkInfo.State.CANCELLED -> LaunchedEffect(workId) { onDismiss() }

        WorkInfo.State.SUCCEEDED -> {
            val data = current.outputData
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.extract_done)) },
                text = { Text(data.getString(ExtractWorker.KEY_NAME).orEmpty()) },
                confirmButton = {
                    TextButton(onClick = {
                        val send = OutputStore(context).shareIntent(
                            data.getString(ExtractWorker.KEY_URI)!!.toUri(),
                            data.getString(ExtractWorker.KEY_MIME) ?: BUNDLE_MIME,
                        )
                        context.startActivity(Intent.createChooser(send, chooserTitle))
                    }) { Text(stringResource(R.string.share)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
            )
        }

        WorkInfo.State.FAILED -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.extract_failed)) },
            text = { Text(current.outputData.getString(ExtractWorker.KEY_REASON).orEmpty()) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        )

        else -> {
            val done = current.progress.getLong(ExtractWorker.KEY_DONE, 0)
            val total = current.progress.getLong(ExtractWorker.KEY_TOTAL, 0)
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.extract)) },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        if (total > 0) {
                            LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { workManager.cancelWorkById(workId) }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}
