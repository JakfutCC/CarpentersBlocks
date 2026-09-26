package com.carpentersblocks.util;

import java.lang.reflect.Method;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.world.ICubicWorld;
import com.cardinalstar.cubicchunks.world.cube.ICubeProvider;

import cpw.mods.fml.common.Loader;

public final class WorldHeight {

    private WorldHeight() {}

    public static boolean shouldQueryCoverSide(IBlockAccess blockAccess, int adjacentY, int sourceY) {
        if (Loader.isModLoaded("cubicchunks")) {
            if (blockAccess instanceof World) {
                return CubicChunksHeight.shouldQueryCoverSide((World) blockAccess, blockAccess, adjacentY, sourceY);
            }
            if (Loader.isModLoaded("angelica")) {
                World world = AngelicaWorldSliceAccess.getWorld(blockAccess);
                if (world != null) {
                    return CubicChunksHeight.shouldQueryCoverSide(world, blockAccess, adjacentY, sourceY);
                }
            }
            return isVanillaCoverSideInRange(blockAccess, adjacentY, sourceY);
        }

        return isLegacyCoverSideInRange(blockAccess, adjacentY, sourceY);
    }

    public static boolean isCoverSideInRange(World world, IBlockAccess blockAccess, int adjacentY, int sourceY) {
        if (Loader.isModLoaded("cubicchunks")) {
            return CubicChunksHeight.isCoverSideInRange(world, blockAccess, adjacentY, sourceY);
        }

        return isLegacyCoverSideInRange(blockAccess, adjacentY, sourceY);
    }

    public static boolean canPlaceTwoHighDoor(World world, int bottomY) {
        if (Loader.isModLoaded("cubicchunks")) {
            return CubicChunksHeight.canPlaceTwoHighDoor(world, bottomY);
        }

        return bottomY < 255;
    }

    private static boolean isLegacyCoverSideInRange(IBlockAccess blockAccess, int adjacentY, int sourceY) {
        int height = blockAccess.getHeight();
        // The adjacent position can be in range while the rendered source is one block beyond it.
        return adjacentY > 0 && adjacentY < height && sourceY >= 0 && sourceY < height;
    }

    private static boolean isVanillaCoverSideInRange(IBlockAccess blockAccess, int adjacentY, int sourceY) {
        // Without a backing World, getHeight alone cannot establish cubic bounds.
        int height = Math.min(blockAccess.getHeight(), 256);
        return adjacentY > 0 && adjacentY < height && sourceY >= 0 && sourceY < height;
    }

    private static final class CubicChunksHeight {

        private static boolean shouldQueryCoverSide(World world, IBlockAccess blockAccess, int adjacentY, int sourceY) {
            if (isCubicWorld(world)) {
                ICubicWorld cubicWorld = (ICubicWorld) world;
                int minHeight = cubicWorld.getMinHeight();
                int maxHeight = cubicWorld.getMaxHeight();
                return adjacentY >= minHeight && adjacentY < maxHeight && sourceY >= minHeight && sourceY < maxHeight;
            }

            return isVanillaCoverSideInRange(blockAccess, adjacentY, sourceY);
        }

        private static boolean isCoverSideInRange(World world, IBlockAccess blockAccess, int adjacentY, int sourceY) {
            if (isCubicWorld(world)) {
                ICubicWorld cubicWorld = (ICubicWorld) world;
                int minHeight = cubicWorld.getMinHeight();
                int maxHeight = cubicWorld.getMaxHeight();
                return adjacentY >= minHeight && adjacentY < maxHeight && sourceY >= minHeight && sourceY < maxHeight;
            }

            return isVanillaCoverSideInRange(blockAccess, adjacentY, sourceY);
        }

        private static boolean canPlaceTwoHighDoor(World world, int bottomY) {
            if (isCubicWorld(world)) {
                ICubicWorld cubicWorld = (ICubicWorld) world;
                long supportY = (long) bottomY - 1;
                long topY = (long) bottomY + 1;
                return supportY >= cubicWorld.getMinHeight() && bottomY < cubicWorld.getMaxHeight()
                        && topY < cubicWorld.getMaxHeight();
            }

            return bottomY < 255;
        }

        private static boolean isCubicWorld(World world) {
            return world != null && world.getChunkProvider() instanceof ICubeProvider;
        }
    }

    private static final class AngelicaWorldSliceAccess {

        private static final String WORLD_SLICE_CLASS = "com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice";
        private static volatile Method getWorldMethod;

        private static World getWorld(IBlockAccess blockAccess) {
            // The existing compile-only Angelica API does not expose this 2.2.10 access interface.
            if (!isWorldSlice(blockAccess.getClass())) {
                return null;
            }

            try {
                Method method = getWorldMethod;
                if (method == null) {
                    synchronized (AngelicaWorldSliceAccess.class) {
                        method = getWorldMethod;
                        if (method == null) {
                            method = blockAccess.getClass().getMethod("getWorld");
                            getWorldMethod = method;
                        }
                    }
                }
                Object world = method.invoke(blockAccess);
                return world instanceof World ? (World) world : null;
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private static boolean isWorldSlice(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (WORLD_SLICE_CLASS.equals(current.getName())) {
                    return true;
                }
            }
            return false;
        }
    }
}
