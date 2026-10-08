package io.github.piscescup.fabricmc.carpetgui.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author REN YuanTong
 * @since
 */
public class ENUSLanguageProvider
    extends FabricLanguageProvider
{

    public ENUSLanguageProvider(
        FabricPackOutput packOutput,
        CompletableFuture<HolderLookup.Provider> registryLookup
    ) {
        super(packOutput, "en_us", registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.@NonNull Provider registryLookup, @NonNull TranslationBuilder translationBuilder) {
        translationBuilder.add("carpet-gui.category.client", "Client");
        translationBuilder.add("carpet-gui.category.command", "Commands");
        translationBuilder.add("carpet-gui.category.creative", "Creative");
        translationBuilder.add("carpet-gui.category.dispenser", "Dispensers");
        translationBuilder.add("carpet-gui.category.experimental", "Experimental");
        translationBuilder.add("carpet-gui.category.feature", "Features");
        translationBuilder.add("carpet-gui.category.mobs", "Mobs");
        translationBuilder.add("carpet-gui.category.player", "Players");
        translationBuilder.add("carpet-gui.category.survival", "Survival");
        translationBuilder.add("carpet-gui.category.world", "World");
        translationBuilder.add("carpet-gui.favorite.add", "Add to favorites");
        translationBuilder.add("carpet-gui.favorite.remove", "Remove from favorites");
        translationBuilder.add("carpet-gui.close", "Close (Esc)");
        translationBuilder.add("carpet-gui.collapse_all", "Collapse All");
        translationBuilder.add("carpet-gui.collapsed", "collapsed");
        translationBuilder.add("carpet-gui.counts", "%s rules / %s groups");
        translationBuilder.add("carpet-gui.current_value", "Current: %s");
        translationBuilder.add("carpet-gui.default_value", "Default: %s");
        translationBuilder.add("carpet-gui.edit.applied", "Client-side rule updated");
        translationBuilder.add("carpet-gui.edit.apply", "Apply");
        translationBuilder.add("carpet-gui.edit.cancel", "Cancel");
        translationBuilder.add("carpet-gui.edit.default_no_server", "Saving defaults requires a Carpet server supporting this rule");
        translationBuilder.add("carpet-gui.edit.default_queued", "Default request sent; check server chat for confirmation that it was saved");
        translationBuilder.add("carpet-gui.edit.default_unsupported", "This rule source does not support saving defaults");
        translationBuilder.add("carpet-gui.edit.inline_hint", "Enter or leave the field to submit; Esc cancels your draft");
        translationBuilder.add("carpet-gui.edit.invalid_value", "Invalid rule value; check your input or server feedback");
        translationBuilder.add("carpet-gui.edit.locked", "This settings manager is locked");
        translationBuilder.add("carpet-gui.edit.no_permission", "The server has not granted permission for this command");
        translationBuilder.add("carpet-gui.edit.no_server", "No supporting Carpet server; only client-side rules can be changed");
        translationBuilder.add("carpet-gui.edit.queued", "Request sent; see chat for server feedback. Values update after sync.");
        translationBuilder.add("carpet-gui.edit.session_only", "Change the current value, not the Carpet config file");
        translationBuilder.add("carpet-gui.edit.value", "Rule value");
        translationBuilder.add("carpet-gui.empty", "No rules match this tab");
        translationBuilder.add("carpet-gui.expand_all", "Expand All");
        translationBuilder.add("carpet-gui.expanded", "expanded");
        translationBuilder.add("carpet-gui.fold_hint", "Hint: click [+] to expand settings");
        translationBuilder.add("carpet-gui.group_label", "%s, %s rules, %s");
        translationBuilder.add("carpet-gui.live.carpet", "Live Carpet rules; ordinary edits are temporary, Set Default saves the world configuration");
        translationBuilder.add("carpet-gui.live.join_world", "Join a world to read vanilla rules");
        translationBuilder.add("carpet-gui.live.loading", "Waiting for vanilla rule values from the server...");
        translationBuilder.add("carpet-gui.live.local", "Local client values; only client-side rules are editable");
        translationBuilder.add("carpet-gui.live.no_managers", "No initialized Carpet settings managers were found");
        translationBuilder.add("carpet-gui.live.packet", "Carpet GUI packet channel; server-validated results, Set Default saves the world configuration");
        translationBuilder.add("carpet-gui.live.vanilla", "Current server gamerules; changes are validated by the server");
        translationBuilder.add("carpet-gui.live.vanilla_permission", "Reading vanilla rules requires server gamemaster permission");
        translationBuilder.add("carpet-gui.network.applied", "Server applied the rule change");
        translationBuilder.add("carpet-gui.network.bad_request", "Invalid request or incompatible protocol version");
        translationBuilder.add("carpet-gui.network.busy", "Too many pending requests; wait for the server to respond");
        translationBuilder.add("carpet-gui.network.disconnected", "Disconnected; the request's outcome could not be confirmed");
        translationBuilder.add("carpet-gui.network.pending", "Waiting for the server to process the packet request…");
        translationBuilder.add("carpet-gui.network.rate_limit", "Too many requests; please try again later");
        translationBuilder.add("carpet-gui.network.save_failed", "The rule may be applied, but saving the default could not be confirmed");
        translationBuilder.add("carpet-gui.network.saved", "Server applied and saved the world's startup default");
        translationBuilder.add("carpet-gui.network.server_error", "Server rule processing failed; check the server log");
        translationBuilder.add("carpet-gui.network.timeout", "No server response; the operation may have executed. Check the current value; no automatic retry");
        translationBuilder.add("carpet-gui.network.unavailable", "Server does not support the Carpet GUI packet protocol");
        translationBuilder.add("carpet-gui.network.unknown_manager", "This rule manager does not exist on the server");
        translationBuilder.add("carpet-gui.network.unknown_rule", "This rule does not exist on the server");
        translationBuilder.add("carpet-gui.next_mod", "Next Carpet mod");
        translationBuilder.add("carpet-gui.preview_notice", "Preview data only · Values are read-only · Esc to close");
        translationBuilder.add("carpet-gui.previous_mod", "Previous Carpet mod");
        translationBuilder.add("carpet-gui.reset", "Reset");
        translationBuilder.add("carpet-gui.screen.title", "Carpet Rules");
        translationBuilder.add("carpet-gui.select_source", "Select rule source");
        translationBuilder.add("carpet-gui.set_default", "Set Default");
        translationBuilder.add("carpet-gui.set_default_hint", "Apply this value and save it to this world's Carpet configuration for future loads; the rule's declared default stays unchanged");
        translationBuilder.add("carpet-gui.tab.all", "All Rules");
        translationBuilder.add("carpet-gui.tab.modified", "Modified Rules");
        translationBuilder.add("carpet-gui.tab.vanilla", "Vanilla Rules");
        translationBuilder.add("key.carpet-gui.open_rules", "Open Carpet rules");
        translationBuilder.add("key.category.carpet-gui.main", "Carpet GUI");
        translationBuilder.add("carpet-gui.workspace.home", "Home");
        translationBuilder.add("carpet-gui.workspace.file", "File");
        translationBuilder.add("carpet-gui.workspace.view", "View");
        translationBuilder.add("carpet-gui.workspace.about", "About");
        translationBuilder.add("carpet-gui.workspace.refresh", "Refresh rules");
        translationBuilder.add("carpet-gui.workspace.close_tab", "Close tab; reopen it from Home");
        translationBuilder.add("carpet-gui.workspace.filters", "Filters");
        translationBuilder.add("carpet-gui.workspace.all_categories", "All categories");
        translationBuilder.add("carpet-gui.workspace.uncategorized", "Uncategorized");
        translationBuilder.add("carpet-gui.workspace.search", "Search");
        translationBuilder.add("carpet-gui.workspace.search_hint", "Search rules (Chinese / English)");
        translationBuilder.add("carpet-gui.workspace.sort", "Sort");
        translationBuilder.add("carpet-gui.workspace.modified_only", "Differs from server config");
        translationBuilder.add("carpet-gui.workspace.modified_only_hint", "Show rules whose current value differs from the server configuration.");
        translationBuilder.add("carpet-gui.workspace.modified_only_server_required", "Requires Carpet GUI on the server.");
        translationBuilder.add("carpet-gui.workspace.initial_difference_only", "Differs from mod initial value");
        translationBuilder.add("carpet-gui.workspace.initial_difference_hint", "Show rules whose current value differs from the value declared in mod code.");
        translationBuilder.add("carpet-gui.workspace.saved_default_only", "Saved as server default");
        translationBuilder.add("carpet-gui.workspace.saved_default_only_hint", "Show rules explicitly saved in this world's Carpet configuration.");
        translationBuilder.add("carpet-gui.workspace.grouping", "Grouping");
        translationBuilder.add("carpet-gui.workspace.grouping.category", "Categories");
        translationBuilder.add("carpet-gui.workspace.grouping.none", "No grouping");
        translationBuilder.add("carpet-gui.workspace.sort.name_asc", "A–Z");
        translationBuilder.add("carpet-gui.workspace.sort.name_desc", "Z–A");
        translationBuilder.add("carpet-gui.workspace.sort.modified_first", "Modified first");
        translationBuilder.add("carpet-gui.workspace.sort.value_asc", "Value: ascending");
        translationBuilder.add("carpet-gui.workspace.sort.value_desc", "Value: descending");
        translationBuilder.add("carpet-gui.workspace.distance", "Distance unit");
        translationBuilder.add("carpet-gui.workspace.distance.auto", "Automatic");
        translationBuilder.add("carpet-gui.workspace.distance.blocks", "Blocks");
        translationBuilder.add("carpet-gui.workspace.distance.meters", "Meters");
        translationBuilder.add("carpet-gui.workspace.distance.kilometers", "Kilometers");
        translationBuilder.add("carpet-gui.workspace.time", "Time unit");
        translationBuilder.add("carpet-gui.workspace.time.auto", "Automatic");
        translationBuilder.add("carpet-gui.workspace.time.ticks", "Ticks");
        translationBuilder.add("carpet-gui.workspace.time.seconds", "Seconds");
        translationBuilder.add("carpet-gui.workspace.time.minutes", "Minutes");
        translationBuilder.add("carpet-gui.workspace.time.hours", "Hours");
        translationBuilder.add("carpet-gui.workspace.quick_access", "Quick access");
        translationBuilder.add("carpet-gui.workspace.features", "Features");
        translationBuilder.add("carpet-gui.workspace.favorites", "Favorites");
        translationBuilder.add("carpet-gui.workspace.rule_groups", "Rule Groups");
        translationBuilder.add("carpet-gui.workspace.rule_groups_home", "Collect, version and deploy a set of rules.");
        translationBuilder.add("carpet-gui.workspace.new_group", "+ New group");
        translationBuilder.add("carpet-gui.workspace.cancel", "Cancel");
        translationBuilder.add("carpet-gui.workspace.group_name", "Group name");
        translationBuilder.add("carpet-gui.workspace.group_tag", "Tag (optional)");
        translationBuilder.add("carpet-gui.workspace.create_group", "Create group");
        translationBuilder.add("carpet-gui.workspace.no_groups", "No groups yet. Create one to collect rules.");
        translationBuilder.add("carpet-gui.workspace.group_name_required", "Enter a group name first.");
        translationBuilder.add("carpet-gui.workspace.group_created", "Created rule group: %s");
        translationBuilder.add("carpet-gui.workspace.group_rules", "Grouped Rules");
        translationBuilder.add("carpet-gui.workspace.manage_rules", "Manage rules");
        translationBuilder.add("carpet-gui.workspace.manage_group_rules", "Manage Grouped Rules");
        translationBuilder.add("carpet-gui.workspace.empty_group", "This group is empty. Add rules first, then edit them here.");
        translationBuilder.add("carpet-gui.workspace.add_group_rules", "Add rules to group");
        translationBuilder.add("carpet-gui.workspace.add_rule", "Add");
        translationBuilder.add("carpet-gui.workspace.done", "Done");
        translationBuilder.add("carpet-gui.workspace.color_new", "New");
        translationBuilder.add("carpet-gui.workspace.color_modified", "Modified");
        translationBuilder.add("carpet-gui.workspace.color_staged", "Added");
        translationBuilder.add("carpet-gui.workspace.color_remote", "Changed by others");
        translationBuilder.add("carpet-gui.workspace.color_committed", "Committed");
        translationBuilder.add("carpet-gui.workspace.remote_change", "%s: %s → %s");
        translationBuilder.add("carpet-gui.workspace.status", "status");
        translationBuilder.add("carpet-gui.workspace.add", "add");
        translationBuilder.add("carpet-gui.workspace.commit", "commit");
        translationBuilder.add("carpet-gui.workspace.push", "push");
        translationBuilder.add("carpet-gui.workspace.select_or_create_group", "Select a group on the left, or create a new one.");
        translationBuilder.add("carpet-gui.workspace.no_rules", "No rules are available from the current source.");
        translationBuilder.add("carpet-gui.workspace.unstage", "Undo");
        translationBuilder.add("carpet-gui.workspace.stage", "Stage");
        translationBuilder.add("carpet-gui.workspace.remove", "Remove");
        translationBuilder.add("carpet-gui.workspace.missing_rule", "Rule provider is not currently available");
        translationBuilder.add("carpet-gui.workspace.no_group_selected", "No rule group selected.");
        translationBuilder.add("carpet-gui.workspace.status_title", "Rule Status");
        translationBuilder.add("carpet-gui.workspace.back", "Back");
        translationBuilder.add("carpet-gui.workspace.unstaged_changes", "Unstaged Changes (%s)");
        translationBuilder.add("carpet-gui.workspace.staged_changes", "Staged Changes (%s)");
        translationBuilder.add("carpet-gui.workspace.no_unstaged_changes", "No unstaged changes.");
        translationBuilder.add("carpet-gui.workspace.no_staged_changes", "Nothing is waiting for commit.");
        translationBuilder.add("carpet-gui.workspace.restore_head", "Restore HEAD");
        translationBuilder.add("carpet-gui.workspace.create_commit", "Create Commit");
        translationBuilder.add("carpet-gui.workspace.commit_message", "Commit message");
        translationBuilder.add("carpet-gui.workspace.commit_message_hint", "Describe this rule change");
        translationBuilder.add("carpet-gui.workspace.commit_message_required", "Enter a commit message first.");
        translationBuilder.add("carpet-gui.workspace.commit_count", "Commit %s staged changes");
        translationBuilder.add("carpet-gui.workspace.nothing_to_commit", "Nothing to commit.");
        translationBuilder.add("carpet-gui.workspace.commit_waiting_push", "Commit created and waiting to be pushed.");
        translationBuilder.add("carpet-gui.workspace.nothing_to_push", "No local commits are waiting to be pushed.");
        translationBuilder.add("carpet-gui.workspace.add_result", "add: staged %s modified rules");
        translationBuilder.add("carpet-gui.workspace.push_result", "push: accepted %s of %s rules");
        translationBuilder.add("carpet-gui.workspace.restore_result", "restore: accepted %s of %s rules");
        translationBuilder.add("carpet-gui.workspace.process", "Commit Process");
        translationBuilder.add("carpet-gui.workspace.pipeline_summary", "%s grouped rules · %s staged · %s waiting to push");
        translationBuilder.add("carpet-gui.workspace.no_commits_graph", "No commits yet. Modify a grouped rule, add it, then create a commit.");
        translationBuilder.add("carpet-gui.workspace.news", "News & Updates");
        translationBuilder.add("carpet-gui.workspace.news_intro", "Updates included in this build; no external news requests.");
        translationBuilder.add("carpet-gui.workspace.news_workspace", "New workspace: Home, mod tabs, category filters, bilingual search and sorting.");
        translationBuilder.add("carpet-gui.workspace.news_addons", "Igny and PRY have dedicated pages. Rule edits and saved defaults still use their original Carpet managers.");
        translationBuilder.add("carpet-gui.workspace.overview", "Overview");
        translationBuilder.add("carpet-gui.workspace.overview_counts", "%s Carpet mods\n%s available rules");
        translationBuilder.add("carpet-gui.workspace.offline", "Not connected. Only supported client-side rules can be changed.");
        translationBuilder.add("carpet-gui.workspace.connected", "Connected. Rule values and edit permissions come from the current server.");
        translationBuilder.add("carpet-gui.workspace.help", "Getting started");
        translationBuilder.add("carpet-gui.workspace.help_body", "Open a mod from Quick access.\n\nFilter or search its rules, then edit the value directly in its row.\n\nSet Default saves the world configuration. Reset restores the declared default.\n\nEnter submits text. Esc cancels a draft; press Esc again to leave the screen.");
        translationBuilder.add("carpet-gui.workspace.home_notice", "Use Quick access to open a mod. Esc returns to the previous screen.");
    }
}
