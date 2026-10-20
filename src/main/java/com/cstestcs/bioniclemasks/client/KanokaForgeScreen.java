package com.cstestcs.bioniclemasks.client;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.forge.KanokaForgeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class KanokaForgeScreen extends AbstractContainerScreen<KanokaForgeMenu> {
    private static final Identifier BACKGROUND = BionicleMasks.id("textures/gui/kanoka_forge.png");

    public KanokaForgeScreen(KanokaForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
