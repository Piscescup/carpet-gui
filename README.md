# Carpet GUI

A graphical workspace for browsing, editing, and organizing Carpet rules and rules from supported Carpet addons. Find the settings you need, collect them into reusable groups, and preview changes before applying them.

## Getting started

Install the build for your Minecraft version together with **Fabric API**, **Carpet**, and **TCDCommons**. **Mod Menu** is optional.

Press **F9** in-game to open the workspace, or open it through Mod Menu. The key binding can be changed in Minecraft's Controls menu. Install the relevant Carpet addons on the client to make their rule definitions available.

## Client-side features

These features do not require Carpet GUI on the server. Editing server rules still requires Carpet, the relevant addons, and sufficient permissions on that server.

- **Rule browser:** browse individual mod pages or All Rules, with collapsible categories and multiple workspace tabs.
- **Search and filters:** find rules by ID, display name, or description; filter by category or favorites, change the grouping, and sort by name or value. Search also recognizes indexed English and Chinese translations.
- **Convenient editing:** use toggle buttons, option lists, and value fields; reset rules to their declared defaults or request persistent defaults with Set Default where permitted.
- **Favorites:** keep frequently used rules easy to find across sessions.
- **Rule groups:** organize related rules into a folder tree. Use `/` in group names, such as `survival/mob-farms`; groups without a folder prefix appear directly under the `origin` root. The navigation pane supports horizontal scrolling for long names.
- **Local change tracking:** stage changes, create commits with messages, and click a commit to inspect its author, timestamp, and changed rules.
- **Preset import and export:** share a group's committed values as a JSON preset, or import a preset into a local group. Exports contain the latest committed snapshot, not uncommitted changes, server addresses, or commit history.
- **Apply preview:** compare current values with a preset or group before applying it, select the changes you want, and identify missing rules or changes that cannot be applied.
- **Separate workspaces:** keep groups, staged changes, and commit history separate for each singleplayer save and multiplayer server. Favorites and preset files remain shared within the game instance.
- **Vanilla gamerules on 26.x:** browse and edit vanilla gamerules on supported Minecraft 26.x builds, subject to server permissions.

Rule groups and commits are local workspace data; they are not automatically shared with other players. Use exported presets to share configurations.

## Additional features with Carpet GUI on both client and server

Install matching versions of Carpet GUI on the client and server to enable:

- **Confirmed rule edits:** receive server-confirmed success or rejection for edits and persistent-default changes.
- **Rule change history:** hover over a rule to see observed player changes, including the player's name, `old -> new` values, and a timestamp formatted as `[YYYY/MM/DD hh:mm:ss]`. Newer changes appear above older ones, and timestamps stay on one line.
- **Configuration-aware browsing:** see synchronized configuration state and filter rules by changed values or explicitly saved defaults.
- **Verified group application:** push committed group values after reviewing the differences. A push is marked complete only after the server confirms all requested changes; failed or incomplete pushes remain pending.

Change history covers observed player changes during the current connection; it is not a permanent server audit log. Batch application is not atomic: some changes may succeed before another fails, and successful changes are not automatically rolled back.

Carpet GUI respects Carpet permissions, validation, and locked rules. Installing it does not grant additional access. In singleplayer, the installed mod also runs on the integrated server.

## Typical preset workflow

1. Create a rule group and choose its rules and target values.
2. Stage and commit the changes, then export the committed snapshot as a JSON preset.
3. Import the preset in another workspace and review the current-to-target differences.
4. Select the applicable changes and confirm. Verified group pushes require Carpet GUI on both sides.

## Data locations

- Singleplayer groups: `<save-root>/carpet-gui/rule-groups.json`
- Multiplayer groups: `<game-instance-root>/carpet-gui/Multiplayergames_<address>_<hash>/rule-groups.json`
- Shared presets: `<game-instance-root>/config/carpet-gui/presets/`
- Shared favorites: `<game-instance-root>/config/carpet-gui/favorites.txt`

For multiplayer workspaces, `carpet-gui` sits beside `config` in the game instance directory.

## For addon developers: Rules API

Use [`CarpetModRulesApi`](src/main/java/io/github/piscescup/fabricmc/carpetgui/api/CarpetModRulesApi.java) to give your addon its own identity and associate its registered Carpet rules with its page. This is especially useful when your addon shares another mod's settings manager, such as `/carpet`.

Compile against the Carpet GUI build for your target Minecraft version. Your rules must still be registered with Carpet normally; this API exposes existing rules to the GUI rather than registering new ones.

### 1. Provide your mod information and rules

The following example is for an addon whose rules are registered in Carpet's shared settings manager. Replace the mod ID, display name, and rule names with your own.

```java
package com.example.addon.compat;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModRulesApi;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ExampleRulesProvider implements CarpetModRulesApi {
    private static final Set<CarpetRule<?>> RULES = ...;

    @Override
    public String carpetModId() {
        return "example_addon";
    }

    @Override
    public String carpetFancyName() {
        return "Example Addon";
    }

    @Override
    public Collection<CarpetRule<?>> asCarpetRules() {
        return RULES;
    }
}
```

- `carpetModId()` must return your actual, installed Fabric mod ID, not the settings manager's command name.
- `carpetFancyName()` supplies the human-readable page name.
- `asCarpetRules()` must return the **live, registered `CarpetRule<?>` instances** owned by your addon. Do not return rule-name strings, copied rules, or another addon's rules.
- Return an empty collection when your manager is not ready; never return `null`. The provider may be queried before a world has loaded.


### 2. Register the provider

Merge this fragment into your addon's `fabric.mod.json`, preserving its existing entrypoints and dependencies:

```json
{
  "entrypoints": {
    "carpet-gui-mods": [
      "com.example.addon.compat.ExampleRulesProvider"
    ]
  },
  "depends": {
    "carpet-gui": "*"
  }
}
```

This example makes Carpet GUI a required dependency. If you want optional integration, ensure the provider is not loaded when Carpet GUI is absent.

Alternatively, an already registered `CarpetExtension` can implement `CarpetModRulesApi` directly. It does not also need a `carpet-gui-mods` entrypoint. Register only one provider for each mod ID.

### Dedicated settings managers

If you have your own settings manager, return its registered rules instead of reading `CarpetServer.settingsManager`.

If your addon owns an entire manager, you can implement [`CarpetModInfoApi`](src/main/java/io/github/piscescup/fabricmc/carpetgui/api/CarpetModInfoApi.java) and override `getSettingsManagerIds()`:

```java
@Override
public Set<String> getSettingsManagerIds() {
    return Set.of("exampleManager");
}
```

The value must match `SettingsManager.identifier()`. Do not claim a whole shared manager: leave this method at its empty default and use `asCarpetRules()` to identify only your own rules.

### Translations

By default, translations are read from your mod's `assets/<namespace>/lang/<language>.json` resources. Use Carpet's original keys, with the actual settings manager identifier as the prefix. For rules in `/carpet`, for example:

```json
{
  "carpet.rule.exampleRule.desc": "Controls the example behavior.",
  "carpet.rule.exampleRule.extra.0": "Additional details about this rule.",
  "carpet.category.example": "Example"
}
```

Override `getTranslations(String language)` if your addon uses a different resource layout or already supplies translations through `CarpetExtension.canHasTranslations`. Return a map of Carpet translation keys to text without changing Carpet's global language state.

### Opening your addon's page

From client-only code, use [`CarpetGuiScreens`](src/main/java/io/github/piscescup/fabricmc/carpetgui/modmenu/CarpetGuiScreens.java) to create a screen focused on your addon, for example from a Mod Menu config screen factory:

```java
return parent -> CarpetGuiScreens.create(parent, "example_addon");
```

Import `io.github.piscescup.fabricmc.carpetgui.modmenu.CarpetGuiScreens` and pass your Fabric mod ID.
