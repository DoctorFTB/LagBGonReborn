package ftblag.lagbgonreborn;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

@Mod.EventBusSubscriber(modid = LagBGonReborn.MODID)
public class LBGEvents {

    @SubscribeEvent
    public static void checkCrowding(EntityJoinWorldEvent e) {
        if (LBGConfig.perChunkSpawnLimit > 0 && e.getEntity() instanceof EntityLiving) {
            Chunk chunk = e.getWorld().getChunk((int) e.getEntity().posX, (int) e.getEntity().posZ);
            int count = 0;
            for (ClassInheritanceMultiMap<Entity> list : chunk.getEntityLists()) {
                count += list.size();
            }
            if (count >= LBGConfig.perChunkSpawnLimit) {
                e.setCanceled(true);
                return;
            }

        }

        if (!LBGConfig.policeCrowd) {
            return;
        }
        Entity ent = e.getEntity();
        if (ent instanceof EntityAgeable) {
            if (((EntityAgeable) ent).getGrowingAge() < 0) {
                int amt = ent.world.getEntitiesWithinAABB(EntityAgeable.class, new AxisAlignedBB(ent.getPosition()).grow(5)).size();
                if (amt > LBGConfig.crowdLimit) {
                    e.setCanceled(true);
                }
            }
        }
    }

    public static long nextClear;
    private static boolean warned = false;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.START)
            return;
        if (LBGCommand.checkTPS()) {
            return;
        }

        if (nextClear == 0) {
            nextClear = System.currentTimeMillis() + (LBGConfig.timeInterval * 1000 * 60);
            return;
        }
        if (nextClear < System.currentTimeMillis() && LBGConfig.automaticRemoval) {
            if (warned) {
                nextClear = System.currentTimeMillis() + (LBGConfig.timeInterval * 1000 * 60);
                LBGCommand.doClear();
                warned = false;
            } else {
                nextClear = System.currentTimeMillis() + 1000 * 60;
                String style = LBGConfig.redBoldWarning ? TextFormatting.RED.toString() + TextFormatting.BOLD : "";
                FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString(style + "Lag'B'Gon will be removing items in 1 minute!"));
                warned = true;
            }
        }
    }
}
