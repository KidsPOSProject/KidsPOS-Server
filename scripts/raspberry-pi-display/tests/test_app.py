import dataclasses
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import app  # noqa: E402
import config as config_module  # noqa: E402
import health  # noqa: E402
import layout  # noqa: E402


def observation(ip="192.168.11.20", **status):
    defaults = {
        "reachable": True,
        "version": "1.0.0",
        "api_version": "1",
        "printer_configured": True,
        "printer_reachable": True,
    }
    defaults.update(status)
    return {"ip": ip, "status": health.ServerStatus(**defaults)}


class RecordingDevice:
    def __init__(self, fail_times=0):
        self.images = []
        self.closed = False
        self._fail_times = fail_times

    def show(self, image):
        if self._fail_times > 0:
            self._fail_times -= 1
            raise OSError("SPI busy")
        self.images.append(image)

    def close(self):
        self.closed = True


class MonitorTest(unittest.TestCase):
    def setUp(self):
        self.config = config_module.Config()

    def test_healthy_observation_produces_all_marks(self):
        state = app.Monitor(self.config).poll(observation())

        self.assertEqual("192.168.11.20", state.ip)
        self.assertEqual("http://192.168.11.20:8080/", state.url)
        self.assertEqual([True, "1.0.0", True], [state.rows[0].mark, state.rows[1].text, state.rows[2].mark])

    def test_unreachable_api_shows_unknown_printer(self):
        state = app.Monitor(self.config).poll({"ip": "192.168.11.20", "status": health.UNREACHABLE})

        self.assertFalse(state.rows[0].mark)
        self.assertEqual("-", state.rows[1].text)
        self.assertIsNone(state.rows[2].mark)

    def test_version_is_kept_when_the_api_goes_down(self):
        monitor = app.Monitor(self.config)
        monitor.poll(observation())

        state = monitor.poll({"ip": "192.168.11.20", "status": health.UNREACHABLE})

        self.assertEqual("1.0.0", state.rows[1].text)

    def test_api_recovers_only_after_the_ok_threshold(self):
        config = dataclasses.replace(self.config, fail_threshold=2, ok_threshold=2)
        monitor = app.Monitor(config)
        monitor.poll({"ip": None, "status": health.UNREACHABLE})

        first = monitor.poll(observation())
        second = monitor.poll(observation())

        self.assertFalse(first.rows[0].mark)
        self.assertTrue(second.rows[0].mark)

    def test_a_single_printer_failure_does_not_flip_the_mark(self):
        config = dataclasses.replace(self.config, fail_threshold=3, ok_threshold=2)
        monitor = app.Monitor(config)
        monitor.poll(observation())

        state = monitor.poll(observation(printer_reachable=False))

        self.assertTrue(state.rows[2].mark)

    def test_no_network_produces_no_url(self):
        state = app.Monitor(self.config).poll(observation(ip=None))

        self.assertIsNone(state.url)

    def test_extra_rows_from_the_config_are_appended(self):
        config = dataclasses.replace(self.config, extra_rows=(layout.Row("DEVICE-A", mark=True),))

        state = app.Monitor(config).poll(observation())

        self.assertEqual("DEVICE-A", state.rows[3].label)


class RunnerTest(unittest.TestCase):
    def setUp(self):
        self.config = dataclasses.replace(config_module.Config(), poll_interval=1, refresh_interval=1000)

    def _runner(self, device, observations, clock=None):
        runner = app.Runner(
            self.config,
            device,
            lambda state, cfg: state,
            clock or (lambda: 0.0),
            sleeper=lambda _: False,
        )
        runner._monitor = _ScriptedMonitor(self.config, observations)
        return runner

    def test_the_first_tick_always_draws(self):
        device = RecordingDevice()
        runner = self._runner(device, [observation()])

        runner.tick()

        self.assertEqual(1, len(device.images))

    def test_an_unchanged_state_is_not_redrawn(self):
        device = RecordingDevice()
        runner = self._runner(device, [observation(), observation()])

        runner.tick()
        runner.tick()

        self.assertEqual(1, len(device.images))

    def test_a_changed_state_is_redrawn(self):
        device = RecordingDevice()
        runner = self._runner(device, [observation(), observation(ip="192.168.11.21")])

        runner.tick()
        runner.tick()

        self.assertEqual(2, len(device.images))

    def test_an_unchanged_state_is_redrawn_after_the_refresh_interval(self):
        clock = _Clock([0.0, 1000.0])
        device = RecordingDevice()
        runner = self._runner(device, [observation(), observation()], clock)

        runner.tick()
        runner.tick()

        self.assertEqual(2, len(device.images))

    def test_a_failed_draw_is_retried_on_the_next_tick(self):
        device = RecordingDevice(fail_times=1)
        runner = self._runner(device, [observation(), observation()])

        with self.assertLogs(app.logger, level="ERROR"):
            runner.tick()
        runner.tick()

        self.assertEqual(1, len(device.images))
        self.assertIsNotNone(runner.previous)

    def test_a_failed_draw_does_not_stop_the_loop(self):
        device = RecordingDevice(fail_times=5)
        runner = self._runner(device, [observation()] * 3)

        with self.assertLogs(app.logger, level="ERROR"):
            runner.run(max_iterations=3)

        self.assertEqual([], device.images)

    def test_run_stops_after_the_requested_iterations(self):
        device = RecordingDevice()
        runner = self._runner(device, [observation(ip="192.168.11.2{}".format(i)) for i in range(3)])

        runner.run(max_iterations=3)

        self.assertEqual(3, len(device.images))

    def test_stop_ends_the_loop(self):
        device = RecordingDevice()
        runner = self._runner(device, [observation()] * 5)
        runner.stop()

        runner.run()

        self.assertEqual([], device.images)


class _Clock:
    def __init__(self, values):
        self._values = list(values)

    def __call__(self):
        return self._values.pop(0) if self._values else 0.0


class _ScriptedMonitor(app.Monitor):
    def __init__(self, config, observations):
        super().__init__(config)
        self._observations = list(observations)

    def poll(self, observation_override=None):
        pending = self._observations.pop(0) if self._observations else observation()
        return super().poll(pending)


class ParseArgsTest(unittest.TestCase):
    def test_defaults_run_forever_against_the_panel(self):
        args = app.parse_args([])

        self.assertFalse(args.once)
        self.assertFalse(args.verbose)

    def test_output_selects_the_file_device(self):
        args = app.parse_args(["--output", "/tmp/preview.png"])

        self.assertEqual("/tmp/preview.png", args.output)

    def test_once_is_recognised(self):
        self.assertTrue(app.parse_args(["--once"]).once)


class BuildDeviceTest(unittest.TestCase):
    def test_an_output_path_selects_the_file_device(self):
        import epaper

        self.assertIsInstance(app.build_device("/tmp/preview.png"), epaper.FileEPaper)

    def test_no_output_path_selects_the_panel(self):
        import epaper

        self.assertIsInstance(app.build_device(None), epaper.EPaper)


if __name__ == "__main__":
    unittest.main()
