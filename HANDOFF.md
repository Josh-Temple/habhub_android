# HANDOFF

## 今回の対応内容（UIトーン調整）
- 参照画像の雰囲気に合わせ、**ライトテーマ配色を寒色系グレー + 控えめオレンジアクセント**へ更新。
- タイポグラフィを見直し、見出しの太さ/字間と本文行間を調整して、静かで読みやすい紙面寄りの印象に寄せた。
- 区切り線の色をハードコードからテーマ由来に変更し、画面全体の色調整へ追従するよう統一。
- Todayヘッダー内のサブタイトルをラベル調にして、情報階層を強調。

## 今回の対応内容（要望反映）
- **Habits一覧で並び替え**（↑/↓ボタン）を追加し、`sort_order` を永続化。
- Today 画面の並びは既存の **未完了→完了** を維持しつつ、各グループ内で `sort_order` が効く構成を維持。
- **単発タスク（One-time task）** を追加。
  - フォームに One-time トグルを追加。
  - One-time 選択時は曜日選択を無効化し、保存時に `repeatType=ONE_TIME` で扱う。
- **完了済み時は通知を抑止**するガードを `ReminderWorker` に追加。
- ベルアイコンは **Today では非表示**、**Habits一覧に表示**へ移動。
- Today の完了トグルに **控えめなスケールアニメーション**を追加。

## 追加リファクタリング
- `HabitManageRow` から未使用の `reminder_enabled` を削除し、DAO投影を簡素化。
- Habits一覧の並び替えボタンは、先頭/末尾で無効化（no-op をUIで抑制）。
- 並び替えボタンのcontentDescriptionを文字列リソース化（`move_up`, `move_down`）。

## 主要変更ファイル
- `app/src/main/java/com/habhub/android/data/HabitEntities.kt`
- `app/src/main/java/com/habhub/android/data/HabitDao.kt`
- `app/src/main/java/com/habhub/android/data/HabHubDatabase.kt`
- `app/src/main/java/com/habhub/android/domain/Model.kt`
- `app/src/main/java/com/habhub/android/repository/HabitRepository.kt`
- `app/src/main/java/com/habhub/android/ui/HabitViewModel.kt`
- `app/src/main/java/com/habhub/android/ui/HabHubApp.kt`
- `app/src/main/java/com/habhub/android/notifications/ReminderWorker.kt`
- `app/src/main/res/values/strings.xml`

## 積み残し確認
- 今回要望として受けた項目（並び順反映、単発タスク、完了済み通知抑止、ベル表示位置変更、Today完了アニメーション）は実装済み。
- ただし**実機E2E確認**（通知タイミング、日付境界、One-time運用）まではこの環境で未完了。

## 次セッションでの確認観点
1. Habits一覧で並び替え → Today画面の未完了/完了各グループ内順序に反映されること。
2. One-timeタスク作成後、完了済み時に同日通知が表示されないこと。
3. ベルアイコンがTodayには出ず、Habits一覧にのみ表示されること。
4. Today完了トグル時のアニメーションが過度でなく、操作感を阻害しないこと。
5. DB v2 への移行端末で既存データが想定どおり扱えること（destructive migration許容方針の確認）。
