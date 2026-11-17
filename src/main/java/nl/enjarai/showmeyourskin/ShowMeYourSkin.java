package nl.enjarai.showmeyourskin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import nl.enjarai.cicada.api.util.ProperLogger;
import nl.enjarai.showmeyourskin.config.ModConfigServer;
import nl.enjarai.showmeyourskin.net.ConfigSyncPacket;
import nl.enjarai.showmeyourskin.net.SettingsUpdatePacket;
import org.slf4j.Logger;

public class ShowMeYourSkin implements ModInitializer {
  public static final String MODID = "showmeyourskin";
  public static final Logger LOGGER = ProperLogger.getLogger(MODID);

  @Override
  public void onInitialize() {
    ModConfigServer.load();

    initNetworking();
  }

  // TODO:
  // - example armors
  // - use nearby players when available

  private static void initNetworking() {
    PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC);
    PayloadTypeRegistry.serverboundPlay().register(SettingsUpdatePacket.TYPE, SettingsUpdatePacket.STREAM_CODEC);
    ServerPlayNetworking.registerGlobalReceiver(SettingsUpdatePacket.TYPE, SettingsUpdatePacket::handleServer);

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      if (ServerPlayNetworking.canSend(handler, ConfigSyncPacket.TYPE)) {
        sender.sendPacket(new ConfigSyncPacket(ModConfigServer.INSTANCE.synced));
      }

      handler.getPlayer().getComponent(Components.ARMOR_CONFIG).ensureValid();
    });
  }

  public static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(MODID, path);
  }
}
