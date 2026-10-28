package dev.nova;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class NovaScreen extends Screen {
    public NovaScreen() {
        super(Text.literal("NOVA"));
    }

    @Override
    protected void init() {
        int left = (this.width - 300) / 2;
        int top = (this.height - 220) / 2;

        addDrawableChild(ButtonWidget.builder(
                storageButtonText(),
                button -> {
                    StorageFinder.toggle();
                    button.setMessage(storageButtonText());
                }
        ).dimensions(left + 20, top + 65, 260, 22).build());

        addDrawableChild(ButtonWidget.builder(
                freecamButtonText(),
                button -> {
                    FreecamController.toggle(client);
                    button.setMessage(freecamButtonText());
                }
        ).dimensions(left + 20, top + 95, 260, 22).build());
    }

    private static Text storageButtonText() {
        return Text.literal("StorageFinder: "
                + (StorageFinder.enabled ? "ON" : "OFF"));
    }

    private static Text freecamButtonText() {
        return Text.literal("Freecam: "
                + (FreecamController.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int panelWidth = 300;
        int panelHeight = 220;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        context.fill(0, 0, this.width, this.height, 0x9906000D);
        context.fill(left, top, left + panelWidth,
                top + panelHeight, 0xFF100B18);
        context.fill(left, top, left + panelWidth,
                top + 3, 0xFF9B5CFF);

        context.drawText(this.textRenderer, "N O V A",
                left + 18, top + 16, 0xFFD7B8FF, false);
        context.drawText(this.textRenderer, "CLIENT MENU",
                left + 18, top + 34, 0xFF9A8AAE, false);

        context.fill(left + 16, top + 55,
                left + panelWidth - 16, top + 56, 0xFF39264E);

        context.drawText(this.textRenderer, "Freecam controls: F6",
                left + 20, top + 130, 0xFFB9ADC8, false);
        context.drawText(this.textRenderer,
                "WASD move, Space up, Shift down",
                left + 20, top + 146, 0xFFB9ADC8, false);
        context.drawText(this.textRenderer,
                "Sprint key moves faster",
                left + 20, top + 162, 0xFFB9ADC8, false);
        context.drawText(this.textRenderer,
                "Right Shift / Esc to close",
                left + 18, top + panelHeight - 22,
                0xFF9A8AAE, false);

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
