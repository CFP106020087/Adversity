package com.adversity.network;

import com.adversity.Adversity;
import com.adversity.sanctuary.StageGatingRegistry;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Client → Server: 保存附魔門控配置
 *
 * 接收: stage, useBlacklist, enchantId[]
 * 處理: 1) 更新 StageGatingRegistry  2) 寫 JSON  3) 寫 ZS
 */
public class PacketSaveEnchantGating implements IMessage {

    private String stage;
    private boolean useBlacklist;
    private String[] enchantIds;

    public PacketSaveEnchantGating() {}

    public PacketSaveEnchantGating(String stage, boolean useBlacklist, String[] enchantIds) {
        this.stage = stage;
        this.useBlacklist = useBlacklist;
        this.enchantIds = enchantIds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stage = ByteBufUtils.readUTF8String(buf);
        useBlacklist = buf.readBoolean();
        int count = buf.readInt();
        enchantIds = new String[count];
        for (int i = 0; i < count; i++) {
            enchantIds[i] = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, stage);
        buf.writeBoolean(useBlacklist);
        buf.writeInt(enchantIds.length);
        for (String id : enchantIds) {
            ByteBufUtils.writeUTF8String(buf, id);
        }
    }

    public static class Handler implements IMessageHandler<PacketSaveEnchantGating, IMessage> {
        @Override
        public IMessage onMessage(PacketSaveEnchantGating msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 只有創造模式玩家可用
            if (!player.isCreative()) {
                Adversity.LOGGER.warn("[Adversity] Non-creative player tried to save enchant gating: {}", player.getName());
                return null;
            }

            player.getServerWorld().addScheduledTask(() -> {
                try {
                    // 1. 更新 Registry
                    Set<ResourceLocation> set = new HashSet<>();
                    for (String id : msg.enchantIds) {
                        set.add(new ResourceLocation(id));
                    }
                    StageGatingRegistry.setEnchantmentGating(true, msg.stage, msg.useBlacklist, set);

                    // 2. 寫 JSON (stage_gating.json 的 enchant_gating 區段)
                    writeJson(msg.stage, msg.useBlacklist, msg.enchantIds);

                    // 3. 寫 ZS
                    writeZenScript(msg.stage, msg.useBlacklist, msg.enchantIds);

                    Adversity.LOGGER.info("[Adversity] Enchant gating saved by {}: stage={}, blacklist={}, entries={}",
                            player.getName(), msg.stage, msg.useBlacklist, msg.enchantIds.length);
                } catch (Exception e) {
                    Adversity.LOGGER.error("[Adversity] Failed to save enchant gating", e);
                }
            });

            return null;
        }

        private void writeJson(String stage, boolean useBlacklist, String[] ids) {
            File configDir = new File(Loader.instance().getConfigDir(), "adversity");
            if (!configDir.exists()) configDir.mkdirs();

            // 讀取現有 stage_gating.json，更新 enchant_gating 區段
            File file = new File(configDir, "stage_gating.json");
            com.google.gson.Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root;

            if (file.exists()) {
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    root = gson.fromJson(reader, JsonObject.class);
                    if (root == null) root = new JsonObject();
                } catch (Exception e) {
                    root = new JsonObject();
                }
            } else {
                root = new JsonObject();
            }

            // 更新 enchant_gating 區段
            JsonObject gating = new JsonObject();
            gating.addProperty("enabled", true);
            gating.addProperty("required_stage", stage);
            gating.addProperty("use_blacklist", useBlacklist);
            JsonArray list = new JsonArray();
            for (String id : ids) {
                list.add(id);
            }
            gating.add("enchantment_list", list);
            root.add("enchant_gating", gating);

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            } catch (IOException e) {
                Adversity.LOGGER.error("[Adversity] Failed to write stage_gating.json", e);
            }
        }

        private void writeZenScript(String stage, boolean useBlacklist, String[] ids) {
            File configDir = new File(Loader.instance().getConfigDir(), "adversity");
            File genDir = new File(configDir, "generated");
            if (!genDir.exists()) genDir.mkdirs();

            File zsFile = new File(genDir, "enchant_gating.zs");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(zsFile), StandardCharsets.UTF_8)) {
                writer.write("// === 自動生成 by Adversity Enchant Gating Tool ===\n");
                writer.write("// 模式: " + (useBlacklist ? "黑名單" : "白名單") + " | 階段: " + stage + "\n");
                writer.write("import mods.adversity.Sanctuary;\n\n");
                writer.write("Sanctuary.setEnchantmentGating(\"" + stage + "\", " + useBlacklist + ", [\n");
                for (int i = 0; i < ids.length; i++) {
                    writer.write("    \"" + ids[i] + "\"");
                    if (i < ids.length - 1) writer.write(",");
                    writer.write("\n");
                }
                writer.write("]);\n");
            } catch (IOException e) {
                Adversity.LOGGER.error("[Adversity] Failed to write enchant_gating.zs", e);
            }
        }
    }
}
