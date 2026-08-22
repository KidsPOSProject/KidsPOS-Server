import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import config  # noqa: E402


class EnvTestCase(unittest.TestCase):
    def setUp(self):
        self._saved = {key: value for key, value in os.environ.items() if key.startswith("KIDSPOS_")}
        for key in self._saved:
            del os.environ[key]

    def tearDown(self):
        for key in [key for key in os.environ if key.startswith("KIDSPOS_")]:
            del os.environ[key]
        os.environ.update(self._saved)


class ConfigDefaultsTest(EnvTestCase):
    def test_defaults_are_used_when_env_is_absent(self):
        cfg = config.Config.from_env()

        self.assertEqual("http://127.0.0.1:8080/api/status", cfg.status_url)
        self.assertEqual(8080, cfg.web_port)
        self.assertEqual(20, cfg.poll_interval)
        self.assertEqual(4, cfg.qr_box_size)
        self.assertEqual(2, cfg.qr_border)
        self.assertEqual(250, cfg.width)
        self.assertEqual(122, cfg.height)
        self.assertEqual((), cfg.extra_rows)

    def test_status_url_points_at_loopback(self):
        self.assertIn("127.0.0.1", config.Config.from_env().status_url)


class ConfigOverrideTest(EnvTestCase):
    def test_values_are_read_from_env(self):
        os.environ["KIDSPOS_STATUS_URL"] = "http://127.0.0.1:9999/api/status"
        os.environ["KIDSPOS_WEB_PORT"] = "9090"
        os.environ["KIDSPOS_POLL_INTERVAL"] = "30"
        os.environ["KIDSPOS_FAIL_THRESHOLD"] = "5"

        cfg = config.Config.from_env()

        self.assertEqual("http://127.0.0.1:9999/api/status", cfg.status_url)
        self.assertEqual(9090, cfg.web_port)
        self.assertEqual(30, cfg.poll_interval)
        self.assertEqual(5, cfg.fail_threshold)

    def test_surrounding_spaces_are_trimmed(self):
        os.environ["KIDSPOS_WEB_SCHEME"] = "  https  "

        self.assertEqual("https", config.Config.from_env().web_scheme)

    def test_empty_value_falls_back_to_default(self):
        os.environ["KIDSPOS_WEB_PORT"] = "   "

        self.assertEqual(8080, config.Config.from_env().web_port)

    def test_non_numeric_value_falls_back_to_default(self):
        os.environ["KIDSPOS_POLL_INTERVAL"] = "twenty"

        with self.assertLogs(config.logger, level="WARNING"):
            self.assertEqual(20, config.Config.from_env().poll_interval)

    def test_too_small_value_falls_back_to_default(self):
        os.environ["KIDSPOS_POLL_INTERVAL"] = "0"

        with self.assertLogs(config.logger, level="WARNING"):
            self.assertEqual(20, config.Config.from_env().poll_interval)

    def test_zero_border_is_allowed(self):
        os.environ["KIDSPOS_QR_BORDER"] = "0"

        self.assertEqual(0, config.Config.from_env().qr_border)

    def test_min_box_size_never_exceeds_box_size(self):
        os.environ["KIDSPOS_QR_BOX_SIZE"] = "3"
        os.environ["KIDSPOS_QR_MIN_BOX_SIZE"] = "6"

        with self.assertLogs(config.logger, level="WARNING"):
            cfg = config.Config.from_env()

        self.assertEqual(3, cfg.qr_box_size)
        self.assertEqual(3, cfg.qr_min_box_size)


class WebUrlTest(EnvTestCase):
    def test_url_contains_ip_and_port(self):
        cfg = config.Config.from_env()

        self.assertEqual("http://192.168.11.20:8080/", cfg.web_url("192.168.11.20"))

    def test_scheme_and_port_are_configurable(self):
        os.environ["KIDSPOS_WEB_SCHEME"] = "https"
        os.environ["KIDSPOS_WEB_PORT"] = "8443"

        self.assertEqual("https://10.0.0.2:8443/", config.Config.from_env().web_url("10.0.0.2"))


if __name__ == "__main__":
    unittest.main()
