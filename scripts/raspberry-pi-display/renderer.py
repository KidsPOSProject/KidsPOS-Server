"""e-Paper に表示する 250x122 の 1bit 画像を生成する."""

from __future__ import annotations

import logging
from typing import Optional, Sequence

import qrcode
from PIL import Image, ImageDraw, ImageFont

import layout

logger = logging.getLogger(__name__)

WHITE = 255
BLACK = 0

IP_FONT_SIZES = (18, 17, 16, 15, 14, 13, 12, 11, 10, 9)
VALUE_FONT_SIZES = (12, 11, 10, 9, 8)
PORT_FONT_SIZE = 12
ROW_FONT_SIZE = 12
MARK_SIZE = 11
LABEL_VALUE_GAP = 4
ELLIPSIS = "…"


def load_font(path: str, size: int):
    try:
        return ImageFont.truetype(path, size)
    except OSError as exc:
        logger.warning("フォントを読み込めないため既定のフォントを使います (%s): %s", path, exc)
        return ImageFont.load_default()


def text_width(draw: ImageDraw.ImageDraw, text: str, font) -> int:
    left, _, right, _ = draw.textbbox((0, 0), text, font=font)
    return right - left


def ink_right(draw: ImageDraw.ImageDraw, text: str, font) -> int:
    """左サイドベアリングを含めた右端. 右寄せの基準に使う."""
    return draw.textbbox((0, 0), text, font=font)[2]


def fit_text(
    draw: ImageDraw.ImageDraw,
    text: str,
    path: str,
    max_width: int,
    sizes: Sequence[int] = IP_FONT_SIZES,
):
    """収まる範囲で最大の文字サイズを選び、最小サイズでも収まらなければ末尾を省く."""
    font = None
    for size in sizes:
        font = load_font(path, size)
        if ink_right(draw, text, font) <= max_width:
            return text, font

    shortened = text
    while shortened and ink_right(draw, shortened + ELLIPSIS, font) > max_width:
        shortened = shortened[:-1]
    return (shortened + ELLIPSIS if shortened else ""), font


def build_qr(url: str, config, max_pixels: int) -> Optional[Image.Image]:
    """補間 resize を挟まず、整数倍のモジュールのまま QR 画像を作る."""
    qr = qrcode.QRCode(
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=config.qr_box_size,
        border=config.qr_border,
    )
    qr.add_data(url)
    qr.make(fit=True)

    box_size = layout.choose_qr_box_size(
        qr.modules_count,
        config.qr_border,
        config.qr_box_size,
        config.qr_min_box_size,
        max_pixels,
    )
    if box_size is None:
        logger.warning(
            "QR が %d px に収まらないため描画を省略します (modules=%d)", max_pixels, qr.modules_count
        )
        return None

    qr.box_size = box_size
    return qr.make_image(fill_color="black", back_color="white").get_image().convert("1")


def draw_mark(draw: ImageDraw.ImageDraw, x: int, y: int, size: int, value: Optional[bool]) -> None:
    """フォントの字形に依存すると環境で化けるため図形で描く."""
    right = x + size - 1
    bottom = y + size - 1
    if value is None:
        middle = y + size // 2
        draw.line((x, middle, right, middle), fill=BLACK, width=2)
        return
    if value:
        draw.ellipse((x, y, right, bottom), outline=BLACK, width=2)
        return
    # 線幅 2 の斜線は端点の外側にはみ出すため、1 px 内側から描く
    draw.line((x + 1, y + 1, right - 1, bottom - 1), fill=BLACK, width=2)
    draw.line((x + 1, bottom - 1, right - 1, y + 1), fill=BLACK, width=2)


def render(state: layout.DisplayState, config) -> Image.Image:
    image = Image.new("1", (config.width, config.height), WHITE)
    draw = ImageDraw.Draw(image)

    qr_area = config.height - layout.MARGIN * 2
    qr_image = build_qr(state.url, config, qr_area) if state.url else None
    qr_pixels = qr_image.size[0] if qr_image is not None else qr_area
    if qr_image is not None:
        image.paste(qr_image, layout.qr_position(qr_pixels, config.height))

    left = layout.text_area_left(qr_pixels)
    last_column = config.width - layout.MARGIN - 1
    available = last_column - left + 1

    top = layout.MARGIN
    ip_text, ip_font = fit_text(draw, state.ip or layout.NO_NETWORK_TEXT, config.font_bold_path, available)
    draw.text((left, top), ip_text, font=ip_font, fill=BLACK)

    top += layout.IP_LINE_HEIGHT
    if state.ip:
        port_font = load_font(config.font_path, PORT_FONT_SIZE)
        draw.text((left, top), ":{}".format(state.port), font=port_font, fill=BLACK)
    top += layout.PORT_LINE_HEIGHT

    row_font = load_font(config.font_path, ROW_FONT_SIZE)
    for y, row in zip(layout.row_positions(top, len(state.rows)), state.rows):
        if y + layout.ROW_HEIGHT > config.height:
            logger.warning("表示行が画面に収まらないため以降を省略します")
            break
        draw.text((left, y), row.label, font=row_font, fill=BLACK)
        if row.is_mark:
            draw_mark(draw, last_column - MARK_SIZE + 1, y + 1, MARK_SIZE, row.mark)
            continue
        label_width = text_width(draw, row.label, row_font)
        value_width = max(0, available - label_width - LABEL_VALUE_GAP)
        value_text, value_font = fit_text(draw, row.text, config.font_path, value_width, VALUE_FONT_SIZES)
        value_x = last_column - ink_right(draw, value_text, value_font) + 1
        draw.text((value_x, y), value_text, font=value_font, fill=BLACK)

    return image
