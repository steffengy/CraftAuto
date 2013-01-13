package net.minecraft.inventory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import steffen.easycraft.EasyCraft;
import steffen.easycraft.InventoryEasyCraft;
import steffen.easycraft.SlotEasyCraft;
import steffen.easycraft.datatypes.EasyRecipe;
import steffen.easycraft.helpers.RecipeHelper;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.src.ModLoader;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayerMP;

public class ContainerWorkbench extends Container
{
    /** The crafting matrix inventory (3x3). */ 
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
    public IInventory craftResult = new InventoryCraftResult();
    private World worldObj;
    private int posX;
    private int posY;
    private int posZ;
    
    public InventoryEasyCraft craftableRecipes;
    private List recipeList;
    public InventoryBasic inventory = new InventoryBasic("tmp", 8*5);
    public EntityPlayer thePlayer;
    private Timer timer;
    private float last = 0.0F;

    public ContainerWorkbench(InventoryPlayer par1InventoryPlayer, World par2World, int par3, int par4, int par5)
    {
    	if(EasyCraft.delay == -1)
    	{
    		File cfg = new File("easycraft.conf");
    		if(!cfg.exists())
    		{
    			try {
					cfg.createNewFile();
				} catch (IOException e) {
					System.out.println("Failed writing config!");
				}
    			BufferedWriter out;
				try {
					out = new BufferedWriter(new FileWriter(cfg));
					out.write("delay=10\n");
					out.close();
				} catch (IOException e) {
					System.out.println("Failed writing config!");
				}
    		}
    		else
    		{
    			try {
					FileInputStream fis = new FileInputStream(cfg);
					BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
					String line;
					try {
						while((line = br.readLine()) != null)
						{
							line = line.trim();
							if(line.contains("="))
							{
								String[] parts = line.split("=", 2);
								if(parts[0].equalsIgnoreCase("delay"))
									EasyCraft.delay = Integer.parseInt(parts[1]);
							}
						}
					} catch (IOException e) {
						System.out.println("Reading error!");
					}
    			} catch (FileNotFoundException e) {
					System.out.println("File not found!");
				}
    		}
    		if(EasyCraft.delay < 0)
    			EasyCraft.delay = 10;
    	}
    	worldObj = par2World;
		thePlayer = par1InventoryPlayer.player;
        craftableRecipes = new InventoryEasyCraft(1000);
        recipeList = Collections.unmodifiableList( CraftingManager.getInstance().getRecipeList() );
        
        this.posX = par3;
        this.posY = par4;
        this.posZ = par5;
        
        /* top inv 0 - 40  */
		for(int l2 = 0; l2 < 5; l2++)
        {
            for(int j3 = 0; j3 < 8; j3++)
            {
            	addSlotToContainer(new SlotEasyCraft(thePlayer, inventory, j3 + l2 * 8, 8 + j3 * 18, 18 + l2 * 18));
            }
        }
        
		/* main_inv */
        for(int j = 0; j < 3; j++)
        {
            for(int i1 = 0; i1 < 9; i1++)
            {
            	addSlotToContainer(new Slot(par1InventoryPlayer, i1 + j * 9 + 9, 8 + i1 * 18, 125 + j * 18));
            }
        }
        /* player inv hotbar */
        for(int i3 = 0; i3 < 9; i3++)
        {
        	addSlotToContainer(new Slot(par1InventoryPlayer, i3, 8 + i3 * 18, 184));
        }
        
        populateSlotsWithRecipes();
        if(worldObj.isRemote) {
        	timer = new Timer();
        	timer.schedule(new RemindTask(), 1000);
        }
        updateVisibleSlots(0.0F);
    }

    /**
     * Callback for when the crafting matrix is changed.
     */
    public void onCraftMatrixChanged(IInventory par1IInventory)
    {
        this.craftResult.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.worldObj));
    }

    /**
     * Callback for when the crafting gui is closed.
     */
    public void onCraftGuiClosed(EntityPlayer par1EntityPlayer)
    {
        super.onCraftGuiClosed(par1EntityPlayer);

        if (!this.worldObj.isRemote)
        {
            for (int var2 = 0; var2 < 9; ++var2)
            {
                ItemStack var3 = this.craftMatrix.getStackInSlotOnClosing(var2);

                if (var3 != null)
                {
                    par1EntityPlayer.dropPlayerItem(var3);
                }
            }
        }
    }

    public boolean canInteractWith(EntityPlayer par1EntityPlayer)
    {
        return this.worldObj.getBlockId(this.posX, this.posY, this.posZ) != Block.workbench.blockID ? false : par1EntityPlayer.getDistanceSq((double)this.posX + 0.5D, (double)this.posY + 0.5D, (double)this.posZ + 0.5D) <= 64.0D;
    }

    /**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     */
    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2)
    {
        ItemStack var3 = null;
        Slot var4 = (Slot)this.inventorySlots.get(par2 - 30); //TODO: (flag) difference to original!

        if (var4 != null && var4.getHasStack())
        {
            ItemStack var5 = var4.getStack();
            var3 = var5.copy();

            if (par2 == 0)
            {
                if (!this.mergeItemStack(var5, 10, 46, true))
                {
                    return null;
                }

                var4.onSlotChange(var5, var3);
            }
            else if (par2 >= 10 && par2 < 37)
            {
                if (!this.mergeItemStack(var5, 37, 46, false))
                {
                    return null;
                }
            }
            else if (par2 >= 37 && par2 < 46)
            {
                if (!this.mergeItemStack(var5, 10, 37, false))
                {
                    return null;
                }
            }
            else if (!this.mergeItemStack(var5, 10, 46, false))
            {
                return null;
            }

            if (var5.stackSize == 0)
            {
                var4.putStack((ItemStack)null);
            }
            else
            {
                var4.onSlotChanged();
            }

            if (var5.stackSize == var3.stackSize)
            {
                return null;
            }

            var4.onPickupFromSlot(par1EntityPlayer, var5);
        }

        return var3;
    }

    /**
     * places itemstacks in first x slots, x being aitemstack.lenght
     */
    public void putStacksInSlots(ItemStack[] par1ArrayOfItemStack)
    {
        for (int var2 = 0; var2 < par1ArrayOfItemStack.length; ++var2)
        {
        	int slot = var2;
        	if(!Minecraft.getMinecraft().isSingleplayer())
        		slot += 30; /* patch this to prevent client side errors */
        	
            this.getSlot(slot).putStack(par1ArrayOfItemStack[var2]);
        }
    }
 	
 	public void putStackInSlot(int par1, ItemStack par2ItemStack)
    {
 		int slot = par1;
 		if(!Minecraft.getMinecraft().isSingleplayer())
 			slot += 30; /* patch this to prevent client side errors */
 		
        this.getSlot(slot).putStack(par2ItemStack);
    }
 	
 	@Override
 	public ItemStack slotClick(int slotIndex, int mouseButton, int shiftIsDown, EntityPlayer entityplayer)
    {
 		if(slotIndex != -999 
				&& inventorySlots.get(slotIndex) != null 
				&& inventorySlots.get(slotIndex) instanceof SlotEasyCraft) 
 		{
	 		if(mouseButton != 1 && shiftIsDown == 0)
	 		{
	 			SlotEasyCraft sE = (SlotEasyCraft) inventorySlots.get(slotIndex);
	 			if(sE.getIRecipe() != null)
	 			{
	 				if(RecipeHelper.canCraft(sE.getIRecipe(), thePlayer.inventory))
	 					requestSingle(sE);
	 				populateSlotsWithRecipes();
	 				updateVisibleSlots(last);
	 			}
	 		}
	 		else
	 		{
	 			SlotEasyCraft sE = (SlotEasyCraft) inventorySlots.get(slotIndex);
	 			if(sE.getIRecipe() != null)
	 			{
	 				if(RecipeHelper.canCraft(sE.getIRecipe(), thePlayer.inventory))
	 					requestMultiple(sE); 
	 				populateSlotsWithRecipes();
	 				updateVisibleSlots(last);
	 			}
	 		}
	 		return null;
 		}
 		//this was a click on none a sloteasycrarft - 30 ; reroute for server
 		//return super.slotClick(slotIndex, mouseButton, shiftIsDown, entityplayer);
 		/* Wrapper to slotClick arguments */
 		int par1 = slotIndex;
 		int par2 = mouseButton;
 		int par3 = shiftIsDown;
 		EntityPlayer par4EntityPlayer = entityplayer;
 		/* TODO: (flag) imported from container - slotClick - flagged changes with todo-flag */

        ItemStack var5 = null;
        InventoryPlayer var6 = par4EntityPlayer.inventory;
        Slot var7;
        ItemStack var8;
        int var10;
        ItemStack var11;

        if ((par3 == 0 || par3 == 1) && (par2 == 0 || par2 == 1))
        {
            if (par1 == -999)
            {
                if (var6.getItemStack() != null && par1 == -999)
                {
                    if (par2 == 0)
                    {
                        par4EntityPlayer.dropPlayerItem(var6.getItemStack());
                        var6.setItemStack((ItemStack)null);
                    }

                    if (par2 == 1)
                    {
                        par4EntityPlayer.dropPlayerItem(var6.getItemStack().splitStack(1));

                        if (var6.getItemStack().stackSize == 0)
                        {
                            var6.setItemStack((ItemStack)null);
                        }
                    }
                }
            }
            else if (par3 == 1)
            {
                var7 = (Slot)this.inventorySlots.get(par1);

                if (var7 != null && var7.canTakeStack(par4EntityPlayer))
                {
                    var8 = this.transferStackInSlot(par4EntityPlayer, par1);

                    if (var8 != null)
                    {
                        int var12 = var8.itemID;
                        var5 = var8.copy();

                        if (var7 != null && var7.getStack() != null && var7.getStack().itemID == var12)
                        {
                            this.retrySlotClick(par1, par2, true, par4EntityPlayer);
                        }
                    }
                }
            }
            else
            {
                if (par1 < 0)
                {
                    return null;
                }

                var7 = (Slot)this.inventorySlots.get(par1);

                if (var7 != null)
                {
                    var8 = var7.getStack();
                    ItemStack var13 = var6.getItemStack();

                    if (var8 != null)
                    {
                        var5 = var8.copy();
                    }

                    if (var8 == null)
                    {
                        if (var13 != null && var7.isItemValid(var13))
                        {
                            var10 = par2 == 0 ? var13.stackSize : 1;

                            if (var10 > var7.getSlotStackLimit())
                            {
                                var10 = var7.getSlotStackLimit();
                            }

                            var7.putStack(var13.splitStack(var10));

                            if (var13.stackSize == 0)
                            {
                                var6.setItemStack((ItemStack)null);
                            }
                        }
                    }
                    else if (var7.canTakeStack(par4EntityPlayer))
                    {
                        if (var13 == null)
                        {
                            var10 = par2 == 0 ? var8.stackSize : (var8.stackSize + 1) / 2;
                            var11 = var7.decrStackSize(var10);
                            var6.setItemStack(var11);

                            if (var8.stackSize == 0)
                            {
                                var7.putStack((ItemStack)null);
                            }

                            var7.onPickupFromSlot(par4EntityPlayer, var6.getItemStack());
                        }
                        else if (var7.isItemValid(var13))
                        {
                            if (var8.itemID == var13.itemID && var8.getItemDamage() == var13.getItemDamage() && ItemStack.areItemStackTagsEqual(var8, var13))
                            {
                                var10 = par2 == 0 ? var13.stackSize : 1;

                                if (var10 > var7.getSlotStackLimit() - var8.stackSize)
                                {
                                    var10 = var7.getSlotStackLimit() - var8.stackSize;
                                }

                                if (var10 > var13.getMaxStackSize() - var8.stackSize)
                                {
                                    var10 = var13.getMaxStackSize() - var8.stackSize;
                                }

                                var13.splitStack(var10);

                                if (var13.stackSize == 0)
                                {
                                    var6.setItemStack((ItemStack)null);
                                }

                                var8.stackSize += var10;
                            }
                            else if (var13.stackSize <= var7.getSlotStackLimit())
                            {
                                var7.putStack(var13);
                                var6.setItemStack(var8);
                            }
                        }
                        else if (var8.itemID == var13.itemID && var13.getMaxStackSize() > 1 && (!var8.getHasSubtypes() || var8.getItemDamage() == var13.getItemDamage()) && ItemStack.areItemStackTagsEqual(var8, var13))
                        {
                            var10 = var8.stackSize;

                            if (var10 > 0 && var10 + var13.stackSize <= var13.getMaxStackSize())
                            {
                                var13.stackSize += var10;
                                var8 = var7.decrStackSize(var10);

                                if (var8.stackSize == 0)
                                {
                                    var7.putStack((ItemStack)null);
                                }

                                var7.onPickupFromSlot(par4EntityPlayer, var6.getItemStack());
                            }
                        }
                    }

                    var7.onSlotChanged();
                }
            }
        }
        else if (par3 == 2 && par2 >= 0 && par2 < 9)
        {
            var7 = (Slot)this.inventorySlots.get(par1);

            if (var7.canTakeStack(par4EntityPlayer))
            {
                var8 = var6.getStackInSlot(par2);
                boolean var9 = var8 == null || var7.inventory == var6 && var7.isItemValid(var8);
                var10 = -1;

                if (!var9)
                {
                    var10 = var6.getFirstEmptyStack();
                    var9 |= var10 > -1;
                }

                if (var7.getHasStack() && var9)
                {
                    var11 = var7.getStack();
                    var6.setInventorySlotContents(par2, var11);

                    if ((var7.inventory != var6 || !var7.isItemValid(var8)) && var8 != null)
                    {
                        if (var10 > -1)
                        {
                            var6.addItemStackToInventory(var8);
                            var7.decrStackSize(var11.stackSize);
                            var7.putStack((ItemStack)null);
                            var7.onPickupFromSlot(par4EntityPlayer, var11);
                        }
                    }
                    else
                    {
                        var7.decrStackSize(var11.stackSize);
                        var7.putStack(var8);
                        var7.onPickupFromSlot(par4EntityPlayer, var11);
                    }
                }
                else if (!var7.getHasStack() && var8 != null && var7.isItemValid(var8))
                {
                    var6.setInventorySlotContents(par2, (ItemStack)null);
                    var7.putStack(var8);
                }
            }
        }
        else if (par3 == 3 && par4EntityPlayer.capabilities.isCreativeMode && var6.getItemStack() == null && par1 >= 0)
        {
            var7 = (Slot)this.inventorySlots.get(par1);

            if (var7 != null && var7.getHasStack())
            {
                var8 = var7.getStack().copy();
                var8.stackSize = var8.getMaxStackSize();
                var6.setItemStack(var8);
            }
        }
        //we need to send this out because the gui won't (send packet)
        if(!Minecraft.getMinecraft().isSingleplayer())
        	EasyCraft.click(var5, EasyCraft.winId(), slotIndex - 30 /* TODO: (flag) send real id */, mouseButton, shiftIsDown, entityplayer, true);
        return var5;
 		/* end */
    }
 	
 	/* TODO: (this todo is just a flag) CUSTOM METHODS */
 	
 // Check InventorPlayer contains the ItemStack.
  	private int getFirstInventoryPlayerSlotWithItemStack(InventoryPlayer inventory, ItemStack itemstack)
  	{
  		for(int i = 0; i < inventory.getSizeInventory(); i++) {
  			ItemStack itemstack1 = inventory.getStackInSlot(i);
  			if(itemstack1 != null
  					&& itemstack1.itemID == itemstack.itemID 
  					&& (itemstack1.getItemDamage() == itemstack.getItemDamage() || itemstack.getItemDamage() == -1)) {
  				return i;
  			}
  		}

  		return -1;
  	}
 	
 	public void updateVisibleSlots(float f)
	{
		int numberOfRecipes = craftableRecipes.getSize();
		int i = (numberOfRecipes / 8 - 4) + 1;
        int j = (int)((double)(f * (float)i) + 0.5D);
        if(j < 0)
            j = 0;
        
        for(int k = 0; k < 5; k++) {
            for(int l = 0; l < 8; l++) {
                int i1 = l + (k + j) * 8;
                int i2 = (l + k * 8);  
                Slot slot = (Slot)inventorySlots.get(i2); 
                if(i1 >= 0 && i1 < numberOfRecipes) {
                	ItemStack recipeOutput = craftableRecipes.getRecipeOutput(i1);
                	if(recipeOutput != null) {
                    	if(slot instanceof SlotEasyCraft) 
                    	{
                    		inventory.setInventorySlotContents(i2, recipeOutput);
                    		((SlotEasyCraft)slot).setRecipe( craftableRecipes.getIRecipe(i1) );
                    	}
                	} else {
                		if(slot instanceof SlotEasyCraft) {
                    		inventory.setInventorySlotContents(i2, null);
                    		((SlotEasyCraft)slot).setRecipe(null);
                    	}
                	}
                } else {
                	if(slot instanceof SlotEasyCraft) {
                    	inventory.setInventorySlotContents(i2, null);
                		((SlotEasyCraft)slot).setRecipe(null);
                	}
                }
            }
        }
	}
 	
    
 	// Populate all the slots with recipes the player can craft.
 	public void populateSlotsWithRecipes()
 	{
 		RecipeHelper.allRecipes = new ArrayList<EasyRecipe>();
 		RecipeHelper.unknownRecipes = new ArrayList<IRecipe>();
 		
 		craftableRecipes.clearRecipes();
 		RecipeHelper.scanRecipes();
 		for(EasyRecipe r : RecipeHelper.getCraftableRecipes(this.thePlayer.inventory, 0, RecipeHelper.getAllRecipes()))
 		{
 			craftableRecipes.addRecipe(r);
 		}
 	}
 	
 	
 	public boolean requestSingle(SlotEasyCraft slot)
 	{
 			// Get IRecipe from slot.
 			EasyRecipe irecipe = slot.getIRecipe();
 			if(irecipe == null)
 				return false;
 			//Apply receipe
 			EasyCraft.writeReceipe(irecipe, thePlayer.inventory);
 			
 			//onCraftMatrixChanged(this.craftMatrix);
 			return true;
 	}
 	
 	public boolean requestMultiple(SlotEasyCraft slot)
 	{
 		//Get IRecipe from slot
 		EasyRecipe irecipe = slot.getIRecipe();
 		if(irecipe == null)
 			return false;
 		int max = 0;
 		for(ItemStack t : EasyCraft.resolveIngredients(irecipe))
 		{
 			if(t != null && t.stackSize > max)
 				max = slot.inventory.getInventoryStackLimit() / t.stackSize;
 		}
 		//Apply recipe
 		for(int c = 0; c < max; c++)
 		{
 			if(!RecipeHelper.canCraft(irecipe, this.thePlayer.inventory))
 				break;
 			requestSingle(slot);
 		}
 		return true;
 	}
 	
 	
    class RemindTask extends TimerTask {
	    public void run() {
	      populateSlotsWithRecipes();
	      timer.cancel();
	    }
	}
}
