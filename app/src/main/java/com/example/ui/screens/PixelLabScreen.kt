package com.example.ui.screens

import android.widget.Toast
import com.example.R
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

    // Set up standard creative recipes for pixel art prompting
    val promptRecipes = listOf(
        Pair("🔥 مكعب ناري متفجر", "مكعب ناري متفجر يتلاشى تدريجيا من السطوع إلى الرماد"),
        Pair("🚀 سفينة فضاء عملاقة", "سفينة فضاء بكسل تدور محركاتها وتطلق طاقة ليزر متحركة"),
        Pair("🪙 عملة ذهبية دوارة", "عملة ذهبية للألعاب تدور حول نفسها في دورة متكاملة"),
        Pair("👻 شبح بكسل كلاسيكي", "شبح صغير يطير للأعلى والأسفل ويحرك ذيله الصغير"),
        Pair("💧 قطرة ماء متساقطة", "قطرة ماء زرقاء تسقط ثم تصطدم بالأرض وتنتشر كالبقع")
    )

    // Set up standard creative recipes for real image generation prompting
    val hdRecipes = listOf(
        Pair("🌌 ثقب أسود كوني", "صورة فوتوغرافية مذهلة لثقب أسود يلتهم مجرة حلزونية، ألوان نيون ساطعة، دقة عالية سينمائية"),
        Pair("🦁 أسد محارب مهيب", "بورتريه مهيب لملك غابة محارب يرتدي درعاً ذهبياً أسطورياً بالكامل، نمط فانتازيا ملحمي"),
        Pair("🌸 قلعة يابانية", "مبنى قلعة يابانية تقليدية تحوطها أشجار أزهار الكرز، بيئة هادئة ودافئة مع جودة سينمائية"),
        Pair("🏎️ سيارة مستقبلية", "سيارة سباق رياضية كهربائية متطورة تسير في شوارع طوكيو الممطرة ليلاً تحت ناطحات السحاب المضيئة بالنيون"),
        Pair("🧙 ساحر حكيم", "ساحر أسطوري يقرأ كتاب تعاويذ سحرية متوهجة تطفو في الهواء، جودة ثلاثية الأبعاد خيالية")
    )

    // Helper functions for parsing color strings safely
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
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Premium Header (Arabic Workstation Style) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ArtisticPrimary.copy(alpha = 0.2f))
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
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
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
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "Vulkan Connection Active",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            text = "بكسل لاب ✦ Pixel Lab",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ArtisticPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "محطة توليد وتحريك فن البكسل الذكية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اكتب مرادك وسيقوم الذكاء الاصطناعي ببناء إطارات الحركة المتناسقة فوراً",
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // --- 2. Interactive Main Studio Display ---
        item {
            val currentAnimation = (uiState as? UiState.Success)?.data
            val palette = currentAnimation?.palette ?: listOf("#1e1b4b", "#f97316", "#facc15", "#ef4444", "#3b82f6", "#ffffff")
            val frames = currentAnimation?.frames ?: listOf("0".repeat(256))
            val activeFrameIndex = currentFrameIndex.coerceIn(0, frames.size - 1)
            val frameString = frames.getOrNull(activeFrameIndex) ?: "0".repeat(256)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ArtisticSecondary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRealImageState) {
                        Text(
                            text = "منصة توليد الصور الواقعية ✦ AI HD Studio",
                            color = ArtisticSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (uiState is UiState.Error) "فشل التوليد. يرجى مراجعة تفاصيل الخطأ بالأسفل." else "توليد صور واقعية ثلاثية الأبعاد بدقة فائقة باستخدام نموذج الذكاء الاصطناعي",
                            color = MutedText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- The Real HD Image Canvas ---
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(2.dp, ArtisticSecondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState is UiState.Loading) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = ArtisticSecondary, modifier = Modifier.size(32.dp))
                                    Text(
                                        text = "جاري الاتصال بنموذج التوليد...",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (uiState is UiState.RealImageSuccess) {
                                val base64Data = (uiState as UiState.RealImageSuccess).base64Data
                                Base64Image(
                                    base64Str = base64Data,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = null,
                                        tint = MutedText,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "الاستوديو جاهز للبناء الفني",
                                        color = LightText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "اكتب مرادك بالأسفل ثم اضغط زر التوليد السحابي الملون بمحرك Imagen",
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = currentAnimation?.title ?: "إطار العمل البدئي",
                            color = ArtisticSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentAnimation?.description ?: "لم يتم توليد أي رسوم متحركة مخصصة بعد. استخدم لوحة التحكم بالأسفل للتوليد.",
                            color = MutedText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- The 16x16 Pixel Canvas Block ---
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(2.dp, ArtisticPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                            topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                            size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f) // overlapping prevents minor rendering lines
                                        )
                                    }
                                }

                                // Subtle retro grid lines
                                if (showGridLines) {
                                    for (i in 1 until 16) {
                                        val offset = i * cellSize
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.12f),
                                            start = androidx.compose.ui.geometry.Offset(offset, 0f),
                                            end = androidx.compose.ui.geometry.Offset(offset, size.height),
                                            strokeWidth = 1f
                                        )
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.12f),
                                            start = androidx.compose.ui.geometry.Offset(0f, offset),
                                            end = androidx.compose.ui.geometry.Offset(size.width, offset),
                                            strokeWidth = 1f
                                        )
                                    }
                                }
                            }
                        }

                        // --- Advanced Settings & Model Center ---
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedSettings = !showAdvancedSettings }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showAdvancedSettings) "إخفاء الخيارات المتقدمة" else "عرض الخيارات المتقدمة ⚙️",
                                color = ArtisticTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showAdvancedSettings) Icons.Default.KeyboardArrowUp else Icons.Default.Settings,
                                contentDescription = null,
                                tint = ArtisticTertiary,
                                modifier = Modifier.size(16.dp).padding(start = 4.dp)
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedSettings) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkCard.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "مركز النماذج والتحكم الدقيق",
                                    color = LightText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Placeholder for iteration slider and seed control
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("دقة المعالجة (Iterations):", color = MutedText, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                    Text("قيمة محسنة", color = ArtisticPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("الحالة:", color = MutedText, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                                    ) {
                                        Text("جاهز للاستخدام", color = Color(0xFF10B981), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (!isRealImageState) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Canvas Utility Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (showGridLines) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (showGridLines) "شبكة الرسم مفعّلة" else "الشبكة مخفية",
                                    color = MutedText,
                                    fontSize = 11.sp
                                )
                                Switch(
                                    checked = showGridLines,
                                    onCheckedChange = { showGridLines = it },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkCard,
                            ) {
                                Text(
                                    text = "الفريم: ${activeFrameIndex + 1} / ${frames.size}",
                                    color = LightText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isLocalHDMode) {
                            Text(
                                text = "اختر النموذج المفتوح المستهدف:",
                                color = ArtisticTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Pair(com.example.engine.LocalHDImageEngine.ModelArchitecture.STABLE_DIFFUSION_V1_5, "Stable Diffusion v1.5"),
                                    Pair(com.example.engine.LocalHDImageEngine.ModelArchitecture.FLUX_1_SCHNELL, "Flux.1 Schnell")
                                ).forEach { (arch, label) ->
                                    val isSelected = selectedHDModel == arch
                                    Surface(
                                        modifier = Modifier.clickable { viewModel.selectedHDModel.value = arch },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ArtisticPrimary.copy(alpha = 0.2f) else DarkCard,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) ArtisticPrimary else Color.White.copy(alpha = 0.05f)
                                        )
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else LightText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // --- Cinematic Playback Console Row ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.stopAnimationPlayback()
                                    val prev = if (activeFrameIndex == 0) frames.size - 1 else activeFrameIndex - 1
                                    viewModel.setCurrentFrameIndex(prev)
                                },
                                modifier = Modifier.testTag("prev_frame_button")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "السابق", tint = LightText)
                            }

                            IconButton(
                                onClick = { viewModel.togglePlayback() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(ArtisticPrimary)
                                    .testTag("play_pause_button")
                            ) {
                                Text(
                                    text = if (isPlaying) "❚❚" else "▶",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = if (isPlaying) 0.dp else 4.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.stopAnimationPlayback()
                                    val next = (activeFrameIndex + 1) % frames.size
                                    viewModel.setCurrentFrameIndex(next)
                                },
                                modifier = Modifier.testTag("next_frame_button")
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "التالي", tint = LightText)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Speed Controllers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سرعة التحريك:",
                                color = LightText,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )

                            listOf(
                                Triple("سريع", 150L, Color(0xFF10B981)),
                                Triple("متوسط", 250L, ArtisticPrimary),
                                Triple("بطيء", 450L, ArtisticSecondary)
                            ).forEach { (label, speed, activeCol) ->
                                val isSelected = playbackSpeedMs == speed
                                Button(
                                    onClick = { viewModel.updatePlaybackSpeed(speed) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) activeCol else DarkCard,
                                        contentColor = if (isSelected) Color.White else MutedText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Prompt Configuration & Control Box ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ArtisticPrimary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "محرك صياغة الأفكار ✦ Simulation Studio",
                        color = ArtisticPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Mode Selection Tab (Pixel retro vs HD photorealistic image generation)
                    Text(
                        text = "اختر نمط التوليد والإنتاج الفني:",
                        color = ArtisticSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCard)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { 
                                viewModel.isRealImageMode.value = false
                                viewModel.isCloudEnabled.value = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRealImageState) ArtisticPrimary else Color.Transparent,
                                contentColor = if (!isRealImageState) Color.White else MutedText
                             ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("محاكي فن البكسل (Pixel Art)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                viewModel.isRealImageMode.value = true
                                viewModel.isCloudEnabled.value = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRealImageState) ArtisticSecondary else Color.Transparent,
                                contentColor = if (isRealImageState) Color.White else MutedText
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("توليد صور واقعية (HD Art)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isRealImageState) {
                        // NEW: operational mode switcher
                        Text(
                            text = "طريقة التشغيل والموديل المستهدف:",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkCard)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.isCloudEnabled.value = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isCloudEnabledState) ArtisticPrimary else Color.Transparent,
                                    contentColor = if (!isCloudEnabledState) Color.White else MutedText
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (!isCloudEnabledState) Color.White else Color.Gray)
                                    )
                                    Text("محلي بالكامل (بدون نت)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.isCloudEnabled.value = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCloudEnabledState) ArtisticSecondary else Color.Transparent,
                                    contentColor = if (isCloudEnabledState) Color.White else MutedText
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isCloudEnabledState) Color.White else Color.Gray)
                                    )
                                    Text("سحابي Gemini API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Show local models chips selector if Cloud is disabled (as default)
                        if (!isCloudEnabledState) {
                            Text(
                                text = "اختر أحد نماذج التوليد المحلية ومفتوحة المصدر:",
                                color = ArtisticTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                localModelsList.forEach { model ->
                                    val isSelected = selectedLocalModelState == model
                                    val (modelTitle, modelIcon, defaultPrompt) = when (model) {
                                        com.example.engine.LocalPixelEngine.MODEL_DIFFUSION -> Triple("نموذج انتشار البكسل LPD", "⚛️", "بطل بكسل خارق يحمل سيفاً مضيئاً يلمع")
                                        com.example.engine.LocalPixelEngine.MODEL_NEURAL_CPPN -> Triple("الشبكة العصبية CPPN", "🧠", "بنية عصبية عميقة")
                                        com.example.engine.LocalPixelEngine.MODEL_FIRE -> Triple("لهب ناري", "🔥", "طاقة لهب نارية جبارة")
                                        com.example.engine.LocalPixelEngine.MODEL_FLUID -> Triple("مياه وسوائل", "💧", "قطرة ماء تسقط وتصطدم")
                                        com.example.engine.LocalPixelEngine.MODEL_SPIN3D -> Triple("دوران ثلاثي الأبعاد", "🪙", "عملة ذهبية تدور حول محورها")
                                        com.example.engine.LocalPixelEngine.MODEL_CELLULAR -> Triple("نمو خلوي", "🧬", "نمو نسيج خلوي ذكي")
                                        com.example.engine.LocalPixelEngine.MODEL_PLASMA -> Triple("موجات البلازما", "⚡", "أمواج ومجال بلازما طاقة")
                                        com.example.engine.LocalPixelEngine.MODEL_GRAVITY -> Triple("جاذبية كوكبية", "🌌", "دوران فلكي في حقل مغناطيسي")
                                        com.example.engine.LocalPixelEngine.MODEL_WALKER -> Triple("كائن مشي حركي", "🚶", "شخصية تسير بحركة كينماتيكية")
                                        else -> Triple("مجهول", "🌀", "")
                                    }

                                    Surface(
                                        modifier = Modifier.clickable {
                                            viewModel.selectedLocalModel.value = model
                                            viewModel.prompt.value = defaultPrompt
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ArtisticPrimary.copy(alpha = 0.2f) else DarkCard,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) ArtisticPrimary else Color.White.copy(alpha = 0.05f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = modelIcon, fontSize = 11.sp)
                                            Text(
                                                text = modelTitle,
                                                color = if (isSelected) Color.White else LightText,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Operational mode switcher for HD Art
                        Text(
                            text = "طريقة التشغيل والموديل المستهدف:",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkCard)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.isLocalHDMode.value = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLocalHDMode) ArtisticPrimary else Color.Transparent,
                                    contentColor = if (isLocalHDMode) Color.White else MutedText
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("محلي (MediaPipe)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.isLocalHDMode.value = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isLocalHDMode) ArtisticSecondary else Color.Transparent,
                                    contentColor = if (!isLocalHDMode) Color.White else MutedText
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("سحابي (Imagen)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Informational row for HD photorealistic generation
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = (if (isLocalHDMode) ArtisticPrimary else ArtisticSecondary).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, (if (isLocalHDMode) ArtisticPrimary else ArtisticSecondary).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(if (isLocalHDMode) "🏠" else "☁️", fontSize = 16.sp)
                                    Text(
                                        text = if (isLocalHDMode) context.getString(R.string.local_hd_title) else context.getString(R.string.cloud_hd_title),
                                        color = LightText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isLocalHDMode) context.getString(R.string.local_hd_desc) else "التوليد الواقعي بدقة عالية يستخدم نموذج Imagen السحابي للحصول على صور فوتوغرافية ولوحات فنية مذهلة مباشرة من خوادم الذكاء الاصطناعي.",
                                    color = LightText,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                if (isLocalHDMode) {
                                    Text(
                                        text = context.getString(R.string.local_hd_requirement),
                                        color = ArtisticTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = promptState,
                        onValueChange = { viewModel.prompt.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_input_field"),
                        label = { Text(if (isRealImageState) "يرجى كتابة وصف الصورة الواقعية بالتفصيل" else "عن ماذا يعبر الرسم المتحرك؟", color = MutedText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArtisticPrimary,
                            unfocusedBorderColor = DarkCard,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (!isRealImageState) {
                        OutlinedTextField(
                            value = paletteHintState,
                            onValueChange = { viewModel.paletteHint.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("المخطط اللوني المفضل (اختياري)", color = MutedText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ArtisticPrimary,
                                unfocusedBorderColor = DarkCard,
                                focusedTextColor = LightText,
                                unfocusedTextColor = LightText
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Frame length slider control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "عدد الإطارات: $frameCountState",
                                color = LightText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(100.dp)
                            )
                            Slider(
                                value = frameCountState.toFloat(),
                                onValueChange = { viewModel.frameCount.value = it.toInt() },
                                valueRange = 4f..8f,
                                steps = 3,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = ArtisticPrimary,
                                    activeTrackColor = ArtisticPrimary,
                                    inactiveTrackColor = DarkCard
                                )
                            )
                        }
                    }

                    // --- Prompt Templates/Recipes Gallery ---
                    Text(
                        text = "أفكار ووصفات جاهزة للتجربة:",
                        color = MutedText,
                        fontSize = 11.sp,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeRecipes = if (isRealImageState) hdRecipes else promptRecipes
                        activeRecipes.forEach { (recipeLabel, recipeFull) ->
                            Surface(
                                modifier = Modifier.clickable {
                                    viewModel.prompt.value = recipeFull
                                    Toast.makeText(context, if (isRealImageState) "تم تحديد الوصف الواقعي" else "تم تحديد وصفة البكسل", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = DarkCard,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Text(
                                    text = recipeLabel,
                                    color = LightText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Artistic Generation Button
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
                                        colors = if (isRealImageState) listOf(ArtisticSecondary, ArtisticTertiary) else listOf(ArtisticPrimary, ArtisticSecondary)
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
                                        text = if (isRealImageState) "توليد صورة واقعية مذهلة ✦ Generate HD" else "توليد بالذكاء الاصطناعي ✦ Generate Retro",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // --- Processing UI State Overlay Feedback ---
                    AnimatedVisibility(visible = uiState is UiState.Error) {
                        val errMsg = (uiState as? UiState.Error)?.message ?: ""
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "⚠️ حدث خطأ: $errMsg",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Interactive Timeline Frame Strip Carousel ---
        if (!isRealImageState) {
            item {
                val currentAnimation = (uiState as? UiState.Success)?.data
                val frames = currentAnimation?.frames ?: listOf("0".repeat(256))
                val palette = currentAnimation?.palette ?: listOf("#1e1b4b", "#f97316", "#facc15", "#ef4444", "#3b82f6", "#ffffff")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "شريط السيناريو الزمني (Frames Timeline Scroll)",
                            color = LightText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(frames.size) { index ->
                                val fString = frames[index]
                                val isSelected = currentFrameIndex == index

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) ArtisticSecondary else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.stopAnimationPlayback()
                                            viewModel.setCurrentFrameIndex(index)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                        val cellSize = size.width / 16f
                                        for (row in 0 until 16) {
                                            for (col in 0 until 16) {
                                                val cIdx = row * 16 + col
                                                val char = if (cIdx < fString.length) fString[cIdx] else '0'
                                                val c = getColorForChar(char, palette)
                                                drawRect(
                                                    color = c,
                                                    topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                                )
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(topStart = 6.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Studio History Gallery List ---
        item {
            if (historyList.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مكتبة المحفوظات الاستوديو (${historyList.size})",
                        color = ArtisticTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    historyList.forEach { histItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadFromHistory(histItem)
                                    Toast.makeText(context, "تم تحميل الرسوم: ${histItem.title}", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Miniature Static rendering
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black)
                                ) {
                                    val fString = histItem.frames.firstOrNull() ?: "0".repeat(256)
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cellSize = size.width / 16f
                                        for (row in 0 until 16) {
                                            for (col in 0 until 16) {
                                                val charIdx = row * 16 + col
                                                val char = if (charIdx < fString.length) fString[charIdx] else '0'
                                                val c = getColorForChar(char, histItem.palette)
                                                drawRect(
                                                    color = c,
                                                    topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = histItem.title,
                                        color = LightText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = histItem.description,
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Badge(
                                    containerColor = ArtisticTertiary.copy(alpha = 0.15f),
                                    contentColor = ArtisticTertiary
                                ) {
                                    Text(
                                        text = "${histItem.frameCount} فريمز",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

// Extension to safely dynamically implement non-compiled modifier elements
private fun Modifier.scale(scale: Float) = this.then(
    Modifier.size(height = (48 * scale).dp, width = (80 * scale).dp)
)

@Composable
fun Base64Image(base64Str: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64Str) {
        try {
            val decodedString = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Generated Real Image",
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text("فشل فك تشفير الصورة", color = Color.White, fontSize = 11.sp)
        }
    }
}
