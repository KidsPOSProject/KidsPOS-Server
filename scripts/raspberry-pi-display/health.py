"""KidsPOS サーバーとネットワークの状態取得."""

from __future__ import annotations

import json
import logging
import socket
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)

LOOPBACK_PREFIX = "127."


@dataclass(frozen=True)
class ServerStatus:
    reachable: bool
    version: Optional[str] = None
    api_version: Optional[str] = None
    printer_configured: Optional[bool] = None
    printer_reachable: Optional[bool] = None

    @property
    def printer_ok(self) -> Optional[bool]:
        if not self.reachable or self.printer_configured is None:
            return None
        if not self.printer_configured:
            return None
        return bool(self.printer_reachable)


UNREACHABLE = ServerStatus(reachable=False)


def get_local_ipv4(probe_host: str, probe_port: int) -> Optional[str]:
    """デフォルトルートで選択される送信元アドレスを返す.

    UDP ソケットの connect はパケットを送出しないため、外部への到達性がなくても
    OS のルーティング結果だけを取り出せる。
    """
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect((probe_host, probe_port))
        ip = sock.getsockname()[0]
    except OSError as exc:
        logger.debug("IPv4 アドレスを取得できませんでした: %s", exc)
        return None
    finally:
        sock.close()

    if not ip or ip.startswith(LOOPBACK_PREFIX):
        return None
    return ip


def fetch_status(url: str, timeout: int) -> ServerStatus:
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            body = response.read()
    except (urllib.error.URLError, OSError, ValueError) as exc:
        logger.debug("ステータスを取得できませんでした: %s", exc)
        return UNREACHABLE

    try:
        payload = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        logger.warning("ステータスの応答が JSON ではありません: %s", exc)
        return UNREACHABLE

    return parse_status(payload)


def parse_status(payload: Any) -> ServerStatus:
    if not isinstance(payload, dict):
        logger.warning("ステータスの応答が期待した形式ではありません: %r", type(payload))
        return UNREACHABLE

    printer = payload.get("printer")
    configured: Optional[bool] = None
    reachable: Optional[bool] = None
    if isinstance(printer, dict):
        configured = _as_bool(printer.get("configured"))
        reachable = _as_bool(printer.get("reachable"))

    return ServerStatus(
        reachable=True,
        version=_as_text(payload.get("version")),
        api_version=_as_text(payload.get("apiVersion")),
        printer_configured=configured,
        printer_reachable=reachable,
    )


def _as_bool(value: Any) -> Optional[bool]:
    if isinstance(value, bool):
        return value
    return None


def _as_text(value: Any) -> Optional[str]:
    if value is None or isinstance(value, (dict, list)):
        return None
    if isinstance(value, bool):
        return None
    text = str(value).strip()
    return text or None


class Debouncer:
    """同じ観測が続いたときだけ状態を切り替える.

    一度の通信失敗で表示が書き換わると e-Paper の寿命を無駄に削るため、
    連続回数がしきい値に達するまで前の状態を保つ。
    """

    def __init__(self, fail_threshold: int, ok_threshold: int) -> None:
        self._fail_threshold = max(1, fail_threshold)
        self._ok_threshold = max(1, ok_threshold)
        self._value: Optional[bool] = None
        self._last_observed: Optional[bool] = None
        self._streak = 0

    @property
    def value(self) -> Optional[bool]:
        return self._value

    def update(self, observed: Optional[bool]) -> Optional[bool]:
        if observed is None:
            self._value = None
            self._last_observed = None
            self._streak = 0
            return None

        if observed == self._last_observed:
            self._streak += 1
        else:
            self._last_observed = observed
            self._streak = 1

        if self._value is None:
            self._value = observed
        elif observed != self._value:
            threshold = self._ok_threshold if observed else self._fail_threshold
            if self._streak >= threshold:
                self._value = observed

        return self._value


def collect(config: Any) -> Dict[str, Any]:
    return {
        "ip": get_local_ipv4(config.probe_host, config.probe_port),
        "status": fetch_status(config.status_url, config.http_timeout),
    }
