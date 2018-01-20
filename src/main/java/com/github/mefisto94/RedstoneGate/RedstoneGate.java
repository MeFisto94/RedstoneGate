package com.github.mefisto94.RedstoneGate;

import com.github.mefisto94.RedstoneGate.block.BlockRedstoneGate;
import com.github.mefisto94.RedstoneGate.gui.GUIHandler;
import com.github.mefisto94.RedstoneGate.network.RedstoneGatePacketHandler;
import com.github.mefisto94.RedstoneGate.network.UpdateGateMessage;
import com.github.mefisto94.RedstoneGate.network.UpdateGateMessageHandler;
import com.github.mefisto94.RedstoneGate.tileentity.TileEntityRedstoneGate;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.logging.Level;
import java.util.logging.Logger;

@Mod(modid = RedstoneGate.MODID, version = RedstoneGate.VERSION)
public class RedstoneGate
{
    public static final String MODID = "redstonegate";
    public static final String VERSION = "1.0";
    public static final Logger LOG = Logger.getLogger(RedstoneGate.class.getSimpleName());
    public static final String docURL = "https://github.com/MeFisto94/RedstoneGate/tree/master/docs/";

    // Configuration
    public static Configuration config;
    public static int conf_colorOn  = 0x006000;
    public static int conf_colorOff = 0x600000;
    public static int conf_colorIO  = 0x606000;
    public static boolean self_triggering = true;
    public static boolean honor_delay = false;
    public static int maxIterationDepth = 30;
    public static boolean notifyChatOnOverflow = true;

    // Blocks
    public static final BlockRedstoneGate BLOCK_REDSTONE_GATE = new BlockRedstoneGate();
    public static final ItemBlock ITEM_REDSTONE_GATE = new ItemBlock(BLOCK_REDSTONE_GATE);

    public static RedstoneGate instance;

    public RedstoneGate() {
        instance = this;
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @EventHandler
    public void PreInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig();

        GameRegistry.registerTileEntity(TileEntityRedstoneGate.class, TileEntityRedstoneGate.class.getSimpleName());
        GameRegistry.register(BLOCK_REDSTONE_GATE);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GUIHandler());
        ITEM_REDSTONE_GATE.setRegistryName(BLOCK_REDSTONE_GATE.getRegistryName());
        GameRegistry.register(ITEM_REDSTONE_GATE);
        RedstoneGatePacketHandler.INSTANCE.registerMessage(UpdateGateMessageHandler.class, UpdateGateMessage.class,
            RedstoneGatePacketHandler.PACKET_ID++, Side.SERVER);
    }

    @EventHandler
    @SideOnly(Side.CLIENT)
    public void PreInit_ClientOnly(FMLPreInitializationEvent event) {
    }

    @EventHandler
    @SideOnly(Side.CLIENT)
    public void Init_ClientOnly(FMLInitializationEvent event) {
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        System.out.println("REGISTER BLOCKS");
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        System.out.println("REGISTER ITEMS");
    }

    public static void syncConfig() {
        try {
            config.load();
            Property propColorOn = config.get(Configuration.CATEGORY_CLIENT, "colorOn", 0x006000,
                    "Which Color to use for Enabled Elements");
            Property propColorOff = config.get(Configuration.CATEGORY_CLIENT, "colorOff", 0x600000,
                    "Which Color to use for Disabled Elements");
            Property propColorIO = config.get(Configuration.CATEGORY_CLIENT, "colorIO", 0x606000,
                    "Which Color to use for IO Elements");
            Property propIOAllowSelfTrigger = config.get(Configuration.CATEGORY_GENERAL, "io.self-triggering.allowed",
                    true, "Allow Ports to be both I and O, so we handle the case of self-triggering." +
                    " Default: true. See " + docURL + "io-config.md for more info");
            Property propIOHonorDelay = config.get(Configuration.CATEGORY_GENERAL, "io.honor.delay", false,
                    "When self-triggering IO Ports, honor the block's delay setting? Only relevant when " +
                    "io.self-triggering.allowed is true. Default: false. See " + docURL + "io-config.md for more info");
            Property propIOMaxIterationDepth = config.get(Configuration.CATEGORY_GENERAL, "io.iteration-depth.max",
                    30, "When io.honor.delay is set to false, the truth table is able to trigger " +
                    "itself in the same frame. This can lead to infinity-loops, if no steady state is reached. " +
                    "This property defines after how many loop iterations the calculation quits. A lower number " +
                    "reduces the server-load in case of failure, but limits the possible uses. Higher numbers " +
                    "rarely make sense. Default: 30. See " + docURL + "io-config.md for more info");
            Property propIONotifyChatOnIterationOverflow = config.get(Configuration.CATEGORY_GENERAL,
                    "io.iteration-overflow.notify-chat", true, "When the io.iteration-depth.max " +
                    "is exceeded print a notification to the chat, so it can be fixed. Warning: If the circuit is pulsed" +
                    " at a high rate, this might spam your chat. Default: true.");

            conf_colorOn = propColorOn.getInt();
            conf_colorOff = propColorOff.getInt();
            conf_colorIO = propColorIO.getInt();
            self_triggering = propIOAllowSelfTrigger.getBoolean();
            honor_delay = propIOHonorDelay.getBoolean();
            maxIterationDepth = propIOMaxIterationDepth.getInt();
            notifyChatOnOverflow = propIONotifyChatOnIterationOverflow.getBoolean();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Exception thrown during the attempt to read the config!", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    public static byte boolean_to_bit(boolean b) {
        return b ? (byte)0x1 : (byte)0x0;
    }

    public static boolean bit_to_boolean(byte b) {
        return (b & 0x1) != 0;
    }

    public static byte diff_in_bits(byte old, byte nu) {
        return (byte)((nu ^ old) & 0xFF);
    }

    public static boolean haveBitsChanged(byte old, byte nu) {
        return diff_in_bits(old, nu) != 0;
    }
}
