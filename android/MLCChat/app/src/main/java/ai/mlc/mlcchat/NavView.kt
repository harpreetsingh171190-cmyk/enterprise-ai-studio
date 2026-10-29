package ai.mlc.mlcchat

import ai.mlc.mlcchat.enterprise.EnterpriseDashboard
import ai.mlc.mlcchat.enterprise.HistoryScreen
import ai.mlc.mlcchat.enterprise.GgufInferenceScreen
import ai.mlc.mlcchat.enterprise.GgufManagerScreen
import ai.mlc.mlcchat.enterprise.LiveAnalyticsScreen
import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@ExperimentalMaterial3Api
@Composable
fun NavView(activity: Activity, appViewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { EnterpriseDashboard(navController, appViewModel) }
        composable("gguf") { GgufManagerScreen(navController) }
        composable("gguf-chat") { GgufInferenceScreen(navController) }
        composable("analytics") { LiveAnalyticsScreen(navController) }
        composable("history") { HistoryScreen(navController) }
        composable("home") { StartView(navController, appViewModel) }
        composable("chat") { ChatView(navController, appViewModel.chatState, activity) }
    }
}
