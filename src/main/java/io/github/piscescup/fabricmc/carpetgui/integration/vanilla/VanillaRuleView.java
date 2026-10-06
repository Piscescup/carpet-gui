package io.github.piscescup.fabricmc.carpetgui.integration.vanilla;

import io.github.piscescup.fabricmc.carpetgui.gui.model.*;
import io.github.piscescup.fabricmc.carpetgui.integration.RuleCommandGateway;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;

import java.util.List;

public final class VanillaRuleView
    implements EditableRuleView, RuleEditor
{
    private final ResourceKey<GameRule<?>> key;
    private final GameRule<?> rule;
    private final RuleCommandGateway gateway;

    public VanillaRuleView(ResourceKey<GameRule<?>> key, GameRule<?> rule, RuleCommandGateway gateway) {
        this.key = key;
        this.rule = rule;
        this.gateway = gateway;
    }

    @Override
    public String id() {
        return rule.getIdentifierWithFallback()
            .toString();
    }

    @Override
    public Component label() {
        return Component.translatableWithFallback(rule.getDescriptionId(), id());
    }

    @Override
    public Component description() {
        return Component.translatableWithFallback(rule.getDescriptionId() + ".description", "");
    }

    @Override
    public String value() {
        return VanillaRuleStore.values()
            .getOrDefault(key, "?");
    }

    @Override
    public String defaultValue() {
        return defaultString(rule);
    }

    private static <T> String defaultString(GameRule<T> rule) {
        return rule.serialize(rule.defaultValue());
    }

    @Override
    public List<String> categories() {
        return List.of(rule.category()
            .id()
            .getPath());
    }

    @Override
    public RuleEditor editor() {
        return this;
    }

    @Override
    public InputKind inputKind() {
        return rule.gameRuleType() == GameRuleType.BOOL ? InputKind.BOOLEAN : InputKind.NUMBER;
    }

    @Override
    public List<String> suggestions() {
        return inputKind() == InputKind.BOOLEAN ? List.of("true", "false") : List.of();
    }

    @Override
    public boolean strict() {
        return inputKind() == InputKind.BOOLEAN;
    }

    @Override
    public boolean editable() {
        return VanillaRuleStore.values()
                   .containsKey(key) && gateway.canExecute("gamerule");
    }

    @Override
    public Component disabledReason() {
        return Component.translatable("carpet-gui.edit.no_permission");
    }

    @Override
    public RuleEditResult submit(String value) {
        if (!editable()) return RuleEditResult.rejected(disabledReason());
        if (value.length() > 256 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\0') >= 0
            || (strict() && !suggestions().contains(value)) || rule.deserialize(value)
                .result()
                .isEmpty()) {
            return RuleEditResult.rejected(Component.translatable("carpet-gui.edit.invalid_value"));
        }
        return gateway.send("gamerule " + id() + " " + value);
    }
}
