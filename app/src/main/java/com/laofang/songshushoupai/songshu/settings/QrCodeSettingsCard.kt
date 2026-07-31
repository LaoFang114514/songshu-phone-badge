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
import androidx.compose.ui.res.stringResource
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
        SettingsSwitchRow(stringResource(R.string.swipe_show_qr), stringResource(R.string.swipe_show_qr_desc), showQrCode, onShowQrCodeChange)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        val alpha = if (showQrCode) 1f else 0.4f
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { this.alpha = alpha },
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.custom_qr), style = MaterialTheme.typography.bodyLarge)
                Text(if (qrCodePath.isNotEmpty()) stringResource(R.string.custom_qr_imported) else stringResource(R.string.custom_qr_default),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = showQrCode, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.import_qr)) }
        }
        Surface(Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha },
            shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                val imgMod = Modifier.fillMaxWidth(0.6f).heightIn(max = 200.dp)
                if (qrPreviewBmp != null) Image(BitmapPainter(qrPreviewBmp.asImageBitmap()), stringResource(R.string.qr_preview), imgMod, contentScale = ContentScale.Fit)
                else Image(painterResource(R.drawable.qr_zanzhu), stringResource(R.string.default_qr), imgMod, contentScale = ContentScale.Fit)
            }
        }
    }
}
