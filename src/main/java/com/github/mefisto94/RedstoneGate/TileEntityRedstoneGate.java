package com.github.mefisto94.RedstoneGate;

import com.github.mefisto94.RedstoneGate.network.RedstoneGatePacketHandler;
import com.github.mefisto94.RedstoneGate.network.UpdateGateMessage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.EnumFaceDirection;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TileEntityRedstoneGate extends TileEntity implements IInventory {
    public static final int MAX_DELAY = 16;
    public static final int DELAY_DECR = 15;
    public static final int OP_AND = 0;
    public static final int OP_OR = 1;
    public static final int OP_XOR = 2;
    public static final int OP_NEG = 3;
    public static final int OP_ON = 4;
    public static final int OP_OFF = 5;
    public static final Block BLOCK_REDSTONE_WIRE = Blocks.REDSTONE_WIRE;
    public static final int[] HAMM_WEIGHT_3 = new int[]{0, 1, 1, 2, 1, 2, 2, 3};
    public static final int[][] relative_to_absolute_direction = new int[][]{{4, 5, 2, 3, 1, 0}, {2, 3, 5, 4, 1, 0}, {5, 4, 3, 2, 1, 0}, {3, 2, 4, 5, 1, 0}};

    // The actual data
    public byte inputMask = 0;
    public byte outputMask = 63;
    public int truthTable = 0;
    public byte delay = 0;

    public byte outputVector = 0;
    /**
     * This essentially denotes if a Block#updateTick() is still pending.
     * Every time onNeighborChange is called, it will set this to false,
     * and the actual tick will set it to true again
     */
    public boolean canUpdate = true;

    private boolean isInvalidConfig(int inputMask, int outputMask) {
        int outputcount = this.hammingWeight(outputMask & 63);
        int combinations = (1 << this.hammingWeight(inputMask & 63)) * outputcount;
        if (outputcount == 0) return true;
        if (32 < combinations) return true;
        return false;
    }

    public String getConfigString() {
        //int meta = worldObj.getBlockMetadata(getPos().getX(), getPos().getY(), getPos().getZ());
        return String.format("%02x%02x-%08x-%01x", inputMask & 63, outputMask & 63, truthTable, delay % 16);
    }

    public boolean setConfigString(String s) {
        try {
            int im = Integer.parseInt(s.substring(0, 2), 16) & 63 | this.inputMask & 192;
            int om = Integer.parseInt(s.substring(2, 4), 16) & 63 | this.outputMask & 192;
            long tt = Long.parseLong(s.substring(5, 13), 16);
            int meta = Integer.parseInt(s.substring(14, 15), 16);
            if (this.isInvalidConfig(im, om)) {
                return false;
            }

            inputMask = (byte)im;
            outputMask = (byte)om;
            truthTable = (int)tt;
            delay = (byte)meta;
            return true;
        }
        catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private int hammingWeight(int value) {
        int result = 0;
        while (0 < value) {
            result += HAMM_WEIGHT_3[value & 0x7];
            value >>= 3;
        }
        return result;
    }

    private long segmentModifier(final int sInput, final int nInput, final int bitcount, final int operation) {
        long m = 0L;
        switch (operation) {
            case 0: {
                for (int j = bitcount - 1; 0 <= j; --j) {
                    m = (m << 1 | (((sInput & (j ^ nInput)) == sInput) ? 1 : 0));
                }
                break;
            }
            case 1: {
                for (int j = bitcount - 1; 0 <= j; --j) {
                    m = (m << 1 | (((sInput & (j ^ nInput)) != 0x0) ? 1 : 0));
                }
                break;
            }
            case 2: {
                for (int j = bitcount - 1; 0 <= j; --j) {
                    m = (m << 1 | this.hammingWeight(sInput & (j ^ nInput)) % 2);
                }
                break;
            }
            case 4: {
                m = (1L << bitcount) - 1L;
                break;
            }
        }
        return m;
    }

    public void performQuickConfig(final byte selected, final byte negated, final int operation, final boolean clear) {
        byte sInput = 0;
        byte sOutput = 0;
        byte bitcount = 1;
        byte nInput = 0;
        byte nOutput = 0;
        for (int i = 32; 0 < i; i >>= 1) {
            final byte sb = (byte)(((selected & i) != 0x0) ? 1 : 0);
            final byte nb = (byte)(((negated & i) != 0x0) ? 1 : 0);
            if ((this.inputMask & i) != 0x0) {
                sInput = (byte)(sInput << 1 | sb);
                nInput = (byte)(nInput << 1 | nb);
                bitcount <<= 1;
            }
            if ((this.outputMask & i) != 0x0) {
                sOutput = (byte)(sOutput << 1 | sb);
                nOutput = (byte)(nOutput << 1 | nb);
            }
        }
        long table = this.truthTable & -1;
        final long max_val = (1L << bitcount) - 1L;
        if (operation == 3) {
            int i = 0;
            while (0 < sOutput) {
                if ((sOutput & 0x1) != 0x0) {
                    table ^= max_val << i;
                }
                i += bitcount;
                sOutput >>= 1;
            }
        }
        else {
            final long m = this.segmentModifier(sInput, nInput, bitcount, operation);
            long n = 0L;
            int i = 0;
            while (0 < sOutput) {
                if ((sOutput & 0x1) != 0x0) {
                    n = (((nOutput & 0x1) != 0x0) ? (m ^ max_val) : m);
                    if (clear) {
                        table = ((table & ~(max_val << i)) | n << i);
                    }
                    else {
                        table |= n << i;
                    }
                }
                i += bitcount;
                sOutput >>= 1;
                nOutput >>= 1;
            }
        }
        this.truthTable = (int)table;
    }

    private boolean isPoweredWire(World world, BlockPos pos) {
        IBlockState wireState = world.getBlockState(pos);
        return wireState.getBlock() == BLOCK_REDSTONE_WIRE &&
            (wireState.getValue(BlockRedstoneWire.POWER) != 0);
    }

    /**
     * Determine if this side is powered....
     * Note: direction is pairwise swapped with EnumFacing, as you can see.
     * This is our internal convention here, we leave it at this stage and might refactor it at a later point.
     *
     * @param world
     * @param pos
     * @param direction
     * @return
     */
    private boolean isSidePowered(World world, BlockPos pos, EnumFacing direction) {
        EnumFacing opposite = direction.getOpposite(); // Because of our wierd logic
        BlockPos newPos = pos.offset(opposite);
        if (this.isPoweredWire(world, newPos)) {
            return true;
        }

        /*
         *  Extract of isBlockIndirectlyProvidingPowerTo:

            if (isBlockNormalCube(par1, par2, par3)) {
                return isBlockGettingPowered(par1, par2, par3);
            }

            int i = getBlockId(par1, par2, par3);

            if (i == 0) {
                return false;
            } else {
                return Block.blocksList[i].isPoweringTo(this, par1, par2, par3, par4);
            }

            Since the purpose seems to be to find whether any block is providing power to par1, par2, par3,
            world.isBlockIndirectlyGettingPowered() seems the right replacement

            // Is a block next to you getting powered (if its an attachable block) or is it providing power directly to you.  Args: x, y, z, direction
            if (world.isBlockIndirectlyProvidingPowerTo(newPos, opposite.getIndex())) {
                return true;
            }
         */

        return world.isBlockIndirectlyGettingPowered(newPos) > 0;
    }

    public void RecomputeOutput(World world, BlockPos pos) {
        int index = 0;
        int bitcount = 1;
        int[] dirmap = relative_to_absolute_direction[(inputMask & 255) >> 6];
        for (int d = 5; 0 <= d; --d) {
            if ((inputMask & 1 << d) == 0) continue;
            bitcount = (byte)(bitcount * 2);
            index = index << 1 | (isSidePowered(world, pos, EnumFacing.VALUES[dirmap[d]]) ? 1 : 0) | outputVector >> dirmap[d] & 1;
        }
        this.outputVector = 0;
        long table = this.truthTable & -1;
        int d2 = 0;
        while (d2 < 6) {
            if ((this.outputMask & 1 << d2) != 0) {
                this.outputVector = (byte)(outputVector | (byte)((table >> index & 1) << dirmap[d2]));
                index += bitcount;
            }
            ++d2;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTagCompound) {
        super.writeToNBT(nbtTagCompound);
        nbtTagCompound.setByte("delay", delay);
        nbtTagCompound.setByte("inputs", inputMask);
        nbtTagCompound.setByte("outputs", outputMask);
        nbtTagCompound.setInteger("table", truthTable);
        return nbtTagCompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound) {
        super.readFromNBT(nbtTagCompound);
        inputMask = nbtTagCompound.getByte("inputs");
        delay = nbtTagCompound.getByte("delay");
        truthTable = nbtTagCompound.getInteger("table");
        if (nbtTagCompound.hasKey("outputs")) {
            outputMask = nbtTagCompound.getByte("outputs");
            return;
        }
        outputMask = (byte)(~ inputMask & 63);
    }

    // The following three methods seem required to update TileEntities over the network
    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getNbtCompound());
    }

    // Called when the server receives something
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
    }

    @Override
    public int getSizeInventory() {
        return 48;
    }

    @Override
    public int getFieldCount() {
        return getSizeInventory(); // Not sure, but I guess that's the Number of available/filled Stacks
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i != 45) return null;
        return new ItemStack(RedstoneGate.BLOCK_REDSTONE_GATE_UNPOWERED);
    }

    public ItemStack decrStackSize(int i, int j) {
        if (i < 0) return null;
        if (this.getSizeInventory() < i) {
            return null;
        }
        if (i == 47) {
            String s = GuiRedstoneGate.copypastebuffer[j];
            GuiRedstoneGate.setStatusMessage((String)(this.setConfigString(s) ? String.format("Pasted %s from slot %d", s, j) : ""));
            return null;
        }
        if (i == 46) {
            String s;
            GuiRedstoneGate.copypastebuffer[j] = s = this.getConfigString();
            GuiRedstoneGate.setStatusMessage((String)String.format("Copied %s to slot %d", s, j));
            return null;
        }
        if (i == 44) {
            // j != 0 signalizes to reduce the delay by one, j == 0 increase delay
            delay = (byte)((delay + (j == 0 ? 1 : DELAY_DECR)) % MAX_DELAY);
            return null;
        }
        if (38 <= i) {
            this.performQuickConfig(GuiRedstoneGate.selectedIO, GuiRedstoneGate.negatedIO, i - 38, j == 0);
            return null;
        }
        if (32 > i) {
            this.truthTable ^= 1 << i;
            return null;
        }
        // Input/Output Matrix.
        byte bit = (byte)(1 << i - 32);
        if (j == 1) { // Rightclick or normal klick? 0 -> Middle Mouse Button
            byte temp = (byte)(GuiRedstoneGate.negatedIO ^ GuiRedstoneGate.selectedIO & bit);
            GuiRedstoneGate.selectedIO = (byte)(GuiRedstoneGate.selectedIO ^ (GuiRedstoneGate.negatedIO | ~ GuiRedstoneGate.selectedIO) & bit);
            GuiRedstoneGate.negatedIO = temp;
            return null;
        }
        do {
            this.inputMask = (byte)(this.inputMask ^ this.outputMask & bit);
            this.outputMask = (byte)(this.outputMask ^ bit);
        } while (isInvalidConfig((int)this.inputMask, (int)this.outputMask));
        return null;
    }

    public void setInventorySlotContents(int i, ItemStack itemstack) {
    }

    @Override
    public String getName() {
        return "Redstone Gate";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        if (worldObj.getTileEntity(getPos()) != this) {
            return false;
        }
        if (entityplayer.getDistanceSq(getPos().add(0.5f, 0.5f, 0.5f)) > 64.0) return false;
        return true;
    }

    // used to be: public ItemStack b(int var1) = func_48081_b. Best guess: removeStackFromSlot
    @Override
    public ItemStack removeStackFromSlot(int index) {
        return null;
    }

    @Override
    public void openInventory(EntityPlayer player) { }

    @Override
    public void closeInventory(EntityPlayer player) { }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return false; // Don't permit items being placed
    }

    // @TODO: Implement. I guess these are for real inventories, but we want to discard anything?

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        //return super.shouldRefresh(world, pos, oldState, newSate);
        return false;
    }
}
