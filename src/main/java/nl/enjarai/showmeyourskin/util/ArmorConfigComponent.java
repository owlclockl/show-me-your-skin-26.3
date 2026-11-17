package nl.enjarai.showmeyourskin.util;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.enjarai.showmeyourskin.config.ArmorConfig;
import nl.enjarai.showmeyourskin.config.ModConfigServer;
import nl.enjarai.showmeyourskin.net.ConfigSyncPacket;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class ArmorConfigComponent implements ComponentV3, AutoSyncedComponent {
  private ArmorConfig config;

  public ArmorConfigComponent(ArmorConfig config) {
    this.config = config;
  }

  @Override
  public void readData(ValueInput readView) {
    var loaded = readView.read("config", ArmorConfig.CODEC).orElseGet(() -> ArmorConfig.VANILLA_VALUES.copy());
    loaded.ensureValid();
    setConfig(loaded);
  }

  @Override
  public void writeData(ValueOutput writeView) {
    writeView.store("config", ArmorConfig.CODEC, config);
  }

  public ArmorConfig getConfig() {
    return config;
  }

  public void setConfig(ArmorConfig config) {
    this.config = config;
  }

  public void ensureValid() {
    getConfig().ensureValid();
    if (!ModConfigServer.INSTANCE.synced.allowNotShowInCombat()) getConfig().showInCombat = true;
    if (!ModConfigServer.INSTANCE.synced.allowNotShowNameTag()) getConfig().showNameTag = true;
    if (!ModConfigServer.INSTANCE.synced.allowNotForceElytraWhenFlying()) getConfig().forceElytraWhenFlying = true;
  }

  @Override
  public boolean shouldSyncWith(ServerPlayer player) {
    // Only clients with this mod installed listen on this channel
    return ServerPlayNetworking.canSend(player, ConfigSyncPacket.TYPE);
  }

  @Override
  public boolean isRequiredOnClient() {
    return false;
  }
}
