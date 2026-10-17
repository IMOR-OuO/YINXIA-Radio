package com.example.soundbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundbox.ui.theme.SoundBoxColors

@Composable
fun AddPadTile(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 140.dp, max = 180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SoundBoxColors.Background)
            .border(1.dp, SoundBoxColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onAdd)
            .semantics {
                role = Role.Button
                contentDescription = "添加音效"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+",
                color = SoundBoxColors.Accent,
                fontSize = 38.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "添加音效",
                color = SoundBoxColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun HomeControlBar(
    activeCount: Int,
    globalVolume: Float,
    interruptAudio: Boolean,
    onStopAll: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onInterruptChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(SoundBoxColors.TopBar)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 86.dp, height = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SoundBoxColors.Card)
                .clickable(onClick = onStopAll),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "■ 停止",
                color = SoundBoxColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Column(modifier = Modifier.width(110.dp)) {
            Text(
                text = "正在播放 $activeCount",
                color = SoundBoxColors.TextSecondary,
                fontSize = 12.sp
            )
            Slider(
                value = globalVolume,
                onValueChange = onVolumeChange,
                modifier = Modifier.height(32.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "音频打断",
                color = SoundBoxColors.TextSecondary,
                fontSize = 12.sp
            )
            Switch(
                checked = interruptAudio,
                onCheckedChange = onInterruptChange
            )
        }
    }
}

@Composable
fun EmptyBoxState(
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "还没有音效",
            color = SoundBoxColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "导入本地音频，创建第一个 Sound Pad",
            modifier = Modifier.padding(top = 8.dp),
            color = SoundBoxColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SoundBoxColors.Accent)
                .clickable(onClick = onImport)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "导入音频",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
enum class BottomMode {
    Home,
    List
}

@Composable
fun BottomModeBar(
    selected: BottomMode,
    onHome: () -> Unit,
    onList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SoundBoxColors.TopBar)
                .border(1.dp, SoundBoxColors.Border, RoundedCornerShape(14.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomModeButton(
                icon = Icons.Rounded.Home,
                contentDescription = "主页",
                selected = selected == BottomMode.Home,
                onClick = onHome,
                modifier = Modifier.weight(1f)
            )
            BottomModeButton(
                icon = Icons.AutoMirrored.Rounded.List,
                contentDescription = "列表",
                selected = selected == BottomMode.List,
                onClick = onList,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomModeButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (selected) SoundBoxColors.CardPressed else SoundBoxColors.Card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) SoundBoxColors.TextPrimary else SoundBoxColors.Border,
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (selected) SoundBoxColors.TextPrimary else SoundBoxColors.TextSecondary
        )
    }
}