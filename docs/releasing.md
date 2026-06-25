# Releasing & installing on your own devices

Listening is **manual** now (the auto-start-on-call hook + `READ_PHONE_STATE` were removed): start
the phone with the **Start listening** button, and the watch with **tap to listen**.

## Signing

Both APKs are signed with **one release keystore** (`vocalis-release.jks`, gitignored) — the same key
+ `applicationId` is required for the Data Layer to pair phone↔watch and for installs to persist.
Local builds read `keystore.properties` (gitignored); CI reads `VOCALIS_*` env vars from secrets.

`keytool` command used to create it (validity ~27 years):
```
keytool -genkeypair -keystore vocalis-release.jks -alias vocalis -keyalg RSA -keysize 2048 \
        -validity 10000 -storepass <pw> -keypass <pw> -dname "CN=Vocalis, O=Vocalis, C=BG"
```
Keep `vocalis-release.jks` + `keystore.properties` safe and backed up — losing the key means you
can't update an installed app (you'd have to uninstall + reinstall).

## Build signed APKs locally

```
export JAVA_HOME=/path/to/jdk-21        # a full JDK with jlink (e.g. E:/android_studio/jbr)
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew :app:assembleRelease :wear:assembleRelease
#  app/build/outputs/apk/release/app-release.apk    -> phone
#  wear/build/outputs/apk/release/wear-release.apk  -> watch
```

## Install on your devices (sideload)

```
# phone (USB or wireless adb)
adb -s <phone>  install -r app/build/outputs/apk/release/app-release.apk
# watch (wireless debugging)
adb -s <watch>  install -r wear/build/outputs/apk/release/wear-release.apk
```

## CI release (GitHub Actions → GitHub Release)

`.github/workflows/release.yml` builds both signed APKs and attaches them to a GitHub Release.

1. Add repo **secrets** (Settings → Secrets and variables → Actions):
   - `KEYSTORE_BASE64` — base64 of the keystore. On Windows PowerShell:
     `[Convert]::ToBase64String([IO.File]::ReadAllBytes("vocalis-release.jks"))`
   - `KEYSTORE_PASSWORD`, `KEY_PASSWORD` — from `keystore.properties`
   - `KEY_ALIAS` — `vocalis`
2. Tag a release: `git tag v1.0.0 && git push origin v1.0.0`.
3. The workflow publishes a Release with `vocalis-phone.apk` + `vocalis-watch.apk` attached.
   (A manual `workflow_dispatch` run just uploads them as build artifacts.)

Tip: install on the phone with **Obtainium** pointed at the GitHub repo for one-tap updates.

## F-Droid / Play Store

- **F-Droid:** not possible — it's FOSS-only and rejects Google Play Services (the watch needs
  `play-services-wearable`).
- **Play Store:** possible but heavy ($25 account + review; audio permissions get scrutiny). Use the
  internal-testing track at most. Sideloading is simpler for personal use.
