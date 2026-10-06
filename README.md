# Carpet GUI

Minecraft **26.3** / Java **25** / Fabric / Carpet **26.3**。GUI 依赖 TCDCommons **5.6.0-beta.2+fabric-26.3**（Modrinth 版本 `FwE7UAgA`）；玩家需安装兼容的 TCDCommons，不需要安装 BetterStats。Mod Menu **21.0.0** 为可选依赖，开发客户端会加载它。

本 Mod 可同时安装在客户端与服务端。客户端提供 GUI，服务端提供可选的规则修改协议，不加载 GUI 或 Mod Menu 类；仅客户端安装时仍支持原来的命令提交方式。

## 使用

进入世界后按 **F9**（可在按键设置中修改），或点击 Mod Menu 中的配置按钮。

- 默认打开 BetterStats 风格的 Home 工作区：左侧 Quick access 使用各 Mod 原始图标打开对应标签，Feature 暂留扩展区域，News 展示随版本维护的本地更新说明；右侧展示 Mod/规则数量、连接状态和使用说明。
- 顶部是 File / View / About 菜单和 Home / Mod 标签栏，名称使用 `CarpetModInfoApi.carpetFancyName()`，页面 ID 使用 `carpetModId()`。标签过多时在标签栏滚轮横向滚动；关闭 Mod 标签后可从 Quick access 重新打开。底部不提供 Close 按钮，Esc 返回父界面。
- Mod 页左侧为分类下拉框（默认 All）、搜索、“仅显示已修改规则”复选框、排序、分组、距离/时间显示偏好，以及展开全部 / 折叠全部。排序/分组/距离/时间使用左侧像素图标与右侧下拉框布局；分组默认为按 `categories()` 分组，也可选择不分组。右侧显示黄色可折叠标题，规则行采用平面样式，悬停为青色描边。
- 搜索匹配规则 ID、拆词标题、分类和描述，同时索引所属 Mod 可用的英文、简体中文、繁体中文翻译，不依赖当前界面语言；缺少的翻译不会机器生成。排序支持名称正/倒序、修改优先、值正/倒序。
- 距离和时间下拉框目前仅保存显示偏好，不推测 Carpet 规则的单位，不换算普通规则，也不改变提交值。筛选、排序、单位和折叠状态保留在当前客户端会话内，不写入磁盘。
- Minecraft 标签是当前服务器返回的原版游戏规则，不属于任意 Carpet 附属，也不是预览数据。
- Carpet 本体、独立管理器的附属和共用管理器的附属都可单独成页。已内置当前 Igny 的适配器：读取 `IGNYSettings.listRules()` 中实际成功注册的规则，将它们从 Carpet 本体页分离，显示为 `Carpet IGNY Addition`；编辑仍使用真实 `/carpet` 管理器和 `carpet.conf`，不会建立假的 Igny 管理器。
- PRY 已内置可选适配：从 `CarpetPrimaryuanSettings` 中带 PRY 自己 `@Rule` 注解的字段获取规则名，查找真实 Carpet 管理器中已经注册的规则，显示在 `Carpet PRY Addition` 页，不再重复出现在 Carpet 本体页。翻译委托 PRY 的 `canHasTranslations(language)`，修改/设为默认仍使用 `/carpet` 和 `carpet.conf`。未安装 PRY 时不加载其专用适配类，也不显示该页。
- Modified Rules 每个客户端 tick 检查本地规则的列表成员变化；收到 Carpet 同步后自动加入非默认规则、移除恢复默认的规则，不额外发送查询包。输入草稿、打开下拉菜单或拖动滚动条期间延后重建，操作结束后刷新；保留分类折叠状态和滚动位置（列表变短时限制到有效范围）。此行为不依赖规则源提供刷新事件，其他 `RuleView` 实现也可复用。
- TCDCommons 提供屏幕树、布局、滚动、焦点、菜单与悬浮提示；`WorkspaceStyle.Dropdown` 与顶部菜单共用 `WorkspaceNavigationMenu` 弹层。所有下拉选项采用深灰背景、灰色像素折角边框、连续无间隔列表行与悬停高亮，长列表支持纵向滚动并限制在屏幕内；`PopupStyle` 集中管理弹层配色和边框，旧界面的下拉框也使用此样式。多分类规则会出现在每个分类，但不因此重复分配到其他 Mod 页。
- 标签栏连续排列，不留 Tab 间隙。文字与输入框统一缩放为原来的 85%，规则描述为 65%；样式尺寸集中在 `WorkspaceStyle`。分类左侧仅显示黄色名称，右侧保留小号 `[+]` / `[-]` 指示，整行仍可点击折叠；数量在分类悬浮提示中显示。
- 背景采用低不透明度的独立遮罩、面板和卡片；颜色集中在 `WorkspaceStyle`。`WorkspaceIcon` 读取 `src/main/resources/assets/carpet-gui/textures/gui/icons/` 中的原创透明 PNG，可直接替换这四个文件修改图标，不再由代码绘制；未复制 BetterStats 资源。`WorkspacePanel` 统一布局/滚动边距，并禁用空白区域拖拽平移，滚轮、滚动条和键盘滚动仍可用。
- 点击分类标题或展开全部 / 折叠全部，它们操作同一份折叠状态，切换标签和管理器时保留。
- 所有规则在当前行直接编辑，不打开额外界面：布尔值点击切换，严格限定选项使用行内下拉框，数值和其他文本直接使用文本框。文本按 Enter 或移出焦点提交，Esc 撤销本次输入；输入过程中不会逐字发送命令。点击 Reset 会丢弃对应输入草稿并提交默认值。
- Reset 将当前值改回规则声明的默认值；它**不是** Carpet 的 `setDefault`，不会写入世界的 Carpet 配置文件。
- Carpet 规则行的 Set Default（设为默认）应用并保存当前值（或文本框草稿）到当前世界的 `<managerId>.conf`，重进世界后仍生效。保存不改变规则声明的默认值，也不改变 Modified Rules 的判断依据。只有连接支持此规则的 Carpet 服务器、具有命令权限且管理器未锁定时可用。数据包通道会回传保存结果；命令通道以聊天反馈为准。原版规则本身随世界保存，不显示此按钮。
- 列表和侧栏独立滚动，小窗口可横向滚动规则行。悬停提示显示拆词标题、`Key: <mod_id>:<rule>`、`Value: <value>`、完整描述和额外说明。
- 支持英文和简体中文。规则文本优先使用客户端资源翻译，再回退到 Carpet 翻译。
- Carpet 规则第一行使用拆分后的英文标题，例如 `creativeNoClip → Creative No Clip`，不使用本地化规则名替换标题；规则 ID 和发送的命令不变。第二行灰色小字显示本地化描述，优先使用当前客户端语言的 `<managerId>.rule.<ruleId>.desc`，缺少时通过 `RuleHelper.translatedDescription(rule)` 读取 Carpet 的描述。描述超长时截断，悬停仍能查看完整描述、规则 ID 和额外信息。

### 翻译来源与数据生成

GUI 自身的按钮/提示翻译分别维护在 `datagen/ENUSLanguageProvider.java` 和 `datagen/ZHCNLanguageProvider.java`，由 `CarpetGUIDataGenerator` 注册。datagen **只生成 GUI 自身文本**，不读取 Carpet / 附属 Mod 的规则翻译，不创建临时 SettingsManager，也不修改 Carpet 的全局语言。已移除规则翻译快照生成器和 `carpet-gui.rule.*` 等复制键。

运行 `gradlew.bat runDatagen` 后再运行 `gradlew.bat assemble`。生成结果位于 `src/main/generated/assets/carpet-gui/lang/en_us.json` 和 `zh_cn.json`，Loom 自动将它们作为资源打包；不要手动修改生成的 JSON。GUI 按钮/提示的翻译保持不变，外部规则翻译不会再写入这些文件。

规则描述、额外说明和分类在运行时按所属 Mod 获取：页面先确定规则归属的 MODID，再调用对应的 `CarpetModTranslationApi.getTranslations(language)`。因此即使 Igny 与 Carpet 共用管理器，Igny 页仍读取 Igny 的翻译，而不是仅由管理器 ID 选择 Carpet 的翻译。每个 Mod 和语言独立缓存，不再把所有 Mod 的文本合并到一个全局表；未实现接口的 Carpet 本体和扩展由自动适配器提供同一接口。适配器读取显式接口、该 Mod 的 `CarpetExtension.canHasTranslations(language)` 和原 Mod 自己的资源，缺失项通过现代 `RuleHelper.translatedDescription(rule)` / `rule.extraInfo()` 回退。

显示优先级为：Minecraft 客户端资源/资源包的原始语言键 → 对应 Mod 接口的客户端当前语言 → 对应 Mod 接口的英文 → Carpet API 回退。标题直接由规则 ID 拆词生成，例如 `creativeNoClip → Creative No Clip`。切换语言会选用相应缓存，重新打开界面会重新发现提供者；运行时不修改 `CarpetSettings.language`、不写语言文件。安装新扩展无需重新运行 datagen。未提供翻译的语言不会自动机器翻译。MODID、管理器命令根 ID、资源命名空间可以三者不同。

### CarpetModInfoApi / CarpetModRulesApi

`CarpetModInfoApi` 仅描述 Mod 信息，提供你指定的两个方法。第一行的按钮使用 Fancy Name，不使用命令根或 MODID 代替显示名称：

```java
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModInfoApi;

public final class ExtraModInfo implements CarpetModInfoApi {
    @Override public String carpetModId() { return "your_mod"; }
    @Override public String carpetFancyName() { return "Your Carpet Addition"; }
}
```

在 Mod 自己的 `fabric.mod.json` 中注册：

```json
{
  "entrypoints": {
    "carpet-gui-mods": ["your.package.ExtraModInfo"]
  }
}
```

本体默认显示 `Carpet Mod`，普通附属未提供接口时回退到 Fabric 元数据名称。仅提供 Info 接口可以覆盖名称，但不能从共享管理器中推断哪些规则属于自己。

也可直接在已注册的 `CarpetExtension` 上实现 Info / Rules 接口，无需重复注册入口；同一 Mod 的独立 `carpet-gui-mods` 入口优先。

需要提供规则归属时，实现可选的 `CarpetModRulesApi`：它继承 Info 和 Translation 接口，另外提供 `Collection<CarpetRule<?>> getRules()`，返回自己已经注册的真实规则。仍通过同一个 `carpet-gui-mods` 入口注册。`getModId()` 默认委托 `carpetModId()`，无需填写两份 MODID；可覆盖 `getTranslations(language)` 委托自己的 Carpet 扩展翻译。

共用管理器时不要用 `getSettingsManagerIds()` 宣称独占 `carpet` 管理器；通过 `getRules()` 声明自己的规则即可。该方法仅用于独立管理器的特殊归属定位。

规则注册和服务端同步仍由原 Mod 负责；接口不创建规则或管理器。只有能在真实已注册管理器中找到的同一个规则实例才会显示。管理器本体返回完整规则集合时，附属对自身规则的声明优先，不会重复展示；两个附属同时声明同一规则时会报错。独立管理器默认按对应 Mod 分页，共享管理器按显式规则归属拆页，未声明归属的规则留在管理器默认 Mod 页中。当前 Igny 和 PRY 已内置适配，不要求改动它们的 jar。

### 已发布附属的通用兼容适配器

Mod 信息自动发现不要求附属实现新接口：遍历 `CarpetServer.extensions`，从每个扩展的运行时类定位其所在 Fabric Mod。类来源优先；无法定位时尝试公开静态 `getModId()` / `carpetModId()` 或 `MOD_ID` / `MODID` / `modId`，结果必须对应实际安装的 Mod。名称尝试公开的 `carpetFancyName()` / `getFancyName()` / `getModName()` 以及 `fancyName` / `FANCY_NAME` / `MOD_NAME` 字段，最后回退到 Fabric 元数据。翻译直接关联该扩展自己的 `canHasTranslations(language)`，无需反射查找单例。共享 Carpet 管理器不会被用于推断附属 MODID。

此流程自动发现所有已注册扩展的 Mod 信息和翻译。独立管理器按现有逻辑分配规则；共享管理器的规则归属仍需实际规则来源，反射得到 MODID 本身不能区分同一管理器内每条规则的注册者。现有 Igny / PRY 专用规则适配保留，原有显式接口仍可覆盖自动发现结果。

`CarpetAddonAdapter` 实现 `CarpetModRulesApi`，让 Carpet GUI 自己提供兼容适配，不要求附属修改或重新发布。Igny 和 PRY 已使用它；专用类仅提供规则集合/配置类和翻译来源，公共类处理管理器查找、名称、未初始化状态、真实注册实例校验和去重。

```java
import io.github.piscescup.fabricmc.carpetgui.api.CarpetAddonAdapter;

// 独立且归该 Mod 所有的管理器：两个 ID 足够，默认名称来自 Fabric 元数据。
var independent = CarpetAddonAdapter.fromIndependentManager("your_mod", "your_manager");

// 共享管理器：必须额外提供归属信息，不能把父管理器全部规则当作附属规则。
var shared = CarpetAddonAdapter.fromRuleNames("your_addon", "carpet",
    () -> List.of("yourRule", "anotherRule"));

// 如果附属提供自身的真实规则集合，优先使用它，避免规则名冲突导致归属误判。
var liveRules = CarpetAddonAdapter.fromRules("your_addon", "carpet", addon::registeredRules);

// 已发布附属只有带注解的配置字段时，传入实际配置类和该附属的规则注解。
var annotated = CarpetAddonAdapter.fromSettingsClass("your_addon", "carpet",
    AddonSettings.class, AddonRule.class);

// 可选覆盖 Fancy Name / 翻译；返回新的适配器，原适配器不变。
var customized = shared.withFancyName("Your Carpet Addition")
    .withTranslations(extension::canHasTranslations);
```

以上是不同接入方式的示例，`addon` / `extension` / 配置类需换成目标附属实际提供的 API。默认翻译仍通过 MODID 读取原 Mod 资源；不会复制到 GUI 的语言文件，也不会修改全局 Carpet 语言。共享管理器适配器不会宣称独占该管理器，`carpetManagerId()` 只用于查找真实命令根。

这些工厂只创建提供者，不会自动注册它。对未实现接口的已发布附属，在 `CarpetModRegistry` 中检测安装状态后，将适配器加入信息、规则和翻译提供者（当前 Igny 和 PRY 就采用此路径）。可选依赖的专用类只能在确认 Mod 已安装后加载。主动接入的附属仍可使用原 `carpet-gui-mods` 入口。

规则在调用 `getRules()` 时才查询，因此创建提供者不会提前触发规则初始化。管理器尚未初始化或目标 Mod 未安装时返回空集合。`fromRules` 排除未成功注册、同名冲突或属于其他管理器的规则实例；所有方式只使用真实规则，不创建规则或假的管理器。`fromSettingsClass` 只检查声明字段的名称与注解，不读取字段值，不调用 `setAccessible`，不访问 Carpet 私有实现。

注意：两个 ID 无法从共享管理器推断归属。规则名/配置字段方案只能查到该名称当前注册的对象，无法证明同名冲突中是哪一个 Mod 注册成功；提供者应确保名称属于目标附属，能取得真实实例时优先用 `fromRules`。分类和翻译键不作为归属依据。适配器按实际版本配置后，规则仍只分配给一个 Mod 标签页；未适配的其他附属不会因此自动独立分页。

Igny 和 PRY 通过 `ReflectiveAddonAdapters` 按完整类名读取公开 API，不导入附属包，也不需要它们的编译依赖或本地 jar。仅在 Fabric 确认附属已安装且没有显式规则提供者时创建适配；接口不兼容时记录警告，规则保留在原管理器页。Igny 仍从 `listRules()` / `rule()` 取得真实注册实例，PRY 按配置字段的原始注解名筛选，翻译通过反射取得服务实例后调用公共 `CarpetExtension.canHasTranslations(language)`。

其他附属也可用字符串重载，避免引用附属类型（确认已安装后调用，并处理反射/链接失败）：

```java
var addon = CarpetAddonAdapter.fromSettingsClass("your_addon", "carpet",
    "your.addon.Settings", "your.addon.settings.Rule");
```

反射适配仍需知道该附属的实际配置类、注解或规则列表 API；不会仅凭分类或同名字段猜测所有共享管理器附属的规则归属。

### CarpetModTranslationApi

公共接口：`io.github.piscescup.fabricmc.carpetgui.api.CarpetModTranslationApi`，不依赖 Mod Menu 或客户端类。

- `getModId()`：唯一必填项，提供自己真实的 Fabric Mod ID。本体和每个附属 Mod 分别注册，不使用父 Mod 的 ID。
- `getSettingsManagerIds()`：可选。入口类无法正确定位归属时，声明自己的命令根 ID；一个 Mod 可拥有多个管理器。已注册的 Carpet 扩展直接实现接口时，其管理器会自动关联。
- `getTranslations(language)`：可选。默认通过 MODID 找到原 Mod，扫描其 `assets/*/lang/<language>.json`，使用 Carpet 的 `Translations.getTranslationFromResourcePath(...)` 读取原始翻译。特殊路径或动态翻译可覆盖此方法，也可直接委托自己扩展的 `canHasTranslations(language)`。返回 Carpet 原始翻译键，不要加 `carpet-gui` 前缀，也不要修改 `CarpetSettings.language`。

最简单的提供者只需实现 Mod ID：

```java
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModTranslationApi;

public final class ExtraTranslations implements CarpetModTranslationApi {
    @Override public String getModId() { return "your_mod"; }
}
```

在该 Mod 自己的 `fabric.mod.json` 注册公共入口（不要放入 `modmenu` 入口）：

```json
{
  "entrypoints": {
    "carpet-gui-translations": ["your.package.ExtraTranslations"]
  }
}
```

也可直接在已经注册到 Carpet 的 `CarpetExtension` 上实现接口；此方式无需再注册上述入口。同一 Mod 同时使用两种方式时，独立入口提供者优先。各 Mod 注册一个提供者；重复独立入口、未加载的 Mod ID 或管理器归属冲突会明确报错。

特殊资源路径可复用 Carpet 的读取 API，并显式提供管理器归属：

```java
@Override public Set<String> getSettingsManagerIds() {
    return Set.of("your_command_root");
}

@Override public Map<String, String> getTranslations(String language) {
    return Translations.getTranslationFromResourcePath(
        "assets/your_resource_namespace/custom_lang/" + language + ".json");
}
```

语言文件中的规则描述键为 `your_command_root.rule.creativeNoClip.desc`，额外说明为 `your_command_root.rule.creativeNoClip.extra.0`，分类为 `your_command_root.category.creative`。旧 `rule.*` / `category.*` 键仍按照 Carpet 的约定映射到 `carpet.*`，附属 Mod 应使用自己的管理器前缀。

使用接口的 Mod 应声明对 `carpet-gui` 的依赖。接口只管理翻译和 Mod 归属，不创建规则、注册 SettingsManager 或改变服务端规则。未实现接口的旧扩展继续自动兼容；Carpet 本体使用相同接口的自动适配器。

按 MODID 获取对应翻译（运行时调用）：

```java
CarpetTranslationRegistry registry = CarpetTranslationRegistry.discover();
registry.findProvider("your_mod").ifPresent(api -> {
    Map<String, String> translations = api.getTranslations("zh_cn");
    String description = translations.get("your_command_root.rule.creativeNoClip.desc");
});
```

GUI 使用 `registry.getTranslations(modId, language)` 读取相同接口并缓存结果，避免每帧重复读资源。这里只在内存中使用文本，不会生成或复制翻译文件。

## Carpet 对接与权限

`CarpetRuleSource → CarpetRulePage → CarpetRuleView` 适配真实的 `carpet.api.settings` API。
`RuleView` 是 GUI 的只读数据契约；`EditableRuleView` 额外提供 `RuleEditor`，其他规则系统可以复用绘制而不依赖 Carpet。

服务器支持 `carpet-gui:rule_edit_v1` 时，优先使用结构化数据包；不支持时，普通修改通过 `/<settingsManager.identifier()> <rule.name()> <value>` 提交，保存默认通过 `/<managerId> setDefault <rule> <value>` 提交。GUI 不会提前篡改客户端镜像。未连接 Carpet 服务器时，仅 `canBeToggledClientSide()` 规则允许通过 API 在本地修改。

### 可选数据包通道

- C2S `RuleEditRequest`：协议版本、请求编号、管理器 ID、规则 ID、操作（修改值 / 保存默认）、值。字符串和长度有界，不接受任意命令或客户端提供的权限/玩家身份。
- S2C `RuleEditResponse`：回传匹配的请求信息、成功状态、服务端实际值和提示键。GUI 将“等待处理”替换成服务器确认或错误提示。失败后的部分生效状态不会被当作保存成功。
- `RuleNetworking` 在公共入口注册协议；Fabric 通道协商决定是否可用。服务端在自身线程上按真实玩家身份检查 Carpet 命令权限、已注册命令根和管理器锁定状态，解析管理器/规则，再调用 Carpet 校验器。
- 普通修改直接调用 `CarpetRule.set(playerSource, value)`。保存默认复用服务端 Carpet 的 `setDefault` 命令；由于 Carpet 的保存方法会吞掉 IO 异常，回传成功前额外核对世界配置文件的对应条目。
- 每连接限制每 20 ticks 最多 20 个请求，并拒绝旧请求编号；客户端最多 32 个待处理请求，10 秒超时。超时表示结果未知，不自动重发或转成命令，以免重复执行；断线清理待处理请求。
- 这是编辑协议，不是全量规则发现协议。规则列表依然由客户端安装的 Carpet 及扩展提供；原版 gamerule 继续使用原来的 Minecraft 请求/命令流程。

原版规则使用 Minecraft 26.3 的 `REQUEST_GAMERULE_VALUES` 请求及响应包读取，需要原版查询权限（通常为管理员）。未收到数据时不把默认值冒充世界当前值。修改使用 `/gamerule`，之后定期重新查询。断开连接时清理缓存。

限制：

- Carpet 和需要展示的扩展必须安装在客户端，且兼容 Minecraft 26.3。仅安装于服务器的扩展不会自动变成客户端 GUI 页面。
- 独立 `SettingsManager` 的扩展自动按所属 Mod 分页；共用管理器的扩展需提供规则归属接口或内置适配器。当前 Igny 和 PRY 已适配，其他未声明归属的共享管理器规则仍留在原管理器页中。
- 管理器 ID 是**命令根 ID**，不一定等于 Fabric mod ID。定位和发送命令使用前者，Mod Menu 归属关联使用后者。
- 自动归属尝试管理器 ID 和扩展入口类所在的 Fabric mod。特殊包装结构可由扩展显式提供以下入口。
- 本版本不提供批量保存、全局新世界默认配置或服务器专用插件规则的协议。

## CarpetModMenuApi

公共接口：`io.github.piscescup.fabricmc.carpetgui.api.CarpetModMenuApi`。

继承 Mod Menu 的 `ModMenuApi`，默认配置工厂打开对应 Mod 规则页，关闭时返回父界面。旧 `getCarpetSettingsManagerId()` 保留为独立管理器的兼容入口；同时实现 `CarpetModInfoApi` 时优先使用其 MODID，因此共享管理器的附属也能正确选中自己的页。

扩展 mod 的用法：

```java
import io.github.piscescup.fabricmc.carpetgui.api.CarpetModMenuApi;

public final class ExtraModMenu implements CarpetModMenuApi {
    @Override
    public String getCarpetSettingsManagerId() {
        // 与自己的 SettingsManager.identifier() 一致，不要直接照抄 Fabric mod ID。
        return "your_manager_identifier";
    }
}
```

在扩展自己的 `fabric.mod.json` 注册：

```json
{
  "entrypoints": {
    "modmenu": ["your.package.ExtraModMenu"]
  }
}
```

扩展编译时需依赖 Carpet GUI 和 Mod Menu API；仅在 Mod Menu 入口类中引用该接口，避免没安装可选 Mod Menu 时从通用初始化代码加载它。若扩展强制使用此接口，也应声明对 `carpet-gui` 的依赖。

不通过 Mod Menu 时：

```java
CarpetGuiScreens.create(parent);             // 默认 Home
CarpetGuiScreens.create(parent, modId);      // 指定 Mod 页，如 carpet-igny-addition
```

工厂应在客户端线程调用；规则尚未初始化时页面为空，不会强行执行生命周期钩子。旧独立管理器 ID 会解析到对应 Mod，找不到指定 ID 时回退到第一个可用页。

Carpet GUI 还通过 `getProvidedConfigScreenFactories()` 给识别的 Carpet / 扩展提供 Mod Menu 入口。扩展自带配置入口优先，不会被覆盖。扩展无需实现本接口也可使用自动入口，显式实现可解决特殊定位问题。

## 可复用 GUI 能力

- `RuleSource / RulePage / RuleView`：规则数据与页面；调用 `CarpetGuiScreens.create(parent, source, initialPageId)` 即可用新工作区接其他规则源，`initialPageId` 为 null 时打开 Home。
- `RuleBrowserModel`：独立于 Carpet 与 TCDCommons 的分类、搜索、排序、折叠和会话偏好模型；后端可覆盖 `RuleView.searchTerms()` 增加其他语言的搜索文本。
- `CarpetWorkspaceScreen / WorkspaceStyle / NativeTextInput`：TCDCommons 工作区、共用平面控件，以及 Minecraft 原生文本框的 Unicode/IME 输入桥接。原 `CarpetRulesScreen` 和旧控件保留兼容，但正式入口已切换到工作区。
- `EditableRuleView / RuleEditor / RuleEditResult`：可选编辑能力、输入类型、权限、提交反馈，区分排队与实际应用。
- `PersistentRuleEditor`：可选的启动默认配置保存能力，界面仅对实现此接口的编辑器提供 Set Default 按钮，不依赖 Carpet。
- `RuleEditor.submit(value, completed)` / `PersistentRuleEditor.saveDefault(value, completed)`：可选异步结果接口。旧编辑器不实现回调也可继续工作。
- `DropdownOption / DropdownState<T> / DropdownWidget<T>`：不依赖 Carpet 的稳定 ID 下拉框，支持键盘、滚动和事件隔离。菜单事件优先路由，`extractOverlay` 最后绘制。
- `Expandable / CollapsibleSection<T> / SectionToggleButton`：折叠状态、动画与文本标题。
- `InlineRuleControl / RuleTextBox`：通用行内编辑控件、焦点提交和实时值同步；不依赖 Carpet。后台更新不会覆盖未提交草稿，打开的行内下拉框独占输入并绘制在列表上方。
- `AbstractConfigScreen / GuiButton / GuiTheme`：背景、父页面导航和灰色按钮。
- `GuiBounds / GuiLayout / RuleRowLayout`：分离位置和大小，禁止裁切中的行控件误触。

`PreviewRules` 仅保留为演示数据源，不再用于正式入口。

## 构建与测试

```text
gradlew.bat build
gradlew.bat runClient
gradlew.bat runClientGameTest
gradlew.bat -PserverGameTests=true build
```

`runClientGameTest` 是显式启动的图形 / 网络测试，不包含在普通构建中。使用 `build/run/clientGameTest` 中新建的隔离世界，验证真实规则发现、Mod Menu 工厂、折叠、行内布尔值 / 下拉框 / 文本框、Enter / 失焦提交、Esc 撤销、草稿保留、数据包修改 / 默认保存 / 拒绝回包 / 权限 / 限流，以及关闭数据包通道后的命令回退和原版规则响应，并保存截图。测试 mod 不打包到正式 jar。

`-PserverGameTests=true build` 额外启动 `build/run/gameTest` 中的独立无界面测试服务端，确认没有客户端类加载错误，并验证规则 API 修改、权限、锁定、校验和配置保存。

Windows 部分 JDK 若报 `Unable to establish loopback connection`，可临时设置（目标路径保持不存在）：

```powershell
$env:JDK_JAVA_OPTIONS = '-Djdk.net.unixdomain.tmpdir=./.gradle/no-unix-sockets'
.\gradlew.bat --no-daemon build
```

## 参考

[Carpet 26.3 API](https://github.com/gnembon/fabric-carpet/tree/v26.3/src/main/java/carpet/api/settings)、[Mod Menu 21.0.0 API](https://github.com/TerraformersMC/ModMenu/tree/v21.0.0/src/main/java/com/terraformersmc/modmenu/api)、[TCDCommons](https://modrinth.com/mod/tcdcommons)、[BetterStats](https://github.com/TheCSDev/betterstats)。

## License

CC0。
