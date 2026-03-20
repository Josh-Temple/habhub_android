# HabHub Android

HabHub の Android 版（Jetpack Compose + Room）です。

## 現在の実装範囲

- Today 画面（未完了を上、完了を下で表示）
- Bottom Navigation（Today / Habits / Settings の画面切替）
- Habits 画面（登録済みハビット一覧 + 編集導線）
- Settings 画面（通知ON/OFF・テーマ・表示サイズを永続設定）
- Today ヘッダーのケバブメニュー（ヘルプ / 1日の切り替え時刻 0:00〜5:00）
- 1日の切り替え時刻設定（既定0:00、例:3:00設定時は2:59まで前日扱い）
- 表示カスタム（フォントサイズ: Small / Normal / Large）
- 習慣ごとの色設定（アイコン色トークン）
- 完了トグル（Room の `completion_logs` を更新）
- ハビット追加/編集ダイアログ
  - タイトル
  - アイコン（固定プリセット30個）
  - 通知時刻
  - Webリンク / アプリリンク
  - 曜日指定（任意）
  - 開始日（必須）/ 終了日（任意）
- 入力バリデーション（必須タイトル / 時刻形式 / Web URL / アプリリンク / 日付形式 / 日付範囲）
- Web / アプリリンク起動（解決可能性チェック + 失敗時 Snackbar）
- 通知権限リクエスト（Android 13+、通知ON時/リマインダー時刻設定時の文脈で要求）
- リマインダーのスケジューリング基盤（WorkManager + 翌日再スケジュール）
- 通知条件の考慮（曜日外・期間外は通知しない）
- Room DB（habits / schedules / links / completion_logs）
- ミニマルテーマ（既定はシステム追従、Settings で Light/Dark/System を選択して永続化）

## プロジェクト構成

- `app/src/main/java/com/habhub/android/ui` : Compose 画面 / ViewModel
- `app/src/main/java/com/habhub/android/repository` : Repository
- `app/src/main/java/com/habhub/android/data` : Room Entity / DAO / Database
- `app/src/main/java/com/habhub/android/notifications` : 通知スケジューリング / Worker
- `app/src/main/java/com/habhub/android/util` : バリデーションユーティリティ
- `docs/product_wireframe_db_schema.md` : ワイヤー + DBスキーマ設計
- `docs/execution_plan.md` : 段階的な実行計画
- `docs/weekly_implementation_tickets.md` : 今週の実装チケット
- `HANDOFF.md` : セッション引き継ぎ資料

## 起動手順

1. Android Studio (JDK 17) を使用
2. リポジトリを開く
3. Gradle Sync
4. `app` を実機またはエミュレータで実行

### Java / Gradle 実行メモ

- このリポジトリは **Java 17** 前提での Gradle 実行を想定しています。
- `mise` を使う環境では `.mise.toml` により `java = 17.0.2` を選択します。
- もしシェル側で Java 24+ が既定になっている場合でも、`gradlew` は以下を優先して Java 17 へフォールバックします。
  - `JAVA17_HOME`
  - `$HOME/.local/share/mise/installs/java/17.0.2`
- Android Studio / CI でも JDK 17 を指定してください。


## Intentリンクの検証メモ（AnkiDroid）

Settings の「Test Android Settings intent」で intent 起動経路が動作することを確認後、AnkiDroid は以下の書式を推奨します。

- ✅ 起動確認できたシンプル形式
  - `intent:#Intent;package=com.ichi2.anki;end`
- ⚠️ 端末差で失敗する可能性がある形式（必要時のみ試す）
  - `intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;package=com.ichi2.anki;end`

まずはシンプル形式を Habit の App link に登録して検証してください。

## 現時点の既知課題

- この実行環境では Android Gradle Plugin の依存取得がネットワーク制約で失敗し、`assembleDebug` が完走しないことがあります。
- Java 24+ を既定 JDK にしている環境では、Gradle Kotlin DSL 初期化時に `java.lang.IllegalArgumentException: 25.0.1` のようなエラーになることがあり、Java 17 での実行が必要です。
- 設定は DataStore へ集約（通知ON/OFF・日付切替時刻・表示サイズ・テーマ）。
- 旧 SharedPreferences (`habhub_prefs`) から DataStore への移行を入れており、既存ユーザー値を引き継ぎます。
- `app/src/test/java/com/habhub/android/util/BusinessDateTest.kt` で日付切り替えロジックのユニットテストを追加しています。
- 曜日・期間ルールに基づく通知は対応済みですが、ルール変更時の通知再評価ケース（大量データ時の最適化）は改善余地があります。

## CI: Debug APK build

- `.github/workflows/build-android-debug-apk.yml` は 2段構成です。
  - `baseline-build`: secrets 不要で `testDebugUnitTest` + `assembleDebug` を実行
  - `signed-debug-apk`: reusable workflow（`Josh-Temple/Ingrain/...@v1`）を常に呼び出し、signing secrets の有無判定は reusable workflow 側に委譲
