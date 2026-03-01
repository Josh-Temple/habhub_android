# habhub_android
HabHub for Android

## CI: Debug APK build
- `.github/workflows/build-android-debug-apk.yml` は外部 reusable workflow `your-org/Ingrain/.github/workflows/reusable-android-debug-apk.yml@v1`（`owner/repo/path@ref` 形式）を利用しています。
- `uses` は `@main` ではなく固定 ref（現在は `@v1`）を使用しています。必要に応じて commit SHA 固定を推奨します。
- このリポジトリ向けに `with` を設定済みです（`app_module: app`, `gradle_task: assembleDebug`, `apk_glob: **/build/outputs/apk/debug/*.apk`, `artifact_name: habhub_android-debug-apk`）。
- 必須 secrets: `DEBUG_KEYSTORE_BASE64`, `DEBUG_KEYSTORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD`。
- `allow_ephemeral_signing: false` のため、secrets 未設定時は workflow が失敗します。
- private リポジトリ間で利用する場合は、呼び出し元から `your-org/Ingrain` の reusable workflow を参照できるように Actions のアクセス許可（organization/repository settings）を事前に設定してください。
