package ai.mlc.mlcchat.enterprise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GgufInferenceScreen(nav: NavController) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val runtime = remember { GgufRuntime(context) }
    val catalog = remember { GgufCatalog(context) }
    val models = remember { catalog.list() }
    var selected by remember { mutableStateOf(models.firstOrNull()) }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(if (GgufNative.available) "Native runtime ready" else "llama.cpp not bundled; build with enableLlamaCpp") }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var contextSize by remember { mutableStateOf(1024f) }
    var maxTokens by remember { mutableStateOf(256f) }
    var temperature by remember { mutableStateOf(0.7f) }
    var topP by remember { mutableStateOf(0.95f) }
    var threads by remember { mutableStateOf(4f) }
    var systemPrompt by remember { mutableStateOf("") }
    var lastPrompt by remember { mutableStateOf("") }
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
            Text("CPU threads: ${threads.toInt()}")
            Slider(value = threads, onValueChange = { threads = it }, valueRange = 1f..8f, steps = 6, enabled = !busy && !loaded)
            Text("Max new tokens: ${maxTokens.toInt()}")
            Slider(value = maxTokens, onValueChange = { maxTokens = it }, valueRange = 32f..1024f, enabled = !busy)
            Text("Temperature: ${"%.2f".format(temperature)}")
            Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..2f, enabled = !busy)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy, onClick = { temperature = 0.2f }) { Text("Precise") }
                OutlinedButton(enabled = !busy, onClick = { temperature = 0.7f }) { Text("Balanced") }
                OutlinedButton(enabled = !busy, onClick = { temperature = 1.2f }) { Text("Creative") }
            }
            Text("Top-P: ${"%.2f".format(topP)}")
            Slider(value = topP, onValueChange = { topP = it }, valueRange = 0.05f..1f, enabled = !busy)
            OutlinedButton(enabled = !busy && !loaded, onClick = {
                contextSize = 1024f
                maxTokens = 256f
                temperature = 0.7f
                topP = 0.95f
                threads = 4f
            }) { Text("Reset to defaults") }
            Button(enabled = selected != null && !busy && GgufNative.available, onClick = {
                busy = true
                scope.launch {
                    val result = runCatching { runtime.load(requireNotNull(selected), contextSize.toInt(), threads.toInt()) }
                    status = result.fold({ loaded = true; it }, { loaded = false; "Load failed: ${it.message}" })
                    busy = false
                }
            }) { Text("Load selected model") }
            OutlinedTextField(value = systemPrompt, onValueChange = { systemPrompt = it }, label = { Text("System prompt (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy, onClick = { prompt = "Summarize this:\n" + prompt }) { Text("Summarize") }
                OutlinedButton(enabled = !busy, onClick = { prompt = "Translate to Punjabi:\n" + prompt }) { Text("Translate to Punjabi") }
                OutlinedButton(enabled = !busy, onClick = { prompt = "Explain this in simple words:\n" + prompt }) { Text("Explain simply") }
                OutlinedButton(enabled = !busy, onClick = { prompt = "Fix the grammar of this text:\n" + prompt }) { Text("Fix grammar") }
            }
            OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Prompt") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = loaded && !busy && prompt.isNotBlank(), onClick = {
                    busy = true
                    answer = ""
                    lastPrompt = prompt
                    status = "Generating…"
                    scope.launch {
                        val startTime = System.nanoTime()
                        val fullPrompt = if (systemPrompt.isBlank()) prompt else systemPrompt.trim() + "\n\n" + prompt
                        val result = runCatching {
                            runtime.generate(fullPrompt, maxTokens.toInt(), temperature, topP) { token ->
                                scope.launch { answer += token }
                            }
                        }
                        val seconds = ((System.nanoTime() - startTime) / 1_000_000_000.0).coerceAtLeast(0.001)
                        status = result.fold({
                            if (it >= 0) "Finished: $it tokens in ${"%.1f".format(seconds)}s (${"%.1f".format(it / seconds)} tok/s)"
                            else "Native generation error $it"
                        }, { "Error: ${it.message}" })
                        busy = false
                    }
                }) { Text("Generate") }
                OutlinedButton(enabled = busy, onClick = { runtime.stop(); status = "Stopping…" }) { Text("Stop") }
                OutlinedButton(enabled = !busy, onClick = { prompt = ""; answer = "" }) { Text("Clear") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy && lastPrompt.isNotEmpty(), onClick = { prompt = lastPrompt }) { Text("Reuse last prompt") }
                OutlinedButton(enabled = answer.isNotEmpty(), onClick = { clipboard.setText(AnnotatedString(answer)); status = "Response copied" }) { Text("Copy response") }
            }
            Text("Response", style = MaterialTheme.typography.titleMedium)
            Text(answer.ifEmpty { "No response yet." })
            Text("${answer.length} characters • ${answer.trim().split(Regex("\\s+")).count { it.isNotEmpty() }} words", style = MaterialTheme.typography.bodySmall)
            Text("RAM checks are estimates, not a guarantee. Avoid switching models while generating.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
