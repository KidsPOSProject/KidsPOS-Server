"""e-Paper に表示する内容とレイアウトの決定.

Pillow や qrcode に依存しないため、描画環境がなくても検証できる。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional, Sequence, Tuple

MARGIN = 3
GAP = 6
IP_LINE_HEIGHT = 18
PORT_LINE_HEIGHT = 14
ROW_HEIGHT = 15
NO_NETWORK_TEXT = "NO NETWORK"


@dataclass(frozen=True)
class Row:
    label: str
    mark: Optional[bool] = None
    text: Optional[str] = None

    @property
    def is_mark(self) -> bool:
        return self.text is None


@dataclass(frozen=True)
class DisplayState:
    ip: Optional[str]
    port: int
    url: Optional[str]
    rows: Tuple[Row, ...]


def build_rows(
    api_ok: Optional[bool],
    version: Optional[str],
    printer_ok: Optional[bool],
    extra: Sequence[Row] = (),
) -> Tuple[Row, ...]:
    rows = [
        Row("API", mark=api_ok),
        Row("VER", text=version or "-"),
        Row("PRINTER", mark=printer_ok),
    ]
    rows.extend(extra)
    return tuple(rows)


def build_state(
    config,
    ip: Optional[str],
    api_ok: Optional[bool],
    version: Optional[str],
    printer_ok: Optional[bool],
    extra: Sequence[Row] = (),
) -> DisplayState:
    return DisplayState(
        ip=ip,
        port=config.web_port,
        url=config.web_url(ip) if ip else None,
        rows=build_rows(api_ok, version, printer_ok, extra),
    )


def choose_qr_box_size(
    module_count: int,
    border: int,
    preferred: int,
    minimum: int,
    max_pixels: int,
) -> Optional[int]:
    """補間 resize を避けるため、収まる範囲で最大の整数倍率を選ぶ."""
    total_modules = module_count + border * 2
    if total_modules <= 0:
        return None
    for box_size in range(preferred, minimum - 1, -1):
        if total_modules * box_size <= max_pixels:
            return box_size
    return None


def qr_position(qr_pixels: int, area_height: int, margin: int = MARGIN) -> Tuple[int, int]:
    y = (area_height - qr_pixels) // 2
    return margin, max(0, y)


def text_area_left(qr_pixels: int, margin: int = MARGIN, gap: int = GAP) -> int:
    return margin + qr_pixels + gap


def row_positions(top: int, count: int, row_height: int = ROW_HEIGHT) -> List[int]:
    return [top + index * row_height for index in range(count)]


def fits_vertically(row_count: int, height: int) -> bool:
    used = MARGIN + IP_LINE_HEIGHT + PORT_LINE_HEIGHT + row_count * ROW_HEIGHT
    return used <= height
