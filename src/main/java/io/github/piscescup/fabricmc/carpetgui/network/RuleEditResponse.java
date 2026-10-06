package io.github.piscescup.fabricmc.carpetgui.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * A result belongs to exactly one request; value is the server's resulting value, never an optimistic guess.
 */
public record RuleEditResponse(
    int version,
    long requestId,
    String managerId,
    String ruleId,
    int operation,
    boolean success,
    String value,
    String messageKey
)
    implements CustomPacketPayload
{
    public static final Type<RuleEditResponse> TYPE = new Type<>(Identifier.fromNamespaceAndPath("carpet-gui", "rule_result_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleEditResponse> CODEC = new StreamCodec<>() {
        @Override
        public RuleEditResponse decode(RegistryFriendlyByteBuf buffer) {
            return new RuleEditResponse(
                buffer.readVarInt(), buffer.readVarLong(), buffer.readUtf(128), buffer.readUtf(128),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf(4096), buffer.readUtf(128)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RuleEditResponse response) {
            buffer.writeVarInt(response.version());
            buffer.writeVarLong(response.requestId());
            buffer.writeUtf(response.managerId(), 128);
            buffer.writeUtf(response.ruleId(), 128);
            buffer.writeVarInt(response.operation());
            buffer.writeBoolean(response.success());
            buffer.writeUtf(response.value(), 4096);
            buffer.writeUtf(response.messageKey(), 128);
        }
    };

    public static RuleEditResponse result(RuleEditRequest request, boolean success, String value, String key) {
        return new RuleEditResponse(
            RuleEditRequest.VERSION, request.requestId(), request.managerId(), request.ruleId(),
            request.operation(), success, value.length() <= 4096 ? value : "", key
        );
    }

    public boolean matches(RuleEditRequest request) {
        return version == RuleEditRequest.VERSION && requestId == request.requestId() && managerId.equals(request.managerId())
               && ruleId.equals(request.ruleId()) && operation == request.operation();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
