"""procutil（跨平台进程工具）与按命令行识别客户端 / 专服的单元测试。运行：python3 -m unittest tools/test_procutil.py"""
import subprocess
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ma_remote  # noqa: E402
import procutil  # noqa: E402
import server_smoke as ss  # noqa: E402

WIN_CLIENT = ('"C:\\Program Files\\Microsoft\\jdk-21\\bin\\java.exe" '
              '"@D:\\Project\\MythicAgeMod\\neoforge\\build\\moddev\\clientRunVmArgs.txt" '
              '"@D:\\Project\\MythicAgeMod\\neoforge\\build\\moddev\\clientRunClasspath.txt" '
              'net.neoforged.devlaunch.Main')
MAC_CLIENT = ('/usr/bin/java @/Users/x/MythicAgeMod/neoforge/build/moddev/clientRunVmArgs.txt '
              '-Dfml.modFolders=mythic_age%%/Users/x/MythicAgeMod/neoforge/build/classes/java/agent net.Main')


class NormTest(unittest.TestCase):
    def test_windows_backslash_and_case(self):
        self.assertEqual("d:/project/a/b.txt", procutil.norm("D:\\Project\\a\\B.txt", windows=True))

    def test_posix_keeps_case(self):
        self.assertEqual("/Users/X/a", procutil.norm("/Users/X/a", windows=False))


class ArgfileTest(unittest.TestCase):
    def test_quoted_and_bare_argfiles(self):
        self.assertEqual(["D:\\Project\\MythicAgeMod\\neoforge\\build\\moddev\\clientRunVmArgs.txt",
                          "D:\\Project\\MythicAgeMod\\neoforge\\build\\moddev\\clientRunClasspath.txt"],
                         procutil.argfiles(WIN_CLIENT))
        self.assertEqual(["/Users/x/MythicAgeMod/neoforge/build/moddev/clientRunVmArgs.txt"],
                         procutil.argfiles(MAC_CLIENT))

    def test_expanded_includes_argfile_text_and_skips_unreadable(self):
        def read(p):
            if p.endswith("clientRunVmArgs.txt"):
                return "-Dfml.modFolders=x%%D:\\Project\\MythicAgeMod\\neoforge\\build\\classes\\java\\agent"
            raise OSError("gone")
        text = procutil.expanded_cmdline(WIN_CLIENT, read)
        self.assertIn("classes\\java\\agent", text)
        self.assertTrue(text.startswith(WIN_CLIENT))


class ParseTest(unittest.TestCase):
    def test_ps_skips_zombies_and_short_rows(self):
        rows = "  12 S    /usr/bin/java -jar a.jar\n  13 Z    [java]\n bad\n  14 R+   python3 x.py\n"
        self.assertEqual([(12, "/usr/bin/java -jar a.jar"), (14, "python3 x.py")], procutil.parse_ps(rows))

    def test_cim_single_object_array_and_empty(self):
        self.assertEqual([(5, "java -x")], procutil.parse_cim_json('{"ProcessId":5,"CommandLine":"java -x"}'))
        self.assertEqual([(5, ""), (6, "b")],
                         procutil.parse_cim_json('[{"ProcessId":5,"CommandLine":null},{"ProcessId":6,"CommandLine":"b"}]'))
        self.assertEqual([], procutil.parse_cim_json("  \r\n"))


class LaunchTest(unittest.TestCase):
    def test_gradlew_per_platform(self):
        self.assertEqual(["./gradlew"], procutil.gradlew(Path("/r"), windows=False))
        self.assertEqual([str(Path("D:/r") / "gradlew.bat")], procutil.gradlew(Path("D:/r"), windows=True))

    def test_detached_kwargs(self):
        self.assertEqual({"start_new_session": True}, procutil.detached_kwargs(windows=False))
        if procutil.IS_WINDOWS:
            flags = procutil.detached_kwargs(windows=True)["creationflags"]
            self.assertTrue(flags & subprocess.CREATE_NEW_PROCESS_GROUP)
            self.assertTrue(flags & subprocess.CREATE_NO_WINDOW)


class IdentifyTest(unittest.TestCase):
    def test_agent_client_on_cmdline_mac(self):
        self.assertTrue(ma_remote.is_agent_client(MAC_CLIENT, read=lambda p: ""))

    def test_agent_client_via_argfile_windows(self):
        agent = "-Dfml.modFolders=x%%D:\\Project\\MythicAgeMod\\neoforge\\build\\classes\\java\\agent"
        self.assertTrue(ma_remote.is_agent_client(WIN_CLIENT, read=lambda p: agent))
        self.assertFalse(ma_remote.is_agent_client(WIN_CLIENT, read=lambda p: "-Dfml.modFolders=x%%main"))

    def test_non_client_java_ignored(self):
        self.assertFalse(ma_remote.is_agent_client("java -jar gradle-daemon.jar classes/java/agent"))

    def test_server_cmdline_separators_and_case(self):
        root = Path("D:/Project/MythicAgeMod")
        cmd = '"C:\\jdk\\bin\\java.exe" "@d:\\project\\mythicagemod\\neoforge\\build\\moddev\\serverRunVmArgs.txt"'
        if procutil.IS_WINDOWS:
            self.assertTrue(ss.is_server_cmdline(cmd, root))
        mac = "/usr/bin/java @/Users/x/M/neoforge/build/moddev/serverRunVmArgs.txt"
        self.assertTrue(ss.is_server_cmdline(mac, Path("/Users/x/M")))
        self.assertFalse(ss.is_server_cmdline("/usr/bin/python3 @/Users/x/M/neoforge/build/moddev/serverRunVmArgs.txt",
                                              Path("/Users/x/M")))
        self.assertFalse(ss.is_server_cmdline(mac, Path("/Users/x/Other")))


if __name__ == "__main__":
    unittest.main()
