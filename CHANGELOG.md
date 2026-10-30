# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Added
- Release workflow: pushing a `v*` tag builds the APK and attaches it to a GitHub Release
- Dependabot for GitHub Actions and Gradle dependencies
- Architecture documentation and a full README

## [0.2.0] - Inference screen improvements

### Added
- CPU thread count slider
- Tokens-per-second in the status line after generation
- Optional system prompt, Clear and Reset to defaults buttons
- Character and word count for responses
- Temperature presets (Precise / Balanced / Creative)
- Prompt templates and "Reuse last prompt"
- Copy response button
- Save to history button (writes the conversation to the local SQLite database)

### Fixed
- Build failure caused by `mutableFloatStateOf`, which does not exist in the Compose runtime used by
  this project (Compose BOM 2022.10.00). Replaced with `mutableStateOf`.

## [0.1.0] - Initial enterprise foundation

### Added
- llama.cpp JNI integration with a pinned llama.cpp version
- GGUF model import, catalog and manager screen
- Local chat history database and viewer
- Dashboard and live RAM diagnostics
- CI workflows for the debug APK and the native JNI compile check
