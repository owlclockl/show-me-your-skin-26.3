package nl.enjarai.showmeyourskin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;
import nl.enjarai.showmeyourskin.mixin.ScreenAccessor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Adds the mod's config icon to an existing row of 20x20 menu icon buttons. */
public final class ConfigScreenMenuButton {
  private ConfigScreenMenuButton() {
  }

  public static void addTo(Screen screen) {
    ScreenAccessor screenAccessor = (ScreenAccessor) (Object) screen;
    int screenWidth = screenAccessor.showMeYourSkin$getWidth();
    int screenHeight = screenAccessor.showMeYourSkin$getHeight();

    var iconRows = screen.children().stream()
      .filter(AbstractWidget.class::isInstance)
      .map(AbstractWidget.class::cast)
      .filter(widget -> widget.getWidth() == IconButton.SIZE && widget.getHeight() == IconButton.SIZE)
      .filter(widget -> widget.getY() < screenHeight - IconButton.SIZE * 2)
      .collect(Collectors.groupingBy(AbstractWidget::getY));

    List<AbstractWidget> iconRow = iconRows.values().stream()
      .filter(row -> row.size() >= 3)
      .max(Comparator.comparingInt(List::size))
      .orElse(List.of());

    int x;
    int y;
    if (iconRow.isEmpty()) {
      x = screenWidth / 2 + 60;
      y = screenHeight / 2 + 18;
    } else {
      iconRow.sort(Comparator.comparingInt(AbstractWidget::getX));
      int shift = (IconButton.SIZE + 4) / 2;
      iconRow.forEach(widget -> widget.setX(widget.getX() - shift));
      AbstractWidget lastButton = iconRow.get(iconRow.size() - 1);
      x = lastButton.getX() + lastButton.getWidth() + 4;
      y = lastButton.getY();
    }

    if (x + IconButton.SIZE > screenWidth - 4) {
      x = screenWidth - IconButton.SIZE - 4;
    }

    screenAccessor.showMeYourSkin$addRenderableWidget(new IconButton(
      x,
      y,
      Component.translatable("key.showmeyourskin.open_settings"),
      () -> ShowMeYourSkin.id("button/armor"),
      () -> Minecraft.getInstance().gui.setScreen(ArmorConfigScreen.createConfigScreen(screen))
    ));
  }
}
