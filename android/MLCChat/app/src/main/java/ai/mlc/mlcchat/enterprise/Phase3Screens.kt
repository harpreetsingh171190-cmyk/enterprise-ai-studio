package ai.mlc.mlcchat.enterprise

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GgufManagerScreen(nav: NavController) {
    val context = LocalContext.current
    val catalog = remember { GgufCatalog(context) }
    var models by remember { mutableStateOf(catalog.list()) }
    var status by remember { mutableStateOf("GGUF import and catalog only. llama.cpp native runtime is not bundled.") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            status = "Importing model…"
            val result = withContext(Dispatchers.IO) { runCatching { catalog.import(uri) } }
            status = result.fold({ "Imported ${it.fileName} (SHA-256 verified locally)" }, { "Import failed: ${it.message}" })
            models = withContext(Dispatchers.IO) { catalog.list() }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("GGUF Model Manager") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("Back") }
            Button(onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) }) { Text("Import GGUF file") }
            Text(status, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { nav.navigate("gguf-chat") }) { Text("Open GGUF Inference") }
            Text("Imported models: ${models.size}", style = MaterialTheme.typography.titleMedium)
            LazyColumn { items(models, key = { it.fileName }) { model ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(model.fileName)
                    Text("${model.bytes / 1048576} MB • SHA-256 ${model.sha256.take(16)}…", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { runCatching { catalog.delete(model) } }
                            status = result.fold({ "Deleted ${model.fileName}" }, { "Delete failed: ${it.message}" })
                            models = withContext(Dispatchers.IO) { catalog.list() }
                        }
                    }) { Text("Delete") }
                    Divider()
                }
            } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAnalyticsScreen(nav: NavController) {
    val context = LocalContext.current
    var metrics by remember { mutableStateOf(DeviceMonitor.sample(context)) }
    val samples = remember { mutableStateListOf<Int>() }
    LaunchedEffect(Unit) {
        while (true) {
            metrics = withContext(Dispatchers.Default) { DeviceMonitor.sample(context) }
            samples.add(metrics.appPssMb)
            if (samples.size > 60) samples.removeAt(0)
            delay(2000)
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Live Device Analytics") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("Back") }
            Text("System free RAM: ${metrics.freeRamMb} / ${metrics.totalRamMb} MB")
            Text("App memory (PSS): ${metrics.appPssMb} MB")
            Text(metrics.gpuInfo)
            Text("App PSS trend · 2-second samples (last 2 minutes)")
            val lineColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val points = samples.toList()
                val max = (points.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
                if (points.size > 1) for (i in 1 until points.size) {
                    drawLine(lineColor, Offset((i - 1) * size.width / 59f, size.height * (1 - points[i-1] / max)),
                        Offset(i * size.width / 59f, size.height * (1 - points[i] / max)), strokeWidth = 3.dp.toPx())
                }
            }
            Text("GPU utilization is not available through a standard Android API. This screen never invents a GPU percentage.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
