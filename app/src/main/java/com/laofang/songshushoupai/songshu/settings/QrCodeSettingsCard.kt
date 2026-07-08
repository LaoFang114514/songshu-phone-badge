package com.laofang.songshushoupai.songshu.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.laofang.songshushoupai.songshu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun QrCodeSettingsCard(
    showQrCode: Boolean,
    onShowQrCodeChange: (Boolean) -> Unit,
    qrCodePath: String,
    onQrCodePathChange: (String) -> Unit,
    qrPreviewBmp: android.graphics.Bitmap?,
    onQrPreviewBmpChange: (android.graphics.Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val qrImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            scope.launch {
                with(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it) ?: return@with
                        val dir = File(context.filesDir, "qrcodes").also { d -> d.mkdirs() }
                        val file = File(dir, "custom_qr.png")
                        FileOutputStream(file).use { out -> inputStream.copyTo(out) }
                        inputStream.close()
                        val path = file.absolutePath
                        onQrCodePathChange(path)
                        onQrPreviewBmpChange(android.graphics.BitmapFactory.decodeFile(path))
                    } catch (_: Exception) {}
                }
            }
        }
    }
    
    Column {
        SettingsSwitchRow("上划展示二维码", "打开上划屏幕展示二维码", showQrCode) {
            onShowQrCodeChange(it)
        }
        HorizontalDivider()
        val qrAlpha = if (showQrCode) 1f else 0.4f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .graphicsLayer { alpha = qrAlpha },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "自定义二维码", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (qrCodePath.isNotEmpty()) "已导入自定义二维码" else "未导入，将使用默认示例图",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { qrImageLauncher.launch("image/*") },
                enabled = showQrCode,
                shape = RoundedCornerShape(12.dp)
            ) { Text("导入") }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = qrAlpha },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrPreviewBmp != null) {
                    Image(
                        painter = BitmapPainter(qrPreviewBmp.asImageBitmap()),
                        contentDescription = "二维码预览",
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .heightIn(max = 200.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.qr_zanzhu),
                        contentDescription = "默认二维码",
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .heightIn(max = 200.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
