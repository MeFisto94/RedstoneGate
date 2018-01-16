package com.github.mefisto94.RedstoneGate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerRedstoneGate extends Container {
    private TileEntityRedstoneGate entityGate;

    public ContainerRedstoneGate(InventoryPlayer inventoryplayer, TileEntityRedstoneGate tileentity) {
        this.entityGate = tileentity;
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return this.entityGate.canInteractWith(entityplayer);
    }

    public ItemStack slotClick(int i, int j, boolean flag, EntityPlayer entityplayer) {
        int n;
        if (i < 0) return null;
        if (inventorySlots.size() <= i) {
            return null;
        }
        Slot slot = (Slot)inventorySlots.get(i);
        if (slot == null) {
            return null;
        }
        if (flag) {
            n = 1;
            return slot.decrStackSize(n);
        }
        n = 0;
        return slot.decrStackSize(n);
    }

    /**
     * Convenience method to expose {@link #addSlotToContainer(Slot)} which used to be available as addSlot
     * @param slot The Slot to add
     * @return The Slot which has been added
     */
    public Slot addSlot(Slot slot) {
        return addSlotToContainer(slot);
    }
}
