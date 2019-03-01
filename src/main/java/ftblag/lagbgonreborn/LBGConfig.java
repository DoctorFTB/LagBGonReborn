package ftblag.lagbgonreborn;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LBGConfig {
    public static Configuration cfg;

    public static List<String> entityBlacklist = new ArrayList<>(), itemBlacklist = new ArrayList<>();
//    public static ArrayList<Item> itemsBlackList = new ArrayList<>();
    public static int timeInterval, TPSForUnload, crowdLimit, perChunkSpawnLimit, timeUnload;
    public static boolean automaticRemoval, policeCrowd, blacklist, namedRemove, redBoldWarning;

    private static LBGConfig ins;

    public static LBGConfig ins() {
        if (ins == null) {
            ins = new LBGConfig();
        }
        return ins;
    }

    public void init(Configuration cfg) {
        LBGConfig.cfg = cfg;
    }

    public void load() {
        cfg.load();
        entityBlacklist = new ArrayList<>(Arrays.asList(cfg.get(Configuration.CATEGORY_GENERAL, "EntityBlackList", new String[]{"minecraft:cow",}, "List of Entities not to destroy.").getStringList()));
        itemBlacklist = new ArrayList<>(Arrays.asList(cfg.get(Configuration.CATEGORY_GENERAL, "ItemBlackList", new String[]{"minecraft:diamond",}, "List of Items not to destroy").getStringList()));
        timeInterval = cfg.get(Configuration.CATEGORY_GENERAL, "Interval", 15, "Interval between clearing entities in minutes. The interval is actually 1 minute longer, as it includes a 1 minute warning.").getInt();
        automaticRemoval = cfg.get(Configuration.CATEGORY_GENERAL, "AutomaticRemoval", true).getBoolean();
        TPSForUnload = cfg.get(Configuration.CATEGORY_GENERAL, "TPSForUnload", 12, "If the server's main TPS drops below this number, \\n Lag'B'Gon will try to unload chunks to improve TPS").getInt();
        crowdLimit = cfg.get("Breeding", "CrowdLimit", 10).getInt();
        policeCrowd = cfg.get("Breeding", "PoliceCrowding", false, "Prevent overbreeding.  If at least CrowdLimit breedable \\n animals are within five blocks, new babies will not \\nspawn.\\n Setting this value to less than 3 prevents breeding entirely.").getBoolean();
        perChunkSpawnLimit = cfg.get(Configuration.CATEGORY_GENERAL, "PerChunkSpawnLimit", 0, "Maximum mobs spawnable per chunk.  0 disables.").getInt();
        blacklist = cfg.get(Configuration.CATEGORY_GENERAL, "Blacklist", true, "Should we use a blacklist or a whitelist for mob removal? \\n Blacklist is default.").getBoolean();
        namedRemove = cfg.get(Configuration.CATEGORY_GENERAL, "NamedRemove", false, "Remove named entities? (With name tag)").getBoolean();
        timeUnload = cfg.get(Configuration.CATEGORY_GENERAL, "Unload", 15, "Interval between unload chunks in minutes.").getInt();
        redBoldWarning = cfg.get(Configuration.CATEGORY_GENERAL, "RedBoldWarning", false, "Add red bold styles for warning message").getBoolean();
        cfg.save();
//        updateBlacklist();
        checkEntityBlacklist();
    }

    public static void checkEntityBlacklist() {
        for (String str : entityBlacklist) {
            if (!str.contains("*") && !ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(str))) {
                System.out.println("[LagBGonReborn] Found error mob id! ID: " + str);
            }
        }
    }

//    private void updateBlacklist() {
//        itemsBlackList.clear();
//        for (String str : itemBlacklist) {
//            itemsBlackList.add(Item.REGISTRY.getObject(new ResourceLocation(str)));
//        }
//    }

    public void toggleAuto() {
        automaticRemoval = !automaticRemoval;
        save();
    }

    public void toggleBlacklist() {
        blacklist = !blacklist;
        save();
    }

    public void toggleNamedRemove() {
        namedRemove = !namedRemove;
        save();
    }

    public void changeMaxPerChunk(int newMax) {
        if (newMax < 0) {
            newMax = 0;
        }
        perChunkSpawnLimit = newMax;
        save();
    }

    public void changeCrowdLimit(int newLimit) {
        if (newLimit < 1) {
            newLimit = 1;
        }
        crowdLimit = newLimit;
        save();
    }

    public void changeInterval(int newInterval) {
        if (newInterval < 1) {
            newInterval = 1;
        }
        timeInterval = newInterval;
        save();
    }

    public void changeUnload(int newInterval) {
        if (newInterval < 1) {
            newInterval = 1;
        }
        timeUnload = newInterval;
        save();
    }

    public void changeTPSForUnload(int newTPS) {
        if (newTPS > 15) {
            TPSForUnload = 15;
        } else {
            TPSForUnload = newTPS;
        }
        save();
    }

    public void togglePolice() {
        policeCrowd = !policeCrowd;
        save();
    }

    public void toggleItem(Item item) {
//        if (itemsBlackList.contains(item)) {
//            itemBlacklist.remove(Item.REGISTRY.getNameForObject(item).toString());
//            itemsBlackList.remove(item);
//        } else {
//            itemBlacklist.add(Item.REGISTRY.getNameForObject(item).toString());
//            itemsBlackList.add(item);
//        }
        String name = item.getRegistryName().toString();
        if (itemBlacklist.contains(name)) {
//            itemBlacklist.remove(Item.REGISTRY.getNameForObject(item).toString());
            itemBlacklist.remove(name);
//            itemsBlackList.remove(item);
        } else {
//            itemBlacklist.add(Item.REGISTRY.getNameForObject(item).toString());
            itemBlacklist.add(name);
//            itemsBlackList.add(item);
        }
        save();
    }

    public void toggleEntity(String name) {
        if (entityBlacklist.contains(name)) {
            entityBlacklist.remove(name);
        } else {
            entityBlacklist.add(name);
        }
        save();
    }

    public boolean isBlacklisted(Item item) {
        ResourceLocation name = item.getRegistryName();
        return itemBlacklist.contains(name.toString()) || itemBlacklist.contains(name.getNamespace() + ":*");
    }

    public boolean isBlacklisted(Entity entity) {
        if (entity == null)
            return false;

        ResourceLocation rl = EntityList.getKey(entity);
        if (rl != null) {
            return entityBlacklist.contains(rl.toString()) || entityBlacklist.contains(rl.getNamespace() + ":*");
        } else {
            String className = entity.getClass().toString();
            System.out.println("Failed to get registered mob name! Class: " + className);
            return entityBlacklist.contains(className);
        }
    }

    public boolean isBlacklisted(String name) {
        return entityBlacklist.contains(name) || itemBlacklist.contains(name);
    }

    public static void save() {
        cfg.get(Configuration.CATEGORY_GENERAL, "EntityBlackList", new String[]{"minecraft:cow",}, "List of Entities not to destroy.").set(entityBlacklist.toArray(new String[entityBlacklist.size()]));
        cfg.get(Configuration.CATEGORY_GENERAL, "ItemBlackList", new String[]{"minecraft:diamond",}, "List of Items not to destroy").set(itemBlacklist.toArray(new String[itemBlacklist.size()]));
        cfg.get(Configuration.CATEGORY_GENERAL, "Interval", 15, "Interval between clearing entities in minutes.").set(timeInterval);
        cfg.get(Configuration.CATEGORY_GENERAL, "AutomaticRemoval", true).set(automaticRemoval);
        cfg.get(Configuration.CATEGORY_GENERAL, "TPSForUnload", 12, "If the server's main TPS drops below this number, \\n Lag'B'Gon will try to unload chunks to improve TPS").set(TPSForUnload);
        cfg.get("Breeding", "CrowdLimit", 10).set(crowdLimit);
        cfg.get("Breeding", "PoliceCrowding", false, "Prevent overbreeding.  If at least CrowdLimit breedable \\n animals are within five blocks, new babies will not \\nspawn.\\n Setting this value to less than 3 prevents breeding entirely.").set(policeCrowd);
        cfg.get(Configuration.CATEGORY_GENERAL, "PerChunkSpawnLimit", 0, "Maximum mobs spawnable per chunk.  0 disables.").set(perChunkSpawnLimit);
        cfg.get(Configuration.CATEGORY_GENERAL, "Blacklist", true, "Should we use a blacklist or a whitelist for mob removal? \\n Blacklist is default.").set(blacklist);

        cfg.get(Configuration.CATEGORY_GENERAL, "NamedRemove", false, "Remove named entities? (With name tag)").set(namedRemove);
        cfg.get(Configuration.CATEGORY_GENERAL, "Unload", 15, "Interval between unload chunks in minutes.").set(timeUnload);
        cfg.save();
    }
}
