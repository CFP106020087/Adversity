package com.adversity.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * ASM Transformer for ContainerEnchantment
 * Hooks:
 * - onCraftMatrixChanged: inject/filter Adversity enchantment clues (display)
 * - enchantItem: modify actual enchantment application
 */
public class ContainerEnchantmentTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "net.minecraft.inventory.ContainerEnchantment";

    // onCraftMatrixChanged - 更新附魔显示
    private static final String TARGET_METHOD = "onCraftMatrixChanged";
    private static final String TARGET_METHOD_OBF = "func_75130_a";

    // enchantItem - 实际附魔物品
    private static final String ENCHANT_METHOD = "enchantItem";
    private static final String ENCHANT_METHOD_OBF = "func_75140_a";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null)
            return null;

        if (!transformedName.equals(TARGET_CLASS)) {
            return basicClass;
        }

        System.out.println("[Adversity ASM] Transforming ContainerEnchantment class: " + name);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            boolean transformed = false;

            for (MethodNode method : classNode.methods) {
                // Hook onCraftMatrixChanged
                if (method.name.equals(TARGET_METHOD) || method.name.equals(TARGET_METHOD_OBF)) {
                    if (method.desc.contains("IInventory")) {
                        System.out.println(
                                "[Adversity ASM] Found onCraftMatrixChanged method: " + method.name + method.desc);
                        transformOnCraftMatrixChanged(method);
                        transformed = true;
                    }
                }

                // Hook enchantItem
                if (method.name.equals(ENCHANT_METHOD) || method.name.equals(ENCHANT_METHOD_OBF)) {
                    System.out.println("[Adversity ASM] Found enchantItem method: " + method.name + method.desc);
                    transformEnchantItem(method);
                    transformed = true;
                }
            }

            if (transformed) {
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(classWriter);
                System.out.println("[Adversity ASM] Successfully transformed ContainerEnchantment");
                return classWriter.toByteArray();
            } else {
                System.out.println("[Adversity ASM] WARN: Target methods not found in ContainerEnchantment");
            }

        } catch (Exception e) {
            System.err.println("[Adversity ASM] Failed to transform ContainerEnchantment: " + e.getMessage());
            e.printStackTrace();
        }

        return basicClass;
    }

    private void transformOnCraftMatrixChanged(MethodNode method) {
        // 在每个 RETURN 指令前插入 hook 调用
        InsnList hookCall = new InsnList();
        hookCall.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        hookCall.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "com/adversity/asm/EnchantmentInjectionHook",
                "onEnchantmentUpdate",
                "(Lnet/minecraft/inventory/ContainerEnchantment;)V",
                false));

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                method.instructions.insertBefore(insn, cloneInsnList(hookCall));
            }
        }

        System.out.println("[Adversity ASM] Injected hook into onCraftMatrixChanged");
    }

    private void transformEnchantItem(MethodNode method) {
        // [PRE-HOOK] 在方法开头注入: EnchantmentInjectionHook.onEnchantItemPre
        InsnList preHook = new InsnList();
        preHook.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        preHook.add(new VarInsnNode(Opcodes.ALOAD, 1)); // player
        preHook.add(new VarInsnNode(Opcodes.ILOAD, 2)); // id
        preHook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "com/adversity/asm/EnchantmentInjectionHook",
                "onEnchantItemPre",
                "(Lnet/minecraft/inventory/ContainerEnchantment;Lnet/minecraft/entity/player/EntityPlayer;I)V",
                false));

        method.instructions.insert(preHook);
        System.out.println("[Adversity ASM] Injected PRE-hook into enchantItem");

        // [POST-HOOK] 在每个 IRETURN 指令前注入逻辑
        // Hook 签名: onEnchantItemAfter(boolean success, ContainerEnchantment container,
        // EntityPlayer player, int slotId)

        InsnList postHook = new InsnList();
        // 栈状态假设: [..., result]
        postHook.add(new InsnNode(Opcodes.DUP)); // -> [..., result, result]
        postHook.add(new VarInsnNode(Opcodes.ALOAD, 0)); // -> [..., result, result, this]
        postHook.add(new VarInsnNode(Opcodes.ALOAD, 1)); // -> [..., result, result, this, player]
        postHook.add(new VarInsnNode(Opcodes.ILOAD, 2)); // -> [..., result, result, this, player, id]
        postHook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "com/adversity/asm/EnchantmentInjectionHook",
                "onEnchantItemAfter",
                "(ZLnet/minecraft/inventory/ContainerEnchantment;Lnet/minecraft/entity/player/EntityPlayer;I)V",
                false));
        // -> [..., result]

        // 遍历指令寻找 IRETURN
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.IRETURN) {
                method.instructions.insertBefore(insn, cloneInsnList(postHook));
            }
        }

        System.out.println("[Adversity ASM] Injected POST-hook into enchantItem");
    }

    private InsnList cloneInsnList(InsnList original) {
        InsnList clone = new InsnList();
        for (AbstractInsnNode insn : original.toArray()) {
            clone.add(insn.clone(null));
        }
        return clone;
    }
}
