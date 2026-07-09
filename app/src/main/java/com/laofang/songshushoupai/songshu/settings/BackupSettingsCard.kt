package com.laofang.songshushoupai.songshu.settings

import java.io.File
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

enum class BackupOperation { EXPORT, IMPORT, WEBDAV_UPLOAD, WEBDAV_DOWNLOAD }

private fun calculateStorageUsed(context: Context): Long {
    return dirSize(File(context.filesDir, "images")) + dirSize(File(context.filesDir, "covers"))
}

private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.listFiles()?.sumOf { if (it.isFile) it.length() else dirSize(it) } ?: 0L
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

private val BtnShape = RoundedCornerShape(12.dp)

@Composable
private fun ProgressOrText(showProgress: Boolean, text: String) {
    if (showProgress) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    else Text(text)
}

@Composable
fun BackupSettingsCard(
    context: Context,
    isLoading: Boolean,
    onBackupOperation: (BackupOperation) -> Unit,
    webdavUrl: String,
    webdavUser: String,
    webdavPass: String,
    onWebdavUrlChange: (String) -> Unit,
    onWebdavUserChange: (String) -> Unit,
    onWebdavPassChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveWebDavConfig: () -> Unit,
    isTesting: Boolean,
    isConfigModified: Boolean
) {
    val cs = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("本地备份", style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        Text("将配置和所有图片导出为ZIP文件，或从ZIP文件恢复。", style = MaterialTheme.typography.bodySmall, color = cs.onSurface.copy(alpha = 0.7f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onBackupOperation(BackupOperation.EXPORT) }, Modifier.weight(1f), !isLoading, BtnShape) { Text("导出配置") }
            OutlinedButton(onClick = { onBackupOperation(BackupOperation.IMPORT) }, Modifier.weight(1f), !isLoading, BtnShape) { Text("导入配置") }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("WebDAV 备份", style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        Text("配置WebDAV服务器地址，将备份上传到服务器或从服务器恢复。", style = MaterialTheme.typography.bodySmall, color = cs.onSurface.copy(alpha = 0.7f))

        OutlinedTextField(webdavUrl, onWebdavUrlChange, label = { Text("服务器地址") },
            placeholder = { Text("https://example.com/remote.php/dav/files/user/") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isTesting)
        OutlinedTextField(webdavUser, onWebdavUserChange, label = { Text("用户名") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isTesting)
        OutlinedTextField(webdavPass, onWebdavPassChange, label = { Text("密码") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), enabled = !isTesting)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTestConnection, Modifier.weight(1f), !isTesting, BtnShape) {
                ProgressOrText(isTesting, "测试连接")
            }
            OutlinedButton(onClick = { onBackupOperation(BackupOperation.WEBDAV_UPLOAD) },
                Modifier.weight(1f), !isLoading && webdavUrl.isNotBlank(), BtnShape) {
                ProgressOrText(isLoading, "备份")
            }
            OutlinedButton(onClick = { onBackupOperation(BackupOperation.WEBDAV_DOWNLOAD) },
                Modifier.weight(1f), !isLoading && webdavUrl.isNotBlank(), BtnShape) {
                ProgressOrText(isLoading, "恢复")
            }
        }

        AnimatedVisibility(isConfigModified, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Button(onSaveWebDavConfig, Modifier.fillMaxWidth(), !isTesting, BtnShape) { Text("保存配置") }
        }

        if (webdavUrl.isBlank()) {
            Text("请先配置 WebDAV 服务器地址", style = MaterialTheme.typography.bodySmall, color = cs.error.copy(alpha = 0.8f))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("要备份的数据大小", style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
            Text(formatSize(calculateStorageUsed(context)), style = MaterialTheme.typography.bodyLarge, color = cs.primary)
        }
    }
}
