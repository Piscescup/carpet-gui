/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
 *
 * Carpet GUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet GUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet GUI.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.piscescup.fabricmc.carpetgui.api;

import java.util.List;

import io.github.piscescup.fabricmc.carpetgui.gui.pages.RulePage;
import net.minecraft.network.chat.Component;

/**
 * Supplies the pages, vanilla rules, refresh lifecycle, and status notice consumed by the rule
 * browser. Implementations may adapt live Carpet data or provide isolated demo/test data.
 *
 * <pre>{@code
 * source.refresh();
 * List<? extends RulePage> pages = source.pages();
 * long observedRevision = source.revision();
 * }</pre>
 *
 * @see RulePage
 * @see RuleView
 */
public interface RuleSource {
    /**
     * Returns the non-vanilla pages currently available to the browser.
     *
     * <pre>{@code
     * source.pages().forEach(browser::addPage);
     * }</pre>
     *
     * @return the ordered rule pages; may be empty
     */
    List<? extends RulePage> pages();

    /**
     * Returns the vanilla game rules exposed alongside Carpet rule pages.
     *
     * <pre>{@code
     * List<? extends RuleView> gameRules = source.vanillaRules();
     * }</pre>
     *
     * @return the ordered vanilla rule views; may be empty
     */
    List<? extends RuleView> vanillaRules();

    /**
     * Requests that the source refresh any cached data. The default implementation performs no
     * work.
     *
     * <pre>{@code
     * source.refresh();
     * if (source.revision() != previousRevision) rebuildRows();
     * }</pre>
     *
     * @throws RuntimeException if an implementation cannot refresh its backend data
     */
    default void refresh() {
    }

    /**
     * Returns a monotonically changing token for the currently exposed data. Callers may compare
     * it with a previously observed value instead of rebuilding the browser unconditionally.
     *
     * <pre>{@code
     * if (source.revision() != renderedRevision) rebuildRows();
     * }</pre>
     *
     * @return the current revision token; {@code 0} when the source does not track revisions
     */
    default long revision() {
        return 0;
    }

    /**
     * Returns a notice associated with the selected rule collection.
     *
     * <pre>{@code
     * Component message = source.notice(showingVanillaRules);
     * }</pre>
     *
     * @param vanilla {@code true} when the caller is showing {@link #vanillaRules()}, otherwise
     *                {@code false} for addon pages
     * @return the notice to display, or an empty component when no notice is required
     */
    default Component notice(boolean vanilla) {
        return Component.empty();
    }
}
