package railcraft.common.api.carts;

import cpw.mods.fml.common.registry.EntityRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockRail;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import railcraft.common.api.core.items.IMinecartItem;

public abstract class CartTools
{

    public static ILinkageManager serverLinkageManager;

    /**
     * Registers a subclass of EntityMinecart with the game engine.
     *
     * This is just a convenience function, it is not required to call this function
     * if you call ModLoader.registerEntityID() and MinecraftForge.registerEntity()
     * elsewhere.
     *
     * @param mod The mod doing the registration
     * @param type The class of the cart
     * @param tag The String identifier
     * @param internalId The mods internal entity id
     */
    public static void registerMinecart(Object mod, Class<? extends EntityMinecart> type, String tag, int internalId) {
        EntityRegistry.registerModEntity(type, tag, internalId, mod, 80, 3, true);
    }

    /**
     * Returns an instance of ILinkageManager.
     *
     * Will return null if Railcraft is not installed.
     *
     * @param world The World, may be required in the future
     * @return an instance of ILinkageManager
     */
    public static ILinkageManager getLinkageManager(World world) {
        return serverLinkageManager;
    }

    /**
     * Sets a carts owner.
     *
     * The is really only needed by the bukkit ports.
     *
     * @param owner
     */
    public static void setCartOwner(EntityMinecart cart, EntityPlayer owner) {
        cart.getEntityData().setString("owner", owner.username);
    }

    /**
     * Sets a carts owner.
     *
     * The is really only needed by the bukkit ports.
     *
     * @param owner
     */
    public static void setCartOwner(EntityMinecart cart, String owner) {
        cart.getEntityData().setString("owner", owner);
    }

    /**
     * Gets a carts owner. (player.username)
     *
     * The is really only needed by the bukkit ports.
     *
     * @param owner
     */
    public static String getCartOwner(EntityMinecart cart) {
        return cart.getEntityData().getString("owner");
    }

    /**
     * Will return true if the cart matches the provided filter item.
     *
     * @param stack the Filter
     * @param cart the Cart
     * @return true if the item matches the cart
     * @see IMinecart
     */
    public static boolean doesCartMatchFilter(ItemStack stack, EntityMinecart cart) {
        if(stack == null) {
            return false;
        }
        if(cart instanceof IMinecart) {
            return ((IMinecart)cart).doesCartMatchFilter(stack, cart);
        }
        ItemStack cartItem = cart.getCartItem();
        return cartItem != null && isItemEqual(stack, cartItem);
    }

    private static boolean isItemEqual(ItemStack a, ItemStack b) {
        if(a == null || b == null) {
            return false;
        }
        if(a.itemID != b.itemID) {
            return false;
        }
        if(a.stackTagCompound != null && !a.stackTagCompound.equals(b.stackTagCompound)) {
            return false;
        }
        if(a.getHasSubtypes() && (a.getItemDamage() == -1 || b.getItemDamage() == -1)) {
            return true;
        }
        if(a.getHasSubtypes() && a.getItemDamage() != b.getItemDamage()) {
            return false;
        }
        return true;
    }

    /**
     * Spawns a new cart entity using the provided item.
     *
     * The backing item must implement <code>IMinecartItem</code>
     * and/or extend <code>ItemMinecart</code>.
     *
     * Generally Forge requires all cart items to extend ItemMinecart.
     *
     * @param owner The player name that should used as the owner
     * @param cart An ItemStack containing a cart item, will not be changed by the function
     * @param world The World object
     * @param i x-Coord
     * @param j y-Coord
     * @param k z-Coord
     * @return the cart placed or null if failed
     * @see IMinecartItem, ItemMinecart
     */
    public static EntityMinecart placeCart(String owner, ItemStack cart, World world, int i, int j, int k) {
        if(cart == null) {
            return null;
        }
        cart = cart.copy();
        if(cart.getItem() instanceof IMinecartItem) {
            IMinecartItem mi = (IMinecartItem)cart.getItem();
            return mi.placeCart(owner, cart, world, i, j, k);
        } else if(cart.getItem() instanceof ItemMinecart) {
            try {
                boolean placed = cart.getItem().onItemUse(cart, null, world, i, j, k, 0, 0, 0, 0);
                if(placed) {
                    List<EntityMinecart> carts = getMinecartsAt(world, i, j, k, 0.3f);
                    if(carts.size() > 0) {
                        setCartOwner(carts.get(0), owner);
                        return carts.get(0);
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Offers an item stack to linked carts or drops it if no one wants it.
     * @param cart
     * @param stack
     */
    public static void offerOrDropItem(EntityMinecart cart, ItemStack stack) {
        EntityMinecart link_A = getLinkageManager(cart.worldObj).getLinkedCartA(cart);
        EntityMinecart link_B = getLinkageManager(cart.worldObj).getLinkedCartB(cart);

        if(stack != null && stack.stackSize > 0 && link_A instanceof IItemTransfer) {
            stack = ((IItemTransfer)link_A).offerItem(cart, stack);
        }
        if(stack != null && stack.stackSize > 0 && link_B instanceof IItemTransfer) {
            stack = ((IItemTransfer)link_B).offerItem(cart, stack);
        }

        if(stack != null && stack.stackSize > 0) {
            cart.entityDropItem(stack, 1);
        }
    }

    public static boolean isMinecartOnRailAt(World world, int i, int j, int k, float sensitivity) {
        return isMinecartOnRailAt(world, i, j, k, sensitivity, null, true);
    }

    public static boolean isMinecartOnRailAt(World world, int i, int j, int k, float sensitivity, Class<? extends EntityMinecart> type, boolean subclass) {
        if(BlockRail.isRailBlockAt(world, i, j, k)) {
            return isMinecartAt(world, i, j, k, sensitivity, type, subclass);
        }
        return false;
    }

    public static boolean isMinecartOnAnySide(World world, int i, int j, int k, float sensitivity) {
        return isMinecartOnAnySide(world, i, j, k, sensitivity, null, true);
    }

    public static boolean isMinecartOnAnySide(World world, int i, int j, int k, float sensitivity, Class<? extends EntityMinecart> type, boolean subclass) {
        List<EntityMinecart> list = new ArrayList<EntityMinecart>();
        for(int side = 0; side < 6; side++) {
            list.addAll(getMinecartsOnSide(world, i, j, k, sensitivity, ForgeDirection.getOrientation(side)));
        }

        if(type == null) {
            return !list.isEmpty();
        } else {
            for(EntityMinecart cart : list) {
                if((subclass && type.isInstance(cart)) || cart.getClass() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMinecartAt(World world, int i, int j, int k, float sensitivity) {
        return isMinecartAt(world, i, j, k, sensitivity, null, true);
    }

    public static boolean isMinecartAt(World world, int i, int j, int k, float sensitivity, Class<? extends EntityMinecart> type, boolean subclass) {
        List<EntityMinecart> list = getMinecartsAt(world, i, j, k, sensitivity);

        if(type == null) {
            return !list.isEmpty();
        } else {
            for(EntityMinecart cart : list) {
                if((subclass && type.isInstance(cart)) || cart.getClass() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<EntityMinecart> getMinecartsOnAllSides(World world, int i, int j, int k, float sensitivity) {
        List<EntityMinecart> carts = new ArrayList<EntityMinecart>();
        for(int side = 0; side < 6; side++) {
            carts.addAll(getMinecartsOnSide(world, i, j, k, sensitivity, ForgeDirection.getOrientation(side)));
        }

        return carts;
    }

    public static List<EntityMinecart> getMinecartsOnAllSides(World world, int i, int j, int k, float sensitivity, Class<? extends EntityMinecart> type, boolean subclass) {
        List<EntityMinecart> list = new ArrayList<EntityMinecart>();
        List<EntityMinecart> carts = new ArrayList<EntityMinecart>();
        for(int side = 0; side < 6; side++) {
            list.addAll(getMinecartsOnSide(world, i, j, k, sensitivity, ForgeDirection.getOrientation(side)));
        }

        for(EntityMinecart cart : list) {
            if((subclass && type.isInstance(cart)) || cart.getClass() == type) {
                carts.add(cart);
            }
        }
        return carts;
    }

    private static int getYOnSide(int y, ForgeDirection side) {
        switch (side) {
            case UP:
                return y + 1;
            case DOWN:
                return y - 1;
            default:
                return y;
        }
    }

    private static int getXOnSide(int x, ForgeDirection side) {
        switch (side) {
            case EAST:
                return x + 1;
            case WEST:
                return x - 1;
            default:
                return x;
        }
    }

    private static int getZOnSide(int z, ForgeDirection side) {
        switch (side) {
            case NORTH:
                return z - 1;
            case SOUTH:
                return z + 1;
            default:
                return z;
        }
    }

    public static List<EntityMinecart> getMinecartsOnSide(World world, int i, int j, int k, float sensitivity, ForgeDirection side) {
        return getMinecartsAt(world, getXOnSide(i, side), getYOnSide(j, side), getZOnSide(k, side), sensitivity);
    }

    public static boolean isMinecartOnSide(World world, int i, int j, int k, float sensitivity, ForgeDirection side) {
        return getMinecartOnSide(world, i, j, k, sensitivity, side) != null;
    }

    public static EntityMinecart getMinecartOnSide(World world, int i, int j, int k, float sensitivity, ForgeDirection side) {
        for(EntityMinecart cart : getMinecartsOnSide(world, i, j, k, sensitivity, side)) {
            return cart;
        }
        return null;
    }

    public static boolean isMinecartOnSide(World world, int i, int j, int k, float sensitivity, ForgeDirection side, Class<? extends EntityMinecart> type, boolean subclass) {
        return getMinecartOnSide(world, i, j, k, sensitivity, side, type, subclass) != null;
    }

    public static EntityMinecart getMinecartOnSide(World world, int i, int j, int k, float sensitivity, ForgeDirection side, Class<? extends EntityMinecart> type, boolean subclass) {
        for(EntityMinecart cart : getMinecartsOnSide(world, i, j, k, sensitivity, side)) {
            if(type == null || (subclass && type.isInstance(cart)) || cart.getClass() == type) {
                return cart;
            }
        }
        return null;
    }

    /**
     *
     * @param world
     * @param i
     * @param j
     * @param k
     * @param sensitivity Controls the size of the search box, ranges from (-inf, 0.49].
     * @return
     */
    public static List<EntityMinecart> getMinecartsAt(World world, int i, int j, int k, float sensitivity) {
        sensitivity = Math.min(sensitivity, 0.49f);
        List entities = world.getEntitiesWithinAABB(EntityMinecart.class, AxisAlignedBB.getAABBPool().addOrModifyAABBInPool(i + sensitivity, j + sensitivity, k + sensitivity, i + 1 - sensitivity, j + 1 - sensitivity, k + 1 - sensitivity));
        List<EntityMinecart> carts = new ArrayList<EntityMinecart>();
        for(Object o : entities) {
            carts.add((EntityMinecart)o);
        }
        return carts;
    }

    public static List<EntityMinecart> getMinecartsIn(World world, int i1, int j1, int k1, int i2, int j2, int k2) {
        List entities = world.getEntitiesWithinAABB(EntityMinecart.class, AxisAlignedBB.getAABBPool().addOrModifyAABBInPool(i1, j1, k1, i2, j2, k2));
        List<EntityMinecart> carts = new ArrayList<EntityMinecart>();
        for(Object o : entities) {
            carts.add((EntityMinecart)o);
        }
        return carts;
    }

    /**
     * Returns the cart's "speed". It is not capped by the carts max speed,
     * it instead returns the cart's "potential" speed.
     * Used by collision and linkage logic.
     * Do not use this to determine how fast a cart is currently moving.
     * @param cart
     * @return speed
     */
    public static double getCartSpeedUncapped(EntityMinecart cart) {
        return Math.sqrt(cart.motionX * cart.motionX + cart.motionZ * cart.motionZ);
    }

    public static boolean cartVelocityIsLessThan(EntityMinecart cart, float vel) {
        return Math.abs(cart.motionX) < vel && Math.abs(cart.motionZ) < vel;
    }
}
