package com.adversity.capability.talisman;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 护符盒能力提供者
 */
public class TalismanCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {

    @CapabilityInject(ITalismanCapability.class)
    public static Capability<ITalismanCapability> TALISMAN_CAPABILITY = null;

    private final ITalismanCapability instance = new TalismanCapability();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == TALISMAN_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == TALISMAN_CAPABILITY) {
            return TALISMAN_CAPABILITY.cast(instance);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        instance.deserializeNBT(nbt);
    }
}
