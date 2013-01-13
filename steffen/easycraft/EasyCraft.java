package steffen.easycraft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet102WindowClick;
import net.minecraft.network.packet.Packet14BlockDig;
import net.minecraft.network.packet.PacketCount;
import net.minecraft.server.MinecraftServer;
import steffen.easycraft.datatypes.EasyItemStack;
import steffen.easycraft.datatypes.EasyRecipe;

public class EasyCraft
{
	public static int delay = -1;
	
	public static void refreshInventory(IInventory inv)
	{
		inv.onInventoryChanged();
	}
	
	@SuppressWarnings("unchecked")
    public static <T, E> T getPrivateValue(Class <? super E > classToAccess, E instance, int fieldIndex) 
    {
        try
        {
            Field f = classToAccess.getDeclaredFields()[fieldIndex];
            f.setAccessible(true);
            return (T) f.get(instance);
        }
        catch (Exception e)
        {
            return null;
        }
    }
	
	/**
	 * @see #click()
	 */
	public static void click(ItemStack result, int windowid, int slot, int mousebutton, int shift, EntityPlayer player)
	{
		click(result, windowid, slot, mousebutton, shift, player, false);
	}
	
	/**
	 * Sends a click packet (102) to the server
	 * @param result a packet will be sent
	 * @param windowid winId()
	 * @param slot par1
	 * @param mousebutton par2
	 * @param shift par3
	 * @param player par4EntityPlayer
	 * @param force Whether to force send packet
	 */
	
	public static void click(ItemStack result, int windowid, int slot, int mousebutton, int shift, EntityPlayer player, boolean force)
	{
		/*if(result == null && !force)
		{
			pc = FMLClientHandler.instance().getClient().playerController;
			pc.windowClick(windowid, slot, mousebutton, shift, player);
		}
		else
		{*/
			short var6 = player.openContainer.getNextTransactionID(player.inventory);
			long startTime = System.currentTimeMillis();
			long backup = getCountOfPacket(106);
			if(Minecraft.getMinecraft().thePlayer != null)
				Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new Packet102WindowClick(windowid , slot, mousebutton, shift, result, var6));
		/*} */
		
		//System.out.println("click - " + windowid + "," + slot + "," + mousebutton + "," + shift);
		while(backup == getCountOfPacket(103) && (System.currentTimeMillis() - startTime) < 3000) {}
		//Handle time security
		try{
			Thread.sleep(EasyCraft.delay);
		}catch(Exception e) { }
	}
	
	public static EntityPlayerMP getSPPlayerOnServer()
	{
		return MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(Minecraft.getMinecraft().thePlayer.username);
	}
	
	/**
	 * Drops an item; drop is executed via a Dig-Packet (Packet-14)
	 */
	public static void drop(boolean holeStack)
	{
		boolean multiPlayer = Minecraft.getMinecraft().isSingleplayer();
		
		if(multiPlayer)
		{
			int status = holeStack ? 3 : 4; /* 3 => hole Stack, 4 => single Item */
			if(Minecraft.getMinecraft().thePlayer != null)
				Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new Packet14BlockDig(status, 0, 0, 0, 0));
		}
		else
			getSPPlayerOnServer().dropPlayerItem(getSPPlayerOnServer().getHeldItem());
	}
	
	public static int winId()
	{
		if(Minecraft.getMinecraft().thePlayer == null)
			return 0;
		return Minecraft.getMinecraft().getMinecraft().thePlayer.openContainer.windowId;
		//return FMLClientHandler.instance().getClient().thePlayer.openContainer.windowId;
	}
	
	/**
	 * Handles all find stuff
	 * @return int, 0 if not enough found , 1 if enough (not used)
	 */
	protected static int find(EasyItemStack stack, InventoryPlayer d, int dest)
	{
		boolean multiPlayer = !Minecraft.getMinecraft().isSingleplayer();
		int left = stack.getSize();
		for(int c = 0; c < d.mainInventory.length; c++)
		{
			if(d.getStackInSlot(c) != null)
			{
				EasyItemStack target = EasyItemStack.fromItemStack(d.getStackInSlot(c));
				if(target.equals(stack, true))
				{	
					int slot = c;
					int realSlot = slot;
					ItemStack backup = d.getStackInSlot(slot).copy();
					if(slot <= 8)
						slot += 37;
					else
						slot += 10; /* 9 crafting + 1 result */
					ItemStack source = d.getStackInSlot(realSlot);

					boolean useHoleStack = false;
					//Determine if we have enough in this slot
					if(d.getStackInSlot(realSlot).stackSize <= left)
						useHoleStack = true;
					if(useHoleStack)
					{
						if(multiPlayer)
						{
							//pull item from source slot
							click(source /* we put the item */ , winId(), slot, 0, 0, d.player);

							//put it into crafting grid
							left -= source.stackSize;
							click(null /* we dont have anything later */, winId(), dest, 0, 0, d.player);
						}
						else
						{
							getSPPlayerOnServer().inventory.setInventorySlotContents(realSlot, null);
						}
						//@GUI @SP slot now empty, also SP
						d.setInventorySlotContents(realSlot, null);
					}
					else
					{
						if(multiPlayer)
						{
							//put enough to crafting grid
							for(int am = 0; am < left && am < d.getStackInSlot(realSlot).stackSize; am++)
							{
								//pull single item
								ItemStack tmp = source.copy();
								click(source, winId(), slot, 0, 0, d.player);
								click(null, winId(), dest, 1, 0, d.player);
								source.stackSize--;
								if(source.stackSize > 0)
									click(null, winId(), slot, 0, 0, d.player);
								//This should be updated to object-references
								--left;
							}
						}
						else
						{
							//pull enough
							for(int am = 0; am < left && am < d.getStackInSlot(realSlot).stackSize; am++)
							{
								source.stackSize--;
								if(!multiPlayer)
									getSPPlayerOnServer().inventory.setInventorySlotContents(realSlot, source.copy());
								--left;
							}
						}
					}
					if(left <= 0)
						return 1;
				}
			}
		}
		return 0;
	}
	
	/**
	 * Gets an integer from values, which is bigger than value and the next to it (lowest difference)
	 * @param value
	 * @param values
	 * @return int
	 */
	public static int nextInt(int value, int[] values)
	{
		//Sort values
		Arrays.sort(values);
		HashMap<Integer, Integer> t = new HashMap<Integer, Integer>();
		for(int c = 0; c < values.length; c++)
		{
			if(value <= values[c])
				t.put(c, values[c] - value);
		}
		int lowest = -1;
		int pointer = -1;
		for(Map.Entry<Integer, Integer> v : t.entrySet())
		{
			if(lowest == -1)
			{
				lowest = v.getValue();
				pointer = v.getKey();
			}
			else
			{
				if(lowest > v.getValue())
				{
					lowest = v.getValue();
					pointer = v.getKey();
				}
			}
		}
		//t.get(pointer) = lowest difference
		return values[pointer];
	}
	
	public static ItemStack[] resolveIngredients(EasyRecipe receipe)
	{
		ItemStack[] arr = new ItemStack[9];
		int arrC = 0;
		for(int c = 0; c < receipe.getIngredientsSize(); c++)
		{
			Object dbg2 = receipe.getIngredient(c);
			if(receipe.getIngredient(c) instanceof EasyItemStack)
			{
				arr[arrC] = ((EasyItemStack)receipe.getIngredient(c)).toItemStack().copy();
				++arrC;
			}
			else if(receipe.getIngredient(c) instanceof ArrayList)
				for(ItemStack stack : (ArrayList<ItemStack>) receipe.getIngredient(c))
				{
					arr[arrC] = stack.copy();			
					++arrC;
				}
			else if(receipe.getIngredient(c) == null)
				++arrC;
		}
		return arr;
	}
	
	/**
	 * Writes a recipe to the server
	 * @param receipe The recipe
	 * @param inv The inventory to look for ingredients
	 * @param inventorySlots The slots, to be used
	 */
	public static void writeReceipe(EasyRecipe receipe, InventoryPlayer inv)
	{
		int dbg = receipe.getIngredientsSize();
		ItemStack[] arr = resolveIngredients(receipe);
		
		int craftingSlot = 0;
		
		for(int c = 0; c < arr.length; c++)
		{
			// 0 -> 8 = hotbar
			// 8 -> 40 = other stuff
			/*
			 *  windowclick - 1,37,0,0
 				Packet 102 - WindowClick - 1,37,0,0, 1xtile.log@0
 				windowclick - 1,1,0,0
 				Packet 102 - WindowClick - 1,1,0,0, null
 				windowclick - 1,0,0,0
 				Packet 102 - WindowClick - 1,0,0,0, 4xtile.wood@0
			 */

			++craftingSlot;
			if(arr[c] == null)
				continue;
			
			//Check if its > width
			int[] c_1 = new int[] { 1, 4, 7 };
			int[] c_2 = new int[] { 1, 2, 4, 5, 7, 8 };
			if(receipe.getWidth() == 1 && !Arrays.asList(c_1).contains(craftingSlot))
				craftingSlot = nextInt(craftingSlot, c_1);
			else if(receipe.getWidth() == 2 && !Arrays.asList(c_2).contains(craftingSlot))
				craftingSlot = nextInt(craftingSlot, c_2);
			/* Other cases allowed */
				
			find(EasyItemStack.fromItemStack(arr[c]), inv, craftingSlot); 
		}
		//get result into hand
		
		ItemStack result_ = receipe.getResult().toItemStack().copy();
		saveOutput(result_ /* we get the result */, inv.player);
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		refreshInventory(inv);
	}
	
	public static long getCountOfPacket(int packet)
	{
		Map<Integer, Long> m = getPrivateValue(PacketCount.class, null, 1);
		if(!m.containsKey(packet))
			return 0;
		return m.get(packet);
	}
	
	/**
	 * Saves the output to an existing stack (if enough space)
	 * Saves the output to either a free slot or drops it 
	 * @param inventorySlots 
	 */
	public static void saveOutput(ItemStack output, EntityPlayer player)
	{
		boolean multiPlayer = !Minecraft.getMinecraft().isSingleplayer();
		
		ArrayList<Integer> slots = new ArrayList<Integer>();
		int left = output.stackSize;
		//find same item
		for(int c = 0; c < player.inventory.mainInventory.length && output.isStackable(); c++)
		{
			ItemStack current = player.inventory.getStackInSlot(c);
			if(current != null)
			{
				if(EasyItemStack.fromItemStack(current).equals(EasyItemStack.fromItemStack(output), true))
				{
					slots.add(c);
					left -= current.stackSize;
					if(left <= 0)
						break; /* we don't need more slots */
				}
			}
		}
		if(left > 0)
		{
			//find free slot if there are some items left , capacity not enough
			for(int c = 0; c < player.inventory.mainInventory.length; c++)
			{
				if(player.inventory.getStackInSlot(c) == null)
				{
					int slot = c;
					if(multiPlayer)
					{
						/* imported from @see #find() */
						if(slot <= 8)
							slot += 37;
						else
							slot += 10; /* 9 crafting + 1 result */
						/* end_import */
						click(output, winId(), 0, 0, 0, player);
						click(null /* i write anything to slot */ , winId(), slot, 0, 0, player);
					}
					else
					{
						getSPPlayerOnServer().inventory.setInventorySlotContents(c, output.copy());
					}
					//@GUI: populate slot
					player.inventory.setInventorySlotContents(c, output.copy());
					return;
				}
			}
		}
		else
		{
			/* Append to slots */
			for(int c = 0; c < slots.size(); c++)
			{
				int slot = slots.get(c), available = 0, left2 = 0, sZ = 0;
				if(multiPlayer)
				{
					/* imported from @see #find() */
					if(slot <= 8)
						slot += 37;
					else
						slot += 10; /* 9 crafting + 1 result */
					/* end_import */
				}
				//get available size
				if(player.inventory.getStackInSlot(slots.get(c)) != null)
					available = player.inventory.getInventoryStackLimit() - player.inventory.getStackInSlot(slots.get(c)).stackSize;
				else
					available = player.inventory.getInventoryStackLimit();
				//save
				//if(multiPlayer)
					click(output, winId(), 0, 0, 0, player);

				if(player.inventory.getStackInSlot(slots.get(c)) != null)
					sZ = player.inventory.getStackInSlot(slots.get(c)).stackSize + output.stackSize;
				else
				    sZ = output.stackSize;
				if(sZ > 64)
				{
					left2 = sZ - 64;
					sZ = 64;
				}
				output.stackSize = sZ;
				//merge with this slot
				//if(multiPlayer)
					click(output, winId(), slot, 0, 0, player);
				//@GUI: save new stack size
				player.inventory.setInventorySlotContents(slots.get(c), output.copy());
				if(multiPlayer)
					getSPPlayerOnServer().inventory.setInventorySlotContents(slots.get(c), output.copy());
				//set stackSize back
				output.stackSize = left2;
				/* we don't need a break; we have it in the loop populating slots */
			}
			if(left <= 0)
				return;
			//drop anything
			drop(true);
		}
	}
}