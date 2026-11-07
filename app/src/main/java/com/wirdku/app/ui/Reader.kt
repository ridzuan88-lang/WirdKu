package com.wirdku.app.ui

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wirdku.app.AppViewModel
import com.wirdku.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(vm: AppViewModel, nav: NavController, collectionId: String, startIndex: Int) {
    val col = remember(collectionId) { vm.repo.collection(collectionId) }
    val s by vm.settings.collectAsState()
    val bms by vm.bookmarks.collectAsState()
    val ctx = LocalContext.current
    if (col == null) { Page("Bacaan", nav) { Text("Koleksi tidak dijumpai.") }; return }

    // Keep-screen-on, user controlled
    DisposableEffect(s.keepScreenOn) {
        val w = (ctx as? android.app.Activity)?.window
        if (s.keepScreenOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); vm.audio.stop() }
    }

    val bg = when (s.background) { 1 -> Sepia; 2 -> Charcoal; else -> Ivory }
    val fg = if (s.background == 2) Color(0xFFEDE7D5) else Color(0xFF1B1B1B)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex.coerceIn(0, (col.sections.size - 1).coerceAtLeast(0)))
    var selected by remember { mutableStateOf<Pair<Section, Word>?>(null) }
    var showModes by remember { mutableStateOf(false) }
    val arabicFont = if (s.useNaskh) NaskhFamily else FontFamily.Serif

    // Persist reading position + history
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { i ->
            vm.savePosition(col.id, i)
            delay(3000)
            col.sections.getOrNull(i)?.let { vm.logHistory(col.id, it.id, i) }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text(col.title) },
                navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                actions = { IconButton({ showModes = !showModes }) { Icon(Icons.Default.Tune, "Mod bacaan") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Emerald, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White))
        },
        bottomBar = { FullAudioBar(vm, col) },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (showModes) ModePanel(s, vm)
            if (col.sections.isEmpty()) Text("Tiada kandungan.", Modifier.padding(16.dp), color = fg)
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
                items(col.sections, key = { it.id }) { sec ->
                    val idx = col.sections.indexOf(sec)
                    val marked = bms.any { it.collectionId == col.id && it.sectionId == sec.id }
                    Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (sec.title.isBlank()) "Bahagian ${idx + 1}" else sec.title, color = Gold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            if (vm.repo.assetExists(sec.audio)) IconButton({ vm.audio.play(sec.audio!!) }) { Icon(Icons.Default.PlayArrow, "Audio bahagian", tint = fg) }
                            IconButton({ vm.toggleBookmark(col.id, sec.id, idx, sec.title.ifBlank { "${col.title} ${idx + 1}" }) }) {
                                Icon(if (marked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Penanda", tint = Gold)
                            }
                        }
                        if (s.showArabic) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.wordSpacing.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                sec.words.forEach { w ->
                                    val sel = selected?.second?.id == w.id
                                    Text(w.arabic, fontFamily = arabicFont, fontSize = s.arabicSize.sp,
                                        lineHeight = (s.arabicSize * s.lineSpacing).sp, color = fg,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(if (sel) Gold.copy(alpha = 0.35f) else Color.Transparent)
                                            .clickable { selected = sec to w })
                                }
                            }
                        }
                        if (s.showRumi && sec.rumi.isNotBlank()) LabeledText("Rumi", sec.rumi, s.malaySize, fg, italic = true)
                        if (s.showTranslation && sec.translation.isNotBlank()) LabeledText("Terjemahan", sec.translation, s.malaySize, fg)
                        if (s.showExplanation && sec.explanation.isNotBlank()) LabeledText("Penjelasan", sec.explanation, s.malaySize, fg)
                        HorizontalDivider(Modifier.padding(top = 24.dp), color = Gold.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    selected?.let { (_, w) ->
        ModalBottomSheet(onDismissRequest = { vm.audio.stop(); selected = null }) { WordSheet(vm, w, arabicFont) }
    }
}

@Composable
private fun LabeledText(label: String, text: String, size: Float, color: Color, italic: Boolean = false) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(label.uppercase(), color = Gold, fontSize = 11.sp)
        Text(text, color = color, fontSize = size.sp,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal)
    }
}

@Composable
private fun ModePanel(s: Settings, vm: AppViewModel) {
    FlowRowChips(
        listOf(
            "Arab" to s.showArabic, "Rumi" to s.showRumi, "Terjemahan" to s.showTranslation, "Penjelasan" to s.showExplanation),
        onToggle = { name ->
            vm.updateSettings {
                when (name) {
                    "Rumi" -> it.copy(showRumi = !it.showRumi)
                    "Terjemahan" -> it.copy(showTranslation = !it.showTranslation)
                    "Penjelasan" -> it.copy(showExplanation = !it.showExplanation)
                    else -> it.copy(showArabic = true) // Arabic always remains primary content
                }
            }
        },
        presets = {
            // Mode 1-4
            vm.updateSettings { st -> when (it) {
                1 -> st.copy(showArabic = true, showRumi = false, showTranslation = false, showExplanation = false)
                2 -> st.copy(showArabic = true, showRumi = true, showTranslation = false, showExplanation = false)
                3 -> st.copy(showArabic = true, showRumi = false, showTranslation = true, showExplanation = false)
                else -> st.copy(showArabic = true, showRumi = false, showTranslation = false, showExplanation = true)
            } }
        })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(items: List<Pair<String, Boolean>>, onToggle: (String) -> Unit, presets: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Mod 1: Arab", "Mod 2: +Rumi", "Mod 3: +Terjemahan", "Mod 4: +Penjelasan").forEachIndexed { i, n ->
                AssistChip({ presets(i + 1) }, { Text(n, fontSize = 12.sp) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (n, on) -> FilterChip(on, { onToggle(n) }, { Text(n, fontSize = 12.sp) }) }
        }
    }
}

@Composable
private fun FullAudioBar(vm: AppViewModel, col: WirdCollection) {
    val has = vm.repo.assetExists(col.audio)
    Surface(tonalElevation = 3.dp) {
        if (!has) {
            Text("Audio penuh belum tersedia (tambah fail rakaman - lihat README).", Modifier.fillMaxWidth().padding(12.dp), fontSize = 12.sp)
            return@Surface
        }
        var playing by remember { mutableStateOf(false) }
        var started by remember { mutableStateOf(false) }
        var pos by remember { mutableStateOf(0L) }
        var dur by remember { mutableStateOf(0L) }
        var speed by remember { mutableStateOf(1f) }
        var rep by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { while (true) { playing = vm.audio.isPlaying; pos = vm.audio.positionMs; dur = vm.audio.durationMs; delay(300) } }
        Column(Modifier.padding(8.dp)) {
            Slider(if (dur > 0) pos.toFloat() / dur else 0f, { vm.audio.seek((it * dur).toLong()) })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                IconButton({ if (!started) { vm.audio.play(col.audio!!, speed, rep); started = true } else vm.audio.toggle() }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Main/Jeda")
                }
                IconButton({ vm.audio.stop(); started = false }) { Icon(Icons.Default.Stop, "Henti") }
                IconButton({ rep = !rep; vm.audio.setRepeat(rep) }) { Icon(Icons.Default.Repeat, "Ulang", tint = if (rep) Gold else LocalContentColorOr()) }
                TextButton({ speed = when (speed) { 1f -> 0.75f; 0.75f -> 1.25f; else -> 1f }; vm.audio.setSpeed(speed) }) { Text("${speed}x") }
            }
        }
    }
}

@Composable private fun LocalContentColorOr() = LocalContentColor.current

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSheet(vm: AppViewModel, w: Word, arabicFont: FontFamily, timedPlay: ((Float, Boolean) -> Unit)? = null) {
    val mk = remember { vm.repo.makhraj() }
    val hasAudio = vm.repo.assetExists(w.audio)
    val hasSlow = vm.repo.assetExists(w.slowAudio)
    var repeat by remember { mutableStateOf(false) }
    val letters = remember(w.arabic) { ArabicText.baseLetters(w.arabic).mapNotNull { c -> mk.letters.firstOrNull { it.char.first() == c } } }
    val marks = remember(w.arabic) { ArabicText.marksIn(w.arabic).mapNotNull { m -> mk.marks.firstOrNull { it.mark.first() == m } } }
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(w.arabic, fontFamily = arabicFont, fontSize = 56.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
        if (hasAudio || timedPlay != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ if (hasAudio) vm.audio.play(w.audio!!, 1f, repeat) else timedPlay?.invoke(1f, repeat) }) { Text("Dengar Sebutan") }
                OutlinedButton({
                    if (hasSlow) vm.audio.play(w.slowAudio!!, 1f, repeat)
                    else if (hasAudio) vm.audio.play(w.audio!!, 0.6f, repeat)
                    else timedPlay?.invoke(0.6f, repeat)
                }) { Text("Dengar Perlahan") }
                FilterChip(repeat, { repeat = !repeat; vm.audio.setRepeat(repeat) }, { Text("Ulang Sebutan") })
            }
            if (!hasAudio) Text("Sebutan diambil daripada rakaman penuh anda pada masa yang anda tanda sendiri.", fontSize = 12.sp)
            else if (w.verified.not()) Text("Rakaman ini belum ditanda sebagai disahkan.", fontSize = 12.sp)
        } else {
            Text("Audio perkataan belum tersedia. Gunakan Mod Penanda Masa dalam skrin Buku untuk menanda perkataan ini.", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Text("Makhraj", style = MaterialTheme.typography.titleMedium, color = Gold)
        if (w.makhraj.isNotBlank()) Text(w.makhraj)
        letters.forEach { l ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Text("${l.char}  ${l.name} - ${l.group}", style = MaterialTheme.typography.titleSmall)
                Text(l.makhraj, fontSize = 14.sp)
                Text("Tip: ${l.tip}", fontSize = 13.sp)
            }
        }
        if (marks.isNotEmpty()) {
            Text("Tanda baris/tajwid", style = MaterialTheme.typography.titleMedium, color = Gold)
            marks.forEach { Text("${it.name}: ${it.note}", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        if (w.tajwid.isNotBlank()) { Text("Tajwid", style = MaterialTheme.typography.titleMedium, color = Gold); Text(w.tajwid) }
        Spacer(Modifier.height(12.dp))
        if (!(w.verified && w.makhraj.isNotBlank())) Text("Penjelasan huruf di atas bersifat umum dan BELUM disemak oleh guru tajwid bertauliah. Rujuk guru untuk kepastian.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
    }
}
