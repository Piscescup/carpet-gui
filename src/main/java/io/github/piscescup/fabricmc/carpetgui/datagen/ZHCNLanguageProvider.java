/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
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

package io.github.piscescup.fabricmc.carpetgui.datagen;

//#if MC >= 260000
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
//#else
//$$ import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
//#endif
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author REN YuanTong
 * @since
 */
public class ZHCNLanguageProvider
    extends FabricLanguageProvider
{

    public ZHCNLanguageProvider(
        //#if MC >= 260000
        FabricPackOutput packOutput,
        //#else
        //$$ FabricDataOutput packOutput,
        //#endif
        CompletableFuture<HolderLookup.Provider> registryLookup
    ) {
        super(packOutput, "zh_cn", registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.@NonNull Provider registryLookup, @NonNull TranslationBuilder builder) {
        builder.add("carpet-gui.category.client", "客户端");
        builder.add("carpet-gui.category.command", "命令");
        builder.add("carpet-gui.category.creative", "创造");
        builder.add("carpet-gui.category.dispenser", "发射器");
        builder.add("carpet-gui.category.experimental", "实验性");
        builder.add("carpet-gui.category.feature", "功能");
        builder.add("carpet-gui.category.mobs", "生物");
        builder.add("carpet-gui.category.player", "玩家");
        builder.add("carpet-gui.category.survival", "生存");
        builder.add("carpet-gui.category.world", "世界");
        builder.add("carpet-gui.favorite.add", "添加到收藏");
        builder.add("carpet-gui.favorite.remove", "取消收藏");
        builder.add("carpet-gui.close", "关闭（Esc）");
        builder.add("carpet-gui.collapse_all", "折叠所有");
        builder.add("carpet-gui.collapsed", "已折叠");
        builder.add("carpet-gui.counts", "%s 条规则 / %s 个分类");
        builder.add("carpet-gui.current_value", "当前值：%s");
        builder.add("carpet-gui.default_value", "默认值：%s");
        builder.add("carpet-gui.edit.applied", "已更新客户端规则");
        builder.add("carpet-gui.edit.apply", "应用");
        builder.add("carpet-gui.edit.cancel", "取消");
        builder.add("carpet-gui.edit.default_no_server", "保存默认配置需要连接支持此规则的 Carpet 服务器");
        builder.add("carpet-gui.edit.default_queued", "已发送设为默认请求；是否保存成功以服务器聊天反馈为准");
        builder.add("carpet-gui.edit.default_unsupported", "此规则来源不支持保存默认配置");
        builder.add("carpet-gui.edit.inline_hint", "按 Enter 或移出焦点提交；Esc 取消本次输入");
        builder.add("carpet-gui.edit.invalid_value", "无效的规则值，请检查输入或服务器提示");
        builder.add("carpet-gui.edit.locked", "规则管理器已锁定");
        builder.add("carpet-gui.edit.no_permission", "服务器未授予此规则命令的权限");
        builder.add("carpet-gui.edit.no_server", "未连接支持此规则的 Carpet 服务器；仅客户端规则可修改");
        builder.add("carpet-gui.edit.queued", "已发送修改请求；服务器结果见聊天，数值以同步结果为准");
        builder.add("carpet-gui.edit.session_only", "修改当前值，不写入 Carpet 配置文件");
        builder.add("carpet-gui.edit.value", "规则值");
        builder.add("carpet-gui.empty", "这个标签下没有规则");
        builder.add("carpet-gui.expand_all", "展开所有");
        builder.add("carpet-gui.expanded", "已展开");
        builder.add("carpet-gui.fold_hint", "提示：点击 [+] 展开设置");
        builder.add("carpet-gui.group_label", "%s，%s 条规则，%s");
        builder.add("carpet-gui.live.carpet", "Carpet 规则镜像 · 点击“设为默认”保存世界配置");
        builder.add("carpet-gui.live.join_world", "进入世界后才能读取原版规则");
        builder.add("carpet-gui.live.loading", "正在等待服务器返回原版规则…");
        builder.add("carpet-gui.live.local", "本地客户端规则值；仅客户端规则可修改");
        builder.add("carpet-gui.live.no_managers", "尚未发现已初始化的 Carpet 规则管理器");
        builder.add("carpet-gui.live.packet", "Carpet GUI 数据包通道 · 服务器校验并返回结果 · 设为默认保存世界配置");
        builder.add("carpet-gui.live.vanilla", "当前服务器的原版规则 · 修改由服务器校验");
        builder.add("carpet-gui.live.vanilla_permission", "读取原版规则需要服务器的游戏管理员权限");
        builder.add("carpet-gui.network.applied", "服务器已应用规则修改");
        builder.add("carpet-gui.network.bad_request", "无效的请求或不兼容的协议版本");
        builder.add("carpet-gui.network.busy", "待处理的请求过多，请等待服务器响应");
        builder.add("carpet-gui.network.disconnected", "连接已断开，无法确认此请求的执行结果");
        builder.add("carpet-gui.network.pending", "正在等待服务器处理数据包请求…");
        builder.add("carpet-gui.network.rate_limit", "请求过于频繁，请稍后再试");
        builder.add("carpet-gui.network.save_failed", "规则可能已应用，但无法确认默认配置已保存");
        builder.add("carpet-gui.network.saved", "服务器已应用并保存当前世界的默认配置");
        builder.add("carpet-gui.network.server_error", "服务器处理规则时发生错误，请检查服务端日志");
        builder.add("carpet-gui.network.timeout", "未收到服务器响应，操作可能已执行；请检查当前值，不会自动重发");
        builder.add("carpet-gui.network.unavailable", "服务器不支持 Carpet GUI 数据包协议");
        builder.add("carpet-gui.network.unknown_manager", "服务器上不存在此规则管理器");
        builder.add("carpet-gui.network.unknown_rule", "服务器上不存在此规则");
        builder.add("carpet-gui.next_mod", "下一个 Carpet 模组");
        builder.add("carpet-gui.preview_notice", "示例数据 · 规则值仅供预览 · Esc 关闭");
        builder.add("carpet-gui.previous_mod", "上一个 Carpet 模组");
        builder.add("carpet-gui.reset", "重置");
        builder.add("carpet-gui.screen.title", "Carpet 规则");
        builder.add("carpet-gui.select_source", "选择规则来源");
        builder.add("carpet-gui.set_default", "设为默认");
        builder.add("carpet-gui.set_default_hint", "应用此值并保存到当前世界的 Carpet 配置，重进世界后仍生效；不会改变规则声明的默认值");
        builder.add("carpet-gui.tab.all", "全部规则");
        builder.add("carpet-gui.tab.modified", "已修改规则");
        builder.add("carpet-gui.tab.vanilla", "原版规则");
        builder.add("key.carpet-gui.open_rules", "打开 Carpet 规则界面");
        builder.add("key.category.carpet-gui.main", "Carpet GUI");
        builder.add("carpet-gui.workspace.home", "首页");
        builder.add("carpet-gui.workspace.file", "文件");
        builder.add("carpet-gui.workspace.view", "视图");
        builder.add("carpet-gui.workspace.about", "关于");
        builder.add("carpet-gui.workspace.refresh", "刷新规则");
        builder.add("carpet-gui.workspace.close_tab", "关闭标签页；可从首页重新打开");
        builder.add("carpet-gui.workspace.filters", "筛选");
        builder.add("carpet-gui.workspace.all_categories", "所有分类");
        builder.add("carpet-gui.workspace.uncategorized", "未分类");
        builder.add("carpet-gui.workspace.search", "搜索");
        builder.add("carpet-gui.workspace.search_hint", "搜索规则（中／英文）");
        builder.add("carpet-gui.workspace.sort", "排序");
        builder.add("carpet-gui.workspace.modified_only", "与服务器配置值不同");
        builder.add("carpet-gui.workspace.modified_only_hint", "显示当前值与服务器配置值不同的规则。");
        builder.add("carpet-gui.workspace.modified_only_server_required", "需要服务端安装 Carpet GUI。");
        builder.add("carpet-gui.workspace.initial_difference_only", "与 Mod 初始值不同");
        builder.add("carpet-gui.workspace.initial_difference_hint", "显示当前值与 Mod Java 代码声明初始值不同的规则。");
        builder.add("carpet-gui.workspace.saved_default_only", "已设为服务器默认值");
        builder.add("carpet-gui.workspace.saved_default_only_hint", "显示已明确保存到当前世界 Carpet 配置文件中的规则。");
        builder.add("carpet-gui.workspace.grouping", "分组");
        builder.add("carpet-gui.workspace.grouping.category", "按分类分组");
        builder.add("carpet-gui.workspace.grouping.none", "不分组");
        builder.add("carpet-gui.workspace.sort.name_asc", "名称：A–Z");
        builder.add("carpet-gui.workspace.sort.name_desc", "名称：Z–A");
        builder.add("carpet-gui.workspace.sort.modified_first", "已修改优先");
        builder.add("carpet-gui.workspace.sort.value_asc", "数值：升序");
        builder.add("carpet-gui.workspace.sort.value_desc", "数值：降序");
        builder.add("carpet-gui.workspace.distance", "距离单位");
        builder.add("carpet-gui.workspace.distance.auto", "自动");
        builder.add("carpet-gui.workspace.distance.blocks", "方块");
        builder.add("carpet-gui.workspace.distance.meters", "米");
        builder.add("carpet-gui.workspace.distance.kilometers", "千米");
        builder.add("carpet-gui.workspace.time", "时间单位");
        builder.add("carpet-gui.workspace.time.auto", "自动");
        builder.add("carpet-gui.workspace.time.ticks", "游戏刻");
        builder.add("carpet-gui.workspace.time.seconds", "秒");
        builder.add("carpet-gui.workspace.time.minutes", "分钟");
        builder.add("carpet-gui.workspace.time.hours", "小时");
        builder.add("carpet-gui.workspace.quick_access", "快速访问");
        builder.add("carpet-gui.workspace.features", "功能");
        builder.add("carpet-gui.workspace.favorites", "收藏");
        builder.add("carpet-gui.workspace.rule_groups", "规则分组");
        builder.add("carpet-gui.workspace.rule_groups_home", "收集、版本化并部署一组规则。");
        builder.add("carpet-gui.workspace.new_group", "+ 新建分组");
        builder.add("carpet-gui.workspace.cancel", "取消");
        builder.add("carpet-gui.workspace.group_name", "分组名称");
        builder.add("carpet-gui.workspace.group_tag", "Tag（可选）");
        builder.add("carpet-gui.workspace.create_group", "创建分组");
        builder.add("carpet-gui.workspace.no_groups", "还没有分组。创建一个分组来收集规则。");
        builder.add("carpet-gui.workspace.group_name_required", "请先输入分组名称。");
        builder.add("carpet-gui.workspace.group_created", "已创建规则分组：%s");
        builder.add("carpet-gui.workspace.group_rules", "分组规则");
        builder.add("carpet-gui.workspace.manage_rules", "管理规则");
        builder.add("carpet-gui.workspace.manage_group_rules", "管理分组规则");
        builder.add("carpet-gui.workspace.empty_group", "此分组还没有规则。请先添加规则，然后在这里直接修改。");
        builder.add("carpet-gui.workspace.add_group_rules", "向分组添加规则");
        builder.add("carpet-gui.workspace.add_rule", "添加");
        builder.add("carpet-gui.workspace.done", "完成");
        builder.add("carpet-gui.workspace.color_new", "新添加");
        builder.add("carpet-gui.workspace.color_modified", "已修改");
        builder.add("carpet-gui.workspace.color_staged", "已 add");
        builder.add("carpet-gui.workspace.color_remote", "他人修改");
        builder.add("carpet-gui.workspace.color_committed", "已 commit");
        builder.add("carpet-gui.workspace.remote_change", "%s：%s → %s");
        builder.add("carpet-gui.workspace.status", "status");
        builder.add("carpet-gui.workspace.add", "add");
        builder.add("carpet-gui.workspace.commit", "commit");
        builder.add("carpet-gui.workspace.push", "push");
        builder.add("carpet-gui.workspace.select_or_create_group", "请在左侧选择分组，或创建一个新分组。");
        builder.add("carpet-gui.workspace.no_rules", "当前来源没有可用规则。");
        builder.add("carpet-gui.workspace.no_matching_rules", "没有符合搜索条件的规则。");
        builder.add("carpet-gui.workspace.unstage", "撤销");
        builder.add("carpet-gui.workspace.stage", "暂存");
        builder.add("carpet-gui.workspace.remove", "移除");
        builder.add("carpet-gui.workspace.missing_rule", "当前无法找到此规则的来源");
        builder.add("carpet-gui.workspace.no_group_selected", "尚未选择规则分组。");
        builder.add("carpet-gui.workspace.status_title", "规则状态");
        builder.add("carpet-gui.workspace.back", "返回");
        builder.add("carpet-gui.workspace.unstaged_changes", "未暂存的更改（%s）");
        builder.add("carpet-gui.workspace.staged_changes", "已暂存的更改（%s）");
        builder.add("carpet-gui.workspace.no_unstaged_changes", "没有未暂存的更改。");
        builder.add("carpet-gui.workspace.no_staged_changes", "没有等待 commit 的更改。");
        builder.add("carpet-gui.workspace.restore_head", "恢复到 HEAD");
        builder.add("carpet-gui.workspace.create_commit", "创建提交");
        builder.add("carpet-gui.workspace.commit_message", "提交信息");
        builder.add("carpet-gui.workspace.commit_message_hint", "描述本次规则修改");
        builder.add("carpet-gui.workspace.commit_message_required", "请先输入提交信息。");
        builder.add("carpet-gui.workspace.commit_count", "提交 %s 项暂存更改");
        builder.add("carpet-gui.workspace.nothing_to_commit", "没有可提交的更改。");
        builder.add("carpet-gui.workspace.commit_waiting_push", "已创建提交，正在等待 push。");
        builder.add("carpet-gui.workspace.nothing_to_push", "没有等待 push 的本地提交。");
        builder.add("carpet-gui.workspace.add_result", "add：已暂存 %s 条修改过的规则");
        builder.add("carpet-gui.workspace.push_result", "push：已接受 %s/%s 条规则");
        builder.add("carpet-gui.workspace.restore_result", "restore：已接受 %s/%s 条规则");
        builder.add("carpet-gui.workspace.process", "提交流程");
        builder.add("carpet-gui.workspace.pipeline_summary", "分组 %s 条规则 · 暂存 %s 项 · %s 个提交等待 push");
        builder.add("carpet-gui.workspace.no_commits_graph", "还没有提交。修改分组规则、执行 add，然后创建 commit。");
        builder.add("carpet-gui.workspace.news", "新闻与更新");
        builder.add("carpet-gui.workspace.news_intro", "当前构建的更新信息；不请求外部新闻。");
        builder.add("carpet-gui.workspace.news_workspace", "新增工作台：首页、模组标签页、分类筛选、中英文搜索和排序。");
        builder.add("carpet-gui.workspace.news_addons", "Igny 和 PRY 已有专属页面；规则修改和保存默认仍使用原来的 Carpet 管理器。");
        builder.add("carpet-gui.workspace.overview", "概览");
        builder.add("carpet-gui.workspace.overview_counts", "%s 个 Carpet 模组\n%s 条可用规则");
        builder.add("carpet-gui.workspace.offline", "尚未连接；仅支持的客户端规则可修改。");
        builder.add("carpet-gui.workspace.connected", "已连接；规则值与修改权限以当前服务器为准。");
        builder.add("carpet-gui.workspace.help", "使用说明");
        builder.add("carpet-gui.workspace.help_body", "从快速访问打开模组页面。\n\n筛选或搜索规则，然后在规则行内直接修改值。\n\n设为默认会保存世界配置；重置恢复规则声明的默认值。\n\nEnter 提交文本。Esc 撤销草稿，再按 Esc 返回上一界面。");
        builder.add("carpet-gui.workspace.home_notice", "从快速访问打开模组；Esc 返回上一界面。");
    }
}
