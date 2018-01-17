package com.github.mefisto94.RedstoneGate;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockRedstoneGate extends BlockContainer {
    public boolean renderAsItem;

    protected BlockRedstoneGate() {
        // blockIndexInTexture = 6
        super(Material.GLASS);
        //super.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f); // minXYZ maxXYZ
        this.renderAsItem = false;

        if (!(this instanceof BlockOldRedstoneGate)) { // don't double register names
            setRegistryName("redstonegate", "gate");
            setUnlocalizedName("redstonegate." + getRegistryName().getResourcePath());
            setCreativeTab(CreativeTabs.REDSTONE);
        }
    }

    // Not supported anymore, just like setBlockBounds
    //@Override
    public boolean renderAsNormalBlock() {
        return !renderAsItem;
    }

    // Same here, used for custom rendering.
    //@Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        if (renderAsItem) {
            return EnumBlockRenderType.INVISIBLE;
        } else {
            return EnumBlockRenderType.MODEL;
        }
    }

    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (this.renderAsItem) {
            return true;
        }
        if (side.getIndex() == 1) return false;
        return true;
    }

    //@Override
    public int getBlockTextureFromSideAndMetadata(int i, int j) {
        if (i == 0) {
            return 6;
        }
        if (i != 1) return 5;
        if (!this.renderAsItem) return 147;
        return 131;
    }

    //@Override
    public int getBlockTextureFromSide(int i) {
        return getBlockTextureFromSideAndMetadata(i, 0);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(worldIn, pos, state, rand);

        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)worldIn.getTileEntity(pos);

        byte old_vector = tile_entity.outputVector;
        tile_entity.RecomputeOutput(worldIn, pos);
        if (tile_entity.outputVector == old_vector) {
            tile_entity.canUpdate = true;
            return;
        }

        worldIn.notifyNeighborsOfStateChange(pos, this);
        worldIn.notifyBlockOfStateChange(pos, this);

        int delay = tile_entity.delay;
        worldIn.scheduleBlockUpdate(pos, this, delay == 0 ? 2 : delay * 2, -1);
    }

    public boolean isIndirectlyPoweringTo(World world, int x, int y, int z, int side) {
        return shouldSideBeRendered(this.getDefaultState(), (IBlockAccess)world, new BlockPos(x, y, z), EnumFacing.VALUES[side]);
    }

    public boolean isPoweringTo(IBlockAccess iblockaccess, BlockPos pos, int side) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)iblockaccess.getTileEntity(pos);
        if ((tile_entity.outputVector & 1 << side) == 0) return false;
        return true;
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)world.getTileEntity(pos);

        // According to 1.2.3's JavaDoc canBlockStay is similar (expect that it's checked for plants?)
        if (!canPlaceBlockAt((World)world, pos)) {
            dropBlockAsItem((World) world, pos, this.getDefaultState(), 0);
            ((World) world).setBlockState(pos, getDefaultState());
            ((World) world).notifyBlockOfStateChange(pos, this);
            ((World) world).notifyNeighborsOfStateChange(pos, this);
            return;
        }

        if (!tile_entity.canUpdate) {
            return;
        }

        tile_entity.canUpdate = false;
        int delay = tile_entity.delay;
        ((World) world).scheduleBlockUpdate(pos, this, delay * 2, -1);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntityRedstoneGate tileentity = (TileEntityRedstoneGate)worldIn.getTileEntity(pos);
        //ModLoader.openGUI(playerIn, (GuiScreen)new GuiRedstoneGate(playerIn, tileentity));
        if (!worldIn.isRemote) {
            playerIn.openGui(RedstoneGate.instance, GUIHandler.getGuiID(), worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    /* @TODO: Investigate Deprecation cause. Probably it's state#canProvidePower? */
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        // @TODO: Find an explanation for that voodo math magic
        int l = ((MathHelper.floor_double((double)((double)(placer.rotationYaw * 4.0f / 360.0f) + 0.5)) & 3) + 2) % 4;
        ((TileEntityRedstoneGate)worldIn.getTileEntity(pos)).inputMask = (byte)(((TileEntityRedstoneGate)worldIn.getTileEntity(pos)).inputMask | l << 6);
    }


    // @TODO: Find out whether these overrides differ from the standard implementation of Block?
    // Probably not every Block notifies neighbors?
    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        worldIn.notifyNeighborsOfStateChange(pos, this);
    }

    @Override
    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockDestroyedByPlayer(worldIn, pos, state);
        worldIn.notifyNeighborsOfStateChange(pos, this);
    }


    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRedstoneGate();
    }
}
 
