# Vocalis

Vocalis helps you take part in Bulgarian conversations when your Bulgarian is
limited. It listens to speech, transcribes it, translates it, and suggests a
short reply you can say back.

## What it is

There are three ways to use it:

- Phone app. During a phone call on speakerphone, it listens to the mic and shows
  a translation plus a short suggested reply in a floating overlay.
- Watch app (Wear OS). For talking to someone in person. Tap to listen and the
  watch streams audio to the phone, which does the work and sends the reply back
  to your wrist.
- VoIP path (a separate repo). Routes a call through a VoIP number to your
  computer so it works without speakerphone.

Speech to text is done by Deepgram. Translation and the reply come from a large
language model through OpenRouter.

## What it does

- Live transcription of Bulgarian speech.
- A short English translation of what was said.
- A short reply in Bulgarian, written in a script you can read (Latin, Cyrillic,
  and a few others).
- History grouped by session, with a topic tag per session and search.
- Listening is manual. You start it from the phone button or by tapping the watch.

## Requirements

- An Android phone running Android 8 (API 26) or newer.
- For the watch app, a Wear OS 3 watch (API 30) or newer.
- Your own Deepgram and OpenRouter API keys. You enter them in the app settings;
  they are not bundled in the app.

## Install

Download the APKs from the Releases page:

- vocalis-phone.apk for the phone
- vocalis-watch.apk for the watch

Install them with adb:

    adb install -r vocalis-phone.apk
    adb install -r vocalis-watch.apk

Open the phone app, go to Settings, and enter your Deepgram and OpenRouter keys.

The phone and watch apps must be signed with the same key to talk to each other.
The release APKs are, so install both from the same release.

## Develop

You need a full JDK 17 or newer that includes jlink. A stripped JetBrains runtime
without jlink will not build the project.

    export JAVA_HOME=/path/to/jdk
    export ANDROID_HOME=/path/to/Android/Sdk
    ./gradlew check               # formatting, static analysis, lint, unit tests
    ./gradlew :app:assembleDebug
    ./gradlew :wear:assembleDebug

Modules:

- core: shared logic. Audio helpers, prompt building, the Deepgram and OpenRouter
  calls, and the data types the watch and phone exchange.
- app: the phone app and the service that processes audio coming from the watch.
- wear: the Wear OS app.

Release signing and the release workflow are described in docs/releasing.md.

## Contribute

Issues and pull requests are welcome. Before opening a pull request, run
`./gradlew check` and make sure it passes. That runs ktlint formatting, detekt
static analysis, Android lint, and the unit tests.

## License

No license has been chosen yet. Add one before sharing or reusing the code.
