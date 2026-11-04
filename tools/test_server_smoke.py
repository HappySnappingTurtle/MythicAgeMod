"""server_smoke.py 的纯函数测试（TC-T24-95）。"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import server_smoke as ss  # noqa: E402

DONE = "[Server thread/INFO] [minecraft/DedicatedServer]: Done (4.9s)! For help, type \"help\"\n"
SAVED = "[Server thread/INFO] [minecraft/MinecraftServer]: All dimensions are saved\n"


class JudgeTest(unittest.TestCase):
    def test_clean_log_passes(self):
        self.assertEqual([], ss.judge("[main/INFO] [mythicage]: Mythic Age loading\n" + DONE))

    def test_not_started(self):
        self.assertTrue(any("Done" in p for p in ss.judge("[main/INFO] starting\n")))

    def test_client_class_fails(self):
        log = DONE + "[main/ERROR] [x]: Attempted to load class net/minecraft/client/Minecraft for invalid dist DEDICATED_SERVER\n"
        self.assertTrue(any("客户端类" in p for p in ss.judge(log)))

    def test_mod_error_fails(self):
        log = DONE + "[Server thread/ERROR] [io.github.happysnappingturtle.mythicage.MythicAge/]: 竞技场 x 出错\n"
        self.assertTrue(any("模组报错" in p for p in ss.judge(log)))

    def test_allowed_error_passes(self):
        log = DONE + "[Worker/ERROR] [mythic_age/]: Unprimed heightmap: WORLD_SURFACE_WG\n"
        self.assertEqual([], ss.judge(log))

    def test_other_mod_error_ignored(self):
        log = DONE + "[Server thread/ERROR] [othermod/]: something\n"
        self.assertEqual([], ss.judge(log))


class PropertiesTest(unittest.TestCase):
    def test_patch_keeps_order_and_appends(self):
        text = "#comment\nlevel-name=world\nserver-port=25599\n"
        out = ss.patch_properties(text, {"level-name": "smoke", "enable-rcon": "true"})
        self.assertEqual("#comment\nlevel-name=smoke\nserver-port=25599\nenable-rcon=true\n", out)

    def test_local_only_and_fixed_ports(self):
        values = ss.local_properties("smoke", "42", "test-password")
        self.assertEqual("127.0.0.1", values["server-ip"])
        self.assertEqual("25599", values["server-port"])
        self.assertEqual("false", values["online-mode"])


class RunJudgeTest(unittest.TestCase):
    def test_full_run(self):
        self.assertEqual([], ss.judge_run(DONE + SAVED, 0, ["配置有效"]))

    def test_failed_exit_is_not_success(self):
        self.assertTrue(ss.judge_run(DONE + SAVED, 1, ["ok"]))

    def test_save_start_is_not_save_finished(self):
        self.assertTrue(ss.judge_run(DONE + "Saving worlds", 0, ["ok"]))

    def test_forced_exit_is_not_success(self):
        self.assertTrue(ss.judge_run(DONE + SAVED, 0, ["ok"], True))

    def test_command_syntax_error(self):
        self.assertTrue(ss.judge_run(DONE + SAVED, 0, ["Unknown or incomplete command <--[HERE]"]))

    def test_empty_command_response(self):
        self.assertTrue(ss.judge_run(DONE + SAVED, 0, [""]))


if __name__ == "__main__":
    unittest.main()
