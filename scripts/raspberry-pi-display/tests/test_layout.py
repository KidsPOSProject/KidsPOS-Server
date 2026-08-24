import os
import sys
import unittest
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import config  # noqa: E402
import layout  # noqa: E402


class RowTest(unittest.TestCase):
    def test_mark_row_has_no_text(self):
        self.assertTrue(layout.Row("API", mark=True).is_mark)

    def test_text_row_is_not_a_mark(self):
        self.assertFalse(layout.Row("VER", text="1.0.0").is_mark)


class FormatVersionTest(unittest.TestCase):
    def test_version_alone_is_returned_as_is(self):
        self.assertEqual("1.0.0", layout.format_version("1.0.0"))

    def test_commit_is_appended_to_the_version(self):
        self.assertEqual("1.0.0+a1b2c3d", layout.format_version("1.0.0", "a1b2c3d"))

    def test_missing_version_is_a_dash(self):
        self.assertEqual("-", layout.format_version(None, "a1b2c3d"))

    def test_blank_commit_is_ignored(self):
        self.assertEqual("1.0.0", layout.format_version("1.0.0", ""))


class UpdatedAtTest(unittest.TestCase):
    def setUp(self):
        self.config = config.Config()
        self.moment = datetime(2026, 8, 24, 9, 5)

    def test_the_time_is_formatted_without_the_year(self):
        self.assertEqual("08/24 09:05", layout.format_updated_at(self.moment))

    def test_the_row_is_appended_last(self):
        state = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True)

        updated = layout.with_updated_at(state, self.moment)

        self.assertEqual("UPDATED", updated.rows[-1].label)
        self.assertEqual("08/24 09:05", updated.rows[-1].text)

    def test_the_original_state_is_left_untouched(self):
        state = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True)

        layout.with_updated_at(state, self.moment)

        self.assertEqual(["API", "VER", "PRINTER"], [row.label for row in state.rows])

    def test_states_stay_comparable_when_only_the_time_differs(self):
        state = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True)
        later = datetime(2026, 8, 24, 9, 6)

        self.assertNotEqual(
            layout.with_updated_at(state, self.moment),
            layout.with_updated_at(state, later),
        )
        self.assertEqual(state, layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True))


class BuildRowsTest(unittest.TestCase):
    def test_default_rows_are_api_version_printer(self):
        rows = layout.build_rows(True, "1.0.0", True)

        self.assertEqual(["API", "VER", "PRINTER"], [row.label for row in rows])

    def test_missing_version_is_shown_as_a_dash(self):
        rows = layout.build_rows(True, None, True)

        self.assertEqual("-", rows[1].text)

    def test_unknown_marks_are_kept_as_none(self):
        rows = layout.build_rows(None, None, None)

        self.assertIsNone(rows[0].mark)
        self.assertIsNone(rows[2].mark)

    def test_commit_is_shown_next_to_the_version(self):
        rows = layout.build_rows(True, "1.0.0", True, (), "a1b2c3d")

        self.assertEqual("1.0.0+a1b2c3d", rows[1].text)

    def test_extra_rows_are_appended(self):
        extra = (layout.Row("DEVICE-A", mark=False),)
        rows = layout.build_rows(True, "1.0.0", True, extra)

        self.assertEqual(["API", "VER", "PRINTER", "DEVICE-A"], [row.label for row in rows])


class BuildStateTest(unittest.TestCase):
    def setUp(self):
        self.config = config.Config()

    def test_url_is_built_from_the_ip(self):
        state = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True)

        self.assertEqual("http://192.168.11.20:8080/", state.url)
        self.assertEqual(8080, state.port)

    def test_url_is_absent_without_an_ip(self):
        state = layout.build_state(self.config, None, False, None, None)

        self.assertIsNone(state.url)
        self.assertIsNone(state.ip)

    def test_states_with_the_same_content_are_equal(self):
        first = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True)
        second = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True)

        self.assertEqual(first, second)

    def test_states_differ_when_the_printer_flips(self):
        first = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", True)
        second = layout.build_state(self.config, "192.168.11.20", True, "1.0.0", False)

        self.assertNotEqual(first, second)


class ChooseQrBoxSizeTest(unittest.TestCase):
    def test_preferred_size_is_used_when_it_fits(self):
        self.assertEqual(4, layout.choose_qr_box_size(25, 2, 4, 2, 116))

    def test_size_is_stepped_down_until_it_fits(self):
        self.assertEqual(3, layout.choose_qr_box_size(29, 2, 4, 2, 116))

    def test_none_is_returned_when_even_the_minimum_overflows(self):
        self.assertIsNone(layout.choose_qr_box_size(57, 2, 4, 2, 116))

    def test_result_is_an_exact_multiple_of_the_module_count(self):
        module_count, border, max_pixels = 25, 2, 116
        box_size = layout.choose_qr_box_size(module_count, border, 4, 2, max_pixels)

        pixels = (module_count + border * 2) * box_size

        self.assertEqual(0, pixels % box_size)
        self.assertLessEqual(pixels, max_pixels)

    def test_zero_modules_return_none(self):
        self.assertIsNone(layout.choose_qr_box_size(0, 0, 4, 2, 116))


class PositionTest(unittest.TestCase):
    def test_qr_is_vertically_centred(self):
        self.assertEqual((3, 3), layout.qr_position(116, 122))

    def test_qr_is_not_placed_above_the_top_edge(self):
        self.assertEqual((3, 0), layout.qr_position(200, 122))

    def test_text_starts_to_the_right_of_the_qr(self):
        self.assertEqual(3 + 116 + 6, layout.text_area_left(116))

    def test_rows_are_evenly_spaced(self):
        self.assertEqual([40, 55, 70], layout.row_positions(40, 3))

    def test_no_rows_produce_no_positions(self):
        self.assertEqual([], layout.row_positions(40, 0))


class FitsVerticallyTest(unittest.TestCase):
    def test_the_three_default_rows_fit(self):
        self.assertTrue(layout.fits_vertically(3, 122))

    def test_the_updated_row_still_fits(self):
        self.assertTrue(layout.fits_vertically(4, 122))

    def test_two_extra_rows_still_fit(self):
        self.assertTrue(layout.fits_vertically(5, 122))

    def test_six_rows_do_not_fit(self):
        self.assertFalse(layout.fits_vertically(6, 122))


if __name__ == "__main__":
    unittest.main()
