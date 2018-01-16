package com.github.mefisto94.RedstoneGate;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

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
    public byte inputMask = 0;
    public byte outputMask = 63;
    public int truthTable = 0;
    public byte outputVector = 0;
    public boolean canUpdate = true;

    private boolean isInvalidConfig(int inputMask, int outputMask) {
        int outputcount = this.hammingWeight(outputMask & 63);
        int combinations = (1 << this.hammingWeight(inputMask & 63)) * outputcount;
        if (outputcount == 0) return true;
        if (32 < combinations) return true;
        return false;
    }

    public String getConfigString() {
        int meta = worldObj.getBlockMetadata(getPos().getX(), getPos().getY(), getPos().getZ());
        return String.format("%02x%02x-%08x-%01x", this.inputMask & 63, this.outputMask & 63, this.truthTable, meta);
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
            this.inputMask = (byte)im;
            this.outputMask = (byte)om;
            this.truthTable = (int)tt;
            worldObj.setBlockMetadata(getPos().getX(), getPos().getY(), getPos().getZ(), meta);
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

    private boolean isPoweredWire(World world, int x, int y, int z) {
        if (world.getBlockId(x, y, z) != BLOCK_REDSTONE_WIRE) return false;
        if (world.getBlockMetadata(x, y, z) == 0) return false;
        return true;
    }

    private boolean isSidePowered(World world, int x, int y, int z, int direction) {
        switch (direction) {
            case 0: {
                if (this.isPoweredWire(world, x, y + 1, z)) return true;
                if (world.markBlockAsNeedsUpdate(x, y + 1, z, 1)) return true;
                return false;
            }
            case 1: {
                if (this.isPoweredWire(world, x, y - 1, z)) return true;
                if (world.markBlockAsNeedsUpdate(x, y - 1, z, 0)) return true;
                return false;
            }
            case 2: {
                if (this.isPoweredWire(world, x, y, z + 1)) return true;
                if (world.markBlockAsNeedsUpdate(x, y, z + 1, 3)) return true;
                return false;
            }
            case 3: {
                if (this.isPoweredWire(world, x, y, z - 1)) return true;
                if (world.markBlockAsNeedsUpdate(x, y, z - 1, 2)) return true;
                return false;
            }
            case 4: {
                if (this.isPoweredWire(world, x + 1, y, z)) return true;
                if (world.markBlockAsNeedsUpdate(x + 1, y, z, 5)) return true;
                return false;
            }
            case 5: {
                if (this.isPoweredWire(world, x - 1, y, z)) return true;
                if (world.markBlockAsNeedsUpdate(x - 1, y, z, 4)) return true;
                return false;
            }
        }
        return false;
    }

    public void RecomputeOutput(World world, int i, int j, int k) {
        int index = 0;
        int bitcount = 1;
        int[] dirmap = relative_to_absolute_direction[(this.inputMask & 255) >> 6];
        for (int d = 5; 0 <= d; --d) {
            if ((this.inputMask & 1 << d) == 0) continue;
            bitcount = (byte)(bitcount * 2);
            index = index << 1 | (this.isSidePowered(world, i, j, k, dirmap[d]) ? 1 : 0) | this.outputVector >> dirmap[d] & 1;
        }
        this.outputVector = 0;
        long table = this.truthTable & -1;
        int d2 = 0;
        while (d2 < 6) {
            if ((this.outputMask & 1 << d2) != 0) {
                this.outputVector = (byte)(this.outputVector | (byte)((table >> index & 1) << dirmap[d2]));
                index += bitcount;
            }
            ++d2;
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setByte("inputs", this.inputMask);
        nbttagcompound.setByte("outputs", this.outputMask);
        nbttagcompound.setInteger("table", this.truthTable);
        return nbttagcompound;
    }

    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        this.inputMask = nbttagcompound.getByte("inputs");
        this.truthTable = nbttagcompound.getInteger("table");
        if (nbttagcompound.hasKey("outputs")) {
            this.outputMask = nbttagcompound.getByte("outputs");
            return;
        }
        this.outputMask = (byte)(~ this.inputMask & 63);
    }

    @Override
    public int getSizeInventory() {
        return 48;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i != 45) return null;
        return new ItemStack(mod_RedstoneGate.redstoneGate);
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
            int delay = (getBlockMetadata() + (j == 0 ? 1 : 15)) % 16;
            worldObj.setMetadata(getPos().getX(), getPos().getY(), getPos().getZ(), delay);
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
        byte bit = (byte)(1 << i - 32);
        if (j == 1) {
            byte temp = (byte)(GuiRedstoneGate.negatedIO ^ GuiRedstoneGate.selectedIO & bit);
            GuiRedstoneGate.selectedIO = (byte)(GuiRedstoneGate.selectedIO ^ (GuiRedstoneGate.negatedIO | ~ GuiRedstoneGate.selectedIO) & bit);
            GuiRedstoneGate.negatedIO = temp;
            return null;
        }
        do {
            this.inputMask = (byte)(this.inputMask ^ this.outputMask & bit);
            this.outputMask = (byte)(this.outputMask ^ bit);
        } while (this.isInvalidConfig((int)this.inputMask, (int)this.outputMask));
        return null;
    }

    public void setInventorySlotContents(int i, ItemStack itemstack) {
    }

    @Override
    public String getName() {
        return "Redstone gate";
    }

    public int getInventoryStackLimit() {
        return 1;
    }

    public void onInventoryChanged() {
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        if (worldObj.getBlockTileEntity(getPos().getX(), getPos().getY(), getPos().getZ()) != this) {
            return false;
        }
        if (entityplayer.getDistanceSq((double)getPos().getX() + 0.5, (double)getPos().getY() + 0.5, (double)getPos().getZ() + 0.5) > 64.0) return false;
        return true;
    }

    public boolean a_(EntityPlayer entityplayer) {
        if (worldObj.getBlockTileEntity(getPos().getX(), getPos().getY(), getPos().getZ()) != this) {
            return false;
        }
        if (entityplayer.getDistanceSq((double) getPos().getX() + 0.5, (double)getPos().getY() + 0.5, (double) getPos().getZ() + 0.5) > 64.0) return false;
        return true;
    }

    // used to be: public ItemStack b(int var1) = func_48081_b. Best guess: removeStackFromSlot
    @Override
    public ItemStack removeStackFromSlot(int index) {
        return null;
    }

    @Override
    public void openChest() {
    }

    @Override
    public void closeChest() {
    }


}
