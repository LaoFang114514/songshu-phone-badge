package com.laofang.songshushoupai.songshu.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun QrCodeSettingsCard(
    showQrCode: Boolean, onShowQrCodeChange: (Boolean) -> Unit,
    qrCodePath: String, onQrCodePathChange: (String) -> Unit,
    qrPreviewBmp: Bitmap?, onQrPreviewBmpChange: (Bitmap?) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val input = ctx.contentResolver.openInputStream(uri) ?: return@launch
                val file = File(File(ctx.filesDir, "qrcodes").also { it.mkdirs() }, "custom_qr.png")
                FileOutputStream(file).use { out -> input.copyTo(out) }
                input.close()
                onQrCodePathChange(file.absolutePath)
                onQrPreviewBmpChange(BitmapFactory.decodeFile(file.absolutePath))
            } catch (_: Exception) {}
        }
    }

    Column {
        SettingsSwitchRow("上划展示二维码", "打开上划屏幕展示二维码", showQrCode, onShowQrCodeChange)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        val alpha = if (showQrCode) 1f else 0.4f
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { this.alpha = alpha },
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("自定义二维码", style = MaterialTheme.typography.bodyLarge)
                Text(if (qrCodePath.isNotEmpty()) "已导入自定义二维码" else "未导入，将使用默认示例图",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = showQrCode, shape = RoundedCornerShape(12.dp)) { Text("导入") }
        }
        Surface(Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha },
            shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                val imgMod = Modifier.fillMaxWidth(0.6f).heightIn(max = 200.dp)
                if (qrPreviewBmp != null) Image(BitmapPainter(qrPreviewBmp.asImageBitmap()), "二维码预览", imgMod, contentScale = ContentScale.Fit)
                else Image(painterResource(R.drawable.qr_zanzhu), "默认二维码", imgMod, contentScale = ContentScale.Fit)
            }
        }
    }
}
