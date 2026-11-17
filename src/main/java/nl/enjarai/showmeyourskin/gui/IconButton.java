package nl.enjarai.showmeyourskin.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A 20x20 vanilla button with one of our icon sprites drawn on top.
 */
public class IconButton extends AbstractButton {
  public static final int SIZE = 20;

  private final Supplier<Identifier> icon;
  private final Runnable onPress;

  public IconButton(int x, int y, Component message, Supplier<Identifier> icon, Runnable onPress) {
    super(x, y, SIZE, SIZE, message);
    this.icon = icon;
    this.onPress = onPress;
    setTooltip(Tooltip.create(message));
  }

  /**
   * A button that flips a boolean, showing {@code button/<name>} when on and {@code button/<name>_disabled} when off.
   */
  public static IconButton toggle(int x, int y, Component message, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
    var on = ShowMeYourSkin.id("button/" + name);
    var off = ShowMeYourSkin.id("button/" + name + "_disabled");
    return new IconButton(x, y, message, () -> getter.getAsBoolean() ? on : off, () -> setter.accept(!getter.getAsBoolean()));
  }

  @Override
  public void onPress(InputWithModifiers input) {
    onPress.run();
  }

  @Override
  protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    extractDefaultSprite(graphics);
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.get(), getX(), getY(), SIZE, SIZE);
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput output) {
    defaultButtonNarrationText(output);
  }
}
