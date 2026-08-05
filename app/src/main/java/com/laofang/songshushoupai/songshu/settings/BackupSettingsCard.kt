package com.laofang.songshushoupai.songshu.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.R
import java.io.File

enum class BackupOperation { EXPORT, IMPORT, WEBDAV_UPLOAD, WEBDAV_DOWNLOAD }

private fun calculateStorageUsed(context: Context): Long =
    dirSize(File(context.filesDir, "images")) + dirSize(File(context.filesDir, "covers")) + dirSize(File(context.filesDir, "qrcodes"))

private fun dirSize(dir: File): Long =
    if (!dir.exists()) 0L else dir.listFiles()?.sumOf { if (it.isFile) it.length() else dirSize(it) } ?: 0L

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1048576 -> "${bytes / 1024} KB"
    bytes < 1073741824 -> "${"%.1f".format(bytes / 1048576.0)} MB"
    else -> "${"%.2f".format(bytes / 1073741824.0)} GB"
}

private val BtnShape = RoundedCornerShape(12.dp)

@Composable
private fun ProgressOrText(showProgress: Boolean, text: String) {
    if (showProgress) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(text)
}

@Composable
fun BackupSettingsCard(
    context: Context, isLoading: Boolean, onBackupOperation: (BackupOperation) -> Unit,
    webdavUrl: String, webdavUser: String, webdavPass: String,
    onWebdavUrlChange: (String) -> Unit, onWebdavUserChange: (String) -> Unit, onWebdavPassChange: (String) -> Unit,
    onTestConnection: () -> Unit, onSaveWebDavConfig: () -> Unit, isTesting: Boolean, isConfigModified: Boolean
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.local_backup), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        Text(stringResource(R.string.local_backup_desc), style = MaterialTheme.typography.bodySmall, color = cs.onSurface.copy(alpha = 0.7f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onBackupOperation(BackupOperation.EXPORT) }, Modifier.weight(1f), !isLoading, BtnShape) { Text(stringResource(R.string.export_config)) }
            Button(onClick = { onBackupOperation(BackupOperation.IMPORT) }, Modifier.weight(1f), !isLoading, BtnShape) { Text(stringResource(R.string.import_config)) }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.webdav_backup), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        Text(stringResource(R.string.webdav_backup_desc), style = MaterialTheme.typography.bodySmall, color = cs.onSurface.copy(alpha = 0.7f))
        OutlinedTextField(webdavUrl, onWebdavUrlChange, label = { Text(stringResource(R.string.server_address)) },
            placeholder = { Text("https://example.com/remote.php/dav/files/user/") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isTesting)
        OutlinedTextField(webdavUser, onWebdavUserChange, label = { Text(stringResource(R.string.username)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isTesting)
        OutlinedTextField(webdavPass, onWebdavPassChange, label = { Text(stringResource(R.string.password)) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), enabled = !isTesting)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTestConnection, Modifier.weight(1f), !isTesting, BtnShape) { ProgressOrText(isTesting, stringResource(R.string.test)) }
            Button(onClick = { onBackupOperation(BackupOperation.WEBDAV_UPLOAD) },
                Modifier.weight(1f), !isLoading && webdavUrl.isNotBlank(), BtnShape) { ProgressOrText(isLoading, stringResource(R.string.backup)) }
            Button(onClick = { onBackupOperation(BackupOperation.WEBDAV_DOWNLOAD) },
                Modifier.weight(1f), !isLoading && webdavUrl.isNotBlank(), BtnShape) { ProgressOrText(isLoading, stringResource(R.string.restore)) }
        }
        AnimatedVisibility(isConfigModified, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Button(onSaveWebDavConfig, Modifier.fillMaxWidth(), !isTesting, BtnShape) { Text(stringResource(R.string.save_config)) }
        }
        if (webdavUrl.isBlank()) Text(stringResource(R.string.config_webdav_first), style = MaterialTheme.typography.bodySmall, color = cs.error.copy(alpha = 0.8f))
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.data_size), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
            Text(formatSize(calculateStorageUsed(context)), style = MaterialTheme.typography.bodyLarge, color = cs.primary)
        }
    }
}
