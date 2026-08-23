"""Waveshare 2.13inch e-Paper HAT+ (V4) への出力."""

from __future__ import annotations

import logging

logger = logging.getLogger(__name__)

WHITE = 0xFF


class EPaperUnavailable(RuntimeError):
    pass


class EPaper:
    """Waveshare のドライバに依存する部分をここだけに閉じ込める.

    ドライバは Raspberry Pi にしか入らないため、import は読み込み時ではなく
    最初の表示時に行い、開発機でも他のモジュールを読めるようにしている。
    """

    def __init__(self) -> None:
        self._epd = None

    def _create(self):
        try:
            from waveshare_epd import epd2in13_V4
        except ImportError as exc:
            raise EPaperUnavailable("waveshare_epd ドライバを読み込めません: {}".format(exc)) from exc
        return epd2in13_V4.EPD()

    def show(self, image) -> None:
        if self._epd is None:
            self._epd = self._create()
        self._epd.init()
        self._epd.display(self._epd.getbuffer(image))
        self._epd.sleep()

    def clear(self) -> None:
        if self._epd is None:
            self._epd = self._create()
        self._epd.init()
        self._epd.Clear(WHITE)
        self._epd.sleep()

    def close(self) -> None:
        if self._epd is None:
            return
        try:
            self._epd.sleep()
        except Exception:
            logger.exception("e-Paper のスリープに失敗しました")
        self._epd = None


class FileEPaper:
    """画像をファイルに保存する. 実機が無い環境で表示内容を確認するために使う."""

    def __init__(self, path: str) -> None:
        self._path = path

    def show(self, image) -> None:
        image.save(self._path)
        logger.info("表示内容を保存しました: %s", self._path)

    def clear(self) -> None:
        logger.info("ファイル出力では消去する画面がありません: %s", self._path)

    def close(self) -> None:
        return
