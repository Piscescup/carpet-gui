# Changelog

Notable changes to Carpet GUI are documented here. Unreleased entries describe the current development build.

## v1.0.0-Fabric

Development changes for version `1.0.0`.

### Added

- In-game rule workspace, accessible with **F9** or through Mod Menu, with Home, All Rules, individual mod pages, and multiple tabs.
- Rule search by ID, display name, and description, including indexed English and Chinese translations.
- Collapsible categories, category and favorites filters, grouping options, and sorting by name or value.
- Persistent favorites for frequently used rules.
- Visual rule editing with toggles, option lists, and text fields, plus reset and **Set Default** actions where supported and permitted.
- Support for discovering rules from Carpet and supported Carpet addons, including reflection-based adapters.
- Vanilla gamerule browsing and editing on supported Minecraft 26.x builds.
- Rule groups with independent membership management and a collapsible folder tree. Group names can use `/` to create nested folders under `origin`.
- Local `status`, `add`, `commit`, and `push` workflow for recording and applying grouped rule values.
- Commit history with messages, authors, timestamps, and rule-value differences.
- JSON preset import and **Export HEAD** for sharing committed rule snapshots between environments.
- Apply preview showing current runtime values and committed target defaults, with per-rule selection, refresh, and reasons for unavailable operations.
- Separate group data, staged changes, and commit history for each singleplayer world and multiplayer server, plus an offline workspace and legacy-group migration.
- Server-confirmed rule edits and persistent-default requests when Carpet GUI is installed on both sides.
- Synchronized saved-configuration state for configuration-aware browsing and filtering when supported by the server.
- Observed player rule-change history in tooltips, showing player names, old and new values, and timestamps, with the newest entries first.
- Verified group application that marks a push complete only after the server confirms every target in the committed snapshot.
- Addon integration APIs for declaring rule ownership, providing mod information and translations, and opening an addon's rule page.

### Changed

- Reorganized the project around a shared source tree and version-specific preprocessing for multiple Minecraft targets.
- Refined the workspace layout, mod icons, group navigation, and commit-history presentation.
- Added horizontal scrolling to group navigation and history panes for content wider than the viewport.
- Added preview validation against the active connection, group revision, runtime values, and saved configuration to reject stale application requests.
- Kept favorites and preset files shared within a game instance while storing group history separately for each environment.

### Fixed

- Fixed clipped **Import preset** and **Export HEAD** button labels. The buttons now stack vertically when the sidebar is too narrow to display both labels side by side, including in the legacy interface.
- Fixed small unintended content shifts when scrolling workspace panels with the mouse wheel.
- Fixed rule-group name and tag display issues.

### Compatibility

- Enabled build targets: Minecraft `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11`, `26.1.2`, `26.2`, and `26.3`.
- The `1.21.1` build also declares compatibility with Minecraft `1.21`.
- Minecraft 1.21.x targets require Java 21 or newer; Minecraft 26.x targets require Java 25 or newer.
- Requires Fabric Loader `0.19.5` or newer, Fabric API, Carpet, and a compatible Fabric build of TCDCommons. Mod Menu is optional.
- Confirmed edits, configuration synchronization, observed player-change history, and verified group pushes require compatible Carpet GUI builds on both client and server. In singleplayer, the mod also runs on the integrated server.

### Documentation

- Expanded the [English README](README.md) with installation instructions, dependency and version tables, workspace screenshots, rule-group workflows, FAQ, addon API examples, and build instructions.
- Added a [Simplified Chinese README](docs/README_ZHCN.md) with language-switch links and matching project acknowledgments.
- Clarified that apply previews compare **current runtime value → committed target default**, while commit details show historical differences. Matching values such as `true → true` can still be submitted for saving as defaults.
- Documented that exports contain committed values, partial or failed pushes remain pending, and batch application does not automatically roll back successful changes.
- Credited Better Statistics Screen as an inspiration for the UI design, alongside the libraries and tools used by the project.
