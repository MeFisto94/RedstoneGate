package com.github.mefisto94.RedstoneGate.network;

import com.github.mefisto94.RedstoneGate.TileEntityRedstoneGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class UpdateGateMessageHandler implements IMessageHandler<UpdateGateMessage, IMessage> {

    @Override
    public IMessage onMessage(UpdateGateMessage message, MessageContext ctx) {
        EntityPlayerMP serverPlayer = ctx.getServerHandler().playerEntity;

        if (message.isValid(serverPlayer.getServerWorld(), serverPlayer.getPosition())) {
            // Execute the action on the main server thread by adding it as a scheduled task
            serverPlayer.getServerWorld().addScheduledTask(() -> {
                BlockPos pos = message.getPos();
                IBlockState state = serverPlayer.getServerWorld().getBlockState(pos);
                TileEntity te = serverPlayer.getServerWorld().getTileEntity(pos);

                if (te != null && te instanceof TileEntityRedstoneGate) {
                    //TileEntityRedstoneGate terg = (TileEntityRedstoneGate)te;
                    te.handleUpdateTag(message.getTag());

                    // Update on all clients
                    serverPlayer.getServerWorld().notifyBlockUpdate(message.getPos(), state, state, 3);
                    //serverPlayer.inventory.addItemStackToInventory(new ItemStack(Items.DIAMOND, 64));
                }
            });
        }

        // No response packet
        return null;
    }
}
