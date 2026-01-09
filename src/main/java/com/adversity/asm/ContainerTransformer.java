package com.adversity.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

/**
 * ASM Transformer for Container.slotClick
 * Injects a hook at the beginning of slotClick to check for sealed slots
 */
public class ContainerTransformer implements IClassTransformer {

    // Obfuscated and deobfuscated names
    private static final String CONTAINER_CLASS = "net.minecraft.inventory.Container";
    private static final String CONTAINER_CLASS_OBF = "aec";  // 1.12.2 obfuscated name

    private static final String SLOT_CLICK_METHOD = "slotClick";
    private static final String SLOT_CLICK_METHOD_OBF = "func_184996_a";  // MCP name
    private static final String SLOT_CLICK_METHOD_NOTCH = "a";  // Notch name

    // slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) -> ItemStack
    private static final String SLOT_CLICK_DESC = "(IILnet/minecraft/inventory/ClickType;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;";
    private static final String SLOT_CLICK_DESC_OBF = "(IILadi;Laed;)Laip;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        // Check if this is the Container class
        if (!transformedName.equals(CONTAINER_CLASS)) {
            return basicClass;
        }

        System.out.println("[Adversity ASM] Transforming Container class: " + name);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            boolean transformed = false;

            for (MethodNode method : classNode.methods) {
                // Check for slotClick method (try multiple names)
                if (isSlotClickMethod(method)) {
                    System.out.println("[Adversity ASM] Found slotClick method: " + method.name + method.desc);
                    transformSlotClick(method);
                    transformed = true;
                    break;
                }
            }

            if (transformed) {
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(classWriter);
                System.out.println("[Adversity ASM] Successfully transformed Container.slotClick");
                return classWriter.toByteArray();
            }

        } catch (Exception e) {
            System.err.println("[Adversity ASM] Failed to transform Container: " + e.getMessage());
            e.printStackTrace();
        }

        return basicClass;
    }

    private boolean isSlotClickMethod(MethodNode method) {
        // Check method name
        if (!method.name.equals(SLOT_CLICK_METHOD) &&
            !method.name.equals(SLOT_CLICK_METHOD_OBF) &&
            !method.name.equals(SLOT_CLICK_METHOD_NOTCH)) {
            return false;
        }

        // Check descriptor pattern: (II...EntityPlayer)ItemStack
        // The descriptor varies based on obfuscation
        return method.desc.startsWith("(II") && method.desc.contains(")L");
    }

    private void transformSlotClick(MethodNode method) {
        // Inject at the beginning of the method:
        // ItemStack result = SlotClickHook.onSlotClick(this, slotId, dragType, clickType, player);
        // if (result != null) return result;

        InsnList injection = new InsnList();

        // Load arguments: this, slotId, dragType, clickType, player
        injection.add(new VarInsnNode(Opcodes.ALOAD, 0));   // this (Container)
        injection.add(new VarInsnNode(Opcodes.ILOAD, 1));   // slotId
        injection.add(new VarInsnNode(Opcodes.ILOAD, 2));   // dragType
        injection.add(new VarInsnNode(Opcodes.ALOAD, 3));   // clickType (ClickType)
        injection.add(new VarInsnNode(Opcodes.ALOAD, 4));   // player (EntityPlayer)

        // Call our hook method - use deobfuscated names since FML remaps at runtime
        injection.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/adversity/asm/SlotClickHook",
            "onSlotClick",
            "(Lnet/minecraft/inventory/Container;IILnet/minecraft/inventory/ClickType;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;",
            false
        ));

        // Store result in a new local variable
        // Find the next available local variable slot
        int resultVar = method.maxLocals;
        injection.add(new VarInsnNode(Opcodes.ASTORE, resultVar));

        // Check if result is not null
        injection.add(new VarInsnNode(Opcodes.ALOAD, resultVar));
        LabelNode continueLabel = new LabelNode();
        injection.add(new JumpInsnNode(Opcodes.IFNULL, continueLabel));

        // If not null, return the result
        injection.add(new VarInsnNode(Opcodes.ALOAD, resultVar));
        injection.add(new InsnNode(Opcodes.ARETURN));

        // Continue with original method
        injection.add(continueLabel);

        // Insert at the beginning of the method
        method.instructions.insert(injection);

        // Increase max locals
        method.maxLocals = resultVar + 1;

        System.out.println("[Adversity ASM] Injected hook into slotClick, maxLocals=" + method.maxLocals);
    }
}
