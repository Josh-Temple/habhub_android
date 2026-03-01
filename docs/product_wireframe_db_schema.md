# HabHub Android 設計書（ワイヤー / DBスキーマ）

## 1. デザイン前提

- 絵文字は利用しない。
- アイコンは Google Material Symbols（Rounded 推奨）を利用する。
- 配色は「モノクロ + Accent 1〜2色」を原則とする。
- 情報表現は余白とタイポグラフィを優先し、テキスト量は最小化する。

### 1.1 カラートークン（例）

- `Mono/Background` : `#FFFFFF`
- `Mono/Surface` : `#FAFAFA`
- `Mono/TextPrimary` : `#111111`
- `Mono/TextSecondary` : `#666666`
- `Mono/Divider` : `#E6E6E6`
- `Accent/Primary` : `#2563EB`（操作・選択状態）
- `Accent/Secondary` : `#14B8A6`（補助状態・ハイライト）

### 1.2 タイポグラフィ（例）

- Title Large: 28sp / Medium
- Title: 20sp / Medium
- Body: 16sp / Regular
- Caption: 12sp / Regular

### 1.3 アイコンポリシー

- 文字ラベルの代替が成立する項目はアイコンのみで表現。
- 初見で誤解しやすい操作のみ、補助ラベルを薄く表示。
- 推奨 Material Symbols:
  - 完了: `check_circle`
  - 未完了: `radio_button_unchecked`
  - リマインダー: `notifications`
  - Webリンク: `open_in_browser`
  - アプリリンク: `open_in_new`
  - 編集: `edit`
  - 追加: `add`
  - 並び替え補助: `sort`

---

## 2. 画面ワイヤー

## 2.1 Today（ホーム）

目的: 今日のハビットを「未完了優先」で即操作できる画面。

```
┌─────────────────────────────────────────────┐
│  HabHub                               [設定] │
│  Today                                 [検索] │
│                                             │
│  [未完了 4]                                 │
│  ────────────────────────────────────────   │
│  ○  深呼吸 5分                      [notifications][open_in_new]  │
│     20:00                                 │
│  ○  英語学習                        [open_in_browser]      │
│     21:00                                 │
│  ○  ストレッチ                        [notifications]    │
│                                             │
│  [完了 2]                                   │
│  ────────────────────────────────────────   │
│  ●  読書 20分                      [notifications][open_in_browser]  │
│  ●  日記                                   │
│                                             │
│                                  [＋]       │
└─────────────────────────────────────────────┘
```

注記:
- 設計方針としては絵文字不使用。上記アイコン表現はすべて Material Symbols 名で統一する。
- リストは当日対象ハビットのみ表示。
- 並び順: 未完了（手動順）→ 完了（手動順）。

### 2.1.1 行コンポーネント仕様

- 左: 完了トグルアイコン
- 中: タイトル（1行） + 補助情報（通知時刻など）
- 右: 状態アイコン群（通知/リンク）
- 長押し: 並び替えモード
- 右スワイプ: 完了切替
- 左スワイプ: クイック編集

---

## 2.2 Habit 作成・編集

目的: 1画面で最小入力完了。

```
┌─────────────────────────────────────────────┐
│ [←] Habit編集                      [保存]    │
│                                             │
│  タイトル                                      │
│  [ 深呼吸 5分                         ]      │
│                                             │
│  アイコン                                      │
│  [ icon picker (Material Symbols) ]         │
│                                             │
│  頻度                                         │
│  (●) 毎日  ( ) 平日  ( ) カスタム             │
│                                             │
│  リマインダー                                  │
│  [ON]  時刻 [20:00]                           │
│                                             │
│  リンク                                        │
│  + Webリンク追加                               │
│  + アプリリンク追加                            │
│                                             │
│  [削除]                                       │
└─────────────────────────────────────────────┘
```

---

## 2.3 Link 設定モーダル

### A. Webリンク

```
タイトル（任意）
URL（https://...）
起動方法: 外部ブラウザ / Custom Tabs
```

### B. アプリリンク

```
タイトル（任意）
Intent URI または Deep Link
package 名（任意）
存在チェック [実行]
```

---

## 2.4 History

目的: 継続状況の確認。

```
┌─────────────────────────────────────────────┐
│  History                                     │
│                                             │
│  連続達成 12日                               │
│                                             │
│  月間ヒートマップ（モノクロ濃淡 + Accent）   │
│                                             │
│  ハビット別達成率                             │
└─────────────────────────────────────────────┘
```

---

## 2.5 Settings

- 通知権限状態
- テーマ（Light / Dark）
- アクセントカラー選択（1〜2色）
- 並び順初期値（未完了優先は固定ON、同グループ内の規則を選択）

---

## 3. 主要インタラクション仕様

### 3.1 未完了優先ソート

1. 当日対象ハビットを抽出
2. その日の完了ログ有無で `isCompletedToday` 判定
3. `isCompletedToday = false` を上段表示
4. `isCompletedToday = true` を下段表示
5. 各段内は `sortOrder` 昇順

### 3.2 リマインダー

- 通知ONかつ時刻設定済みのハビットのみスケジュール登録。
- Android 13+ は通知権限要求。
- 通知タップで対象 Habit 詳細へ遷移。

### 3.3 リンク起動

- Webリンク: `https` のみ許容（初期仕様）。
- アプリリンク: `intent://` / カスタムスキーム / package 指定に対応。
- 起動不可時はエラー表示し、編集導線を提示。

---

## 4. DB スキーマ（Room想定）

## 4.1 テーブル一覧

1. `habits`（ハビット本体）
2. `habit_schedules`（頻度・リマインダー設定）
3. `habit_links`（Web/アプリリンク）
4. `completion_logs`（日次完了ログ）

---

## 4.2 DDL案

```sql
CREATE TABLE habits (
  id TEXT PRIMARY KEY NOT NULL,
  title TEXT NOT NULL,
  icon_name TEXT NOT NULL,
  color_token TEXT,
  sort_order INTEGER NOT NULL DEFAULT 0,
  is_archived INTEGER NOT NULL DEFAULT 0,
  created_at_epoch_ms INTEGER NOT NULL,
  updated_at_epoch_ms INTEGER NOT NULL
);

CREATE TABLE habit_schedules (
  id TEXT PRIMARY KEY NOT NULL,
  habit_id TEXT NOT NULL,
  repeat_type TEXT NOT NULL,           -- DAILY / WEEKDAYS / CUSTOM
  repeat_days_mask INTEGER,            -- CUSTOM時のみ利用(例: bitmask)
  reminder_enabled INTEGER NOT NULL DEFAULT 0,
  reminder_time_local TEXT,            -- HH:mm
  timezone_id TEXT NOT NULL,
  start_date TEXT NOT NULL,            -- YYYY-MM-DD
  end_date TEXT,
  FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE
);

CREATE TABLE habit_links (
  id TEXT PRIMARY KEY NOT NULL,
  habit_id TEXT NOT NULL,
  link_type TEXT NOT NULL,             -- WEB / APP_INTENT
  title TEXT,
  url_or_intent TEXT NOT NULL,
  package_name TEXT,
  open_mode TEXT,                      -- EXTERNAL_BROWSER / CUSTOM_TABS / INTENT
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at_epoch_ms INTEGER NOT NULL,
  FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE
);

CREATE TABLE completion_logs (
  id TEXT PRIMARY KEY NOT NULL,
  habit_id TEXT NOT NULL,
  local_date TEXT NOT NULL,            -- YYYY-MM-DD
  completed_at_epoch_ms INTEGER NOT NULL,
  source TEXT NOT NULL,                -- MANUAL / NOTIFICATION_ACTION
  FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE,
  UNIQUE(habit_id, local_date)
);

CREATE INDEX idx_habits_sort_order ON habits(sort_order);
CREATE INDEX idx_schedules_habit_id ON habit_schedules(habit_id);
CREATE INDEX idx_links_habit_id ON habit_links(habit_id);
CREATE INDEX idx_completion_habit_date ON completion_logs(habit_id, local_date);
```

---

## 4.3 Entity案（Kotlin概念）

### Habit

- `id: String`
- `title: String`
- `iconName: String`（Material Symbols名）
- `colorToken: String?`
- `sortOrder: Int`
- `isArchived: Boolean`
- `createdAt: Long`
- `updatedAt: Long`

### HabitSchedule

- `id: String`
- `habitId: String`
- `repeatType: RepeatType`
- `repeatDaysMask: Int?`
- `reminderEnabled: Boolean`
- `reminderTimeLocal: String?`
- `timezoneId: String`
- `startDate: LocalDate`
- `endDate: LocalDate?`

### HabitLink

- `id: String`
- `habitId: String`
- `linkType: LinkType`
- `title: String?`
- `urlOrIntent: String`
- `packageName: String?`
- `openMode: OpenMode?`
- `sortOrder: Int`
- `createdAt: Long`

### CompletionLog

- `id: String`
- `habitId: String`
- `localDate: LocalDate`
- `completedAt: Long`
- `source: CompletionSource`

---

## 4.4 クエリ設計（未完了上位）

```sql
SELECT
  h.id,
  h.title,
  h.icon_name,
  h.sort_order,
  CASE WHEN c.id IS NULL THEN 0 ELSE 1 END AS completed_flag
FROM habits h
LEFT JOIN completion_logs c
  ON c.habit_id = h.id
  AND c.local_date = :today
WHERE h.is_archived = 0
ORDER BY completed_flag ASC, h.sort_order ASC;
```

- `completed_flag = 0` が未完了。
- これにより未完了が上に固定される。

---

## 5. 実装時の非機能要件メモ

- ローカル優先（オフライン動作可能）。
- 通知再登録（端末再起動/アプリ更新）に対応。
- 大きなモーションを避け、短いトランジションで集中を阻害しない。
- アクセシビリティ: コントラスト比・タップ領域・スクリーンリーダーラベルを確保。
