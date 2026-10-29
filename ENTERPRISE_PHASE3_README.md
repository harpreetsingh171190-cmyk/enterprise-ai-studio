# Enterprise AI Studio — Phase 3 (partial)

## Implemented in this source patch
- GGUF file picker (Android Storage Access Framework), app-private copy, GGUF magic-header check, SHA-256 hash, JSON catalog, deletion.
- Live RAM: system free/total, app proportional set size (PSS), in-memory 2-second samples, Compose line chart.
- GPU: best-effort readable vendor clock; **not GPU utilization**. Clearly indicates unsupported devices.
- Dashboard navigation to both screens.

## llama.cpp integration status — NOT COMPLETE
This archive does **not** bundle llama.cpp source or compiled native libraries. GGUF files are **not inferable** from this patch. The earlier backend router defines an extension point, not a working runtime. To implement inference, vendor a license-compatible pinned llama.cpp revision, build Android arm64-v8a via NDK/CMake, implement JNI bindings with cancellation, streaming tokens, thread safety, memory limits and lifecycle, then test on devices. No false claim of functional llama.cpp support is made.

## Build verification
No Gradle build was run or verified here. Verify `:app:assembleDebug` and device behavior after ensuring the existing MLC native prerequisites are installed.

## Limitations
- Import copies models into private app storage; enough free space is required.
- Import currently runs on IO dispatcher but has no progress notification or resume support.
- GPU clock may be inaccessible; utilization requires platform-specific APIs.
- Analytics history is in-memory and resets when screen closes.
- The existing MLC model manager is unchanged.
