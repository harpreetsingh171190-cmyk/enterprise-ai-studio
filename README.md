# Enterprise AI Studio (Android)

[![Build Android APK](https://github.com/harpreetsingh171190-cmyk/enterprise-ai-studio/actions/workflows/build-apk.yml/badge.svg)](https://github.com/harpreetsingh171190-cmyk/enterprise-ai-studio/actions/workflows/build-apk.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF)

An offline Android app that runs **GGUF language models on-device** using
[llama.cpp](https://github.com/ggml-org/llama.cpp) through JNI. No cloud, no account: models are imported
from local storage and stay in app-private storage.

> This project started as a fork of MLC-LLM. The MLC/TVM engine was removed and inference is now
> llama.cpp only. The original MLC-LLM is Apache-2.0 licensed (see [LICENSE](LICENSE)).

## Features

**Inference**
- Native llama.cpp inference (arm64-v8a) with streaming token output and a Stop button
- Adjustable context size, max new tokens, temperature, top-P and CPU thread count
- Temperature presets (Precise / Balanced / Creative) and optional system prompt
- Prompt templates (summarize, translate, explain, fix grammar) and "reuse last prompt"
- Live status with tokens-per-second after each generation
- Copy response to clipboard, character and word count

**Model management**
- Import `.gguf` files with header validation and a SHA-256 checksum
- Size limit, safe file naming and a catalog stored in app-private storage
- RAM estimate before loading so large models fail with a clear message, not a crash

**Data and diagnostics**
- Local SQLite chat history with a "Save to history" button and a read-only viewer
- Dashboard with a 7-day chart of saved sessions
- Live device diagnostics: free RAM, app PSS memory trend (GPU usage is shown as unavailable
  rather than guessed)

## Screenshots

<!-- Add screenshots to docs/screenshots/ and link them here, for example:
![Inference screen](docs/screenshots/inference.png) -->

## Architecture

Jetpack Compose UI -> `GgufRuntime` (Kotlin) -> JNI (`enterprise_gguf`) -> llama.cpp.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full picture.

## Build

The easiest way is GitHub Actions: the **Build Android APK** workflow builds a debug APK (with the pinned
llama.cpp) on every push to `main` that touches `android/`. Download it from the run's **Artifacts**.

Local build (needs Android SDK, NDK and CMake):

```bash
cd android/MLCChat/app/src/main/cpp && bash setup-llama.sh   # clones the pinned llama.cpp
cd ../../../.. && ./gradlew :app:assembleDebug -PenableLlamaCpp=true
```

Without `-PenableLlamaCpp=true` the app still builds, but the native runtime is not included and the
inference screen says so.

## Releases

Pushing a tag such as `v1.0` (or publishing a release with a new tag on GitHub) triggers the workflow,
which builds the APK and attaches it to the release automatically. Releases are marked as pre-release
because they are debug builds.

## CI

| Workflow | Purpose |
| --- | --- |
| `build-apk.yml` | Builds the debug APK with llama.cpp, uploads it as an artifact, publishes tagged releases |
| `android-llama-jni.yml` | Compile check for the native JNI library with the Android NDK |

Dependabot keeps the GitHub Actions versions up to date.

## Project status and limitations

- CI builds the APK including the native library. Inference speed depends heavily on the device, the
  model size and its quantization.
- Chat history is stored in a local SQLite database and is **not encrypted at rest**.
- RAM checks are estimates, not a guarantee.
- Only CPU inference is supported.

Earlier development notes are in the `ENTERPRISE_PHASE*_README.md` files.

## License

Apache-2.0. See [LICENSE](LICENSE).
