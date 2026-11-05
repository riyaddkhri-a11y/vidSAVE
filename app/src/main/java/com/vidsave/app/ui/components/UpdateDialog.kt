package com.vidsave.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.vidsave.app.R
import com.vidsave.app.data.model.UpdateMetadata
import com.vidsave.app.ui.theme.PrimaryAccent
import com.vidsave.app.update.UpdateStatus

@Composable
fun UpdateDialog(
    updateStatus: UpdateStatus,
    currentVersionName: String,
    onDownloadAndInstall: (UpdateMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    val metadata = (updateStatus as? UpdateStatus.Available)?.metadata

    if (metadata != null || updateStatus is UpdateStatus.Downloading || updateStatus is UpdateStatus.VerificationFailed) {
        AlertDialog(
            onDismissRequest = {
                if (updateStatus !is UpdateStatus.Downloading) onDismiss()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "System Update",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (metadata != null) stringResource(R.string.update_available, metadata.version) else stringResource(R.string.downloading_update),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.current_installed, currentVersionName),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (metadata != null) {
                        Text(
                            text = stringResource(R.string.release_notes),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metadata.releaseNotes.ifBlank { stringResource(R.string.default_release_notes) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (updateStatus) {
                        is UpdateStatus.Downloading -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.downloading_update),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${updateStatus.progressPercentage}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { updateStatus.progressPercentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = PrimaryAccent
                                )
                            }
                        }
                        is UpdateStatus.VerificationFailed -> {
                            val isSignatureMismatch = updateStatus.reason.contains("SIGNATURE_MISMATCH")
                            val errorText = if (isSignatureMismatch) {
                                stringResource(R.string.verification_error_signature_mismatch)
                            } else {
                                stringResource(R.string.verification_error_generic, updateStatus.reason)
                            }
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                if (metadata != null && updateStatus is UpdateStatus.Available) {
                    Button(
                        onClick = { onDownloadAndInstall(metadata) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier.testTag("confirm_update_button")
                    ) {
                        Text(stringResource(R.string.update_now), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (updateStatus !is UpdateStatus.Downloading) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_update_button")
                    ) {
                        Text(stringResource(R.string.later))
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
