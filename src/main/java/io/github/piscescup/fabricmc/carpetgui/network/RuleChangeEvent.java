package io.github.piscescup.fabricmc.carpetgui.network;

import io.github.piscescup.fabricmc.carpetgui.References;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/** Server-authored event broadcast after a successful GUI rule edit. */
public record RuleChangeEvent(
    int version,
    String managerId,
    String ruleId,
    int operation,
    String actorId,
    String actorName,
    String oldValue,
    String newValue,
    long changedAt
)
    implements CustomPacketPayload
{
    public static final int VERSION = 1;
    public static final Type<RuleChangeEvent> TYPE = new Type<>(References.fromPath("rule_change_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleChangeEvent> CODEC = new StreamCodec<>() {
        @NonNull
        @Override
        public RuleChangeEvent decode(RegistryFriendlyByteBuf buffer) {
            return new RuleChangeEvent(
                buffer.readVarInt(), buffer.readUtf(128), buffer.readUtf(128), buffer.readVarInt(),
                buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(4096), buffer.readUtf(4096),
                buffer.readVarLong()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RuleChangeEvent event) {
            buffer.writeVarInt(event.version());
            buffer.writeUtf(event.managerId(), 128);
            buffer.writeUtf(event.ruleId(), 128);
            buffer.writeVarInt(event.operation());
            buffer.writeUtf(event.actorId(), 64);
            buffer.writeUtf(event.actorName(), 64);
            buffer.writeUtf(event.oldValue(), 4096);
            buffer.writeUtf(event.newValue(), 4096);
            buffer.writeVarLong(event.changedAt());
        }
    };

    public boolean valid() {
        return version == VERSION && managerId.length() <= 128 && ruleId.length() <= 128
               && actorId.length() <= 64 && actorName.length() <= 64
               && oldValue.length() <= 4096 && newValue.length() <= 4096
               && (operation == RuleEditRequest.SET_VALUE || operation == RuleEditRequest.SAVE_DEFAULT);
    }

    public String stateId() {
        return managerId + ":" + ruleId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
