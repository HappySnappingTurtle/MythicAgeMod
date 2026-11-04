#!/usr/bin/env python3
"""专服实测用的常驻专服（T24 设计文档 §10.2）。

  python3 tools/ma_server.py start [--world ma_srv] [--seed 20261013]   后台启动，等到 Done
  python3 tools/ma_server.py cmd '<控制台命令>' ['<命令>' ...]         经 RCON 执行并打印回应
  python3 tools/ma_server.py stop                                       发 stop、等退出、恢复 server.properties

server.properties 的原样备份在 neoforge/runs/server/server.properties.ma_server_backup，stop 时恢复。
遥控客户端用 `python3 tools/ma_remote.py start --server localhost:25599` 连进来。
"""
from __future__ import annotations

import argparse
import json
import os
import re
import secrets
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import server_smoke as ss  # noqa: E402

STATE = ss.RUN_DIR / "ma_server.json"
BACKUP = ss.RUN_DIR / "server.properties.ma_server_backup"
OUT = ss.RUN_DIR / "logs/ma_server_gradle.log"


def start(a) -> int:
    if STATE.exists() or BACKUP.exists():
        print("专服已在运行（有 ma_server.json）；先 stop")
        return 1
    if not re.fullmatch(r"[A-Za-z0-9_-]+", a.world):
        raise ValueError("测试存档名只能包含字母、数字、下划线和连字符")
    ss.ensure_ports_free()
    ss.RUN_DIR.mkdir(parents=True, exist_ok=True)
    existed = ss.PROPS.exists()
    original = ss.PROPS.read_bytes() if existed else b""
    BACKUP.write_bytes(original)
    BACKUP.chmod(0o600)
    password = secrets.token_hex(8)
    ss.PROPS.write_text(ss.patch_properties(original.decode("utf-8"),
        ss.local_properties(a.world, a.seed, password)), encoding="utf-8")
    if ss.LOG.exists():
        ss.LOG.unlink()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    proc = None
    try:
        with open(OUT, "w") as log:
            proc = subprocess.Popen(["./gradlew", ":neoforge:runServer", "--offline", "--console=plain"],
                                    cwd=ss.ROOT, stdin=subprocess.DEVNULL,
                                    stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        with os.fdopen(os.open(STATE, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as state:
            json.dump({"pid": proc.pid, "password": password, "world": a.world, "properties_existed": existed}, state)
        if not ss.wait_for("Done (", a.timeout, proc) or not ss.wait_for("RCON running", 60, proc):
            raise RuntimeError(f"没有在时限内启动完成，见 {OUT}")
    except (OSError, RuntimeError) as exc:
        if proc is not None:
            ss.stop_process(proc, timeout=0)
        restore_properties(existed)
        STATE.unlink(missing_ok=True)
        print(exc)
        return 1
    print(f"专服就绪：存档 {a.world}，端口 25599")
    return 0


def restore_properties(existed: bool) -> None:
    if BACKUP.exists():
        if existed:
            ss.PROPS.write_bytes(BACKUP.read_bytes())
        else:
            ss.PROPS.unlink(missing_ok=True)
        BACKUP.unlink()


def group_alive(pid: int) -> bool:
    """忽略已结束但尚未回收的僵尸；同时检查 Gradle 拉起的 Minecraft 子进程。"""
    rows = subprocess.run(["ps", "-axo", "pgid=,stat="], capture_output=True, text=True, check=True).stdout
    return any(parts[0] == str(pid) and not parts[1].startswith("Z")
               for row in rows.splitlines() if len(parts := row.split()) >= 2)


def cmd(a) -> int:
    st = json.loads(STATE.read_text(encoding="utf-8"))
    r = ss.Rcon(ss.RCON_PORT, st["password"])
    try:
        for c in a.commands:
            print(f"> {c}\n{r.command(c).strip()}")
    finally:
        r.close()
    return 0


def stop(a) -> int:
    if not STATE.exists():
        print("没有在运行的专服")
        return 0
    st = json.loads(STATE.read_text(encoding="utf-8"))
    r = None
    try:
        r = ss.Rcon(ss.RCON_PORT, st["password"])
        r.command("stop")
    except OSError as e:
        print("RCON 不通：", e)
    finally:
        if r is not None:
            r.close()
    forced = False
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        if not group_alive(st["pid"]) and not ss.server_pids():
            break
        time.sleep(2)
    else:
        forced = True
        for sig in (signal.SIGTERM, signal.SIGKILL):
            for pid in ss.server_pids():
                try:
                    os.kill(pid, sig)
                except ProcessLookupError:
                    pass
            try:
                os.killpg(st["pid"], sig)
            except ProcessLookupError:
                pass
            time.sleep(2)
            if not group_alive(st["pid"]) and not ss.server_pids():
                break
    if group_alive(st["pid"]) or ss.server_pids():
        print("进程仍在运行，保留状态与备份，不能宣称停止成功")
        return 1
    restore_properties(st.get("properties_existed", True))
    STATE.unlink()
    print("专服已停止，server.properties 已恢复")
    return 1 if forced else 0


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Mythic Age 常驻专服")
    sub = ap.add_subparsers(dest="action", required=True)
    s = sub.add_parser("start")
    s.add_argument("--world", default="ma_srv")
    s.add_argument("--seed", default="20261013")
    s.add_argument("--timeout", type=int, default=600)
    c = sub.add_parser("cmd")
    c.add_argument("commands", nargs="+")
    sub.add_parser("stop")
    a = ap.parse_args(argv)
    return {"start": start, "cmd": cmd, "stop": stop}[a.action](a)


if __name__ == "__main__":
    sys.exit(main())
