package io.github.happysnappingturtle.mythicage.agent.remote;

import io.github.happysnappingturtle.mythicage.agent.ScenarioStep;
import net.minecraft.client.Minecraft;

/** walk 指令：朝目标直线行走，不寻路，卡住时跳一下；隔墙会超时返回 reached=false。 */
final class WalkStep implements ScenarioStep {
    private static final double ARRIVE_DISTANCE = 0.6;
    private static final double STUCK_THRESHOLD = 0.01;
    private static final int STUCK_TICKS_BEFORE_JUMP = 10;

    private final double toX;
    private final double toZ;
    private final int timeoutTicks;
    private int elapsedTicks;
    private int stuckTicks;
    private double lastX;
    private double lastZ;
    private boolean started;
    private boolean reached;
    private double finalX;
    private double finalY;
    private double finalZ;

    WalkStep(double toX, double toZ, int timeoutTicks) {
        this.toX = toX;
        this.toZ = toZ;
        this.timeoutTicks = timeoutTicks;
    }

    boolean reached() {
        return reached;
    }

    double[] finalPos() {
        return new double[]{finalX, finalY, finalZ};
    }

    int usedTicks() {
        return elapsedTicks;
    }

    @Override
    public boolean tick(Minecraft mc) {
        if (mc.player == null) {
            return true;
        }
        if (!started) {
            started = true;
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
        }
        double dx = toX - mc.player.getX();
        double dz = toZ - mc.player.getZ();
        if (Math.hypot(dx, dz) < ARRIVE_DISTANCE) {
            finish(mc, true);
            return true;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        mc.player.setYRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.yHeadRot = yaw;
        mc.player.yBodyRot = yaw;
        mc.options.keyUp.setDown(true);

        double moved = Math.hypot(mc.player.getX() - lastX, mc.player.getZ() - lastZ);
        if (moved < STUCK_THRESHOLD) {
            stuckTicks++;
            mc.options.keyJump.setDown(stuckTicks == STUCK_TICKS_BEFORE_JUMP);
        } else {
            stuckTicks = 0;
            mc.options.keyJump.setDown(false);
        }
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
        if (++elapsedTicks > timeoutTicks) {
            finish(mc, false);
            return true;
        }
        return false;
    }

    private void finish(Minecraft mc, boolean ok) {
        mc.options.keyUp.setDown(false);
        mc.options.keyJump.setDown(false);
        reached = ok;
        finalX = mc.player.getX();
        finalY = mc.player.getY();
        finalZ = mc.player.getZ();
    }
}
