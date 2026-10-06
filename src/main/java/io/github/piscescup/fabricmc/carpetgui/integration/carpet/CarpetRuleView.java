package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.InvalidRuleValueException;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import io.github.piscescup.fabricmc.carpetgui.gui.model.*;
import io.github.piscescup.fabricmc.carpetgui.integration.RuleCommandGateway;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Live adapter; remote edits use structured networking when available, otherwise the server command.
 */
public final class CarpetRuleView
    implements EditableRuleView, PersistentRuleEditor
{
    private final SettingsManager manager;
    private final CarpetRule<?> rule;
    private final RuleCommandGateway gateway;
    private final CarpetClientTranslationResolver translations;
    private final String modId;
    private List<String> searchableText;

    public CarpetRuleView(SettingsManager manager, CarpetRule<?> rule, RuleCommandGateway gateway) {
        this(manager, rule, gateway, null);
    }

    public CarpetRuleView(
        SettingsManager manager, CarpetRule<?> rule, RuleCommandGateway gateway,
        CarpetClientTranslationResolver translations
    ) {
        this(manager.identifier(), manager, rule, gateway, translations);
    }

    public CarpetRuleView(String modId, SettingsManager manager, CarpetRule<?> rule, RuleCommandGateway gateway,
                          CarpetClientTranslationResolver translations) {
        this.modId = modId;
        this.manager = manager;
        this.rule = rule;
        this.gateway = gateway;
        this.translations = translations;
    }

    @Override
    public String id() {
        return rule.name();
    }

    @Override
    public String stateId() {
        return manager.identifier() + ":" + rule.name();
    }

    @Override
    public Component label() {
        return Component.literal(RuleNames.displayName(rule.name()));
    }

    @Override
    public Component description() {
        String key = manager.identifier() + ".rule." + id() + ".desc";
        String apiDescription = RuleHelper.translatedDescription(rule);
        String fallback = apiDescription.equals(key) ? "" : apiDescription;
        return translations == null ? Component.translatableWithFallback(key, fallback)
            : translations.resolve(modId, key, fallback);
    }

    @Override
    public String value() {
        return RuleHelper.toRuleString(rule.value());
    }

    @Override
    public String defaultValue() {
        return RuleHelper.toRuleString(rule.defaultValue());
    }

    @Override
    public List<String> categories() {
        return List.copyOf(rule.categories());
    }

    @Override
    public List<String> searchTerms() {
        if (searchableText == null) {
            List<String> terms = new ArrayList<>(List.of(id(), label().getString(), description().getString(),
                String.join(" ", categories())));
            if (translations != null) terms.addAll(translations.searchTerms(modId, manager.identifier() + ".rule." + id() + "."));
            searchableText = List.copyOf(terms);
        }
        return searchableText;
    }

    @Override
    public List<Component> extraInfo() {
        List<Component> apiInfo = rule.extraInfo();
        List<Component> info = new ArrayList<>();
        for (int index = 0; ; index++) {
            String sourceKey = manager.identifier() + ".rule." + id() + ".extra." + index;
            boolean translated = translations == null ? Language.getInstance()
                .has(sourceKey)
                : translations.has(modId, sourceKey);
            if (index >= apiInfo.size() && !translated) break;
            String fallback = index < apiInfo.size() ? apiInfo.get(index)
                .getString() : "";
            var line = translations == null ? Component.translatableWithFallback(sourceKey, fallback)
                : translations.resolve(modId, sourceKey, fallback)
                    .copy();
            if (index < apiInfo.size()) {
                line.withStyle(apiInfo.get(index)
                    .getStyle());
            }
            info.add(line);
        }
        return List.copyOf(info);
    }

    @Override
    public boolean modified() {
        return !RuleHelper.isInDefaultValue(rule);
    }

    @Override
    public RuleEditor editor() {
        return this;
    }

    @Override
    public InputKind inputKind() {
        if (rule.type() == Boolean.class) return InputKind.BOOLEAN;
        return Number.class.isAssignableFrom(rule.type()) ? InputKind.NUMBER : InputKind.TEXT;
    }

    @Override
    public List<String> suggestions() {
        return List.copyOf(rule.suggestions());
    }

    @Override
    public boolean strict() {
        return rule.strict();
    }

    @Override
    public boolean editable() {
        return !manager.locked() && (gateway.carpetServer() ? gateway.canExecute(manager.identifier())
            : rule.canBeToggledClientSide());
    }

    @Override
    public Component disabledReason() {
        if (manager.locked()) return Component.translatable("carpet-gui.edit.locked");
        return Component.translatable(gateway.carpetServer() ? "carpet-gui.edit.no_permission" : "carpet-gui.edit.no_server");
    }

    @Override
    public RuleEditResult submit(String value) {
        return submit(
            value, ignored -> {
            }
        );
    }

    @Override
    public RuleEditResult submit(String value, Consumer<RuleEditResult> completed) {
        if (!editable()) return RuleEditResult.rejected(disabledReason());
        if (!validInput(value)) {
            return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.invalid_value"));
        }
        if (gateway.carpetServer()) {
            if (value.isEmpty()) return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.invalid_value"));
            return gateway.editCarpetRule(manager.identifier(), id(), value, false, completed);
        }
        try {
            rule.set(null, value); // Carpet explicitly permits offline changes only for client-side rules.
            return RuleEditResult.applied();
        } catch (InvalidRuleValueException exception) {
            return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.invalid_value"));
        }
    }

    @Override
    public boolean canSaveDefault() {
        return gateway.carpetServer() && editable();
    }

    @Override
    public Component defaultDisabledReason() {
        return gateway.carpetServer() ? disabledReason() : Component.translatable("carpet-gui.edit.default_no_server");
    }

    @Override
    public RuleEditResult saveDefault(String value) {
        return saveDefault(
            value, ignored -> {
            }
        );
    }

    @Override
    public RuleEditResult saveDefault(String value, Consumer<RuleEditResult> completed) {
        if (!canSaveDefault()) return RuleEditResult.rejected(defaultDisabledReason());
        if (value.isEmpty() || !validInput(value)) {
            return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.invalid_value"));
        }
        return gateway.editCarpetRule(manager.identifier(), id(), value, true, completed);
    }

    private boolean validInput(String value) {
        return value.length() <= 256 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && value.indexOf('\0') < 0
               && (!strict() || suggestions().contains(value));
    }
}
