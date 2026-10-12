package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.entity.PegasusEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class PegasusFlightGoal extends Goal {

    private final PegasusEntity pegasus;
    private Vec3d circleCenter;
    private double circleAngle;
    private double targetAltitude;
    private int ticksInPhase;
    private Phase phase;

    private static final double CIRCLE_RADIUS = 12.0D;
    private static final double CLIMB_SPEED = 0.22D;
    private static final double CRUISE_SPEED = 0.26D;
    private static final double DESCEND_SPEED = 0.18D;
    private static final double ORBIT_ANGULAR_SPEED = 0.035D;
    private static final int CLIMB_DURATION = 60;
    private static final int CRUISE_MIN_DURATION = 200;
    private static final int DESCEND_DURATION = 80;

    private enum Phase { CLIMB, CRUISE, DESCEND }

    public PegasusFlightGoal(PegasusEntity pegasus) {
        this.pegasus = pegasus;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (pegasus.hasPassengers()) return false;
        if (!pegasus.isOnGround() && pegasus.isFlying()) return true;
        return pegasus.getRandom().nextFloat() < 0.005F;
    }

    @Override
    public void start() {
        this.circleCenter = pegasus.getPos();
        this.circleAngle = pegasus.getRandom().nextDouble() * Math.PI * 2.0D;
        this.targetAltitude = pegasus.getY() + 8.0D + pegasus.getRandom().nextDouble() * 10.0D;
        this.targetAltitude = MathHelper.clamp(this.targetAltitude,
                pegasus.getWorld().getBottomY() + 6.0D,
                pegasus.getWorld().getTopY() - 10.0D);
        this.phase = Phase.CLIMB;
        this.ticksInPhase = 0;
    }

    @Override
    public boolean shouldContinue() {
        if (pegasus.hasPassengers()) return false;
        if (phase == Phase.DESCEND && pegasus.isOnGround()) return false;
        return pegasus.isAlive();
    }

    @Override
    public void tick() {
        ticksInPhase++;

        switch (phase) {
            case CLIMB -> tickClimb();
            case CRUISE -> tickCruise();
            case DESCEND -> tickDescend();
        }
    }

    private void tickClimb() {
        double dy = targetAltitude - pegasus.getY();
        double verticalSpeed = Math.min(CLIMB_SPEED, Math.max(0.04D, dy * 0.08D));

        circleAngle += ORBIT_ANGULAR_SPEED * 0.6D;
        double tx = circleCenter.x + Math.cos(circleAngle) * CIRCLE_RADIUS * 0.5D;
        double tz = circleCenter.z + Math.sin(circleAngle) * CIRCLE_RADIUS * 0.5D;

        Vec3d toTarget = new Vec3d(tx - pegasus.getX(), verticalSpeed, tz - pegasus.getZ());
        Vec3d desired = toTarget.lengthSquared() > 0.0001D ? toTarget.normalize().multiply(CLIMB_SPEED) : Vec3d.ZERO;
        Vec3d velocity = pegasus.getVelocity().multiply(0.82D).add(desired.multiply(0.28D));
        pegasus.setVelocity(velocity.x, verticalSpeed, velocity.z);
        faceVelocity(velocity);

        if (ticksInPhase >= CLIMB_DURATION || Math.abs(dy) < 1.5D) {
            transitionTo(Phase.CRUISE);
        }
    }

    private void tickCruise() {
        circleAngle += ORBIT_ANGULAR_SPEED;
        double tx = circleCenter.x + Math.cos(circleAngle) * CIRCLE_RADIUS;
        double tz = circleCenter.z + Math.sin(circleAngle) * CIRCLE_RADIUS;

        double dy = targetAltitude - pegasus.getY();
        double vy = MathHelper.clamp(dy * 0.05D, -0.08D, 0.08D);
        vy += MathHelper.sin(pegasus.age * 0.08F) * 0.012D;

        Vec3d toTarget = new Vec3d(tx - pegasus.getX(), 0, tz - pegasus.getZ());
        Vec3d desired = toTarget.lengthSquared() > 0.0001D ? toTarget.normalize().multiply(CRUISE_SPEED) : Vec3d.ZERO;
        Vec3d velocity = pegasus.getVelocity().multiply(0.85D).add(desired.multiply(0.22D));
        pegasus.setVelocity(velocity.x, vy, velocity.z);
        faceVelocity(velocity);

        if (ticksInPhase >= CRUISE_MIN_DURATION && pegasus.getRandom().nextFloat() < 0.008F) {
            if (pegasus.getRandom().nextFloat() < 0.4F) {
                rechooseOrbit();
                return;
            }
            transitionTo(Phase.DESCEND);
        }
    }

    private void tickDescend() {
        circleAngle += ORBIT_ANGULAR_SPEED * 0.8D;
        double shrinkFactor = Math.max(0.3D, 1.0D - (double) ticksInPhase / DESCEND_DURATION);
        double tx = circleCenter.x + Math.cos(circleAngle) * CIRCLE_RADIUS * shrinkFactor;
        double tz = circleCenter.z + Math.sin(circleAngle) * CIRCLE_RADIUS * shrinkFactor;

        double vy = -DESCEND_SPEED * 0.6D;

        Vec3d toTarget = new Vec3d(tx - pegasus.getX(), 0, tz - pegasus.getZ());
        Vec3d desired = toTarget.lengthSquared() > 0.0001D ? toTarget.normalize().multiply(DESCEND_SPEED) : Vec3d.ZERO;
        Vec3d velocity = pegasus.getVelocity().multiply(0.84D).add(desired.multiply(0.24D));
        pegasus.setVelocity(velocity.x, vy, velocity.z);
        faceVelocity(velocity);
    }

    private void rechooseOrbit() {
        this.circleCenter = pegasus.getPos();
        this.targetAltitude = pegasus.getY() + (pegasus.getRandom().nextDouble() - 0.3D) * 6.0D;
        this.targetAltitude = MathHelper.clamp(this.targetAltitude,
                pegasus.getWorld().getBottomY() + 6.0D,
                pegasus.getWorld().getTopY() - 10.0D);
        this.ticksInPhase = 0;
    }

    private void transitionTo(Phase next) {
        this.phase = next;
        this.ticksInPhase = 0;
    }

    private void faceVelocity(Vec3d velocity) {
        if (velocity.horizontalLengthSquared() > 0.0001D) {
            float yaw = (float) (MathHelper.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
            pegasus.setYaw(yaw);
            pegasus.setBodyYaw(yaw);
            pegasus.headYaw = yaw;
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}
