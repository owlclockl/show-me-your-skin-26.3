package nl.enjarai.showmeyourskin.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;
import nl.enjarai.showmeyourskin.gui.ArmorConfigScreen;
import nl.enjarai.showmeyourskin.gui.IconButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
  @Shadow
  public int width;

  @Shadow
  public int height;

  @Inject(method = "init", at = @At("TAIL"))
  private void showMeYourSkin$addSettingsButton(CallbackInfo ci) {
    Screen screen = (Screen) (Object) this;
    var iconRows = screen.children().stream()
      .filter(AbstractWidget.class::isInstance)
      .map(AbstractWidget.class::cast)
      .filter(widget -> widget.getWidth() == IconButton.SIZE && widget.getHeight() == IconButton.SIZE)
      .filter(widget -> widget.getY() > height / 2 && widget.getY() < height - IconButton.SIZE * 2)
      .collect(Collectors.groupingBy(AbstractWidget::getY));

    List<AbstractWidget> iconRow = iconRows.values().stream()
      .filter(row -> row.size() >= 3)
      .max(Comparator.comparingInt(List::size))
      .orElse(List.of());

    int x;
    int y;
    if (iconRow.isEmpty()) {
      x = width / 2 + 60;
      y = height / 2 + 18;
    } else {
      iconRow.sort(Comparator.comparingInt(AbstractWidget::getX));
      int shift = (IconButton.SIZE + 4) / 2;
      iconRow.forEach(widget -> widget.setX(widget.getX() - shift));
      AbstractWidget lastButton = iconRow.get(iconRow.size() - 1);
      x = lastButton.getX() + lastButton.getWidth() + 4;
      y = lastButton.getY();
    }

    if (x + IconButton.SIZE > width - 4) {
      x = width - IconButton.SIZE - 4;
    }

    ((ScreenAccessor) (Object) screen).showMeYourSkin$addRenderableWidget(new IconButton(
      x,
      y,
      Component.translatable("key.showmeyourskin.open_settings"),
      () -> ShowMeYourSkin.id("button/armor"),
      () -> Minecraft.getInstance().gui.setScreen(ArmorConfigScreen.createConfigScreen(screen))
    ));
  }
}
