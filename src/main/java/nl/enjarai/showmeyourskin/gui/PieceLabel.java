package nl.enjarai.showmeyourskin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * The name of an armor piece. It underlines on hover, and clicking it toggles the whole piece.
 */
public class PieceLabel extends AbstractButton {
  private final Runnable onPress;

  public PieceLabel(int x, int y, int width, int height, Component message, Runnable onPress) {
    super(x, y, width, height, message);
    this.onPress = onPress;
  }

  @Override
  public void onPress(InputWithModifiers input) {
    onPress.run();
  }

  @Override
  protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    var font = Minecraft.getInstance().font;
    var text = getMessage().copy().withStyle(style -> style.withUnderlined(isHoveredOrFocused()));
    var clipped = Language.getInstance().getVisualOrder(font.substrByWidth(text, getWidth()));
    graphics.text(font, clipped, getX(), getY() + (getHeight() - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput output) {
    defaultButtonNarrationText(output);
  }
}
