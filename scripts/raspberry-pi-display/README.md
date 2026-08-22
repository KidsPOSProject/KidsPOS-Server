# e-Paper 表示サービス

Raspberry Pi に接続した e-Paper に、KidsPOS Server へのアクセス情報と稼働状態を表示する常駐サービスです。
サーバー本体（scripts/raspberry-pi）とは別プロセスで動くため、サーバーが停止していても表示は落ちず、API × のまま待ち続けます。

## 前提

- パネル: Waveshare 2.13inch e-Paper HAT+（白黒、250x122、SPI 接続、ドライバ epd2in13_V4）
- 配置先: /opt/kidspos-display
- サービス名: kidspos-display（systemd）
- SPI が有効になっていること（sudo raspi-config → Interface Options → SPI）
- Python 3、Pillow、qrcode、Waveshare の waveshare_epd ライブラリ

サーバーの状態は同じ Pi 上の http://127.0.0.1:8080/api/status から取得します。
LAN を経由しないため、ネットワークが切れていてもサーバー自体の生死は判定できます。

## 表示内容

左に Web UI への QR コード、右に文字情報を並べます。

- IPv4 アドレス（ルーティングで実際に使われるアドレス。127.x.x.x は表示しません）
- 待ち受けポート
- API: サーバーが応答すれば ○、応答しなければ ×
- VER: サーバーが返すバージョン。取得できていなければ -
- PRINTER: レシートプリンターへの TCP 接続可否。○ / ×、未設定なら -

QR コードには http://<Pi の IPv4>:8080/ を格納します。
IPv4 を取得できない場合は QR を描かず NO NETWORK と表示します。

文字は入り切る範囲で最大の大きさを選びます。最小の大きさでも入らない場合は末尾を省略記号に置き換えるため、隣の項目やパネルの外へはみ出しません。

PRINTER の判定はサーバー側の /api/status がそのまま返す値です。
ICMP ではなく実際に印刷で使う TCP ポート（既定 9100）への接続確認で、表示側にプリンターの設定を持ちません。

## ファイル

| ファイル | 用途 |
|---|---|
| app.py | 常駐プロセス本体（監視ループ、変化検知、シグナル処理） |
| config.py | 環境変数からの設定読み込み |
| health.py | IPv4 の取得、/api/status の取得と解釈、チャタリング抑制 |
| layout.py | 表示項目とレイアウトの決定（描画ライブラリに依存しない） |
| renderer.py | Pillow と qrcode による 250x122 の 1bit 画像生成 |
| epaper.py | パネルへの出力（動作確認用に PNG へ書き出す実装も持つ） |
| kidspos-display.service | systemd ユニットのテンプレート |
| install-display.sh | 導入（依存確認、配置、ユニット配置、自動起動、起動） |
| test-install-display.sh | install-display.sh の自動テスト |
| tests/ | Python 側の自動テスト |

## 初回セットアップ

依存を先に導入します。install-display.sh は依存を自動で入れません（導入元が混在すると復旧が難しくなるため）。

```bash
sudo apt install -y python3-pil python3-qrcode
```

Waveshare のドライバを導入します。

```bash
git clone https://github.com/waveshareteam/e-Paper.git
sudo cp -r e-Paper/RaspberryPi_JetsonNano/python/lib/waveshare_epd \
  "$(python3 -c 'import site; print(site.getsitepackages()[0])')"
```

このディレクトリを Raspberry Pi にコピーして実行します。

```bash
sudo ./install-display.sh              # 導入して起動する
sudo ./install-display.sh --no-start   # 導入だけ行い起動しない
sudo ./install-display.sh --skip-deps  # 依存の確認を省略する
```

再実行しても設定は壊れません。systemd ユニットは内容が変わったときだけ書き換え、サービスが稼働中なら配置し直したコードを読ませるために再起動します。
実行ユーザーが spi / gpio グループに入っていなければ追加します。グループ追加を反映するには一度ログインし直してください。

## 起動と自動復旧

install-display.sh を実行すると、配置・自動起動の有効化・起動までを一度に行います。
起動後の操作は systemd から行います。

```bash
sudo systemctl start kidspos-display
sudo systemctl stop kidspos-display
sudo systemctl restart kidspos-display
```

自動起動は install-display.sh が systemctl enable を実行して有効にします。手動で切り替える場合:

```bash
sudo systemctl enable kidspos-display
sudo systemctl disable kidspos-display
systemctl is-enabled kidspos-display
```

ユニットは WantedBy=multi-user.target で通常の起動シーケンスに乗り、After / Wants=network-online.target でネットワークが上がってから起動します。
ネットワークが未確立のまま起動しても失敗しません。IPv4 を取得できるまで NO NETWORK を表示し、取得できた時点で QR 付きの表示に切り替わります。

異常終了したときは Restart=always と RestartSec=10 により 10 秒後に再起動します。
ただし通常の障害でプロセスは終了しません。

- サーバーが停止していても終了しません。状態取得の失敗は API × として扱い、監視を続けます
- パネルへの書き込みに失敗しても終了しません。例外をログに残して次の周期へ進みます
- プリンターが未設定・到達不能でも PRINTER の表示が - や × になるだけです
- SIGTERM / SIGINT を受け取ると監視を止め、パネルを閉じてから終了します。systemctl stop と restart はこの経路を通ります

Restart=always は Python 自体の異常終了に対する保険です。

サーバー本体のユニット（scripts/raspberry-pi）も同じ設定で、両者は独立して動きます。
表示側はサーバーの状態を一定間隔で取得するだけなので、起動順の依存関係はありません。

## 動作確認

パネルに出さずに画像を確認できます。実機以外でも実行できるため、レイアウトの確認はこの方法が確実です。

```bash
python3 app.py --once --output /tmp/preview.png --verbose
```

サービスの状態とログ:

```bash
systemctl status kidspos-display
journalctl -u kidspos-display -f
```

## 更新の頻度

既定では 20 秒ごとに状態を取得しますが、書き換えるのは表示内容が前回と変わったときだけです。
e-Paper の全面書き換えは遅く残像も残るため、変化がなければ画面には触れません。

○ と × は 1 回の結果では切り替えません。3 回続けて失敗したら ×、2 回続けて成功したら ○ に戻します。
一時的な取りこぼしで表示が点滅するのを防ぐためです。

変化がない状態が続いても、残像を消すため 24 時間に 1 度だけ書き直します。

## 設定の上書き

環境変数で上書きできます。systemd ユニットに Environment= を追記するか、drop-in（/etc/systemd/system/kidspos-display.service.d/）で指定してください。

| 環境変数 | 既定値 | 対象 |
|---|---|---|
| KIDSPOS_STATUS_URL | http://127.0.0.1:8080/api/status | 状態の取得先 |
| KIDSPOS_WEB_SCHEME | http | QR に入れる URL のスキーム |
| KIDSPOS_WEB_PORT | 8080 | QR に入れる URL のポートと表示するポート |
| KIDSPOS_POLL_INTERVAL | 20 | 状態を取得する間隔（秒） |
| KIDSPOS_HTTP_TIMEOUT | 3 | 状態取得のタイムアウト（秒） |
| KIDSPOS_FAIL_THRESHOLD | 3 | × に切り替えるまでの連続失敗回数 |
| KIDSPOS_OK_THRESHOLD | 2 | ○ に戻すまでの連続成功回数 |
| KIDSPOS_REFRESH_INTERVAL | 86400 | 変化が無くても書き直す間隔（秒） |
| KIDSPOS_QR_BOX_SIZE | 4 | QR 1 モジュールあたりの画素数 |
| KIDSPOS_QR_MIN_BOX_SIZE | 2 | 収まらないときに下げてよい下限 |
| KIDSPOS_QR_BORDER | 2 | QR の余白（モジュール数） |
| KIDSPOS_PROBE_HOST | 1.1.1.1 | IPv4 を得るための宛先（パケットは送りません） |
| KIDSPOS_PROBE_PORT | 80 | 同上 |
| KIDSPOS_FONT_PATH | /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf | 通常フォント |
| KIDSPOS_FONT_BOLD_PATH | /usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf | 太字フォント |

install-display.sh 自体の配置先も上書きできます。

| 環境変数 | 既定値 |
|---|---|
| KIDSPOS_DISPLAY_APP_DIR | /opt/kidspos-display |
| KIDSPOS_DISPLAY_SERVICE | kidspos-display |
| KIDSPOS_DISPLAY_SERVICE_USER | pi |
| KIDSPOS_DISPLAY_UNIT_DIR | /etc/systemd/system |
| KIDSPOS_DISPLAY_PYTHON | /usr/bin/python3 |
| KIDSPOS_DISPLAY_SPI_DEVICE | /dev/spidev0.0 |
| KIDSPOS_DISPLAY_GROUPS | spi gpio |

## QR コードの大きさ

QR は生成後に拡大縮小しません。補間が入るとモジュールの境界が滲み、読み取りにくくなるためです。
qrcode の box_size に整数を渡してそのまま描き、パネルに収まらない場合だけ box_size を 1 ずつ下げます。

IPv4 の URL は QR のバージョン 2（25 モジュール）に収まるため、余白 2 モジュールを含めて 29 モジュール、box_size 4 で 116 画素になります。
パネルの高さ 122 画素から上下の余白を引いた 116 画素にちょうど収まる大きさです。

## 表示項目を増やす

layout.Row を config の extra_rows に渡すと、PRINTER の下に行が追加されます。
縦に入り切らない行は描かず、警告をログに残します。既定のレイアウトでは 5 行まで入ります。

## テスト

Python 側:

```bash
cd scripts/raspberry-pi-display && python3 -m unittest discover -s tests -t .
```

renderer のテストは Pillow と qrcode が無い環境では自動でスキップされます。

導入スクリプト側は systemctl・python3・usermod などをスタブに差し替えるため、Raspberry Pi 以外でもそのまま実行できます。

```bash
bash scripts/raspberry-pi-display/test-install-display.sh
```
