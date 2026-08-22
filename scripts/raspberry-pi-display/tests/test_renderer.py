import dataclasses
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import config as config_module  # noqa: E402
import layout  # noqa: E402

try:
    import qrcode
    from PIL import Image

    import renderer

    DEPENDENCIES_AVAILABLE = True
except ImportError:
    DEPENDENCIES_AVAILABLE = False


requires_pillow = unittest.skipUnless(
    DEPENDENCIES_AVAILABLE, "Pillow と qrcode が導入されていないためスキップします"
)

URL = "http://192.168.11.20:8080/"


def black_pixels(image, box=None):
    region = image.crop(box) if box else image
    return sum(1 for value in region.getdata() if value == 0)


@requires_pillow
class BuildQrTest(unittest.TestCase):
    def setUp(self):
        self.config = config_module.Config()

    def test_the_qr_fits_in_the_panel_height(self):
        image = renderer.build_qr(URL, self.config, 116)

        self.assertLessEqual(image.size[0], 116)
        self.assertEqual(image.size[0], image.size[1])

    def test_the_size_is_an_exact_multiple_of_the_module_count(self):
        image = renderer.build_qr(URL, self.config, 116)

        modules = self._module_count(URL)
        box_size = image.size[0] // modules

        self.assertEqual(modules * box_size, image.size[0])
        self.assertGreaterEqual(box_size, self.config.qr_min_box_size)

    def test_every_module_is_a_solid_block(self):
        """補間 resize が挟まると境界が滲むため、ブロックが単色であることで確かめる."""
        image = renderer.build_qr(URL, self.config, 116)
        modules = self._module_count(URL)
        box_size = image.size[0] // modules

        for row in range(modules):
            for column in range(modules):
                box = (column * box_size, row * box_size, (column + 1) * box_size, (row + 1) * box_size)
                values = set(image.crop(box).getdata())
                self.assertEqual(1, len(values), "module ({}, {}) が単色ではありません".format(column, row))

    def test_the_image_is_black_and_white(self):
        image = renderer.build_qr(URL, self.config, 116)

        self.assertEqual("1", image.mode)
        self.assertLessEqual(set(image.getdata()), {0, 255})

    def test_the_quiet_zone_is_kept(self):
        image = renderer.build_qr(URL, self.config, 116)
        modules = self._module_count(URL)
        box_size = image.size[0] // modules
        quiet = self.config.qr_border * box_size

        self.assertEqual(0, black_pixels(image, (0, 0, image.size[0], quiet)))
        self.assertEqual(0, black_pixels(image, (0, 0, quiet, image.size[1])))

    def test_the_encoded_data_matches_the_url(self):
        image = renderer.build_qr(URL, self.config, 116)
        modules = self._module_count(URL)
        box_size = image.size[0] // modules
        matrix = self._matrix(URL)

        for row in range(modules):
            for column in range(modules):
                expected = 0 if matrix[row][column] else 255
                actual = image.getpixel((column * box_size, row * box_size))
                self.assertEqual(expected, actual, "module ({}, {}) が一致しません".format(column, row))

    def test_the_box_size_is_stepped_down_when_the_preferred_size_overflows(self):
        image = renderer.build_qr(URL, self.config, 90)

        modules = self._module_count(URL)

        self.assertLessEqual(image.size[0], 90)
        self.assertLess(image.size[0] // modules, self.config.qr_box_size)

    def test_nothing_is_drawn_when_even_the_minimum_overflows(self):
        with self.assertLogs(renderer.logger, level="WARNING"):
            self.assertIsNone(renderer.build_qr(URL, self.config, 20))

    def _module_count(self, url):
        code = qrcode.QRCode(
            error_correction=qrcode.constants.ERROR_CORRECT_L,
            box_size=1,
            border=self.config.qr_border,
        )
        code.add_data(url)
        code.make(fit=True)
        return code.modules_count + self.config.qr_border * 2

    def _matrix(self, url):
        code = qrcode.QRCode(
            error_correction=qrcode.constants.ERROR_CORRECT_L,
            box_size=1,
            border=self.config.qr_border,
        )
        code.add_data(url)
        code.make(fit=True)
        return code.get_matrix()


@requires_pillow
class DrawMarkTest(unittest.TestCase):
    def _draw(self, value):
        from PIL import ImageDraw

        image = Image.new("1", (20, 20), renderer.WHITE)
        renderer.draw_mark(ImageDraw.Draw(image), 2, 2, renderer.MARK_SIZE, value)
        return image

    def test_each_mark_puts_ink_on_the_panel(self):
        for value in (True, False, None):
            with self.subTest(value=value):
                self.assertGreater(black_pixels(self._draw(value)), 0)

    def test_the_three_marks_are_distinguishable(self):
        shapes = {value: tuple(self._draw(value).getdata()) for value in (True, False, None)}

        self.assertEqual(3, len(set(shapes.values())))

    def test_the_unknown_mark_is_the_lightest(self):
        counts = {value: black_pixels(self._draw(value)) for value in (True, False, None)}

        self.assertEqual(None, min(counts, key=counts.get))


@requires_pillow
class RenderTest(unittest.TestCase):
    def setUp(self):
        self.config = config_module.Config()

    def _state(self, ip="192.168.11.20", api_ok=True, version="1.0.0", printer_ok=True, extra=()):
        return layout.build_state(self.config, ip, api_ok, version, printer_ok, extra)

    def test_the_image_matches_the_panel(self):
        image = renderer.render(self._state(), self.config)

        self.assertEqual((250, 122), image.size)
        self.assertEqual("1", image.mode)

    def test_the_qr_is_drawn_on_the_left(self):
        image = renderer.render(self._state(), self.config)

        left = black_pixels(image, (0, 0, 120, 122))

        self.assertGreater(left, 200)

    def test_the_text_is_drawn_on_the_right(self):
        image = renderer.render(self._state(), self.config)

        right = black_pixels(image, (125, 0, 250, 122))

        self.assertGreater(right, 100)

    def test_the_qr_stays_inside_the_left_column(self):
        with_qr = renderer.render(self._state(), self.config)
        without_qr = renderer.render(self._state(ip=None), self.config)
        boundary = layout.text_area_left(self.config.height - layout.MARGIN * 2) - layout.GAP

        self.assertEqual(0, black_pixels(without_qr, (0, 0, boundary, 122)))
        self.assertGreater(black_pixels(with_qr, (0, 0, boundary, 122)), 0)

    def test_no_network_is_shown_without_an_ip(self):
        with_ip = renderer.render(self._state(), self.config)
        without_ip = renderer.render(self._state(ip=None), self.config)

        self.assertNotEqual(tuple(with_ip.getdata()), tuple(without_ip.getdata()))
        self.assertGreater(black_pixels(without_ip, (125, 0, 250, 40)), 0)

    def test_the_text_column_does_not_move_when_the_qr_is_missing(self):
        with_ip = renderer.render(self._state(), self.config)
        without_ip = renderer.render(self._state(ip=None), self.config)

        self.assertEqual(self._first_inked_column(with_ip, 60), self._first_inked_column(without_ip, 60))

    def test_the_marks_change_with_the_status(self):
        healthy = renderer.render(self._state(), self.config)
        broken = renderer.render(self._state(printer_ok=False), self.config)
        unknown = renderer.render(self._state(printer_ok=None), self.config)

        self.assertNotEqual(tuple(healthy.getdata()), tuple(broken.getdata()))
        self.assertNotEqual(tuple(broken.getdata()), tuple(unknown.getdata()))

    def test_the_version_is_drawn(self):
        short = renderer.render(self._state(version="1.0.0"), self.config)
        long_version = renderer.render(self._state(version="1.0.0-SNAPSHOT"), self.config)

        self.assertNotEqual(tuple(short.getdata()), tuple(long_version.getdata()))

    def test_a_long_version_stays_inside_the_panel(self):
        image = renderer.render(self._state(version="1.2.3-SNAPSHOT-20260101"), self.config)

        self.assertEqual(0, black_pixels(image, (250 - layout.MARGIN, 0, 250, 122)))

    def test_a_long_ip_stays_inside_the_panel(self):
        image = renderer.render(self._state(ip="192.168.100.200"), self.config)

        self.assertEqual(0, black_pixels(image, (250 - layout.MARGIN, 0, 250, 122)))

    def test_extra_rows_are_drawn(self):
        extra = (layout.Row("DEVICE-A", mark=True),)

        base = renderer.render(self._state(), self.config)
        extended = renderer.render(self._state(extra=extra), self.config)

        self.assertGreater(black_pixels(extended, (125, 70, 250, 122)), black_pixels(base, (125, 70, 250, 122)))

    def test_rows_that_do_not_fit_are_dropped_with_a_warning(self):
        extra = tuple(layout.Row("DEVICE-{}".format(index), mark=True) for index in range(6))

        with self.assertLogs(renderer.logger, level="WARNING"):
            image = renderer.render(self._state(extra=extra), self.config)

        self.assertEqual((250, 122), image.size)

    def test_a_missing_font_falls_back_instead_of_failing(self):
        config = dataclasses.replace(
            self.config,
            font_path="/nonexistent/Font.ttf",
            font_bold_path="/nonexistent/Font-Bold.ttf",
        )

        with self.assertLogs(renderer.logger, level="WARNING"):
            image = renderer.render(self._state(), config)

        self.assertGreater(black_pixels(image, (125, 0, 250, 122)), 0)

    def _first_inked_column(self, image, top):
        for x in range(120, 250):
            if black_pixels(image, (x, top, x + 1, 122)) > 0:
                return x
        return None


if __name__ == "__main__":
    unittest.main()
