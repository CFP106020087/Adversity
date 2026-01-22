package com.adversity.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * ASM Transformer for GuiContainer.drawSlot
 * Injects a hook to prevent rendering items in sealed slots
 */
public class GuiContainerTransformer implements IClassTransformer {

    private static final String GUI_CONTAINER_CLASS = "net.minecraft.client.gui.inventory.GuiContainer";
    private static final String GUI_CONTAINER_CLASS_OBF = "bjs";  // 1.12.2 obfuscated

    // drawSlot(Slot slot) - void
    private static final String DRAW_SLOT_METHOD = "drawSlot";
    private static final String DRAW_SLOT_METHOD_OBF = "func_146977_a";
    private static final String DRAW_SLOT_METHOD_NOTCH = "a";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (!transformedName.equals(GUI_CONTAINER_CLASS)) {
            return basicClass;
        }

        System.out.println("[Adversity ASM] Transforming GuiContainer class: " + name);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            boolean transformed = false;

            for (MethodNode method : classNode.methods) {
                if (isDrawSlotMethod(method)) {
                    System.out.println("[Adversity ASM] Found drawSlot method: " + method.name + method.desc);
                    transformDrawSlot(method);
                    transformed = true;
                    break;
                }
            }

            if (transformed) {
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(classWriter);
                System.out.println("[Adversity ASM] Successfully transformed GuiContainer.drawSlot");
                return classWriter.toByteArray();
            }

        } catch (Exception e) {
            System.err.println("[Adversity ASM] Failed to transform GuiContainer: " + e.getMessage());
            e.printStackTrace();
        }

        return basicClass;
    }

    private boolean isDrawSlotMethod(MethodNode method) {
        if (!method.name.equals(DRAW_SLOT_METHOD) &&
            !method.name.equals(DRAW_SLOT_METHOD_OBF) &&
            !method.name.equals(DRAW_SLOT_METHOD_NOTCH)) {
            return false;
        }
        // Check if it takes a Slot parameter and returns void
        return method.desc.contains("Slot") || method.desc.contains("Lbgi;");
    }

    private void transformDrawSlot(MethodNode method) {
        // Inject at the beginning:
        // if (DrawSlotHook.shouldSkipSlot(slot)) return;

        InsnList injection = new InsnList();

        // Load slot argument (index 1)
        injection.add(new VarInsnNode(Opcodes.ALOAD, 1));

        // Call hook method
        injection.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/adversity/asm/DrawSlotHook",
            "shouldSkipSlot",
            "(Lnet/minecraft/inventory/Slot;)Z",
            false
        ));

        // If true, return early
        LabelNode continueLabel = new LabelNode();
        injection.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        injection.add(new InsnNode(Opcodes.RETURN));
        injection.add(continueLabel);

        // Insert at beginning
        method.instructions.insert(injection);

        System.out.println("[Adversity ASM] Injected hook into drawSlot");
    }
}
