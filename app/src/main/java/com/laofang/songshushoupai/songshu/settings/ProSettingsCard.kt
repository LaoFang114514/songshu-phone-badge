package com.laofang.songshushoupai.songshu.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.laofang.songshushoupai.songshu.R

private val BtnShape = RoundedCornerShape(12.dp)

@Composable
fun ProSettingsCard() {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.pro_logo),
            contentDescription = stringResource(R.string.pro_app_name),
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(20.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.pro_app_name), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.pro_desc),
            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/LaoFang114514/songshu-badge-pro".toUri())) },
                Modifier.fillMaxWidth(), shape = BtnShape) { Text("GitHub") }
            OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, "https://gitcode.com/LaoFang233/songshu-badge-pro".toUri())) },
                Modifier.fillMaxWidth(), shape = BtnShape) { Text("GitCode") }
        }
    }
}