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

    // Configuration
    public static Configuration config;
    public static int conf_colorOn  = 0x006000;
    public static int conf_colorOff = 0x600000;
    public static int conf_colorIO  = 0x606000;

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
            Property propColorOn = config.get(Configuration.CATEGORY_CLIENT, "colorOn", 0x006000, "Which Color to use for Enabled Elements");
            Property propColorOff = config.get(Configuration.CATEGORY_CLIENT, "colorOff", 0x600000, "Which Color to use for Disabled Elements");
            Property propColorIO = config.get(Configuration.CATEGORY_CLIENT, "colorIO", 0x606000, "Which Color to use for IO Elements");

            conf_colorOn = propColorOn.getInt();
            conf_colorOff = propColorOff.getInt();
            conf_colorIO = propColorIO.getInt();
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
}
