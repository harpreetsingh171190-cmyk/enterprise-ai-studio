# Phase 2: Android dashboard, local chat history and model manager

Implemented:
- Compose dashboard with a seven-day chart of saved chat snapshots (real database counts, not sample values).
- Local SQLite database with sessions and messages, and read-only history browser/delete.
- Explicit **Save** action in chat; saving does not automatically occur on every message.
- Dashboard navigation and existing model download/load/pause/delete screen renamed Model Manager.

Limitations:
- Database is app-private but NOT encrypted; do not store secrets until encryption is added.
- Saved sessions are read-only; reopening a saved session in the inference engine is not supported.
- The existing model manager remains MLC-only; GGUF is not yet functional.
- Charts show saved session counts, not token/s, GPU or RAM metrics.
- This is source code, not a compiled/tested APK. MLC native dependencies are needed for a build.

Build: open android/MLCChat in Android Studio and run `./gradlew :app:assembleDebug` after provisioning Android SDK, NDK and MLC libraries.
