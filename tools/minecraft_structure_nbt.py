#!/usr/bin/env python3
"""Minimal Minecraft Java NBT helpers for structure template files."""

from __future__ import annotations

import gzip
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Any


TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


@dataclass(frozen=True)
class NbtTag:
    tag_id: int
    value: Any


def byte_tag(value: int) -> NbtTag:
    return NbtTag(TAG_BYTE, value)


def int_tag(value: int) -> NbtTag:
    return NbtTag(TAG_INT, value)


def string_tag(value: str) -> NbtTag:
    return NbtTag(TAG_STRING, value)


def list_tag(element_type: int, values: list[Any]) -> NbtTag:
    return NbtTag(TAG_LIST, (element_type, values))


def compound_tag(value: dict[str, NbtTag]) -> NbtTag:
    return NbtTag(TAG_COMPOUND, value)


def int_list(values: list[int]) -> NbtTag:
    return list_tag(TAG_INT, values)


def compound_list(values: list[dict[str, NbtTag]]) -> NbtTag:
    return list_tag(TAG_COMPOUND, values)


def empty_compound() -> NbtTag:
    return compound_tag({})


def _write_short(value: int) -> bytes:
    return struct.pack(">h", value)


def _write_int(value: int) -> bytes:
    return struct.pack(">i", value)


def _write_long(value: int) -> bytes:
    return struct.pack(">q", value)


def _write_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    if len(encoded) > 32767:
        raise ValueError("NBT string is too long")
    return _write_short(len(encoded)) + encoded


def _write_payload(tag_id: int, value: Any) -> bytes:
    if tag_id == TAG_BYTE:
        return struct.pack(">b", value)
    if tag_id == TAG_SHORT:
        return _write_short(value)
    if tag_id == TAG_INT:
        return _write_int(value)
    if tag_id == TAG_LONG:
        return _write_long(value)
    if tag_id == TAG_STRING:
        return _write_string(value)
    if tag_id == TAG_LIST:
        element_type, values = value
        payload = bytes([element_type]) + _write_int(len(values))
        for item in values:
            payload += _write_payload(element_type, item)
        return payload
    if tag_id == TAG_COMPOUND:
        payload = b""
        for name, tag in value.items():
            payload += bytes([tag.tag_id])
            payload += _write_string(name)
            payload += _write_payload(tag.tag_id, tag.value)
        return payload + bytes([TAG_END])
    if tag_id == TAG_INT_ARRAY:
        payload = _write_int(len(value))
        for item in value:
            payload += _write_int(item)
        return payload
    raise NotImplementedError(f"NBT write tag {tag_id} is not implemented")


def write_root_compound(path: Path, root: dict[str, NbtTag], root_name: str = "") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = bytes([TAG_COMPOUND]) + _write_string(root_name) + _write_payload(TAG_COMPOUND, root)
    with gzip.open(path, "wb") as handle:
        handle.write(raw)


class _NbtReader:
    def __init__(self, data: bytes) -> None:
        self.data = data
        self.offset = 0

    def read(self, length: int) -> bytes:
        if self.offset + length > len(self.data):
            raise ValueError("Unexpected end of NBT data")
        chunk = self.data[self.offset:self.offset + length]
        self.offset += length
        return chunk

    def read_byte(self) -> int:
        return struct.unpack(">b", self.read(1))[0]

    def read_unsigned_byte(self) -> int:
        return self.read(1)[0]

    def read_short(self) -> int:
        return struct.unpack(">h", self.read(2))[0]

    def read_int(self) -> int:
        return struct.unpack(">i", self.read(4))[0]

    def read_long(self) -> int:
        return struct.unpack(">q", self.read(8))[0]

    def read_string(self) -> str:
        length = self.read_short()
        return self.read(length).decode("utf-8")

    def read_payload(self, tag_id: int) -> Any:
        if tag_id == TAG_BYTE:
            return self.read_byte()
        if tag_id == TAG_SHORT:
            return self.read_short()
        if tag_id == TAG_INT:
            return self.read_int()
        if tag_id == TAG_LONG:
            return self.read_long()
        if tag_id == TAG_STRING:
            return self.read_string()
        if tag_id == TAG_LIST:
            element_type = self.read_unsigned_byte()
            length = self.read_int()
            return [self.read_payload(element_type) for _ in range(length)]
        if tag_id == TAG_COMPOUND:
            payload: dict[str, Any] = {}
            while True:
                child_id = self.read_unsigned_byte()
                if child_id == TAG_END:
                    break
                name = self.read_string()
                payload[name] = self.read_payload(child_id)
            return payload
        if tag_id == TAG_INT_ARRAY:
            length = self.read_int()
            return [self.read_int() for _ in range(length)]
        if tag_id == TAG_BYTE_ARRAY:
            length = self.read_int()
            return list(self.read(length))
        if tag_id == TAG_LONG_ARRAY:
            length = self.read_int()
            return [self.read_long() for _ in range(length)]
        raise NotImplementedError(f"NBT read tag {tag_id} is not implemented")


def read_root_compound(path: Path) -> dict[str, Any]:
    with gzip.open(path, "rb") as handle:
        data = handle.read()
    reader = _NbtReader(data)
    tag_id = reader.read_unsigned_byte()
    if tag_id != TAG_COMPOUND:
        raise ValueError(f"Root NBT tag must be a compound, got {tag_id}")
    _root_name = reader.read_string()
    return reader.read_payload(TAG_COMPOUND)


def palette_block_names(structure: dict[str, Any]) -> list[str]:
    palette = structure.get("palette", [])
    names: list[str] = []
    for state in palette:
        if isinstance(state, dict) and isinstance(state.get("Name"), str):
            names.append(state["Name"])
    return names


def validate_structure_template(path: Path) -> list[str]:
    errors: list[str] = []
    try:
        structure = read_root_compound(path)
    except Exception as exc:  # noqa: BLE001 - validation should report parse errors.
        return [f"cannot read NBT: {exc}"]

    size = structure.get("size")
    if not isinstance(size, list) or len(size) != 3 or not all(isinstance(value, int) and value > 0 for value in size):
        errors.append("missing or invalid positive size[3]")
    palette = structure.get("palette")
    if not isinstance(palette, list) or not palette:
        errors.append("missing or empty palette")
    blocks = structure.get("blocks")
    if not isinstance(blocks, list) or not blocks:
        errors.append("missing or empty blocks")
    if "entities" not in structure or not isinstance(structure.get("entities"), list):
        errors.append("missing entities list")
    if not isinstance(structure.get("DataVersion"), int):
        errors.append("missing integer DataVersion")
    return errors
