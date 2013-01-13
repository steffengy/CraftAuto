package steffen.easycraft.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import steffen.easycraft.EasyCraft;
import steffen.easycraft.datatypes.EasyItemStack;
import steffen.easycraft.datatypes.EasyRecipe;

import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;

public class RecipeHelper {

    public static ArrayList<EasyRecipe> allRecipes = new ArrayList<EasyRecipe>();
    public static ArrayList<IRecipe> unknownRecipes = new ArrayList<IRecipe>();

    /**
     * Get a list of all recipes that are scanned and available. If recipes are not yet scanned it will return an empty list.
     */
    public static ArrayList<EasyRecipe> getAllRecipes() {
    	ArrayList t = new ArrayList();
    	t.addAll(allRecipes);
    	return t;
    }

    /**
     * Scan, convert, sort and store the recognised recipes. To access the recipes use {@link #getAllRecipes()}.
     */
    public static void scanRecipes() {
        long beforeTime = System.nanoTime();

        boolean forge = true, ic2 = true, unknown = false;
        //Forge
        try {
        	 Class.forName("net.minecraftforge.oredict.ShapedOreRecipe");
        } 
        catch( ClassNotFoundException e )
        {
        	forge = false; 
        }
        //IC2
        try{
        	Class.forName("ic2.core.AdvRecipe");
        }
        catch(ClassNotFoundException e)
        {
        	ic2 = false;
        }
        
        List mcRecipes = CraftingManager.getInstance().getRecipeList();
        ArrayList<EasyRecipe> tmp = new ArrayList<EasyRecipe>();
        int skipped = 0;
        int heightSlots = 3;
        int widthSlots = 3;
        
        for (int i = 0; i < mcRecipes.size(); i++) {
        	widthSlots = 3;
            IRecipe r = (IRecipe) mcRecipes.get(i);
            ArrayList ingredients = null;
            
            if (r instanceof ShapedRecipes) {
            	ItemStack[] input = EasyCraft.<ItemStack[], ShapedRecipes> getPrivateValue(ShapedRecipes.class, (ShapedRecipes) r, 2);
                ingredients = new ArrayList(Arrays.asList(input));
                widthSlots = EasyCraft.<Integer, ShapedRecipes> getPrivateValue(ShapedRecipes.class, (ShapedRecipes) r, 0);
            } else if (r instanceof ShapelessRecipes) {
            	List input = EasyCraft.<List, ShapelessRecipes> getPrivateValue(ShapelessRecipes.class, (ShapelessRecipes) r, 1);
                ingredients = new ArrayList(input);
            } 
            else 
            {
            	if(forge)
            	{
            		if (r instanceof net.minecraftforge.oredict.ShapedOreRecipe) {
            			Object[] input = EasyCraft.<Object[], net.minecraftforge.oredict.ShapedOreRecipe> getPrivateValue(net.minecraftforge.oredict.ShapedOreRecipe.class, (net.minecraftforge.oredict.ShapedOreRecipe) r, 3);
            			ingredients = new ArrayList(Arrays.asList(input));
            			widthSlots = EasyCraft.<Integer, net.minecraftforge.oredict.ShapedOreRecipe> getPrivateValue(net.minecraftforge.oredict.ShapedOreRecipe.class, (net.minecraftforge.oredict.ShapedOreRecipe) r, 4);
            		} else if (r instanceof net.minecraftforge.oredict.ShapelessOreRecipe) {
            			List input = EasyCraft.<List, net.minecraftforge.oredict.ShapelessOreRecipe> getPrivateValue(net.minecraftforge.oredict.ShapelessOreRecipe.class, (net.minecraftforge.oredict.ShapelessOreRecipe) r, 1);
            			ingredients = new ArrayList(input);
            		} 
            		else
            		{
            			unknown = true;
            		} 
            	}
            
            	if (ic2 && unknown)
            	{
            		if(r instanceof ic2.core.AdvRecipe)
            		{
            			ingredients = new ArrayList(Arrays.asList(((ic2.core.AdvRecipe)r).input));
            			widthSlots = ((ic2.core.AdvRecipe)r).inputWidth;
            			unknown = false;
            		}
            		else if(r instanceof ic2.core.AdvShapelessRecipe)
            		{
            			ingredients = new ArrayList(Arrays.asList(((ic2.core.AdvShapelessRecipe)r).input));
            			unknown = false;
            		}
            		else
            		{
            			unknown = true;
            		}
            	}
            }
            
            if(unknown)
            {
            	unknownRecipes.add(r);
            	continue;
            }
            tmp.add(new EasyRecipe(EasyItemStack.fromItemStack(r.getRecipeOutput()), ingredients, widthSlots));
        }

        allRecipes.addAll(tmp);
        Collections.sort(allRecipes, new RecipeComparator());
    }

    /**
     * Returns a list of recipes that can be crafted using ingredients from the specified inventory.
     * 
     * @param inventory - inventory to check with
     * @param maxRecursion - how deep to recurse when trying to craft
     * @param recipesToCheck - a list of recipes to be checked
     */
    public static ArrayList<EasyRecipe> getCraftableRecipes(InventoryPlayer inventory, int maxRecursion, List<EasyRecipe> recipesToCheck) {
        ArrayList<EasyRecipe> tmpCraftable = new ArrayList<EasyRecipe>();
        ArrayList<EasyRecipe> tmpAll = new ArrayList<EasyRecipe>(recipesToCheck);

        for (EasyRecipe er : tmpAll) {
            if (canCraft(er, inventory)) {
                tmpCraftable.add(er);
            }
        }
        tmpAll.removeAll(tmpCraftable);

        if (!tmpCraftable.isEmpty()) {
            for (int recursion = 0; recursion < maxRecursion; recursion++) {
                if (tmpAll.isEmpty()) {
                    break;
                }

                ArrayList<EasyRecipe> immutableCraftable = new ArrayList<EasyRecipe>();
                immutableCraftable.addAll(tmpCraftable);
                for (EasyRecipe er : tmpAll) {
                    if (canCraft(er, inventory, immutableCraftable, maxRecursion)) {
                        tmpCraftable.add(er);
                    }
                }
                tmpAll.removeAll(tmpCraftable);

                if (immutableCraftable.size() == tmpCraftable.size()) {
                    break;
                }
            }
        }
        return tmpCraftable;
    }

    /**
     * Check if a recipe can be crafted with the ingredients from the inventory.
     * 
     * @param recipe - recipe to check
     * @param inventory - inventory to use the ingredients from
     * 
     * @see #canCraft(EasyRecipe, InventoryPlayer, ImmutableList, boolean, int)
     */
    public static boolean canCraft(EasyRecipe recipe, InventoryPlayer inventory) {
        return canCraft(recipe, inventory, null, false, 1, 0) > 0;
    }

    /**
     * Check if a recipe can be crafted with the ingredients from the inventory. If an ingredient is missing try to craft it from a list of recipes.
     * 
     * @param recipe - recipe to check
     * @param inventory - inventory to use the ingredients from
     * @param recipesToCheck - a list of recipes to try and craft from if an ingredient is missing
     * @param recursion - how deep to recurse while trying to craft an ingredient (must be nonnegative)
     * 
     * @see #canCraft(EasyRecipe, InventoryPlayer, ImmutableList, boolean, int)
     */
    public static boolean canCraft(EasyRecipe recipe, InventoryPlayer inventory, ArrayList<EasyRecipe> recipesToCheck, int recursion) {
        return canCraft(recipe, inventory, recipesToCheck, false, 1, recursion) > 0;
    }

    /**
     * Check if a recipe can be crafted with the ingredients from the inventory. If an ingredient is missing try to craft it from a list of recipes.
     * 
     * @param recipe - recipe to check
     * @param inventory - inventory to use the ingredients from
     * @param recipesToCheck - a list of recipes to try and craft from if an ingredient is missing
     * @param take - whether or not to take the ingredients from the inventory
     * @param recursion - how deep to recurse while trying to craft an ingredient (must be nonnegative)
     */
    public static int canCraft(EasyRecipe recipe, InventoryPlayer inventory, ArrayList<EasyRecipe> recipesToCheck, boolean take, int maxTimes, int recursion) {
        if (recursion < 0) {
            return 0;
        }
        
        recipe.getResult().setCharge(null);

        InventoryPlayer tmp = new InventoryPlayer(inventory.player);
        InventoryPlayer tmp2 = new InventoryPlayer(inventory.player);
        tmp.copyInventory(inventory);

        ArrayList<ItemStack> usedIngredients = new ArrayList<ItemStack>();

        int timesCrafted = 0;
        timesLoop: while (timesCrafted < maxTimes) {

            iiLoop: for (int ii = 0; ii < recipe.getIngredientsSize(); ii++) {
                if (recipe.getIngredient(ii) instanceof EasyItemStack) {
                    EasyItemStack ingredient = (EasyItemStack) recipe.getIngredient(ii);
                    int inventoryIndex = InventoryHelper.isItemInInventory(tmp, ingredient);
                    if (inventoryIndex != -1 && InventoryHelper.consumeItemForCrafting(tmp, inventoryIndex, usedIngredients)) {
                        continue iiLoop;
                    }
                    //
                    if (recipesToCheck != null && (recursion - 1) >= 0) {
                        ArrayList<EasyRecipe> list = getRecipesForItemFromList(ingredient, recipesToCheck);
                        for (EasyRecipe er : list) {
                            if (canCraft(er, tmp, recipesToCheck, true, 1, (recursion - 1)) > 0) {
                                ItemStack is = er.getResult().toItemStack();
                                is.stackSize--;
                                if (is.stackSize > 0 && !tmp.addItemStackToInventory(is)) {
                                    break timesLoop;
                                }
                                ItemStack usedItemStack = is.copy();
                                usedItemStack.stackSize = 1;
                                usedIngredients.add(usedItemStack);
                                continue iiLoop;
                            }
                        }
                    }
                    //
                    break timesLoop;
                } else if (recipe.getIngredient(ii) instanceof ArrayList) {
                    ArrayList<ItemStack> ingredients = (ArrayList<ItemStack>) recipe.getIngredient(ii);
                    int inventoryIndex = InventoryHelper.isItemInInventory(tmp, ingredients);
                    if (inventoryIndex != -1 && InventoryHelper.consumeItemForCrafting(tmp, inventoryIndex, usedIngredients)) {
                        continue iiLoop;
                    }
                    //
                    if (recipesToCheck != null && (recursion - 1) >= 0) {
                        ArrayList<EasyRecipe> list = getRecipesForItemFromList(ingredients, recipesToCheck);
                        for (EasyRecipe er : list) {
                            if (canCraft(er, tmp, recipesToCheck, true, 1, (recursion - 1)) > 0) {
                                ItemStack is = er.getResult().toItemStack();
                                is.stackSize--;
                                if (is.stackSize > 0 && !tmp.addItemStackToInventory(is)) {
                                    break timesLoop;
                                }
                                ItemStack usedItemStack = is.copy();
                                usedItemStack.stackSize = 1;
                                usedIngredients.add(usedItemStack);
                                continue iiLoop;
                            }
                        }
                    }
                    //
                    break timesLoop;
                }
            }

            timesCrafted++;
            tmp2.copyInventory(tmp);
        }

        recipe.getResult().setCharge(usedIngredients);

        if (take && timesCrafted > 0) {
            inventory.copyInventory(tmp2);
        }
        return timesCrafted;
    }

    /**
     * Get a list of recipes that can be used to craft a specified item/block.
     * 
     * @param ingredient - the item/block we want to craft
     * @param recipesToCheck - a list of recipes to be checked
     */
    private static ArrayList<EasyRecipe> getRecipesForItemFromList(EasyItemStack ingredient, ArrayList<EasyRecipe> recipesToCheck) {
        ArrayList<EasyRecipe> returnList = new ArrayList<EasyRecipe>();
        for (EasyRecipe er : recipesToCheck) {
            if (er.getResult().equals(ingredient, true)) {
                returnList.add(er);
            }
        }
        return returnList;
    }

    /**
     * Same as {@link #getRecipesForItemFromList(EasyItemStack, ImmutableList)} but for any of the items contained in the list.
     * 
     * @param ingredients - a list itemstacks
     * @param recipesToCheck - a list of recipes to be checked
     */
    private static ArrayList<EasyRecipe> getRecipesForItemFromList(ArrayList<ItemStack> ingredients, ArrayList<EasyRecipe> recipesToCheck) {
        ArrayList<EasyRecipe> returnList = new ArrayList<EasyRecipe>();
        for (ItemStack is : ingredients) {
            returnList.addAll(getRecipesForItemFromList(EasyItemStack.fromItemStack(is), recipesToCheck));
        }
        return returnList;
    }

    /**
     * Get the recipe that matches provided result and ingredients.
     * 
     * @param result
     * @param ingredients
     * @return the mached EasyRecipe instance, null if none of the recipes match
     */
    public static EasyRecipe getValidRecipe(EasyItemStack result, ItemStack[] ingredients) {
        ArrayList<EasyRecipe> all = getAllRecipes();
        allLoop: for (EasyRecipe r : all) {
            if (r.getResult().equals(result) && r.getIngredientsSize() == ingredients.length) {
                for (int j = 0; j < r.getIngredientsSize(); j++) {
                    if (r.getIngredient(j) instanceof EasyItemStack) {
                        EasyItemStack eis = (EasyItemStack) r.getIngredient(j);
                        if (!eis.equalsItemStack(ingredients[j])) {
                            continue allLoop;
                        }
                    } else if (r.getIngredient(j) instanceof ArrayList) {
                        if (ingredients[j].itemID != -1) {
                            // TODO: check against oredict
                            continue allLoop;
                        }
                    }
                }
                return r;
            }
        }
        return null;
    }

    /**
     * How many times can we fit the resulting itemstack in players hand.
     * 
     * @param result itemstack we are trying to fit
     * @param inHand itemstack currently in hand
     */
    public static int calculateCraftingMultiplierUntilMaxStack(ItemStack result, ItemStack inHand) {
        // TODO: there has to be a better way to calculate this
        int maxTimes = (int) ((double) result.getMaxStackSize() / (double) result.stackSize);
        if (inHand != null) {
            int diff = result.getMaxStackSize() - (maxTimes * result.stackSize);
            if (inHand.stackSize > diff) {
                maxTimes -= (int) (((double) (inHand.stackSize - diff) / (double) result.stackSize) + 1);
            }
        }
        return maxTimes;
    }

    /**
     * Get the recipe from the gui index position.
     */
    public static EasyRecipe getValidRecipe(GuiCrafting gui, int slot_index, ItemStack result) {
        /* TODO: find a better way 
        int recipe_index = slot_index + (gui.currentScroll * 8);
        if (recipe_index >= 0 && gui.renderList != null && recipe_index < gui.renderList.size()) {
            EasyRecipe r = gui.renderList.get(recipe_index);
            if (r.getResult().equalsItemStack(result) && gui.craftableList.contains(r)) {
                return r;
            }
        } */
        return null;
    }

    /**
     *  
     */
    // TODO: javadoc
    public static ArrayList<ItemStack> resolveOreAndLiquidDictionaries(String string) {
    	boolean forge = true;
        try {
        	 Class.forName("net.minecraftforge.oredict.ShapedOreRecipe");
        } 
        catch( ClassNotFoundException e )
        {
        	forge = false; 
        }
        if(!forge)
        	return new ArrayList<ItemStack>();
        
        if (string.startsWith("liquid$")) {
            ArrayList<ItemStack> result = new ArrayList<ItemStack>();

            int separator = string.indexOf(':');
            int id;
            int meta = -1;

            try {
                if (separator > -1) {
                    id = Integer.parseInt(string.substring(7, separator - 1));
                    meta = Integer.parseInt(string.substring(separator + 1));
                } else {
                    id = Integer.parseInt(string.substring(7));
                }
            } catch (NumberFormatException e) {
                return result;
            }

            for (net.minecraftforge.liquids.LiquidContainerData data : net.minecraftforge.liquids.LiquidContainerRegistry.getRegisteredLiquidContainerData()) {
                if ((data.stillLiquid.itemID == id) && ((meta == -1) || (data.stillLiquid.itemMeta == meta))) {
                    result.add(data.filled);
                }
            }

            return result;
        }
        return net.minecraftforge.oredict.OreDictionary.getOres(string);
    }

    /**
     * Compare recipes by their results (id, damage, stack size).
     */
    public static class RecipeComparator implements Comparator<EasyRecipe> {

        @Override
        public int compare(EasyRecipe o1, EasyRecipe o2) {
        	if(o1 == null && o2 == null)
        		return 0;
        	else if(o1 == null || o2 == null)
        		return 1;
            if (o1.getResult().getID() > o2.getResult().getID()) {
                return 1;
            } else if (o1.getResult().getID() < o2.getResult().getID()) {
                return -1;
            }

            if (o1.getResult().getDamage() > o2.getResult().getDamage()) {
                return 1;
            } else if (o1.getResult().getDamage() < o2.getResult().getDamage()) {
                return -1;
            }

            if (o1.getResult().getSize() > o2.getResult().getSize()) {
                return 1;
            } else if (o1.getResult().getSize() < o2.getResult().getSize()) {
                return -1;
            }
            return 0;
        }
    }
}
