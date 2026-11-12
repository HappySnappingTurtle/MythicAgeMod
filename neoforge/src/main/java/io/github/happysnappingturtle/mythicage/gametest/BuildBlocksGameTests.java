package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.village.build.*;
import io.github.happysnappingturtle.mythicage.core.village.gen.*;
import io.github.happysnappingturtle.mythicage.power.TempBlocks;
import io.github.happysnappingturtle.mythicage.village.*;
import io.github.happysnappingturtle.mythicage.village.build.*;
import io.github.happysnappingturtle.mythicage.village.economy.ProjectNbt;
import io.github.happysnappingturtle.mythicage.village.gen.*;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;
import java.util.function.Consumer;
import static io.github.happysnappingturtle.mythicage.gametest.VillageBuildGameTests.*;

@GameTestHolder("mythic_age")
@PrefixGameTestTemplate(false)
public final class BuildBlocksGameTests {
    private static final String T=GameTestTemplates.EMPTY_17X16X36, B="build_blocks";
    private BuildBlocksGameTests() {}
    private static void workerCycle(GameTestHelper h, boolean effects, boolean banked) {
        var tv=village(h); var p=hut(h,tv,30,4);
        if(banked) {
            VillageScheduler.forceRunningForTest(tv.id(),false);
            h.assertTrue(Construction.drop(ctx(h,tv),p,0)==Construction.Result.WAIT && p.placedCells.isEmpty(),"非真实运行不落块");
            Construction.accrue(ctx(h,tv),48000);
            VillageScheduler.forceRunningForTest(tv.id(),true);
        }
        var r=VillageWorkGameTests.resident(h,tv,"buildtest:worker",8,6);
        List<String> sounds=new ArrayList<>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener=e -> {
            if(e.getLevel()==h.getLevel() && e.getSound()!=null && e.getPosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(rel(h,7,2,11)))<64)
                sounds.add(e.getSound().value().getLocation().toString());
        };
        NeoForge.EVENT_BUS.addListener(listener);
        long start=h.getLevel().getGameTime(); int[] previous={0}, placements={0}, swings={0};
        VillageScheduler.processNowForTest(h.getLevel(),tv.id());
        var job=r.job().orElseThrow();
        h.assertTrue(job.steps().get(1).ticks()==20, "仓储取货停20刻");
        h.assertTrue(job.steps().getFirst().target().equals(VillageContext.pos(ctx(h,tv).center())), "无专用仓储点时使用村中心");
        h.onEachTick(() -> {
            int count=BuildProject.count(p.placedCells);
            h.assertTrue(count-previous[0]<=1,"相邻刻每工人新增不超过1格");
            if(count>previous[0]) {
                h.assertTrue(h.getLevel().getGameTime()-start>=20,"取货完成前不放块");
                h.assertTrue(p.spent.values().stream().mapToDouble(Double::doubleValue).sum()<=count,"逐格累计扣料");
                var expected = new TreeMap<String,Double>();
                var site = Construction.site(ctx(h,tv),p).orElseThrow();
                for (var segment : site.segments) for (int i=0;i<segment.cells().size();i++) {
                    if (p.cellPlaced(segment.index(),i)) BuildCosts.add(expected,site.costs.cost(segment.cells().get(i).block()));
                }
                h.assertTrue(p.consumed.equals(BuildCosts.ceil(expected)),"每一格之后精确核对累计扣料");
                placements[0]++; if(r.swinging) swings[0]++;
            }
            previous[0]=count;
        });
        h.succeedWhen(() -> {
            h.assertTrue(!p.active(),"等待真实工人完成："+p.state+" cells="+BuildProject.count(p.placedCells)+" pos="+r.blockPosition()+" "+BuildJob.diagnostic(r));
            h.assertTrue(placements[0]>=20,"确实经历多次独立放置");
            if(effects) {
                h.assertTrue(swings[0]>0,"放置有挥手");
                h.assertTrue(sounds.contains(Blocks.STONE.defaultBlockState().getSoundType().getPlaceSound().getLocation().toString()),"石头实际放置音效");
                h.assertTrue(p.consumed.get("minecraft:stone_bricks")==23,"正好逐块消耗23块石砖");
                h.assertTrue(p.consumed.get("#planks")==null || p.consumed.get("#planks")==2,"门材料正确");
            }
            if(banked) h.assertTrue(ctx(h,tv).state().constructionBank<48000,"追赶储备实际消耗");
            NeoForge.EVENT_BUS.unregister(listener); cleanup(h,tv);
        });
    }
    @GameTest(template=T,batch=B,timeoutTicks=3000) public static void tc20_fetch_walk_individual_blocks(GameTestHelper h) { workerCycle(h,false,false); }
    @GameTest(template=T,batch=B,timeoutTicks=3000) public static void tc21_payment_swing_and_sound(GameTestHelper h) { workerCycle(h,true,false); }
    @GameTest(template=T,batch=B,timeoutTicks=1600) public static void tc22_only_player_cell_blocked(GameTestHelper h) {
        var tv=village(h);var p=hut(h,tv,30,4);h.setBlock(new BlockPos(6,2,11),Blocks.OAK_PLANKS);
        h.startSequence().thenWaitUntil(() -> { step(h,tv,p);h.assertTrue(p.state==BuildProject.State.BLOCKED,"完成可放格后才BLOCKED"); })
            .thenExecute(() -> {
                h.assertTrue(BuildProject.count(p.blockedCells)==1,"仅被挡一格");
                h.assertTrue(BuildProject.count(p.placedCells)==26,"其余26格（含空气格）已完成");
                h.assertTrue(h.getLevel().getBlockState(rel(h,6,2,11)).is(Blocks.OAK_PLANKS),"玩家木板没有被改动");
                Construction.cancel(ctx(h,tv),p);cleanup(h,tv);h.succeed();
            });
    }
    @GameTest(template=T,batch=B,timeoutTicks=1800) public static void tc23_blocked_recheck_and_finish(GameTestHelper h) { build_blocked_then_resumes(h); }
    @GameTest(template=T,batch=B,timeoutTicks=1800) public static void tc24_missing_then_supply(GameTestHelper h) {
        var tv=village(h);var p=hut(h,tv,30,4);var stock=state(h,tv).stock;
        stock.consumePart(p.escrowId(),"minecraft:stone_bricks",23);p.held.remove("minecraft:stone_bricks");
        // Remove the surplus too, so periodic stocking cannot mask the missing-cell path.
        stock.reserve("ga10_test",Map.of("minecraft:stone_bricks",7));stock.consumePart("ga10_test","minecraft:stone_bricks",7);
        h.startSequence().thenWaitUntil(() -> {
            step(h,tv,p);h.assertTrue(BuildProject.count(p.missingCells)>0,"记缺料格");
        }).thenExecute(() -> {
            h.assertTrue(p.blockedCells.isEmpty(),"缺料不是被挡");stock.putUnchecked("minecraft:stone_bricks",23);
            finishOverTicks(h,tv,p,() -> h.assertTrue(p.missingCells.isEmpty(),"补料后全部补放"));
        });
    }
    static BuildProject wall(GameTestHelper h,VillageWorkGameTests.TV tv,String suffix,int z) {
        // 21 physical blocks / 7 per trip provides three concurrent worker trips, including the high-wall stress fixture.
        VillageData.overrideForTest(CIV,civ().withConstruction(new BuildPacing.Settings(15,5,3,7,20,8,32)));
        String id="buildtest:wall_"+suffix, template="mythic_age:village/buildtest/wall";
        BlockPos source=new BlockPos(12,2,30);
        for(int x=0;x<3;x++) for(int y=0;y<7;y++) for(int zz=0;zz<3;zz++) h.setBlock(source.offset(x,y,zz),zz==1?Blocks.STONE:Blocks.AIR);
        var t=h.getLevel().getServer().getStructureManager().getOrCreate(ResourceLocation.parse(template));
        t.fillFromWorld(h.getLevel(),h.absolutePos(source),new Vec3i(3,7,3),false,Blocks.STRUCTURE_VOID);
        for(int x=0;x<3;x++) for(int y=0;y<7;y++) for(int zz=0;zz<3;zz++) h.setBlock(source.offset(x,y,zz),Blocks.AIR);
        Blueprint.clear();
        var spec=BuildingSpec.parse(JsonParser.parseString("""
            {"id":"%s","civ":"buildtest","template":"%s","size":[3,7,3],"floor_y":0,"stage":"classical",
             "generated":false,"plot_kinds":["hut"],"door":{"pos":[1,0,0],"facing":"north"},"anchors":{}}
            """.formatted(id,template)).getAsJsonObject());
        VillageBuildings.putForTest(id,spec);
        var info=tv.plan().info();var plot=new VillagePlan.Plot("wall","hut","classical",absBox(h,6,2,z,8,z+2),"north");
        VillagePlans.get(h.getLevel().getServer()).updateInfo(tv.id(),new VillageInfoPiece.Info(info.civ(),info.patron(),info.layout(),info.layoutVersion(),info.templateSet(),info.center(),info.anchors(),info.built(),List.of(plot)));
        state(h,tv).stock.putUnchecked("minecraft:stone_bricks",100);
        var p=Construction.start(ctx(h,tv),new ProjectPicker.Pick(id,"wall","","")).orElseThrow();
        Construction.tick(ctx(h,tv));return p;
    }
    @GameTest(template=T,batch=B,timeoutTicks=5000) public static void tc25_high_wall_without_scaffolds(GameTestHelper h) {
        // 2026-11-11 用户定取消脚手架：10 格高墙由工人站在地面直接放（8 格内；找不到站位时放宽到 12 格），全程不出现脚手架
        var tv=village(h);var p=wall(h,tv,"high",10);var r=VillageWorkGameTests.resident(h,tv,"buildtest:worker",8,6);
        boolean[] scaffold={false};
        VillageScheduler.processNowForTest(h.getLevel(),tv.id());
        h.onEachTick(() -> { if(h.getLevel().getBlockState(r.blockPosition()).is(Blocks.SCAFFOLDING)
                || h.getLevel().getBlockState(r.blockPosition().below()).is(Blocks.SCAFFOLDING)) scaffold[0]=true; });
        h.succeedWhen(() -> {
            h.assertTrue(!p.active(),"高墙未完："+BuildProject.count(p.placedCells)+" worker="+r.blockPosition()+" "+BuildJob.diagnostic(r));
            h.assertTrue(!scaffold[0],"不搭脚手架");cleanup(h,tv);
        });
    }
    @GameTest(template=T,batch=B,timeoutTicks=14000) public static void tc28_expansion_demolition_then_placement(GameTestHelper h) { temple_replace_to_classical(h); }
    @GameTest(template=T,batch=B,timeoutTicks=1800) public static void tc29_sign_summary_and_cleanup(GameTestHelper h) {
        var tv=village(h);var p=hut(h,tv,30,4);var context=ctx(h,tv);
        h.startSequence().thenWaitUntil(() -> {
            SiteMarkers.ensure(context,p,Construction.site(context,p).orElseThrow());
            h.assertTrue(TempBlocks.get(h.getLevel().getServer()).entries().stream().anyMatch(e -> e.owner().equals(SiteMarkers.owner(context,p))
                    && e.placed().is(ConstructionSignBlock.SIGN.get())),"等待全服配额允许施工牌放置");
        }).thenExecute(() -> {
        var entry=TempBlocks.get(h.getLevel().getServer()).entries().stream().filter(e -> e.owner().equals(SiteMarkers.owner(context,p)) && e.placed().is(ConstructionSignBlock.SIGN.get())).findFirst().orElseThrow();
        h.assertTrue(!Construction.site(context,p).orElseThrow().box().containsXZ(entry.pos().pos().getX(),entry.pos().pos().getZ()),"牌在范围外");
        h.assertTrue(SiteMarkers.describe(h.getLevel(),entry.pos().pos()).isPresent(),"聊天摘要含项目和进度");
        finishOverTicks(h,tv,p,() -> h.assertTrue(h.getLevel().getBlockState(entry.pos().pos()).isAir(),"落成移除牌"));
        });
    }
    @GameTest(template=T,batch=B,timeoutTicks=2400) public static void tc30_offline_still_individual(GameTestHelper h) { workerCycle(h,false,true); }
    @GameTest(template=T,batch=B) public static void tc31_command_speed_not_saved(GameTestHelper h) {
        var tv=village(h);var p=hut(h,tv,30,4);
        var server=h.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(h.getLevel())
                .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(ctx(h,tv).center())).withPermission(4),"mythicage dev village build speed 10");
        h.assertTrue(Construction.speed(tv.id())==10,"命令设置10倍");
        h.assertTrue(BuildPacing.ticks(ctx(h,tv).data().construction(),0,Construction.speed(tv.id()))==2,"15刻缩为2刻");
        h.assertTrue(!ProjectNbt.write(p).contains("speed"),"速度不写存档");
        Construction.clearForTest(tv.id());h.assertTrue(Construction.speed(tv.id())==1,"运行态清理后恢复1倍");cleanup(h,tv);h.succeed();
    }
    @GameTest(template=T,batch=B,timeoutTicks=1800) public static void tc32_old_nbt_continue(GameTestHelper h) {
        var tv=village(h);var p=hut(h,tv,30,4);
        p.layoutVersion=0;Construction.clearForTest(tv.id());Construction.site(ctx(h,tv),p).orElseThrow();
        h.startSequence().thenWaitUntil(() -> {
            Construction.drop(ctx(h,tv),p,0);h.assertTrue(p.placed.get(0),"先完成旧布局的第一段");
        }).thenExecute(() -> {
            var tag=ProjectNbt.write(p);
            tag.remove("layoutVersion");tag.remove("placedCells");tag.remove("blockedCells");tag.remove("missingCells");tag.remove("tripSize");
            var old=ProjectNbt.read(tag);Construction.clearForTest(tv.id());state(h,tv).projects.clear();state(h,tv).projects.add(old);
            h.assertTrue(old.layoutVersion==0,"旧NBT选择旧切段布局");
            var site=Construction.site(ctx(h,tv),old).orElseThrow();
            h.assertTrue(old.placedCells.get(0).cardinality()==site.segments.get(0).cells().size(),"已落旧段的所有格迁移");
            finishOverTicks(h,tv,old,() -> h.assertTrue(doorAt(h),"旧存档逐块继续施工落成"));
        });
    }
    @GameTest(template=T,batch=B) public static void tc32b_new_nbt_all_cell_sets(GameTestHelper h) {
        var tv=village(h);var p=hut(h,tv,30,4);
        BuildProject.mark(p.placedCells,0,0,true);BuildProject.mark(p.blockedCells,0,1,true);BuildProject.mark(p.missingCells,1,2,true);
        p.cleanupCells.add(new Pos(1,2,3));
        var copy=ProjectNbt.read(ProjectNbt.write(p));
        h.assertTrue(copy.cleanupCells.equals(p.cleanupCells),"逐格天然树干收尾队列也跨存档保留");
        h.assertTrue(copy.placedCells.equals(p.placedCells) && copy.blockedCells.equals(p.blockedCells) && copy.missingCells.equals(p.missingCells),"三种非空位图NBT往返");
        Construction.cancel(ctx(h,tv),p);cleanup(h,tv);h.succeed();
    }
    @GameTest(template=T,batch=B) public static void tc22b_player_natural_block_is_protected(GameTestHelper h) {
        var tv=village(h);var p=hut(h,tv,30,4);var player=player(h,"ga10_log",rel(h,1,2,1));
        var site=Construction.site(ctx(h,tv),p).orElseThrow();var cell=site.segments.get(0).cells().getFirst();var at=VillageContext.block(cell.pos());
        h.getLevel().setBlock(at,Blocks.OAK_LOG.defaultBlockState(),3);
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(
                net.neoforged.neoforge.common.util.BlockSnapshot.create(h.getLevel().dimension(),h.getLevel(),at),Blocks.AIR.defaultBlockState(),player));
        h.assertTrue(Construction.dropCell(ctx(h,tv),p,0,0)==Construction.Result.BLOCKED,"玩家放的原木不能按自然地形清除");
        h.assertTrue(h.getLevel().getBlockState(at).is(Blocks.OAK_LOG),"保留玩家原木");
        io.github.happysnappingturtle.mythicage.player.OnlinePlayers.forTest(player,false);Construction.cancel(ctx(h,tv),p);cleanup(h,tv);h.succeed();
    }
    @GameTest(template=T,batch=B,timeoutTicks=5000) public static void tc33_three_villages_nine_workers_measured(GameTestHelper h) {
        List<VillageWorkGameTests.TV> villages=new ArrayList<>();List<BuildProject> projects=new ArrayList<>();List<ResidentEntity> workers=new ArrayList<>();
        for(int i=0;i<3;i++) {
            var tv=village(h);villages.add(tv);var p=wall(h,tv,"perf"+i,6+i*9);projects.add(p);
            for(int j=0;j<3;j++) workers.add(VillageWorkGameTests.resident(h,tv,"buildtest:worker",4+j,4+i*9));
        }
        villages.forEach(tv -> VillageScheduler.processNowForTest(h.getLevel(),tv.id()));
        long[] previous={BuildPerformance.totalNanos()}, total={0}, maximum={0};int[] ticks={0},peak={0};boolean[] wasActive={true};
        h.onEachTick(() -> {
            long now=BuildPerformance.totalNanos(), elapsed=now-previous[0];previous[0]=now;
            if(wasActive[0]) { total[0]+=elapsed;maximum[0]=Math.max(maximum[0],elapsed);ticks[0]++; }
            wasActive[0]=projects.stream().anyMatch(BuildProject::active);
            peak[0]=Math.max(peak[0],(int)workers.stream().filter(r -> r.job().map(j -> j.type().equals(BuildJob.ID)).orElse(false)).count());
        });
        h.succeedWhen(() -> {
            h.assertTrue(projects.stream().noneMatch(BuildProject::active),"等待3村实建完成："+projects.stream().map(p -> BuildProject.count(p.placedCells)).toList()+" workers="+workers.stream().map(r -> r.blockPosition()+" "+BuildJob.diagnostic(r)).toList());
            h.assertTrue(peak[0]==9,"确实9名工人同时持有施工单："+peak[0]);
            h.assertTrue(ticks[0]>=50,"有效测量刻数");
            double average=total[0]/(double)ticks[0]/1_000_000;
            MythicAge.LOGGER.info("TC-GA10-33 measured ticks={} totalMs={} averageMs={} maxMs={} workers={}",ticks[0],total[0]/1_000_000.0,average,maximum[0]/1_000_000.0,peak[0]);
            h.assertTrue(average<=1.0,"施工每刻平均<=1ms，实际 "+average);
            villages.forEach(tv -> cleanup(h,tv));
        });
    }
    @GameTest(template=T,batch=B,timeoutTicks=3000) public static void tc34_existing_full_cycle_regression(GameTestHelper h) { build_full_cycle(h); }
}
