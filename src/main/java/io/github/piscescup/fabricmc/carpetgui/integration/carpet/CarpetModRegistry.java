package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import com.liuyue.igny.IGNYServer;
import com.liuyue.igny.IGNYServerMod;
import com.liuyue.igny.IGNYSettings;
import com.liuyue.igny.rule.RuleContext;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetAddonAdapter;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModTranslationApi;
import me.primaryuan.carpet.CarpetPrimaryuanMod;
import me.primaryuan.carpet.CarpetPrimaryuanServer;
import me.primaryuan.carpet.CarpetPrimaryuanSettings;
import me.primaryuan.carpet.settings.Rule;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.piscescup.fabricmc.carpetgui.References.LOGGER;

/** Separates mod/page identity from rule manager identity and preserves shared-manager ownership. */
public final class CarpetModRegistry {
    private CarpetModRegistry() {}

    private record ModInfo(String carpetModId, String carpetFancyName) implements CarpetModInfoApi {}
    private record RuleKey(String manager, String name) {
        private static RuleKey of(CarpetRule<?> rule) {
            return new RuleKey(rule.settingsManager().identifier(), rule.name());
        }
    }
    private record Providers(Map<String, CarpetModInfoApi> infos, Map<String, CarpetModRulesApi> rules) {}

    /** Can be called by Mod Menu before the game/world initializes rules. */
    public static List<CarpetModInfoApi> discoverInfos() {
        return List.copyOf(providers(CarpetTranslationRegistry.discover()).infos().values());
    }

    private static Providers providers(CarpetTranslationRegistry translations) {
        FabricLoader loader = FabricLoader.getInstance();
        Map<String, CarpetModInfoApi> infos = new LinkedHashMap<>();
        Map<String, CarpetModRulesApi> rules = new LinkedHashMap<>();
        loader.getModContainer("carpet").ifPresent(mod -> infos.put("carpet", new ModInfo("carpet", "Carpet Mod")));
        for (String id : translations.modIds()) {
            loader.getModContainer(id).ifPresent(mod -> infos.putIfAbsent(id, new ModInfo(id, mod.getMetadata().getName())));
        }
        Map<String, CarpetModInfoApi> declared = new LinkedHashMap<>();
        for (CarpetModInfoApi info : loader.getEntrypoints(CarpetModInfoApi.ENTRYPOINT, CarpetModInfoApi.class)) {
            if (declared.putIfAbsent(info.carpetModId(), info) != null) {
                throw new IllegalArgumentException("Duplicate Carpet mod info for " + info.carpetModId());
            }
        }
        for (CarpetExtension extension : List.copyOf(CarpetServer.extensions)) {
            if (extension instanceof CarpetModInfoApi info) declared.putIfAbsent(info.carpetModId(), info);
        }
        for (CarpetModInfoApi info : declared.values()) {
            String id = info.carpetModId();
            if (id == null || !loader.isModLoaded(id) || info.carpetFancyName() == null || info.carpetFancyName().isBlank()) {
                throw new IllegalArgumentException("Invalid Carpet mod info provider: " + id);
            }
            infos.put(id, info);
            translations.registerMod(id);
            if (info instanceof CarpetModRulesApi provider) rules.put(id, provider);
            if (info instanceof CarpetModTranslationApi provider) translations.registerProviderIfAbsent(provider);
        }
        if (loader.isModLoaded("carpet-igny-addition") && !rules.containsKey("carpet-igny-addition")) {
            CarpetModRulesApi igny = CarpetAddonAdapter.fromRules(
                    IGNYServerMod.getModId(), "carpet",
                    () -> IGNYSettings.listRules().stream().map(RuleContext::rule).toList())
                .withFancyName(IGNYServer.fancyName)
                .withTranslations(language -> IGNYServer.getInstance().canHasTranslations(language));
            infos.putIfAbsent(igny.carpetModId(), igny);
            if (!declared.containsKey(igny.carpetModId())) infos.put(igny.carpetModId(), igny);
            rules.put(igny.carpetModId(), igny);
            translations.registerProviderIfAbsent(igny);
        }
        if (loader.isModLoaded("carpet-pry-addition") && !rules.containsKey("carpet-pry-addition")) {
            CarpetModRulesApi pry = CarpetAddonAdapter.fromSettingsClass(
                    CarpetPrimaryuanMod.getModId(), "carpet",
                    CarpetPrimaryuanSettings.class, Rule.class)
                .withTranslations(language -> CarpetPrimaryuanServer.getInstance().canHasTranslations(language));
            infos.putIfAbsent(pry.carpetModId(), pry);
            if (!declared.containsKey(pry.carpetModId())) infos.put(pry.carpetModId(), pry);
            rules.put(pry.carpetModId(), pry);
            translations.registerProviderIfAbsent(pry);
        }
        return new Providers(infos, rules);
    }

    public static List<CarpetModBinding> discover(CarpetTranslationRegistry translations) {
        Providers providers = providers(translations);
        var managers = CarpetManagerBinding.discover(translations);
        Map<RuleKey, CarpetRule<?>> registered = new LinkedHashMap<>();
        Map<RuleKey, String> owners = new LinkedHashMap<>();
        for (CarpetManagerBinding binding : managers) {
            for (CarpetRule<?> rule : binding.manager().getCarpetRules()) {
                registered.putIfAbsent(RuleKey.of(rule), rule);
                owners.putIfAbsent(RuleKey.of(rule), binding.modId());
            }
        }
        Map<RuleKey, String> defaultOwners = new LinkedHashMap<>(owners);
        Map<RuleKey, String> claimed = new LinkedHashMap<>();
        for (CarpetModRulesApi provider : providers.rules().values()) {
            for (CarpetRule<?> rule : provider.getRules()) {
                if (rule == null || rule.settingsManager() == null) continue;
                RuleKey key = RuleKey.of(rule);
                if (registered.get(key) != rule) {
                    LOGGER.warn("Ignoring unregistered rule {}/{} from {}", key.manager(), key.name(), provider.carpetModId());
                    continue;
                }
                String previous = claimed.putIfAbsent(key, provider.carpetModId());
                if (previous != null && !previous.equals(provider.carpetModId())) {
                    // A manager owner's broad list is a fallback; a specific addon owns its shared rules.
                    if (provider.carpetModId().equals(defaultOwners.get(key))) continue;
                    if (!previous.equals(defaultOwners.get(key))) {
                        throw new IllegalArgumentException("Rule " + key + " claimed by " + previous + " and " + provider.carpetModId());
                    }
                    claimed.put(key, provider.carpetModId());
                }
                owners.put(key, provider.carpetModId());
            }
        }
        Map<String, List<CarpetRule<?>>> grouped = new LinkedHashMap<>();
        registered.forEach((key, rule) -> grouped.computeIfAbsent(owners.get(key), ignored -> new ArrayList<>()).add(rule));
        List<CarpetModBinding> pages = new ArrayList<>();
        providers.infos().forEach((id, info) -> pages.add(new CarpetModBinding(info, grouped.getOrDefault(id, List.of()))));
        return List.copyOf(pages);
    }
}
