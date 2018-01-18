package com.github.mefisto94.RedstoneGate.network;

import com.github.mefisto94.RedstoneGate.RedstoneGate;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class RedstoneGatePacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(RedstoneGate.MODID);
    public static int PACKET_ID = 0; // see http://mcforge.readthedocs.io/en/latest/networking/simpleimpl/#registering-packets

}
