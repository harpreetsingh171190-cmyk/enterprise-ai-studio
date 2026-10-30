package ai.mlc.mlcchat

import ai.mlc.mlcchat.enterprise.EnterpriseDashboard
import ai.mlc.mlcchat.enterprise.GgufInferenceScreen
import ai.mlc.mlcchat.enterprise.GgufManagerScreen
import ai.mlc.mlcchat.enterprise.HistoryScreen
import ai.mlc.mlcchat.enterprise.LiveAnalyticsScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@ExperimentalMaterial3Api
@Composable
fun NavView() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { EnterpriseDashboard(navController) }
        composable("gguf") { GgufManagerScreen(navController) }
        composable("gguf-chat") { GgufInferenceScreen(navController) }
        composable("analytics") { LiveAnalyticsScreen(navController) }
        composable("history") { HistoryScreen(navController) }
    }
}
