package com.github.mefisto94.RedstoneGate.tileentity;

import com.github.mefisto94.RedstoneGate.gui.GuiRedstoneGate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TileEntityRedstoneGate extends InventoryProviderTileEntity {
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

    /* CONFIG-START */
    private boolean isInvalidConfig(int inputMask, int outputMask) {
        int output_count = this.hammingWeight(outputMask & 63);
        int combinations = (1 << this.hammingWeight(inputMask & 63)) * output_count;
        return (output_count == 0 || combinations > 32);
    }

    public String getConfigString() {
        //int meta = worldObj.getBlockMetadata(getPos().getX(), getPos().getY(), getPos().getZ());
        return String.format("%02x%02x-%08x-%01x", inputMask & 63, outputMask & 63, truthTable, delay % 16);
    }

    public boolean setConfigString(String s) {
        try {
            int im = Integer.parseInt(s.substring(0, 2), 16) & 63 | inputMask & 192;
            int om = Integer.parseInt(s.substring(2, 4), 16) & 63 | outputMask & 192;
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
            if ((inputMask & i) != 0x0) {
                sInput = (byte)(sInput << 1 | sb);
                nInput = (byte)(nInput << 1 | nb);
                bitcount <<= 1;
            }
            if ((outputMask & i) != 0x0) {
                sOutput = (byte)(sOutput << 1 | sb);
                nOutput = (byte)(nOutput << 1 | nb);
            }
        }
        long table = truthTable & 0xFFFFFFFFL;
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
    /* CONFIG-END */

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
     * @param world The World Element
     * @param pos The BlockPosition
     * @param direction The Direction which is queried as input (in Mod Format, which is opposite)
     * @return Whether this side is powered
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
        int[] dirmap = relative_to_absolute_direction[(inputMask & 0xFF) >> 6];

        /* Iterate over all directions */
        for (int d = 5; d >= 0; d--) {
            if ((inputMask & (1 << d)) == 0) { // Not an input
                continue;
            }

            // Guess: bitcount is the mask for the highest set bit in index
            bitcount = (byte)(bitcount * 2); // double the bitcount for each input?
            index = index << 1 | // "Make space by shifting"
                    (isSidePowered(world, pos, EnumFacing.VALUES[dirmap[d]]) ? 1 : 0) | // If we have an input there
                    (outputVector >> dirmap[d]) & 1; // or we wanted to ouput there already
                    // Note: for IO's which are triggered "NOW", outputVector still is zero
        }

        outputVector = 0;
        long table = truthTable & 0xFFFFFFFFL; // Prevents expansion of the negate sign

        /* Iterate again over all directions, but this time inverted in order */
        for (int d = 0; d < 6; d++) {
            if ((outputMask & (1 << d)) != 0) { // Is this an output
                outputVector |= (byte)((  // is this index set? move it to it's position
                    (table >> index) // table content for index
                    & 0x1) // only get the last bit
                    << dirmap[d]); // move it to the appropriate place in the output vector
                index += bitcount; // load the next value in index.
            }
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

    public ItemStack decrStackSize(int i, int j) {
        if (i < 0) return null;
        if (this.getSizeInventory() < i) {
            return null;
        }
        if (i == 47) {
            String s = GuiRedstoneGate.copypastebuffer[j];
            GuiRedstoneGate.setStatusMessage((setConfigString(s) ? String.format("Pasted %s from slot %d", s, j) : ""));
            return null;
        }
        if (i == 46) {
            String s;
            GuiRedstoneGate.copypastebuffer[j] = s = getConfigString();
            GuiRedstoneGate.setStatusMessage(String.format("Copied %s to slot %d", s, j));
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

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return false; // Blockstate Changes (ON/OFF) shall not lead to a new TileEntity
    }
}
