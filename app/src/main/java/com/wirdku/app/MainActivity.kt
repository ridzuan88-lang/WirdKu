package com.wirdku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wirdku.app.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsState()
            WirdTheme(dark = settings.background == 2) {
                var splash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(1200); splash = false }
                if (splash) { SplashScreen(); return@WirdTheme }
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") { HomeScreen(vm, nav) }
                    composable("library") { LibraryScreen(vm, nav) }
                    composable("read/{cid}/{idx}") { e ->
                        ReaderScreen(vm, nav, e.arguments?.getString("cid") ?: "hizib_bahr",
                            e.arguments?.getString("idx")?.toIntOrNull() ?: 0)
                    }
                    composable("bookmarks") { BookmarksScreen(vm, nav) }
                    composable("tasbih") { TasbihScreen(vm, nav) }
                    composable("settings") { SettingsScreen(vm, nav) }
                }
            }
        }
    }
}
