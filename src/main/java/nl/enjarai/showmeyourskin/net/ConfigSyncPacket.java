package nl.enjarai.showmeyourskin.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;
import nl.enjarai.showmeyourskin.config.SyncedModConfig;

public record ConfigSyncPacket(SyncedModConfig config) implements CustomPacketPayload {
  public static final Type<ConfigSyncPacket> TYPE = new Type<>(ShowMeYourSkin.id("config_sync"));
  public static final StreamCodec<ByteBuf, ConfigSyncPacket> STREAM_CODEC =
    SyncedModConfig.STREAM_CODEC.map(ConfigSyncPacket::new, ConfigSyncPacket::config);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
