package com.adversity.sanctuary;

import com.adversity.Adversity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 圣所数据持久化
 * 存储所有激活的圣所信息（天然+人造）
 */
public class SanctuaryData extends WorldSavedData {

    private static final String DATA_NAME = Adversity.MODID + "_sanctuary";

    // 所有激活的圣所
    private final List<SanctuaryZone> sanctuaries = new ArrayList<>();

    public SanctuaryData() {
        super(DATA_NAME);
    }

    public SanctuaryData(String name) {
        super(name);
    }

    /**
     * 获取世界的圣所数据
     */
    public static SanctuaryData get(World world) {
        // 始终使用主世界存储，避免跨维度问题
        World overworld = world.getMinecraftServer() != null 
            ? world.getMinecraftServer().getWorld(0) 
            : world;
        
        MapStorage storage = overworld.getMapStorage();
        if (storage == null) {
            return new SanctuaryData();
        }

        SanctuaryData data = (SanctuaryData) storage.getOrLoadData(
            SanctuaryData.class, DATA_NAME);

        if (data == null) {
            data = new SanctuaryData();
            storage.setData(DATA_NAME, data);
        }

        return data;
    }

    // ==================== 圣所管理 ====================

    /**
     * 添加圣所
     */
    public void addSanctuary(SanctuaryZone zone) {
        // 检查是否已存在相同位置的圣所
        for (SanctuaryZone existing : sanctuaries) {
            if (existing.dimension == zone.dimension && 
                existing.center.equals(zone.center)) {
                return; // 已存在，不重复添加
            }
        }
        sanctuaries.add(zone);
        markDirty();
        Adversity.LOGGER.info("Sanctuary added at {} in dimension {} (type={}, radius={})",
            zone.center, zone.dimension, zone.type, zone.radius);
    }

    /**
     * 移除圣所
     */
    public boolean removeSanctuary(int dimension, BlockPos center) {
        Iterator<SanctuaryZone> it = sanctuaries.iterator();
        while (it.hasNext()) {
            SanctuaryZone zone = it.next();
            if (zone.dimension == dimension && zone.center.equals(center)) {
                it.remove();
                markDirty();
                Adversity.LOGGER.info("Sanctuary removed at {} in dimension {}", center, dimension);
                return true;
            }
        }
        return false;
    }

    /**
     * 获取位置所在的圣所（如果有）
     */
    public SanctuaryZone getSanctuaryAt(int dimension, BlockPos pos) {
        for (SanctuaryZone zone : sanctuaries) {
            if (zone.dimension == dimension && zone.contains(pos)) {
                return zone;
            }
        }
        return null;
    }

    /**
     * 检查位置是否在任何圣所内
     */
    public boolean isInSanctuary(int dimension, BlockPos pos) {
        return getSanctuaryAt(dimension, pos) != null;
    }

    /**
     * 获取指定维度的所有圣所
     */
    public List<SanctuaryZone> getSanctuariesInDimension(int dimension) {
        List<SanctuaryZone> result = new ArrayList<>();
        for (SanctuaryZone zone : sanctuaries) {
            if (zone.dimension == dimension) {
                result.add(zone);
            }
        }
        return result;
    }

    /**
     * 获取所有圣所
     */
    public List<SanctuaryZone> getAllSanctuaries() {
        return new ArrayList<>(sanctuaries);
    }

    /**
     * 消耗指定圣所的燃料
     * @return 剩余燃料，如果圣所不存在返回-1
     */
    public int consumeFuel(int dimension, BlockPos center, int amount) {
        for (SanctuaryZone zone : sanctuaries) {
            if (zone.dimension == dimension && zone.center.equals(center)) {
                zone.fuel = Math.max(0, zone.fuel - amount);
                markDirty();
                return zone.fuel;
            }
        }
        return -1;
    }

    /**
     * 添加燃料到指定圣所
     */
    public int addFuel(int dimension, BlockPos center, int amount) {
        for (SanctuaryZone zone : sanctuaries) {
            if (zone.dimension == dimension && zone.center.equals(center)) {
                zone.fuel = Math.min(zone.maxFuel, zone.fuel + amount);
                markDirty();
                return zone.fuel;
            }
        }
        return -1;
    }

    // ==================== NBT 序列化 ====================

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        sanctuaries.clear();

        NBTTagList list = nbt.getTagList("sanctuaries", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound zoneNbt = list.getCompoundTagAt(i);
            SanctuaryZone zone = SanctuaryZone.fromNBT(zoneNbt);
            if (zone != null) {
                sanctuaries.add(zone);
            }
        }

        Adversity.LOGGER.debug("Loaded {} sanctuaries from NBT", sanctuaries.size());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (SanctuaryZone zone : sanctuaries) {
            list.appendTag(zone.toNBT());
        }
        nbt.setTag("sanctuaries", list);
        return nbt;
    }
}
