package com.vidsave.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidsave.app.data.model.UpdateMetadata
import com.vidsave.app.update.InAppUpdateManager
import com.vidsave.app.update.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val updateManager = InAppUpdateManager(application)

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    val currentVersionName: String get() = updateManager.currentVersionName
    val currentVersionCode: Long get() = updateManager.currentVersionCode

    init {
        // Automatic update check on app launch
        checkForUpdates(forceCheck = false)
    }

    fun checkForUpdates(forceCheck: Boolean = true) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val result = updateManager.checkForUpdates(forceCheck = forceCheck)

            if (result.isSuccess) {
                val metadata = result.getOrNull()
                if (metadata != null) {
                    _updateStatus.value = UpdateStatus.Available(metadata)
                } else {
                    _updateStatus.value = UpdateStatus.NoUpdate
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to check for updates."
                _updateStatus.value = UpdateStatus.Error(errorMsg)
            }
        }
    }

    fun downloadAndInstall(metadata: UpdateMetadata) {
        viewModelScope.launch {
            updateManager.downloadAndVerifyApk(metadata).collect { status ->
                _updateStatus.value = status

                if (status is UpdateStatus.ReadyToInstall) {
                    val installed = updateManager.installApk(status.apkFile)
                    if (!installed) {
                        _updateStatus.value = UpdateStatus.Error("Failed to trigger package installer. Check permissions.")
                    }
                }
            }
        }
    }

    fun dismissDialog() {
        _updateStatus.value = UpdateStatus.Idle
    }
}
