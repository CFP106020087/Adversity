package com.adversity.client.gui;

import com.adversity.Adversity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 综合指南GUI - 重制版
 * 采用侧边栏布局以适应不同语言长度
 * 使用剪裁测试(Scissor Test)解决滚动溢出问题
 */
@SideOnly(Side.CLIENT)
public class GuiAdversityGuide extends GuiScreen {

    // GUI尺寸
    private static final int GUI_WIDTH = 340; // 加宽以容纳侧边栏
    private static final int GUI_HEIGHT = 230;

    // 区域定义
    private static final int SIDEBAR_WIDTH = 90;
    private static final int CONTENT_PADDING = 10;

    // 内容区域 (相对于GUI左上角)
    private int contentAreaX;
    private int contentAreaY;
    private int contentAreaWidth;
    private int contentAreaHeight;

    // 当前页签
    private int currentTab = 0;
    private static final String[] TAB_NAMES = { "overview", "affixes", "sanctuary", "baubles", "enchants", "rituals",
            "progression", "tips", "crt" };

    // 滚动
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private List<String> contentLines = new ArrayList<>();

    // GUI位置
    private int guiLeft;
    private int guiTop;

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        // 计算内容区域
        contentAreaX = SIDEBAR_WIDTH + CONTENT_PADDING;
        contentAreaY = 30; // 标题下方
        contentAreaWidth = GUI_WIDTH - SIDEBAR_WIDTH - (CONTENT_PADDING * 2) - 6; // -6 for scrollbar
        contentAreaHeight = GUI_HEIGHT - 40;

        buttonList.clear();

        // 关闭按钮 (右上角)
        buttonList.add(new GuiButton(0, guiLeft + GUI_WIDTH - 20, guiTop + 5, 15, 15, "X"));

        // 页签按钮 (左侧垂直列表)
        int tabY = guiTop + 35;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            String tabName = I18n.format("guide.adversity.tab." + TAB_NAMES[i]);
            buttonList.add(new GuiTabButton(10 + i, guiLeft + 5, tabY, SIDEBAR_WIDTH - 10, 20, tabName, i));
            tabY += 22; // 按钮间距
        }

        refreshContent();
    }

    private void refreshContent() {
        contentLines.clear();
        // 保持滚动位置还是重置？通常切换标签重置
        if (contentLines.isEmpty()) {
            scrollOffset = 0;
        }

        // 实际上需要先重新生成内容再计算
        scrollOffset = 0;

        switch (currentTab) {
            case 0:
                addOverviewContent();
                break;
            case 1:
                addAffixContent();
                break;
            case 2:
                addSanctuaryContent();
                break;
            case 3:
                addBaublesContent();
                break;
            case 4:
                addEnchantmentsContent();
                break;
            case 5:
                addRitualsContent();
                break;
            case 6:
                addProgressionContent();
                break;
            case 7:
                addTipsContent();
                break;
            case 8:
                addCRTContent();
                break;
        }

        // 计算最大滚动
        int totalHeight = contentLines.size() * 12; // 增加行高间距
        maxScroll = Math.max(0, totalHeight - contentAreaHeight);
    }

    // === 内容生成 ===
    // (保留原有的内容生成逻辑，但适配新的宽度)

    private void addRitualsContent() {
        addTitle("guide.adversity.rituals.title");
        addLine("");
        addWrappedText("guide.adversity.rituals.intro");
        addLine("");
        addSubtitle("guide.adversity.rituals.how_to");
        addWrappedText("guide.adversity.rituals.how_to.desc");
        addLine("");

        // Stage Unlock Rituals
        addSubtitle("guide.adversity.rituals.stages");
        addBullet("guide.adversity.rituals.awakening");
        addBullet("guide.adversity.rituals.scholar");
        addBullet("guide.adversity.rituals.warden");
        addBullet("guide.adversity.rituals.champion");
        addLine("");

        // Utility Rituals
        addSubtitle("guide.adversity.rituals.utility");
        addBullet("guide.adversity.rituals.cleanse_flesh");
        addBullet("guide.adversity.rituals.refine_iron");
        addBullet("guide.adversity.rituals.bless_apple");
        addBullet("guide.adversity.rituals.purge_curse");
        addBullet("guide.adversity.rituals.resurrection");
        addLine("");

        // Difficulty Adjustment Rituals
        addSubtitle("guide.adversity.rituals.difficulty");
        addBullet("guide.adversity.rituals.prayer_peace");
        addBullet("guide.adversity.rituals.peace_offering");
        addBullet("guide.adversity.rituals.tranquility");
        addBullet("guide.adversity.rituals.realm_cleansing");
    }

    private void addProgressionContent() {
        addTitle("guide.adversity.progression.title");
        addLine("");
        addWrappedText("guide.adversity.progression.intro");
        addLine("");
        addSubtitle("guide.adversity.progression.stages");
        for (int i = 0; i <= 4; i++)
            addBullet("guide.adversity.progression.stage." + i);
        addLine("");

        // Curses system
        addSubtitle("guide.adversity.curses.title");
        addWrappedText("guide.adversity.curses.intro");
        addLine("guide.adversity.curses.warning");
        addBullet("guide.adversity.curses.black_swan");
        addBullet("guide.adversity.curses.black_friday");
        addBullet("guide.adversity.curses.black_coffin");
        addBullet("guide.adversity.curses.counter");
        addLine("");

        // Nightmare system
        addSubtitle("guide.adversity.nightmare.title");
        addWrappedText("guide.adversity.nightmare.intro");
        addBullet("guide.adversity.nightmare.trigger");
        addBullet("guide.adversity.nightmare.scaling");
        addBullet("guide.adversity.nightmare.tip");
    }

    private void addOverviewContent() {
        addTitle("guide.adversity.overview.title");
        addLine("");
        addWrappedText("guide.adversity.overview.intro");
        addLine("");
        addSubtitle("guide.adversity.overview.difficulty");
        addWrappedText("guide.adversity.overview.difficulty.desc");
        addLine("");
        addSubtitle("guide.adversity.overview.elites");
        addWrappedText("guide.adversity.overview.elites.desc");
        addLine("");
        addSubtitle("guide.adversity.overview.affixes");
        addWrappedText("guide.adversity.overview.affixes.desc");
        addLine("");
        addSubtitle("guide.adversity.overview.counterplay");
        addWrappedText("guide.adversity.overview.counterplay.desc");
    }

    private void addAffixContent() {
        addTitle("guide.adversity.affixes.title");
        addLine("");
        addWrappedText("guide.adversity.affixes.intro");
        addLine("");

        // === 攻击型词条 ===
        addSubtitle("guide.adversity.affixes.offensive");
        addLine("");
        addBullet("guide.adversity.affixes.fiery");
        addBullet("guide.adversity.affixes.frosty");
        addBullet("guide.adversity.affixes.withering");
        addBullet("guide.adversity.affixes.blunt");
        addBullet("guide.adversity.affixes.giant_slayer");
        addBullet("guide.adversity.affixes.shield_breaker");
        addBullet("guide.adversity.affixes.bloodthirst");
        addBullet("guide.adversity.affixes.avarice");
        addBullet("guide.adversity.affixes.greed");
        addBullet("guide.adversity.affixes.reaper");
        addBullet("guide.adversity.affixes.nightmare");
        addLine("");

        // === 永久诅咒 ===
        addSubtitle("guide.adversity.affixes.curses");
        addLine("");
        addBullet("guide.adversity.affixes.black_swan");
        addBullet("guide.adversity.affixes.black_friday");
        addBullet("guide.adversity.affixes.black_coffin");
        addLine("");

        // === 防御型词条 ===
        addSubtitle("guide.adversity.affixes.defensive");
        addLine("");
        addBullet("guide.adversity.affixes.reflective");
        addBullet("guide.adversity.affixes.regenerating");
        addBullet("guide.adversity.affixes.hardened");
        addBullet("guide.adversity.affixes.fortified");
        addBullet("guide.adversity.affixes.hero");
        addBullet("guide.adversity.affixes.divine");
        addBullet("guide.adversity.affixes.outer_god");
        addBullet("guide.adversity.affixes.adaptive");
        addBullet("guide.adversity.affixes.antimagic");
        addBullet("guide.adversity.affixes.symbiotic");
        addBullet("guide.adversity.affixes.shedding");
        addBullet("guide.adversity.affixes.ascension");
        addLine("");

        // === 功能型词条 ===
        addSubtitle("guide.adversity.affixes.utility");
        addLine("");
        addBullet("guide.adversity.affixes.teleporting");
        addBullet("guide.adversity.affixes.blinding");
        addBullet("guide.adversity.affixes.horror");
        addBullet("guide.adversity.affixes.shackle");
        addBullet("guide.adversity.affixes.divest");
        addBullet("guide.adversity.affixes.disenchant");
        addBullet("guide.adversity.affixes.gravity");
        addBullet("guide.adversity.affixes.haste");
        addBullet("guide.adversity.affixes.splitting");
        addBullet("guide.adversity.affixes.oblivion");
        addBullet("guide.adversity.affixes.tetanus");
        addBullet("guide.adversity.affixes.reversal");
        addLine("");

        // === 特殊型词条 ===
        addSubtitle("guide.adversity.affixes.special");
        addLine("");
        addBullet("guide.adversity.affixes.vampiric");
        addBullet("guide.adversity.affixes.purifier");
        addBullet("guide.adversity.affixes.unclean");
        addBullet("guide.adversity.affixes.annihilate");
        addBullet("guide.adversity.affixes.thorny");
    }

    private void addSanctuaryContent() {
        addTitle("guide.adversity.sanctuary.title");
        addLine("");
        addWrappedText("guide.adversity.sanctuary.intro");
        addLine("");
        addSubtitle("guide.adversity.sanctuary.natural");
        addWrappedText("guide.adversity.sanctuary.natural.desc");
        addBullet("guide.adversity.sanctuary.natural.find");
        addBullet("guide.adversity.sanctuary.natural.activate");
        addBullet("guide.adversity.sanctuary.natural.effect");
        addLine("");
        addSubtitle("guide.adversity.sanctuary.artificial");
        addWrappedText("guide.adversity.sanctuary.artificial.desc");
        addBullet("guide.adversity.sanctuary.artificial.build");
        addBullet("guide.adversity.sanctuary.artificial.materials");
        addBullet("guide.adversity.sanctuary.artificial.effect");
        addLine("");

        // 多方块结构搭建说明
        addSubtitle("guide.adversity.sanctuary.structure");
        addWrappedText("guide.adversity.sanctuary.structure.desc");
        addLine("");
        addLine("§7┌─────────────────┐");
        addLine("§7│ §f第3层 (最顶层) §7│");
        addLine("§7│ §e石英楼梯 §7围绕中心 §7│");
        addLine("§7├─────────────────┤");
        addLine("§7│ §f第2层 (中间层) §7│");
        addLine("§7│ §b圆石/石砖 §7围绕  §7│");
        addLine("§7│ §6祭坛方块 §7在中心  §7│");
        addLine("§7├─────────────────┤");
        addLine("§7│ §f第1层 (底层)   §7│");
        addLine("§7│ §a石英块 §7=5x5方格  §7│");
        addLine("§7└─────────────────┘");
        addLine("");
        addBullet("guide.adversity.sanctuary.structure.layer1");
        addBullet("guide.adversity.sanctuary.structure.layer2");
        addBullet("guide.adversity.sanctuary.structure.layer3");
        addBullet("guide.adversity.sanctuary.structure.tip");
        addLine("");

        addSubtitle("guide.adversity.sanctuary.entropy");
        addWrappedText("guide.adversity.sanctuary.entropy.desc");
        int shardFuel = com.adversity.config.AdversityConfig.sanctuarySettings.entropyShardFuel;
        int crystalFuel = com.adversity.config.AdversityConfig.sanctuarySettings.entropyCrystalFuel;
        int coreFuel = com.adversity.config.AdversityConfig.sanctuarySettings.entropyCoreFuel;
        addLine("§7• 熵能碎片：基础燃料(§f" + shardFuel + "§7点)，掉落自T1-T4精英");
        addLine("§7• 熵能结晶：中级燃料(§f" + crystalFuel + "§7点)，掉落自T5-T7精英，或由碎片合成");
        addLine("§7• 熵能核心：高级燃料(§f" + coreFuel + "§7点)，掉落自T8-T10精英，或由结晶合成");
        addLine("");

        // ========== 动态数据：保护范围 ==========
        addSubtitle("guide.adversity.sanctuary.protection");
        addWrappedText("guide.adversity.sanctuary.protection.desc");
        addBullet("guide.adversity.sanctuary.protection.elite");
        addBullet("guide.adversity.sanctuary.protection.affix");
        addBullet("guide.adversity.sanctuary.protection.bauble");
        addBullet("guide.adversity.sanctuary.protection.craft");
        addLine("");

        int baseRadius = com.adversity.config.AdversityConfig.sanctuarySettings.naturalBaseRadius;
        int radiusPerTier = com.adversity.config.AdversityConfig.sanctuarySettings.naturalRadiusPerTier;

        addLine("§e⬡ 天然圣所保护范围");
        for (int t = 1; t <= 5; t++) {
            int radius = baseRadius + (t - 1) * radiusPerTier;
            addLine("§7  T" + t + ": §f" + radius + " §7格");
        }
        addLine("");

        // ========== 动态数据：模式对比 ==========
        addSubtitle("guide.adversity.sanctuary.modes");
        addWrappedText("guide.adversity.sanctuary.modes.desc");
        addLine("");

        double safeMul = com.adversity.config.AdversityConfig.sanctuarySettings.safeModeFuelMultiplier;
        double easeMul = com.adversity.config.AdversityConfig.sanctuarySettings.easeModeFuelMultiplier;
        double farmMul = com.adversity.config.AdversityConfig.sanctuarySettings.farmModeFuelMultiplier;
        double tierMul = com.adversity.config.AdversityConfig.sanctuarySettings.tierFuelMultiplier;
        double easeDiff = com.adversity.config.AdversityConfig.sanctuarySettings.easeModeDifficultyMultiplier;
        int naturalUsage = com.adversity.config.AdversityConfig.sanctuarySettings.naturalFuelUsage;
        int naturalCap = com.adversity.config.AdversityConfig.sanctuarySettings.naturalFuelCapacity;

        addLine("§e⧉ 模式对比");
        addLine("§6  🛡 庇护 SAFE §7- 消耗倍率: §c" + String.format("%.1f", safeMul) + "x");
        addLine("§7    完全阻止精英生成，最高词条压制");
        addLine("§6  📉 压制 EASE §7- 消耗倍率: §e" + String.format("%.1f", easeMul) + "x");
        addLine("§7    难度降低至 §f" + (int) (easeDiff * 100) + "% §7，词条压制+20%");
        addLine("§6  ⚔ 狩猎 FARM §7- 消耗倍率: §a" + String.format("%.1f", farmMul) + "x");
        addLine("§7    允许≤T4精英生成，词条压制大幅降低");
        addLine("");

        // ========== 动态数据：等级燃料消耗表 ==========
        addLine("§e⧉ 燃料消耗 (天然圣所, 每小时基础=" + naturalUsage + ")");
        addLine("§7  等级倍率公式: 模式×(1+(T-1)×" + String.format("%.2f", tierMul) + ")");
        addLine("");
        addLine("§7  等级  §6庇护/秒    §e压制/秒    §a狩猎/秒");
        addLine("§7  ────────────────────────────────");

        for (int t = 1; t <= 5; t++) {
            double tierScale = 1.0 + (t - 1) * tierMul;
            double safePerHour = naturalUsage * safeMul * tierScale;
            double easePerHour = naturalUsage * easeMul * tierScale;
            double farmPerHour = naturalUsage * farmMul * tierScale;

            // 每秒 = 每小时 / 3600
            String safeSec = String.format("%.2f", safePerHour / 3600.0);
            String easeSec = String.format("%.2f", easePerHour / 3600.0);
            String farmSec = String.format("%.2f", farmPerHour / 3600.0);

            addLine("§7  T" + t + "    §c" + safeSec + "      §e" + easeSec + "      §a" + farmSec);
        }
        addLine("");

        // 续航时间
        addLine("§e⧉ 满燃料续航时间 (容量=" + naturalCap + ")");
        for (int t = 1; t <= 5; t++) {
            double tierScale = 1.0 + (t - 1) * tierMul;
            double safeH = naturalCap / (naturalUsage * safeMul * tierScale);
            double easeH = naturalCap / (naturalUsage * easeMul * tierScale);
            double farmH = naturalCap / (naturalUsage * farmMul * tierScale);

            addLine("§7  T" + t + ": §c庇护" + String.format("%.1f", safeH)
                    + "h §e压制" + String.format("%.1f", easeH)
                    + "h §a狩猎" + String.format("%.1f", farmH) + "h");
        }
        addLine("");

        // 词条伤害压制
        addSubtitle("guide.adversity.sanctuary.suppress");
        addBullet("guide.adversity.sanctuary.suppress.natural");
        addBullet("guide.adversity.sanctuary.suppress.artificial");
        addLine("");

        // 升级机制
        addSubtitle("guide.adversity.sanctuary.upgrade");
        addWrappedText("guide.adversity.sanctuary.upgrade.desc");
        addBullet("guide.adversity.sanctuary.upgrade.t1");
        addBullet("guide.adversity.sanctuary.upgrade.t2");
        addBullet("guide.adversity.sanctuary.upgrade.t3");
        addLine("");

        // ========== CFG 可调参数 ==========
        addSubtitle("§e⚙ 配置参数 (adversity.cfg)");
        addLine("");
        addLine("§7  § 基础设置");
        addLine("§f  enableSanctuaries §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.enableSanctuaries);
        addLine("§f  activationItems §7= "
                + String.join(", ", com.adversity.config.AdversityConfig.sanctuarySettings.activationItems));
        addLine("§f  consumeActivationItem §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.consumeActivationItem);
        addLine("");
        addLine("§7  § 范围");
        addLine("§f  naturalBaseRadius §7= " + com.adversity.config.AdversityConfig.sanctuarySettings.naturalBaseRadius
                + " §8[100~10000]");
        addLine("§f  naturalRadiusPerTier §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.naturalRadiusPerTier + " §8[100~5000]");
        addLine("§f  artificialRadius §7= " + com.adversity.config.AdversityConfig.sanctuarySettings.artificialRadius
                + " §8[4~32]");
        addLine("");
        addLine("§7  § 燃料");
        addLine("§f  naturalFuelCapacity §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.naturalFuelCapacity + " §8[1000~1000000]");
        addLine("§f  artificialFuelCapacity §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.artificialFuelCapacity + " §8[1000~1000000]");
        addLine("§f  naturalFuelUsage §7= " + com.adversity.config.AdversityConfig.sanctuarySettings.naturalFuelUsage
                + "/h §8[0~1000]");
        addLine("§f  artificialFuelUsage §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.artificialFuelUsage + "/h §8[0~1000]");
        addLine("§f  entropyShardFuel §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.entropyShardFuel + " §8[1~1000]");
        addLine("§f  entropyCrystalFuel §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.entropyCrystalFuel + " §8[1~5000]");
        addLine("§f  entropyCoreFuel §7= "
                + com.adversity.config.AdversityConfig.sanctuarySettings.entropyCoreFuel + " §8[1~20000]");
        addLine("");
        addLine("§7  § 模式倍率");
        addLine("§f  safeModeFuelMultiplier §7= "
                + String.format("%.1f", com.adversity.config.AdversityConfig.sanctuarySettings.safeModeFuelMultiplier)
                + "x §8[0.1~10.0]");
        addLine("§f  easeModeFuelMultiplier §7= "
                + String.format("%.1f", com.adversity.config.AdversityConfig.sanctuarySettings.easeModeFuelMultiplier)
                + "x §8[0.1~10.0]");
        addLine("§f  farmModeFuelMultiplier §7= "
                + String.format("%.1f", com.adversity.config.AdversityConfig.sanctuarySettings.farmModeFuelMultiplier)
                + "x §8[0.1~10.0]");
        addLine("§f  tierFuelMultiplier §7= "
                + String.format("%.2f", com.adversity.config.AdversityConfig.sanctuarySettings.tierFuelMultiplier)
                + " §8[0.0~2.0]");
        addLine("§f  easeModeDifficultyMultiplier §7= "
                + String.format("%.1f",
                        com.adversity.config.AdversityConfig.sanctuarySettings.easeModeDifficultyMultiplier)
                + " §8[0.0~1.0]");
        addLine("");
        addLine("§7  § 维度");
        StringBuilder dims = new StringBuilder();
        for (int d : com.adversity.config.AdversityConfig.sanctuarySettings.allowedDimensions) {
            dims.append(d).append(" ");
        }
        addLine("§f  allowedDimensions §7= [" + dims.toString().trim() + "]");
        StringBuilder blackDims = new StringBuilder();
        for (int d : com.adversity.config.AdversityConfig.sanctuarySettings.dimensionBlacklist) {
            blackDims.append(d).append(" ");
        }
        addLine("§f  dimensionBlacklist §7= [" + blackDims.toString().trim() + "]");
        addLine("");

        // ========== CRT 接口 ==========
        addSubtitle("§e⚗ CraftTweaker 接口");
        addLine("");
        addLine("§6import mods.adversity.Sanctuary;");
        addLine("");
        addLine("§7  § 仪式管理");
        addLine("§f  addRitual§7(name, input, output, cost, reqStage, rewardStage)");
        addLine("§f  addRitualWithCommand§7(name, input, output, cost, reqStage, rewardStage, cmd)");
        addLine("§f  addCommandRitual§7(name, input, cost, reqStage, cmd)");
        addLine("§f  removeRitual§7(id)");
        addLine("§f  removeAllRituals§7()");
        addLine("§f  getRitualCount§7()");
        addLine("");
        addLine("§7  § 阶段门控");
        addLine("§f  setItemStage§7(item, stage)");
        addLine("§f  setItemStages§7(items[], stage)");
        addLine("§f  setEnchantmentStage§7(enchId, stage)");
        addLine("§f  removeItemStage§7(item)");
        addLine("§f  removeEnchantmentStage§7(enchId)");
    }

    private void addBaublesContent() {
        addTitle("guide.adversity.baubles.title");
        addLine("");
        addWrappedText("guide.adversity.baubles.intro");
        addLine("");
        addSubtitle("guide.adversity.baubles.equipment");
        addBullet("guide.adversity.baubles.soul_chain");
        addBullet("guide.adversity.baubles.spatial_anchor");
        addBullet("guide.adversity.baubles.enchant_guardian");
        addLine("");
        addSubtitle("guide.adversity.baubles.elemental");
        addBullet("guide.adversity.baubles.flame_ward");
        addBullet("guide.adversity.baubles.frost_ward");
        addBullet("guide.adversity.baubles.corrosion_bane");
        addLine("");
        addSubtitle("guide.adversity.baubles.special");
        addBullet("guide.adversity.baubles.guardian_heart");
        addBullet("guide.adversity.baubles.anchor_stone");
        addBullet("guide.adversity.baubles.clarity_lens");
        addBullet("guide.adversity.baubles.courage_charm");
        addLine("");
        addSubtitle("guide.adversity.baubles.counter");
        addBullet("guide.adversity.baubles.balance_charm");
        addBullet("guide.adversity.baubles.immunity_badge");
        addBullet("guide.adversity.baubles.memory_crystal");
        addBullet("guide.adversity.baubles.soul_seal_dust");
        addBullet("guide.adversity.baubles.light_amulet");
        addBullet("guide.adversity.baubles.anchor_heart");
        addBullet("guide.adversity.baubles.armor_pierce");
        addBullet("guide.adversity.baubles.barrier_ward");
        addBullet("guide.adversity.baubles.guardian_soul");
        addBullet("guide.adversity.baubles.purify_badge");
        addBullet("guide.adversity.baubles.true_crystal");
        addLine("");
        addSubtitle("guide.adversity.baubles.offensive");
        addBullet("guide.adversity.baubles.bloodrage_emblem");
        addBullet("guide.adversity.baubles.phantom_cloak");
        addBullet("guide.adversity.baubles.thunder_ring");
        addBullet("guide.adversity.baubles.aegis_medal");
        addLine("");
        addSubtitle("guide.adversity.baubles.utility");
        addBullet("guide.adversity.baubles.void_heart");
        addBullet("guide.adversity.baubles.temporal_watch");
        addBullet("guide.adversity.baubles.wither_mark");
        addLine("");
        addSubtitle("guide.adversity.baubles.sanctuary_bonus");
        addWrappedText("guide.adversity.baubles.sanctuary_bonus.desc");
    }

    private void addEnchantmentsContent() {
        addTitle("guide.adversity.enchants.title");
        addLine("");
        addWrappedText("guide.adversity.enchants.intro");
        addLine("");
        addSubtitle("guide.adversity.enchants.list");
        addBullet("guide.adversity.enchants.soulbound");
        addBullet("guide.adversity.enchants.breaker");
        addBullet("guide.adversity.enchants.entropy_affinity");
        addBullet("guide.adversity.enchants.purifying_touch");
        addBullet("guide.adversity.enchants.resolute_will");
    }

    private void addTipsContent() {
        addTitle("guide.adversity.tips.title");
        addLine("");
        addSubtitle("guide.adversity.tips.beginner");
        for (int i = 1; i <= 3; i++)
            addBullet("guide.adversity.tips.beginner." + i);
        addLine("");
        addSubtitle("guide.adversity.tips.advanced");
        for (int i = 1; i <= 3; i++)
            addBullet("guide.adversity.tips.advanced." + i);
        addLine("");
        addSubtitle("guide.adversity.tips.danger");
        for (int i = 1; i <= 2; i++)
            addBullet("guide.adversity.tips.danger." + i);
    }

    private void addCRTContent() {
        addTitle("guide.adversity.crt.title");
        addLine("");
        addWrappedText("guide.adversity.crt.intro");
        addLine("");

        // 仪式API
        addSubtitle("guide.adversity.crt.rituals");
        addBullet("guide.adversity.crt.rituals.add");
        addBullet("guide.adversity.crt.rituals.remove");
        addBullet("guide.adversity.crt.rituals.command");
        addLine("");

        // 阶段门控
        addSubtitle("guide.adversity.crt.stage_gating");
        addBullet("guide.adversity.crt.stage_gating.item");
        addBullet("guide.adversity.crt.stage_gating.enchant");
        addLine("");

        // 示例
        addSubtitle("guide.adversity.crt.example");
        addLine("§7import mods.adversity.Sanctuary;");
        addLine("§7Sanctuary.addRitual(id, in, out, cost, req, reward);");
        addLine("§7Sanctuary.setItemStage(<item>, \"scholar\");");
    }

    // === 内容辅助方法 ===

    private void addTitle(String key) {
        contentLines.add("§6§l" + I18n.format(key));
    }

    private void addSubtitle(String key) {
        contentLines.add("§e" + I18n.format(key));
    }

    private void addLine(String text) {
        contentLines.add(text);
    }

    private void addBullet(String key) {
        String text = I18n.format(key);
        // Bullet处理换行
        String bullet = "• ";
        List<String> wrapped = fontRenderer.listFormattedStringToWidth(text, contentAreaWidth - 10);
        for (int i = 0; i < wrapped.size(); i++) {
            if (i == 0) {
                contentLines.add("§7" + bullet + wrapped.get(i));
            } else {
                contentLines.add("§7  " + wrapped.get(i)); // 缩进
            }
        }
    }

    private void addWrappedText(String key) {
        String text = I18n.format(key);
        List<String> wrapped = fontRenderer.listFormattedStringToWidth(text, contentAreaWidth);
        for (String line : wrapped) {
            contentLines.add("§7" + line);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. 绘制全屏遮罩 (Overlay)
        drawDefaultBackground();

        // 2. 绘制GUI背景
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // 主背景 (深色书本风格)
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF202028);

        // 侧边栏背景
        drawRect(guiLeft, guiTop, guiLeft + SIDEBAR_WIDTH, guiTop + GUI_HEIGHT, 0xFF181820);

        // 边框装饰
        int borderColor = 0xFF5A4C63; // 紫色边框
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop, borderColor);
        drawHorizontalLine(guiLeft, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, borderColor);
        drawVerticalLine(guiLeft, guiTop, guiTop + GUI_HEIGHT, borderColor);
        drawVerticalLine(guiLeft + GUI_WIDTH - 1, guiTop, guiTop + GUI_HEIGHT, borderColor);
        drawVerticalLine(guiLeft + SIDEBAR_WIDTH, guiTop, guiTop + GUI_HEIGHT, borderColor);

        // 3. 绘制主标题
        String title = I18n.format("guide.adversity.title");
        this.drawCenteredString(fontRenderer, title, guiLeft + SIDEBAR_WIDTH + (GUI_WIDTH - SIDEBAR_WIDTH) / 2,
                guiTop + 12, 0xFFD4AF37);

        // 4. 绘制内容区 (使用 Scissor Test)
        int drawX = guiLeft + contentAreaX;
        int drawY = guiTop + contentAreaY;

        // 启用剪裁
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(drawX, drawY, contentAreaWidth, contentAreaHeight);

        int y = drawY - scrollOffset;
        for (String line : contentLines) {
            // 简单优化：只绘制在视口内的行
            if (y > drawY - 15 && y < drawY + contentAreaHeight) {
                fontRenderer.drawString(line, drawX, y, 0xFFFFFF);
            }
            y += 12; // 行高
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 5. 绘制滚动条
        if (maxScroll > 0) {
            int scrollBarX = guiLeft + GUI_WIDTH - 8;
            int scrollBarY = drawY;

            // 轨道
            drawRect(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + contentAreaHeight, 0x40000000);

            // 滑块
            int thumbHeight = Math.max(20, contentAreaHeight * contentAreaHeight / (maxScroll + contentAreaHeight));
            int thumbY = scrollBarY + (contentAreaHeight - thumbHeight) * scrollOffset / maxScroll;
            drawRect(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xFF808080);
        }

        // 6. 绘制按钮和Tooltip
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * 辅助方法：设置剪裁区域
     */
    private void scissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int screenHeight = mc.displayHeight;

        GL11.glScissor(x * scale, screenHeight - (y + height) * scale, width * scale, height * scale);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(null);
        } else if (button instanceof GuiTabButton) {
            // 切换标签
            currentTab = ((GuiTabButton) button).tabIndex;
            refreshContent();
            // 更新按钮状态
            for (GuiButton btn : buttonList) {
                if (btn instanceof GuiTabButton) {
                    ((GuiTabButton) btn).enabled = ((((GuiTabButton) btn).tabIndex) != currentTab);
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            scrollOffset -= Integer.signum(scroll) * 15; // 增加滚动速度
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * 自定义页签按钮
     */
    class GuiTabButton extends GuiButton {
        final int tabIndex;

        public GuiTabButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, int tabIndex) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
            this.tabIndex = tabIndex;
            // 初始激活状态 (非当前页签为激活/可点击)
            this.enabled = (tabIndex != currentTab);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
                        && mouseY < this.y + this.height;

                // 背景色
                int color = 0x00000000; // 透明
                if (!this.enabled) { // 被选中
                    color = 0xFF2A2A35; // 高亮背景
                    drawRect(this.x, this.y, this.x + width + 5, this.y + height, color); // 延伸到右侧
                    // 左侧指示条
                    drawRect(this.x, this.y, this.x + 2, this.y + height, 0xFFFFD700);
                } else if (this.hovered) {
                    color = 0xFF252530; // 悬停背景
                    drawRect(this.x, this.y, this.x + width, this.y + height, color);
                }

                // 文字
                int textColor = 0xE0E0E0;
                if (!this.enabled)
                    textColor = 0xFFD700; // 选中为金色
                else if (this.hovered)
                    textColor = 0xFFFFFF; // 悬停为白色

                // 绘制左对齐文字
                GuiAdversityGuide.this.fontRenderer.drawString(displayString, this.x + 8,
                        this.y + (this.height - 8) / 2, textColor);
            }
        }
    }
}
