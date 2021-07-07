package eu.codeways;

import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftFallingBlock;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestColors extends JavaPlugin implements Listener {

    private int task;
    private List<FallingBlock> fallingBlocks;

    @Override
    public void onEnable() {
        fallingBlocks = new LinkedList();
        getLogger().info("TestColor enabled");
        getServer().getPluginManager().registerEvents(this,this);
        locations = new LinkedList<Location>();
        this.task = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                if (!fallingBlocks.isEmpty()) {
                    fallingBlocks.forEach(fallingBlock -> {
                        Entity testEntity = ((CraftEntity) fallingBlock).getHandle();
                        NBTTagCompound tags = new NBTTagCompound();
                        testEntity.save(tags);
                        tags.set("Time",NBTTagShort.a((short)1));
                        getLogger().info(tags.get("Tags").asString());
                        testEntity.load(tags);
                    });
                }
            }
        }, 0,400L);
    }

    @Override
    public void onDisable() {
        getLogger().info("TestColor enabled");
        if(!fallingBlocks.isEmpty()) {
            fallingBlocks.forEach(fallingBlock -> {
                fallingBlock.remove();
            });
        }
    }

    @EventHandler
    public void playerPickupItem(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack stack = event.getItem().getItemStack();
        net.minecraft.server.v1_16_R3.ItemStack nmsCopy = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound itemCompound = (nmsCopy.hasTag()) ? nmsCopy.getTag() : new NBTTagCompound();
        if(entity instanceof Player) {
            /*List<MetadataValue> meta = item.getMetadata("pickable");

            if(!meta.isEmpty() && meta.get(0).asString().equals("false")) {
                event.setCancelled(true);
                return;
            }*/

            String pickable = itemCompound.getString("pickable");
            if(pickable.equals("false")) {
                event.setCancelled(true);
                return;
            }

            Player player = (Player) entity;
            player.sendMessage("picked up "+event.getItem().getName());
        }
    }

    List<Location> locations;

    @EventHandler
    public void PlayerRightClick(PlayerInteractEvent event) throws NoSuchFieldException, IllegalAccessException {
        Action action = event.getAction();
        Player player = event.getPlayer();

        if(action.equals(Action.RIGHT_CLICK_BLOCK)) {
            Block block = event.getClickedBlock();
            Location blockLoc = block.getLocation().add(new Vector(0.5,1,0.5));
            AtomicBoolean contains = new AtomicBoolean(false);
            locations.forEach( (Location loc) -> {
                if(loc.getX() == blockLoc.getX() && loc.getY() == blockLoc.getY() && loc.getZ() == blockLoc.getZ()) {
                    contains.set(true);
                }
            });

            if (!contains.get()) {
                locations.add(blockLoc);
                ItemStack stack = new ItemStack(Material.DIAMOND, 1);

                net.minecraft.server.v1_16_R3.ItemStack nmsCopy = CraftItemStack.asNMSCopy(stack);
                NBTTagCompound itemCompound = (nmsCopy.hasTag()) ? nmsCopy.getTag() : new NBTTagCompound();
                itemCompound.set("pickable", NBTTagString.a("false"));
                nmsCopy.setTag(itemCompound);
                stack = CraftItemStack.asBukkitCopy(nmsCopy);

                Item i = player.getWorld().dropItem(blockLoc, stack);
                i.setVelocity(new Vector());
                i.setInvulnerable(true);
                i.setMetadata("pickable", new FixedMetadataValue(this, "false"));
                i.setGravity(false);

                final EntityItem handle = (EntityItem) ((CraftItem) i).getHandle();
                final Field ageField = handle.getClass().getField("age");
                ageField.setAccessible(true);
                ageField.set(handle, Short.MIN_VALUE);

                FallingBlock fallingBlock = player.getWorld().spawnFallingBlock(blockLoc, Material.DIAMOND_BLOCK.createBlockData());
                fallingBlock.setGravity(false);
                fallingBlock.setDropItem(false);

                Entity testEntity = ((CraftEntity) fallingBlock).getHandle();
                NBTTagCompound tags = new NBTTagCompound();
                testEntity.save(tags);
                NBTTagList list = new NBTTagList();
                list.add(NBTTagString.a("undespawnable"));
                //tags.set("Time",NBTTagShort.a((short)-200));
                tags.set("Tags",list);
                testEntity.load(tags);

                fallingBlocks.add(fallingBlock);

                //final EntityFallingBlock blockHandle = (EntityFallingBlock) ((CraftFallingBlock) fallingBlock).getHandle();
                /*final Field timeField = blockHandle.getClass().getField("Time");
                this.getLogger().info(timeField.toString());
                timeField.setAccessible(true);
                timeField.set(blockHandle,-666);*/
            }
        }
    }

    @EventHandler
    public void playerMessageEvent(AsyncPlayerChatEvent event) {
        String message = translateHexCodes(event.getMessage());
        event.setMessage(message);
        getLogger().info(message);
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#(\\w{5}[0-9a-f])");

    //stolen from github lol
    private String translateHexCodes (String textToTranslate) {

        Matcher matcher = HEX_PATTERN.matcher(textToTranslate);
        StringBuffer buffer = new StringBuffer();

        while(matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }

        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());

    }
}
