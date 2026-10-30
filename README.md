# Enterprise AI Studio (Android)

Offline Android app for running **GGUF** models on-device with llama.cpp (JNI), with a local
dashboard, chat-history database, live RAM diagnostics and a GGUF model manager.

> This project started as a fork of MLC-LLM. The MLC/TVM engine has been removed; inference is
> llama.cpp only. Original MLC-LLM is Apache-2.0 licensed (see `LICENSE`).

## Build
```bash
cd android/MLCChat/app/src/main/cpp && bash setup-llama.sh   # clones pinned llama.cpp
cd ../../../.. && ./gradlew :app:assembleDebug -PenableLlamaCpp=true
```
Requires Android SDK + NDK + CMake. See `ENTERPRISE_PHASE*_README.md` for phase notes.

## Status
Native build and on-device tests have not been run yet; CI workflows are provided for both.
