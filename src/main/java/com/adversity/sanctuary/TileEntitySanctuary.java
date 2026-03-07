package com.adversity.sanctuary;

import com.adversity.Adversity;
import com.adversity.item.ItemEntropy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * 圣所祭坛/核心方块实体
 * 管理圣所的燃料存储和状态显示
 */
public class TileEntitySanctuary extends TileEntity implements ITickable, IInventory {

    private SanctuaryType type = SanctuaryType.NATURAL;
    private int tier = 1; // 仅天然圣所使用
    private boolean activated = false;

    // 本地缓存，每秒从SanctuaryData同步
    private int cachedFuel = 0;
    private int cachedMaxFuel = 0;
    private long lastSyncTick = 0;
    private SanctuaryMode cachedMode = SanctuaryMode.SAFE;

    private static final int SYNC_INTERVAL = 20; // 每秒同步一次

    // ========== 槽位常量 ==========
    public static final int MAX_RITUAL_SLOTS = 100;
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_RITUAL_START = 2; // 2 ~ 101
    public static final int TOTAL_SLOTS = 2 + MAX_RITUAL_SLOTS; // 102

    // 物品槽位陣列
    private ItemStack[] inventory = new ItemStack[TOTAL_SLOTS];

    public TileEntitySanctuary() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
    }

    public void setType(SanctuaryType type) {
        this.type = type;
        markDirty();
    }

    public void setTier(int tier) {
        this.tier = tier;
        markDirty();
    }

    public SanctuaryType getType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    public boolean isActivated() {
        return activated;
    }

    public SanctuaryMode getMode() {
        return cachedMode;
    }

    /**
     * 激活圣所
     */
    public boolean activate() {
        if (activated || world == null || world.isRemote) {
            return false;
        }

        boolean success;
        if (type == SanctuaryType.NATURAL) {
            success = SanctuaryManager.activateNaturalSanctuary(world, pos, tier);
        } else {
            success = SanctuaryManager.activateArtificialSanctuary(world, pos);
        }

        if (success) {
            activated = true;
            markDirty();
            syncFromData();
            Adversity.LOGGER.info("Sanctuary activated at {} (type={}, tier={})", pos, type, tier);
        }

        return success;
    }

    /**
     * 停用圣所
     */
    public boolean deactivate() {
        if (!activated || world == null || world.isRemote) {
            return false;
        }

        boolean success = SanctuaryManager.deactivateSanctuary(world, pos);
        if (success) {
            activated = false;
            cachedFuel = 0;
            markDirty();
        }

        return success;
    }

    /**
     * 添加燃料
     * 使用范围查找圣所（与 syncFromData 一致），避免精确坐标不匹配导致添加失败
     * 
     * @return 添加后的燃料值
     */
    public int addFuel(int amount) {
        if (!activated || world == null || world.isRemote) {
            return -1;
        }

        SanctuaryData data = SanctuaryData.get(world);
        SanctuaryZone zone = data.getSanctuaryAt(world.provider.getDimension(), pos);
        if (zone != null) {
            zone.fuel = Math.min(zone.maxFuel, zone.fuel + amount);
            data.markDirty();
            cachedFuel = zone.fuel;
            cachedMaxFuel = zone.maxFuel;
            return zone.fuel;
        }
        return -1;
    }

    /**
     * 获取缓存的燃料值
     */
    public int getFuel() {
        return cachedFuel;
    }

    /**
     * 获取缓存的最大燃料值
     */
    public int getMaxFuel() {
        return cachedMaxFuel;
    }

    /**
     * 获取燃料百分比 (0.0-1.0)
     */
    public float getFuelPercentage() {
        return cachedMaxFuel > 0 ? (float) cachedFuel / cachedMaxFuel : 0;
    }

    /**
     * 圣所是否有燃料运作中
     */
    public boolean isActive() {
        return activated && cachedFuel > 0;
    }

    /**
     * 切换圣所模式
     */
    public boolean toggleMode() {
        if (!activated || world == null || world.isRemote) {
            return false;
        }

        SanctuaryMode nextMode = cachedMode.next();
        if (SanctuaryManager.setSanctuaryMode(world, pos, nextMode)) {
            cachedMode = nextMode;
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * 升级圣所等级
     */
    public boolean upgradeTier() {
        if (!activated || world == null || world.isRemote) {
            return false;
        }

        if (type != SanctuaryType.NATURAL) {
            return false; // 只有天然圣所可以升级
        }

        if (tier >= 5) {
            return false; // 最高5级
        }

        // 升级需要熵能满（消耗全部燃料）
        if (cachedFuel < cachedMaxFuel || cachedMaxFuel <= 0) {
            return false;
        }

        // 扣除全部燃料
        SanctuaryManager.consumeFuel(world, pos, cachedFuel);
        cachedFuel = 0;

        // 升级
        int oldTier = tier;
        tier++;
        SanctuaryManager.upgradeSanctuary(world, pos, tier);
        markDirty();

        // 清空旧建筑 → 放置新等级schematic
        SanctuaryGenerator.upgradeSanctuary(world, pos, oldTier, tier);

        // 升级后新的TileEntity需要继承激活状态
        net.minecraft.tileentity.TileEntity newTe = world.getTileEntity(pos.up());
        if (newTe instanceof TileEntitySanctuary) {
            TileEntitySanctuary newSanc = (TileEntitySanctuary) newTe;
            newSanc.activated = true;
            newSanc.tier = tier;
            newSanc.type = SanctuaryType.NATURAL;
            newSanc.syncFromData();
            newSanc.markDirty();
        }

        // 播放升级特效
        playUpgradeEffects();

        return true;
    }

    /**
     * 播放圣所升级视觉效果
     */
    private void playUpgradeEffects() {
        if (world == null || world.isRemote)
            return;

        // 播放音效
        world.playSound(null, pos, net.minecraft.init.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.util.SoundCategory.BLOCKS, 1.0f, 1.0f);

        // 获取新半径用于粒子范围
        int newRadius = com.adversity.config.AdversityConfig.sanctuarySettings.naturalBaseRadius +
                (tier - 1) * com.adversity.config.AdversityConfig.sanctuarySettings.naturalRadiusPerTier;

        // 生成扩展波粒子 - 从中心向外扩散的圆环
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) world;

            // 多层粒子环，模拟扩张效果
            for (int ring = 0; ring < 3; ring++) {
                final int ringDelay = ring * 5; // 延迟帧
                final int ringRadius = newRadius - 10 + ring * 5;

                // 在圆周上生成粒子
                for (int angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double px = pos.getX() + 0.5 + ringRadius * Math.cos(rad);
                    double py = pos.getY() + 1.0;
                    double pz = pos.getZ() + 0.5 + ringRadius * Math.sin(rad);

                    // 金色火焰粒子
                    ws.spawnParticle(net.minecraft.util.EnumParticleTypes.FLAME,
                            px, py, pz, 1, 0.3, 0.5, 0.3, 0.02);
                    // 末影粒子作为魔法效果
                    ws.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL,
                            px, py + 0.5, pz, 3, 0.5, 1.0, 0.5, 0.1);
                }
            }

            // 祭坛中心的爆发粒子
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.TOTEM,
                    pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                    50, 0.5, 1.0, 0.5, 0.5);
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    30, 0.3, 0.5, 0.3, 0.1);
        }

        // 向附近玩家发送屏幕覆盖效果
        for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
            if (player.getDistanceSq(pos) < 2500) { // 50格内
                if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                    com.adversity.network.PacketHandler.INSTANCE.sendTo(
                            new com.adversity.network.PacketVisualEffect(
                                    com.adversity.client.visual.VisualEffectType.SANCTUARY_UPGRADE,
                                    40, // 2秒
                                    0.8f, // 强度
                                    -1),
                            (net.minecraft.entity.player.EntityPlayerMP) player);
                }
            }
        }
    }

    /**
     * 传送到其他圣所
     */
    public boolean teleportTo(net.minecraft.entity.player.EntityPlayer player, BlockPos targetPos) {
        if (!activated || world == null || world.isRemote)
            return false;

        // 不能传送到自己
        if (targetPos.equals(pos)) {
            player.sendMessage(
                    new net.minecraft.util.text.TextComponentTranslation("adversity.sanctuary.teleport.same"));
            return false;
        }

        // 检查目标是否是有效的激活圣所 (检查 TileEntity)
        net.minecraft.tileentity.TileEntity targetTE = world.getTileEntity(targetPos);
        if (!(targetTE instanceof TileEntitySanctuary)) {
            player.sendMessage(
                    new net.minecraft.util.text.TextComponentTranslation("adversity.sanctuary.teleport.invalid"));
            return false;
        }

        TileEntitySanctuary targetSanctuary = (TileEntitySanctuary) targetTE;
        if (!targetSanctuary.isActivated()) {
            player.sendMessage(
                    new net.minecraft.util.text.TextComponentTranslation("adversity.sanctuary.teleport.inactive"));
            return false;
        }

        // 检查熵能是否足够 (消耗 500)
        int cost = 500;
        if (cachedFuel < cost) {
            player.sendMessage(
                    new net.minecraft.util.text.TextComponentTranslation("adversity.sanctuary.teleport.no_fuel"));
            return false;
        }

        // 扣除燃料
        SanctuaryManager.consumeFuel(world, pos, cost);
        cachedFuel -= cost;

        // 执行传送
        if (player.dimension != world.provider.getDimension()) {
            player.changeDimension(world.provider.getDimension());
        }
        player.setPositionAndUpdate(targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5);

        // 特效
        world.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);
        world.playSound(null, targetPos, net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);

        player.sendMessage(
                new net.minecraft.util.text.TextComponentTranslation("adversity.sanctuary.teleport.success"));
        return true;
    }

    /**
     * 尝试执行仪式
     * 
     * @param player 触发仪式的玩家
     * @param input  输入物品
     * @return 仪式结果 (消耗了输入物品则返回剩余物品，否则返回原物品)
     */
    public net.minecraft.item.ItemStack performRitual(net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.item.ItemStack input) {
        if (!activated || world == null || world.isRemote)
            return input;

        // 查找匹配的仪式
        com.adversity.sanctuary.ritual.Rite rite = com.adversity.sanctuary.ritual.RitualManager.getRite(input);
        if (rite == null)
            return input;

        // 检查燃料
        if (cachedFuel < rite.getEntropyCost()) {
            player.sendMessage(
                    new net.minecraft.util.text.TextComponentTranslation("adversity.ritual.not_enough_fuel"));
            return input;
        }

        // 检查前置阶段
        com.adversity.capability.IAdversityCapability.IProgression progression = player
                .getCapability(com.adversity.capability.CapabilityHandler.PROGRESSION_CAPABILITY, null);

        if (rite.getRequiredStage() != null && progression != null && !progression.hasStage(rite.getRequiredStage())) {
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("adversity.ritual.stage_locked",
                    rite.getRequiredStage()));
            return input;
        }

        // 消耗燃料
        SanctuaryManager.consumeFuel(world, pos, rite.getEntropyCost());
        cachedFuel -= rite.getEntropyCost();

        // 消耗物品
        input.shrink(rite.getInput().getCount());

        // 给予产物
        if (!rite.getOutput().isEmpty()) {
            net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, rite.getOutput().copy());
            world.spawnEntity(entityItem);
        }

        // 给予阶段奖励 (通过 EventHandler 触发事件)
        if (rite.getRewardStage() != null) {
            com.adversity.progression.ProgressionEventHandler.addStage(player, rite.getRewardStage());
        }

        // 播放音效
        world.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_ILLAGER_CAST_SPELL,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);

        return input;
    }

    /**
     * 从仪式槽位执行仪式 (GUI按钮调用)
     * 
     * @param player 触发仪式的玩家
     * @return 是否成功执行
     */
    public boolean performRitualFromSlot(net.minecraft.entity.player.EntityPlayer player) {
        if (!activated || world == null || world.isRemote)
            return false;

        // 检查输入槽位（第一个输入槽）
        if (inventory[SLOT_RITUAL_START].isEmpty())
            return false;

        // 检查输出槽位是否为空
        if (!inventory[SLOT_OUTPUT].isEmpty())
            return false;

        // 查找匹配的仪式
        com.adversity.sanctuary.ritual.Rite rite = com.adversity.sanctuary.ritual.RitualManager
                .getRite(inventory[SLOT_RITUAL_START]);
        if (rite == null) {
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("adversity.ritual.no_match"));
            return false;
        }

        // 检查输入数量是否足够
        if (inventory[SLOT_RITUAL_START].getCount() < rite.getInput().getCount()) {
            player.sendMessage(
                    new net.minecraft.util.text.TextComponentTranslation("adversity.ritual.not_enough_input"));
            return false;
        }

        // 检查燃料
        if (cachedFuel < rite.getEntropyCost()) {
            player.sendMessage(
                    new net.minecraft.util.text.TextComponentTranslation("adversity.ritual.not_enough_fuel"));
            return false;
        }

        // 检查前置阶段
        com.adversity.capability.IAdversityCapability.IProgression progression = player
                .getCapability(com.adversity.capability.CapabilityHandler.PROGRESSION_CAPABILITY, null);

        if (rite.getRequiredStage() != null && progression != null && !progression.hasStage(rite.getRequiredStage())) {
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("adversity.ritual.stage_locked",
                    rite.getRequiredStage()));
            return false;
        }

        // 检查冷却
        if (rite.hasCooldown()) {
            long remaining = com.adversity.sanctuary.ritual.RitualManager.getRemainingCooldown(
                    player.getName(), rite, world.getTotalWorldTime());
            if (remaining > 0) {
                int seconds = (int) (remaining / 20);
                player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "adversity.ritual.cooldown", seconds));
                return false;
            }
        }

        // 消耗燃料
        SanctuaryManager.consumeFuel(world, pos, rite.getEntropyCost());
        cachedFuel -= rite.getEntropyCost();

        // 消耗输入物品
        inventory[SLOT_RITUAL_START].shrink(rite.getInput().getCount());

        // 放置输出物品
        if (!rite.getOutput().isEmpty()) {
            inventory[SLOT_OUTPUT] = rite.getOutput().copy();
        }

        // 给予阶段奖励 (通过 EventHandler 触发事件)
        if (rite.getRewardStage() != null) {
            com.adversity.progression.ProgressionEventHandler.addStage(player, rite.getRewardStage());
        }

        // 设置冷却
        if (rite.hasCooldown()) {
            com.adversity.sanctuary.ritual.RitualManager.setCooldown(
                    player.getName(), rite, world.getTotalWorldTime());
        }

        // 执行仪式命令（静默执行）
        if (rite.hasCommand() && player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            net.minecraft.server.MinecraftServer server = world.getMinecraftServer();
            rite.executeCommand(server, (net.minecraft.entity.player.EntityPlayerMP) player);
        }

        // ==================== 特殊仪式效果处理 ====================
        handleSpecialRitualEffects(rite, player);

        // 播放音效
        world.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_ILLAGER_CAST_SPELL,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);

        markDirty();
        return true;
    }

    /**
     * 處理儀式效果（通過 RitualEffectRegistry 分派）
     */
    private void handleSpecialRitualEffects(com.adversity.sanctuary.ritual.Rite rite,
            net.minecraft.entity.player.EntityPlayer player) {

        // 優先使用 rite 指定的 effectId
        net.minecraft.util.ResourceLocation effectId = rite.getEffectId();

        // 如果沒有指定 effectId，嘗試用 rite ID 查找（向後兼容）
        if (effectId == null) {
            effectId = rite.getId();
        }

        com.adversity.sanctuary.ritual.IRitualEffect effect = com.adversity.sanctuary.ritual.RitualEffectRegistry
                .get(effectId);

        if (effect != null) {
            effect.apply(player, rite, this, rite.getEffectParams());
        }
        // 沒有註冊效果 = 純物品轉化/命令儀式，無額外效果
    }

    /**
     * 获取当前输入槽位匹配的仪式信息 (用于GUI显示)
     */
    public com.adversity.sanctuary.ritual.Rite getMatchingRite() {
        if (inventory[SLOT_RITUAL_START].isEmpty())
            return null;
        return com.adversity.sanctuary.ritual.RitualManager.getRite(inventory[SLOT_RITUAL_START]);
    }

    @Override
    public void update() {
        if (world.isRemote)
            return;

        // 处理燃料槽位的物品
        if (!inventory[SLOT_FUEL].isEmpty() && activated) {
            int fuelValue = getFuelValueFromStack(inventory[SLOT_FUEL]);
            if (fuelValue > 0 && cachedFuel < cachedMaxFuel) {
                // 添加燃料
                int added = addFuel(fuelValue);
                if (added >= 0) {
                    inventory[SLOT_FUEL].shrink(1);
                    markDirty();
                } else {
                    Adversity.LOGGER.warn("Sanctuary fuel add failed at {} (zone not found?)", pos);
                }
            }
        }

        if (!activated)
            return;

        // 定期从SanctuaryData同步数据
        long currentTick = world.getTotalWorldTime();
        if (currentTick - lastSyncTick >= SYNC_INTERVAL) {
            syncFromData();
            lastSyncTick = currentTick;
        }
    }

    /**
     * 获取物品堆的燃料值
     */
    public static int getFuelValueFromStack(ItemStack stack) {
        if (stack.isEmpty())
            return 0;

        if (stack.getItem() instanceof ItemEntropy) {
            return ItemEntropy.getFuelValue(stack);
        }
        return 0;
    }

    /**
     * 检查物品是否是有效燃料
     */
    public static boolean isValidFuel(ItemStack stack) {
        return getFuelValueFromStack(stack) > 0;
    }

    /**
     * 从SanctuaryData同步燃料数据
     */
    private void syncFromData() {
        if (world == null || world.isRemote)
            return;

        SanctuaryData data = SanctuaryData.get(world);
        SanctuaryZone zone = data.getSanctuaryAt(world.provider.getDimension(), pos);

        if (zone != null) {
            cachedFuel = zone.fuel;
            cachedMaxFuel = zone.maxFuel;
            cachedMode = zone.mode;
        } else {
            // 圣所数据丢失，重置状态
            activated = false;
            cachedFuel = 0;
            cachedMaxFuel = 0;
            cachedMode = SanctuaryMode.SAFE;
        }
    }

    // ==================== IInventory 实现 ====================

    @Override
    public int getSizeInventory() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            return inventory[index];
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack target = getStackInSlot(index);
        if (target.isEmpty())
            return ItemStack.EMPTY;

        ItemStack result = target.splitStack(count);
        if (target.isEmpty()) {
            setInventorySlotContents(index, ItemStack.EMPTY);
        }
        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack result = getStackInSlot(index);
        setInventorySlotContents(index, ItemStack.EMPTY);
        markDirty();
        return result;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            inventory[index] = stack;
        }
        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return player.getDistanceSq(pos) <= 64.0;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        // 打开GUI时立即同步，确保燃料条显示最新数据
        if (!world.isRemote && activated) {
            syncFromData();
        }
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == SLOT_FUEL)
            return isValidFuel(stack);
        if (index == SLOT_OUTPUT)
            return false; // 输出槽只能取出
        if (index >= SLOT_RITUAL_START && index < TOTAL_SLOTS)
            return true; // 仪式输入槽
        return false;
    }

    @Override
    public int getField(int id) {
        switch (id) {
            case 0:
                return cachedFuel;
            case 1:
                return cachedMaxFuel;
            case 2:
                return cachedMode.ordinal();
            case 3:
                return tier;
            case 4:
                return activated ? 1 : 0;
            default:
                return 0;
        }
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0:
                cachedFuel = value;
                break;
            case 1:
                cachedMaxFuel = value;
                break;
            case 2:
                cachedMode = SanctuaryMode.values()[value % SanctuaryMode.values().length];
                break;
            case 3:
                tier = value;
                break;
            case 4:
                activated = value != 0;
                break;
        }
    }

    @Override
    public int getFieldCount() {
        return 5;
    }

    @Override
    public void clear() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public String getName() {
        return "tile.adversity.sanctuary_altar.name";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    // ==================== NBT 序列化 ====================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("sanctuaryType", type.name());
        nbt.setInteger("tier", tier);
        nbt.setBoolean("activated", activated);

        // 保存所有非空槽位
        net.minecraft.nbt.NBTTagList invList = new net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!inventory[i].isEmpty()) {
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setInteger("Slot", i);
                inventory[i].writeToNBT(slotTag);
                invList.appendTag(slotTag);
            }
        }
        nbt.setTag("Inventory", invList);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        try {
            type = SanctuaryType.valueOf(nbt.getString("sanctuaryType"));
        } catch (Exception e) {
            type = SanctuaryType.NATURAL;
        }
        tier = nbt.getInteger("tier");
        activated = nbt.getBoolean("activated");

        // 清空并重讀
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory[i] = ItemStack.EMPTY;
        }

        if (nbt.hasKey("Inventory")) {
            // 新格式
            net.minecraft.nbt.NBTTagList invList = nbt.getTagList("Inventory", 10);
            for (int i = 0; i < invList.tagCount(); i++) {
                NBTTagCompound slotTag = invList.getCompoundTagAt(i);
                int slot = slotTag.getInteger("Slot");
                if (slot >= 0 && slot < TOTAL_SLOTS) {
                    inventory[slot] = new ItemStack(slotTag);
                }
            }
        } else {
            // 舉格式向後兌容
            if (nbt.hasKey("FuelSlot")) {
                inventory[SLOT_FUEL] = new ItemStack(nbt.getCompoundTag("FuelSlot"));
            }
            if (nbt.hasKey("RitualInputSlot")) {
                inventory[SLOT_RITUAL_START] = new ItemStack(nbt.getCompoundTag("RitualInputSlot"));
            }
            if (nbt.hasKey("RitualOutputSlot")) {
                inventory[SLOT_OUTPUT] = new ItemStack(nbt.getCompoundTag("RitualOutputSlot"));
            }
        }
    }
}
