package com.github.mefisto94.RedstoneGate;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockRedstoneGate extends BlockRedstoneDiode implements ITileEntityProvider {
    protected static final AxisAlignedBB REDSTONE_GATE_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);

    protected BlockRedstoneGate(boolean powered) {
        super(powered);

        if (!(this instanceof BlockOldRedstoneGate)) { // don't double register names
            setRegistryName("redstonegate", "gate" + (powered ? "-powered" : "-unpowered"));
            setUnlocalizedName("redstonegate." + getRegistryName().getResourcePath());
            setCreativeTab(CreativeTabs.REDSTONE);
        }

        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.isBlockContainer = true;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)worldIn.getTileEntity(pos);
        byte old_vector = tile_entity.outputVector;
        tile_entity.RecomputeOutput(worldIn, pos);

        boolean shouldBePowered = tile_entity.outputVector != 0;//this.shouldBePowered(worldIn, pos, state);
        if (this.isRepeaterPowered && !shouldBePowered) {
            worldIn.setBlockState(pos, getUnpoweredState(state), 2);
        } else if (!this.isRepeaterPowered) {
            worldIn.setBlockState(pos, getPoweredState(state), 2);
        }

        if (tile_entity.outputVector == old_vector) {
            tile_entity.canUpdate = true;

            if (!isRepeaterPowered && !shouldBePowered) {
                worldIn.updateBlockTick(pos, getPoweredState(state).getBlock(), getTickDelay(state), -1);
            }

            return;
        }

        if (!isRepeaterPowered && !shouldBePowered) {
            worldIn.updateBlockTick(pos, getPoweredState(state).getBlock(), getTickDelay(state), -1);
        }

        worldIn.notifyNeighborsOfStateChange(pos, this);
        worldIn.notifyBlockOfStateChange(pos, this);
        worldIn.scheduleBlockUpdate(pos, this, tile_entity.delay == 0 ? 2 : tile_entity.delay * 2, -1);
    }

    // Used to be "isPoweringTo"
    @Override
    protected int getPowerOnSide(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)worldIn.getTileEntity(pos);
        if ((tile_entity.outputVector & 1 << side.getIndex()) == 0) return 0;
        return 15;
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)world.getTileEntity(pos);

        if (!canBlockStay((World)world, pos)) {
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
        if (!worldIn.isRemote) {
            playerIn.openGui(RedstoneGate.instance, GUIHandler.getGuiID(), worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
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

    // Satisfy BlockRedstoneDiode
    @Override
    protected int getDelay(IBlockState state) {
        return 1;
    }

    @Override
    protected IBlockState getPoweredState(IBlockState unpoweredState) {
        EnumFacing enumfacing = (EnumFacing)unpoweredState.getValue(FACING);
        return RedstoneGate.BLOCK_REDSTONE_GATE_POWERED.getDefaultState().withProperty(FACING, enumfacing);
    }

    @Override
    protected IBlockState getUnpoweredState(IBlockState poweredState) {
        EnumFacing enumfacing = (EnumFacing)poweredState.getValue(FACING);
        return RedstoneGate.BLOCK_REDSTONE_GATE_UNPOWERED.getDefaultState().withProperty(FACING, enumfacing);
    }

    // Copied from BlockRedstoneRepeater. There they did not appear as Deprecated to the IDE
    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     */
    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot)
    {
        return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     */
    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
    {
        return state.withRotation(mirrorIn.toRotation((EnumFacing)state.getValue(FACING)));
    }

    @Nullable
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return RedstoneGate.ITEM_REDSTONE_GATE;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(RedstoneGate.ITEM_REDSTONE_GATE);
    }

    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand)
    {
        if (this.isRepeaterPowered)
        {
            EnumFacing enumfacing = (EnumFacing)stateIn.getValue(FACING);
            double d0 = (double)((float)pos.getX() + 0.5F) + (double)(rand.nextFloat() - 0.5F) * 0.2D;
            double d1 = (double)((float)pos.getY() + 0.4F) + (double)(rand.nextFloat() - 0.5F) * 0.2D;
            double d2 = (double)((float)pos.getZ() + 0.5F) + (double)(rand.nextFloat() - 0.5F) * 0.2D;
            float f = -5.0F;

            if (rand.nextBoolean())
            {
                f = (float)(2 - 1);
            }

            f = f / 16.0F;
            double d3 = (double)(f * (float)enumfacing.getFrontOffsetX());
            double d4 = (double)(f * (float)enumfacing.getFrontOffsetZ());
            worldIn.spawnParticle(EnumParticleTypes.REDSTONE, d0 + d3, d1, d2 + d4, 0.0D, 0.0D, 0.0D, new int[0]);
        }
    }


    /**
     * Convert the given metadata into a BlockState for this Block
     */
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state)
    {
        int i = 0;
        i = i | ((EnumFacing)state.getValue(FACING)).getHorizontalIndex();
        return i;
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {FACING});
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRedstoneGate();
    }

    // Taken from BlockContainer

    /**
     * Called serverside after this block is replaced with another in Chunk, but before the Tile Entity is updated
     */
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        super.breakBlock(worldIn, pos, state);
        worldIn.removeTileEntity(pos);
    }

    /**
     * Called on both Client and Server when World#addBlockEvent is called. On the Server, this may perform additional
     * changes to the world, like pistons replacing the block with an extended base. On the client, the update may
     * involve replacing tile entities, playing sounds, or performing other visual actions to reflect the server side
     * changes.
     */
    public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id, int param)
    {
        super.eventReceived(state, worldIn, pos, id, param);
        TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity == null ? false : tileentity.receiveClientEvent(id, param);
    }
}
 
