package steffen.easycraft.datatypes;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class EasyItemStack {

    private int id;
    private int damage;
    private int size;
    private int charge;
    private NBTTagCompound stackTagCompound;

    public EasyItemStack(int id, int damage, int size, int charge) {
        this.id = id;
        this.damage = damage;
        this.size = size;
        this.charge = charge;
    }

    public EasyItemStack(int id, int damage, int size) {
        this(id, damage, size, 0);
    }

    public EasyItemStack(int id, int damage) {
        this(id, damage, 1, 0);
    }

    public EasyItemStack(int id) {
        this(id, 0, 1, 0);
    }

    public int getID() {
        return id;
    }

    public int getDamage() {
        return damage;
    }

    public int getSize() {
        return size;
    }

    public int getCharge() {
        return charge;
    }

    public ItemStack toItemStack() {
        ItemStack is = new ItemStack(id, size, damage);
        is.setTagCompound(stackTagCompound);
        
        return is;
    }

    public static EasyItemStack fromItemStack(ItemStack is) {
        int charge = 0;
        if(is == null)
        	return null;
        EasyItemStack eis = new EasyItemStack(is.itemID, is.getItemDamage(), is.stackSize, charge);
        eis.stackTagCompound = is.getTagCompound();
        return eis;
    }

    public static boolean areStackTagsEqual(EasyItemStack is0, ItemStack is1) {
        if (is0 == null && is1 == null) {
            return true;
        } else {
            if (is0 != null && is1 != null) {
                if (is0.stackTagCompound == null && is1.stackTagCompound != null) {
                    return false;
                } else {
                    return is0.stackTagCompound == null || is0.stackTagCompound.equals(is1.stackTagCompound);
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public String toString() {
        return "EasyItemStack [id=" + id + ", damage=" + damage + ", size=" + size + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return equals(obj, false);
    }

    public boolean equals(Object obj, boolean ignoreSize) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EasyItemStack other = (EasyItemStack) obj;
        if (id != other.id) {
            return false;
        }
        if (damage != other.damage && damage != -1 && other.damage != -1) {
            return false;
        }
        if (!ignoreSize && size != other.size) {
            return false;
        }
        return true;
    }

    public boolean equalsItemStack(ItemStack is) {
        return equalsItemStack(is, false);
    }

    public boolean equalsItemStack(ItemStack is, boolean ignoreSize) {
        if (is == null) {
            return false;
        }
        if (id != is.itemID) {
            return false;
        }
        if (damage != is.getItemDamage() && damage != -1 && is.getItemDamage() != -1 && is.getHasSubtypes() ) {
            return false;
        }
        if (!ignoreSize && size != is.stackSize) {
            return false;
        }
        return true;
    }

    public void setCharge(ArrayList<ItemStack> usedIngredients) {
        int outputCharge = 0;

        if (usedIngredients != null) {
            for (int i = 0; i < usedIngredients.size(); i++) {
                ItemStack ingredient = usedIngredients.get(i);
            }
        }

        this.charge = outputCharge;
    }
}