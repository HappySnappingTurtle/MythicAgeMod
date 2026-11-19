"""跨平台的进程与窗口小工具（macOS / Linux / Windows），给 ma_remote、ma_server、server_smoke 共用。

各平台差异都收在这里：
- 启动 Gradle：POSIX 用 ./gradlew，Windows 用 gradlew.bat；
- 后台启动：POSIX 开新会话（可整组结束），Windows 开新进程组、不弹控制台窗口；
- 列出 java 进程与命令行：POSIX 用 ps，Windows 用 PowerShell 的 Win32_Process；
- 结束进程：POSIX 发 SIGTERM / SIGKILL（单个或整组），Windows 用 taskkill（/T 连同子进程，/F 强制）；
- Windows 上的「不抢前台」：把游戏窗口压到底层（不最小化，最小化后画面不渲染、截图失败），并把焦点还给原来的窗口。

命令行里的路径一律先 norm()：反斜杠换成 /，Windows 上再转小写（路径不分大小写）。
MDG 用 @参数文件 启动 Minecraft，参数文件里的内容也算进命令行（expanded_cmdline）。
"""
from __future__ import annotations

import json
import os
import re
import signal
import subprocess
import sys
from pathlib import Path

IS_WINDOWS = os.name == "nt"


# ───────────── 纯函数（有单元测试） ─────────────

def norm(text: str, windows: bool = IS_WINDOWS) -> str:
    """路径比较用：统一成 /，Windows 上不分大小写。"""
    text = text.replace("\\", "/")
    return text.lower() if windows else text


ARGFILE = re.compile(r'''(?:^|\s)["']?@([^"'\s]+)''')


def argfiles(cmdline: str) -> list[str]:
    """命令行里 java 的 @参数文件 路径。"""
    return ARGFILE.findall(cmdline)


def expanded_cmdline(cmdline: str, read=None) -> str:
    """命令行连同其中 @参数文件 的内容（读不到的参数文件忽略）。"""
    read = read or (lambda p: Path(p).read_text(encoding="utf-8", errors="replace"))
    parts = [cmdline]
    for p in argfiles(cmdline):
        try:
            parts.append(read(p))
        except OSError:
            continue
    return "\n".join(parts)


def gradlew(root: Path, windows: bool = IS_WINDOWS) -> list[str]:
    """调用 Gradle wrapper 的命令前缀（cwd 应为仓库根目录）。"""
    return [str(Path(root) / "gradlew.bat")] if windows else ["./gradlew"]


def detached_kwargs(windows: bool = IS_WINDOWS) -> dict:
    """后台启动 Gradle 的 Popen 参数。"""
    if windows:
        return {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP | subprocess.CREATE_NO_WINDOW}
    return {"start_new_session": True}


def parse_ps(rows: str) -> list[tuple[int, str]]:
    """解析 `ps -axo pid=,stat=,command=` 的输出，去掉僵尸进程。"""
    out = []
    for line in rows.splitlines():
        p = line.split(None, 2)
        if len(p) == 3 and p[0].isdigit() and not p[1].startswith("Z"):
            out.append((int(p[0]), p[2]))
    return out


def parse_cim_json(text: str) -> list[tuple[int, str]]:
    """解析 PowerShell `Select-Object ProcessId,CommandLine | ConvertTo-Json` 的输出（单个对象或数组）。"""
    text = text.strip()
    if not text:
        return []
    data = json.loads(text)
    if isinstance(data, dict):
        data = [data]
    return [(int(d["ProcessId"]), d.get("CommandLine") or "") for d in data]


# ───────────── 进程 ─────────────

_PS_JAVA = ("[Console]::OutputEncoding = [Text.Encoding]::UTF8; "
            "Get-CimInstance Win32_Process -Filter \"Name='java.exe' or Name='javaw.exe'\" | "
            "Select-Object ProcessId,CommandLine | ConvertTo-Json -Compress")


def java_processes() -> list[tuple[int, str]]:
    """本机的 java 进程 (pid, 命令行)。POSIX 上返回全部进程，由调用方按命令行筛。"""
    if IS_WINDOWS:
        r = subprocess.run(["powershell", "-NoProfile", "-NonInteractive", "-Command", _PS_JAVA],
                           capture_output=True, text=True, encoding="utf-8", errors="replace")
        return parse_cim_json(r.stdout)
    r = subprocess.run(["ps", "-axo", "pid=,stat=,command="], capture_output=True, text=True, check=True)
    return parse_ps(r.stdout)


def pid_alive(pid: int) -> bool:
    if IS_WINDOWS:
        import ctypes
        k32 = ctypes.windll.kernel32
        h = k32.OpenProcess(0x1000, False, int(pid))  # PROCESS_QUERY_LIMITED_INFORMATION
        if not h:
            return False
        try:
            code = ctypes.c_ulong()
            return bool(k32.GetExitCodeProcess(h, ctypes.byref(code))) and code.value == 259  # STILL_ACTIVE
        finally:
            k32.CloseHandle(h)
    try:
        os.kill(int(pid), 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    return True


def group_alive(pid: int) -> bool:
    """POSIX：进程组里还有非僵尸进程；Windows：没有进程组概念，看进程本身。"""
    if IS_WINDOWS:
        return pid_alive(pid)
    rows = subprocess.run(["ps", "-axo", "pgid=,stat="], capture_output=True, text=True, check=True).stdout
    return any(parts[0] == str(pid) and not parts[1].startswith("Z")
               for row in rows.splitlines() if len(parts := row.split()) >= 2)


def terminate(pids, force: bool = False) -> None:
    """结束进程（Windows 上连同子进程）。force=False 是请求退出，force=True 是强制。"""
    for pid in pids:
        if IS_WINDOWS:
            subprocess.run(["taskkill", "/PID", str(pid), "/T", *(["/F"] if force else [])], capture_output=True)
        else:
            try:
                os.kill(int(pid), signal.SIGKILL if force else signal.SIGTERM)
            except ProcessLookupError:
                pass


def terminate_group(pid: int, force: bool = False) -> None:
    """结束后台启动的整组进程（POSIX 进程组；Windows 进程树）。"""
    if IS_WINDOWS:
        terminate([pid], force)
        return
    try:
        os.killpg(int(pid), signal.SIGKILL if force else signal.SIGTERM)
    except ProcessLookupError:
        pass


# ───────────── Windows 窗口（不抢前台） ─────────────

def _user32():
    import ctypes
    from ctypes import wintypes
    u = ctypes.windll.user32
    u.GetForegroundWindow.restype = wintypes.HWND
    u.GetWindowThreadProcessId.argtypes = [wintypes.HWND, ctypes.POINTER(wintypes.DWORD)]
    u.SetWindowPos.argtypes = [wintypes.HWND, wintypes.HWND, ctypes.c_int, ctypes.c_int, ctypes.c_int,
                               ctypes.c_int, ctypes.c_uint]
    u.SetForegroundWindow.argtypes = [wintypes.HWND]
    u.IsWindow.argtypes = [wintypes.HWND]
    u.IsWindowVisible.argtypes = [wintypes.HWND]
    u.GetWindowTextW.argtypes = [wintypes.HWND, wintypes.LPWSTR, ctypes.c_int]
    return ctypes, wintypes, u


def win_foreground():
    """当前前台窗口句柄（非 Windows 返回 None）。"""
    if not IS_WINDOWS:
        return None
    _, _, u = _user32()
    return u.GetForegroundWindow() or None


def win_window_pid(hwnd) -> int:
    ctypes, wintypes, u = _user32()
    pid = wintypes.DWORD()
    u.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
    return pid.value


def win_title(hwnd) -> str:
    if not IS_WINDOWS or not hwnd:
        return ""
    ctypes, _, u = _user32()
    buf = ctypes.create_unicode_buffer(256)
    u.GetWindowTextW(hwnd, buf, 256)
    return buf.value


def win_windows_of(pids) -> list:
    """属于这些进程的可见顶层窗口。"""
    if not IS_WINDOWS:
        return []
    ctypes, wintypes, u = _user32()
    want = {int(p) for p in pids}
    found = []
    proto = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)

    def cb(hwnd, _):
        if u.IsWindowVisible(hwnd) and win_window_pid(hwnd) in want:
            found.append(hwnd)
        return True

    u.EnumWindows(proto(cb), 0)
    return found


def win_send_to_back(pids) -> bool:
    """把游戏窗口压到所有窗口下面，不激活、不最小化（最小化后不渲染，截图会失败）。"""
    if not IS_WINDOWS:
        return False
    _, _, u = _user32()
    moved = False
    for hwnd in win_windows_of(pids):
        # HWND_BOTTOM=1；SWP_NOSIZE|SWP_NOMOVE|SWP_NOACTIVATE = 0x13
        moved = bool(u.SetWindowPos(hwnd, 1, 0, 0, 0, 0, 0x13)) or moved
    return moved


def win_give_back(prev, pids) -> bool:
    """游戏窗口抢了前台时，把焦点还给 prev。Windows 限制后台进程设前台，先模拟一下 Alt 键解锁。"""
    if not IS_WINDOWS or not prev:
        return False
    _, _, u = _user32()
    fg = u.GetForegroundWindow()
    if not fg or fg == prev or not u.IsWindow(prev) or win_window_pid(fg) not in {int(p) for p in pids}:
        return False
    u.keybd_event(0x12, 0, 0, 0)  # VK_MENU 按下
    u.keybd_event(0x12, 0, 2, 0)  # KEYEVENTF_KEYUP
    return bool(u.SetForegroundWindow(prev))


if __name__ == "__main__":
    print(json.dumps({"windows": IS_WINDOWS, "java": java_processes()[:5]}, ensure_ascii=False, indent=1))
    sys.exit(0)
