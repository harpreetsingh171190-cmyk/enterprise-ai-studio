# Phase 5 — Pinned llama.cpp, JNI compile CI, chat templates, UTF-8 streaming

Changes:
- Pinned llama.cpp source acquisition to `v0.8.0` in `LLAMA_CPP_VERSION.txt`.
- Added Android NDK cross-compile GitHub Actions workflow; catches JNI C++ compilation failures on CI.
- Added model-specific GGUF chat template formatting through llama.cpp API.
- Replaced unsafe per-token `NewStringUTF` with byte-array JNI callbacks and incremental UTF-8 decoding.
- Added UTF-8 streaming unit tests (including Punjabi and emoji).
- Added basic context overflow guard and portable Android CMake flags.

Build prerequisite:
`cd android/MLCChat/app/src/main/cpp && bash setup-llama.sh`
`cd android/MLCChat && ./gradlew :app:testDebugUnitTest :app:assembleDebug -PenableLlamaCpp=true`

Validation status:
- Source edited, ZIP checked.
- Not compiled: this environment has no Android SDK/NDK and cannot access GitHub to fetch llama.cpp.
- The pinned tag is a reproducibility target, NOT a proven compatible native build.
- CI workflow is added but has NOT run. Its CMake/API compatibility may need fixes after the first run.
- Chat template supports system+single user turn; multi-turn structured history is not yet implemented.
- Cancellation, model lifecycle synchronization, native memory limits and error handling need device stress tests before production release.
- No claim of a production-ready APK or production-certified streaming.
