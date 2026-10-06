package io.github.piscescup.fabricmc.carpetgui.gui.workspace;

import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Filtering/sorting/session preferences, independent of Carpet and the GUI framework.
 */
public final class RuleBrowserModel {
    public static final String UNCATEGORIZED = "__uncategorized";
    public static final String ALL_RULES = "__all_rules";
    private static final Map<String, RuleBrowserModel> SESSION = new LinkedHashMap<>();

    public enum Sort {
        NAME_ASC,
        NAME_DESC,
        MODIFIED_FIRST,
        VALUE_ASC,
        VALUE_DESC
    }

    public enum Distance {
        AUTO,
        BLOCKS,
        METERS,
        KILOMETERS
    }

    public enum Grouping { CATEGORY, NONE }

    public enum Time {
        AUTO,
        TICKS,
        SECONDS,
        MINUTES,
        HOURS
    }

    public record Group(
        String category,
        List<RuleView> rules
    ) {
    }

    public String query = "";
    public String category = "";
    public boolean modifiedOnly;
    public Sort sort = Sort.NAME_ASC;
    public Grouping grouping = Grouping.CATEGORY;
    public Distance distance = Distance.AUTO;
    public Time time = Time.AUTO;
    private final Map<String, Boolean> expanded = new LinkedHashMap<>();
    private final Map<String, String> searchIndex = new LinkedHashMap<>();

    public static RuleBrowserModel forPage(String id) {
        return SESSION.computeIfAbsent(id, ignored -> new RuleBrowserModel());
    }

    public boolean expanded(String category) {
        return expanded.getOrDefault(category, true);
    }

    public void setExpanded(String category, boolean value) {
        expanded.put(category, value);
    }

    public void expandAll(RulePage page, boolean value) {
        categories(page).forEach(category -> setExpanded(category, value));
        setExpanded(ALL_RULES, value);
    }

    public List<String> categories(RulePage page) {
        return page.rules()
            .stream()
            .flatMap(rule -> rule.categories()
                .isEmpty()
                ? List.of(UNCATEGORIZED)
                .stream()
                : rule.categories()
                    .stream())
            .distinct()
            .sorted()
            .toList();
    }

    public List<RuleView> rules(RulePage page) {
        List<String> tokens = List.of(query.strip()
            .split("\\s+"));
        return page.rules()
            .stream()
            .filter(rule -> !modifiedOnly || rule.modified())
            .filter(rule -> category.isEmpty() || rule.categories()
                .contains(category)
                            || category.equals(UNCATEGORIZED) && rule.categories()
                .isEmpty())
            .filter(rule -> matches(rule, tokens))
            .sorted(comparator())
            .map(rule -> (RuleView) rule)
            .toList();
    }

    public List<Group> groups(RulePage page) {
        if (grouping == Grouping.NONE) {
            List<RuleView> rules = rules(page);
            return rules.isEmpty() ? List.of() : List.of(new Group(ALL_RULES, rules));
        }
        Map<String, List<RuleView>> groups = new LinkedHashMap<>();
        for (RuleView rule : rules(page)) {
            List<String> categories = rule.categories()
                .isEmpty() ? List.of(UNCATEGORIZED) : rule.categories();
            for (String group : categories) {
                if (category.isEmpty() || category.equals(group)) {
                    groups.computeIfAbsent(group, ignored -> new ArrayList<>())
                        .add(rule);
                }
            }
        }
        return groups.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new Group(entry.getKey(), List.copyOf(entry.getValue())))
            .toList();
    }

    /**
     * Values and modified flags are included so live synchronization also updates value-based sort order.
     */
    public List<String> signature(RulePage page) {
        return rules(page).stream()
            .map(rule -> rule.stateId() + "\0" + rule.value() + "\0" + rule.modified())
            .toList();
    }

    private boolean matches(RuleView rule, List<String> tokens) {
        String text = searchIndex.computeIfAbsent(rule.stateId(), ignored -> normalize(String.join(" ", rule.searchTerms())));
        return tokens.stream()
            .allMatch(token -> text.contains(normalize(token)));
    }

    public void invalidateSearch() {
        searchIndex.clear();
    }

    private Comparator<RuleView> comparator() {
        Comparator<RuleView> names = Comparator.comparing(rule -> rule.label()
            .getString()
            .toLowerCase(Locale.ROOT));
        return switch (sort) {
            case NAME_ASC -> names.thenComparing(RuleView::stateId);
            case NAME_DESC -> names.reversed()
                .thenComparing(RuleView::stateId);
            case MODIFIED_FIRST -> Comparator.comparing(RuleView::modified)
                .reversed()
                .thenComparing(names);
            case VALUE_ASC -> ((Comparator<RuleView>) (left, right) -> compareValues(left.value(), right.value())).thenComparing(names);
            case VALUE_DESC -> ((Comparator<RuleView>) (left, right) -> compareValues(right.value(), left.value())).thenComparing(names);
        };
    }

    private static int compareValues(String left, String right) {
        BigDecimal leftNumber = numericValue(left), rightNumber = numericValue(right);
        if (leftNumber != null && rightNumber != null) return leftNumber.compareTo(rightNumber);
        if (leftNumber != null) return -1;
        if (rightNumber != null) return 1;
        return left.compareToIgnoreCase(right);
    }

    private static BigDecimal numericValue(String value) {
        try { return new BigDecimal(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[\\s_\\-]+", "");
    }
}
