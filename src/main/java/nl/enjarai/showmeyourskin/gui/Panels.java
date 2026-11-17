package nl.enjarai.showmeyourskin.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws the vanilla-style panel backgrounds used by the config screen.
 */
public final class Panels {
  private Panels() {
  }

  /**
   * The light grey, black-bordered panel used by vanilla container screens.
   */
  public static void light(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    // Black outline with clipped corners
    graphics.fill(x + 1, y, x + width - 1, y + height, 0xFF000000);
    graphics.fill(x, y + 1, x + width, y + height - 1, 0xFF000000);
    // Bevel
    graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF555555);
    graphics.fill(x + 1, y + 1, x + width - 2, y + height - 2, 0xFFFFFFFF);
    graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFC6C6C6);
  }

  /**
   * The sunken grey area vanilla uses for slots.
   */
  public static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    graphics.fill(x, y, x + width, y + height, 0xFFFFFFFF);
    graphics.fill(x, y, x + width - 1, y + height - 1, 0xFF373737);
    graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
  }

  /**
   * A darker sunken area, used behind the player preview.
   */
  public static void darkInset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    graphics.fill(x, y, x + width, y + height, 0xFFFFFFFF);
    graphics.fill(x, y, x + width - 1, y + height - 1, 0xFF373737);
    graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF1E1E1E);
  }
}
