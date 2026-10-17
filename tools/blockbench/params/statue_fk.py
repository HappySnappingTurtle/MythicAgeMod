"""神像参数用的简易正向运动学（卡片坐标：+X 角色左、+Y 上、+Z 前；右手定则）。

骨骼静止旋转按 Blockbench / GeckoLib 的顺序：先绕 X、再 Y、再 Z（R = Rz·Ry·Rx），绕骨骼枢轴。
用来在出参数时算出手、握孔方向落在哪里，从而把权杖、雷霆摆到正确位置（检查清单 §三：武器只随手走）。
"""
import math


def _rx(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0], [0, c, -s], [0, s, c]]


def _ry(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s], [0, 1, 0], [-s, 0, c]]


def _rz(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def _mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def _mv(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def rot(deg):
    rx, ry, rz = (math.radians(d) for d in deg)
    return _mm(_rz(rz), _mm(_ry(ry), _rx(rx)))


class Skeleton:
    def __init__(self, bones):
        """bones: [(id, parent, pivot, rotation_deg)]"""
        self.b = {bid: (parent, pivot, r) for bid, parent, pivot, r in bones}

    def chain(self, bone):
        out = []
        while bone:
            out.append(bone)
            bone = self.b[bone][0]
        return out  # 子 → 根

    def point(self, bone, p):
        """静止坐标中挂在 bone 上的点 → 摆好姿势后的坐标。"""
        p = list(p)
        for bid in self.chain(bone):
            _, pivot, r = self.b[bid]
            m = rot(r)
            d = [p[i] - pivot[i] for i in range(3)]
            d = _mv(m, d)
            p = [pivot[i] + d[i] for i in range(3)]
        return p

    def direction(self, bone, v):
        """静止坐标中挂在 bone 上的方向 → 摆好姿势后的方向。"""
        v = list(v)
        for bid in self.chain(bone):
            v = _mv(rot(self.b[bid][2]), v)
        return v


def fmt(v):
    return "(" + ", ".join(f"{x:.2f}" for x in v) + ")"
