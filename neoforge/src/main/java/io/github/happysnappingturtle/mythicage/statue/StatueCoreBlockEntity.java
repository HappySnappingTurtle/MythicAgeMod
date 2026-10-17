package io.github.happysnappingturtle.mythicage.statue;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 主神像核心的方块实体（T11a 设计文档 §5.2）：文明、主神，以及放置时记下的结构清单（相对核心的位置 → 应有的方块状态），
 * 用来判断神像是否完整。主人与保护沿用 {@link ShrineBlockEntity}；神庙种类为 {@code <文明>:statue_<主神>}。
 */
public class StatueCoreBlockEntity extends ShrineBlockEntity implements GeoBlockEntity {
    /** 结构里的一格：相对核心的偏移与应有状态。 */
    public record Part(BlockPos offset, BlockState state) {
    }

    private Civilization civ = Civilization.GREEK;
    private String patron = "";
    private final List<Part> parts = new ArrayList<>();
    /** 只在内存：上次检查的结果与缺失格（绝对坐标）。 */
    private boolean intact = true;
    private final List<BlockPos> missing = new ArrayList<>();
    private boolean dirty = true;
    /** GeckoLib 渲染用（T11b：各主神的神像模型；静态，没有动画控制器）。 */
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public StatueCoreBlockEntity(BlockPos pos, BlockState state) {
        super(PatronStatues.CORE_BE.get(), pos, state);
    }

    @Override
    public ShrineKind kind() {
        return PatronStatues.kindFor(civ, patron);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    public Civilization civ() {
        return civ;
    }

    public String patron() {
        return patron;
    }

    public List<Part> parts() {
        return List.copyOf(parts);
    }

    public boolean intact() {
        return intact;
    }

    public List<BlockPos> missing() {
        return List.copyOf(missing);
    }

    void setup(Civilization civ, String patron, List<Part> parts) {
        this.civ = civ;
        this.patron = patron;
        this.parts.clear();
        this.parts.addAll(parts);
        this.dirty = true;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    boolean dirty() {
        return dirty;
    }

    void markDirty() {
        dirty = true;
    }

    void setResult(boolean intact, List<BlockPos> missing) {
        this.intact = intact;
        this.missing.clear();
        this.missing.addAll(missing);
        this.dirty = false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Civ", civ.id());
        tag.putString("Patron", patron);
        ListTag list = new ListTag();
        for (Part p : parts) {
            CompoundTag c = new CompoundTag();
            c.putLong("O", p.offset().asLong());
            c.put("S", NbtUtils.writeBlockState(p.state()));
            list.add(c);
        }
        tag.put("Parts", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        civ = Civilization.byId(tag.getString("Civ")).orElse(Civilization.GREEK);
        patron = tag.getString("Patron");
        parts.clear();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag t : tag.getList("Parts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            parts.add(new Part(BlockPos.of(c.getLong("O")), NbtUtils.readBlockState(blocks, c.getCompound("S"))));
        }
        dirty = true;
    }

    // ───────────── 客户端同步（渲染器按主神选神像模型，T11b）─────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Civ", civ.id());
        tag.putString("Patron", patron);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        civ = Civilization.byId(tag.getString("Civ")).orElse(Civilization.GREEK);
        patron = tag.getString("Patron");
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }
}
