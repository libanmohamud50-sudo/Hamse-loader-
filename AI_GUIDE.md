# Key Login Android App - AI Operating Guide

You are helping the owner maintain a legitimate license-key login app: the user enters a license key, the app verifies it against the owner's own Cloudflare Worker, then shows a dashboard. Scope: license verification, UI customization, online config, in-app updates. Never add game-cheating, bypass, injection, hooking, evasion or anything malicious.

## How to work with the owner
- The owner works ONLY in Termux on an Android phone. Every answer must be copy-paste ready: ONE bash block (with python3 heredoc patches inside when code must change).
- The owner usually writes Somali. Reply in the language they write. ALL app text, warnings and messages stay English.
- You cannot see the files. If you need the current state, ask the owner to run and paste: cfg ; cat <file> ; git log --oneline -5 ; ls -R app/src/main | head -60. Never guess file contents.

## Decision rule for every request
A) Can it be done through config.json? Then answer with cfg / card commands ONLY. No rebuild, no reinstall. The app picks it up within about 1 minute (when opened or resumed).
B) It needs code changes? Then give ONE script: git pull, python patches with assertions, then: up "message". Tell the owner to watch GitHub Actions. If it fails (red), ask for the lines after "What went wrong" or "error:" and fix with a small patch.
Prefer small patches over rewriting whole files. Never change several unrelated things at once.

## Project facts
- Termux folder: ~/keylogin. GitHub repo: libanmohamud50-sudo/Hamse-loader- (branch main).
- Package and applicationId: com.example.keylogin. AGP 8.2.1, Gradle 8.2.1, JDK 17, compileSdk 34, minSdk 24, Java 17 compatibility, Material Components, no Kotlin.
- Files (app/src/main/java/com/example/keylogin/):
  - LoginActivity.java: reads cached config, then refreshes it online (also in onResume). applyConfig() sets title, subtitle, accent (login button tint), maintenance message, notice, and checks for updates (latest_version greater than installed versionCode and update_url not empty gives an UPDATE dialog that calls Updater.start). doLogin() calls verify(): GET api_url?key=KEY (URL-encoded). On success it opens MainActivity with extras "expire" and "key".
  - MainActivity.java: dashboard. Header (dashboard_title + LOG OUT). If dashboard_url starts with https:// it shows a WebView (JavaScript and DOM storage on, no JS interface). Otherwise it builds native views: a LICENSE card (status, masked key, expire, days left), an optional NOTICE card, then every item of cards[] (title, text, optional button + https url).
  - RemoteConfig.java: downloads config.json through the GitHub API (api.github.com/repos/libanmohamud50-sudo/Hamse-loader-/contents/config.json?ref=main, header Accept: application/vnd.github.raw), caches it in SharedPreferences "cfg" key "json". All failures are silent; the app keeps the cached copy.
  - Updater.java: in-app update. Downloads update_url to cache/updates/update.apk showing "Updating... N%" then "Done", then opens the Android installer via FileProvider. Asks for the "install unknown apps" permission when needed.
- Resources: res/layout/activity_login.xml (ids: title, subtitle, keyInput, pasteBtn, eyeBtn, status, langBtn, loginBtn, getKeyBtn), drawables bg_input, ic_lock, ic_paste, ic_eye, values/styles.xml (AppTheme, MaterialComponents DayNight NoActionBar), xml/file_paths.xml, AndroidManifest.xml (android:label and android:icon are compile-time).
- Colors: background #0A0A0F, card #14141C, accent #00DDF8, muted text #9AA0AE, error #FF6B6B.

## License Worker API (owner's own)
- GET https://a.hamse.workers.dev/verify?key=KEY (POST JSON {"key":"KEY"} also works).
- Valid: {"status":true,"valid":true,"expire":"YYYY-MM-DD HH:MM:SS","msg":"success"}
- Invalid: {"status":false,"valid":false,"reason":"Invalid key"}
- The app only reads valid, expire and reason.

## config.json keys (repo root)
title, subtitle, api_url, get_key_url, accent (hex color), notice, maintenance (true/false), maintenance_msg, latest_version (integer, auto-bumped by CI), force_update (true/false), update_url (auto-set by CI), update_msg, dashboard_title, dashboard_url (https page shown inside the dashboard; empty means native cards), cards (list of objects with title, text, button, url).

## Termux commands (already installed)
- cfg                      show config.json
- cfg key value [key value ...]   set values; true/false/numbers/JSON are parsed. Example: cfg notice "Hello" accent "#FF9800"
- card add "TITLE" "text" "Button" "https://url"     add a dashboard card (button and url optional)
- card              list cards     |  card del N     |  card clear
- up "message"      commit everything, pull --rebase, push. Triggers a build when code changed.
cfg and card pull before editing and push after. config.json and *.md changes do NOT trigger a build.

## CI (.github/workflows/build.yml)
On push (ignoring config.json and *.md): installs JDK 17 and Gradle 8.2.1, creates app/debug.keystore if missing, runs gradle assembleDebug, then publishes the APK as GitHub release tag "latest" (asset app-debug.apk), then sets latest_version = run number and update_url in config.json and commits it. versionCode comes from GITHUB_RUN_NUMBER. If the build fails, the release is NOT replaced, so users never get a broken APK.

## Rules to never crash the app
1. Read every config value with a default (optString/optBoolean/optInt/optJSONArray) and null-check. Wrap color parsing, date parsing, image decoding and network code in try/catch. Never trust config content.
2. No network or disk work on the main thread. Update views only on the UI thread.
3. Do NOT touch: applicationId, namespace, signingConfigs, debug.keystore, versionCode formula, FileProvider authority and file_paths.xml, the permissions (INTERNET, REQUEST_INSTALL_PACKAGES), the RemoteConfig URL, the workflow release and bump steps.
4. Do not add libraries unless absolutely necessary. Do not rename existing ids, classes or config keys. Only add.
5. Every code patch must assert that the text it replaces exists, so a mismatch aborts instead of silently corrupting a file. Use this helper:

    def patch(path, old, new):
        s = open(path).read()
        assert old in s, "pattern not found in " + path
        open(path, "w").write(s.replace(old, new, 1))

6. Never put a literal backslash-n inside a replacement string by accident; it breaks XML and Java. Use real newlines.
7. Before pushing config by hand, validate it: python3 -c "import json;json.load(open('config.json'))". cfg and card already write valid JSON.
8. Keep dialogs and messages in English. Never print or store secrets (tokens, passwords).

## Known limits and first tasks
1. The launcher icon and the app name shown under the icon are compile-time. Changing them means a code change: edit res and the manifest, run up, wait for Actions, then users get it through the in-app UPDATE.
2. The logo on the login screen and dashboard is a built-in vector (ic_lock), not remote yet. FIRST TASK: add remote branding. New config keys logo_url (https PNG or JPG) and app_name. LoginActivity and MainActivity should download the logo in a background thread, cache it in filesDir, decode safely (sampled, size limit about 2 MB), show it in the logo ImageView, and fall back to ic_lock on ANY error. app_name should replace the dashboard header text when set. After that, the owner can change them with: cfg logo_url "https://..." app_name "My App"
3. The language button is only a placeholder list.
