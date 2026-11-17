package nl.enjarai.showmeyourskin.net;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.enjarai.showmeyourskin.Components;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;
import nl.enjarai.showmeyourskin.config.ArmorConfig;

public record SettingsUpdatePacket(ArmorConfig settings) implements CustomPacketPayload {
  public static final Type<SettingsUpdatePacket> TYPE = new Type<>(ShowMeYourSkin.id("settings_update"));
  public static final StreamCodec<ByteBuf, SettingsUpdatePacket> STREAM_CODEC =
    ArmorConfig.STREAM_CODEC.map(SettingsUpdatePacket::new, SettingsUpdatePacket::settings);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void handleServer(ServerPlayNetworking.Context context) {
    var component = Components.ARMOR_CONFIG.get(context.player());
    component.setConfig(settings());
    component.ensureValid();
    Components.ARMOR_CONFIG.sync(context.player());
  }
}
