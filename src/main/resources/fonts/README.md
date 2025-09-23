# フォントファイルの配置について

## 日本語フォントの設定方法

バーコードPDFで日本語を表示するには、このディレクトリに日本語フォントファイルを配置してください。

### 推奨フォント

1. **IPAフォント** (無料)
   - ダウンロード: https://moji.or.jp/ipafont/
   - ファイル名: `ipag.ttf` または `ipagp.ttf`
   - ライセンス: IPAフォントライセンス

2. **Noto Sans CJK JP** (無料)
   - ダウンロード: https://github.com/googlefonts/noto-cjk
   - ファイル名: `NotoSansCJKjp-Regular.otf`
   - ライセンス: Open Font License

### 設定方法

1. 上記のいずれかのフォントをダウンロード
2. フォントファイル（.ttf または .otf）を `src/main/resources/fonts/` ディレクトリに配置
3. ファイル名を `japanese.ttf` にリネーム（任意）
4. アプリケーションをビルドし直す

### 注意事項

- フォントファイルはJARファイルに含まれます
- フォントのライセンスに従ってください
- フォントファイルのサイズが大きい場合、JARファイルも大きくなります