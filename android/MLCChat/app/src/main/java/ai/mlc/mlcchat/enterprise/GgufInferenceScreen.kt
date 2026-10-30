package ai.mlc.mlcchat.enterprise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GgufInferenceScreen(nav: NavController) {
    val context = LocalContext.current
    val runtime = remember { GgufRuntime(context) }
    val catalog = remember { GgufCatalog(context) }
    val models = remember { catalog.list() }
    var selected by remember { mutableStateOf(models.firstOrNull()) }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(if (GgufNative.available) "Native runtime ready" else "llama.cpp not bundled; build with enableLlamaCpp") }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var contextSize by remember { mutableFloatStateOf(1024f) }
    var maxTokens by remember { mutableFloatStateOf(256f) }
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.95f) }
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        onDispose { runtime.stop(); scope.launch { runtime.unload() } }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("GGUF Inference") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("Back") }
            Text(status, style = MaterialTheme.typography.bodySmall)
            Text("CPU inference • ${models.size} imported models")
            models.forEach { model ->
                Row {
                    RadioButton(selected == model, onClick = { if (!busy) { selected = model; loaded = false } }, enabled = !busy)
                    Text(model.fileName, modifier = Modifier.padding(top = 12.dp))
                }
            }
            Text("Context: ${contextSize.toInt()} tokens")
            Slider(value = contextSize, onValueChange = { contextSize = it }, valueRange = 512f..4096f, steps = 6, enabled = !busy && !loaded)
            Text("Max new tokens: ${maxTokens.toInt()}")
            Slider(value = maxTokens, onValueChange = { maxTokens = it }, valueRange = 32f..1024f, enabled = !busy)
            Text("Temperature: ${"%.2f".format(temperature)}")
            Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..2f, enabled = !busy)
            Text("Top-P: ${"%.2f".format(topP)}")
            Slider(value = topP, onValueChange = { topP = it }, valueRange = 0.05f..1f, enabled = !busy)
            Button(enabled = selected != null && !busy && GgufNative.available, onClick = {
                busy = true
                scope.launch {
                    val result = runCatching { runtime.load(requireNotNull(selected), contextSize.toInt(), 4) }
                    status = result.fold({ loaded = true; it }, { loaded = false; "Load failed: ${it.message}" })
                    busy = false
                }
            }) { Text("Load selected model") }
            OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Prompt") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = loaded && !busy && prompt.isNotBlank(), onClick = {
                    busy = true
                    answer = ""
                    status = "Generating…"
                    scope.launch {
                        val result = runCatching {
                            runtime.generate(prompt, maxTokens.toInt(), temperature, topP) { token ->
                                scope.launch { answer += token }
                            }
                        }
                        status = result.fold({ if (it >= 0) "Finished: $it tokens" else "Native generation error $it" }, { "Error: ${it.message}" })
                        busy = false
                    }
                }) { Text("Generate") }
                OutlinedButton(enabled = busy, onClick = { runtime.stop(); status = "Stopping…" }) { Text("Stop") }
            }
            Text("Response", style = MaterialTheme.typography.titleMedium)
            Text(answer.ifEmpty { "No response yet." })
            Text("RAM checks are estimates, not a guarantee. Avoid switching models while generating.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
