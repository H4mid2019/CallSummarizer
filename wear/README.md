# SayBack Wear - in-person assistant

A Wear OS companion to the phone app. Raise your wrist in a shop / café / office, **tap the mic**,
and the watch shows what the other person (Bulgarian) said, what it means, and a **short, easy reply
to say back**. Every exchange is saved to history on the phone.

This is the **in-person** tool. It does **not** solve phone calls - on a call the watch mic is just
another acoustic mic. Calls remain the job of the VoIP-to-PC path discussed separately.

## Modules (this is a real, building multi-module project)

```
:core   Android library - shared by phone + watch. Pure logic with no UI:
        rms()/SILENCE_RMS_FLOOR, buildPrompt(), parseLlmAnswer()/LlmAnswer, ReplyScript,
        and the watch<->phone contract (Engine, Turn, Suggestion, Paths, AudioSpec, ResultPayload).
        The unit tests (Audio/LlmParser/PromptBuilder/ReplyScript) moved here with the code.
:app    Phone app (existing) - now depends on :core, plus a new `companion/` package:
        WearAudioListenerService (receives the watch audio channel) -> Pipeline (online/offline
        router) -> OnDeviceEngine (offline) / HistoryStore (history + optional API sync).
:wear   Wear OS app (new) - Compose UI. WearMicService streams mic PCM to the phone; PhoneBridge
        receives results; ListenActivity/ListenScreen are the tap-to-listen screen.
```

## How it works - "watch captures, phone thinks"

The watch ships mic bytes and renders results; the phone does ASR/translate/LLM. That keeps the
watch light, keeps API keys on the phone, and gives history + offline for free.

```
WATCH                                         PHONE
 tap mic                                        WearAudioListenerService (Data Layer)
  WearMicService: AudioRecord 16kHz PCM          reads InputStream
   ChannelClient OutputStream  ───── audio ────▶ Pipeline.chooseEngine(connectivity)
                                                   ├─ ONLINE  -> Deepgram -> OpenRouter (reuse :core)
 ListenScreen: Say-this / Meaning / Heard         └─ OFFLINE -> OnDeviceEngine:
   recent history  ◀──── result JSON ──────────       gemma-4-e4b-it (audio->text+translate+reply)
                          (MessageClient)              or ML Kit translate (Bulgarian) fallback
                                                   HistoryStore (Room) -> optional API sync
```

- **Audio (watch -> phone):** `ChannelClient` (`/vocalis/audio`) - the documented way to stream mic.
- **Results (phone -> watch):** `MessageClient` (`/vocalis/result`) - tiny `ResultPayload` JSON.
- **Control:** `MessageClient` (`/vocalis/listen/start` · `/stop`).

## Online vs offline

`Pipeline.chooseEngine()` checks for a validated network. The result contract is identical either
way, so the watch UI never changes.

| | Engine | Transcribe | Translate | Reply | Notes |
|---|---|---|---|---|---|
| **Online** | Deepgram + OpenRouter (reuse existing app code via :core) | Deepgram | LLM | LLM | best quality |
| **Offline A** | **gemma-4-e4b-it** on-device (LiteRT-LM) | ✅ audio-in | ✅ | ✅ | one model; multimodal incl. audio, pretrained on 140+ langs -> strong Bulgarian |
| **Offline B** | ASR + **ML Kit translate** (`bg`) + short reply | Whisper/SpeechRecognizer | ML Kit | small LLM/template | lighter; reply weaker than Gemma |

Bulgarian offline ASR is the weak link - prefer gemma-4-e4b-it (audio) or a bundled Whisper model
rather than relying on the system recognizer having a `bg` pack.

## "Short, easy reply"

The reply contract is `Suggestion(text, phonetic?)` where `text` is in the user's `ReplyScript`
(Latin/Cyrillic/native...) and `phonetic` is an optional pronunciation hint. The online path reuses
`buildPrompt()` (already tuned for the shortest possible reply); the offline engine is prompted to
emit the same `{translation, reply}` JSON that `parseLlmAnswer()` reads.

## History

`HistoryStore` (phone) keeps every `Turn`; the watch mirrors only the last few. `HistorySync`
optionally POSTs new turns to a user endpoint (off by default; enable via the `history_sync` prefs).

## What's real vs. stubbed

Real and building today: module wiring, shared `:core` extraction (with passing tests), the watch
capture->stream service, the Compose UI, the Data Layer plumbing, the phone listener + engine router,
history scaffold. Stubbed with `Later:` markers (compile, but no real model calls yet): the online
Deepgram/OpenRouter reuse inside `Pipeline.processOnline`, the gemma-4-e4b-it / ML Kit calls in
`OnDeviceEngine`, and the Room persistence + API upload in `HistoryStore`.

## Build & run

```bash
# JAVA_HOME must point at a FULL JDK that includes jlink (Android's JdkImageTransform needs it).
# A stripped JetBrains Runtime (PyCharm/AndroidStudio "jbr") without jlink/jmods will fail.
export JAVA_HOME=/path/to/jdk-21          # e.g. a Temurin 21 JDK
export ANDROID_HOME=/path/to/Android/Sdk

./gradlew :core:testDebugUnitTest          # shared logic tests
./gradlew :app:assembleDebug               # phone APK (+ companion)
./gradlew :wear:assembleDebug              # watch APK
./gradlew check                            # full CI gate: lint + detekt + spotless + tests
```

The phone and watch APKs share `applicationId` (`website.ahdesign.vocalis`) and must be signed with
the **same key**, or the Data Layer won't pair them.

## Next implementation steps

1. Extract the Deepgram WS + OpenRouter call out of `CallService` into `:core`, then call it from
   `Pipeline.processOnline` (so phone calls and the watch share one online pipeline).
2. Wire `OnDeviceEngine` to LiteRT-LM with a downloaded gemma-4-e4b-it model (audio in -> JSON out).
3. Add Room to `HistoryStore`; build a phone-side history screen.

## Sources

- [LLM Inference / LiteRT-LM for Android - Google AI Edge](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
- [Gemma 4 model card](https://ai.google.dev/gemma/docs/core/model_card_4)
- [ML Kit translation: supported languages](https://developers.google.com/ml-kit/language/translation/translation-language-support)
- [Wear OS - Data Layer / ChannelClient](https://developer.android.com/training/wearables/data/data-layer)
