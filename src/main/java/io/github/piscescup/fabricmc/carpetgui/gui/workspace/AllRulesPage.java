package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import net.minecraft.network.chat.Component;

import java.util.List;

/** A live aggregate of every rule page exposed by the current source. */
final class AllRulesPage
    implements RulePage
{
    static final String ID = "carpet-gui:all-rules";
    private final List<? extends RulePage> pages;

    AllRulesPage(List<? extends RulePage> pages) {
        this.pages = List.copyOf(pages);
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
        return pages.stream()
            .flatMap(page -> page.rules().stream())
            .toList();
    }

    @Override
    public Component categoryLabel(String category) {
        return pages.stream()
            .filter(page -> page.rules().stream().anyMatch(rule -> rule.categories().contains(category)))
            .findFirst()
            .map(page -> page.categoryLabel(category))
            .orElseGet(() -> RulePage.super.categoryLabel(category));
    }

    String ownerId(RuleView rule) {
        return pages.stream()
            .filter(page -> page.rules().stream().anyMatch(candidate -> candidate.stateId().equals(rule.stateId())))
            .map(RulePage::id)
            .findFirst()
            .orElse(ID);
    }
}
