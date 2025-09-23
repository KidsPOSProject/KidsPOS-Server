# プライバシーとパス記述ルール【最重要】

## 絶対に守るべきルール

### ❌ 禁止事項

- **絶対にローカルパスを含めない**
    - `/Users/username/`
    - `/home/username/`
    - 個人を特定できる具体的なユーザーパス

### ✅ 正しい記述方法

- **環境変数や抽象パスを使用**
    - `$HOME`
    - `~`
    - `$USER_HOME$`
    - `{Java インストールディレクトリ}`
    - 相対パス

### 具体例

❌ 悪い例:

```
/Users/{username}/.local/share/mise/installs/java/
/Users/{username}/projects/KidsPOS-Server/
```

✅ 良い例:

```
$HOME/.local/share/mise/installs/java/
~/projects/KidsPOS-Server/
./relative/path/
```

## 理由

- **個人情報の保護**: ユーザー名などの個人情報を露出させない
- **セキュリティ**: システムパス情報の漏洩を防ぐ
- **ポータビリティ**: 他の環境でも動作する汎用的な記述

## 適用範囲

- コード内のパス
- ドキュメント
- コメント
- ログ出力
- エラーメッセージ
- メモリファイル
- **すべての出力において厳守**
