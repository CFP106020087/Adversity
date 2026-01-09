package com.adversity.curse;

import com.adversity.Adversity;
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
 * 包含赎罪机制
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

    // ==================== 配置常量 ====================

    /** 黑天鹅每次削减百分比 */
    public static final float BLACK_SWAN_REDUCTION = 0.05f;  // 5%

    /** 黑天鹅最大削减百分比 */
    public static final float BLACK_SWAN_MAX_REDUCTION = 0.8f;  // 80%

    /** 黑色星期五每次削减血量（半心） */
    public static final float BLACK_FRIDAY_REDUCTION = 1.0f;  // 0.5心

    /** 黑色星期五最小血量 */
    public static final float BLACK_FRIDAY_MIN_HEALTH = 2.0f;  // 1心

    /** 黑棺最大封印槽位数 */
    public static final int BLACK_COFFIN_MAX_SLOTS = 27;  // 背包主区域

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
        float newReduction = currentReduction + BLACK_SWAN_REDUCTION;

        // 检查是否达到ban阈值
        if (newReduction >= 1.0f) {
            // 攻击力归零，应该ban
            data.blackSwanReduction = 1.0f;
            data.banned = true;
            data.banReason = "black_swan";
            markDirty();
            return true;
        }

        data.blackSwanReduction = Math.min(newReduction, BLACK_SWAN_MAX_REDUCTION);
        markDirty();

        // 发送警告
        if (newReduction >= 0.5f) {
            sendWarning(player, "black_swan", newReduction);
        }

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
        float newReduction = currentReduction + BLACK_FRIDAY_REDUCTION;

        // 检查是否达到ban阈值（最大生命归零）
        float baseHealth = 20.0f;  // 原版基础生命
        if (baseHealth - newReduction <= 0) {
            data.blackFridayReduction = baseHealth;
            data.banned = true;
            data.banReason = "black_friday";
            markDirty();
            return true;
        }

        data.blackFridayReduction = newReduction;
        markDirty();

        // 发送警告
        float remainingHealth = baseHealth - newReduction;
        if (remainingHealth <= 6.0f) {  // 3心以下
            sendWarning(player, "black_friday", newReduction / baseHealth);
        }

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

    /**
     * 增加黑棺诅咒（封印背包槽位）
     * @return 是否应该ban玩家
     */
    public boolean addBlackCoffinCurse(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        PlayerCurseData data = getOrCreateData(playerId);

        int currentSealed = data.blackCoffinSealed;
        int newSealed = currentSealed + 1;

        // 检查是否达到ban阈值
        if (newSealed >= BLACK_COFFIN_MAX_SLOTS) {
            data.blackCoffinSealed = BLACK_COFFIN_MAX_SLOTS;
            data.banned = true;
            data.banReason = "black_coffin";
            markDirty();
            return true;
        }

        data.blackCoffinSealed = newSealed;
        markDirty();

        // 发送警告
        if (newSealed >= BLACK_COFFIN_MAX_SLOTS - 5) {
            sendWarning(player, "black_coffin", (float) newSealed / BLACK_COFFIN_MAX_SLOTS);
        }

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
     */
    public boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        int sealedCount = getBlackCoffinSealed(player);
        // 从背包末尾开始封印
        return slotIndex >= (36 - sealedCount) && slotIndex < 36;
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
