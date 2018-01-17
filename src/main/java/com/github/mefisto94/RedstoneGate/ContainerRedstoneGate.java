package com.github.mefisto94.RedstoneGate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class ContainerRedstoneGate extends Container {
    private TileEntityRedstoneGate entityGate;

    public ContainerRedstoneGate(InventoryPlayer inventoryplayer, TileEntityRedstoneGate tileentity) {
        this.entityGate = tileentity;
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return this.entityGate.isUseableByPlayer(entityplayer);
    }

    @Nullable
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        if (slotId < 0) {
            return null;
        }

        if (inventorySlots.size() <= slotId) {
            return null;
        }

        Slot slot = inventorySlots.get(slotId);
        if (slot == null) {
            return null;
        }
        if (clickTypeIn == ClickType.PICKUP) {
            return slot.decrStackSize(1);
        } else {
            return slot.decrStackSize(0);
        }
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
