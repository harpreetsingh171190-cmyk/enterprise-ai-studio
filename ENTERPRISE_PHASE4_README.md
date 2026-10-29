# Phase 4 — llama.cpp JNI integration (source implementation)

## Delivered
- CMake bridge linking `llama` and `enterprise_gguf` native libraries.
- JNI model load, CPU inference, token callbacks, cancellation and unload.
- GGUF inference UI with context, max tokens, temperature, top-p and stop.
- Conservative RAM preflight estimate and low-memory refusal.
- Opt-in native build so the existing MLC build does not fail when llama.cpp is absent.

## REQUIRED external source and build
The llama.cpp source is **not bundled** in this ZIP. Native integration is not executable until a compatible source checkout is added.
From `android/MLCChat/app/src/main/cpp`:
`git clone https://github.com/ggml-org/llama.cpp.git llama.cpp`
Pin and test a known compatible commit before shipping. The JNI source targets the modern llama.cpp APIs (`llama_model_load_from_file`, `llama_memory_clear`, sampler chain). These APIs change; adapt to the checked-out revision.
Build: `cd android/MLCChat && ./gradlew :app:assembleDebug -PenableLlamaCpp=true`
Requires Android SDK, NDK, CMake and the pre-existing MLC project dependencies.

## Limitations / validation required
- No native library or tested APK bundled. Native build and physical-device tests have NOT been run.
- CPU-only baseline; GPU acceleration not implemented.
- GGUF chat templates are not yet applied: the prompt is raw text.
- Output UTF-8 pieces may span token boundaries; production code should buffer incomplete UTF-8 sequences.
- Context overflow during long generation is not proactively checked; set max tokens conservatively.
- Stop is cooperative; model load may not stop immediately.
- Memory estimate is heuristic; Android may kill the app on allocation pressure.
- Existing MLC Chat is unchanged. GGUF inference has its own screen.
- Do not ship before implementing stricter lifecycle synchronization, testing cancellation, UTF-8 streaming and low-memory behavior.
