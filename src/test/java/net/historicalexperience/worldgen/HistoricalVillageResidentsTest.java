/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.worldgen;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HistoricalVillageResidentsTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void allResidentialPiecesHaveResidentsButWellRoadAndFarmsDoNot() {
        var counts = Map.of(2, 1, 3, 1, 4, 2, 7, 1, 8, 2, 9, 1, 10, 1, 11, 1, 12, 1);
        for (int type = 0; type <= 12; type++) assertEquals(counts.getOrDefault(type, 0), HistoricalVillageResidents.spots(type).size(), "piece" + type);
        assertTrue(HistoricalVillageResidents.spots(0).isEmpty(), "well never receives a resident");
        assertTrue(HistoricalVillageResidents.spots(-1).isEmpty());
        assertTrue(HistoricalVillageResidents.spots(99).isEmpty());
    }
    @Test void smallHouseOnlyAndShackOnlyLayoutsCannotBeEmptyAfterVillagerIntroduction() {
        for (int type : List.of(2,3,10,11)) {
            assertEquals(1, HistoricalVillageResidents.spots(type).size());
            var point=HistoricalVillageResidents.spots(type).getFirst();
            assertEquals(2,point.y(),"above the floor at local y1");
            assertEquals(2,point.z());
            assertEquals(type==2 || type==3 ? 2 : 1,point.x(),"clear of walls, ladder, fence table and doorway");
        }
    }
    @Test void pinnedSourceCoordinatesAreNotPieceCentres() {
        assertEquals(List.of(new HistoricalVillageResidents.Spot(2,2,3), new HistoricalVillageResidents.Spot(4,2,3)), HistoricalVillageResidents.spots(4));
        assertEquals(List.of(new HistoricalVillageResidents.Spot(4,2,2)), HistoricalVillageResidents.spots(7));
        assertEquals(List.of(new HistoricalVillageResidents.Spot(1,2,7), new HistoricalVillageResidents.Spot(4,2,7)), HistoricalVillageResidents.spots(8));
        assertEquals(List.of(new HistoricalVillageResidents.Spot(2,2,7)), HistoricalVillageResidents.spots(9));
        assertEquals(List.of(new HistoricalVillageResidents.Spot(2,2,3)), HistoricalVillageResidents.spots(12));
        assertThrows(UnsupportedOperationException.class, () -> HistoricalVillageResidents.spots(4).clear());
    }
    @Test void residentPositionsUseNativeOrientationAtNegativeCoordinates() {
        for (var facing : Direction.Plane.HORIZONTAL) for (int type : List.of(2,3,4,7,8,9,10,11,12)) {
            var piece = new SavedPiece(tag(type, facing, 0)); var positions = piece.residentPositions();
            var spots = HistoricalVillageResidents.spots(type);
            for (int i=0; i<spots.size(); i++) {
                var spot=spots.get(i); int x = switch(facing) { case WEST -> -18-spot.z(); case EAST -> -32+spot.z(); default -> -32+spot.x(); };
                int z = facing == Direction.NORTH ? 62-spot.z() : (facing == Direction.SOUTH ? 48+spot.z() : 48+spot.x());
                assertEquals(new BlockPos(x,64+spot.y(),z), positions.get(i), facing + "/" + type);
            }
        }
    }
    @Test void savedResidentClaimsSurviveReloadAndClampUnknownBits() {
        var piece = new SavedPiece(tag(4, Direction.NORTH, 3));
        assertEquals(3, piece.residentMask());
        var saved = new CompoundTag(); piece.saveOwn(saved);
        assertEquals(3, saved.getInt("HexResidentMask"));
        assertEquals(4, saved.getInt("PieceType"));
        var reloaded = tag(4, Direction.NORTH, saved.getInt("HexResidentMask"));
        assertEquals(3, new SavedPiece(reloaded).residentMask());
        assertEquals(0, new SavedPiece(tag(0,Direction.SOUTH,-1)).residentMask(), "well has no claim bits");
        assertEquals(1, new SavedPiece(tag(7,Direction.SOUTH,-1)).residentMask());
        var legacy = tag(4,Direction.SOUTH,0); legacy.remove("HexResidentMask");
        assertEquals(0, new SavedPiece(legacy).residentMask());
    }
    private static CompoundTag tag(int type, Direction facing, int mask) {
        var tag = new CompoundTag(); tag.putIntArray("BB", new int[]{-32,64,48,-18,74,62});
        tag.putInt("O",facing.get2DDataValue()); tag.putInt("GD",0); tag.putInt("PieceType",type);
        tag.putInt("VillageStyle",0); tag.putString("HexVersion",""); tag.putInt("HexResidentMask",mask); return tag;
    }
    private static final class SavedPiece extends HistoricalVillagePieces.VillagePiece {
        SavedPiece(CompoundTag tag) { super((context, value) -> null, tag); }
        void saveOwn(CompoundTag tag) { addAdditionalSaveData(null, tag); }
    }
}
