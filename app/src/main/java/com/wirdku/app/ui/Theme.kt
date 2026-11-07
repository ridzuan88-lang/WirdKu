package com.wirdku.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wirdku.app.R

val Emerald = Color(0xFF0B5D4B)
val Gold = Color(0xFFC9A24B)
val Ivory = Color(0xFFFBF6E9)
val Sepia = Color(0xFFF1E4C8)
val Charcoal = Color(0xFF1E2321)
val NaskhFamily = FontFamily(Font(R.font.noto_naskh_arabic))

@Composable
fun WirdTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(primary = Gold, onPrimary = Charcoal, background = Charcoal,
        surface = Color(0xFF262C29), onSurface = Color(0xFFEDE7D5), onBackground = Color(0xFFEDE7D5),
        secondaryContainer = Color(0xFF34403B))
    else lightColorScheme(primary = Emerald, onPrimary = Color.White, secondary = Gold, background = Ivory,
        surface = Color.White, onSurface = Color(0xFF1B1B1B), onBackground = Color(0xFF1B1B1B),
        secondaryContainer = Color(0xFFE5EFE9))
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Emerald), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦", color = Gold, fontSize = 56.sp)
            Text("وِرْدِي", color = Gold, fontSize = 44.sp, fontFamily = NaskhFamily)
            Text("WirdKu", color = Color.White, fontSize = 28.sp)
            Text("Koleksi Wirid & Zikir", color = Color(0xFFE8DDBB), fontSize = 14.sp)
        }
    }
}
