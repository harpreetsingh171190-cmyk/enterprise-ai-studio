package ai.mlc.mlcchat.enterprise

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import java.io.File

data class DeviceMetrics(val freeRamMb: Long, val totalRamMb: Long, val appPssMb: Int, val gpuInfo: String)
object DeviceMonitor {
    fun sample(context: Context): DeviceMetrics {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        val pss = manager.getProcessMemoryInfo(intArrayOf(Process.myPid())).firstOrNull()?.totalPss ?: 0
        // GPU telemetry is vendor-specific; a readable clock is not utilization.
        val candidates = listOf("/sys/class/kgsl/kgsl-3d0/gpuclk", "/sys/class/devfreq")
        val clock = runCatching { File(candidates[0]).takeIf { it.canRead() }?.readText()?.trim()?.toLongOrNull() }.getOrNull()
        return DeviceMetrics(info.availMem / 1048576, info.totalMem / 1048576, pss / 1024,
            clock?.let { "GPU clock: ${it / 1000000} MHz (not utilization)" } ?: "GPU usage unavailable on this device")
    }
}
