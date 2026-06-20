# Task: wire the on-device (offline) engine

Status: **not started** — `OnDeviceEngine` compiles but only logs; the online path is done.
Owner file: [app/src/main/java/website/ahdesign/vocalis/companion/offline/OnDeviceEngine.kt](../app/src/main/java/website/ahdesign/vocalis/companion/offline/OnDeviceEngine.kt)

## Goal

When there's no internet, the phone (the "brain") transcribes + translates + suggests a short reply
**on-device**, so the watch still works in a shop with no signal. Output must be the same
`Turn { heard, meaning, reply, engine, tsEpochMs }` the online path emits — the watch UI is already
engine-agnostic, so nothing on the watch changes.

## How it plugs in (already built)

`Pipeline.chooseEngine()` returns `OFFLINE_GEMMA` / `OFFLINE_MLKIT` when the network isn't validated,
then calls `OnDeviceEngine.process(pcm, engine, onTurn)`. So this task is **only** filling in
`processGemma()` and `processMlKit()` to consume the PCM `InputStream` and call `onTurn(Turn(...))`.
Reuse `parseLlmAnswer()` from `:core` so the model's JSON is parsed exactly like OpenRouter's.

## Strategy A — gemma-4-e4b-it (primary)

The model from Google AI Edge Gallery. Multimodal **including audio**, pretrained on 140+ languages
(strong Bulgarian), so one model does ASR + translation + reply.

- **Runtime:** LiteRT-LM Kotlin API (preferred; MediaPipe LLM Inference is maintenance-mode).
  Candidate dep: `com.google.mediapipe:tasks-genai:<latest>` (MediaPipe LLM Inference) or the
  LiteRT-LM artifact — verify the current name/version against the docs before adding.
- **Model file:** `gemma-4-e4b-it` in `.litertlm` (or `.task`) format, downloaded into
  `context.filesDir` as `gemma-4-e4b-it.litertlm` (the path `OnDeviceEngine.gemmaReady()` already
  checks). Add a one-time downloader (with progress) — the file is hundreds of MB; require Wi-Fi.
- **Init:** lazily create the LLM inference session pointing at the model file; reuse across turns.
- **Per utterance:** chunk the PCM by simple VAD (or fixed windows), feed each as **audio input**
  with a prompt asking for `{transcript, translation, short reply + phonetic}` as JSON. Parse with
  `parseLlmAnswer` (extend it if a `transcript` field is added), then `onTurn(...)`.
- **Risks:** needs a capable phone (RAM/NPU); first token latency; model download size/UX.

## Strategy B — ML Kit translate (lighter fallback)

When running a full LLM is too slow / not downloaded.

- **ASR:** bundled Whisper (`whisper.cpp`/tflite) or Android on-device `SpeechRecognizer` **if** a
  `bg` pack exists (often it doesn't — Whisper is the reliable route for Bulgarian).
- **Translate:** ML Kit on-device, Bulgarian supported.
  `com.google.mlkit:translate:<latest>`, `TranslateLanguage.BULGARIAN` → user's language;
  `translator.downloadModelIfNeeded()` once.
- **Reply:** a small on-device LLM or a phrasebook/template keyed by intent; keep it short.
- **Risks:** Bulgarian offline ASR is the weak link; reply quality lower than Gemma.

## Acceptance criteria

- [ ] With no internet and a downloaded Gemma model, speaking Bulgarian near the watch produces a
      `Turn` on the watch (heard + meaning + short reply), engine badge shows "offline".
- [ ] With no model present, falls back to Strategy B (or a clear "needs model/internet" message).
- [ ] `./gradlew check` stays green (detekt/spotless/lint/tests).
- [ ] Model download is one-time, Wi-Fi-gated, with visible progress; never blocks the UI thread.

## References

- LiteRT-LM / LLM Inference (Android): https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
- Gemma 4 model card: https://ai.google.dev/gemma/docs/core/model_card_4
- ML Kit translation languages (incl. `bg`): https://developers.google.com/ml-kit/language/translation/translation-language-support
