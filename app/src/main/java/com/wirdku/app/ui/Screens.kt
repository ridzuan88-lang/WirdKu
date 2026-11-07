package com.wirdku.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wirdku.app.AppViewModel
import com.wirdku.app.data.Tasbih

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(title: String, nav: NavController?, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title) },
            navigationIcon = { if (nav != null) IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Emerald, titleContentColor = androidx.compose.ui.graphics.Color.White,
                navigationIconContentColor = androidx.compose.ui.graphics.Color.White))
    }) { pad -> Column(Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()), content = content) }
}

@Composable
private fun Tile(text: String, sub: String = "", onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick),
        border = BorderStroke(1.dp, Gold)) {
        Column(Modifier.padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.titleMedium)
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavController) {
    val pos by vm.position.collectAsState()
    val bms by vm.bookmarks.collectAsState()
    val cols = remember { vm.repo.collections().filter { it.active } }
    Page("WirdKu", null) {
        cols.firstOrNull()?.let { c ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Emerald)) {
                Column(Modifier.padding(20.dp)) {
                    Text(c.title, color = Gold, style = MaterialTheme.typography.headlineSmall)
                    Text(c.arabicTitle, color = androidx.compose.ui.graphics.Color.White, fontSize = 32.sp, fontFamily = NaskhFamily)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (c.pages.isNotEmpty())
                            Button({ nav.navigate("book/${c.id}") }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Buka Buku") }
                        OutlinedButton({ nav.navigate("read/${c.id}/0") }) { Text("Bacaan Teks", color = androidx.compose.ui.graphics.Color.White) }
                    }
                }
            }
            if (c.sourceStatus == "PLACEHOLDER")
                Text("Nota: teks Hizib Bahr sebenar belum dimasukkan. Sila tambah fail sumber (lihat README).",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            if (c.sourceStatus == "UNVERIFIED_WEB_SOURCE" || c.sourceStatus == "USER_BOOK_TRANSCRIPTION")
                Text("Nota: halaman buku ialah imbasan buku anda. Teks yang boleh diketik ialah salinan taipan dan belum disemak - bandingkan dengan halaman buku.",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (pos.collectionId.isNotBlank()) {
            val c = vm.repo.collection(pos.collectionId)
            if (c != null) Tile("Sambung Bacaan", "${c.title} - bahagian ${pos.sectionIndex + 1} / ${c.sections.size}") {
                nav.navigate("read/${c.id}/${pos.sectionIndex}")
            }
        }
        Tile("Koleksi Saya", "${cols.size} koleksi aktif") { nav.navigate("library") }
        Tile("Penanda Buku", "${bms.size} penanda") { nav.navigate("bookmarks") }
        Tile("Tasbih Digital") { nav.navigate("tasbih") }
        Tile("Tetapan") { nav.navigate("settings") }
    }
}

@Composable
fun LibraryScreen(vm: AppViewModel, nav: NavController) {
    val cols = remember { vm.repo.collections() }
    Page("Koleksi", nav) {
        cols.forEach { c ->
            Tile(c.title + if (c.arabicTitle.isNotBlank()) "  ${c.arabicTitle}" else "",
                if (c.active) "${c.sections.size} bahagian" else "Akan datang") { if (c.active) nav.navigate("read/${c.id}/0") }
        }
        Text("Koleksi lain boleh ditambah melalui fail JSON (lihat README).", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BookmarksScreen(vm: AppViewModel, nav: NavController) {
    val bms by vm.bookmarks.collectAsState()
    val hist by vm.history.collectAsState()
    Page("Penanda & Sejarah", nav) {
        Text("Penanda Buku", style = MaterialTheme.typography.titleMedium)
        if (bms.isEmpty()) Text("Belum ada penanda.")
        bms.forEach { b -> Tile(b.label.ifBlank { "Bahagian ${b.sectionIndex + 1}" }, b.collectionId) { nav.navigate("read/${b.collectionId}/${b.sectionIndex}") } }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sejarah Bacaan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton({ vm.clearHistory() }) { Text("Kosongkan") }
        }
        if (hist.isEmpty()) Text("Belum ada sejarah.")
        hist.forEach { h ->
            val fmt = remember(h.readAt) { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(h.readAt)) }
            Tile("${h.collectionId} - bahagian ${h.sectionIndex + 1}", fmt) { nav.navigate("read/${h.collectionId}/${h.sectionIndex}") }
        }
    }
}

@Composable
fun TasbihScreen(vm: AppViewModel, nav: NavController) {
    val t by vm.tasbih.collectAsState()
    val haptic = LocalHapticFeedback.current
    var label by remember(t.label) { mutableStateOf(t.label) }
    Page("Tasbih Digital", nav) {
        OutlinedTextField(label, { label = it; vm.saveTasbih(t.copy(label = it)) }, label = { Text("Label zikir") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text("${t.count} / ${t.target}", fontSize = 56.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (t.target > 0 && t.count >= t.target) Text("Sasaran dicapai", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Emerald)
        Spacer(Modifier.height(16.dp))
        Button({
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.saveTasbih(t.copy(count = t.count + 1))
        }, Modifier.fillMaxWidth().height(160.dp)) { Text("TEKAN", fontSize = 28.sp) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ vm.saveTasbih(t.copy(count = 0)) }) { Text("Set Semula") }
            listOf(33, 99, 100).forEach { n -> FilterChip(t.target == n, { vm.saveTasbih(t.copy(target = n)) }, { Text("$n") }) }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    val s by vm.settings.collectAsState()
    Page("Tetapan", nav) {
        @Composable fun slider(title: String, v: Float, r: ClosedFloatingPointRange<Float>, f: (Float) -> Unit) {
            Text("$title: ${"%.1f".format(v)}"); Slider(v, f, valueRange = r)
        }
        slider("Saiz Arab (sp)", s.arabicSize, 24f..64f) { x -> vm.updateSettings { it.copy(arabicSize = x) } }
        slider("Saiz Melayu (sp)", s.malaySize, 12f..28f) { x -> vm.updateSettings { it.copy(malaySize = x) } }
        slider("Jarak baris", s.lineSpacing, 1.2f..2.6f) { x -> vm.updateSettings { it.copy(lineSpacing = x) } }
        slider("Jarak perkataan (dp)", s.wordSpacing, 0f..24f) { x -> vm.updateSettings { it.copy(wordSpacing = x) } }
        Text("Latar belakang")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Gading", "Sepia", "Gelap").forEachIndexed { i, n -> FilterChip(s.background == i, { vm.updateSettings { it.copy(background = i) } }, { Text(n) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fon Naskh (Noto Naskh Arabic)", Modifier.weight(1f)); Switch(s.useNaskh, { x -> vm.updateSettings { it.copy(useNaskh = x) } })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Skrin sentiasa hidup ketika membaca", Modifier.weight(1f)); Switch(s.keepScreenOn, { x -> vm.updateSettings { it.copy(keepScreenOn = x) } })
        }
        Spacer(Modifier.height(12.dp))
        Text("Fon: Noto Naskh Arabic (SIL Open Font License).", style = MaterialTheme.typography.bodySmall)
    }
}
