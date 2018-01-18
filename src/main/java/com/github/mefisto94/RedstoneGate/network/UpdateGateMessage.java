package com.github.mefisto94.RedstoneGate.network;

import com.github.mefisto94.RedstoneGate.TileEntityRedstoneGate;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class UpdateGateMessage implements IMessage {
    BlockPos pos;
    NBTTagCompound tag;

    /**
     * Default constructor, only used for serialization.
     * DO NOT USE!!
     */
    public UpdateGateMessage() {
    }

    /**
     * Constructs an UpdateGateMessage, which tells the Server that we've changed the block locally.
     * It will lead to a synchronization of block data.
     *
     * @param pos The Position of our gate.
     * @param tag The serialized TileEntity
     */
    public UpdateGateMessage(BlockPos pos, NBTTagCompound tag) {
        this.pos = pos;
        this.tag = tag;
    }

    /**
     * Constructs an UpdateGameMessage, which tells the Server that we've changed the block locally.
     * it will lead to a synchronization of block data.
     *
     * @param tileEntityRedstoneGate The TileEntity which has changed
     */
    public UpdateGateMessage(TileEntityRedstoneGate tileEntityRedstoneGate) {
        this(tileEntityRedstoneGate.getPos(), tileEntityRedstoneGate.getUpdateTag());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        tag = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        ByteBufUtils.writeTag(buf, tag);
    }

    protected boolean isValid(World worldIn, BlockPos playerPosition) {
        // .add(0.5f, 0.5f, 0.5f) to be coherent with the client (and to check from the block's center)
        return worldIn.isBlockLoaded(pos) && playerPosition.distanceSq(pos.add(0.5f, 0.5f, 0.5f)) < 64;
    }

    /**
     * Gets the Position of the Block which has to be synchronized
     * @return The Block Position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Gets the NBT Tag representing the {@link com.github.mefisto94.RedstoneGate.TileEntityRedstoneGate }
     * @return The NBT Tag
     */
    public NBTTagCompound getTag() {
        return tag;
    }
}
