/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.verification;

import net.historicalexperience.worldgen.HistoricalVillagePieces;
import net.historicalexperience.worldgen.HistoricalVillageResidents;
import net.historicalexperience.worldgen.HistoricalVillageRules;
import net.historicalexperience.worldgen.HistoricalVillageStructure;
import net.historicalexperience.worldgen.HistoricalVillageTypes;

import java.util.*;
import net.historicalexperience.worldgen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit developer-owned native entity/callback fixture, not a terrain or original-artifact audit. */
public final class VillageResidentAcceptance {
    private VillageResidentAcceptance() { }
    public static List<String> run(MinecraftServer server, String selection) throws Exception {
        if (!Boolean.getBoolean("historical_experience.villageResidentsProbe")) throw new IllegalStateException("Probe not armed");
        var level = server.overworld(); var actor = server.getPlayerList().getPlayers().getFirst();
        var structure = (HistoricalVillageStructure)level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(HistoricalVillageTypes.structureId());
        require(structure != null, "registered historical structure");
        String version = selection.isEmpty() ? "historical_experience:release_1_1" : selection;
        boolean inhabited = !selection.isEmpty() && !selection.endsWith("beta_1_8_1");
        // Keep fixture entities inside the player's already tracked area. A distant FULL chunk can
        // accept an entity before entity visibility is promoted; that is not a failed insertion.
        int x0 = actor.chunkPosition().getMinBlockX() - 25, z0 = actor.chunkPosition().getMinBlockZ() - 25;
        int y = Math.min(200, level.getMaxBuildHeight() - 16);
        List<StructurePiece> pieces = new ArrayList<>();
        Map<BlockPos,Integer> planned = new HashMap<>(); int index = 0;
        for (var facing : Direction.Plane.HORIZONTAL) {
            for (int type : List.of(2,3,4,7,8,9,10,11,12)) {
                int x=x0+(index%6)*10, z=z0+(index/6)*10; index++;
                // The two north-facing large-house residents deliberately straddle a chunk boundary.
                if(type==4 && facing==Direction.NORTH) x=actor.chunkPosition().getMinBlockX()-3;
                var piece = HistoricalVillagePieces.house(type, new BoundingBox(x,y,z,x+8,y+12,z+8), facing, 0, version);
                pieces.add(piece);
                for (var pos : piece.residentPositions()) planned.merge(pos, 1, Integer::sum);
            }
        }
        // Every nonresidential type is included, especially the well, which the broken loop populated first.
        for (int type : List.of(0,1,5,6)) pieces.add(HistoricalVillagePieces.house(type,
            new BoundingBox(x0+96,y,z0,x0+110,y+12,z0+14), Direction.SOUTH, 0, version));
        var container = new PiecesContainer(pieces);
        List<ChunkPos> chunks = planned.keySet().stream().map(ChunkPos::new).distinct()
            .sorted(Comparator.comparingInt((ChunkPos p) -> p.x).thenComparingInt(p -> p.z)).toList();
        for (var chunk : chunks) { level.setChunkForced(chunk.x, chunk.z, true); level.getChunk(chunk.x,chunk.z); }
        for (var pos : planned.keySet()) {
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        }
        var marker = EntityType.VILLAGER.create(level); require(marker != null, "existing villager marker created");
        var markerPos = planned.keySet().iterator().next().above(4);
        marker.moveTo(markerPos.getX()+0.5,markerPos.getY(),markerPos.getZ()+0.5,0,0);
        marker.setNoAi(true); marker.setPersistenceRequired(); marker.setCustomName(Component.literal("Existing villager preservation sentinel"));
        marker.setAge(-1234); marker.setHealth(17); marker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.EMERALD,7));
        require(level.addFreshEntity(marker), "existing marker inserted");
        var beforeMarker = marker.saveWithoutId(new CompoundTag());
        var before = ids(level);
        var reversed = new ArrayList<>(chunks); Collections.reverse(reversed);
        for (var chunk : reversed) callback(structure,level,container,chunk);
        var residents = villagers(level).stream().filter(e -> !before.contains(e.getUUID())).toList();
        int expected = inhabited ? 44 : 0;
        require(residents.size() == expected, "actual native resident count " + residents.size() + " expected " + expected + " masks=" + masks(container));
        Map<BlockPos,Integer> actual = new HashMap<>();
        for (var villager : residents) {
            actual.merge(villager.blockPosition(),1,Integer::sum); villager.setNoAi(true);
            require(villager.getVillagerData().getType() == VillagerType.PLAINS, "historical plain skin");
            if (selection.endsWith("release_1_1")) require(villager.getVillagerData().getProfession() == VillagerProfession.NONE, "pre1.3 no profession");
            else require(HistoricalVillageRules.RELEASE_1_3_PROFESSIONS.contains(
                net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString()), "old profession only");
        }
        require(actual.equals(inhabited ? planned : Map.of()), "every actual resident occupies a unique expected residential point, all four rotations");
        for (var piece : container.pieces()) {
            var village = (HistoricalVillagePieces.VillagePiece)piece;
            require(village.residentMask() == (inhabited ? HistoricalVillageResidents.validMask(village.pieceType()) : 0), "claim count matches piece " + village.pieceType());
        }
        var residentIds = ids(level);
        // Real registered piece serialization and compressed disk roundtrip, not a reconstructed helper mask.
        var context = StructurePieceSerializationContext.fromLevel(level); var root = new CompoundTag(); root.put("Pieces",container.save(context));
        var file = server.getWorldPath(LevelResource.ROOT).resolve("village-resident-fixture.nbt");
        NbtIo.writeCompressed(root,file);
        var reloaded = PiecesContainer.load(NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()).getList("Pieces",Tag.TAG_COMPOUND),context);
        require(masks(container).equals(masks(reloaded)), "registered piece disk roundtrip retains all resident claims");
        for (int pass=0; pass<3; pass++) for (var chunk : chunks) callback(structure,level,reloaded,chunk);
        require(ids(level).equals(residentIds), "reordered/repeated callbacks after disk reload create zero new entities");
        require(marker.saveWithoutId(new CompoundTag()).equals(beforeMarker), "existing villager UUID/name/age/health/equipment/brain data unchanged");
        for (var chunk : chunks) level.setChunkForced(chunk.x,chunk.z,false);
        return List.of(selection + " native population=" + expected + ", no well/nonresidential spawns; four rotations/current-chunk clipping",
            selection + " registered-piece compressed disk reload + three repeated callback passes preserve exact entity UUID set",
            selection + " existing villager data unchanged; historical age/profession/plain-skin and ordinary isolation controls pass");
    }
    public record NaturalState(ServerLevel level, BoundingBox bounds, int expected, String description) {
        public int count() { int n=0; for(var entity:level.getAllEntities()) if(entity instanceof Villager && bounds.isInside(entity.blockPosition()))n++; return n; }
    }
    /** Actual terrain generation, including WorldGenRegion insertion, rather than only a ServerLevel fixture. */
    public static NaturalState generateNatural(MinecraftServer server,String selection) {
        if(!Boolean.getBoolean("historical_experience.villageResidentsProbe"))throw new IllegalStateException("Probe not armed");
        var level=server.overworld(); var registry=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var holder=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,HistoricalVillageTypes.structureId()));
        var gen=level.getChunkSource().getGenerator();
        var found=gen.findNearestMapStructure(level,net.minecraft.core.HolderSet.direct(holder),server.getPlayerList().getPlayers().getFirst().blockPosition(),24,false);
        require(found!=null,"natural historical village located");
        var centre=new ChunkPos(found.getFirst()); level.setChunkForced(centre.x,centre.z,true);
        var start=level.getChunk(centre.x,centre.z).getStartForStructure(holder.value()); require(start!=null && start.isValid(),"actual structure start");
        var bb=start.getBoundingBox();
        for(int x=(bb.minX()>>4);x<=(bb.maxX()>>4);x++)for(int z=(bb.minZ()>>4);z<=(bb.maxZ()>>4);z++) {
            level.setChunkForced(x,z,true); level.getChunk(x,z);
        }
        int planned=0,claimed=0; var types=new TreeMap<Integer,Integer>();
        for(var p:start.getPieces())if(p instanceof HistoricalVillagePieces.VillagePiece piece) {
            planned+=piece.residentPositions().size(); claimed+=Integer.bitCount(piece.residentMask()); types.merge(piece.pieceType(),1,Integer::sum);
        }
        boolean inhabited=!selection.endsWith("beta_1_8_1");
        require(!inhabited || planned>0,"real village has residential pieces: " + types);
        require(claimed==(inhabited?planned:0),"real generation claims="+claimed+" expected="+(inhabited?planned:0)+" types="+types);
        return new NaturalState(level,new BoundingBox(bb.minX()-16,level.getMinBuildHeight(),bb.minZ()-16,bb.maxX()+16,level.getMaxBuildHeight()-1,bb.maxZ()+16),inhabited?planned:0,
            selection+" actual generated village "+centre+" pieces="+types+" planned="+planned+" claimed="+claimed);
    }
    private static List<Integer> masks(PiecesContainer pieces) { return pieces.pieces().stream().map(p -> ((HistoricalVillagePieces.VillagePiece)p).residentMask()).toList(); }
    private static void callback(HistoricalVillageStructure structure,ServerLevel level,PiecesContainer pieces,ChunkPos chunk) {
        structure.afterPlace(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(7),
            new BoundingBox(chunk.getMinBlockX(),level.getMinBuildHeight(),chunk.getMinBlockZ(),chunk.getMaxBlockX(),level.getMaxBuildHeight()-1,chunk.getMaxBlockZ()),chunk,pieces);
    }
    private static List<Villager> villagers(ServerLevel level) { var result=new ArrayList<Villager>(); for (var entity:level.getAllEntities()) if(entity instanceof Villager villager)result.add(villager); return result; }
    private static Set<UUID> ids(ServerLevel level) { var result=new HashSet<UUID>(); for (Entity entity:level.getAllEntities())result.add(entity.getUUID()); return result; }
    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message); }
}
