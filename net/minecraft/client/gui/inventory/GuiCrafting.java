package net.minecraft.client.gui.inventory;

import java.util.*;

import java.util.Map.Entry;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import steffen.easycraft.SlotEasyCraft;

public class GuiCrafting extends GuiContainer {

	private float field_35312_g;
    private boolean field_35313_h;
    private boolean field_35314_i;
    private boolean shouldShowDescriptions = false;
    
	public GuiCrafting(InventoryPlayer par1InventoryPlayer, World par2World, int par3, int par4, int par5)
    {
        super( new ContainerWorkbench(par1InventoryPlayer, par2World, par3, par4, par5) );
        field_35312_g = 0.0F;
        field_35313_h = false;
        allowUserInput = true;
        shouldShowDescriptions = false;
        ySize = 208;
        
        ((ContainerWorkbench)inventorySlots).updateVisibleSlots(field_35312_g);	
    }

	public void updateContainer()
	{
		((ContainerWorkbench)inventorySlots).populateSlotsWithRecipes();
	}

	public void initGui()
    {
		super.initGui();
    	controlList.clear();
    }

	@Override
	protected void handleMouseClick(Slot slot, int i, int j, int flag)
    {
        inventorySlots.slotClick(i, j, flag, mc.thePlayer);
    }

	// Slot pressed?
	/*
	
	protected void func_35309_a(Slot slot, int i, int j, boolean flag)
	{
		super.func_35309_a(slot, i, j, flag);
		ContainerClevercraft container = (ContainerClevercraft)inventorySlots;
		if(slot.inventory == container.visibleRecipes)
			inventorySlots.slotClick(slot.slotNumber, j, flag, mc.thePlayer);
		else
			container.populateSlotsWithRecipes();
		//updateScreen();
	}*/

	public void drawScreen(int i, int j, float f)
    {
        boolean flag = Mouse.isButtonDown(0);
        int k = guiLeft;
        int l = guiTop;
        int i1 = k + 155;
        int j1 = l + 17;
        int k1 = i1 + 14;
        int l1 = j1 + 88 + 2;
        
        if(!field_35314_i && flag && i >= i1 && j >= j1 && i < k1 && j < l1)
        {
            field_35313_h = true;
        }
        if(!flag)
        {
            field_35313_h = false;
        }
        field_35314_i = flag;
        if(field_35313_h)
        {
            field_35312_g = (float)(j - (j1 + 8)) / ((float)(l1 - j1) - 16F);
            if(field_35312_g < 0.0F)
            {
                field_35312_g = 0.0F;
            }
            if(field_35312_g > 1.0F)
            {
                field_35312_g = 1.0F;
            }
            ((ContainerWorkbench)inventorySlots).updateVisibleSlots(field_35312_g);
        }
        super.drawScreen(i, j, f);
        //----
        //----
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(2896 /*GL_LIGHTING*/);
    }

	protected void drawGuiContainerForegroundLayer()
    {
        fontRenderer.drawString("Crafting Table", 8, 6, 0x404040);
    }

    protected void drawGuiContainerBackgroundLayer(float f, int i, int j)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int k = mc.renderEngine.getTexture("/steffen/easycraft/textures/crafttable.png");
        mc.renderEngine.bindTexture(k);
        int l = guiLeft;
        int i1 = guiTop;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
        int j1 = l + 155;
        int k1 = i1 + 17;
        int l1 = k1 + 88 + 2;
        drawTexturedModalRect(l + 154, i1 + 17 + (int)((float)(l1 - k1 - 17) * field_35312_g), 0, 208, 16, 16);
    }
    
    public void handleMouseInput()
    {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();
        ContainerWorkbench container = (ContainerWorkbench)inventorySlots;
        if(i != 0)
        {
            int j = (container.craftableRecipes.getSize() / 8 - 4) + 1;
            if(i > 0)
            {
                i = 1;
            }
            if(i < 0)
            {
                i = -1;
            }
            field_35312_g -= (double)i / (double)j;
            if(field_35312_g < 0.0F)
            {
                field_35312_g = 0.0F;
            }
            if(field_35312_g > 1.0F)
            {
                field_35312_g = 1.0F;
            }
            container.updateVisibleSlots(field_35312_g);
        }
    }
    
    public void resetScroll()
	{
		field_35312_g = 0.0F;
		((ContainerWorkbench)inventorySlots).updateVisibleSlots(field_35312_g);
	}
}