"""最小 NBT 写出器（T13b）：只写结构模板要用到的类型，输出 gzip 压缩的二进制 NBT。

用法：值用带类型的包装表示——
  Int(3)、String("a")、List(TAG_INT, [Int(1), ...])、Compound({"k": Int(1)})、Double(0.5)、Float(0.0)、Byte(1)、Long(5)
"""
from __future__ import annotations

import gzip
import struct
from dataclasses import dataclass

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_STRING, TAG_LIST, TAG_COMPOUND = 8, 9, 10


@dataclass(frozen=True)
class Byte:
    v: int
    tag = TAG_BYTE


@dataclass(frozen=True)
class Int:
    v: int
    tag = TAG_INT


@dataclass(frozen=True)
class Long:
    v: int
    tag = TAG_LONG


@dataclass(frozen=True)
class Float:
    v: float
    tag = TAG_FLOAT


@dataclass(frozen=True)
class Double:
    v: float
    tag = TAG_DOUBLE


@dataclass(frozen=True)
class String:
    v: str
    tag = TAG_STRING


@dataclass
class List:
    elem: int
    items: list
    tag = TAG_LIST


@dataclass
class Compound:
    items: dict
    tag = TAG_COMPOUND


def _str(s: str) -> bytes:
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _payload(v) -> bytes:
    t = v.tag
    if t == TAG_BYTE:
        return struct.pack(">b", v.v)
    if t == TAG_INT:
        return struct.pack(">i", v.v)
    if t == TAG_LONG:
        return struct.pack(">q", v.v)
    if t == TAG_FLOAT:
        return struct.pack(">f", v.v)
    if t == TAG_DOUBLE:
        return struct.pack(">d", v.v)
    if t == TAG_STRING:
        return _str(v.v)
    if t == TAG_LIST:
        out = struct.pack(">bi", v.elem if v.items else TAG_END, len(v.items))
        for it in v.items:
            if it.tag != v.elem:
                raise ValueError(f"列表元素类型不一致：{it.tag} != {v.elem}")
            out += _payload(it)
        return out
    if t == TAG_COMPOUND:
        out = b""
        for k, it in v.items.items():
            out += struct.pack(">b", it.tag) + _str(k) + _payload(it)
        return out + struct.pack(">b", TAG_END)
    raise ValueError(f"不支持的类型 {t}")


def dumps(root: Compound, name: str = "") -> bytes:
    """根标签（未压缩）。"""
    return struct.pack(">b", TAG_COMPOUND) + _str(name) + _payload(root)


def write_gzip(path, root: Compound):
    path.parent.mkdir(parents=True, exist_ok=True)
    # mtime 固定为 0：同一体素每次导出的文件逐字节相同，git 不会因为重新导出而出现无意义的改动
    with open(path, "wb") as f:
        with gzip.GzipFile(fileobj=f, mode="wb", mtime=0) as g:
            g.write(dumps(root))


# ───────────── 读取（只用于自检） ─────────────

def loads(data: bytes):
    """读回成普通 Python 值（dict / list / 数字 / 字符串），用于单元测试与导出自检。"""
    pos = 0

    def take(n):
        nonlocal pos
        b = data[pos:pos + n]
        pos += n
        return b

    def rstr():
        (n,) = struct.unpack(">H", take(2))
        return take(n).decode("utf-8")

    def payload(t):
        if t == TAG_BYTE:
            return struct.unpack(">b", take(1))[0]
        if t == TAG_SHORT:
            return struct.unpack(">h", take(2))[0]
        if t == TAG_INT:
            return struct.unpack(">i", take(4))[0]
        if t == TAG_LONG:
            return struct.unpack(">q", take(8))[0]
        if t == TAG_FLOAT:
            return struct.unpack(">f", take(4))[0]
        if t == TAG_DOUBLE:
            return struct.unpack(">d", take(8))[0]
        if t == TAG_STRING:
            return rstr()
        if t == TAG_LIST:
            et, n = struct.unpack(">bi", take(5))
            return [payload(et) for _ in range(n)]
        if t == TAG_COMPOUND:
            out = {}
            while True:
                (ct,) = struct.unpack(">b", take(1))
                if ct == TAG_END:
                    return out
                k = rstr()
                out[k] = payload(ct)
        raise ValueError(f"不支持的类型 {t}")

    (t,) = struct.unpack(">b", take(1))
    rstr()
    return payload(t)
