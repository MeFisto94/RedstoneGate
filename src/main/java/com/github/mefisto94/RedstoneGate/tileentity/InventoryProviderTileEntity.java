package com.github.mefisto94.RedstoneGate.tileentity;

import com.github.mefisto94.RedstoneGate.RedstoneGate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/**
 * This class was not present in the original mod, but is used to reduce the size of the original
 * {@link TileEntityRedstoneGate} by offloading all these Inventory things
 */
public abstract class InventoryProviderTileEntity extends TileEntity implements IInventory {

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
    public String getName() {
        return "Redstone Gate";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
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
        if (i != 45) {
            return null;
        }
        return new ItemStack(RedstoneGate.BLOCK_REDSTONE_GATE);
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        if (worldObj.getTileEntity(getPos()) != this) {
            return false;
        }
        return !(entityplayer.getDistanceSq(getPos().add(0.5f, 0.5f, 0.5f)) > 64.0);
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

}
