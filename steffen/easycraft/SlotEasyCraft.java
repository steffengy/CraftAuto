package steffen.easycraft;

import steffen.easycraft.datatypes.EasyRecipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotEasyCraft extends Slot {

	private EntityPlayer thePlayer;
	private EasyRecipe irecipe;
	private int index = 0;

	public SlotEasyCraft(EntityPlayer entityplayer, IInventory craftableRecipes, int i, int j, int k)
    {
        super(craftableRecipes, i, j, k);
        thePlayer = entityplayer;
		index = i;
    }

	public void setRecipe(EasyRecipe theIRecipe)
	{
		irecipe = theIRecipe;
	}

	public EasyRecipe getIRecipe()
	{
		return irecipe;
	}

	public boolean isItemValid(ItemStack itemstack)
    {
        return false;
    }
}
