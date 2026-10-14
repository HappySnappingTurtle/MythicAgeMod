package io.github.happysnappingturtle.mythicage.agent.remote;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;

/** 一条指令的执行体：可跨多个 tick，tick 返回 true 后 ok/error/data 才有意义。 */
public interface RemoteExecution {
    boolean tick(Minecraft mc);

    boolean ok();

    String error();

    JsonElement data();
}
