package com.github.mefisto94.RedstoneGate;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import org.lwjgl.opengl.GL11;

public class GuiRedstoneGate extends GuiContainer {
    public static final String BACKGROUND_IMAGE = "/gui/redstonegate.png";
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 170;
    public static final int FIRST_GRID_ID = 0;
    public static final int FIRST_INPUT_ID = 32;
    public static final int FIRST_CONFIG_ID = 38;
    public static final int DELAY_ID = 44;
    public static final int GATE_IMAGE_ID = 45;
    public static final int CONFIG_COPY_ID = 46;
    public static final int CONFIG_PASTE_ID = 47;
    public static final int CELLS_PER_ROW = 4;
    public static final int CELL_WIDTH = 18;
    public static final int CELL_HEIGHT = 18;
    public static final int X_GRID_LEFT = 98;
    public static final int Y_GRID_TOP = 20;
    public static final int X_CONFIG_LEFT = 8;
    public static final int Y_CONFIG_TOP = 110;
    public static final int X_INPUT_LEFT = 8;
    public static final int Y_INPUT_TOP = 38;
    public static final int X_DELAY = 71;
    public static final int Y_DELAY = 146;
    public static final int[][] input_locations;
    public static final int[][] output_images;
    public static final int[][] input_images;
    public static final int[][] input_output_images;
    public static final int X_ROW_HEADER = 71;
    public static final int X_GRID_TEXT = 99;
    public static final int Y_GRID_TEXT = 26;
    public static final int CL_GREY = 4210752;
    public static final String PADDING = "00000";
    public static byte selectedIO;
    public static byte negatedIO;
    public static String[] copypastebuffer;
    private TileEntityRedstoneGate entityGate;
    private String column_header;
    private String[] sides;
    private String[] quickConfigLabels;
    private String[] outputs;
    private int outputcount;
    private int rows_per_output;
    private int input_range;
    private ContainerRedstoneGate container;
    private static String statusbar;
    private static int status_timer;
    private static final String allowedCodeChars = "0123456789ABCDEFabcdef";
    private static final String CODE_DISPLAY = "Enter code: %s%s";
    private static final String CODE_TEMPLATE = "____-________-_";
    private static final int CODE_LENGTH;
    private boolean code_entry;
    private String gate_code;
    private String code_display;
    private int code_index;

    public GuiRedstoneGate(final EntityPlayer entityplayer, final TileEntityRedstoneGate tileentity) {
        super((Container)new ContainerRedstoneGate(entityplayer.inventory, tileentity));
        this.column_header = "";
        this.sides = new String[] { "Right", "Left", "Back", "Front", "Down", "Up" };
        this.quickConfigLabels = new String[] { "and", "or", "xor", "neg", "on", "off" };
        this.outputs = new String[6];
        this.outputcount = 0;
        this.rows_per_output = 0;
        this.input_range = 0;
        this.entityGate = tileentity;
        GuiRedstoneGate.selectedIO = 63;
        GuiRedstoneGate.negatedIO = 0;
        this.xSize = 176;
        this.ySize = 170;
        this.container = (ContainerRedstoneGate)inventorySlots;
        GuiRedstoneGate.status_timer = 0;
        GuiRedstoneGate.statusbar = "";
        this.code_entry = false;
        this.code_index = 0;
        this.gate_code = "";
        this.code_display = String.format("", "____-________-_");
    }

    public static void setStatusMessage(final String s) {
        GuiRedstoneGate.statusbar = s;
        GuiRedstoneGate.status_timer = 256;
    }

    protected void updateNeighbours(final int x, final int y, final int z, final int blockID) {
        mc.theWorld.notifyBlocksOfNeighborChange(x, y - 1, z, blockID);
        mc.theWorld.notifyBlocksOfNeighborChange(x, y + 1, z, blockID);
        mc.theWorld.notifyBlocksOfNeighborChange(x - 1, y, z, blockID);
        mc.theWorld.notifyBlocksOfNeighborChange(x + 1, y, z, blockID);
        mc.theWorld.notifyBlocksOfNeighborChange(x, y, z - 1, blockID);
        mc.theWorld.notifyBlocksOfNeighborChange(x, y, z + 1, blockID);
        mc.theWorld.notifyBlocksOfNeighborChange(x, y, z, blockID);
    }

    private void codeKeyTyped(final char c, final int i) {
        if (i == 1) {
            this.code_entry = false;
            setStatusMessage("");
        }
        else if (i == mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.code_entry = false;
            this.a(c, i);
        }
        else if (i == 14 && 0 < this.code_index) {
            --this.code_index;
            if (this.gate_code.charAt(this.code_index) == '-') {
                --this.code_index;
            }
            this.gate_code = this.gate_code.substring(0, this.code_index);
            this.code_display = String.format("Enter code: %s%s", this.gate_code, "____-________-_".substring(this.code_index, GuiRedstoneGate.CODE_LENGTH));
        }
        else if (i == 28 && this.code_index == GuiRedstoneGate.CODE_LENGTH) {
            this.code_entry = false;
            if (this.entityGate.setConfigString(this.gate_code)) {
                setStatusMessage("Code accepted");
            }
            else {
                setStatusMessage("Invalid code");
            }
        }
        else if (this.code_index < GuiRedstoneGate.CODE_LENGTH && 0 <= "0123456789ABCDEFabcdef".indexOf(c)) {
            this.gate_code += c;
            ++this.code_index;
            if (this.code_index < GuiRedstoneGate.CODE_LENGTH && "____-________-_".charAt(this.code_index) == '-') {
                this.gate_code += '-';
                ++this.code_index;
            }
            this.code_display = String.format("Enter code: %s%s", this.gate_code, (this.code_index < GuiRedstoneGate.CODE_LENGTH) ? "____-________-_".substring(this.code_index, GuiRedstoneGate.CODE_LENGTH) : "");
        }
    }

    protected void a(final char c, final int i) {
        if (this.code_entry) {
            this.codeKeyTyped(c, i);
        }
        else if (i == 1 || i == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.thePlayer.closeScreen();
            this.updateNeighbours(this.entityGate.getPos().getX(), this.entityGate.getPos().getY(), this.entityGate.getPos().getZ(), mod_RedstoneGate.blockID);
        }
        else if (0 <= "0123456789ABCDEFabcdef".indexOf(c)) {
            this.code_entry = true;
            this.code_index = 0;
            this.gate_code = "";
            this.codeKeyTyped(c, i);
        }
        else if (i == 14) {
            this.code_entry = true;
            this.code_index = GuiRedstoneGate.CODE_LENGTH;
            this.gate_code = this.entityGate.getConfigString();
            this.code_display = String.format("Enter code: %s%s", this.gate_code, "");
        }
    }

    private void drawCellString(final String s, final int x, final int y, final int c) {
        final int scaledx = 4 * x / 3;
        final int scaledy = 4 * y / 3;
        switch (s.length()) {
            case 4: {
                fontRendererObj.drawString(s.substring(0, 2), scaledx, scaledy - 5, c);
                fontRendererObj.drawString(s.substring(2, 4), scaledx, scaledy + 5, c);
                break;
            }
            case 5: {
                fontRendererObj.drawString(s.substring(0, 3), scaledx, scaledy - 5, c);
                fontRendererObj.drawString(s.substring(3, 5), scaledx, scaledy + 5, c);
                break;
            }
            default: {
                fontRendererObj.drawString(s, scaledx, scaledy, c);
                break;
            }
        }
    }

    private void drawGridHeaders() {
        for (int j = 0; j < this.outputcount; ++j) {
            fontRendererObj.drawString(this.outputs[j], 94, 4 * (26 + j * 18 * this.rows_per_output) / 3, 4210752);
        }
        final int offset = (18 - this.computeCellContentWidth(this.column_header)) / 2;
        for (int i = 0; i < 4; ++i) {
            this.drawCellString(this.column_header, 99 + offset + 18 * i, 9, 4210752);
        }
    }

    private int computeCellContentWidth(final String s) {
        switch (s.length()) {
            case 0: {
                return fontRendererObj.getStringWidth("0");
            }
            case 4: {
                return fontRendererObj.getStringWidth("00");
            }
            case 5: {
                return fontRendererObj.getStringWidth("000");
            }
            default: {
                return fontRendererObj.getStringWidth(s);
            }
        }
    }

    public void addConfigSlots() {
        for (int i = 0; i < 6; ++i) {
            container.addSlot(new Slot((IInventory)this.entityGate, 38 + i, 8 + 18 * (i % 3), 110 + 18 * (i / 3)));
            final String s = this.quickConfigLabels[i];
            final int xOffset = 18 * (i % 3) + (18 - fontRendererObj.getStringWidth(s)) / 2 + 2;
            final int yOffset = 18 * (i / 3) + 4 + 2;
            fontRendererObj.drawString(s, 4 * (8 + xOffset) / 3, 4 * (110 + yOffset) / 3, 4210752);
        }
        container.addSlot(new Slot((IInventory)this.entityGate, 46, 8, 146));
        container.addSlot(new Slot((IInventory)this.entityGate, 47, 26, 146));
        int xOffset = (18 - this.computeCellContentWidth("Cpy")) / 2 + 2;
        final int yOffset = 42;
        this.drawCellString("Cpy", 8 + xOffset, 110 + yOffset, 4210752);
        xOffset = (18 - this.computeCellContentWidth("Pst")) / 2 + 2;
        this.drawCellString("Pst", 26 + xOffset, 110 + yOffset, 4210752);
    }

    public void addDelaySlot() {
        final String d = Integer.toString(this.entityGate.getBlockMetadata());
        final int offset = (18 - fontRendererObj.getStringWidth(d)) / 2;
        fontRendererObj.drawString("Delay:", 4 * (99 - this.computeCellContentWidth("Delay") - 36) / 3, 202, 4210752);
        container.addSlot(new Slot((IInventory)this.entityGate, 44, 71, 146));
        fontRendererObj.drawString(d, 4 * (71 + offset) / 3, 202, 4210752);
    }

    public void addInputSlots() {
        for (int i = 0; i < 6; ++i) {
            container.addSlot(new Slot((IInventory)this.entityGate, 32 + i, 8 + GuiRedstoneGate.input_locations[i][0] * 18, 38 + GuiRedstoneGate.input_locations[i][1] * 18));
        }
        container.addSlot(new Slot((IInventory)this.entityGate, 45, 26, 56));
    }

    public void addGridSlot(final int col, final int row, final int index) {
        container.addSlot(new Slot((IInventory)this.entityGate, index, 98 + 18 * col, 20 + 18 * row));
    }

    private void drawGridCells() {
        this.container.inventorySlots.clear();
        this.addInputSlots();
        this.addConfigSlots();
        this.addDelaySlot();
        int s_width = this.column_header.length();
        if (s_width == 0) {
            s_width = 1;
        }
        final int offset = (18 - this.computeCellContentWidth(this.column_header)) / 2;
        int index = 0;
        for (int j = 0; j < this.outputcount; ++j) {
            for (int i = 0; i < this.input_range; ++i) {
                this.drawGridCell(i, j, offset, s_width);
                this.addGridSlot(i % 4, i / 4 + j * this.rows_per_output, index);
                ++index;
            }
        }
    }

    private void drawGridCell(final int input, final int output, final int offset, final int width) {
        final boolean isSet = (this.entityGate.truthTable >> this.input_range * output + input & 0x1) != 0x0;
        final String s = Integer.toBinaryString(input);
        this.drawCellString("00000".substring(0, width - s.length()) + s, 99 + offset + input % 4 * 18, 26 + (output * this.rows_per_output + input / 4) * 18, isSet ? RedstoneGate.conf_colorOn : RedstoneGate.conf_colorOff);
    }

    private void drawInputString(final int input, final int x, final int y, final boolean isOutput, final boolean isInput) {
        GL11.glPopMatrix();
        // @TODO: Maybe .loadTexture first?
        final int i = mc.renderEngine.getTexture("/gui/redstonegate.png").getGlTextureId();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        mc.renderEngine.bindTexture(i);
        if (isOutput || isInput) {
            final int bit = 1 << input;
            if ((GuiRedstoneGate.selectedIO & bit) != 0x0) {
                final int[] images = (isOutput ? (isInput ? GuiRedstoneGate.input_output_images : GuiRedstoneGate.output_images) : GuiRedstoneGate.input_images)[input];
                int ix = images[0];
                final int iy = images[1];
                if ((GuiRedstoneGate.negatedIO & bit) != 0x0) {
                    ix += 18;
                }
                drawTexturedModalRect(x - 1, y - 1, ix, iy, 18, 18);
            }
        }
        GL11.glPushMatrix();
        GL11.glScalef(0.65f, 0.65f, 1.0f);
        final int scaledX = 20 * x / 13;
        final int scaledY = 20 * y / 13;
        final boolean toprow = GuiRedstoneGate.input_locations[input][1] == 0;
        final int color = isOutput ? (isInput ? RedstoneGate.conf_colorIO : RedstoneGate.conf_colorOff) : (isInput ? RedstoneGate.conf_colorOn : 4210752);
        fontRendererObj.drawString(this.sides[input], scaledX, scaledY + (toprow ? -9 : 28), color);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glScalef(0.75f, 0.75f, 1.0f);
    }

    private void drawTruthTable(final int inputMask, final int outputMask) {
        GL11.glPushMatrix();
        GL11.glScalef(0.75f, 0.75f, 1.0f);
        this.outputcount = 0;
        this.column_header = "";
        this.input_range = 1;
        for (int i = 0; i < 6; ++i) {
            final boolean isOutput = (outputMask & 1 << i) != 0x0;
            final boolean isInput = (inputMask & 1 << i) != 0x0;
            this.drawInputString(i, 8 + GuiRedstoneGate.input_locations[i][0] * 18, 38 + GuiRedstoneGate.input_locations[i][1] * 18, isOutput, isInput);
            if (isOutput) {
                this.outputs[this.outputcount] = this.sides[i];
                ++this.outputcount;
            }
            if (isInput) {
                this.column_header = this.sides[i].charAt(0) + this.column_header;
                this.input_range <<= 1;
            }
        }
        this.rows_per_output = ((this.input_range < 4) ? 1 : (this.input_range / 4));
        this.drawGridHeaders();
        this.drawGridCells();
        GL11.glPopMatrix();
    }

    protected void d() {
        fontRendererObj.drawString("Redstone gate", 8, 8, 4210752);
        GL11.glPushMatrix();
        GL11.glScalef(0.75f, 0.75f, 1.0f);
        fontRendererObj.drawString("Inputs/Outputs", 10, 34, RedstoneGate.conf_colorOff);
        fontRendererObj.drawString("Inputs", 10, 34, RedstoneGate.conf_colorOn);
        fontRendererObj.drawString("Auto config", 10, 134, 4210752);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glScalef(0.5f, 0.5f, 1.0f);
        fontRendererObj.drawString("(0=off, 1=on)", 268, 326, 4210752);
        if (this.code_entry) {
            setStatusMessage(this.code_display);
        }
        if (0 < GuiRedstoneGate.status_timer) {
            --GuiRedstoneGate.status_timer;
            fontRendererObj.drawString(GuiRedstoneGate.statusbar, 16, 326, 4210752);
        }
        GL11.glPopMatrix();
        this.drawTruthTable(this.entityGate.inputMask, this.entityGate.outputMask);
    }

    protected void a(final float f, final int i, final int j) {
        final int t = mc.renderEngine.getTexture("/gui/redstonegate.png");
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        mc.renderEngine.bindTexture(t);
        final int x = (width - xSize) / 2;
        final int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    }

    static {
        input_locations = new int[][] { { 2, 2 }, { 0, 0 }, { 0, 2 }, { 2, 0 }, { 1, 2 }, { 1, 0 } };
        output_images = new int[][] { { 176, 18 }, { 212, 18 }, { 212, 18 }, { 176, 18 }, { 212, 0 }, { 176, 0 } };
        input_images = new int[][] { { 212, 54 }, { 176, 54 }, { 176, 54 }, { 212, 54 }, { 176, 36 }, { 212, 36 } };
        input_output_images = new int[][] { { 212, 72 }, { 212, 72 }, { 212, 72 }, { 212, 72 }, { 176, 72 }, { 176, 72 } };
        GuiRedstoneGate.selectedIO = 63;
        GuiRedstoneGate.negatedIO = 0;
        GuiRedstoneGate.copypastebuffer = new String[] { "003F-00000000-0", "0308-00000000-0" };
        CODE_LENGTH = "____-________-_".length();
    }
}
