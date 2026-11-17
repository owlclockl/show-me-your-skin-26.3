package nl.enjarai.showmeyourskin.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SyncedModConfig(boolean allowNotShowInCombat, boolean allowNotShowNameTag,
                              boolean allowNotForceElytraWhenFlying) {
  public static final Codec<SyncedModConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    Codec.BOOL.optionalFieldOf("allow_not_show_in_combat", true).forGetter(SyncedModConfig::allowNotShowInCombat),
    Codec.BOOL.optionalFieldOf("allow_not_show_name_tag", true).forGetter(SyncedModConfig::allowNotShowNameTag),
    Codec.BOOL.optionalFieldOf("allow_not_force_elytra_when_flying", true).forGetter(SyncedModConfig::allowNotForceElytraWhenFlying)
  ).apply(instance, SyncedModConfig::new));
  public static final StreamCodec<ByteBuf, SyncedModConfig> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
