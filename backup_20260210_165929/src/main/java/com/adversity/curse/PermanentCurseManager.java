package com.adversity.curse;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSyncCurse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

/**
 * 永久诅咒管理器
 * 管理黑天鹅、黑色星期五、黑棺等永久诅咒效果
 * 包含赎罪机制（只能预防，不能恢复）
 * 阈值和减少量可在配置文件中调整
 */
public class PermanentCurseManager extends WorldSavedData {

    private static final String DATA_NAME = Adversity.MODID + "_permanent_curses";

    // ==================== 诅咒类型常量 ====================

    /** 黑天鹅 - 攻击力削减 */
    public static final String CURSE_BLACK_SWAN = "black_swan";

    /** 黑色星期五 - 生命值削减 */
    public static final String CURSE_BLACK_FRIDAY = "black_friday";

    /** 黑棺 - 背包槽位封印 */
    public static final String CURSE_BLACK_COFFIN = "black_coffin";

    // ==================== 配置访问器 ====================

    /** 获取黑天鹅每次削减百分比 */
    public static float getBlackSwanReductionAmount() {
        return (float) AdversityConfig.curseSettings.blackSwanReductionPerTrigger;
    }

    /** 获取黑天鹅封禁阈值 */
    public static float getBlackSwanBanThreshold() {
        return (float) AdversityConfig.curseSettings.blackSwanBanThreshold;
    }

    /** 获取黑色星期五每次削减血量 */
    public static float getBlackFridayReductionAmount() {
        return (float) AdversityConfig.curseSettings.blackFridayReductionPerTrigger;
    }

    /** 获取黑色星期五封禁阈值 */
    public static float getBlackFridayBanThreshold() {
        return (float) AdversityConfig.curseSettings.blackFridayBanThreshold;
    }

    /** 获取黑棺每次封印槽位数 */
    public static int getBlackCoffinSlotsAmount() {
        return AdversityConfig.curseSettings.blackCoffinSlotsPerTrigger;
    }

    /** 获取黑棺封禁阈值 */
    public static int getBlackCoffinBanThreshold() {
        return AdversityConfig.curseSettings.blackCoffinBanThreshold;
    }

    /** 检查永久诅咒系统是否启用 */
    public static boolean isEnabled() {
        return AdversityConfig.curseSettings.enablePermanentCurses;
    }

    // ==================== 数据存储 ====================

    /** 玩家诅咒数据 - UUID -> CurseData */
    private final Map<UUID, PlayerCurseData> playerCurses = new HashMap<>();

    public PermanentCurseManager() {
        super(DATA_NAME);
    }

    public PermanentCurseManager(String name) {
        super(name);
    }

    /**
     * 获取世界的诅咒管理器实例
     */
    public static PermanentCurseManager get(World world) {
        PermanentCurseManager data = (PermanentCurseManager) world.getMapStorage()
            .getOrLoadData(PermanentCurseManager.class, DATA_NAME);

        if (data == null) {
            data = new PermanentCurseManager();
            world.getMapStorage().setData(DATA_NAME, data);
        }

        return data;
    }

    // ==================== 黑天鹅 (攻击力削减) ====================

    /**
     * 增加黑天鹅诅咒（削减攻击力）
     * @return 是否应该ban玩家
     */
    public boolean addBlackSwanCurse(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        PlayerCurseData data = getOrCreateData(playerId);

        float currentReduction = data.blackSwanReduction;
        float newReduction = currentReduction + getBlackSwanReductionAmount();

        // 检查是否达到ban阈值
        if (newReduction >= getBlackSwanBanThreshold()) {
            // 攻击力归零，应该ban
            data.blackSwanReduction = getBlackSwanBanThreshold();
            data.banned = true;
            data.banReason = "black_swan";
            markDirty();
            return true;
        }

        data.blackSwanReduction = newReduction;
        markDirty();

        // 发送警告（从第一次触发就开始警告）
        sendWarning(player, "black_swan", newReduction);

        return false;
    }

    /**
     * 获取黑天鹅攻击力削减百分比
     */
    public float getBlackSwanReduction(EntityPlayer player) {
        PlayerCurseData data = playerCurses.get(player.getUniqueID());
        return data != null ? data.blackSwanReduction : 0f;
    }

    // ==================== 黑色星期五 (生命值削减) ====================

    /**
     * 增加黑色星期五诅咒（削减最大生命值）
     * @return 是否应该ban玩家
     */
    public boolean addBlackFridayCurse(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        PlayerCurseData data = getOrCreateData(playerId);

        float currentReduction = data.blackFridayReduction;
        float newReduction = currentReduction + getBlackFridayReductionAmount();

        // 检查是否达到ban阈值（最大生命归零）
        float baseHealth = 20.0f;  // 原版基础生命
        if (baseHealth - newReduction <= getBlackFridayBanThreshold()) {
            data.blackFridayReduction = baseHealth - getBlackFridayBanThreshold();
            data.banned = true;
            data.banReason = "black_friday";
            markDirty();
            return true;
        }

        data.blackFridayReduction = newReduction;
        markDirty();

        // 发送警告（从第一次触发就开始警告）
        float remainingHealth = baseHealth - newReduction;
        sendWarning(player, "black_friday", newReduction / baseHealth);

        return false;
    }

    /**
     * 获取黑色星期五生命值削减量
     */
    public float getBlackFridayReduction(EntityPlayer player) {
        PlayerCurseData data = playerCurses.get(player.getUniqueID());
        return data != null ? data.blackFridayReduction : 0f;
    }

    // ==================== 黑棺 (背包封印) ====================

    /** 快捷栏封印ban阈值：必须封印完所有36个槽位（包括快捷栏）才会ban */
    private static final int HOTBAR_BAN_THRESHOLD = 36;

    /**
     * 增加黑棺诅咒（封印背包槽位）
     * @return 是否应该ban玩家
     */
    public boolean addBlackCoffinCurse(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        PlayerCurseData data = getOrCreateData(playerId);

        int currentSealed = data.blackCoffinSealed;
        int newSealed = currentSealed + getBlackCoffinSlotsAmount();
        int warningThreshold = getBlackCoffinBanThreshold();

        // 检查是否达到ban阈值（必须封印完快捷栏，即全部36个槽位）
        if (newSealed >= HOTBAR_BAN_THRESHOLD) {
            data.blackCoffinSealed = HOTBAR_BAN_THRESHOLD;
            data.banned = true;
            data.banReason = "black_coffin";
            markDirty();
            return true;
        }

        data.blackCoffinSealed = newSealed;
        markDirty();

        // 发送警告（从第一次触发就开始警告，使用配置的阈值计算百分比）
        sendWarning(player, "black_coffin", (float) newSealed / HOTBAR_BAN_THRESHOLD);

        return false;
    }

    /**
     * 获取黑棺封印槽位数
     */
    public int getBlackCoffinSealed(EntityPlayer player) {
        PlayerCurseData data = playerCurses.get(player.getUniqueID());
        return data != null ? data.blackCoffinSealed : 0;
    }

    /**
     * 检查指定槽位是否被黑棺封印
     * 先封主背包(9-35)，再封快捷栏(0-8)
     * 顺序：9,10,11...35, 0,1,2...8
     */
    public boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        int sealedCount = getBlackCoffinSealed(player);
        if (sealedCount <= 0) return false;

        // 主背包有27个槽位(9-35)，快捷栏有9个槽位(0-8)
        if (slotIndex >= 9 && slotIndex < 36) {
            // 主背包槽位：先封印
            // slotIndex 9 对应第1个封印，slotIndex 35 对应第27个封印
            int sealOrder = slotIndex - 9;  // 0-26
            return sealOrder < sealedCount;
        } else if (slotIndex >= 0 && slotIndex < 9) {
            // 快捷栏槽位：后封印（在主背包全部封印之后）
            // 需要超过27个封印才开始封快捷栏
            if (sealedCount <= 27) return false;
            int hotbarSealed = sealedCount - 27;  // 快捷栏已封印数
            return slotIndex < hotbarSealed;
        }
        return false;
    }

    // ==================== 赎罪系统 ====================

    /**
     * 使用赎罪物品减少诅咒
     * 注意：一旦被ban，无法通过赎罪恢复
     * 赎罪只能用于防止达到ban阈值
     * @return 是否赎罪成功
     */
    public boolean redeemCurse(EntityPlayer player, String curseType, float amount) {
        UUID playerId = player.getUniqueID();
        PlayerCurseData data = playerCurses.get(playerId);
        if (data == null) return false;

        // 已被ban的玩家无法赎罪
        if (data.banned) {
            return false;
        }

        switch (curseType) {
            case CURSE_BLACK_SWAN:
                data.blackSwanReduction = Math.max(0, data.blackSwanReduction - amount);
                break;
            case CURSE_BLACK_FRIDAY:
                data.blackFridayReduction = Math.max(0, data.blackFridayReduction - amount);
                break;
            case CURSE_BLACK_COFFIN:
                data.blackCoffinSealed = Math.max(0, data.blackCoffinSealed - (int) amount);
                break;
            default:
                return false;
        }

        markDirty();

        // 同步到客户端
        syncToClient(player);

        return true;
    }

    // ==================== Ban检查 ====================

    /**
     * 检查玩家是否被ban
     */
    public boolean isPlayerBanned(UUID playerId) {
        PlayerCurseData data = playerCurses.get(playerId);
        return data != null && data.banned;
    }

    /**
     * 获取ban原因
     */
    public String getBanReason(UUID playerId) {
        PlayerCurseData data = playerCurses.get(playerId);
        return data != null ? data.banReason : null;
    }

    /**
     * 踢出被ban的玩家
     */
    public void kickBannedPlayer(EntityPlayerMP player) {
        String reason = getBanReason(player.getUniqueID());
        String langKey = "adversity.curse.banned." + (reason != null ? reason : "default");
        player.connection.disconnect(new TextComponentTranslation(langKey));
    }

    // ==================== 辅助方法 ====================

    private PlayerCurseData getOrCreateData(UUID playerId) {
        return playerCurses.computeIfAbsent(playerId, k -> new PlayerCurseData());
    }

    private void sendWarning(EntityPlayer player, String curseType, float severity) {
        TextComponentTranslation warning = new TextComponentTranslation(
            "adversity.curse.warning." + curseType,
            (int)(severity * 100)
        );
        warning.getStyle().setColor(severity > 0.7f ? TextFormatting.DARK_RED : TextFormatting.RED);
        player.sendMessage(warning);

        // 同步到客户端
        syncToClient(player);
    }

    /**
     * 同步诅咒数据到客户端
     */
    public void syncToClient(EntityPlayer player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayerMP)) return;

        PlayerCurseData data = playerCurses.get(player.getUniqueID());
        int sealed = data != null ? data.blackCoffinSealed : 0;
        float attack = data != null ? data.blackSwanReduction : 0f;
        float health = data != null ? data.blackFridayReduction : 0f;

        PacketSyncCurse packet = new PacketSyncCurse(sealed, attack, health);
        PacketHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) player);
    }

    // ==================== NBT 序列化 ====================

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        playerCurses.clear();

        int count = nbt.getInteger("PlayerCount");
        for (int i = 0; i < count; i++) {
            NBTTagCompound playerNbt = nbt.getCompoundTag("Player_" + i);
            UUID playerId = UUID.fromString(playerNbt.getString("UUID"));

            PlayerCurseData data = new PlayerCurseData();
            data.blackSwanReduction = playerNbt.getFloat("BlackSwanReduction");
            data.blackFridayReduction = playerNbt.getFloat("BlackFridayReduction");
            data.blackCoffinSealed = playerNbt.getInteger("BlackCoffinSealed");
            data.banned = playerNbt.getBoolean("Banned");
            data.banReason = playerNbt.hasKey("BanReason") ? playerNbt.getString("BanReason") : null;

            playerCurses.put(playerId, data);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("PlayerCount", playerCurses.size());

        int i = 0;
        for (Map.Entry<UUID, PlayerCurseData> entry : playerCurses.entrySet()) {
            NBTTagCompound playerNbt = new NBTTagCompound();
            playerNbt.setString("UUID", entry.getKey().toString());

            PlayerCurseData data = entry.getValue();
            playerNbt.setFloat("BlackSwanReduction", data.blackSwanReduction);
            playerNbt.setFloat("BlackFridayReduction", data.blackFridayReduction);
            playerNbt.setInteger("BlackCoffinSealed", data.blackCoffinSealed);
            playerNbt.setBoolean("Banned", data.banned);
            if (data.banReason != null) {
                playerNbt.setString("BanReason", data.banReason);
            }

            nbt.setTag("Player_" + i, playerNbt);
            i++;
        }

        return nbt;
    }

    /**
     * 玩家诅咒数据结构
     */
    private static class PlayerCurseData {
        float blackSwanReduction = 0f;
        float blackFridayReduction = 0f;
        int blackCoffinSealed = 0;
        boolean banned = false;
        String banReason = null;
    }
}
