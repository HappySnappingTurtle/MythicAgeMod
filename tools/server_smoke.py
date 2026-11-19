#!/usr/bin/env python3
"""专服冒烟（T24 设计文档 §10.1）。

  python3 tools/server_smoke.py [--seed 20261013] [--timeout 600] [--keep-world]
  （Windows 上用 python 代替 python3。）

1. 临时改 neoforge/runs/server/server.properties：新存档名、固定种子、打开 RCON（结束后原样恢复）；
2. 后台跑 ./gradlew（Windows：gradlew.bat）:neoforge:runServer，等日志出现 "Done ("；
3. 经 RCON 发几条控制台命令（配置、定位迷宫、竞技场、村庄、性能采样），把回应写进报告；
4. 发 stop，等进程退出；
5. 检查日志：没有客户端类加载错误、没有 mythic_age 相关 ERROR（允许清单除外）。

退出码 0 = 通过。报告写到 neoforge/runs/server/logs/smoke_report.txt。
"""
from __future__ import annotations

import argparse
import re
import secrets
import shutil
import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import procutil  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RUN_DIR = ROOT / "neoforge/runs/server"
PROPS = RUN_DIR / "server.properties"
LOG = RUN_DIR / "logs/latest.log"
REPORT = RUN_DIR / "logs/smoke_report.txt"
RCON_PORT = 25576
SERVER_PORT = 25599

COMMANDS = [
    "mythicage config show all",
    "locate structure #mythic_age:labyrinth/greek",
    "mythicage arena list",
    "mythicage village list",
    "mythicage dev perf start 10",
]
AFTER_WAIT = ["mythicage dev perf report"]

# 已知无害的报错：村庄 / 神殿 / 迷宫批次生成时原版偶尔打出的高度图提示（T23 记录）
ALLOWED_ERRORS = [
    re.compile(r"Unprimed heightmap"),
]
CLIENT_CLASS = re.compile(r"(Attempted to load class net/minecraft/client|NoClassDefFoundError: net/minecraft/client"
                          r"|RuntimeDistCleaner|invalid dist DEDICATED_SERVER)")
ERROR_LINE = re.compile(r"/(ERROR|FATAL)\]")


# ───────────── 纯函数（有单元测试） ─────────────

def judge(log: str) -> list[str]:
    """从服务器日志里挑出问题；空表 = 通过。"""
    problems: list[str] = []
    if "Done (" not in log:
        problems.append("日志里没有 Done (，服务器没有启动完成")
    for line in log.splitlines():
        if CLIENT_CLASS.search(line):
            problems.append("专服加载了客户端类：" + line.strip()[:200])
            continue
        if ERROR_LINE.search(line) and "mythic" in line.lower():
            if not any(p.search(line) for p in ALLOWED_ERRORS):
                problems.append("模组报错：" + line.strip()[:200])
    return problems


def judge_run(log: str, returncode: int | None, replies: list[str], forced: bool = False) -> list[str]:
    """成功必须包含启动、命令、保存和正常退出，不能只凭 Done 判绿。"""
    problems = judge(log)
    if returncode != 0:
        problems.append(f"服务器进程退出码不是 0：{returncode}")
    if forced:
        problems.append("服务器未正常停止，使用了强制结束")
    if "All dimensions are saved" not in log:
        problems.append("日志里没有所有维度保存完成的记录")
    for reply in replies:
        if not reply.strip() or re.search(r"Unknown or incomplete command|Incorrect argument|<--\[HERE\]|未知或不完整|参数不正确", reply):
            problems.append("控制台命令失败：" + repr(reply[:200]))
    return problems


def local_properties(level: str, seed: str, password: str) -> dict[str, str]:
    """离线测试服务仅监听本机，不把免认证服务器暴露给局域网。"""
    return {"level-name": level, "level-seed": seed, "enable-rcon": "true",
            "server-ip": "127.0.0.1", "server-port": str(SERVER_PORT),
            "rcon.port": str(RCON_PORT), "rcon.password": password, "online-mode": "false",
            "spawn-protection": "0"}


def ensure_ports_free() -> None:
    if server_pids():
        raise RuntimeError("本项目已有专服 JVM（可能尚在启动/保存）；不启动第二个")
    for port in (SERVER_PORT, RCON_PORT):
        with socket.socket() as sock:
            # 正常停止后的 TIME_WAIT 不等于仍有服务器；Minecraft/Netty 重启同样使用地址复用。
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                sock.bind(("127.0.0.1", port))
            except OSError as exc:
                raise RuntimeError(f"端口 {port} 已被占用；不修改正在使用的专服配置") from exc


def is_server_cmdline(cmdline: str, root: Path = ROOT) -> bool:
    """命令行是否为本项目的专服 JVM（路径比较不分 / 与 \\，Windows 上不分大小写）。"""
    marker = procutil.norm(str(Path(root) / "neoforge/build/moddev/serverRunVmArgs.txt"))
    return marker in procutil.norm(cmdline) and "java" in cmdline.lower()


def server_pids() -> list[int]:
    """Gradle daemon 的子进程不一定属于启动器进程组，按本项目专服参数文件精确识别。"""
    return [pid for pid, cmd in procutil.java_processes() if is_server_cmdline(cmd)]


def stop_process(proc: subprocess.Popen, timeout: float = 180) -> bool:
    """等待正常退出；超时时结束整个独立进程组（Windows：进程树），防止遗留 Minecraft 子进程。"""
    try:
        proc.wait(timeout=timeout)
        if not server_pids():
            return False
    except subprocess.TimeoutExpired:
        pass
    for force in (False, True):
        procutil.terminate(server_pids(), force=force)
        if proc.poll() is None:
            procutil.terminate_group(proc.pid, force=force)
        try:
            proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            pass
        deadline = time.monotonic() + 10
        while server_pids() and time.monotonic() < deadline:
            time.sleep(0.2)
        if not server_pids() and proc.poll() is not None:
            break
    return True


def patch_properties(text: str, values: dict[str, str]) -> str:
    """改写（或追加）server.properties 的键，保留其余行与顺序。"""
    out: list[str] = []
    seen: set[str] = set()
    for line in text.splitlines():
        key = line.split("=", 1)[0].strip() if "=" in line and not line.lstrip().startswith("#") else None
        if key in values:
            out.append(f"{key}={values[key]}")
            seen.add(key)
        else:
            out.append(line)
    for k, v in values.items():
        if k not in seen:
            out.append(f"{k}={v}")
    return "\n".join(out) + "\n"


# ───────────── RCON ─────────────

class Rcon:
    def __init__(self, port: int, password: str):
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=30)
        self.req = 0
        if self._send(3, password) == -1:
            raise RuntimeError("RCON 认证失败")

    def _send(self, kind: int, body: str) -> int:
        self.req += 1
        data = struct.pack("<ii", self.req, kind) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(data)) + data)
        self.last = self._read()
        return self.last[0]

    def _read(self) -> tuple[int, str]:
        size = struct.unpack("<i", self._recv(4))[0]
        if not 10 <= size <= 4 * 1024 * 1024:
            raise ValueError(f"RCON 包长度非法：{size}")
        payload = self._recv(size)
        req, _ = struct.unpack("<ii", payload[:8])
        return req, payload[8:-2].decode("utf-8", errors="replace")

    def _recv(self, n: int) -> bytes:
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("RCON 连接断开")
            buf += chunk
        return buf

    def command(self, cmd: str) -> str:
        self._send(2, cmd)
        return self.last[1]

    def close(self):
        self.sock.close()


# ───────────── 主流程 ─────────────

def wait_for(pattern: str, timeout: float, proc: subprocess.Popen) -> bool:
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        if proc.poll() is not None:
            return False
        if LOG.exists() and pattern in LOG.read_text(encoding="utf-8", errors="replace"):
            return True
        time.sleep(2)
    return False


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Mythic Age 专服冒烟")
    ap.add_argument("--seed", default="20261013")
    ap.add_argument("--timeout", type=int, default=600, help="等服务器启动的秒数")
    ap.add_argument("--keep-world", action="store_true", help="保留这次的存档目录")
    a = ap.parse_args(argv)

    if (RUN_DIR / "ma_server.json").exists() or (RUN_DIR / "server.properties.ma_server_backup").exists():
        ap.error("常驻测试专服尚未停止；先运行 ma_server.py stop")
    ensure_ports_free()
    RUN_DIR.mkdir(parents=True, exist_ok=True)
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    level = "smoke_" + time.strftime("%Y%m%d_%H%M%S") + "_" + secrets.token_hex(3)
    password = secrets.token_hex(8)
    backup = PROPS.read_bytes() if PROPS.exists() else None
    PROPS.write_text(patch_properties(backup.decode("utf-8") if backup is not None else "",
                                     local_properties(level, a.seed, password)), encoding="utf-8")
    if LOG.exists():
        LOG.unlink()
    report: list[str] = [f"存档 {level}，种子 {a.seed}"]
    proc = None
    replies: list[str] = []
    problems: list[str] = []
    forced = False
    output = (REPORT.parent / "smoke_gradle.log").open("w", encoding="utf-8")
    t0 = time.time()
    try:
        proc = subprocess.Popen([*procutil.gradlew(ROOT), ":neoforge:runServer", "--offline", "--console=plain"],
                                cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT,
                                **procutil.detached_kwargs())
        if not wait_for("Done (", a.timeout, proc):
            problems.append("服务器没有在时限内启动完成")
        else:
            report.append(f"启动用时 {time.time() - t0:.0f} 秒（含 Gradle）")
            if not wait_for("RCON running", 60, proc):
                raise RuntimeError("RCON 没有在时限内启动完成")
            rcon = Rcon(RCON_PORT, password)
            try:
                for cmd in COMMANDS:
                    reply = rcon.command(cmd).strip()
                    replies.append(reply)
                    report.append(f"> {cmd}\n{reply}")
                time.sleep(12)
                for cmd in AFTER_WAIT:
                    reply = rcon.command(cmd).strip()
                    replies.append(reply)
                    report.append(f"> {cmd}\n{reply}")
                rcon.command("stop")
            finally:
                rcon.close()
    except (OSError, RuntimeError, ValueError) as exc:
        problems.append(f"冒烟执行异常：{exc}")
    finally:
        if proc is not None:
            forced = stop_process(proc, timeout=10 if problems else 180)
        output.close()
        if backup is None:
            PROPS.unlink(missing_ok=True)
        else:
            PROPS.write_bytes(backup)
        if not a.keep_world and not server_pids():
            shutil.rmtree(RUN_DIR / level, ignore_errors=True)
    log = LOG.read_text(encoding="utf-8", errors="replace") if LOG.exists() else ""
    problems.extend(judge_run(log, proc.returncode if proc is not None else None, replies, forced))
    report.append("问题：" + ("无" if not problems else "\n  " + "\n  ".join(problems)))
    REPORT.write_text("\n".join(report) + "\n", encoding="utf-8")
    print("\n".join(report))
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main())
