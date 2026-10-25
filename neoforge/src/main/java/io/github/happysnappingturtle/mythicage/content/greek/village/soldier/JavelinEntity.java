package io.github.happysnappingturtle.mythicage.content.greek.village.soldier;

import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierTargetRules;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierAmmo;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierTargets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻装投矛兵的投矛（T20 设计文档 §9.2）：抛物线飞行；每个目标只打一次；只打士兵能打的（飞过居民身上不伤人）；
 * 打中或撞到方块就落地插着，最多留 {@code lifetime} 刻。落在村庄已建范围外的直接损耗；玩家捡不起来。
 * 每村同时最多 {@link #VILLAGE_CAP} 支在飞或插地，多出的最早那支直接计损耗（§13）。
 */
public class JavelinEntity extends Projectile implements ItemSupplier, SoldierAmmo {
    public static final int VILLAGE_CAP = 12;
    private static final EntityDataAccessor<String> ITEM = SynchedEntityData.defineId(JavelinEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(JavelinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Map<UUID, Deque<UUID>> BY_VILLAGE = new ConcurrentHashMap<>();

    private UUID village;
    private float damage = 5;
    private double gravity = 0.05;
    private double waterDrag = 0.8;
    private int lifetime = 1200;
    private int age;
    private int landedAge;
    private final Set<UUID> hit = new HashSet<>();

    public JavelinEntity(EntityType<? extends JavelinEntity> type, Level level) {
        super(type, level);
    }

    public void configure(UUID village, String item, float damage, double gravity, double waterDrag, int lifetime) {
        this.village = village;
        entityData.set(ITEM, item);
        this.damage = damage;
        this.gravity = gravity;
        this.waterDrag = waterDrag;
        this.lifetime = lifetime;
        Deque<UUID> q = BY_VILLAGE.computeIfAbsent(village, k -> new ArrayDeque<>());
        q.addLast(getUUID());
        while (q.size() > VILLAGE_CAP && level() instanceof ServerLevel sl) {
            Entity old = sl.getEntity(q.pollFirst());
            if (old != null) {
                old.discard();
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, "mythic_age:javelin");
        builder.define(LANDED, false);
    }

    @Override
    public UUID ammoVillage() {
        return village;
    }

    @Override
    public String ammoItem() {
        return entityData.get(ITEM);
    }

    @Override
    public boolean landed() {
        return entityData.get(LANDED);
    }

    public float damage() {
        return damage;
    }

    /** 已命中的目标（GameTest 读）。 */
    public Set<UUID> hits() {
        return Set.copyOf(hit);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(ammoItem())));
    }

    @Override
    public void tick() {
        super.tick();
        if (landed()) {
            if (!level().isClientSide && ++landedAge > lifetime) {
                discard();
            }
            return;
        }
        Vec3 v = getDeltaMovement();
        Vec3 from = position();
        Vec3 to = from.add(v);
        if (!level().isClientSide) {
            if (++age > 200) {
                land();
                return;
            }
            HitResult block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (block.getType() != HitResult.Type.MISS) {
                to = block.getLocation();
            }
            Entity owner = getOwner();
            AABB swept = getBoundingBox().expandTowards(to.subtract(from)).inflate(0.3);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, swept,
                    e -> e.isAlive() && e != owner && !hit.contains(e.getUUID()))) {
                if (!canStrike(owner, e)) {
                    continue;
                }
                hit.add(e.getUUID());
                e.hurt(damageSources().thrown(this, owner), damage);
                playSound(SoundEvents.TRIDENT_HIT, 0.8F, 1.2F);
                setPos(e.getX(), e.getY(), e.getZ());
                land();
                return;
            }
            if (block.getType() != HitResult.Type.MISS) {
                setPos(to.x, to.y, to.z);
                playSound(SoundEvents.TRIDENT_HIT_GROUND, 0.7F, 1.1F);
                land();
                return;
            }
        }
        setPos(to.x, to.y, to.z);
        double drag = isInWater() ? waterDrag : 0.99;
        setDeltaMovement(v.x * drag, v.y * drag - gravity, v.z * drag);
        double flat = v.horizontalDistance();
        setYRot((float) Math.toDegrees(Math.atan2(v.x, v.z)));
        setXRot((float) Math.toDegrees(Math.atan2(v.y, flat)));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    /** 只打士兵能打的（接敌那一类）；不是士兵投的就谁都不打。 */
    private boolean canStrike(Entity owner, LivingEntity e) {
        if (!(owner instanceof ResidentEntity s) || village == null) {
            return false;
        }
        return SoldierTargetRules.decide(SoldierTargets.facts(s, e, village, 12, level().getGameTime()))
                == SoldierTargetRules.Decision.ENGAGE;
    }

    /** 落地插着；落在村外的直接损耗。 */
    private void land() {
        setDeltaMovement(Vec3.ZERO);
        entityData.set(LANDED, true);
        if (level() instanceof ServerLevel sl && (village == null || !SoldierTargets.inVillage(sl, blockPosition(), village))) {
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (village != null && reason.shouldDestroy()) {
            Deque<UUID> q = BY_VILLAGE.get(village);
            if (q != null) {
                q.remove(getUUID());
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void playerTouch(Player player) {
        // 玩家捡不起来（不能拿去刷）
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (village != null) {
            tag.putUUID("Village", village);
        }
        tag.putString("Item", ammoItem());
        tag.putBoolean("Landed", landed());
        tag.putFloat("Damage", damage);
        tag.putDouble("Gravity", gravity);
        tag.putDouble("WaterDrag", waterDrag);
        tag.putInt("Lifetime", lifetime);
        tag.putInt("Age", age);
        tag.putInt("LandedAge", landedAge);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        village = tag.hasUUID("Village") ? tag.getUUID("Village") : null;
        entityData.set(ITEM, tag.getString("Item"));
        entityData.set(LANDED, tag.getBoolean("Landed"));
        damage = tag.getFloat("Damage");
        gravity = tag.getDouble("Gravity");
        waterDrag = tag.getDouble("WaterDrag");
        lifetime = tag.getInt("Lifetime");
        age = tag.getInt("Age");
        landedAge = tag.getInt("LandedAge");
    }

    public static void clearForTest(UUID village) {
        BY_VILLAGE.remove(village);
    }
}
