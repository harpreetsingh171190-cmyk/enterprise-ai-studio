package ai.mlc.mlcchat.enterprise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/** One finished generation, recorded by the inference screen. */
data class BenchmarkRun(
    val model: String,
    val threads: Int,
    val contextSize: Int,
    val tokens: Int,
    val seconds: Double,
    val tokensPerSecond: Double,
    val loadSeconds: Double
)

/** In-memory log of benchmark runs. It is cleared when the app process ends. */
object BenchmarkLog {
    val runs = mutableStateListOf<BenchmarkRun>()
    fun add(run: BenchmarkRun) {
        runs.add(0, run)
        while (runs.size > 50) runs.removeAt(runs.size - 1)
    }
    fun clear() = runs.clear()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(nav: NavController) {
    val runs = BenchmarkLog.runs
    val best = runs.maxOfOrNull { it.tokensPerSecond } ?: 0.0
    val average = if (runs.isEmpty()) 0.0 else runs.sumOf { it.tokensPerSecond } / runs.size
    val barColor = MaterialTheme.colorScheme.primary
    Scaffold(topBar = { TopAppBar(title = { Text("Benchmark Results") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { nav.popBackStack() }) { Text("Back") }
                OutlinedButton(enabled = runs.isNotEmpty(), onClick = { BenchmarkLog.clear() }) { Text("Clear results") }
            }
            if (runs.isEmpty()) {
                Text("No runs yet. Load a model and generate a response on the GGUF Inference screen.")
            } else {
                Text("Runs: ${runs.size}   Best: ${"%.1f".format(best)} tok/s   Average: ${"%.1f".format(average)} tok/s")
                Text("Tokens per second · latest 10 runs, oldest on the left", style = MaterialTheme.typography.titleSmall)
                Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                    val points = runs.take(10).reversed()
                    val max = (points.maxOfOrNull { it.tokensPerSecond } ?: 1.0).coerceAtLeast(0.1).toFloat()
                    val width = size.width / 10f
                    points.forEachIndexed { index, run ->
                        val h = size.height * (run.tokensPerSecond.toFloat() / max)
                        drawRect(barColor, topLeft = Offset(index * width + width * 0.15f, size.height - h), size = Size(width * 0.7f, h))
                    }
                }
                LazyColumn(Modifier.weight(1f)) { items(runs.toList()) { run ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("${run.model} · ${"%.1f".format(run.tokensPerSecond)} tok/s")
                        Text(
                            "${run.tokens} tokens in ${"%.1f".format(run.seconds)}s · ${run.threads} threads · " +
                                "context ${run.contextSize} · load ${"%.1f".format(run.loadSeconds)}s",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Divider()
                } }
            }
            Text(
                "Speed depends on the model, quantization, prompt, device temperature and background apps. Results are kept in memory only.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
