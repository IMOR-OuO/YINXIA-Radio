package com.example.soundbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundbox.data.Pad
import com.example.soundbox.ui.AppUiState
import com.example.soundbox.ui.components.EmptyBoxState
import com.example.soundbox.ui.components.BottomMode
import com.example.soundbox.ui.components.BottomModeBar
import com.example.soundbox.ui.components.SoundPadCard
import com.example.soundbox.ui.theme.SoundBoxColors

@Composable
fun HomeScreen(
    state: AppUiState,
    onOpenManager: () -> Unit,
    onEditBox: () -> Unit,
    onAddAudio: () -> Unit,
    onPlayPad: (Pad) -> Unit,
    onEditPad: (Pad) -> Unit,
    onStopAll: () -> Unit,
    onKeyPad: (String) -> Unit
) {
    val box = state.currentBox
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()

            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.nativeKeyEvent.repeatCount > 0) {
                    false
                } else {
                    keyName(event.key)?.let {
                        onKeyPad(it)
                        true
                    } ?: false
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SoundBoxColors.TopBar)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenManager) {
                Icon(Icons.Default.Menu, contentDescription = "音乐盒管理", tint = SoundBoxColors.TextPrimary)
            }
            Text(
                text = box?.name ?: "音乐盒",
                modifier = Modifier.weight(1f),
                color = SoundBoxColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = onStopAll) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(SoundBoxColors.TextPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SoundBoxColors.Card)
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = SoundBoxColors.TextPrimary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("编辑音乐盒") }, onClick = { menuOpen = false; onEditBox() })
                    DropdownMenuItem(text = { Text("音乐盒管理") }, onClick = { menuOpen = false; onOpenManager() })
                }
            }
        }
        if (box == null || box.pads.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyBoxState(onImport = onAddAudio)
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val adaptiveColumns = (maxWidth / 152.dp).toInt().coerceIn(2, 6)
                val columns = box.preferredColumns?.coerceIn(2, 6) ?: adaptiveColumns
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(box.pads, key = { it.id }) { pad ->
                        SoundPadCard(
                            pad = pad,
                            active = (state.activeVoices[pad.id] ?: 0) > 0,
                            onTrigger = { onPlayPad(pad) },
                            onLongPress = { onEditPad(pad) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                }
            }
        }

        BottomModeBar(
            selected = BottomMode.Home,
            onHome = {},
            onList = onOpenManager
        )
    }
}

private fun keyName(key: Key): String? = when (key) {
    Key.A -> "A"
    Key.B -> "B"
    Key.C -> "C"
    Key.D -> "D"
    Key.E -> "E"
    Key.F -> "F"
    Key.G -> "G"
    Key.H -> "H"
    Key.I -> "I"
    Key.J -> "J"
    Key.K -> "K"
    Key.L -> "L"
    Key.M -> "M"
    Key.N -> "N"
    Key.O -> "O"
    Key.P -> "P"
    Key.Q -> "Q"
    Key.R -> "R"
    Key.S -> "S"
    Key.T -> "T"
    Key.U -> "U"
    Key.V -> "V"
    Key.W -> "W"
    Key.X -> "X"
    Key.Y -> "Y"
    Key.Z -> "Z"
    else -> null
}
