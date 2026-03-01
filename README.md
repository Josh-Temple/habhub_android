# HabHub Android

HabHub の Android 版（Jetpack Compose + Room）です。

## 現在の実装範囲

- Today 画面（未完了を上、完了を下で表示）
- Bottom Navigation（Today / History / Settings の画面切替）
- History 画面（直近の完了件数を日付ごとに表示）
- Settings 画面（通知ON/OFFトグルの基礎UI）
- 完了トグル（Room の `completion_logs` を更新）
- ハビット追加ダイアログ（タイトル・通知時刻・Webリンク・アプリリンク）
- 入力バリデーション（必須タイトル / 時刻形式 / Web URL / アプリリンク）
- Web / アプリリンク起動（解決可能性チェック + 失敗時 Snackbar）
- 通知権限リクエスト（Android 13+）
- リマインダーのスケジューリング基盤（WorkManager + 翌日再スケジュール）
- Room DB（habits / schedules / links / completion_logs）
- システムのライト/ダークに追従するミニマルテーマ

## プロジェクト構成

- `app/src/main/java/com/habhub/android/ui` : Compose 画面 / ViewModel
- `app/src/main/java/com/habhub/android/repository` : Repository
- `app/src/main/java/com/habhub/android/data` : Room Entity / DAO / Database
- `app/src/main/java/com/habhub/android/notifications` : 通知スケジューリング / Worker
- `app/src/main/java/com/habhub/android/util` : バリデーションユーティリティ
- `docs/product_wireframe_db_schema.md` : ワイヤー + DBスキーマ設計
- `docs/execution_plan.md` : 段階的な実行計画
- `docs/weekly_implementation_tickets.md` : 今週の実装チケット

## 起動手順

1. Android Studio (JDK 17) を使用
2. リポジトリを開く
3. Gradle Sync
4. `app` を実機またはエミュレータで実行

## 現時点の既知課題

- この実行環境では Android Gradle Plugin の解決に失敗し、`assembleDebug` が完走しないことがあります。
- Habit 作成/編集の詳細（曜日ルール、通知アクション、リンク詳細編集）は拡張余地があります。
- Settings のトグル状態永続化（DataStore など）は次フェーズです。

## CI: Debug APK build

- `.github/workflows/build-android-debug-apk.yml` は `Josh-Temple/Ingrain/.github/workflows/reusable-android-debug-apk.yml@v1` を利用しています。
- 必須 secrets: `DEBUG_KEYSTORE_BASE64`, `DEBUG_KEYSTORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD`。
- `allow_ephemeral_signing: false` のため、secrets 未設定時は workflow が失敗します。
