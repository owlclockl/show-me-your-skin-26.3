package nl.enjarai.showmeyourskin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.Mannequin;
import org.jetbrains.annotations.Nullable;

/**
 * Shows a mannequin wearing the selected player's armor, rendered with the config being edited.
 */
public class PreviewWidget extends AbstractWidget {
  // Show the preview anyway if the skin hasn't loaded after this many ticks (e.g. offline accounts)
  private static final int SKIN_TIMEOUT_TICKS = 40;

  private PlayerOption option;
  private @Nullable Mannequin mannequin;
  private int ticksWaited;

  public PreviewWidget(PlayerOption option) {
    super(0, 0, 0, 0, Component.empty());
    setOption(option);
  }

  public void setOption(PlayerOption option) {
    this.option = option;
    this.mannequin = option.constructMannequin();
    this.ticksWaited = 0;
    tick();
  }

  public void tick() {
    if (mannequin != null) {
      mannequin.tick();
      ticksWaited++;
    }
  }

  private boolean isReady() {
    return mannequin != null &&
      (((ClientMannequin) mannequin).getSkin() != ClientMannequin.DEFAULT_SKIN || ticksWaited >= SKIN_TIMEOUT_TICKS);
  }

  @Override
  protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    var font = Minecraft.getInstance().font;
    int x = getX(), y = getY(), w = getWidth(), h = getHeight();

    Panels.darkInset(graphics, x, y, w, h);

    if (mannequin == null) {
      var text = Component.translatable("gui.showmeyourskin.armorScreen.previewUnavailable");
      var lines = font.split(text, w - 8);
      int textY = y + (h - lines.size() * font.lineHeight) / 2;
      for (var line : lines) {
        graphics.text(font, line, x + (w - font.width(line)) / 2, textY, 0xFF555555, false);
        textY += font.lineHeight;
      }
    } else if (isReady()) {
      int size = (int) Math.min(w * 0.55f, (h - 16) / 2.2f);
      InventoryScreen.extractEntityInInventoryFollowsMouse(
        graphics, x + 1, y + 12, x + w - 1, y + h - 1, size, 0.0625f, mouseX, mouseY, mannequin
      );
    }

    graphics.text(font, option.title(), x + 4, y + 4, 0xFFAAAAAA, false);
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput output) {
  }
}
