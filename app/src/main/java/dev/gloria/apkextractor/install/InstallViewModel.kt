package dev.gloria.apkextractor.install

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface InstallState {
    data object Idle : InstallState
    data object Reading : InstallState
    data class Ready(val fileName: String, val contents: BundleContents.Apks) : InstallState
    data class Invalid(val reason: String) : InstallState
    data object Installing : InstallState
    data class Done(val result: InstallResult) : InstallState
}

class InstallViewModel(
    private val resolver: ContentResolver,
    private val installer: ApkInstaller,
) : ViewModel() {

    private val _state = MutableStateFlow<InstallState>(InstallState.Idle)
    val state: StateFlow<InstallState> = _state

    private var installJob: Job? = null

    fun onFilePicked(uri: Uri) {
        _state.value = InstallState.Reading
        viewModelScope.launch {
            _state.value = try {
                val name = withContext(Dispatchers.IO) { displayName(uri) }
                val contents = withContext(Dispatchers.IO) {
                    readBundle({ resolver.openInputStream(uri) ?: throw IOException("No se puede abrir el archivo") }, name)
                }
                when (contents) {
                    is BundleContents.Apks -> InstallState.Ready(name, contents)
                    is BundleContents.Invalid -> InstallState.Invalid(contents.reason)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                InstallState.Invalid(e.message ?: "No se puede leer el archivo")
            }
        }
    }

    fun install() {
        val ready = _state.value as? InstallState.Ready ?: return
        _state.value = InstallState.Installing
        installJob = viewModelScope.launch {
            _state.value = InstallState.Done(installer.install(ready.contents.entries))
        }
    }

    fun cancelInstall() {
        installJob?.cancel()
        _state.value = InstallState.Idle
    }

    fun reset() {
        _state.value = InstallState.Idle
    }

    fun canInstall(): Boolean = installer.canInstall()

    private fun displayName(uri: Uri): String =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: uri.lastPathSegment ?: "paquete"
}
