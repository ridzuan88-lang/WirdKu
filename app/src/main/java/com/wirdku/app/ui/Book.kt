package com.wirdku.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wirdku.app.AppViewModel
import com.wirdku.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun fmt(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

private fun sectionEnd(col: WirdCollection, marks: Map<String, Long>, sec: Section): Long? {
    val start = marks[sec.id] ?: return null
    return col.sections.mapNotNull { marks[it.id] }.filter { it > start }.minOrNull()
}

private fun wordRange(col: WirdCollection, marks: Map<String, Long>, sec: Section, w: Word): Pair<Long, Long>? {
    val start = marks[w.id] ?: return null
    val nextWord = sec.words.mapNotNull { marks[it.id] }.filter { it > start }.minOrNull()
    return start to (nextWord ?: sectionEnd(col, marks, sec) ?: (start + 4000))
}

@Composable
private fun PageImage(path: String) {
    val ctx = LocalContext.current
    val bmp = remember(path) {
        runCatching { ctx.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
    }
    if (bmp != null) Image(bmp, contentDescription = "Halaman buku", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    else Text("Halaman tidak dapat dimuatkan", Modifier.padding(16.dp))
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookScreen(vm: AppViewModel, nav: NavController, collectionId: String) {
    val col = remember(collectionId) { vm.repo.collection(collectionId) }
    if (col == null || col.pages.isEmpty()) {
        Page("Buku", nav) { Text("Tiada halaman buku untuk koleksi ini.") }
        return
    }
    val marksFlow = remember(collectionId) { vm.prefs.marks(collectionId) }
    val marks by marksFlow.collectAsState(initial = emptyMap())
    val pager = rememberPagerState { col.pages.size }
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsState()
    val hasAudio = remember(col) { vm.repo.assetExists(col.audio) }
    val arabicFont = if (settings.useNaskh) NaskhFamily else androidx.compose.ui.text.font.FontFamily.Serif

    var textMode by remember { mutableStateOf(false) }
    var markMode by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Pair<Section, Word>?>(null) }
    var pos by remember { mutableStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) { pos = vm.audio.positionMs; playing = vm.audio.isPlaying; delay(250) }
    }
    DisposableEffect(Unit) { onDispose { vm.audio.stop() } }

    fun mark(id: String) {
        if (vm.audio.isFull(col.audio)) {
            val ms = vm.audio.positionMs
            scope.launch { vm.prefs.setMark(col.id, id, ms) }
            hint = "Ditanda pada ${fmt(ms)}"
        } else hint = "Mainkan audio penuh dahulu (butang main di bawah), kemudian tanda."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${col.title}  ${pager.currentPage + 1}/${col.pages.size}") },
                navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                actions = {
                    if (textMode && hasAudio) IconButton({ markMode = !markMode; hint = "" }) {
                        Icon(Icons.Default.Timer, "Mod penanda masa", tint = if (markMode) Gold else Color.White)
                    }
                    IconButton({ textMode = !textMode; if (!textMode) markMode = false }) {
                        Icon(if (textMode) Icons.Default.Image else Icons.Default.MenuBook, "Tukar paparan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Emerald, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White))
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    if (!hasAudio) {
                        Text("Audio belum tersedia.", fontSize = 12.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton({
                                if (!vm.audio.isFull(col.audio)) vm.audio.play(col.audio!!, speed, false) else vm.audio.toggle()
                            }) { Icon(if (playing && vm.audio.isFull(col.audio)) Icons.Default.Pause else Icons.Default.PlayArrow, "Main/Jeda") }
                            IconButton({ vm.audio.stop() }) { Icon(Icons.Default.Stop, "Henti") }
                            Text(if (vm.audio.isFull(col.audio)) fmt(pos) else "--:--", modifier = Modifier.weight(1f))
                            TextButton({
                                speed = when (speed) { 1f -> 0.75f; 0.75f -> 1.25f; else -> 1f }
                                vm.audio.setSpeed(speed)
                            }) { Text("${speed}x") }
                        }
                        if (markMode) {
                            Text("Mod penanda masa: mainkan audio, kemudian ketik 'Tanda' pada ayat atau ketik perkataan tepat semasa ia dibaca.", fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(hint, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton({ scope.launch { vm.prefs.clearMarks(col.id) }; hint = "Semua penanda dipadam" }) { Text("Padam semua") }
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        // Arabic book: page 1 on the right, swipe left-to-right to go forward. Flip-style transition around the right spine.
        HorizontalPager(state = pager, reverseLayout = true, modifier = Modifier.padding(pad).fillMaxSize()) { page ->
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val o = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    cameraDistance = 14f * density
                    transformOrigin = TransformOrigin(1f, 0.5f)
                    rotationY = o.coerceIn(-1f, 1f) * 40f
                    alpha = 1f - kotlin.math.abs(o).coerceIn(0f, 1f) * 0.5f
                }.background(Color(0xFFFBF6E9))
            ) {
                if (!textMode) PageImage(col.pages[page])
                else {
                    val secs = col.sections.filter { it.page == page + 1 }
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        if (secs.isEmpty()) Text("Tiada teks untuk halaman ini.")
                        secs.forEach { sec ->
                            val m = marks[sec.id]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sec.title, color = Gold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                if (m != null && hasAudio) IconButton({
                                    vm.audio.playClip(col.audio!!, m, sectionEnd(col, marks, sec), speed)
                                }) { Icon(Icons.Default.PlayArrow, "Main ayat") }
                                if (markMode) TextButton({ mark(sec.id) }) { Text(if (m != null) "✓ ${fmt(m)}" else "Tanda") }
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(settings.wordSpacing.dp)) {
                                    sec.words.forEach { w ->
                                        val has = marks[w.id] != null
                                        Text(w.arabic, fontFamily = arabicFont, fontSize = settings.arabicSize.sp,
                                            lineHeight = (settings.arabicSize * settings.lineSpacing).sp,
                                            color = Color(0xFF1B1B1B),
                                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                                .background(if (has) Color(0x3328A745) else Color.Transparent)
                                                .clickable { if (markMode) mark(w.id) else selected = sec to w })
                                    }
                                }
                            }
                            if (sec.explanation.isNotBlank() && settings.showExplanation)
                                Text(sec.explanation, fontSize = settings.malaySize.sp, modifier = Modifier.padding(top = 8.dp))
                            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Gold.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    selected?.let { (sec, w) ->
        val range = wordRange(col, marks, sec, w)
        val tp: ((Float, Boolean) -> Unit)? =
            if (range != null && hasAudio) { { sp: Float, rep: Boolean -> vm.audio.playClip(col.audio!!, range.first, range.second, sp, rep) } } else null
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            WordSheet(vm, w, arabicFont, timedPlay = tp)
        }
    }
}
