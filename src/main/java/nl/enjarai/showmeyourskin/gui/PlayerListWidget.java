package nl.enjarai.showmeyourskin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;
import nl.enjarai.showmeyourskin.config.ModConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The panel on the left of the local config screen: the global entry, then a scrolling list of players.
 */
public class PlayerListWidget extends AbstractWidget {
  private static final int PADDING = 6;
  private static final int ROW_HEIGHT = 20;
  private static final int DELETE_SIZE = 11;
  private static final int SCROLLBAR_WIDTH = 4;

  private final PlayerOption globalOption;
  private final List<PlayerOption> options;
  private final Supplier<PlayerOption> selected;
  private final Consumer<PlayerOption> onSelect;
  private final Consumer<PlayerOption> onDelete;
  private final Map<UUID, PlayerSkin> skins = new ConcurrentHashMap<>();
  private double scroll;

  public PlayerListWidget(PlayerOption globalOption, List<PlayerOption> options, Supplier<PlayerOption> selected,
                          Consumer<PlayerOption> onSelect, Consumer<PlayerOption> onDelete) {
    super(0, 0, 0, 0, Component.translatable("gui.showmeyourskin.armorScreen.playerSelector"));
    this.globalOption = globalOption;
    this.options = options;
    this.selected = selected;
    this.onSelect = onSelect;
    this.onDelete = onDelete;

    options.forEach(this::loadSkin);
  }

  private void loadSkin(PlayerOption option) {
    if (option.player() != null) {
      skins.put(option.uuid(), option.player().getSkin());
      return;
    }

    var mc = Minecraft.getInstance();
    CompletableFuture
      .supplyAsync(() -> mc.services().profileResolver().fetchById(option.uuid()))
      .thenCompose(profile -> profile.map(p -> mc.getSkinManager().get(p)).orElse(CompletableFuture.completedFuture(java.util.Optional.empty())))
      .thenAccept(skin -> skin.ifPresent(s -> mc.execute(() -> skins.put(option.uuid(), s))));
  }

  // Layout helpers

  private int globalTop() {
    return getY() + PADDING;
  }

  private int listLeft() {
    return getX() + PADDING;
  }

  private int listTop() {
    return globalTop() + ROW_HEIGHT + 2 + 4;
  }

  private int listRight() {
    return getX() + getWidth() - PADDING;
  }

  private int listBottom() {
    return getY() + getHeight() - PADDING;
  }

  private int visibleHeight() {
    return listBottom() - listTop() - 2;
  }

  private int maxScroll() {
    return Math.max(0, options.size() * ROW_HEIGHT - visibleHeight());
  }

  private int rowWidth() {
    return listRight() - listLeft() - 2 - (maxScroll() > 0 ? SCROLLBAR_WIDTH + 1 : 0);
  }

  @Override
  protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    Panels.light(graphics, getX(), getY(), getWidth(), getHeight());

    // Global entry
    Panels.inset(graphics, listLeft(), globalTop(), listRight() - listLeft(), ROW_HEIGHT + 2);
    extractRow(graphics, globalOption, listLeft() + 1, globalTop() + 1, listRight() - listLeft() - 2, mouseX, mouseY, false);

    // Player list
    Panels.inset(graphics, listLeft(), listTop(), listRight() - listLeft(), listBottom() - listTop());
    graphics.enableScissor(listLeft() + 1, listTop() + 1, listRight() - 1, listBottom() - 1);
    boolean mouseInList = mouseY >= listTop() + 1 && mouseY < listBottom() - 1;
    for (int i = 0; i < options.size(); i++) {
      int rowY = listTop() + 1 + i * ROW_HEIGHT - (int) scroll;
      if (rowY + ROW_HEIGHT < listTop() || rowY > listBottom()) continue;
      var option = options.get(i);
      extractRow(graphics, option, listLeft() + 1, rowY, rowWidth(), mouseInList ? mouseX : -1, mouseInList ? mouseY : -1,
        ModConfig.INSTANCE.overrides.containsKey(option.uuid()));
    }
    graphics.disableScissor();

    // Scrollbar
    if (maxScroll() > 0) {
      int trackX = listRight() - 1 - SCROLLBAR_WIDTH;
      int trackTop = listTop() + 1;
      int trackHeight = visibleHeight();
      int thumbHeight = Math.max(8, trackHeight * trackHeight / (options.size() * ROW_HEIGHT));
      int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll()));
      graphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackTop + trackHeight, 0xFF373737);
      graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFC6C6C6);
      graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight - 1, 0xFFFFFFFF);
    }
  }

  private void extractRow(GuiGraphicsExtractor graphics, PlayerOption option, int x, int y, int width,
                          int mouseX, int mouseY, boolean deletable) {
    var font = Minecraft.getInstance().font;
    boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT;

    if (option == selected.get()) {
      graphics.fill(x, y, x + width, y + ROW_HEIGHT, 0xFF4A4A4A);
    } else if (hovered) {
      graphics.fill(x, y, x + width, y + ROW_HEIGHT, 0xFF9D9D9D);
    }

    // Head
    graphics.fill(x + 2, y + 2, x + 18, y + 18, 0xFF161616);
    if (option == globalOption) {
      graphics.blit(RenderPipelines.GUI_TEXTURED, ShowMeYourSkin.id("textures/gui/global_icon.png"), x + 2, y + 2, 0, 0, 16, 16, 16, 16);
    } else {
      var skin = skins.get(option.uuid());
      if (skin != null) {
        PlayerFaceExtractor.extractRenderState(graphics, skin, x + 2, y + 2, 16);
      }
    }

    // Name
    int nameWidth = width - 24 - (deletable ? DELETE_SIZE + 2 : 0);
    var name = Language.getInstance().getVisualOrder(font.substrByWidth(option.title(), nameWidth));
    graphics.text(font, name, x + 22, y + (ROW_HEIGHT - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);

    // Delete button
    if (deletable) {
      int dx = x + width - DELETE_SIZE - 2;
      int dy = y + (ROW_HEIGHT - DELETE_SIZE) / 2;
      boolean deleteHovered = mouseX >= dx && mouseX < dx + DELETE_SIZE && mouseY >= dy && mouseY < dy + DELETE_SIZE;
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
        ShowMeYourSkin.id(deleteHovered ? "button/delete_override_highlighted" : "button/delete_override"),
        dx, dy, DELETE_SIZE, DELETE_SIZE);
      if (deleteHovered) {
        graphics.setTooltipForNextFrame(font, Component.translatable("gui.showmeyourskin.armorScreen.deleteButtonTooltip"), mouseX, mouseY);
      }
    }
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (!isMouseOver(event.x(), event.y()) || !isValidClickButton(event.buttonInfo())) {
      return false;
    }

    var option = optionAt(event.x(), event.y());
    if (option == null) {
      return false;
    }

    playButtonClickSound(Minecraft.getInstance().getSoundManager());

    if (option != globalOption && ModConfig.INSTANCE.overrides.containsKey(option.uuid())) {
      int dx = listLeft() + 1 + rowWidth() - DELETE_SIZE - 2;
      if (event.x() >= dx && event.x() < dx + DELETE_SIZE) {
        onDelete.accept(option);
        return true;
      }
    }

    onSelect.accept(option);
    return true;
  }

  private @Nullable PlayerOption optionAt(double mouseX, double mouseY) {
    if (mouseX < listLeft() + 1 || mouseX >= listRight() - 1) {
      return null;
    }

    if (mouseY >= globalTop() + 1 && mouseY < globalTop() + 1 + ROW_HEIGHT) {
      return globalOption;
    }

    if (mouseY >= listTop() + 1 && mouseY < listBottom() - 1 && mouseX < listLeft() + 1 + rowWidth()) {
      int index = (int) ((mouseY - listTop() - 1 + scroll) / ROW_HEIGHT);
      if (index >= 0 && index < options.size()) {
        return options.get(index);
      }
    }

    return null;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
    if (!isMouseOver(mouseX, mouseY) || maxScroll() == 0) {
      return false;
    }

    scroll = Mth.clamp(scroll - vertical * ROW_HEIGHT, 0, maxScroll());
    return true;
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput output) {
    defaultButtonNarrationText(output);
  }
}
