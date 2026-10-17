package com.example.soundbox.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundbox.data.Pad
import com.example.soundbox.ui.theme.SoundBoxColors

@Composable
fun SoundPadCard(
    pad: Pad,
    active: Boolean,
    onTrigger: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember(pad.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "padScale"
    )

    Box(
        modifier = modifier
            .heightIn(min = 140.dp, max = 180.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) SoundBoxColors.CardPressed else SoundBoxColors.Card)
            .border(
                width = if (pressed || active) 1.5.dp else 0.dp,
                color = if (active) SoundBoxColors.Accent else SoundBoxColors.Border,
                shape = RoundedCornerShape(12.dp)
            )
            .semantics {
                role = Role.Button
                contentDescription = "${pad.name}${pad.key?.let { "，快捷键 $it" } ?: ""}"
                onClick {
                    onTrigger()
                    true
                }
            }
            .pointerInput(pad.id) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onTrigger()
                        tryAwaitRelease()
                        pressed = false
                    },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Text(
            text = pad.name,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            color = SoundBoxColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )


        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(SoundBoxColors.Accent, RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "播放中",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}