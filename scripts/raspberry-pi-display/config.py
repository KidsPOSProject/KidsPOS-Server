"""KidsPOS e-Paper ディスプレイの設定."""

from __future__ import annotations

import logging
import os
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)

DEFAULT_STATUS_URL = "http://127.0.0.1:8080/api/status"
DEFAULT_WEB_SCHEME = "http"
DEFAULT_WEB_PORT = 8080
DEFAULT_POLL_INTERVAL = 20
DEFAULT_HTTP_TIMEOUT = 3
DEFAULT_FAIL_THRESHOLD = 3
DEFAULT_OK_THRESHOLD = 2
DEFAULT_REFRESH_INTERVAL = 86400
DEFAULT_QR_BOX_SIZE = 4
DEFAULT_QR_BORDER = 2
DEFAULT_QR_MIN_BOX_SIZE = 2
DEFAULT_PROBE_HOST = "1.1.1.1"
DEFAULT_PROBE_PORT = 80
DEFAULT_FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
DEFAULT_FONT_BOLD_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

DISPLAY_WIDTH = 250
DISPLAY_HEIGHT = 122


def _env_int(name: str, default: int, minimum: int = 1) -> int:
    raw = os.environ.get(name)
    if raw is None or raw.strip() == "":
        return default
    try:
        value = int(raw)
    except ValueError:
        logger.warning("%s の値が整数ではありません: %r。既定値 %d を使います", name, raw, default)
        return default
    if value < minimum:
        logger.warning("%s の値が小さすぎます: %d。既定値 %d を使います", name, value, default)
        return default
    return value


def _env_str(name: str, default: str) -> str:
    raw = os.environ.get(name)
    if raw is None or raw.strip() == "":
        return default
    return raw.strip()


@dataclass(frozen=True)
class Config:
    status_url: str = DEFAULT_STATUS_URL
    web_scheme: str = DEFAULT_WEB_SCHEME
    web_port: int = DEFAULT_WEB_PORT
    poll_interval: int = DEFAULT_POLL_INTERVAL
    http_timeout: int = DEFAULT_HTTP_TIMEOUT
    fail_threshold: int = DEFAULT_FAIL_THRESHOLD
    ok_threshold: int = DEFAULT_OK_THRESHOLD
    refresh_interval: int = DEFAULT_REFRESH_INTERVAL
    qr_box_size: int = DEFAULT_QR_BOX_SIZE
    qr_border: int = DEFAULT_QR_BORDER
    qr_min_box_size: int = DEFAULT_QR_MIN_BOX_SIZE
    probe_host: str = DEFAULT_PROBE_HOST
    probe_port: int = DEFAULT_PROBE_PORT
    font_path: str = DEFAULT_FONT_PATH
    font_bold_path: str = DEFAULT_FONT_BOLD_PATH
    width: int = DISPLAY_WIDTH
    height: int = DISPLAY_HEIGHT
    extra_rows: tuple = field(default_factory=tuple)

    @classmethod
    def from_env(cls) -> "Config":
        qr_box_size = _env_int("KIDSPOS_QR_BOX_SIZE", DEFAULT_QR_BOX_SIZE)
        qr_min_box_size = _env_int("KIDSPOS_QR_MIN_BOX_SIZE", DEFAULT_QR_MIN_BOX_SIZE)
        if qr_min_box_size > qr_box_size:
            logger.warning(
                "KIDSPOS_QR_MIN_BOX_SIZE (%d) が KIDSPOS_QR_BOX_SIZE (%d) を超えています。%d に丸めます",
                qr_min_box_size,
                qr_box_size,
                qr_box_size,
            )
            qr_min_box_size = qr_box_size
        return cls(
            status_url=_env_str("KIDSPOS_STATUS_URL", DEFAULT_STATUS_URL),
            web_scheme=_env_str("KIDSPOS_WEB_SCHEME", DEFAULT_WEB_SCHEME),
            web_port=_env_int("KIDSPOS_WEB_PORT", DEFAULT_WEB_PORT),
            poll_interval=_env_int("KIDSPOS_POLL_INTERVAL", DEFAULT_POLL_INTERVAL),
            http_timeout=_env_int("KIDSPOS_HTTP_TIMEOUT", DEFAULT_HTTP_TIMEOUT),
            fail_threshold=_env_int("KIDSPOS_FAIL_THRESHOLD", DEFAULT_FAIL_THRESHOLD),
            ok_threshold=_env_int("KIDSPOS_OK_THRESHOLD", DEFAULT_OK_THRESHOLD),
            refresh_interval=_env_int("KIDSPOS_REFRESH_INTERVAL", DEFAULT_REFRESH_INTERVAL),
            qr_box_size=qr_box_size,
            qr_border=_env_int("KIDSPOS_QR_BORDER", DEFAULT_QR_BORDER, minimum=0),
            qr_min_box_size=qr_min_box_size,
            probe_host=_env_str("KIDSPOS_PROBE_HOST", DEFAULT_PROBE_HOST),
            probe_port=_env_int("KIDSPOS_PROBE_PORT", DEFAULT_PROBE_PORT),
            font_path=_env_str("KIDSPOS_FONT_PATH", DEFAULT_FONT_PATH),
            font_bold_path=_env_str("KIDSPOS_FONT_BOLD_PATH", DEFAULT_FONT_BOLD_PATH),
        )

    def web_url(self, ip: str) -> str:
        return "{}://{}:{}/".format(self.web_scheme, ip, self.web_port)
