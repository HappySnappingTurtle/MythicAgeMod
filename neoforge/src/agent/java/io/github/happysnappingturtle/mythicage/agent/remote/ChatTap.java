package io.github.happysnappingturtle.mythicage.agent.remote;

import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** 收集系统与玩家聊天（环形缓冲最近 200 条，带遥控口 tick 号），回填到指令结果。 */
public final class ChatTap {
    private record Entry(long tick, String text) {
    }

    private static final int CAPACITY = 200;
    private static final Deque<Entry> BUFFER = new ArrayDeque<>();
    private static volatile long currentTick;

    private ChatTap() {
    }

    static void setCurrentTick(long tick) {
        currentTick = tick;
    }

    public static void onSystem(ClientChatReceivedEvent.System event) {
        record(event.getMessage().getString());
    }

    public static void onPlayer(ClientChatReceivedEvent.Player event) {
        record(event.getMessage().getString());
    }

    private static synchronized void record(String text) {
        BUFFER.addLast(new Entry(currentTick, text));
        while (BUFFER.size() > CAPACITY) {
            BUFFER.removeFirst();
        }
    }

    /** 某条指令开始之后收到的全部聊天行。 */
    static synchronized List<String> since(long sinceTickInclusive) {
        List<String> out = new ArrayList<>();
        for (Entry e : BUFFER) {
            if (e.tick() >= sinceTickInclusive) {
                out.add(e.text());
            }
        }
        return out;
    }

    /** 最近 n 条。 */
    static synchronized List<String> recent(int n) {
        List<String> all = BUFFER.stream().map(Entry::text).toList();
        return all.subList(Math.max(0, all.size() - n), all.size());
    }
}
