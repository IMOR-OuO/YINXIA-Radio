package com.example.soundbox.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundbox.R
import com.example.soundbox.data.AppSettings
import com.example.soundbox.ui.theme.SoundBoxColors
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(
    settings: AppSettings,
    backgroundFile: File?,
    onSettingsChange: (AppSettings) -> Unit,
    onSelectBackground: (Uri) -> Unit,
    onRestoreDefaultBackground: () -> Unit,
    onBack: () -> Unit
) {
    var selectedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val inPreview = LocalInspectionMode.current
    val imagePicker = if (inPreview) null else rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onSelectBackground)
    }

    LaunchedEffect(backgroundFile?.absolutePath) {
        selectedBitmap = if (backgroundFile?.exists() == true) {
            withContext(Dispatchers.IO) {
                runCatching {
                    BitmapFactory.decodeFile(backgroundFile.absolutePath)?.asImageBitmap()
                }.getOrNull()
            }
        } else {
            null
        }
    }
    val backgroundPainter: Painter = selectedBitmap?.let { BitmapPainter(it) }
        ?: painterResource(R.drawable.bg_about)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = backgroundPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - settings.backgroundOpacity).coerceIn(0f, 1f)))
        )
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SoundBoxColors.TopBar)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = SoundBoxColors.TextPrimary
                )
            }
            Text(
                text = "关于软件",
                modifier = Modifier.weight(1f),
                color = SoundBoxColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .padding(7.dp)
                    .border(1.dp, SoundBoxColors.TextSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    color = SoundBoxColors.TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.bg_about),
                    contentDescription = "背景图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.46f))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "音匣",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v1.1",
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color(0xFFE0E0E0),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "一款自定义音频音乐盒",
                        modifier = Modifier.padding(top = 18.dp),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "开发者：Imor",
                        modifier = Modifier.padding(top = 10.dp),
                        color = Color(0xFFD0D0D0),
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = "这是一款以音频打击垫为核心的本地音乐盒。你可以导入自己的音频素材，并将声音绑定到不同按键，点击屏幕或使用实体键盘即可快速触发。",
                color = SoundBoxColors.TextPrimary,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )

            Text(
                text = "支持多个音频同时播放，也提供音频打断模式。所有音频保存在应用私有目录中，不依赖原始文件的移动或删除。",
                color = SoundBoxColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )

            Text(
                text = "该软件完全免费，不存在付费版本！！！",
                color = SoundBoxColors.Danger,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "背景设置",
                    color = SoundBoxColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = { imagePicker?.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("选择图片")
                }
                OutlinedButton(
                    onClick = {
                        onSettingsChange(settings.copy(backgroundApplied = true))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("应用到主页和管理页面")
                }
                OutlinedButton(
                    onClick = onRestoreDefaultBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("恢复默认背景")
                }
                Column {
                    Text(
                        text = "背景透明度 ${(((1f - settings.backgroundOpacity) * 100).toInt())}%",
                        color = SoundBoxColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = (1f - settings.backgroundOpacity).coerceIn(0f, 1f),
                        onValueChange = {
                            onSettingsChange(settings.copy(backgroundOpacity = 1f - it))
                        },
                        valueRange = 0f..1f
                    )
                }
            }

            Text(
                text = "设置",
                color = SoundBoxColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingSwitch(
                    title = "音频打断",
                    description = "播放新音频时停止上一条音频",
                    checked = settings.interruptAudio,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(interruptAudio = it))
                    }
                )

                SettingSwitch(
                    title = "防止重音",
                    description = "同一按键未播放完时不会重复叠加",
                    checked = settings.preventRetrigger,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(preventRetrigger = it))
                    }
                )
                SettingSwitch(
                    title = "保持屏幕常亮",
                    description = "演奏过程中避免屏幕自动关闭",
                    checked = settings.keepScreenOn,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(keepScreenOn = it))
                    }
                )

                Column {
                    Text(
                        text = "总音量 ${(settings.globalVolume * 100).toInt()}%",
                        color = SoundBoxColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = settings.globalVolume,
                        onValueChange = {
                            onSettingsChange(settings.copy(globalVolume = it))
                        },
                        valueRange = 0f..1f
                    )
                }
            }
        }
    }
}
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SoundBoxColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 3.dp),
                color = SoundBoxColors.TextSecondary,
                fontSize = 12.sp
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
