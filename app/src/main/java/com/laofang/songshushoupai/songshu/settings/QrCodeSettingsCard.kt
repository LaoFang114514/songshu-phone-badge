package com.laofang.songshushoupai.songshu.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.core.QrCodeGenerator
import com.laofang.songshushoupai.songshu.core.QrCodeItem
import com.laofang.songshushoupai.songshu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val DlgShape = RoundedCornerShape(12.dp)

@Composable
fun QrCodeSettingsCard(
    showQrCode: Boolean, onShowQrCodeChange: (Boolean) -> Unit,
    qrList: List<QrCodeItem>, selectedIndex: Int,
    onSelect: (Int) -> Unit, onAdd: (QrCodeItem) -> Unit,
    onDelete: (Int) -> Unit, onMoveUp: (Int) -> Unit, onMoveDown: (Int) -> Unit,
    onRename: (Int, String) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var delIdx by remember { mutableIntStateOf(-1) }
    var renIdx by remember { mutableIntStateOf(-1) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val input = ctx.contentResolver.openInputStream(uri) ?: return@launch
                val file = File(File(ctx.filesDir, "qrcodes").also { it.mkdirs() }, "qr_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out -> input.copyTo(out) }
                input.close()
                onAdd(QrCodeItem(file.absolutePath, ""))
            } catch (_: Exception) {}
        }
    }

    SettingsSwitchRow(stringResource(R.string.swipe_show_qr), stringResource(R.string.swipe_show_qr_desc), showQrCode, onShowQrCodeChange)

    HorizontalDivider(Modifier.padding(vertical = 8.dp))

    Row(Modifier.fillMaxWidth().graphicsLayer { this.alpha = if (showQrCode) 1f else 0.4f },
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { linkInput = ""; showLinkDialog = true },
            modifier = Modifier.weight(1f), enabled = showQrCode, shape = DlgShape
        ) { Text(stringResource(R.string.qr_from_link)) }
        OutlinedButton(
            onClick = { picker.launch("image/*") },
            modifier = Modifier.weight(1f), enabled = showQrCode, shape = DlgShape
        ) { Text(stringResource(R.string.import_qr)) }
    }

    Column(Modifier.fillMaxWidth().graphicsLayer { this.alpha = if (showQrCode) 1f else 0.4f },
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (qrList.isEmpty()) {
            DefaultQrCard()
        } else {
            qrList.forEachIndexed { i, item ->
                QrCard(item, i == selectedIndex, { onSelect(i) },
                    { delIdx = i }, { onMoveUp(i) }, { onMoveDown(i) },
                    { renIdx = i }, i > 0, i < qrList.size - 1, enabled = showQrCode)
            }
        }
    }

    if (delIdx in qrList.indices) {
        AlertDialog(onDismissRequest = { delIdx = -1 },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_msg, qrList[delIdx].name)) },
            confirmButton = { Button(onClick = { onDelete(delIdx); delIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.delete)) } },
            dismissButton = { OutlinedButton(onClick = { delIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.cancel)) } })
    }
    if (renIdx in qrList.indices) {
        var txt by remember { mutableStateOf(qrList[renIdx].name) }
        AlertDialog(onDismissRequest = { renIdx = -1 },
            title = { Text(stringResource(R.string.rename)) },
            text = { OutlinedTextField(txt, { txt = it }, singleLine = true, label = { Text(stringResource(R.string.name_label)) }) },
            confirmButton = { Button(onClick = { if (txt.isNotBlank()) onRename(renIdx, txt.trim()); renIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { OutlinedButton(onClick = { renIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.cancel)) } })
    }
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGenerating) showLinkDialog = false },
            title = { Text(stringResource(R.string.qr_generate_from_link)) },
            text = {
                Column {
                    OutlinedTextField(linkInput, { linkInput = it },
                        label = { Text(stringResource(R.string.qr_input_link)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isGenerating)
                    if (isGenerating) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(" 生成中...", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (linkInput.isNotBlank()) {
                        isGenerating = true
                        scope.launch(Dispatchers.IO) {
                            val bmp = QrCodeGenerator.generate(linkInput.trim())
                            if (bmp != null) {
                                val file = File(File(ctx.filesDir, "qrcodes").also { it.mkdirs() }, "qr_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                                onAdd(QrCodeItem(file.absolutePath, "", linkInput.trim()))
                            }
                            withContext(Dispatchers.Main) { isGenerating = false; showLinkDialog = false }
                        }
                    }
                }, shape = DlgShape, enabled = linkInput.isNotBlank() && !isGenerating) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLinkDialog = false }, shape = DlgShape, enabled = !isGenerating) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun QrCard(
    item: QrCodeItem, isSelected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit, onRenameClick: () -> Unit,
    canMoveUp: Boolean, canMoveDown: Boolean, enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var bitmap by remember(item.path) { mutableStateOf<Bitmap?>(null) }
    val borderWidth by animateFloatAsState(if (isSelected) 2f else 1f, tween(250), label = "borderWidth")
    val cs = MaterialTheme.colorScheme
    val displayName = item.name

    Row(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp))
        .background(cs.surface)
        .border(BorderStroke(borderWidth.dp, if (isSelected) cs.primary else cs.primary.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
        .clickable(enabled = enabled) { onSelect() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {

        Box(Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant)) {
            val bmp = bitmap
            if (bmp != null) Image(BitmapPainter(bmp.asImageBitmap()), displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Image(painterResource(R.drawable.qr_zanzhu), displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (isSelected) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(cs.primary))
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(displayName, style = MaterialTheme.typography.titleMedium)
        }

        Box {
            IconButton({ expanded = true }, enabled = enabled) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options)) }
            DropdownMenu(expanded, { expanded = false }, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(BorderStroke(1.dp, cs.outlineVariant), RoundedCornerShape(12.dp))) {
                DropdownMenuItem({ Text(stringResource(R.string.rename)) }, onClick = { onRenameClick(); expanded = false })
                DropdownMenuItem({ Text(stringResource(R.string.move_up)) }, onClick = { onMoveUp(); expanded = false }, enabled = canMoveUp)
                DropdownMenuItem({ Text(stringResource(R.string.move_down)) }, onClick = { onMoveDown(); expanded = false }, enabled = canMoveDown)
                DropdownMenuItem({ Text(stringResource(R.string.delete)) }, onClick = { onDelete(); expanded = false })
            }
        }
    }

    LaunchedEffect(item.path) {
        bitmap = withContext(Dispatchers.IO) {
            if (item.path.isEmpty()) return@withContext null
            try { BitmapFactory.decodeFile(item.path) } catch (_: Throwable) { null }
        }
    }
}

@Composable
private fun DefaultQrCard() {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp))
        .background(cs.surface)
        .border(BorderStroke(1.dp, cs.primary.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
        .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {

        Box(Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant)) {
            Image(painterResource(R.drawable.qr_zanzhu), stringResource(R.string.default_qr),
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.default_qr), style = MaterialTheme.typography.titleMedium)
        }
    }
}
