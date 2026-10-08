package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** A live aggregate of every rule page exposed by the current source. */
final class AllRulesPage
    implements RulePage
{
    static final String ID = "carpet-gui:all-rules";
    private final List<? extends RulePage> pages;
    private List<RuleView> rules = List.of();
    private final Map<String, String> owners = new LinkedHashMap<>();
    private final Map<String, RulePage> categoryOwners = new LinkedHashMap<>();

    AllRulesPage(List<? extends RulePage> pages) {
        this.pages = List.copyOf(pages);
        refreshIndex();
    }

    void refreshIndex() {
        var aggregate = new ArrayList<RuleView>();
        owners.clear();
        categoryOwners.clear();
        for (RulePage page : pages) {
            for (RuleView rule : page.rules()) {
                aggregate.add(rule);
                owners.putIfAbsent(rule.stateId(), page.id());
                for (String category : rule.categories()) categoryOwners.putIfAbsent(category, page);
            }
        }
        rules = List.copyOf(aggregate);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Component title() {
        return Component.translatable("carpet-gui.tab.all");
    }

    @Override
    public List<? extends RuleView> rules() {
        return rules;
    }

    @Override
    public Component categoryLabel(String category) {
        RulePage owner = categoryOwners.get(category);
        return owner == null ? RulePage.super.categoryLabel(category) : owner.categoryLabel(category);
    }

    String ownerId(RuleView rule) {
        return owners.getOrDefault(rule.stateId(), ID);
    }
}
