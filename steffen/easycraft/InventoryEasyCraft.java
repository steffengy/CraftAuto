package steffen.easycraft;
 
import steffen.easycraft.datatypes.EasyRecipe;
import net.minecraft.inventory.InventoryBasic;  
import net.minecraft.item.ItemStack;

public class InventoryEasyCraft {

	private EasyRecipe[] recipes;
	private int recipesLength;

	public InventoryEasyCraft(int i)
	{
		recipesLength = i;
		recipes = new EasyRecipe[recipesLength];
	}

	public int getSize()
	{
		for(int i = 0; i < recipes.length; i++) {
			if(recipes[i] == null)
				return i;
		}

		return 0;
	}

	public boolean addRecipe(EasyRecipe irecipe)
	{
		int size = getSize();
		if(size >= recipesLength || irecipe == null)
			return false;

		recipes[size] = irecipe;
		return true;
	}

	public EasyRecipe getIRecipe(int i)
	{
		return recipes[i];
	}

	public ItemStack getRecipeOutput(int i)
	{
		if(recipes[i] != null)
			return recipes[i].getResult().toItemStack().copy();
		else
			return null;
	}

	public void clearRecipes() {
		recipes = null;
		recipes = new EasyRecipe[recipesLength];
	}
}
