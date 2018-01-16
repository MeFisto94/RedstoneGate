package com.github.mefisto94.RedstoneGate;

import java.util.*;
import java.io.*;

import net.minecraft.block.Block;
import net.minecraft.client.*;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

public class mod_RedstoneGate extends BaseMod
{
    public static int renderID;
    public static final File cfgdir;
    public static final File cfgfile;
    public static final String CONFIG_HEADER;
    public static final Properties config;
    public static final int blockID;
    public static int oldBlockID;
    public static final int colourOn;
    public static final int colourOff;
    public static final int colourIO;
    public static final Block redstoneGate;
    public static Block oldGateBlock;

    private static int getConfigKey(final String key, final boolean isHex) {
        final String value = mod_RedstoneGate.config.getProperty(key);
        try {
            return isHex ? Integer.parseInt(value, 16) : Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            System.out.println("Configuration file content is invalid, reverting to defaults.");
            setDefaultConfig(mod_RedstoneGate.config);
            saveConfig(mod_RedstoneGate.config);
            return getConfigKey(key, isHex);
        }
    }

    private static void setDefaultConfig(final Properties properties) {
        properties.clear();
        properties.setProperty("blockID", "129");
        properties.setProperty("oldBlockID", "129");
        properties.setProperty("colourOn", "6000");
        properties.setProperty("colourOff", "600000");
        properties.setProperty("colourInputOutput", "606000");
    }

    private static void saveConfig(final Properties config) {
        try {
            final FileOutputStream out = new FileOutputStream(mod_RedstoneGate.cfgfile);
            config.store(out, mod_RedstoneGate.CONFIG_HEADER);
            out.close();
        }
        catch (IOException e) {
            System.out.println("Error while writing configuration file:");
            System.out.println(e.getMessage());
        }
    }

    private static void loadConfig(final Properties config) {
        try {
            final FileInputStream in = new FileInputStream(mod_RedstoneGate.cfgfile);
            config.load(in);
            in.close();
        }
        catch (IOException e) {
            System.out.println("Error while reading config file:");
            System.out.println(e.getMessage());
            setDefaultConfig(config);
        }
    }

    private static Properties getConfig() {
        final Properties config = new Properties();
        mod_RedstoneGate.cfgdir.mkdir();
        if (mod_RedstoneGate.cfgfile.exists()) {
            if (mod_RedstoneGate.cfgfile.canRead()) {
                loadConfig(config);
            }
            else {
                System.out.println("Could not read the configuration file, using defaults.");
                setDefaultConfig(config);
            }
        }
        else {
            setDefaultConfig(config);
            try {
                if (!mod_RedstoneGate.cfgfile.createNewFile()) {
                    System.out.println("Could not find nor create the configuration file, using defaults.");
                }
                else if (mod_RedstoneGate.cfgfile.canWrite()) {
                    saveConfig(config);
                }
                else {
                    System.out.println("Could not write configuration file.");
                }
            }
            catch (IOException e) {
                System.out.println("IO error:");
                System.out.println(e.getMessage());
            }
        }
        return config;
    }

    public void renderInvBlock(final RenderBlocks renderblocks, final Block block, final int i, final int j) {
        if (j == mod_RedstoneGate.renderID) {
            ((BlockRedstoneGate)block).renderAsItem = true;
            renderblocks.a(block, 0, 1.0f);
            ((BlockRedstoneGate)block).renderAsItem = false;
        }
    }

    public boolean renderWorldBlock(final RenderBlocks renderblocks, final IBlockAccess iblockaccess, final int x, final int y, final int z, final Block block, final int l) {
        if (l != mod_RedstoneGate.renderID) {
            return false;
        }
        final TileEntityRedstoneGate tileentity = (TileEntityRedstoneGate)iblockaccess.getBlockTileEntity(x, y, z);
        final int i2 = (tileentity.inputMask & 0xFF) >> 6;
        return this.renderBlockRedstoneGate(renderblocks, iblockaccess, block, x, y, z, i2);
    }

    public boolean renderBlockRedstoneGate(final RenderBlocks renderblocks, final IBlockAccess blockAccess, final Block block, final int x, final int y, final int z, final int i1) {
        renderblocks.renderStandardBlock(block, x, y, z);
        final Tessellator tessellator = Tessellator.getInstance();
        tessellator.setBrightness(block.getMixedBrightnessForBlock(blockAccess, x, y, z));
        tessellator.addVertex(1.0f, 1.0f, 1.0f);
        final int k2 = block.getBlockTextureFromSide(1);
        final int l1 = (k2 & 0xF) << 4;
        final int i2 = k2 & 0xF0;
        final double d5 = l1 / 256.0f;
        final double d6 = (l1 + 15.99f) / 256.0f;
        final double d7 = i2 / 256.0f;
        final double d8 = (i2 + 15.99f) / 256.0f;
        final double d9 = 0.5;
        double d10 = x + 1;
        double d11 = x + 1;
        double d12 = x + 0;
        double d13 = x + 0;
        double d14 = z + 0;
        double d15 = z + 1;
        double d16 = z + 1;
        double d17 = z + 0;
        final double d18 = y + d9;
        if (i1 == 2) {
            d11 = (d10 = x + 0);
            d13 = (d12 = x + 1);
            d17 = (d14 = z + 1);
            d16 = (d15 = z + 0);
        }
        else if (i1 == 3) {
            d13 = (d10 = x + 0);
            d12 = (d11 = x + 1);
            d15 = (d14 = z + 0);
            d17 = (d16 = z + 1);
        }
        else if (i1 == 1) {
            d13 = (d10 = x + 1);
            d12 = (d11 = x + 0);
            d15 = (d14 = z + 1);
            d17 = (d16 = z + 0);
        }
        tessellator.addVertexWithUV(d13, d18, d17, d5, d7);
        tessellator.addVertexWithUV(d12, d18, d16, d5, d8);
        tessellator.addVertexWithUV(d11, d18, d15, d6, d8);
        tessellator.addVertexWithUV(d10, d18, d14, d6, d7);
        return true;
    }

    public void load() {
        ModLoader.addName((Object)mod_RedstoneGate.redstoneGate, "Redstone Gate");
        ModLoader.registerBlock(mod_RedstoneGate.redstoneGate);
        ModLoader.addRecipe(new ItemStack(mod_RedstoneGate.redstoneGate, 1), new Object[] { "rrr", "rrr", "rrr", 'r', Items.REDSTONE });
        ModLoader.addRecipe(new ItemStack(Items.REDSTONE, 9), new Object[] { "r", 'r', mod_RedstoneGate.redstoneGate });
        ModLoader.registerTileEntity((Class)TileEntityRedstoneGate.class, "RedstoneGate");
        mod_RedstoneGate.renderID = ModLoader.getUniqueBlockModelID((BaseMod)this, true);
        if (mod_RedstoneGate.oldBlockID != mod_RedstoneGate.blockID) {
            ModLoader.registerBlock(mod_RedstoneGate.oldGateBlock = new BlockOldRedstoneGate(mod_RedstoneGate.oldBlockID).a("OldRedstoneGate"));
        }
    }

    public String getVersion() {
        return "1.4.3 Beta for Minecraft 1.2.3";
    }

    static {
        cfgdir = new File(Minecraft.getMinecraft()., "config");
        cfgfile = new File(mod_RedstoneGate.cfgdir, "RedstoneGate.cfg");
        CONFIG_HEADER = String.format("RedstoneGate configuration\n%s\n%s\n%s\n%s\n%s", "# Change blockID into a free block ID to resolve conflicts with other mods.", "# setting oldBlockID to any value different from blockID will assume that ID is a RedstoneGate block", "# and will replace it by the new blockID whenever the block is rendered.", "# The colourOn and colourOff values allow the colours of the truth table outputs to be modified", "# Format: RRGGBB, base 16 (i.e. hexadecimal)!");
        config = getConfig();
        blockID = getConfigKey("blockID", false);
        mod_RedstoneGate.oldBlockID = getConfigKey("oldBlockID", false);
        colourOn = getConfigKey("colourOn", true);
        colourOff = getConfigKey("colourOff", true);
        colourIO = getConfigKey("colourInputOutput", true);
        redstoneGate = new BlockRedstoneGate(mod_RedstoneGate.blockID).c(1.5f).a(0.0f).a("RedstoneGate");
        mod_RedstoneGate.oldGateBlock = null;
    }
}
