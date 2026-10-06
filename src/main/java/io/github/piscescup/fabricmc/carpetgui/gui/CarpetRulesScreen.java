package io.github.piscescup.fabricmc.carpetgui.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.carpetgui.integration.carpet.CarpetRuleSource;
import io.github.piscescup.fabricmc.carpetgui.gui.model.EditableRuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.model.PersistentRuleEditor;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleView;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RulePage;
import io.github.piscescup.fabricmc.carpetgui.gui.model.RuleSource;
import io.github.piscescup.fabricmc.carpetgui.gui.model.Expandable;
import io.github.piscescup.fabricmc.carpetgui.gui.model.CollapsibleSection;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiBounds;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.GuiLayout;
import io.github.piscescup.fabricmc.carpetgui.gui.layout.RuleRowLayout;
import io.github.piscescup.fabricmc.carpetgui.gui.render.GuiTheme;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.GuiButton;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.SectionToggleButton;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.DropdownWidget;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.InlineRuleControl;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.RuleTextBox;
import io.github.piscescup.fabricmc.carpetgui.gui.widget.TabBarWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A rule browser with optional backend-driven editing capabilities.
 */
public final class CarpetRulesScreen
    extends AbstractConfigScreen
{
    private static final int TEXT = GuiTheme.TEXT;
    private static final int MUTED = GuiTheme.MUTED;
    private static final int LINE = GuiTheme.LINE;
    private static final int HEADER_HEIGHT = 32;
    private static final int ROW_HEIGHT = 30;
    private static final int GROUP_GAP = 8;
    private final RuleSource source;
    private final List<? extends RulePage> profiles;
    private final Map<String, Boolean> expandedCategories = new LinkedHashMap<>();
    private final List<CategoryGroup> groups = new ArrayList<>();
    private Filter filter = Filter.ALL;
    private int profileIndex;
    private int listTop;
    private int listBottom;
    private int ruleCount;
    private double scroll;
    private double contentHeight;
    private long lastFrameNanos;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;
    private TabBarWidget<RulePage> modTabs;
    private long observedRevision;
    private List<String> displayedModifiedRuleIds = List.of();
    private int refreshTicks;
    private Component feedback = Component.empty();

    public CarpetRulesScreen(Screen parent) {
        this(parent, new CarpetRuleSource());
    }

    public CarpetRulesScreen(Screen parent, RuleSource source) {
        this(parent, source, null);
    }

    public CarpetRulesScreen(Screen parent, RuleSource source, String initialPageId) {
        super(parent, Component.translatable("carpet-gui.screen.title"));
        this.source = Objects.requireNonNull(source);
        this.profiles = List.copyOf(source.pages());
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index)
                .id()
                .equals(initialPageId)) {
                profileIndex = index;
            }
        }
    }

    private RulePage profile() {
        return profiles.get(profileIndex);
    }

    @Override
    protected void init() {
        for (CategoryGroup group : groups)
            for (RuleRow row : group.rows)
                row.control.cancelDraft();

        setFocused(null);
        clearWidgets();
        groups.clear();
        draggingScrollbar = false;
        listTop = 112;
        listBottom = Math.max(listTop + 1, height - 26);

        if (profiles.isEmpty()) return;

        modTabs = addRenderableWidget(new TabBarWidget<>(new GuiBounds(10, 30, Math.max(1, width - 20), 20),
            profiles, profile().id(), this::selectPage));

        int tabX = 10;
        int available = width - 28;
        for (Filter tab : Filter.values()) {
            Component label = Component.translatable(tab.key);
            int tabWidth = Math.min(font.width(label) + 18, available / Filter.values().length);
            GuiButton button = addButton(tabX, 56, tabWidth, 20, label, () -> selectFilter(tab), null);
            button.setSelected(tab == filter);
            tabX += tabWidth + 4;
        }

        int actionWidth = Math.min(96, (width - 28) / 3);
        addButton(
            10, 82, actionWidth, 20,
            Component.translatable("carpet-gui.expand_all"), () -> setAllExpanded(true), null
        );
        addButton(
            14 + actionWidth, 82, actionWidth, 20,
            Component.translatable("carpet-gui.collapse_all"), () -> setAllExpanded(false), null
        );

        buildGroups();
        lastFrameNanos = System.nanoTime();
        layoutGroups();
        observedRevision = source.revision();
        source.refresh();
    }

    private GuiButton addButton(int x, int y, int w, int h, Component label, Runnable action, String tooltip) {
        GuiButton button = new GuiButton(x, y, w, h, label, ignored -> action.run());
        if (tooltip != null) {
            button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        }
        return addRenderableWidget(button);
    }

    private void buildGroups() {
        List<? extends RuleView> rules = profile().rules();
        if (filter == Filter.MODIFIED) {
            rules = rules.stream()
                .filter(RuleView::modified)
                .toList();
        }
        displayedModifiedRuleIds = filter == Filter.MODIFIED
            ? rules.stream().map(RuleView::stateId).toList() : List.of();
        ruleCount = rules.size();
        Map<String, List<RuleView>> grouped = new LinkedHashMap<>();
        for (RuleView rule : rules) {
            for (String category : rule.categories()) {
                grouped.computeIfAbsent(category, ignored -> new ArrayList<>())
                    .add(rule);
            }
        }
        grouped.forEach((category, categoryRules) -> {
            String stateKey = profile().id() + ":" + category;
            CategoryGroup group = new CategoryGroup(categoryRules, stateKey);
            group.header = addWidget(new SectionToggleButton(
                group,
                profile().categoryLabel(category), category, this::listBounds
            ));
            for (RuleView rule : group.rules) {
                group.rows.add(new RuleRow(rule));
            }
            groups.add(group);
        });
    }

    private void selectPage(RulePage selected) {
        for (int index = 0; index < profiles.size(); index++) {
            if (!profiles.get(index)
                .id()
                .equals(selected.id())) {
                continue;
            }
            if (profileIndex != index) {
                profileIndex = index;
                scroll = 0;
                init();
                setFocused(modTabs);
            }
            return;
        }
    }

    private void selectFilter(Filter selected) {
        if (filter != selected) {
            filter = selected;
            scroll = 0;
            init();
        }
    }

    private void setAllExpanded(boolean expanded) {
        Expandable.setAll(groups, expanded);
        layoutGroups();
    }

    private void animateGroups() {
        long now = System.nanoTime();
        double seconds = Math.min((now - lastFrameNanos) / 1_000_000_000.0, 0.1);
        lastFrameNanos = now;
        for (CategoryGroup group : groups) {
            group.section.advance(seconds);
        }
    }

    private void layoutGroups() {
        contentHeight = 0;
        for (CategoryGroup group : groups) {
            contentHeight += HEADER_HEIGHT + group.section.bodyHeight(ROW_HEIGHT) + GROUP_GAP;
        }
        contentHeight = Math.max(0, contentHeight - GROUP_GAP);
        scroll = Math.clamp(scroll, 0, maxScroll());
        double y = listTop - scroll;
        for (CategoryGroup group : groups) {
            group.y = (int) Math.round(y);
            GuiLayout.place(group.header, new GuiBounds(14, group.y, Math.max(1, width - 38), HEADER_HEIGHT));
            group.header.visible = group.y + HEADER_HEIGHT > listTop && group.y < listBottom;
            int rowY = group.y + HEADER_HEIGHT;
            for (RuleRow row : group.rows) {
                RuleRowLayout columns = row.columns(rowY);
                GuiLayout.place(row.value, columns.value());
                GuiLayout.place(row.reset, columns.reset());
                if (row.saveDefault != null) GuiLayout.place(row.saveDefault, columns.saveDefault());
                // Drawing is scissored; native widget hit testing is not. Only fully exposed controls accept input.
                boolean visible = rowY + 22 > listTop && rowY + 2 < listBottom
                                  && rowY + 2 < group.y + HEADER_HEIGHT + group.section.bodyHeight(ROW_HEIGHT);
                row.value.visible = visible;
                row.reset.visible = visible;
                if (row.saveDefault != null) row.saveDefault.visible = visible;
                boolean editable = row.rule instanceof EditableRuleView rule && rule.editor()
                    .editable();
                int clipTop = Math.max(listTop, group.y + HEADER_HEIGHT);
                int clipBottom = Math.min(listBottom, group.y + HEADER_HEIGHT + group.section.bodyHeight(ROW_HEIGHT));
                GuiBounds exposed = new GuiBounds(10, clipTop, Math.max(1, width - 34), Math.max(0, clipBottom - clipTop));
                boolean interactive = visible && exposed.encloses(columns.value()) && exposed.encloses(columns.reset());
                if (!interactive && getFocused() == row.value) setFocused(null);
                row.control.refresh(interactive);
                row.reset.active = interactive && editable && row.rule.modified();
                if (row.saveDefault != null) {
                    var editor = (PersistentRuleEditor) ((EditableRuleView) row.rule).editor();
                    row.saveDefault.active = interactive && exposed.encloses(columns.saveDefault()) && editor.canSaveDefault();
                    row.saveDefault.setTooltip(Tooltip.create(editor.canSaveDefault()
                        ? Component.translatable("carpet-gui.set_default_hint") : editor.defaultDisabledReason()));
                }
                rowY += ROW_HEIGHT;
            }
            y += HEADER_HEIGHT + group.section.bodyHeight(ROW_HEIGHT) + GROUP_GAP;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (profiles.isEmpty()) {
            graphics.text(font, title, 20, 9, TEXT);
            graphics.centeredText(font, Component.translatable("carpet-gui.live.no_managers"), width / 2, height / 2, MUTED);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }
        animateGroups();
        layoutGroups();
        String title = fit(
            profile().title()
                .getString(), width - 36
        );
        graphics.text(font, title, 20, 9, TEXT);
        int hintX = 26 + font.width(title);
        if (width - hintX > 80) {
            graphics.text(
                font,
                fit(
                    Component.translatable("carpet-gui.fold_hint")
                        .getString(), width - hintX - 10
                ),
                hintX,
                9,
                0xFFFF5555
            );
        }
        int countX = 24 + Math.min(96, (width - 28) / 3) * 2;
        String count = Component.translatable("carpet-gui.counts", ruleCount, groups.size())
            .getString();
        if (width - countX > 60) graphics.text(font, fit(count, width - countX - 14), countX, 88, MUTED);

        graphics.enableScissor(10, listTop, width - 14, listBottom);
        if (groups.isEmpty()) {
            graphics.centeredText(
                font, Component.translatable("carpet-gui.empty"),
                width / 2, listTop + Math.max(8, (listBottom - listTop) / 2 - 5), MUTED
            );
        }
        for (CategoryGroup group : groups) {
            renderGroup(graphics, group, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();
        renderScrollbar(graphics);
        graphics.text(
            font,
            fit(
                (feedback.getString()
                    .isEmpty() ? source.notice(profile().isVanilla()) : feedback)
                    .getString(), width - 36
            ),
            18,
            height - 19,
            MUTED
        );
        graphics.horizontalLine(10, width - 10, height - 8, LINE);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var popup = openDropdown();
        if (popup != null) popup.extractOverlay(graphics, mouseX, mouseY);
    }

    private void renderGroup(GuiGraphicsExtractor graphics, CategoryGroup group, int mouseX, int mouseY, float partialTick) {
        int bodyTop = group.y + HEADER_HEIGHT;
        int bodyBottom = bodyTop + group.section.bodyHeight(ROW_HEIGHT);
        if (bodyBottom > listTop && bodyTop < listBottom && bodyBottom > bodyTop) {
            graphics.enableScissor(
                10, Math.max(listTop, bodyTop),
                width - 24, Math.min(listBottom, bodyBottom)
            );
            int rowY = bodyTop;
            for (RuleRow row : group.rows) {
                if (rowY + ROW_HEIGHT > listTop && rowY < listBottom && rowY < bodyBottom) {
                    renderRule(graphics, row, rowY, mouseX, mouseY, Math.min(listBottom, bodyBottom), partialTick);
                }
                rowY += ROW_HEIGHT;
            }
            graphics.disableScissor();
        }
        if (group.header.visible) {
            group.header.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderRule(
        GuiGraphicsExtractor graphics, RuleRow row, int y, int mouseX, int mouseY,
        int clipBottom, float partialTick
    )
    {
        RuleView rule = row.rule;
        RuleRowLayout columns = row.columns(y);
        int left = columns.labelLeft();
        int right = width - 24;
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= Math.max(y, listTop)
                          && mouseY < Math.min(y + ROW_HEIGHT, clipBottom);
        int textWidth = columns.labelWidth();
        graphics.text(
            font,
            fit(
                rule.label()
                    .getString(), textWidth
            ),
            left,
            y + 2,
            TEXT
        );
        String description = rule.description().getString().replaceAll("\\s+", " ").trim();
        GuiTheme.smallText(graphics, font, description, left, y + 15, textWidth);
        row.value.extractRenderState(graphics, mouseX, mouseY, partialTick);
        row.reset.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (row.saveDefault != null) row.saveDefault.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (hovered && openDropdown() == null) {
            List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal(rule.id()), rule.description(),
                Component.translatable("carpet-gui.default_value", rule.defaultValue()),
                Component.translatable("carpet-gui.current_value", rule.value())
            ));
            tooltip.addAll(rule.extraInfo());
            if (row.saveDefault != null && columns.saveDefault().contains(mouseX, mouseY)) {
                var editor = (PersistentRuleEditor) ((EditableRuleView) rule).editor();
                tooltip.add(Component.translatable("carpet-gui.set_default_hint"));
                if (!editor.canSaveDefault()) tooltip.add(editor.defaultDisabledReason());
            }
            if (row.value instanceof RuleTextBox) tooltip.add(Component.translatable("carpet-gui.edit.inline_hint"));
            if (rule instanceof EditableRuleView editable && !editable.editor()
                .editable()) {
                tooltip.add(editable.editor()
                    .disabledReason());
            }
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private GuiBounds listBounds() {
        return new GuiBounds(10, listTop, Math.max(1, width - 24), listBottom - listTop);
    }

    private DropdownWidget<?> openDropdown() {
        for (CategoryGroup group : groups)
            for (RuleRow row : group.rows) {
                var dropdown = row.control.dropdown();
                if (dropdown != null && dropdown.isOpen()) return dropdown;
            }
        return null;
    }

    private boolean editingRow() {
        return groups.stream()
            .flatMap(group -> group.rows.stream())
            .anyMatch(row -> row.control.editing());
    }

    @Override
    public void onClose() {
        for (CategoryGroup group : groups) for (RuleRow row : group.rows) row.control.cancelDraft();
        setFocused(null);
        super.onClose();
    }

    private String fit(String text, int availableWidth) {
        return GuiTheme.fit(font, text, availableWidth);
    }

    private double maxScroll() {
        return Math.max(0, contentHeight - (listBottom - listTop));
    }

    private int thumbHeight() {
        int viewport = listBottom - listTop;
        return Math.clamp((int) (viewport * viewport / Math.max(1, contentHeight)), 18, viewport);
    }

    private int thumbTop() {
        return listTop + (int) Math.round(scroll / Math.max(1, maxScroll()) * (listBottom - listTop - thumbHeight()));
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        if (maxScroll() <= 0) return;
        int x = width - 18;
        graphics.fill(x, listTop, x + 4, listBottom, 0x80000000);
        graphics.fill(x, thumbTop(), x + 4, thumbTop() + thumbHeight(), draggingScrollbar ? TEXT : 0xFF999999);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        var popup = openDropdown();
        if (popup != null) return popup.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        if (modTabs != null && modTabs.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        if (mouseX >= 10 && mouseX < width - 14
            && mouseY >= listTop && mouseY < listBottom) {
            scroll = Math.clamp(scroll - vertical * 28, 0, maxScroll());
            layoutGroups();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        var popup = openDropdown();
        if (popup != null) popup.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        var popup = openDropdown();
        if (popup != null) return popup.mouseClicked(event, doubleClick);
        if (modTabs != null && new GuiBounds(modTabs.getX(), modTabs.getY(), modTabs.getWidth(), modTabs.getHeight())
            .contains(event.x(), event.y())) {
            if (getFocused() instanceof RuleTextBox) setFocused(null);
            boolean handled = modTabs.mouseClicked(event, doubleClick);
            if (handled) setFocused(modTabs);
            return handled;
        }
        // Save an inline draft directly via the persistence backend before blur can submit/reset it.
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (CategoryGroup group : groups)
                for (RuleRow row : group.rows) {
                    if (row.saveDefault != null && row.saveDefault.active && row.saveDefault.visible &&
                        new GuiBounds(row.saveDefault.getX(), row.saveDefault.getY(), row.saveDefault.getWidth(),
                            row.saveDefault.getHeight()).contains(event.x(), event.y())) {
                        row.saveDefault.onPress(null);
                        return true;
                    }
                }
        }
        if (getFocused() instanceof RuleTextBox box &&
            !new GuiBounds(box.getX(), box.getY(), box.getWidth(), box.getHeight()).contains(event.x(), event.y())) {
            // Reset discards the corresponding draft instead of first submitting it on blur.
            for (CategoryGroup group : groups)
                for (RuleRow row : group.rows) {
                    if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && row.reset.active && row.reset.visible &&
                        new GuiBounds(row.reset.getX(), row.reset.getY(), row.reset.getWidth(), row.reset.getHeight()).contains(
                            event.x(),
                            event.y()
                        )) {
                        for (CategoryGroup otherGroup : groups)
                            for (RuleRow other : otherGroup.rows)
                                if (other.rule.stateId()
                                    .equals(row.rule.stateId())) {
                                    other.control.cancelDraft();
                                }
                    }
                }
            setFocused(null);
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && maxScroll() > 0
            && event.x() >= width - 24 && event.x() < width - 14
            && event.y() >= listTop && event.y() < listBottom) {
            draggingScrollbar = true;
            scrollbarGrabOffset = event.y() >= thumbTop() && event.y() < thumbTop() + thumbHeight()
                ? event.y() - thumbTop() : thumbHeight() / 2.0;
            dragScrollbar(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void dragScrollbar(double mouseY) {
        int travel = listBottom - listTop - thumbHeight();
        scroll = travel <= 0 ? 0 : Math.clamp((mouseY - listTop - scrollbarGrabOffset) / travel, 0, 1) * maxScroll();
        layoutGroups();
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (openDropdown() != null) return true;
        if (draggingScrollbar) {
            dragScrollbar(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        var popup = openDropdown();
        if (popup != null && popup.keyPressed(event)) return true;
        if (modTabs != null && modTabs.isFocused() && modTabs.keyPressed(event)) {
            setFocused(modTabs);
            return true;
        }
        if (getFocused() instanceof RuleTextBox box) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                box.cancelDraft();
                setFocused(null);
                return true;
            }
            if (box.keyPressed(event)) return true;
        }
        double change = switch (event.key()) {
            case InputConstants.KEY_PAGEDOWN -> listBottom - listTop;
            case InputConstants.KEY_PAGEUP -> listTop - listBottom;
            case InputConstants.KEY_HOME -> -scroll;
            case InputConstants.KEY_END -> maxScroll() - scroll;
            default -> Double.NaN;
        };
        if (!Double.isNaN(change)) {
            scroll = Math.clamp(scroll + change, 0, maxScroll());
            layoutGroups();
            return true;
        }
        return super.keyPressed(event);
    }

    private enum Filter {
        ALL("carpet-gui.tab.all"),
        MODIFIED("carpet-gui.tab.modified");
        private final String key;

        Filter(String key) {
            this.key = key;
        }
    }

    @Override
    public void tick() {
        if (profiles.isEmpty()) return;
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            source.refresh();
        }
        // Carpet's remote synchronization uses rule.set(null, value), which does not notify
        // rule observers. Check local membership too, without sending additional requests.
        boolean modifiedMembershipChanged = filter == Filter.MODIFIED && !displayedModifiedRuleIds.equals(
            profile().rules().stream().filter(RuleView::modified).map(RuleView::stateId).toList()
        );
        if ((observedRevision != source.revision() || modifiedMembershipChanged)
            && openDropdown() == null && !editingRow() && !draggingScrollbar) {
            observedRevision = source.revision();
            if (filter == Filter.MODIFIED || profile().isVanilla()) init();
        }
    }

    private final class CategoryGroup
        implements Expandable
    {
        private final CollapsibleSection<RuleView> section;
        private final List<RuleView> rules;
        private final List<RuleRow> rows = new ArrayList<>();
        private int y;
        private SectionToggleButton header;

        private CategoryGroup(List<RuleView> rules, String stateKey) {
            this.rules = List.copyOf(rules);
            this.section = new CollapsibleSection<>(
                stateKey, rules, expandedCategories.getOrDefault(stateKey, true),
                expanded -> expandedCategories.put(stateKey, expanded)
            );
        }

        @Override
        public boolean isExpanded() {
            return section.isExpanded();
        }

        @Override
        public void setExpanded(boolean expanded) {
            section.setExpanded(expanded);
            if (header != null) header.refreshLabel();
        }

    }

    private void saveDefault(RuleRow target) {
        RuleRow input = target;
        for (CategoryGroup group : groups)
            for (RuleRow row : group.rows)
                if (row.rule.stateId().equals(target.rule.stateId()) && getFocused() == row.value) input = row;
        if (input.control.saveDefault().accepted()) {
            for (CategoryGroup group : groups)
                for (RuleRow row : group.rows)
                    if (row.rule.stateId().equals(target.rule.stateId())) {
                        row.control.cancelDraft();
                        if (getFocused() == row.value) setFocused(null);
                    }
        }
    }

    private final class RuleRow {
        private final RuleView rule;
        private final InlineRuleControl control;
        private final AbstractWidget value;
        private final Button reset;
        private final Button saveDefault;

        private RuleRow(RuleView rule) {
            this.rule = rule;
            boolean persistent = rule instanceof EditableRuleView editable && editable.editor() instanceof PersistentRuleEditor;
            control = new InlineRuleControl(
                font,
                RuleRowLayout.at(width, 0, persistent)
                    .value(),
                rule,
                CarpetRulesScreen.this::listBounds,
                result -> feedback = rule.label()
                    .copy()
                    .append(": ")
                    .append(result.message())
            );
            value = addWidget(control.widget());
            reset = addWidget(new GuiButton(
                0, 0, 58, 20, Component.translatable("carpet-gui.reset"), ignored -> {
                control.cancelDraft();
                control.submit(rule.defaultValue());
            }
            ));
            reset.active = false;
            saveDefault = persistent ? addWidget(new GuiButton(
                0, 0, 90, 20, Component.translatable("carpet-gui.set_default"), ignored -> saveDefault(this)
            )) : null;
            if (saveDefault != null) saveDefault.active = false;
        }

        private RuleRowLayout columns(int y) {
            return RuleRowLayout.at(width, y, saveDefault != null);
        }

    }

}
