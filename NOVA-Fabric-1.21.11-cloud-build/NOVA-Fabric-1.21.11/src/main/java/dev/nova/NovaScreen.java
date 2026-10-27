package dev.nova;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class NovaScreen extends Screen {
    public NovaScreen() {
        super(Text.literal("NOVA"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = 300;
        int panelHeight = 190;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        context.fill(0, 0, this.width, this.height, 0x9906000D);
        context.fill(left, top, left + panelWidth, top + panelHeight, 0xFF100B18);
        context.fill(left, top, left + panelWidth, top + 3, 0xFF9B5CFF);
        context.drawText(this.textRenderer, "N O V A", left + 18, top + 16, 0xFFD7B8FF, false);
        context.drawText(this.textRenderer, "CLIENT MENU", left + 18, top + 34, 0xFF9A8AAE, false);
        context.fill(left + 16, top + 55, left + panelWidth - 16, top + 56, 0xFF39264E);
        context.drawText(this.textRenderer, "StorageFinder", left + 18, top + 70, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "Feature scaffold - rendering still in progress", left + 18, top + 86, 0xFFB9ADC8, false);
        context.drawText(this.textRenderer, "Freecam", left + 18, top + 112, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "Feature scaffold - camera logic still in progress", left + 18, top + 128, 0xFFB9ADC8, false);
        context.drawText(this.textRenderer, "Right Shift / Esc to close", left + 18, top + panelHeight - 22, 0xFF9A8AAE, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        NovaClient.guiEnabled = false;
        super.close();
    }
}
