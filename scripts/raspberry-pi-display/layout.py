"""e-Paper に表示する内容とレイアウトの決定.

Pillow や qrcode に依存しないため、描画環境がなくても検証できる。
"""

from __future__ import annotations

from dataclasses import dataclass, replace
from datetime import datetime
from typing import List, Optional, Sequence, Tuple

MARGIN = 3
GAP = 6
IP_LINE_HEIGHT = 18
PORT_LINE_HEIGHT = 14
ROW_HEIGHT = 15
NO_NETWORK_TEXT = "NO NETWORK"
UPDATED_LABEL = "UPDATED"
UPDATED_FORMAT = "%m/%d %H:%M"
UNKNOWN_TEXT = "-"


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


def format_version(version: Optional[str], commit: Optional[str] = None) -> str:
    """version だけでは同じ値のまま更新されることがあるため commit を添える."""
    if not version:
        return UNKNOWN_TEXT
    if not commit:
        return version
    return "{}+{}".format(version, commit)


def format_updated_at(moment: datetime) -> str:
    return moment.strftime(UPDATED_FORMAT)


def build_rows(
    api_ok: Optional[bool],
    version: Optional[str],
    printer_ok: Optional[bool],
    extra: Sequence[Row] = (),
    commit: Optional[str] = None,
) -> Tuple[Row, ...]:
    rows = [
        Row("API", mark=api_ok),
        Row("VER", text=format_version(version, commit)),
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
    commit: Optional[str] = None,
) -> DisplayState:
    return DisplayState(
        ip=ip,
        port=config.web_port,
        url=config.web_url(ip) if ip else None,
        rows=build_rows(api_ok, version, printer_ok, extra, commit),
    )


def with_updated_at(state: DisplayState, moment: datetime) -> DisplayState:
    """描画直前に最終更新行を足す.

    再描画するかどうかは更新行を持たない state 同士で判定する。時刻を state 側に
    持たせるとポーリングのたびに別物になり、20 秒ごとに e-Paper を焼くことになる。
    """
    row = Row(UPDATED_LABEL, text=format_updated_at(moment))
    return replace(state, rows=state.rows + (row,))


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
