package com.cstestcs.bioniclemasks.client;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.forge.MaskForgeLogic;
import com.cstestcs.bioniclemasks.forge.MaskForgeMenu;
import com.cstestcs.bioniclemasks.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class MaskForgeScreen extends AbstractContainerScreen<MaskForgeMenu> {
    private static final Identifier BACKGROUND = BionicleMasks.id("textures/gui/mask_forge.png");
    private Button forgeButton;
    private String lastPreview = "";

    public MaskForgeScreen(MaskForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
    }

    @Override
    protected void init() {
        super.init();
        forgeButton = addRenderableWidget(Button.builder(Component.translatable("gui.bioniclemasks.forge"),
                        b -> ClientPacketDistributor.sendToServer(new ModNetwork.ForgeButton(menu.containerId, 0)))
                .bounds(leftPos + 104, topPos + 62, 62, 18)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        // The button's tooltip previews what the forge will make.
        MaskForgeLogic.Plan plan = menu.preview();
        if (forgeButton != null && !plan.message().equals(lastPreview)) {
            lastPreview = plan.message();
            forgeButton.setTooltip(Tooltip.create(Component.literal(plan.message())));
        }
        int color = plan.output().isEmpty() ? 0xFF8B4040 : 0xFF40A040;
        graphics.fill(leftPos + 104, topPos + 82, leftPos + 166, topPos + 84, color);
    }
}
