package ftblag.lagbgonreborn;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

public class LBGCommand extends CommandBase {

    private static LBGConfig config = LBGConfig.ins();
    private static long nextUnload;
    private static final String[] al = new String[]{"toggleitem", "toggleentity", "clear", "interval", "toggleauto", "listitems", "listentities", "settps", "unload", "blacklist", "togglepolice", "setbreedlimit", "scanentities", "maxperchunk"};
//    private static ArrayList<String> alias = new ArrayList<String>(Arrays.asList(new String[]{"toggleitem", "toggleentity", "clear", "interval", "toggleauto", "listitems", "listentities", "settps", "unload", "blacklist", "togglepolice", "setbreedlimit", "scanentities", "maxperchunk"}));

    @Override
    public String getName() {
        return "bgon";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bgon : Shows help for using Lag'B'Gon Reborn";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, al);
        else if (args.length == 2 && args[0].equals("toggleentity"))
            return getListOfStringsMatchingLastWord(args, EntityList.getEntityNameList());
        else
            return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            LagBGonReborn.sendMsg(sender, "/bgon toggleitem: Toggles the blacklist status of held item.");
            LagBGonReborn.sendMsg(sender, "/bgon toggleentity <modid:name>: Toggles the blacklist status of the named entity.");
            LagBGonReborn.sendMsg(sender, "/bgon clear: Clears all items/entities from the world not on blacklist.");
            LagBGonReborn.sendMsg(sender, "/bgon interval <minutes>: Sets the interval for automatic running of /bgon clear. The interval is actually 1 minute longer, as it includes a 1 minute warning.");
            LagBGonReborn.sendMsg(sender, "/bgon toggleauto: Toggles automatic clearing of entities, and unloading of chunks.");
            LagBGonReborn.sendMsg(sender, "/bgon listitems: Lists the items in the blacklist.");
            LagBGonReborn.sendMsg(sender, "/bgon listentities: Lists the entities in the blacklist.");
            LagBGonReborn.sendMsg(sender, "/bgon settps <target tps>: Sets the maximum TPS for unloading chunks.");
            LagBGonReborn.sendMsg(sender, "/bgon unload: Unloads unused chunks.");
            LagBGonReborn.sendMsg(sender, "/bgon blacklist: Switches between using blacklist and whitelist.");
            LagBGonReborn.sendMsg(sender, "/bgon togglepolice: Toggles Breeding policing.");
            LagBGonReborn.sendMsg(sender, "/bgon setbreedlimit <amount>: Sets the limit for breeding.");
            LagBGonReborn.sendMsg(sender, "/bgon scanentities: Lists nearby entities,by name, for blacklisting.");
            LagBGonReborn.sendMsg(sender, "/bgon maxperchunk <amount>: Sets maximum entities to spawn per chunk.");
            LagBGonReborn.sendMsg(sender, "/bgon togglenamedremove: Toggle Named Remove.");
        } else if (args.length == 1) {
            if (args[0].equals("blacklist")) {
                config.toggleBlacklist();
                LagBGonReborn.sendMsg(sender, (LBGConfig.blacklist ? "Black" : "While") + "list enabled.");
            } else if (args[0].equals("scanentities")) {
                if (!(sender instanceof EntityPlayer)) {
                    LagBGonReborn.sendMsg(sender, "Only for players!");
                    return;
                }
                scanEntities((EntityPlayer) sender);
            } else if (args[0].equals("togglepolice")) {
                config.togglePolice();
                LagBGonReborn.sendMsg(sender, "Breeding policing " + (LBGConfig.policeCrowd ? "en" : "dis") + "abled.");
            } else if (args[0].equals("unload")) {
                unloadChunks();
            } else if (args[0].equals("listitems")) {
                StringBuilder line = new StringBuilder();
                LagBGonReborn.sendMsg(sender, "Item Blacklist contains:");
                for (String item : LBGConfig.itemBlacklist) {
                    if (line.length() > 40) {
                        LagBGonReborn.sendMsg(sender, line.toString());
                        line = new StringBuilder();
                    }
                    line.append(item);
                    line.append(", ");
                }
                if (line.length() > 0) {
                    LagBGonReborn.sendMsg(sender, (String) line.toString().subSequence(0, line.length() - 2));
                }
            } else if (args[0].equals("listentities")) {
                StringBuilder line = new StringBuilder();
                LagBGonReborn.sendMsg(sender, "Entity Blacklist contains:");
                for (String item : LBGConfig.entityBlacklist) {
                    if (line.length() > 40) {
                        LagBGonReborn.sendMsg(sender, line.toString());
                        line = new StringBuilder();
                    }
                    line.append(item);
                    line.append(", ");
                }
                if (line.length() > 0) {
                    LagBGonReborn.sendMsg(sender, (String) line.toString().subSequence(0, line.length() - 2));
                }
            } else if (args[0].equals("toggleauto")) {
                config.toggleAuto();
                LagBGonReborn.sendMsg(sender, "Automatic clearing " + (LBGConfig.automaticRemoval ? "en" : "dis") + "abled.");
            } else if (args[0].equals("toggleitem")) {
                if (!(sender instanceof EntityPlayer)) {
                    LagBGonReborn.sendMsg(sender, "Only for players!");
                    return;
                }
                EntityPlayer plr = (EntityPlayer) sender;
                if (plr.getHeldItemMainhand().isEmpty()) {
                    LagBGonReborn.sendMsg(sender, "You must have an item selected");
                    return;
                }
                Item item = plr.getHeldItemMainhand().getItem();
                config.toggleItem(item);
                boolean hav = !config.isBlacklisted(plr.getHeldItemMainhand().getItem());
                String nam = item.getItemStackDisplayName(plr.getHeldItemMainhand());
                LagBGonReborn.sendMsg(sender, nam + (hav ? " removed from" : " added to") + " blacklist.");
            } else if (args[0].equals("clear")) {
                if (!DimensionManager.getWorld(0).isRemote) {
                    doClear();
                }
            } else if (args[0].equals("togglenamedremove")) {
                config.toggleNamedRemove();
                LagBGonReborn.sendMsg(sender, "Named Remove " + (LBGConfig.namedRemove ? "en" : "dis") + "abled.");
            } else {
                LagBGonReborn.sendMsg(sender, "Cmd not found!");
            }
        } else if (args.length == 2) {
            if (args[0].equals("maxperchunk")) {
                int max = Integer.parseInt(args[1]);
                config.changeMaxPerChunk(max);
                LagBGonReborn.sendMsg(sender, "New Maximium spawns per chunk: " + max);
            } else if (args[0].equals("setbreedlimit")) {
                int limit = Integer.parseInt(args[1]);
                config.changeCrowdLimit(limit);
                LagBGonReborn.sendMsg(sender, "Breeding limit set to: " + LBGConfig.crowdLimit);
            } else if (args[0].equals("toggleentity")) {
                config.toggleEntity(args[1]);
                boolean hav = config.isBlacklisted(args[1]);
                LagBGonReborn.sendMsg(sender, args[1] + " has been " + (hav ? "added to" : "removed from") + " the blacklist.");
                LBGConfig.checkEntityBlacklist();
            } else if (args[0].equals("interval")) {
                int newInterval = Integer.parseInt(args[1]);
                config.changeInterval(newInterval);
                LBGEvents.nextClear = System.currentTimeMillis() + (LBGConfig.timeInterval * 1000 * 60);
                LagBGonReborn.sendMsg(sender, "Automatic removal interval set to: " + newInterval);
            } else if (args[0].equals("unload")) {
                int newInterval = Integer.parseInt(args[1]);
                config.changeUnload(newInterval);
                nextUnload = System.currentTimeMillis() + (LBGConfig.timeUnload * 1000 * 60);
                LagBGonReborn.sendMsg(sender, "Automatic removal unload interval set to: " + newInterval);
            } else if (args[0].equals("settps")) {
                int newTPS = Integer.parseInt(args[1]);
                config.changeTPSForUnload(newTPS);
                LagBGonReborn.sendMsg(sender, "New TPS minimum set to: " + newTPS);
            } else {
                LagBGonReborn.sendMsg(sender, "Cmd not found!");
            }
        } else {
            if (args[0].equals("toggleentity")) {
                StringBuilder name = new StringBuilder();
                for (String word : args) {
                    if (!word.equals("toggleentity")) {
                        name.append(word);
                        name.append(" ");
                    }
                }
                name.replace(name.length() - 1, name.length(), "");
                config.toggleEntity(name.toString());
                boolean hav = config.isBlacklisted(name.toString());
                LagBGonReborn.sendMsg(sender, name.toString() + " has been " + (hav ? "added to" : "removed from") + " the blacklist.");
            } else {
                LagBGonReborn.sendMsg(sender, "Cmd not found!");
            }
        }
    }

    public static void doClear() {
        EntityItem item;
        Entity entity;
        int itemsRemoved = 0;
        int entitiesRemoved = 0;
        ArrayList<Entity> toRemove = new ArrayList<>();
        for (World world : DimensionManager.getWorlds()) {
            if (world == null) {
                continue;
            }
            if (world.isRemote) {
                System.out.println("How?!?");
            }
            Iterator<Entity> iter = world.loadedEntityList.iterator();
            Entity obj;
            while (iter.hasNext()) {
                obj = iter.next();
                if (obj instanceof EntityItem) {
                    item = (EntityItem) obj;
                    if (LBGConfig.blacklist && config.isBlacklisted(item.getItem().getItem())) {
                        toRemove.add(item);
                        itemsRemoved++;
                    }
                    if (!LBGConfig.blacklist && !config.isBlacklisted(item.getItem().getItem())) {
                        toRemove.add(item);
                        itemsRemoved++;
                    }
                } else if (!(obj instanceof EntityPlayer)) {
                    entity = obj;
                    if (!config.isBlacklisted(entity) && LBGConfig.blacklist) {
                        toRemove.add(entity);
                        entitiesRemoved++;
                    }
                    if (config.isBlacklisted(entity) && !LBGConfig.blacklist) {
                        toRemove.add(entity);
                        entitiesRemoved++;
                    }
                }
            }
            for (Entity e : toRemove) {
                if (e.hasCustomName() && LBGConfig.namedRemove || !e.hasCustomName() && !LBGConfig.namedRemove)
                    e.setDead();
            }
            toRemove.clear();
        }
        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString("Lag'B'Gon has removed " + itemsRemoved + " items and " + entitiesRemoved + " entities"));
    }

    @Override
    public int getRequiredPermissionLevel() {

        return 2;
    }

    private static long mean(long num[]) {
        long val = 0;
        for (long n : num) {
            val += n;
        }
        return val / num.length;
    }

    private static boolean unloadChunks() {

        ChunkProviderServer cPS;
        int oldChunksLoaded;
        int newChunksLoaded;
        boolean unloadSafe;

        oldChunksLoaded = 0;
        newChunksLoaded = 0;

        List<ChunkPos> playerPos = Lists.newArrayList();
        int radius = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getViewDistance() + 1;
        for (EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            for (int x = player.chunkCoordX - radius; x <= player.chunkCoordX + radius; x++) {
                for (int z = player.chunkCoordZ - radius; z <= player.chunkCoordZ + radius; z++) {
                    playerPos.add(new ChunkPos(x, z));
                }
            }
        }

        for (WorldServer world : DimensionManager.getWorlds()) {
            oldChunksLoaded += world.getChunkProvider().getLoadedChunkCount();
            if (world.getChunkProvider() instanceof ChunkProviderServer) {
                cPS = world.getChunkProvider();

                for (Chunk chunk : cPS.loadedChunks.values()) {
                    ChunkPos chunkPos = new ChunkPos(chunk.x, chunk.z);
                    unloadSafe = !world.getPersistentChunks().containsKey(chunkPos);
                    if (unloadSafe) {
                        unloadSafe = !playerPos.contains(chunkPos);
//                        for (EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
//                            if ((player.chunkCoordX == chunk.x && player.chunkCoordZ == chunk.z)) {
//                                unloadSafe = false;
//                                break;
//                            }
//                        }
                    }
                    if (unloadSafe) {
                        cPS.queueUnload(chunk);
                    }

                }
                cPS.tick();

            }
            newChunksLoaded += world.getChunkProvider().getLoadedChunkCount();
        }
        nextUnload = System.currentTimeMillis() + (LBGConfig.timeUnload * 1000 * 60);
        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString((oldChunksLoaded - newChunksLoaded) + " chunks unloaded by Lag'B'Gon."));

        return true;

    }

    public static boolean checkTPS() {
        double meanTickTime = mean(FMLCommonHandler.instance().getMinecraftServerInstance().tickTimeArray) * 1.0E-6D;
        double meanTPS = Math.min(1000.0 / meanTickTime, 20);
        if (nextUnload < System.currentTimeMillis()) {
            if (meanTPS < LBGConfig.TPSForUnload) {
                unloadChunks();
                return true;
            }
        }
        return false;
    }

    private void scanEntities(EntityPlayer plr) {
        List<Entity> entities = plr.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(plr.getPosition()).grow(5));
        if (entities.isEmpty())
            return;
        ArrayList<String> entityNames = new ArrayList<>();
        for (Entity ent : entities) {
            if (!entityNames.contains(ent instanceof EntityPlayer ? ent.getName() : EntityList.getKey(ent).toString())) {
                entityNames.add(ent instanceof EntityPlayer ? ent.getName() : EntityList.getKey(ent).toString());
            }
        }

        StringBuilder line = new StringBuilder();
        LagBGonReborn.sendMsg(plr, "Nearby Entities");
        for (String item : entityNames) {
            if (line.length() > 40) {
                LagBGonReborn.sendMsg(plr, line.toString());
                line = new StringBuilder();
            }
            line.append(item);
            line.append(", ");
        }
        if (line.length() > 0) {
            LagBGonReborn.sendMsg(plr, (String) line.toString().subSequence(0, line.length() - 2));
        }
    }
}
