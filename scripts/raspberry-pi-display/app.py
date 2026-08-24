#!/usr/bin/env python3
"""KidsPOS の稼働状況を e-Paper に表示する常駐プロセス."""

from __future__ import annotations

import argparse
import logging
import os
import signal
import threading
import time
from datetime import datetime
from typing import Callable, Dict, Optional, Tuple

import config as config_module
import health
import layout

logger = logging.getLogger("kidspos-display")

STAGE_POLL = "状態の取得"
STAGE_DRAW = "表示の更新"


class Monitor:
    """観測値を debounce し、画面に出す状態へ組み立てる."""

    def __init__(self, config) -> None:
        self._config = config
        self._api = health.Debouncer(config.fail_threshold, config.ok_threshold)
        self._printer = health.Debouncer(config.fail_threshold, config.ok_threshold)
        self._version: Optional[str] = None
        self._commit: Optional[str] = None

    def poll(self, observation: Optional[dict] = None) -> layout.DisplayState:
        if observation is None:
            observation = health.collect(self._config)
        status = observation["status"]

        api_ok = self._api.update(status.reachable)
        printer_ok = self._printer.update(status.printer_ok)
        if status.version:
            self._version = status.version
            self._commit = status.commit

        return layout.build_state(
            self._config,
            observation["ip"],
            api_ok,
            self._version,
            printer_ok,
            self._config.extra_rows,
            self._commit,
        )


class Runner:
    """状態が変わったときだけ e-Paper を書き換える."""

    def __init__(
        self,
        config,
        device,
        render: Callable,
        clock: Callable[[], float] = time.monotonic,
        sleeper: Optional[Callable[[float], bool]] = None,
        now: Callable[[], datetime] = datetime.now,
    ) -> None:
        self._config = config
        self._device = device
        self._render = render
        self._clock = clock
        self._now = now
        self._monitor = Monitor(config)
        self._stop = threading.Event()
        self._sleeper = sleeper or self._stop.wait
        self._previous: Optional[layout.DisplayState] = None
        self._last_drawn: Optional[float] = None
        self._failures: Dict[str, Tuple[Tuple[str, str], int]] = {}

    @property
    def previous(self) -> Optional[layout.DisplayState]:
        return self._previous

    def stop(self) -> None:
        self._stop.set()

    def _note_failure(self, stage: str, exc: BaseException) -> None:
        """同じ失敗が続く間はスタックトレースを繰り返さない.

        数十秒おきに動き続けるため、抑制しないと journal が同一の
        トレースで埋まって他のログが読めなくなる。
        """
        signature = (type(exc).__name__, str(exc))
        previous = self._failures.get(stage)
        if previous is not None and previous[0] == signature:
            count = previous[1] + 1
            self._failures[stage] = (signature, count)
            logger.debug("%sに失敗しました（%d 回連続）: %s", stage, count, exc)
            return

        self._failures[stage] = (signature, 1)
        logger.exception("%sに失敗しました", stage)

    def _note_success(self, stage: str) -> None:
        previous = self._failures.pop(stage, None)
        if previous is not None:
            logger.info("%sが回復しました（%d 回連続で失敗していました）", stage, previous[1])

    def tick(self) -> Optional[layout.DisplayState]:
        try:
            state = self._monitor.poll()
        except Exception as exc:
            self._note_failure(STAGE_POLL, exc)
            return self._previous
        self._note_success(STAGE_POLL)

        now = self._clock()
        # ghosting を薄めるため、変化が無くても間隔を空けて描き直す
        stale = self._last_drawn is None or (now - self._last_drawn) >= self._config.refresh_interval
        if state == self._previous and not stale:
            return state

        try:
            self._device.show(self._render(layout.with_updated_at(state, self._now()), self._config))
        except Exception as exc:
            self._note_failure(STAGE_DRAW, exc)
            return state
        self._note_success(STAGE_DRAW)

        self._previous = state
        self._last_drawn = now
        return state

    def run(self, max_iterations: Optional[int] = None) -> None:
        iterations = 0
        while not self._stop.is_set():
            self.tick()
            iterations += 1
            if max_iterations is not None and iterations >= max_iterations:
                break
            if self._sleeper(self._config.poll_interval):
                break


def build_device(output: Optional[str]):
    if output:
        import epaper

        return epaper.FileEPaper(output)

    import epaper

    return epaper.EPaper()


def build_render() -> Callable:
    import renderer

    return renderer.render


def parse_args(argv=None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="KidsPOS の稼働状況を e-Paper に表示します")
    parser.add_argument(
        "--output",
        default=os.environ.get("KIDSPOS_DISPLAY_OUTPUT"),
        help="e-Paper ではなく指定したパスに画像を保存する（実機が無い環境での確認用）",
    )
    parser.add_argument("--once", action="store_true", help="1 回だけ描画して終了する")
    parser.add_argument("--clear", action="store_true", help="画面を白に戻して終了する（常駐を止めた後の消去用）")
    parser.add_argument("--verbose", action="store_true", help="デバッグログを出力する")
    return parser.parse_args(argv)


def main(argv=None) -> int:
    args = parse_args(argv)
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )

    config = config_module.Config.from_env()
    device = build_device(args.output)

    if args.clear:
        try:
            device.clear()
        except Exception:
            logger.exception("画面の消去に失敗しました")
            return 1
        finally:
            device.close()
        return 0

    runner = Runner(config, device, build_render())

    for name in ("SIGTERM", "SIGINT"):
        received = getattr(signal, name, None)
        if received is not None:
            signal.signal(received, lambda *_: runner.stop())

    try:
        runner.run(max_iterations=1 if args.once else None)
    finally:
        device.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
