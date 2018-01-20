package com.github.mefisto94.RedstoneGate;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockRedstoneGate extends BlockHorizontal implements ITileEntityProvider {
    protected static final AxisAlignedBB REDSTONE_GATE_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final Logger LOG = Logger.getLogger(BlockRedstoneGate.class.getSimpleName());

    protected BlockRedstoneGate() {
        super(Material.CIRCUITS);

        if (!(this instanceof BlockOldRedstoneGate)) { // don't double register names
            setRegistryName("redstonegate", "gate" + (false ? "-powered" : "-unpowered"));
            setUnlocalizedName("redstonegate." + getRegistryName().getResourcePath());
            setCreativeTab(CreativeTabs.REDSTONE);
        }

        this.setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(POWERED, false));
        this.isBlockContainer = true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return REDSTONE_GATE_AABB;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        LOG.log(Level.INFO, "[" + pos.toString() + "]: Ticking");

        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)worldIn.getTileEntity(pos);
        boolean powered = state.getValue(POWERED);
        byte old_vector = tile_entity.outputVector;
        tile_entity.RecomputeOutput(worldIn, pos);

        boolean shouldBePowered = tile_entity.outputVector != 0;

        if (powered && !shouldBePowered) {
            updateBlockState(worldIn, pos, getUnpoweredState(state));
            LOG.log(Level.INFO, "[" + pos.toString() + "]: Switching to UNPOWERED STATE");
        } else if (!powered && shouldBePowered) {
            updateBlockState(worldIn, pos, getPoweredState(state));
            LOG.log(Level.INFO, "[" + pos.toString() + "]: Switching to POWERED STATE");
        }
        tile_entity.canUpdate = true;
    }

    protected int getPowerOnSide(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)worldIn.getTileEntity(pos);
        boolean b = (tile_entity.outputVector & 0xFF & (1 << side.getIndex())) == 0;
        if (!b) { // Reduce logging spam when side is unpowered.
            LOG.log(Level.INFO, "[" + pos.toString() + "]: getPowerOnSide(" + side.toString() + ") = " + (b ? " 0" : " 15"));
        }
        if (tile_entity != null && (tile_entity.outputVector & 0xFF & (1 << side.getIndex())) == 0) {
            return 0;
        } else {
            return 15;
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn) {
        TileEntityRedstoneGate tile_entity = (TileEntityRedstoneGate)worldIn.getTileEntity(pos);
        LOG.log(Level.INFO, "[" + pos.toString() + "]: neighborChanged...");

        if (!canBlockStay(worldIn, pos)) {
            dropBlockAsItem(worldIn, pos, this.getDefaultState(), 0);
            worldIn.setBlockToAir(pos);

            worldIn.notifyBlockOfStateChange(pos, this);
            worldIn.notifyNeighborsOfStateChange(pos, this);
            return;
        }

        if (!tile_entity.canUpdate) {
            LOG.log(Level.WARNING, "[" + pos.toString() + "]: A neighbor has been changed but we can't update yet again." +
                    "This means the change had to be discarded. Try to reduce the delay");
            return;
        }

        tile_entity.canUpdate = false;
        int delay = tile_entity.delay;
        worldIn.scheduleBlockUpdate(pos, this, delay * 2, -1);
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        /* @TODO: Use to not allow redstone if there is no input/output defined? But that has the downside that you can
        * only wire things up when you've configured them before.
        */
        return super.canConnectRedstone(state, world, pos, side); // Used to determine where redstone could be placed
    }

    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return getPowerOnSide(blockAccess, pos, side);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return getPowerOnSide(blockAccess, pos, side);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
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

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()); // See BlockRedstoneDiode
    }

    protected void updateBlockState(World worldIn, BlockPos pos, IBlockState state) {
        worldIn.setBlockState(pos, state, 3);
        worldIn.notifyNeighborsOfStateChange(pos, this);
    }

    // Not every Block notifies neighbors.
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

    // From BlockRedstoneDiode
    protected IBlockState getPoweredState(IBlockState unpoweredState) {
        EnumFacing enumfacing = unpoweredState.getValue(FACING);
        return unpoweredState.withProperty(FACING, enumfacing).withProperty(POWERED, true);
    }

    protected IBlockState getUnpoweredState(IBlockState poweredState) {
        EnumFacing enumfacing = poweredState.getValue(FACING);
        return poweredState.withProperty(FACING, enumfacing).withProperty(POWERED, false);
    }

    public boolean canBlockStay(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.down()).isFullyOpaque();
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
        if (stateIn.getValue(POWERED))
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
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta))
            .withProperty(POWERED, RedstoneGate.bit_to_boolean((byte)(meta >> 2)));
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state)
    {
        int i = 0;
        i |= (state.getValue(FACING)).getHorizontalIndex(); // 0..3, that's 2 bits
        i |= (RedstoneGate.boolean_to_bit(state.getValue(POWERED)) << 2); // 0..1, that's 1 bit
        return i;
    }

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { FACING, POWERED });
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
 
