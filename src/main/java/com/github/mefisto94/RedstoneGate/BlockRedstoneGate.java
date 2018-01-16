package com.github.mefisto94.RedstoneGate;
/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  BlockRedstoneGate
 *  GuiRedstoneGate
 *  ModLoader
 *  TileEntityRedstoneGate
 *  aci = Material
 *  acl = net/minecraft/src/EntityLiving
 *  ags = net/minecraft/src/BlockContainer
 *  alc = net/minecraft/src/IBlockAccess
 *  gh  = net/minecraft/src/MathHelper
 *  kt  = net/minecraft/src/TileEntity
 *  mod_RedstoneGate
 *  vl  = net/minecraft/src/GuiScreen
 *  wz  = net/minecraft/src/World
 *  yr  = net/minecraft/src/EntityPlayer
 * 
 * Relevant  Methods:
 * net/minecraft/src/BlockContainer.<init>(IILnet/minecraft/src/Material;)V=|p_i116_1_,p_i116_2_,p_i116_3_
 * net/minecraft/src/BlockContainer.<init>(ILnet/minecraft/src/Material;)V=|p_i115_1_,p_i115_2_
 * net/minecraft/src/BlockContainer.func_21024_a(Lnet/minecraft/src/World;IIIII)V=|p_21024_1_,p_21024_2_,p_21024_3_,p_21024_4_,p_21024_5_,p_21024_6_
 * net/minecraft/src/BlockContainer.func_214_b(Lnet/minecraft/src/World;III)V=|p_214_1_,p_214_2_,p_214_3_,p_214_4_ // onBlockRemoval, Called whenever the Block is removed
 * net/minecraft/src/BlockContainer.func_235_e(Lnet/minecraft/src/World;III)V=|p_235_1_,p_235_2_,p_235_3_,p_235_4_
 *
 */

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;

import java.util.Random;

public class BlockRedstoneGate extends BlockContainer {
    public boolean renderAsItem;

    protected BlockRedstoneGate() {
        // blockIndexInTexture = 6
        super(Material.GLASS, 6);
        super.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f); // minXYZ maxXYZ
        this.renderAsItem = false;
        setRegistryName("redstonegate", "gate");
        setUnlocalizedName("redstonegate." + getRegistryName().getResourcePath());
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public boolean renderAsNormalBlock() {
        return !renderAsItem;
    }

    @Override
    public int getRenderType() {
        if (renderAsItem) {
            return 0;
        }
        return mod_RedstoneGate.renderID;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess iblockaccess, int x, int y, int z, int side) {
        if (this.renderAsItem) {
            return true;
        }
        if (side == 1) return false;
        return true;
    }

    @Override
    public int getBlockTextureFromSideAndMetadata(int i, int j) {
        if (i == 0) {
            return 6;
        }
        if (i != 1) return 5;
        if (!this.renderAsItem) return 147;
        return 131;
    }

    @Override
    public int getBlockTextureFromSide(int i) {
        return getBlockTextureFromSideAndMetadata(i, 0);
    }

    public void getStrVsBlock(World world, int x, int y, int z, Random random) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)world.getBlockTileEntity(x, y, z);
        byte old_vector = tile_entity.outputVector;
        tile_entity.RecomputeOutput(world, x, y, z);
        if (tile_entity.outputVector == old_vector) {
            tile_entity.canUpdate = true;
            return;
        }

        BlockPos pos = new BlockPos(x, y, z);
        /*world.j(x, y - 1, z, this.blockid);
        world.j(x, y + 1, z, this.blockid);
        world.j(x - 1, y, z, this.blockid);
        world.j(x + 1, y, z, this.blockid);
        world.j(x, y, z - 1, this.blockid);
        world.j(x, y, z + 1, this.blockid);*/
        world.notifyNeighborsOfStateChange(pos, this);
        world.notifyBlockOfStateChange(pos, this);

        int delay = world.getBlockMetadata(x, y, z);
        world.scheduleBlockUpdate(pos, this, delay == 0 ? 2 : delay * 2, -1);
    }

    public boolean isIndirectlyPoweringTo(World world, int x, int y, int z, int side) {
        return shouldSideBeRendered((IBlockAccess)world, x, y, z, side);
    }

    public boolean isPoweringTo(IBlockAccess iblockaccess, int x, int y, int z, int side) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)iblockaccess.getBlockTileEntity(x, y, z);
        if ((tile_entity.outputVector & 1 << side) == 0) return false;
        return true;
    }

    public void onNeighborBlockChange(World world, int x, int y, int z, int side) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)world.getBlockTileEntity(x, y, z);
        if (!canBlockStay(world, x, y, z)) {
            this.a(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockWithNotify(x, y, z, 0);
            return;
        }
        if (!tile_entity.canUpdate) return;
        tile_entity.canUpdate = false;
        int delay = world.getBlockMetadata(x, y, z);
        world.scheduleBlockUpdate(x, y, z, this.blockid, delay * 2);
    }

    public boolean b(World world, int x, int y, int z, EntityPlayer entityplayer) {
        TileEntityRedstoneGate tileentity = (TileEntityRedstoneGate)world.getBlockTileEntity(x, y, z);
        ModLoader.openGUI((EntityPlayer)entityplayer, (GuiScreen)new GuiRedstoneGate(entityplayer, tileentity));
        return true;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving entityliving) {
        super.onBlockPlacedBy(world, x, y, z, entityliving);
        // @TODO: Find an explanation for that voodo math magic
        int l = ((MathHelper.floor_double((double)((double)(entityliving.rotationYaw * 4.0f / 360.0f) + 0.5)) & 3) + 2) % 4;
        ((TileEntityRedstoneGate)world.getBlockTileEntity((int)x, (int)y, (int)z)).inputMask = (byte)(((TileEntityRedstoneGate)world.getBlockTileEntity((int)x, (int)y, (int)z)).inputMask | l << 6);
    }

    public void onBlockAdded(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);

        super.onBlockAdded(world, pos, null);
        world.notifyNeighborsOfStateChange(pos, this);

        /*
        world.notifyBlocksOfNeighborChange(i + 1, j, k, this.blockId);
        world.notifyBlocksOfNeighborChange(i - 1, j, k, this.blockid);
        world.notifyBlocksOfNeighborChange(i, j, k + 1, this.blockid);
        world.notifyBlocksOfNeighborChange(i, j, k - 1, this.blockid);
        world.notifyBlocksOfNeighborChange(i, j - 1, k, this.blockid);
        world.notifyBlocksOfNeighborChange(i, j + 1, k, this.blockid);
        */
    }

    public void onBlockDestroyedByPlayer(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        super.onBlockDestroyedByPlayer(world, pos, null);
        world.notifyNeighborsOfStateChange(pos, this);
        /*
        world.notifyBlocksOfNeighborChange(i, j - 1, k, this.blockid);
        world.notifyBlocksOfNeighborChange(i, j + 1, k, this.blockid);
        world.notifyBlocksOfNeighborChange(i - 1, j, k, this.blockid);
        world.notifyBlocksOfNeighborChange(i + 1, j, k, this.blockid);
        world.notifyBlocksOfNeighborChange(i, j, k - 1, this.blockid);
        world.notifyBlocksOfNeighborChange(i, j, k + 1, this.blockid);
        */
    }

    //@Override
    public boolean isOpaqueCube() {
        return false;
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRedstoneGate();
    }
}
 
