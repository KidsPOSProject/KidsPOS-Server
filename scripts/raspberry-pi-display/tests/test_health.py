import http.client
import io
import json
import os
import socket
import sys
import unittest
import urllib.error
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import health  # noqa: E402


def status_payload(**overrides):
    payload = {
        "status": "OK",
        "version": "1.0.0",
        "apiVersion": 1,
        "printer": {"configured": True, "reachable": True, "total": 1, "reachableCount": 1},
    }
    payload.update(overrides)
    return payload


class ParseStatusTest(unittest.TestCase):
    def test_full_payload_is_parsed(self):
        status = health.parse_status(status_payload())

        self.assertTrue(status.reachable)
        self.assertEqual("1.0.0", status.version)
        self.assertEqual("1", status.api_version)
        self.assertTrue(status.printer_configured)
        self.assertTrue(status.printer_reachable)
        self.assertTrue(status.printer_ok)

    def test_unreachable_printer_is_not_ok(self):
        payload = status_payload(printer={"configured": True, "reachable": False})

        self.assertFalse(health.parse_status(payload).printer_ok)

    def test_unconfigured_printer_is_unknown(self):
        payload = status_payload(printer={"configured": False, "reachable": False})

        self.assertIsNone(health.parse_status(payload).printer_ok)

    def test_missing_printer_section_is_unknown(self):
        payload = status_payload()
        del payload["printer"]

        status = health.parse_status(payload)

        self.assertTrue(status.reachable)
        self.assertIsNone(status.printer_ok)

    def test_null_printer_section_is_unknown(self):
        self.assertIsNone(health.parse_status(status_payload(printer=None)).printer_ok)

    def test_missing_version_is_none(self):
        payload = status_payload()
        del payload["version"]

        self.assertIsNone(health.parse_status(payload).version)

    def test_blank_version_is_none(self):
        self.assertIsNone(health.parse_status(status_payload(version="   ")).version)

    def test_non_dict_payload_is_unreachable(self):
        with self.assertLogs(health.logger, level="WARNING"):
            self.assertFalse(health.parse_status([1, 2, 3]).reachable)

    def test_unreachable_status_reports_unknown_printer(self):
        self.assertIsNone(health.UNREACHABLE.printer_ok)


class FetchStatusTest(unittest.TestCase):
    def _response(self, body):
        response = mock.MagicMock()
        response.read.return_value = body
        response.__enter__.return_value = response
        response.__exit__.return_value = False
        return response

    def test_valid_response_is_parsed(self):
        body = json.dumps(status_payload()).encode("utf-8")
        with mock.patch.object(health.urllib.request, "urlopen", return_value=self._response(body)):
            status = health.fetch_status("http://127.0.0.1:8080/api/status", 3)

        self.assertTrue(status.reachable)
        self.assertEqual("1.0.0", status.version)

    def test_connection_error_is_unreachable(self):
        error = urllib.error.URLError("refused")
        with mock.patch.object(health.urllib.request, "urlopen", side_effect=error):
            self.assertFalse(health.fetch_status("http://127.0.0.1:8080/api/status", 3).reachable)

    def test_http_error_is_unreachable(self):
        error = urllib.error.HTTPError("url", 500, "boom", {}, io.BytesIO(b""))
        with mock.patch.object(health.urllib.request, "urlopen", side_effect=error):
            self.assertFalse(health.fetch_status("http://127.0.0.1:8080/api/status", 3).reachable)

    def test_timeout_is_unreachable(self):
        with mock.patch.object(health.urllib.request, "urlopen", side_effect=socket.timeout()):
            self.assertFalse(health.fetch_status("http://127.0.0.1:8080/api/status", 3).reachable)

    def test_malformed_status_line_is_unreachable(self):
        error = http.client.BadStatusLine("GARBAGE")
        with mock.patch.object(health.urllib.request, "urlopen", side_effect=error):
            self.assertFalse(health.fetch_status("http://127.0.0.1:8080/api/status", 3).reachable)

    def test_truncated_body_is_unreachable(self):
        response = self._response(b"")
        response.read.side_effect = http.client.IncompleteRead(b"12345", 95)
        with mock.patch.object(health.urllib.request, "urlopen", return_value=response):
            self.assertFalse(health.fetch_status("http://127.0.0.1:8080/api/status", 3).reachable)

    def test_broken_json_is_unreachable(self):
        with mock.patch.object(health.urllib.request, "urlopen", return_value=self._response(b"<html>")):
            with self.assertLogs(health.logger, level="WARNING"):
                self.assertFalse(health.fetch_status("http://127.0.0.1:8080/api/status", 3).reachable)

    def test_timeout_is_passed_through(self):
        body = json.dumps(status_payload()).encode("utf-8")
        with mock.patch.object(
            health.urllib.request, "urlopen", return_value=self._response(body)
        ) as urlopen:
            health.fetch_status("http://127.0.0.1:8080/api/status", 7)

        self.assertEqual(7, urlopen.call_args.kwargs["timeout"])


class LocalIpv4Test(unittest.TestCase):
    def _socket(self, ip, error=None):
        sock = mock.MagicMock()
        if error is not None:
            sock.connect.side_effect = error
        sock.getsockname.return_value = (ip, 12345)
        return sock

    def test_routed_address_is_returned(self):
        sock = self._socket("192.168.11.20")
        with mock.patch.object(health.socket, "socket", return_value=sock):
            self.assertEqual("192.168.11.20", health.get_local_ipv4("1.1.1.1", 80))

        sock.connect.assert_called_once_with(("1.1.1.1", 80))
        sock.close.assert_called_once_with()

    def test_loopback_is_rejected(self):
        with mock.patch.object(health.socket, "socket", return_value=self._socket("127.0.0.1")):
            self.assertIsNone(health.get_local_ipv4("1.1.1.1", 80))

    def test_connect_failure_returns_none(self):
        sock = self._socket("192.168.11.20", error=OSError("network unreachable"))
        with mock.patch.object(health.socket, "socket", return_value=sock):
            self.assertIsNone(health.get_local_ipv4("1.1.1.1", 80))

        sock.close.assert_called_once_with()

    def test_socket_creation_failure_returns_none(self):
        with mock.patch.object(health.socket, "socket", side_effect=OSError("too many open files")):
            self.assertIsNone(health.get_local_ipv4("1.1.1.1", 80))

    def test_udp_socket_is_used_so_no_packet_is_sent(self):
        with mock.patch.object(health.socket, "socket") as factory:
            factory.return_value = self._socket("192.168.11.20")
            health.get_local_ipv4("1.1.1.1", 80)

        factory.assert_called_once_with(socket.AF_INET, socket.SOCK_DGRAM)


class DebouncerTest(unittest.TestCase):
    def test_first_success_is_adopted_immediately(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)

        self.assertTrue(debouncer.update(True))

    def test_first_failure_stays_unknown_until_the_fail_threshold(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)

        self.assertIsNone(debouncer.update(False))
        self.assertIsNone(debouncer.update(False))
        self.assertFalse(debouncer.update(False))

    def test_a_success_while_unknown_cancels_the_failure_streak(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(False)
        debouncer.update(False)

        self.assertTrue(debouncer.update(True))

    def test_single_failure_does_not_flip(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(True)

        self.assertTrue(debouncer.update(False))
        self.assertTrue(debouncer.update(False))

    def test_flips_after_threshold_is_reached(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(True)

        debouncer.update(False)
        debouncer.update(False)

        self.assertFalse(debouncer.update(False))

    def test_streak_is_reset_by_a_differing_observation(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(True)
        debouncer.update(False)
        debouncer.update(False)
        debouncer.update(True)

        self.assertTrue(debouncer.update(False))
        self.assertTrue(debouncer.update(False))
        self.assertFalse(debouncer.update(False))

    def test_recovery_uses_the_ok_threshold(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(True)
        for _ in range(3):
            debouncer.update(False)

        self.assertFalse(debouncer.update(True))
        self.assertTrue(debouncer.update(True))

    def test_unknown_observation_clears_the_value(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(True)

        self.assertIsNone(debouncer.update(None))
        self.assertIsNone(debouncer.value)

    def test_failure_after_unknown_needs_the_fail_threshold_again(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(True)
        debouncer.update(None)

        self.assertIsNone(debouncer.update(False))
        self.assertIsNone(debouncer.update(False))
        self.assertFalse(debouncer.update(False))

    def test_success_after_unknown_is_adopted_immediately(self):
        debouncer = health.Debouncer(fail_threshold=3, ok_threshold=2)
        debouncer.update(False)
        debouncer.update(False)
        debouncer.update(False)
        debouncer.update(None)

        self.assertTrue(debouncer.update(True))

    def test_thresholds_below_one_are_clamped(self):
        debouncer = health.Debouncer(fail_threshold=0, ok_threshold=0)
        debouncer.update(True)

        self.assertFalse(debouncer.update(False))


class CollectTest(unittest.TestCase):
    def test_ip_and_status_are_collected(self):
        config = mock.MagicMock(probe_host="1.1.1.1", probe_port=80, status_url="url", http_timeout=3)
        with mock.patch.object(health, "get_local_ipv4", return_value="192.168.11.20"), mock.patch.object(
            health, "fetch_status", return_value=health.UNREACHABLE
        ) as fetch:
            observation = health.collect(config)

        self.assertEqual("192.168.11.20", observation["ip"])
        self.assertFalse(observation["status"].reachable)
        fetch.assert_called_once_with("url", 3)


if __name__ == "__main__":
    unittest.main()
