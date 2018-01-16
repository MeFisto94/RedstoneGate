package com.github.mefisto94.RedstoneGate;

import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockOldRedstoneGate extends BlockRedstoneGate {
    protected BlockOldRedstoneGate() {
        super();
        setRegistryName("redstonegate", "old-gate");
        setUnlocalizedName("redstonegate." + getRegistryName().getResourcePath());
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess iblockaccess, int x, int y, int z, int side) {
        switch (side) {
            case 0: {
                ++y;
                break;
            }
            case 1: {
                --y;
                break;
            }
            case 2: {
                ++z;
                break;
            }
            case 3: {
                --z;
                break;
            }
            case 4: {
                ++x;
                break;
            }
            case 5: {
                --x;
            }
        }

        if (iblockaccess.getBlockId(x, y, z) != mod_RedstoneGate.oldBlockID) {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            System.out.println("Invalid minecraft instance");
            return false;
        }
        World world = mc.theWorld;
        if (world == null) {
            System.out.println("null World");
            return false;
        }
        TileEntityRedstoneGate old_entity = (TileEntityRedstoneGate)iblockaccess.getBlockTileEntity(x, y, z);
        int tt = old_entity.truthTable;
        byte im = old_entity.inputMask;
        byte ov = old_entity.outputVector;
        System.out.println("Replacing instance");
        world.setBlock(x, y, z, mod_RedstoneGate.blockID);
        TileEntityRedstoneGate new_entity = (TileEntityRedstoneGate)iblockaccess.getBlockTileEntity(x, y, z);
        new_entity.truthTable = tt;
        new_entity.inputMask = im;
        new_entity.outputVector = ov;
        if (iblockaccess.getBlockId(x, y, z) != mod_RedstoneGate.blockID) return false;
        return true;
    }
}
