package com.example.ui.screens

import android.widget.Toast
import com.example.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PixelArtAnimationResponse
import com.example.ui.theme.*
import com.example.ui.viewmodel.PixelAnimatorViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelLabScreen(
    viewModel: PixelAnimatorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val promptState by viewModel.prompt.collectAsState()
    val frameCountState by viewModel.frameCount.collectAsState()
    val paletteHintState by viewModel.paletteHint.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val currentFrameIndex by viewModel.currentFrameIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackSpeedMs by viewModel.playbackSpeedMs.collectAsState()
    val historyList by viewModel.history.collectAsState()

    val isCloudEnabledState by viewModel.isCloudEnabled.collectAsState()
    val selectedLocalModelState by viewModel.selectedLocalModel.collectAsState()
    val localModelsList = viewModel.localModelsList

    val isRealImageState by viewModel.isRealImageMode.collectAsState()
    val isLocalHDMode by viewModel.isLocalHDMode.collectAsState()
    val selectedHDModel by viewModel.selectedHDModel.collectAsState()
    val realImageBase64 by viewModel.realImageBase64.collectAsState()

    var showGridLines by remember { mutableStateOf(true) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    // Glow Animation for Retro feel
    val infiniteTransition = rememberInfiniteTransition(label = "retro_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Standard Bilingual Retro Gaming Presets
    val bilingualRetroRecipes = listOf(
        Pair("🎮 ماريو بكسل", "mario sprite, retro nes plumber hero character, game boy style"),
        Pair("🗡️ بطل فاميكوم", "retro pixel knight hero with a shiny gold sword, nes style"),
        Pair("👾 وحش كلاسيكي", "retro alien monster sprite, glowing eyes, gameboy classic"),
        Pair("🪙 عملة ذهبية", "retro gold coin sprite, shiny nes gold, arcade item"),
        Pair("🏰 قلعة ريترو", "nes castle brick tower with flag, game boy retro background")
    )

    fun getColorForChar(char: Char, palette: List<String>): Color {
        val index = try {
            char.toString().toInt(16)
        } catch (e: Exception) {
            0
        }
        val hexColor = palette.getOrNull(index) ?: "#0F172A"
        return try {
            Color(android.graphics.Color.parseColor(hexColor))
        } catch (e: Exception) {
            Color.Transparent
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C14)) // Pitch dark arcade background
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Retro CRT Arcade Header ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF007F), Color(0xFF00F0FF))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00FFCC))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF00FFCC))
                                )
                                Text(
                                    text = "BILINGUAL RETRO GGUF LOADED",
                                    color = Color(0xFF00FFCC),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            text = "بكسل ريترو ✦ RETRO LAB",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF007F)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "محاكي وألعاب الطيبين ✦ NES Game Boy Studio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اكتب باللغة العربية أو الإنجليزية لتوليد بكسل آرت حقيقي وجبار",
                        fontSize = 11.sp,
                        color = Color(0xFF8A8A9E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // --- 2. CRT Screen & Pixel Canvas ---
        item {
            val currentAnimation = (uiState as? UiState.Success)?.data
            val palette = currentAnimation?.palette ?: listOf("#0B0F19", "#4F46E5", "#06B6D4", "#F43F5E", "#EC4899", "#FFFFFF")
            val frames = currentAnimation?.frames ?: listOf("0".repeat(256))
            val activeFrameIndex = currentFrameIndex.coerceIn(0, frames.size - 1)
            val frameString = frames.getOrNull(activeFrameIndex) ?: "0".repeat(256)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = Color(0xFF00F0FF).copy(alpha = glowAlpha),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10101C)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "شاشة التوليد الكلاسيكية ✦ CRT MONITOR",
                        color = Color(0xFF00F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- The 16x16 Pixel Canvas Block with scanline overlays ---
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(3.dp, Color(0xFF00F0FF), RoundedCornerShape(12.dp))
                            .drawWithContent {
                                drawContent()
                                // Simulate CRT Retro Scanlines Overlay
                                val scanlineHeight = 4f
                                val numLines = (size.height / scanlineHeight).toInt()
                                for (i in 0 until numLines) {
                                    if (i % 2 == 0) {
                                        drawLine(
                                            color = Color.Black.copy(alpha = 0.15f),
                                            start = Offset(0f, i * scanlineHeight),
                                            end = Offset(size.width, i * scanlineHeight),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }
                            .testTag("pixel_canvas")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cellSize = size.width / 16f
                            for (row in 0 until 16) {
                                for (col in 0 until 16) {
                                    val charIdx = row * 16 + col
                                    val char = if (charIdx < frameString.length) frameString[charIdx] else '0'
                                    val cellColor = getColorForChar(char, palette)

                                    drawRect(
                                        color = cellColor,
                                        topLeft = Offset(col * cellSize, row * cellSize),
                                        size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                                    )
                                }
                            }

                            // Subtle retro grid lines
                            if (showGridLines) {
                                for (i in 1 until 16) {
                                    val offset = i * cellSize
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(offset, 0f),
                                        end = Offset(offset, size.height),
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(0f, offset),
                                        end = Offset(size.width, offset),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CRT Status and toggle network
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "شبكة الرسم مفعّلة",
                                color = Color(0xFF8A8A9E),
                                fontSize = 11.sp
                            )
                            Switch(
                                checked = showGridLines,
                                onCheckedChange = { showGridLines = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00F0FF),
                                    checkedTrackColor = Color(0xFF00F0FF).copy(alpha = 0.3f)
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1B1B30),
                        ) {
                            Text(
                                text = "فريم: ${activeFrameIndex + 1} / ${frames.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. NES Console Controller (D-Pad & Buttons) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFF007F).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "جهاز التحكم ✦ FAMICOM CONTROLLER",
                        color = Color(0xFFFF007F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Classic D-Pad Style Controls (Left/Right)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.stopAnimationPlayback()
                                    viewModel.setCurrentFrameIndex((currentFrameIndex - 1).coerceAtLeast(0))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24243C)),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "السابق", tint = Color.White)
                            }

                            Text(
                                text = "D-PAD",
                                color = Color(0xFF8A8A9E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = {
                                    viewModel.stopAnimationPlayback()
                                    viewModel.setCurrentFrameIndex(currentFrameIndex + 1)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24243C)),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "التالي", tint = Color.White)
                            }
                        }

                        // Play/Pause Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = { viewModel.togglePlayback() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (isPlaying) "❚❚" else "▶",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text("A-START", color = Color(0xFFFF007F), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = { viewModel.stopAnimationPlayback() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "توقف", tint = Color.Black)
                                }
                                Text("B-STOP", color = Color(0xFF00F0FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Inputs & Bilingual Prompts config ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "لوحة التوجيه الثنائية ✦ LANGUAGE CENTER",
                        color = Color(0xFF00F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = promptState,
                        onValueChange = { viewModel.prompt.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_input_field"),
                        label = { Text("اكتب وصف اللعبة باللغة العربية أو الإنجليزية", color = Color(0xFF8A8A9E)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0xFF24243C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Presets
                    Text(
                        text = "عناصر مجهزة سريعة ✦ QUICK PRESETS:",
                        color = Color(0xFF8A8A9E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bilingualRetroRecipes.forEach { (label, fullPrompt) ->
                            Surface(
                                modifier = Modifier.clickable {
                                    viewModel.prompt.value = fullPrompt
                                    Toast.makeText(context, "تم اختيار: $label", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1D1D35),
                                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Generation button
                    Button(
                        onClick = { viewModel.generatePixelArt() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFF007F), Color(0xFF00F0FF))
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState is UiState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "توليد صورة ريترو فخمة ✦ GENERATE",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
