package com.laofang.songshushoupai.songshu.settings


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

@Composable
fun BackupSettingsCard(
    isLoading: Boolean,
    onShowBackupConfirmDialogChange: (Boolean, () -> Unit) -> Unit,
    onShowWebDavConfigDialogChange: (Boolean) -> Unit,
    webdavUrl: String,
    onStatusMessageChange: (String?) -> Unit
) {

    Column {
        Text(
            text = "本地备份",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "将配置和所有图片导出为ZIP文件，或从ZIP文件恢复。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "WebDAV 备份",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "配置WebDAV服务器地址，将备份上传到服务器或从服务器恢复。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}
