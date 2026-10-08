package io.github.piscescup.fabricmc.carpetgui.network;

import io.github.piscescup.fabricmc.carpetgui.References;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Server-authored effective configuration values for the current connection. */
public final class ClientRuleConfigurations {
    private static final Map<String, Configuration> VALUES = new LinkedHashMap<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static boolean ready;

    private ClientRuleConfigurations() {}

    public static void initialize() {
        References.LOGGER.info(
            "Initializing client rule configuration sync of {} ver {}",
            References.MOD_NAME, References.MOD_VERSION
        );
        ClientPlayNetworking.registerGlobalReceiver(
            RuleConfigurationSnapshot.TYPE,
            (snapshot, context) -> receive(snapshot)
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static Optional<String> value(String stateId) {
        return Optional.ofNullable(VALUES.get(stateId)).map(Configuration::value);
    }

    public static boolean explicitlyConfigured(String stateId) {
        Configuration configuration = VALUES.get(stateId);
        return configuration != null && configuration.explicitlyConfigured();
    }

    public static boolean ready() {
        return ready;
    }

    public static long revision() {
        return REVISION.get();
    }

    private static void receive(RuleConfigurationSnapshot snapshot) {
        if (!snapshot.valid()) return;
        if (snapshot.replace()) VALUES.clear();
        for (RuleConfigurationSnapshot.Entry entry : snapshot.entries()) {
            VALUES.put(entry.stateId(), new Configuration(entry.value(), entry.explicitlyConfigured()));
        }
        if (snapshot.replace()) ready = true;
        REVISION.incrementAndGet();
    }

    private static void clear() {
        VALUES.clear();
        ready = false;
        REVISION.incrementAndGet();
    }

    private record Configuration(String value, boolean explicitlyConfigured) {}
}
