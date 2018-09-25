package ftblag.lagbgonreborn;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = LagBGonReborn.MODID, name = "Lag'B'Gon Reborn", version = LagBGonReborn.VERSION, serverSideOnly = true, acceptableRemoteVersions = "*")
public class LagBGonReborn {
    public static final String MODID = "lagbgonreborn";
    public static final String VERSION = "@VERSION@";

    @Mod.Instance(MODID)
    public static LagBGonReborn instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LBGConfig.ins().init(new Configuration(event.getSuggestedConfigurationFile()));
        LBGConfig.ins().load();
    }

    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new LBGCommand());
    }

    public static void sendMsg(ICommandSender sender, String str) {
        sender.sendMessage(new TextComponentString(str));
    }
}
