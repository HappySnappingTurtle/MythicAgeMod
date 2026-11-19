package io.github.happysnappingturtle.mythicage.core.lair;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * 嵌进坡面的巢穴选址（五文明通用，T25a §5.2）：洞口朝外、洞身挖进山体。纯函数，采样由平台层做。
 * <p>
 * 采样沿洞的纵轴分三排：洞口一排（{@code front}）、洞身中段一排（{@code mid}）、洞底一排（{@code back}），每排在洞宽上取 3 点，
 * 值是「地面上第一个空格的 y」。合格条件：
 * <ol>
 *   <li>洞口一排的起伏 ≤ {@value #MAX_FRONT_RELIEF}，且洞口不在水里、地面高于海平面；</li>
 *   <li>洞底一排最低处 ≥ 地面 + 洞高 + {@value #ROOF_COVER}（洞底埋在山里；山不够厚时洞顶的岩壳露出来，像一块岩丘）；</li>
 *   <li>洞身中段最低处 ≥ 地面 + 洞高 − {@value #MID_SLACK}（中段洞顶大体被山盖住；露出来的那点由岩壳补成岩拱）。</li>
 * </ol>
 * 地面取洞口一排的中位数。合格的候选里取「洞底比地面高得最多」的（山最厚、最不容易露出人造岩壳）。
 */
public final class LairSiteRules {
    public static final int MAX_FRONT_RELIEF = 3;
    public static final int ROOF_COVER = 0;
    public static final int MID_SLACK = 3;

    private LairSiteRules() {
    }

    /** 合格就返回洞口地面的高度（地面上第一层的 y），否则为空。 */
    public static OptionalInt floor(int[] front, int[] mid, int[] back, int caveHeight, boolean frontWet, int seaLevel) {
        OptionalInt f = frontFloor(front, frontWet, seaLevel);
        if (f.isEmpty() || !backOk(back, f.getAsInt(), caveHeight) || !midOk(mid, f.getAsInt(), caveHeight)) {
            return OptionalInt.empty();
        }
        return f;
    }

    /** 第一步（只看洞口一排，平台层据此提前放弃，省下另外两排的采样）：洞口平、不在水里、高于海平面。 */
    public static OptionalInt frontFloor(int[] front, boolean frontWet, int seaLevel) {
        if (front.length == 0 || frontWet || max(front) - min(front) > MAX_FRONT_RELIEF) {
            return OptionalInt.empty();
        }
        int[] f = front.clone();
        Arrays.sort(f);
        int floor = f[f.length / 2];
        return floor > seaLevel ? OptionalInt.of(floor) : OptionalInt.empty();
    }

    /** 第二步：洞底埋在山里（山顶不低于洞顶 + {@value #ROOF_COVER}）。 */
    public static boolean backOk(int[] back, int floor, int caveHeight) {
        return back.length > 0 && min(back) >= floor + caveHeight + ROOF_COVER;
    }

    /** 第三步：洞身中段大体被山盖住。 */
    public static boolean midOk(int[] mid, int floor, int caveHeight) {
        return mid.length > 0 && min(mid) >= floor + caveHeight - MID_SLACK;
    }

    /** 候选的好坏：洞底一排比地面高出多少（越大越好）。 */
    public static int score(int[] back, int floor) {
        return min(back) - floor;
    }

    private static int min(int[] a) {
        int m = Integer.MAX_VALUE;
        for (int v : a) {
            m = Math.min(m, v);
        }
        return m;
    }

    private static int max(int[] a) {
        int m = Integer.MIN_VALUE;
        for (int v : a) {
            m = Math.max(m, v);
        }
        return m;
    }
}
