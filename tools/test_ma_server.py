"""常驻专服的配置恢复与启动失败不遗留测试。"""
import argparse
import sys
import tempfile
import unittest
from contextlib import ExitStack
from pathlib import Path
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ma_server as ms
import server_smoke as ss


class ServerLifecycleTest(unittest.TestCase):
    def setUp(self):
        self.stack = ExitStack()
        self.addCleanup(self.stack.close)
        root = Path(self.stack.enter_context(tempfile.TemporaryDirectory()))
        for mod, attr, value in [
            (ss, "RUN_DIR", root), (ss, "PROPS", root / "server.properties"),
            (ss, "LOG", root / "logs/latest.log"),
            (ms, "STATE", root / "ma_server.json"),
            (ms, "BACKUP", root / "server.properties.ma_server_backup"),
            (ms, "OUT", root / "logs/gradle.log"),
        ]:
            self.stack.enter_context(patch.object(mod, attr, value))
        self.stack.enter_context(patch.object(ss, "ensure_ports_free"))

    def test_failed_start_restores_original_bytes_and_cleans_state(self):
        original = b"# existing\r\nserver-port=25600\r\n"
        ss.PROPS.write_bytes(original)
        proc = Mock(pid=1234)
        with patch.object(ms.subprocess, "Popen", return_value=proc), \
                patch.object(ss, "wait_for", return_value=False), \
                patch.object(ss, "stop_process") as stop:
            self.assertEqual(1, ms.start(argparse.Namespace(world="test", seed="42", timeout=1)))
            stop.assert_called_once_with(proc, timeout=0)
        self.assertEqual(original, ss.PROPS.read_bytes())
        self.assertFalse(ms.STATE.exists())
        self.assertFalse(ms.BACKUP.exists())

    def test_missing_original_stays_missing_after_launch_failure(self):
        with patch.object(ms.subprocess, "Popen", side_effect=OSError("launch failed")):
            self.assertEqual(1, ms.start(argparse.Namespace(world="test", seed="42", timeout=1)))
        self.assertFalse(ss.PROPS.exists())
        self.assertFalse(ms.BACKUP.exists())

    def test_existing_backup_is_not_overwritten(self):
        ms.BACKUP.write_bytes(b"precious")
        self.assertEqual(1, ms.start(argparse.Namespace(world="test", seed="42", timeout=1)))
        self.assertEqual(b"precious", ms.BACKUP.read_bytes())

    def test_reject_path_traversal_world(self):
        with self.assertRaises(ValueError):
            ms.start(argparse.Namespace(world="../../world", seed="42", timeout=1))
        self.assertFalse(ss.PROPS.exists())


if __name__ == "__main__":
    unittest.main()
