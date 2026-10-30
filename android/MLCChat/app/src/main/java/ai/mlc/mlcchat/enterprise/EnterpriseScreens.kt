package ai.mlc.mlcchat.enterprise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseDashboard(nav: NavController) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(emptyList<SessionSummary>()) }
    LaunchedEffect(Unit) { sessions = ChatHistoryDatabase(context).use { it.sessions() } }
    val counts = remember(sessions) { (0..6).map { day ->
        val start = System.currentTimeMillis() - (6-day)*86400000L
        sessions.count { it.createdAt >= start && it.createdAt < start+86400000L }
    } }
    Scaffold(topBar = { TopAppBar(title = { Text("Enterprise AI Studio") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Local AI Dashboard", style = MaterialTheme.typography.headlineSmall)
            Text("Saved sessions: ${sessions.size}")
            Text("Saved sessions · last 7 days", style = MaterialTheme.typography.titleMedium)
            val barColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
                val width = size.width / 7f
                counts.forEachIndexed { index, count ->
                    val h = size.height * count / max
                    drawRect(barColor, topLeft = Offset(index*width+width*.15f,size.height-h), size = Size(width*.7f,h))
                }
            }
            Text("Charts reflect saved snapshots, not live inference speed.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { nav.navigate("gguf") }, modifier = Modifier.fillMaxWidth()) { Text("GGUF Model Manager") }
            OutlinedButton(onClick = { nav.navigate("gguf-chat") }, modifier = Modifier.fillMaxWidth()) { Text("GGUF Inference") }
            OutlinedButton(onClick = { nav.navigate("analytics") }, modifier = Modifier.fillMaxWidth()) { Text("Live RAM / GPU Diagnostics") }
            OutlinedButton(onClick = { nav.navigate("benchmark") }, modifier = Modifier.fillMaxWidth()) { Text("Benchmark Results") }
            OutlinedButton(onClick = { nav.navigate("history") }, modifier = Modifier.fillMaxWidth()) { Text("Chat History Database") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(nav: NavController) {
    val context = LocalContext.current
    val db = remember { ChatHistoryDatabase(context) }
    DisposableEffect(db) { onDispose { db.close() } }
    var sessions by remember { mutableStateOf(db.sessions()) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    val reload = { sessions = if (query.isBlank()) db.sessions() else db.searchSessions(query.trim()) }
    Scaffold(topBar = { TopAppBar(title = { Text("Saved Chat History") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("Back") }
            if (selected != null) {
                Text("Saved conversation (read-only)", style = MaterialTheme.typography.titleMedium)
                LazyColumn { items(db.messages(selected!!)) { (role, content) ->
                    Text("$role: $content", Modifier.padding(vertical = 8.dp))
                    Divider()
                } }
            } else {
                OutlinedTextField(value = query, onValueChange = { query = it; reload() }, label = { Text("Search chats") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                if (sessions.isEmpty()) Text(if (query.isBlank()) "No saved chats yet." else "No chats match \"${query.trim()}\".", style = MaterialTheme.typography.bodySmall)
                LazyColumn { items(sessions, key = { it.id }) { session ->
                    ListItem(headlineContent = { Text(session.model) }, supportingContent = {
                        Text("${DateFormat.getDateTimeInstance().format(Date(session.createdAt))} · ${session.messageCount} messages")
                    }, trailingContent = {
                        Row {
                            TextButton(onClick = { selected = session.id }) { Text("Open") }
                            TextButton(onClick = { db.delete(session.id); reload() }) { Text("Delete") }
                        }
                    })
                    Divider()
                } }
            }
        }
    }
}
