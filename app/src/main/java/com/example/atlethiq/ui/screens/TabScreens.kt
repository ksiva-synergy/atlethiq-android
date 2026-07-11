package com.example.atlethiq.ui.screens

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atlethiq.data.LogHistoryRow
import com.example.atlethiq.data.SourceSummary
import com.example.atlethiq.data.TrendsChartData
import com.example.atlethiq.data.models.DailySnapshot
import com.example.atlethiq.domain.dedup.PlaceholderDedupConfig
import com.example.atlethiq.theme.*
import com.example.atlethiq.theme.Text as ColorText
import com.example.atlethiq.ui.viewmodel.TodayViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.core.common.shape.Shape as VicoShape
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.Scroll
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.example.atlethiq.data.DataRepository
import io.github.jan.supabase.SupabaseClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class FactorRowData(
    val name: String,
    val direction: String,
    val value: String,
    val baseline: String,
    val source: String,
    val directionColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreen(
    title: String,
    isToday: Boolean = false,
    todayViewModel: TodayViewModel,
    content: @Composable () -> Unit
) {
    val dateString by todayViewModel.dateString.collectAsState()
    val formattedDate = remember(dateString) {
        try {
            val date = LocalDate.parse(dateString)
            date.format(DateTimeFormatter.ofPattern("EEE dd MMM", Locale.US)).uppercase()
        } catch (e: Exception) {
            dateString
        }
    }
    val debugMode by todayViewModel.debugMode.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (isToday) {
                        Text(
                            text = formattedDate,
                            style = Typography.labelLarge,
                            color = Muted
                        )
                    } else {
                        Text(
                            text = title,
                            style = Typography.headlineMedium.copy(fontSize = 20.sp),
                            color = ColorText
                        )
                    }
                },
                navigationIcon = {
                    Text(
                        text = "A",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        color = SignalLime,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { todayViewModel.toggleDebugMode() }) {
                        Text(
                            text = "🧪", // flask beaker icon
                            fontSize = 20.sp,
                            modifier = Modifier.alpha(if (debugMode) 1f else 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Base,
                    titleContentColor = ColorText
                )
            )
        },
        containerColor = Base
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (debugMode) Modifier.border(1.dp, SignalLime) else Modifier
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            content()
            
            // Temporary day picker dialog when clicking flask or debug picker in header
            var showDayPicker by remember { mutableStateOf(false) }
            val currentDayIndex by todayViewModel.dayIndex.collectAsState()
            
            if (debugMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // The daily-loop mechanism: advance the frontier one day and let the WorkManager
                    // job detect the new snapshot and fire the notification (no direct-post shortcut).
                    Button(
                        onClick = { todayViewModel.advanceSimulatedDay() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = FlagOrange),
                        shape = Shapes.small
                    ) {
                        Text("Simulate morning data arrival", fontFamily = JetBrainsMono, fontSize = 12.sp)
                    }
                    // Repositions the frontier to (selected day − 1) so the next simulate lands on the
                    // selected day and fires its Call — lets any state be reached in one advance.
                    Button(
                        onClick = { todayViewModel.armSimulationAtSelectedDay() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = Muted),
                        shape = Shapes.small
                    ) {
                        Text("Arm arrival at day $currentDayIndex", fontFamily = JetBrainsMono, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { showDayPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = SignalLime),
                        shape = Shapes.small
                    ) {
                        Text("Select Day: $currentDayIndex", fontFamily = JetBrainsMono, fontSize = 12.sp)
                    }
                }
            }

            if (showDayPicker) {
                AlertDialog(
                    onDismissRequest = { showDayPicker = false },
                    title = { Text("Debug Day Picker", color = ColorText) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            (1..45).forEach { i ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            todayViewModel.selectDay(i)
                                            showDayPicker = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Day $i", color = if (i == currentDayIndex) SignalLime else ColorText, fontFamily = JetBrainsMono)
                                    val baseDate = LocalDate.parse("2026-07-10")
                                    val dateForIdx = baseDate.minusDays((45 - i).toLong()).toString()
                                    Text(dateForIdx, color = Muted, fontFamily = JetBrainsMono, fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDayPicker = false }) {
                            Text("Close", color = SignalLime)
                        }
                    },
                    containerColor = Surface
                )
            }
        }
    }
}

// Highlight stroke drawer modifier
fun Modifier.highlightStroke(color: Color, fraction: Float): Modifier = this.drawBehind {
    val strokeWidth = 8.dp.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width * fraction, y),
        strokeWidth = strokeWidth
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    todayViewModel: TodayViewModel = viewModel()
) {
    val snapshot by todayViewModel.currentSnapshot.collectAsState()
    val sparklineHrv by todayViewModel.sparklineHrv.collectAsState()
    val debugMode by todayViewModel.debugMode.collectAsState()
    val showDecodeSheet by todayViewModel.showDecodeSheet.collectAsState()
    val dayIndex by todayViewModel.dayIndex.collectAsState()

    // Animatable for highlight sweep effect
    val highlightSweep = remember { Animatable(0f) }
    LaunchedEffect(snapshot) {
        highlightSweep.snapTo(0f)
        highlightSweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    // Resolve state ink colors
    val inkColor = when (snapshot?.call) {
        "go" -> SignalLime
        "hold" -> ElectricCyan
        "back_off" -> FlagOrange
        else -> Muted
    }

    // Desaturated/calibration color adjustment
    val calibratedInk = if (snapshot?.confidence == "provisional") {
        inkColor.copy(alpha = 0.7f)
    } else {
        inkColor
    }

    TabScreen(title = "Today", isToday = true, todayViewModel = todayViewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (snapshot == null || snapshot?.call == "no_data") {
                // Stale/Empty State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No read yet.",
                            style = Typography.displayLarge.copy(fontSize = 32.sp),
                            color = Muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last night hasn't synced from Health Connect. Open WHOOP to push it, or pull to refresh.",
                            style = Typography.bodyLarge,
                            color = Muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val currentSnapshot = snapshot!!

                // Today screen framing: calibration overlay logic (days 1-3 calibrating)
                if (currentSnapshot.confidence == "calibrating") {
                    // Calibrating Overlay
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Calibrating",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = Muted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val daysLeft = 4 - dayIndex
                            Text(
                                text = "First Call in $daysLeft day${if (daysLeft > 1) "s" else ""}",
                                style = Typography.bodyLarge,
                                color = Muted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Miniature Timeline graphic
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TimelineDot("D1", active = dayIndex >= 1)
                                TimelineDivider()
                                TimelineDot("D4", active = false)
                                TimelineDivider()
                                TimelineDot("D14", active = false)
                                TimelineDivider()
                                TimelineDot("D30", active = false)
                            }
                        }
                    }
                } else {
                    // Call Block Hero Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "THE CALL",
                            style = Typography.labelLarge,
                            color = Muted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val verb = when (currentSnapshot.call) {
                            "go" -> "Go hard."
                            "hold" -> "Hold."
                            "back_off" -> "Back off."
                            else -> "No data"
                        }
                        
                        Box(
                            modifier = Modifier
                                .highlightStroke(calibratedInk, highlightSweep.value)
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = verb,
                                style = Typography.displayLarge,
                                color = ColorText
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val supportingText = when (currentSnapshot.call) {
                            "go" -> "You're recovered and trending up. Today's the day to push."
                            "hold" -> "You're steady, not surging. Train — but don't chase a peak today."
                            "back_off" -> "You're ramping faster than you're recovering. Ease off, or this becomes an injury."
                            else -> ""
                        }

                        Text(
                            text = supportingText,
                            style = Typography.bodyLarge,
                            color = ColorText
                        )

                        // Provisional caption
                        if (currentSnapshot.confidence == "provisional") {
                            val daysToReliable = 14 - dayIndex
                            Text(
                                text = "Early read — your baseline is $daysToReliable days from reliable.",
                                style = Typography.bodyLarge.copy(fontSize = 13.sp),
                                color = Muted,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Signal Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "SIGNAL",
                                style = Typography.labelLarge,
                                color = Muted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentSnapshot.signalScore?.toString() ?: "--",
                                style = Typography.displayMedium,
                                color = calibratedInk
                            )
                        }

                        // Sparkline View
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .padding(horizontal = 16.dp)
                                .background(SurfaceDeep, RoundedCornerShape(8.dp))
                        ) {
                            SparklineComponent(data = sparklineHrv, strokeColor = calibratedInk)
                        }

                        // Confidence Badge
                        ConfidenceBadgeComponent(confidence = currentSnapshot.confidence)
                    }
                }

                // Decode Teaser Card
                var teaserText = "No decode data available."
                try {
                    val decodeJson = Json.parseToJsonElement(currentSnapshot.decodeJson).jsonArray
                    // Find strongest factor or first factor
                    if (decodeJson.isNotEmpty()) {
                        val factor = decodeJson[0].jsonObject
                        val name = factor["name"]?.jsonPrimitive?.content ?: ""
                        val direction = factor["direction"]?.jsonPrimitive?.content ?: ""
                        val value = factor["value"]?.jsonPrimitive?.content ?: ""
                        val baseline = factor["baseline"]?.jsonPrimitive?.content ?: ""
                        teaserText = "$name is $direction: $value vs $baseline baseline."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Highlighted teaser
                val teaserAnnotated = buildAnnotatedString {
                    val index = teaserText.indexOf(":")
                    if (index != -1) {
                        append(teaserText.substring(0, index + 1))
                        withStyle(style = SpanStyle(background = ReadYellow, color = Base)) {
                            append(teaserText.substring(index + 1))
                        }
                    } else {
                        append(teaserText)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { todayViewModel.setShowDecodeSheet(true) },
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DECODE WHY",
                                style = Typography.labelLarge,
                                color = Muted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = teaserAnnotated,
                                style = Typography.bodyLarge
                            )
                        }
                        Text(
                            text = "→",
                            style = Typography.titleMedium,
                            color = SignalLime,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Today's Plan Card
                val intensityTarget = when (currentSnapshot.call) {
                    "go" -> "Quality work is on the table. Keep the hard session hard; skip junk volume."
                    "hold" -> "Maintain current consistency. Focus on recovery and sub-maximal load."
                    "back_off" -> "Critical fatigue detected. Switch to active recovery or rest completely."
                    else -> "No plan available."
                }
                
                var loadContext = "Load 7d: -- · 28d avg: --"
                try {
                    val debugObj = Json.parseToJsonElement(currentSnapshot.debugJson).jsonObject
                    val load7d = debugObj["load_7d_sum"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val load28d = debugObj["load_28d_sum"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    loadContext = "Load 7d: ${load7d.toInt()} · 28d avg: ${(load28d / 4).toInt()}"
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TODAY'S PLAN",
                            style = Typography.labelLarge,
                            color = Muted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = intensityTarget,
                            style = Typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loadContext,
                            style = Typography.bodySmall,
                            color = Muted
                        )
                    }
                }

                // Debug Annotation Layer (Debug mode details)
                if (debugMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDeep, Shapes.medium)
                            .border(1.dp, Line, Shapes.medium)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "DEBUG PAYLOAD",
                            style = Typography.labelLarge,
                            color = SignalLime
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var rawString = ""
                        try {
                            val debugObj = Json.parseToJsonElement(currentSnapshot.debugJson).jsonObject
                            val hrvZ = debugObj["hrv_z_score"]?.jsonPrimitive?.content ?: ""
                            rawString = "state=${currentSnapshot.call.uppercase()} · score=${currentSnapshot.signalScore} · conf=${currentSnapshot.confidence} · hrv_z=$hrvZ"
                        } catch (e: Exception) {
                            rawString = "state=${currentSnapshot.call.uppercase()} · score=${currentSnapshot.signalScore}"
                        }
                        
                        Text(
                            text = rawString,
                            style = Typography.bodySmall,
                            color = ColorText
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet for Decode
    if (showDecodeSheet && snapshot != null) {
        DecodeBottomSheet(
            snapshot = snapshot!!,
            onDismiss = { todayViewModel.setShowDecodeSheet(false) },
            debugMode = debugMode,
            onSeeRawInputs = {
                todayViewModel.setShowDecodeSheet(false)
                todayViewModel.toggleDebugMode()
            }
        )
    }
}

@Composable
fun TimelineDot(label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) SignalLime else Muted)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = Typography.labelLarge, color = if (active) ColorText else Muted)
    }
}

@Composable
fun TimelineDivider() {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(2.dp)
            .background(Muted)
    )
}

@Composable
fun SparklineComponent(data: List<Double>, strokeColor: Color) {
    if (data.isEmpty()) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxVal = data.maxOrNull() ?: 1.0
        val minVal = data.minOrNull() ?: 0.0
        val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal
        
        val width = size.width
        val height = size.height
        val step = width / (data.size - 1)
        
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * step
            val y = height - ((value - minVal) / range * height).toFloat()
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun ConfidenceBadgeComponent(confidence: String) {
    val outlineColor = when (confidence) {
        "calibrating" -> Muted
        "provisional" -> ElectricCyan
        "reliable" -> ElectricCyan
        "high" -> SignalLime
        else -> Muted
    }
    
    val badgeLabel = when (confidence) {
        "calibrating" -> "Calibrating"
        "provisional" -> "Provisional"
        "reliable" -> "Reliable"
        "high" -> "High confidence"
        else -> confidence
    }

    Box(
        modifier = Modifier
            .border(1.dp, outlineColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (confidence == "reliable" || confidence == "high") {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(outlineColor)
                )
            }
            Text(
                text = badgeLabel,
                style = Typography.labelLarge.copy(fontSize = 11.sp),
                color = ColorText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecodeBottomSheet(
    snapshot: DailySnapshot,
    onDismiss: () -> Unit,
    debugMode: Boolean,
    onSeeRawInputs: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "WHY THIS CALL",
                style = Typography.labelLarge,
                color = Muted
            )
            Text(
                text = "Decode",
                style = Typography.headlineMedium,
                color = ColorText
            )

            // Lead sentence with yellow-highlighted key phrase
            val leadPhrase = when (snapshot.call) {
                "go" -> "HRV is running above baseline."
                "hold" -> "Physiology is stable today."
                "back_off" -> "HRV is sagging below baseline."
                else -> ""
            }
            val highlightedSentence = buildAnnotatedString {
                append("Your ")
                withStyle(style = SpanStyle(background = ReadYellow, color = Base)) {
                    append(leadPhrase)
                }
                append(" Keep consistency in mind.")
            }
            
            Text(
                text = highlightedSentence,
                style = Typography.bodyLarge
            )

            // Factors List
            val factorsList = remember(snapshot.decodeJson) {
                try {
                    val decodeJson = Json.parseToJsonElement(snapshot.decodeJson).jsonArray
                    decodeJson.map { element ->
                        val factor = element.jsonObject
                        val name = factor["name"]?.jsonPrimitive?.content ?: ""
                        val direction = factor["direction"]?.jsonPrimitive?.content ?: ""
                        val value = factor["value"]?.jsonPrimitive?.content ?: ""
                        val baseline = factor["baseline"]?.jsonPrimitive?.content ?: ""
                        val source = factor["source"]?.jsonPrimitive?.content ?: ""
                        
                        val directionColor = when (direction.lowercase()) {
                            "lifting you", "climbing", "fresh" -> SignalLime
                            "dragging you", "sagging", "debt", "overload" -> FlagOrange
                            else -> Muted
                        }
                        
                        FactorRowData(name, direction, value, baseline, source, directionColor)
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (factorsList.isEmpty()) {
                    Text("No factors loaded or error parsing decode JSON", color = FlagOrange)
                } else {
                    factorsList.forEach { factor ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDeep),
                            shape = Shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = factor.name,
                                        style = Typography.titleMedium,
                                        color = ColorText
                                    )
                                    Text(
                                        text = factor.direction,
                                        style = Typography.labelLarge,
                                        color = factor.directionColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${factor.value} · baseline ${factor.baseline}",
                                        style = Typography.bodySmall,
                                        color = Muted
                                    )
                                    
                                    // Source chip
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Line, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = factor.source.uppercase(),
                                            style = Typography.bodySmall.copy(fontSize = 10.sp),
                                            color = Muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Honesty footnote
            Text(
                text = "Load from wrist data approximates court and racket sessions. Log RPE to sharpen it.",
                style = Typography.bodySmall.copy(fontSize = 11.sp),
                color = Muted
            )

            // Confidence Badge + Link to Debug
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfidenceBadgeComponent(confidence = snapshot.confidence)
                
                Text(
                    text = "See raw inputs →",
                    style = Typography.bodyLarge.copy(textDecoration = TextDecoration.Underline),
                    color = SignalLime,
                    modifier = Modifier.clickable { onSeeRawInputs() }
                )
            }
        }
    }
}

// =============================================================================
// TRENDS
// =============================================================================

@Composable
fun TrendsScreen(todayViewModel: TodayViewModel) {
    val trendsData by todayViewModel.trendsData.collectAsState()
    val snapshot by todayViewModel.currentSnapshot.collectAsState()
    val dayIndex by todayViewModel.dayIndex.collectAsState()

    TabScreen(title = "Trends", todayViewModel = todayViewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DriftStatusLine(snapshot)

            val data = trendsData
            if (data == null) {
                Text("Loading trends…", style = Typography.bodyLarge, color = Muted)
            } else {
                val i = (dayIndex - 1).coerceIn(0, data.dates.size - 1)

                TrendChartCard(
                    eyebrow = "HRV · 7D VS 30D BASELINE",
                    currentValueLabel = "${data.hrv7d.getOrElse(i) { 0.0 }.toInt()} ms",
                    dates = data.dates,
                    inkValues = data.hrv7d,
                    baselineValues = data.hrv30dBaseline,
                    selectedIndex = dayIndex,
                    inkColor = ElectricCyan
                )

                TrendChartCard(
                    eyebrow = "RESTING HEART RATE",
                    currentValueLabel = "${data.rhr7d.getOrElse(i) { 0.0 }.toInt()} bpm",
                    dates = data.dates,
                    inkValues = data.rhr7d,
                    baselineValues = data.rhr30dBaseline,
                    selectedIndex = dayIndex,
                    inkColor = ElectricCyan
                )

                SleepChartCard(
                    dates = data.dates,
                    sleepHours = data.sleepHours,
                    needHours = data.sleepNeedHours,
                    selectedIndex = dayIndex,
                    currentValueLabel = "${"%.1f".format(data.sleepHours.getOrElse(i) { 0.0 })} h"
                )

                TrendChartCard(
                    eyebrow = "LOAD · DIRECTIONAL",
                    currentValueLabel = "${data.load7d.getOrElse(i) { 0.0 }.toInt()} u",
                    dates = data.dates,
                    inkValues = data.load7d,
                    baselineValues = data.load28dWeeklyAvg,
                    selectedIndex = dayIndex,
                    inkColor = ElectricCyan
                )
            }
        }
    }
}

@Composable
fun DriftStatusLine(snapshot: DailySnapshot?) {
    val debug = remember(snapshot?.debugJson) {
        try {
            snapshot?.debugJson?.let { Json.parseToJsonElement(it).jsonObject }
        } catch (e: Exception) {
            null
        }
    }
    val driftRising = debug?.get("drift_rising")?.jsonPrimitive?.booleanOrNull ?: false
    val sagDays = debug?.get("consecutive_sag_days")?.jsonPrimitive?.doubleOrNull?.toInt() ?: 0

    val copy = if (driftRising) {
        "Drift: rising — $sagDays days of load outpacing recovery"
    } else {
        "Drift: none — recovery is keeping pace with load"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (driftRising) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(FlagOrange)
            )
        }
        Text(
            text = copy,
            style = Typography.bodyLarge,
            color = if (driftRising) FlagOrange else Muted
        )
    }
}

@Composable
private fun ChartCardShell(
    eyebrow: String,
    currentValueLabel: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = eyebrow, style = Typography.labelLarge, color = Muted)
                Text(text = currentValueLabel, style = Typography.bodySmall, color = ElectricCyan)
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * Zooms the y-axis to the data's actual range (with padding) instead of Vico's default zero-based
 * range. Zero values are excluded from the range calculation: early calibration days have no
 * 30-day rolling window yet, so the engine reports a 0.0 placeholder baseline for them — treating
 * that as real data would flatten the whole chart down to a sliver near the top.
 */
private fun paddedYRange(vararg lists: List<Double>?): AxisValueOverrider {
    val all = lists.filterNotNull().flatten().filter { it > 0.0 }
    if (all.isEmpty()) return AxisValueOverrider.auto()
    val min = all.min()
    val max = all.max()
    val padding = ((max - min) * 0.15).coerceAtLeast(1.0)
    val step = if (max > 200) 10.0 else 1.0
    val lo = (Math.floor((min - padding) / step) * step).coerceAtLeast(0.0)
    val hi = Math.ceil((max + padding) / step) * step
    return AxisValueOverrider.fixed(minY = lo.toFloat(), maxY = hi.toFloat())
}

private fun dateLabelFor(dates: List<String>, x: Float): String {
    val index = (x.toInt() - 1).coerceIn(0, dates.size - 1)
    return try {
        LocalDate.parse(dates[index]).format(DateTimeFormatter.ofPattern("M/d"))
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun rememberTodayMarker(color: Color) = rememberDefaultCartesianMarker(
    label = rememberTextComponent(
        color = Base,
        textSize = 11.sp,
        background = rememberShapeComponent(shape = VicoShape.Pill, color = color),
        padding = Dimensions.of(6.dp, 3.dp),
        typeface = Typeface.MONOSPACE
    ),
    labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
    guideline = rememberAxisGuidelineComponent(color = color, shape = VicoShape.Rectangle)
)

@Composable
fun TrendChartCard(
    eyebrow: String,
    currentValueLabel: String,
    dates: List<String>,
    inkValues: List<Double>,
    baselineValues: List<Double>?,
    selectedIndex: Int,
    inkColor: Color
) {
    ChartCardShell(eyebrow = eyebrow, currentValueLabel = currentValueLabel) {
        val modelProducer = remember { CartesianChartModelProducer.build() }
        LaunchedEffect(inkValues, baselineValues) {
            modelProducer.runTransaction {
                lineSeries {
                    series(x = inkValues.indices.map { it + 1 }, y = inkValues)
                    if (baselineValues != null) {
                        series(x = baselineValues.indices.map { it + 1 }, y = baselineValues)
                    }
                }
            }
        }

        ProvideVicoTheme(rememberM3VicoTheme(lineColor = Line, textColor = Muted)) {
            CartesianChartHost(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(
                    initialScroll = remember(selectedIndex) { Scroll.Absolute.x(selectedIndex.toFloat(), bias = 0.5f) }
                ),
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lines = listOfNotNull(
                            rememberLineSpec(shader = DynamicShader.color(inkColor), backgroundShader = null),
                            if (baselineValues != null) {
                                rememberLineSpec(shader = DynamicShader.color(Line), backgroundShader = null)
                            } else null
                        ),
                        axisValueOverrider = paddedYRange(inkValues, baselineValues)
                    ),
                    startAxis = rememberStartAxis(guideline = rememberAxisGuidelineComponent(color = Line)),
                    bottomAxis = rememberBottomAxis(
                        guideline = null,
                        valueFormatter = CartesianValueFormatter { value, _, _ -> dateLabelFor(dates, value) }
                    ),
                    persistentMarkers = mapOf(selectedIndex.toFloat() to rememberTodayMarker(inkColor))
                )
            )
        }
    }
}

@Composable
fun SleepChartCard(
    dates: List<String>,
    sleepHours: List<Double>,
    needHours: Double,
    selectedIndex: Int,
    currentValueLabel: String
) {
    ChartCardShell(eyebrow = "SLEEP · DURATION VS NEED", currentValueLabel = currentValueLabel) {
        val modelProducer = remember { CartesianChartModelProducer.build() }
        val needLine = remember(sleepHours, needHours) { List(sleepHours.size) { needHours } }
        LaunchedEffect(sleepHours, needHours) {
            modelProducer.runTransaction {
                columnSeries { series(x = sleepHours.indices.map { it + 1 }, y = sleepHours) }
                lineSeries { series(x = needLine.indices.map { it + 1 }, y = needLine) }
            }
        }

        ProvideVicoTheme(rememberM3VicoTheme(lineColor = Line, textColor = Muted)) {
            CartesianChartHost(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(
                    initialScroll = remember(selectedIndex) { Scroll.Absolute.x(selectedIndex.toFloat(), bias = 0.5f) }
                ),
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            listOf(rememberLineComponent(color = ElectricCyan, thickness = 6.dp, shape = VicoShape.rounded(30)))
                        )
                    ),
                    rememberLineCartesianLayer(
                        lines = listOf(rememberLineSpec(shader = DynamicShader.color(Line), backgroundShader = null))
                    ),
                    startAxis = rememberStartAxis(guideline = rememberAxisGuidelineComponent(color = Line)),
                    bottomAxis = rememberBottomAxis(
                        guideline = null,
                        valueFormatter = CartesianValueFormatter { value, _, _ -> dateLabelFor(dates, value) }
                    ),
                    persistentMarkers = mapOf(selectedIndex.toFloat() to rememberTodayMarker(ElectricCyan))
                )
            )
        }
    }
}

// =============================================================================
// LOG
// =============================================================================

private val FeelOptions = listOf("Fresh" to "felt_fresh", "Normal" to "normal", "Flat" to "felt_flat", "Wrecked" to "felt_wrecked")

@Composable
fun LogScreen(todayViewModel: TodayViewModel) {
    val history by todayViewModel.logHistory.collectAsState()
    val submitting by todayViewModel.logSubmitting.collectAsState()
    val snapshot by todayViewModel.currentSnapshot.collectAsState()
    val showDecodeSheet by todayViewModel.showDecodeSheet.collectAsState()
    val debugMode by todayViewModel.debugMode.collectAsState()

    var rpe by remember { mutableStateOf<Int?>(null) }
    var feel by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    TabScreen(title = "Log", todayViewModel = todayViewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "How did today feel?", style = Typography.titleMedium, color = ColorText)

                    RpeSelector(value = rpe, onChange = { rpe = it })

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeelOptions.forEach { (label, raw) ->
                            FeelChip(label = label, selected = feel == raw, onClick = { feel = if (feel == raw) null else raw })
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Add a note (optional)", color = Muted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ColorText,
                            unfocusedTextColor = ColorText,
                            focusedBorderColor = Line,
                            unfocusedBorderColor = Line,
                            cursorColor = SignalLime
                        )
                    )

                    Text(
                        text = "Your read is the label the model learns from.",
                        style = Typography.bodySmall.copy(fontSize = 12.sp),
                        color = Muted
                    )

                    Button(
                        onClick = {
                            todayViewModel.logManualEntry(rpe, feel, note.ifBlank { null })
                            rpe = null
                            feel = null
                            note = ""
                        },
                        enabled = rpe != null && !submitting,
                        colors = ButtonDefaults.buttonColors(containerColor = SignalLime, contentColor = Base),
                        shape = Shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (submitting) "Logging…" else "Log it", fontFamily = Inter, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Text(text = "HISTORY", style = Typography.labelLarge, color = Muted)

            if (history.isEmpty()) {
                Text("No entries yet.", style = Typography.bodyLarge, color = Muted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach { row ->
                        LogHistoryRowCard(row) {
                            todayViewModel.selectDayByDate(row.date)
                            todayViewModel.setShowDecodeSheet(true)
                        }
                    }
                }
            }
        }
    }

    if (showDecodeSheet && snapshot != null) {
        DecodeBottomSheet(
            snapshot = snapshot!!,
            onDismiss = { todayViewModel.setShowDecodeSheet(false) },
            debugMode = debugMode,
            onSeeRawInputs = { todayViewModel.setShowDecodeSheet(false) }
        )
    }
}

@Composable
fun RpeSelector(value: Int?, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "RPE", style = Typography.labelLarge, color = Muted)
            Text(
                text = value?.toString() ?: "—",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (value != null) lerp(ElectricCyan, FlagOrange, (value - 1) / 9f) else Muted
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (1..10).forEach { i ->
                val filled = value != null && i <= value
                val segmentColor = if (filled) lerp(ElectricCyan, FlagOrange, (i - 1) / 9f) else SurfaceRaised
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(segmentColor)
                        .border(1.dp, if (filled) Color.Transparent else Line, RoundedCornerShape(4.dp))
                        .clickable { onChange(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = i.toString(),
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = if (filled) Base else Muted
                    )
                }
            }
        }
    }
}

@Composable
fun FeelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) SignalLime else Color.Transparent)
            .border(1.dp, if (selected) SignalLime else Line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = Typography.bodySmall.copy(fontSize = 13.sp),
            color = if (selected) Base else ColorText
        )
    }
}

@Composable
fun LogHistoryRowCard(row: LogHistoryRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = row.date, fontFamily = JetBrainsMono, color = ColorText, fontSize = 14.sp)
                CallPill(call = row.call)
            }
            if (row.sessionSummary != null) {
                Text(text = row.sessionSummary, style = Typography.bodySmall, color = Muted)
            }
            if (row.feelLabel != null || row.rpe != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (row.rpe != null) {
                        Text(text = "RPE ${row.rpe}", fontFamily = JetBrainsMono, fontSize = 12.sp, color = Muted)
                    }
                    if (row.feelLabel != null) {
                        FeelChip(label = row.feelLabel, selected = false, onClick = {})
                    }
                }
            }
        }
    }
}

@Composable
fun CallPill(call: String) {
    val (label, color) = when (call) {
        "go" -> "Go" to SignalLime
        "hold" -> "Hold" to ElectricCyan
        "back_off" -> "Back off" to FlagOrange
        "calibrating" -> "Calibrating" to Muted
        else -> "No data" to Muted
    }
    Box(
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = Typography.labelLarge.copy(fontSize = 11.sp), color = color)
    }
}

// =============================================================================
// SOURCES
// =============================================================================

@Composable
fun SourcesScreen(todayViewModel: TodayViewModel) {
    val sources by todayViewModel.sourceSummaries.collectAsState()
    val dateString by todayViewModel.dateString.collectAsState()
    val attributionRows = remember { todayViewModel.attributionRows() }

    TabScreen(title = "Sources", todayViewModel = todayViewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Mock feed · as of $dateString",
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                color = Muted
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sources.forEach { source ->
                    SourceCard(source)
                }
            }

            Text(text = "WHO WINS WHAT", style = Typography.labelLarge, color = Muted)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    attributionRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${row.metricLabel} ← ${row.winner}",
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                color = ColorText
                            )
                            if (row.dedupedNote != null) {
                                Text(
                                    text = row.dedupedNote,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = Muted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When two sources report the same thing, the higher-priority source wins. Everything is kept; one is trusted.",
                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                        color = Muted
                    )
                    Text(
                        text = "Placeholder priority config — the real config-driven dedup resolver lands in M6.",
                        style = Typography.bodySmall.copy(fontSize = 11.sp),
                        color = FlagOrange
                    )
                }
            }
        }
    }
}

@Composable
fun SourceCard(source: SourceSummary) {
    val dotColor = when {
        source.neverConnected -> Muted
        source.flowingToday -> SignalLime
        else -> FlagOrange
    }
    val lastDataLabel = source.lastDataAtMillis?.let {
        val instant = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC)
        instant.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"))
    } ?: "Never"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor)
                )
                Text(
                    text = PlaceholderDedupConfig.sourceDisplayName(source.sourceApp),
                    style = Typography.titleMedium,
                    color = ColorText
                )
            }
            Text(text = "Last data: $lastDataLabel", fontFamily = JetBrainsMono, fontSize = 12.sp, color = Muted)
            if (source.dataTypeChips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    source.dataTypeChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, Line, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = chip.uppercase(), style = Typography.bodySmall.copy(fontSize = 10.sp), color = Muted)
                        }
                    }
                }
            }
        }
    }
}
