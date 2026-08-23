import os
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import epaper  # noqa: E402


class EPaperTest(unittest.TestCase):
    def test_missing_driver_is_reported_at_show_time(self):
        panel = epaper.EPaper()

        with self.assertRaises(epaper.EPaperUnavailable):
            panel.show(object())

    def test_the_driver_is_not_imported_when_the_module_is_loaded(self):
        self.assertNotIn("waveshare_epd", sys.modules)

    def test_show_initialises_draws_and_sleeps(self):
        panel = epaper.EPaper()
        driver = mock.MagicMock()
        driver.getbuffer.return_value = "buffer"
        panel._create = lambda: driver

        panel.show("image")

        driver.init.assert_called_once_with()
        driver.getbuffer.assert_called_once_with("image")
        driver.display.assert_called_once_with("buffer")
        driver.sleep.assert_called_once_with()

    def test_the_driver_is_created_only_once(self):
        panel = epaper.EPaper()
        driver = mock.MagicMock()
        create = mock.MagicMock(return_value=driver)
        panel._create = create

        panel.show("first")
        panel.show("second")

        self.assertEqual(1, create.call_count)
        self.assertEqual(2, driver.init.call_count)

    def test_clear_initialises_whitens_and_sleeps(self):
        panel = epaper.EPaper()
        driver = mock.MagicMock()
        panel._create = lambda: driver

        panel.clear()

        driver.init.assert_called_once_with()
        driver.Clear.assert_called_once_with(epaper.WHITE)
        driver.display.assert_not_called()
        driver.sleep.assert_called_once_with()

    def test_clear_reports_a_missing_driver(self):
        with self.assertRaises(epaper.EPaperUnavailable):
            epaper.EPaper().clear()

    def test_clear_reuses_the_driver_created_by_show(self):
        panel = epaper.EPaper()
        driver = mock.MagicMock()
        create = mock.MagicMock(return_value=driver)
        panel._create = create

        panel.show("image")
        panel.clear()

        self.assertEqual(1, create.call_count)

    def test_close_sleeps_the_panel(self):
        panel = epaper.EPaper()
        driver = mock.MagicMock()
        panel._create = lambda: driver
        panel.show("image")
        driver.sleep.reset_mock()

        panel.close()

        driver.sleep.assert_called_once_with()

    def test_close_is_safe_before_any_draw(self):
        epaper.EPaper().close()

    def test_close_survives_a_failing_driver(self):
        panel = epaper.EPaper()
        driver = mock.MagicMock()
        panel._create = lambda: driver
        panel.show("image")
        driver.sleep.side_effect = OSError("SPI busy")

        with self.assertLogs(epaper.logger, level="ERROR"):
            panel.close()


class FileEPaperTest(unittest.TestCase):
    def test_the_image_is_saved_to_the_given_path(self):
        with tempfile.TemporaryDirectory() as directory:
            path = os.path.join(directory, "preview.png")
            image = mock.MagicMock()

            epaper.FileEPaper(path).show(image)

            image.save.assert_called_once_with(path)

    def test_clear_leaves_no_file_behind(self):
        with tempfile.TemporaryDirectory() as directory:
            path = os.path.join(directory, "preview.png")

            with self.assertLogs(epaper.logger, level="INFO"):
                epaper.FileEPaper(path).clear()

            self.assertFalse(os.path.exists(path))

    def test_close_does_nothing(self):
        epaper.FileEPaper("/tmp/preview.png").close()


if __name__ == "__main__":
    unittest.main()
