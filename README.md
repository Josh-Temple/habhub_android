# habhub_android
HabHub for Android

## CI: Debug APK build
- `.github/workflows/build-android-debug-apk.yml` は `your-org/Ingrain/.github/workflows/reusable-android-debug-apk.yml@v1` を利用しています。
- 必須 secrets: `DEBUG_KEYSTORE_BASE64`, `DEBUG_KEYSTORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD`。
- `allow_ephemeral_signing: false` のため、secrets 未設定時は workflow が失敗します。
