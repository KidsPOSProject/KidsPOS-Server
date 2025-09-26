# Visual Regression Testing (VRT) Setup Guide

## 概要

KidsPOS ServerではPlaywrightを使用したVisual Regression Testing (VRT)を実装しています。
PRごとにUIの見た目の変更を自動的に検出し、意図しない変更を防ぎます。
すべてGitHubで完結し、外部サービスは不要です。

## 技術スタック

- **Playwright**: ブラウザ自動化とスクリーンショット比較
- **GitHub Actions**: CI/CDパイプラインでの自動実行
- **GitHub Artifacts**: テスト結果とスクリーンショットの保存

## ローカルセットアップ

### 1. 依存関係のインストール

```bash
# Node.js依存関係のインストール
npm install

# Playwrightブラウザのインストール
npx playwright install --with-deps chromium
```

### 2. アプリケーションの起動

```bash
# Spring Bootアプリケーションを起動
./gradlew bootRun
```

### 3. VRTの実行

```bash
# 基本的なVRT実行（スクリーンショット比較）
npm run test:vrt

# スナップショットの更新
npm run test:vrt:update

# レポートの表示
npm run test:vrt:report
```

## GitHub Actions設定

### ワークフローのトリガー

VRTは以下のタイミングで自動実行されます：

1. **PR作成時**: 全ページのスクリーンショットを取得
2. **PR更新時**: 変更を再チェック
3. **手動更新**: `update-snapshots`ラベルをPRに追加

### テスト結果の確認

1. **PRのChecksタブ**: テストの成功/失敗を確認
2. **Artifacts**: 以下のファイルがダウンロード可能
   - `playwright-report`: HTMLレポート
   - `test-results`: テスト実行結果
   - `failed-snapshots`: 失敗時の差分画像

## テスト対象ページ

現在、以下のページがVRTの対象です：

- ホームページ (`/`)
- 商品管理 (`/items`)
- 店舗管理 (`/stores`)
- スタッフ管理 (`/staffs`)
- 売上管理 (`/sales`)
- 売上レポート (`/reports/sales`)

各ページは以下のビューポートでテストされます：

- **Desktop**: 1280x720
- **Tablet**: iPad (768x1024)
- **Mobile**: iPhone 13 (375x667)

## スナップショットの更新

### 自動更新（推奨）

1. PRに`update-snapshots`ラベルを追加
2. GitHub Actionsが自動的にスナップショットを更新
3. 更新されたスナップショットが自動コミット
4. ラベルは自動的に削除される

### 手動更新

```bash
# ローカルでスナップショットを更新
npm run test:vrt:update

# 変更をコミット
git add tests/vrt/**/*.png
git commit -m "chore: update visual regression snapshots"
git push
```

## ディレクトリ構造

```
tests/
└── vrt/
    ├── pages.spec.ts           # テストスクリプト
    └── pages.spec.ts-snapshots/  # スナップショット画像
        ├── homepage-chromium-linux.png
        ├── items-page-chromium-linux.png
        └── ...
```

## トラブルシューティング

### よくある問題と解決方法

#### 1. スナップショットの不一致

```bash
# 差分を確認
npm run test:vrt:report

# 期待される変更の場合はスナップショットを更新
npm run test:vrt:update
```

#### 2. タイムアウトエラー

`playwright.config.ts`でタイムアウトを調整：

```javascript
use: {
  navigationTimeout: 60000,
  actionTimeout: 30000,
}
```

#### 3. フォントの違い

異なるOS間でのフォントの違いは、CSSで統一フォントを指定して対処：

```css
* {
  font-family: 'Arial', sans-serif !important;
}
```

## ベストプラクティス

### 1. 動的コンテンツの扱い

- タイムスタンプなど変化する要素は`data-test-id`属性で識別
- テスト時に非表示にするか、固定値に置換

### 2. アニメーションの無効化

テスト実行時はアニメーションを無効にする：

```javascript
animations: 'disabled'
```

### 3. 待機処理

ページの完全読み込みを待つ：

```javascript
await page.waitForLoadState('networkidle');
```

### 4. レビュープロセス

- 意図的なUI変更は必ずPRで説明
- スナップショット更新は別コミットに
- 差分画像を必ず確認してから承認

## 開発フロー

1. **機能開発**: UI変更を実装
2. **ローカル確認**: `npm run test:vrt`で変更を確認
3. **PR作成**: 変更内容を説明
4. **自動テスト**: GitHub ActionsでVRT実行
5. **差分確認**: Artifactsから差分を確認
6. **更新**: 必要に応じて`update-snapshots`ラベルを追加
7. **マージ**: レビュー後にマージ

## コマンドリファレンス

```bash
# VRT実行
npm run test:vrt

# スナップショット更新
npm run test:vrt:update

# HTMLレポート表示
npm run test:vrt:report

# Playwrightインストール
npm run playwright:install
```

## 参考リンク

- [Playwright Documentation](https://playwright.dev/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
