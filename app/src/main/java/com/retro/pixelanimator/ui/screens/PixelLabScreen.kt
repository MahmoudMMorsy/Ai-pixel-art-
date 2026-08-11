package com.retro.pixelanimator.ui.screens

import android.net.Uri
import android.widget.Toast
import com.retro.pixelanimator.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.retro.pixelanimator.engine.PixelArtAnimationResponse
import com.retro.pixelanimator.ui.theme.*
import com.retro.pixelanimator.ui.viewmodel.PixelAnimatorViewModel
import com.retro.pixelanimator.ui.viewmodel.UiState
import android.graphics.BitmapFactory
import android.util.Base64

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

    // Sideload model states
    val isModelImported by viewModel.isModelImported.collectAsState()
    val importedModelName by viewModel.importedModelName.collectAsState()
    val importedModelSize by viewModel.importedModelSize.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importError by viewModel.importError.collectAsState()

    var showGridLines by remember { mutableStateOf(true) }

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

    // Activity Result Launcher for importing `.gguf` file
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importGgufModel(uri)
        }
    }

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
                            color = if (isModelImported) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color(0xFFFF007F).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isModelImported) Color(0xFF00FFCC) else Color(0xFFFF007F))
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
                                        .background(if (isModelImported) Color(0xFF00FFCC) else Color(0xFFFF007F))
                                )
                                Text(
                                    text = if (isModelImported) "GGUF MODEL LOADED" else "NO MODEL IMPORTED",
                                    color = if (isModelImported) Color(0xFF00FFCC) else Color(0xFFFF007F),
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

        // --- 2. Model Center (مركز النماذج) for Import & Status Info ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isModelImported) Color(0xFF00FFCC).copy(alpha = 0.6f) else Color(0xFFFF3366).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isModelImported) Color(0xFF00FFCC) else Color(0xFFFF3366)
                        )
                        Text(
                            text = "مركز إدارة النماذج المحلية ✦ MODEL CENTER",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    HorizontalDivider(color = Color(0xFF24243C))

                    if (isImporting) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                            Text(
                                text = "جاري استيراد وتهيئة ملف الـ GGUF محلياً... يرجى الانتظار",
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        if (isModelImported) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("اسم النموذج:", color = Color(0xFF8A8A9E), fontSize = 11.sp)
                                    Text(importedModelName ?: "غير معروف", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("حجم الملف:", color = Color(0xFF8A8A9E), fontSize = 11.sp)
                                    Text(importedModelSize ?: "0 MB", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("حالة التشغيل:", color = Color(0xFF8A8A9E), fontSize = 11.sp)
                                    Text("جاهز ومكتمل (Offline Ready)", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { viewModel.deleteImportedModel() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حذف النموذج المستورد وتحرير المساحة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "التطبيق خفيف وحر! لم يتم تجميع أي نموذج ضخم بداخله. يرجى استيراد أي نموذج GGUF خاص بك لتفعيل التوليد الذكي بالكامل محلياً بدون إنترنت.",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { modelPickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("استيراد نموذج GGUF من جهازك", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    importError?.let { err ->
                        Text(
                            text = err,
                            color = Color(0xFFFF3366),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // --- 3. Mode Toggles (Pixel vs Real HD Image Mode) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isRealImageState,
                        onClick = { viewModel.isRealImageMode.value = false },
                        label = { Text("نمط ألعاب البكسل 🎮", color = Color.White) }
                    )
                    FilterChip(
                        selected = isRealImageState,
                        onClick = { viewModel.isRealImageMode.value = true },
                        label = { Text("توليد صور واقعية 📷", color = Color.White) }
                    )
                }
            }
        }

        // --- 4. CRT Screen & Pixel Canvas ---
        item {
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
                        text = "شاشة العرض الكلاسيكية ✦ MONITOR SCREEN",
                        color = Color(0xFF00F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Screen rendering depending on Active Mode
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(3.dp, Color(0xFF00F0FF), RoundedCornerShape(12.dp))
                            .drawWithContent {
                                drawContent()
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
                    ) {
                        if (isRealImageState) {
                            // Render GGUF real image output
                            if (uiState is UiState.Loading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFFF007F))
                                }
                            } else if (realImageBase64 != null) {
                                val imageBytes = Base64.decode(realImageBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("شاشة خاملة ✦ اكتب وصفك واضغط توليد", color = Color(0xFF8A8A9E), fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            // Retro pixel canvas
                            val currentAnimation = (uiState as? UiState.Success)?.data
                            val palette = currentAnimation?.palette ?: listOf("#0B0F19", "#4F46E5", "#06B6D4", "#F43F5E", "#EC4899", "#FFFFFF")
                            val frames = currentAnimation?.frames ?: listOf("0".repeat(256))
                            val activeFrameIndex = currentFrameIndex.coerceIn(0, frames.size - 1)
                            val frameString = frames.getOrNull(activeFrameIndex) ?: "0".repeat(256)

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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isRealImageState) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("شبكة الرسم مفعّلة", color = Color(0xFF8A8A9E), fontSize = 11.sp)
                                Switch(
                                    checked = showGridLines,
                                    onCheckedChange = { showGridLines = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00F0FF),
                                        checkedTrackColor = Color(0xFF00F0FF).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Inputs & Prompts Config ---
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
                        text = "لوحة التوجيه والتحكم ✦ CONTROL CENTER",
                        color = Color(0xFF00F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = promptState,
                        onValueChange = { viewModel.prompt.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("اكتب الوصف أو التوجيه هنا", color = Color(0xFF8A8A9E)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0xFF24243C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التوليد المحلي عبر الـ NPU/GGUF:", color = Color(0xFF8A8A9E), fontSize = 11.sp)
                        Switch(
                            checked = isLocalHDMode,
                            onCheckedChange = { viewModel.isLocalHDMode.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00FFCC)
                            )
                        )
                    }

                    Button(
                        onClick = { viewModel.generatePixelArt() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
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
                                Text("بدء عملية التوليد الفائقة ✦ GENERATE", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
