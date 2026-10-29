# Enterprise Phase 1 foundation patch

Implemented: backend interface, router, model descriptor, validated model paths, and two JUnit test classes.

Not yet implemented: llama.cpp adapter, GGUF inference, Room, RAG, voice, agents, enterprise dashboard, signing. The existing MLC engine remains unchanged.

Build in Android Studio from `android/MLCChat`. After satisfying the existing MLC native library requirements, run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

**Build not verified in this environment.** Existing model IDs must be simple folder names. Migrate any legacy model IDs containing slashes before using this patch.
