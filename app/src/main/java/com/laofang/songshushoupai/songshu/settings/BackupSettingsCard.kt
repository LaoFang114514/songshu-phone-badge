package com.laofang.songshushoupai.songshu.settings


import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.WebDavConfig
import java.io.File

private fun calculateStorageUsed(context: Context): Long {
    val imgDir = File(context.filesDir, "images")
    val covDir = File(context.filesDir, "covers")
    return dirSize(imgDir) + dirSize(covDir)
}

private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.listFiles()?.sumOf { if (it.isFile) it.length() else dirSize(it) } ?: 0L
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

@Composable
fun BackupSettingsCard(
    context: Context,
    isLoading: Boolean,
    onShowBackupConfirmDialogChange: (Boolean, () -> Unit) -> Unit,
    onShowWebDavConfigDialogChange: (Boolean) -> Unit,
    webdavUrl: String,
    onStatusMessageChange: (String?) -> Unit
) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "本地备份",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "将配置和所有图片导出为ZIP文件，或从ZIP文件恢复。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    onShowBackupConfirmDialogChange(true) {
                        onStatusMessageChange("功能已在主文件处理")
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) { Text("导出配置") }
            OutlinedButton(
                onClick = {
                    onShowBackupConfirmDialogChange(true) {
                        onStatusMessageChange("功能已在主文件处理")
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) { Text("导入配置") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "WebDAV 备份",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "配置WebDAV服务器地址，将备份上传到服务器或从服务器恢复。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        OutlinedButton(
            onClick = { onShowWebDavConfigDialogChange(true) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (webdavUrl.isNotBlank()) "服务器配置：已配置 (${webdavUrl.take(30)}...)"
                else "点击配置 WebDAV 服务器"
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val config = WebDavConfig(webdavUrl, "", "")
                    if (config.url.isNotBlank()) {
                        onShowBackupConfirmDialogChange(true) {
                            onStatusMessageChange("功能已在主文件处理")
                        }
                    } else { onStatusMessageChange("请先配置服务器地址") }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("备份到服务器")
            }
            OutlinedButton(
                onClick = {
                    val config = WebDavConfig(webdavUrl, "", "")
                    if (config.url.isNotBlank()) {
                        onShowBackupConfirmDialogChange(true) {
                            onStatusMessageChange("功能已在主文件处理")
                        }
                    } else { onStatusMessageChange("请先配置服务器地址") }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("从服务器恢复")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "已占用存储（测试）",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatSize(calculateStorageUsed(context)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
