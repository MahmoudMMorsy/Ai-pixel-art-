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

    // Pixelator States
    val isImagePixelatorModeState by viewModel.isImagePixelatorMode.collectAsState()

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

    // Activity Result Launcher for importing multiple images to pixelate
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.selectPixelatorImages(uris)
        }
    }

    // Decode generated base64 image
    val decodedBitmap = remember(realImageBase64) {
        if (!realImageBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = android.util.Base64.decode(realImageBase64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
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

        // --- Mode Selector Tab Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141424), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    Triple("🎮 بكسل 16x16", false, false),
                    Triple("✨ صور ذكاء AI", true, false),
                    Triple("🎨 محول بكسل 256", false, true)
                )

                modes.forEach { (label, isReal, isPixelator) ->
                    val isSelected = (isRealImageState == isReal && isImagePixelatorModeState == isPixelator)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFF007F) else Color.Transparent)
                            .clickable {
                                viewModel.stopAnimationPlayback()
                                viewModel.isRealImageMode.value = isReal
                                viewModel.isImagePixelatorMode.value = isPixelator
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF8A8A9E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

                    Divider(color = Color(0xFF24243C))

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

        // --- 3. CRT Screen & Pixel Canvas ---
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
                            .border(
                                width = 3.dp,
                                color = if (isImagePixelatorModeState) Color(0xFF00FFCC) else Color(0xFF00F0FF),
                                shape = RoundedCornerShape(12.dp)
                            )
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
                        if (isImagePixelatorModeState) {
                            val pixelatedBmp by viewModel.pixelatedBitmap.collectAsState()
                            if (pixelatedBmp != null) {
                                Image(
                                    bitmap = pixelatedBmp!!.asImageBitmap(),
                                    contentDescription = "Retro Pixelated Image",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "الرجاء اختيار صورة للبدء 🎨\n(حجم 256x256 بكسل ولون 48)",
                                        color = Color(0xFF8A8A9E),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else if (isRealImageState) {
                            if (decodedBitmap != null) {
                                Image(
                                    bitmap = decodedBitmap.asImageBitmap(),
                                    contentDescription = "AI Generated Real Image",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد صورة مولدة بعد ✨\nاكتب وصفاً واضغط توليد",
                                        color = Color(0xFF8A8A9E),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
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

        if (isImagePixelatorModeState) {
            // --- Retro Pixelator Mode Control Panel ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "محول بكسل وتأثيرات ريترو ✦ RETRO PIXELATOR PRO",
                            color = Color(0xFF00FFCC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Multiple Image Picker Button
                        val urisState by viewModel.pixelatorImagesUris.collectAsState()
                        val isPixelatingState by viewModel.isPixelating.collectAsState()
                        val pixelatorErrorState by viewModel.pixelatorError.collectAsState()

                        Button(
                            onClick = { multiplePhotoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (urisState.isEmpty()) "تحميل صور من المعرض (اختر حتى 3 صور)" else "تغيير الصور المحددة (${urisState.size} صور)",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (urisState.isNotEmpty()) {
                            Text(
                                text = "الصور المختارة للدمج والتحويل:",
                                color = Color(0xFF8A8A9E),
                                fontSize = 11.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                urisState.forEachIndexed { index, uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(8.dp))
                                            .background(Color(0xFF24243C)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("#${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Blend strength slider if more than 1 image
                            if (urisState.size > 1) {
                                val blendStrengthVal by viewModel.pixelatorBlendStrength.collectAsState()
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("قوة الدمج والدمج اللوني:", color = Color.White, fontSize = 11.sp)
                                        Text(String.format("%.2f", blendStrengthVal), color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = blendStrengthVal,
                                        onValueChange = { viewModel.setPixelatorBlendStrength(it) },
                                        valueRange = 0.1f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF00FFCC),
                                            activeTrackColor = Color(0xFF00FFCC)
                                        )
                                    )
                                }
                            }
                        }

                        // Grid Size Selector
                        val gridSizeVal by viewModel.pixelatorGridSize.collectAsState()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("دقة وكتل شبكة البكسل (Grid Size):", color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(16, 32, 64, 128, 256).forEach { size ->
                                    val isSel = (gridSizeVal == size)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF00FFCC) else Color(0xFF1D1D35))
                                            .clickable { viewModel.setPixelatorGridSize(size) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${size}x${size}",
                                            color = if (isSel) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Color Count Selector
                        val colorCountVal by viewModel.pixelatorColorCount.collectAsState()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("عدد الألوان في اللوحة (Color Count):", color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(8, 16, 24, 32, 48, 64).forEach { count ->
                                    val isSel = (colorCountVal == count)
                                    val isRecommended = (count == 48)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(
                                                width = if (isRecommended) 1.dp else 0.dp,
                                                color = if (isRecommended) Color(0xFFFFD700) else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .background(if (isSel) Color(0xFF00FFCC) else if (isRecommended) Color(0xFF2C2C14) else Color(0xFF1D1D35))
                                            .clickable { viewModel.setPixelatorColorCount(count) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$count لون",
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isRecommended) {
                                                Text(
                                                    text = "موصى به",
                                                    color = if (isSel) Color.Black else Color(0xFFFFD700),
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Arabic Text Overlay Field
                        val arabicTextVal by viewModel.pixelatorArabicText.collectAsState()
                        OutlinedTextField(
                            value = arabicTextVal,
                            onValueChange = { viewModel.setPixelatorArabicText(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("كتابة نصوص عربية سليمة على الصورة", color = Color(0xFF8A8A9E)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FFCC),
                                unfocusedBorderColor = Color(0xFF24243C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Export/Save Button
                        Button(
                            onClick = {
                                val msg = viewModel.exportPixelatedImage(context)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وتصدير الصورة الكلاسيكية ✦ EXPORT ART", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Loading/Processing Status
                        if (isPixelatingState) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري معالجة الكتل وتطبيق لوحة الـ 48 لون...", color = Color(0xFF00FFCC), fontSize = 11.sp)
                            }
                        }

                        pixelatorErrorState?.let { err ->
                            Text(text = err, color = Color.Red, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        } else if (isRealImageState) {
            // --- AI HD Engine Mode Control Panel ---
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
                            text = "محرك التوليد فائق الدقة ✦ AI HD ENGINE",
                            color = Color(0xFF00F0FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Model Architecture / Engine Selectors
                        val isCloudVal by viewModel.isCloudEnabled.collectAsState()
                        val isLocalHDModeVal by viewModel.isLocalHDMode.collectAsState()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.isCloudEnabled.value = true; viewModel.isLocalHDMode.value = false },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isCloudVal) Color(0xFF00F0FF) else Color(0xFF1D1D35)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("سحابي (Gemini API)", color = if (isCloudVal) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.isCloudEnabled.value = false; viewModel.isLocalHDMode.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isLocalHDModeVal) Color(0xFF00F0FF) else Color(0xFF1D1D35)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("محلي (SD/Flux GGUF)", color = if (isLocalHDModeVal) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = promptState,
                            onValueChange = { viewModel.prompt.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("اكتب الوصف التفصيلي لتوليد صورة فائقة الجودة", color = Color(0xFF8A8A9E)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0xFF24243C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = { viewModel.generateRealImage() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
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
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState is UiState.Loading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("توليد صورة واقعية ✦ GENERATE REAL IMAGE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- Classic Animation Mode Controls ---
            // --- 4. NES Console Controller (D-Pad & Buttons) ---
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

            // --- 5. Inputs & Bilingual Prompts config ---
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
}
