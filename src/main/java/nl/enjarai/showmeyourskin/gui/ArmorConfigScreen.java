package nl.enjarai.showmeyourskin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import nl.enjarai.showmeyourskin.Components;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;
import nl.enjarai.showmeyourskin.ShowMeYourSkinClient;
import nl.enjarai.showmeyourskin.client.ModKeyBindings;
import nl.enjarai.showmeyourskin.config.ArmorConfig;
import nl.enjarai.showmeyourskin.config.ModConfig;
import nl.enjarai.showmeyourskin.config.SyncedModConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * The armor config screen, built from vanilla widgets.
 * <p>
 * In local mode it shows a player list on the left for per-player overrides.
 * In synced mode (the server has the mod) it edits the config the server shares with everyone,
 * with a row of global toggles on top.
 */
public class ArmorConfigScreen extends Screen {
  private static final int PADDING = 6;
  private static final int GAP = 4;
  private static final int ROW_HEIGHT = 38;
  private static final int CONTROLS_WIDTH = 132;
  private static final int CONTROLS_HEIGHT = 2 + 7 * ROW_HEIGHT + 2;
  private static final int OPTIONS_HEIGHT = 28;
  private static final int PREVIEW_WIDTH = 80;
  private static final int PANEL_WIDTH = PADDING + CONTROLS_WIDTH + GAP + PREVIEW_WIDTH + PADDING;
  private static final int PANEL_HEIGHT = PADDING + CONTROLS_HEIGHT + GAP + OPTIONS_HEIGHT + PADDING;
  private static final int LIST_WIDTH = 100;
  private static final int TOP_BAR_HEIGHT = IconButton.SIZE + GAP;

  private final @Nullable Screen parent;
  private final boolean synced;
  private final @Nullable PlayerOption globalOption;
  private final List<PlayerOption> options;
  private PlayerOption selected;

  private final PreviewWidget preview;
  private final @Nullable PlayerListWidget playerList;

  // Position of the main config panel, set in init()
  private int panelX;
  private int panelY;

  private ArmorConfigScreen(@Nullable Screen parent, boolean synced, PlayerOption selected,
                            @Nullable PlayerOption globalOption, List<PlayerOption> options) {
    super(Component.translatable(synced ? "gui.showmeyourskin.armorScreen.title.serverIntegrated" : "gui.showmeyourskin.armorScreen.title"));
    this.parent = parent;
    this.synced = synced;
    this.selected = selected;
    this.globalOption = globalOption;
    this.options = options;

    this.preview = new PreviewWidget(selected);
    this.playerList = globalOption == null ? null :
      new PlayerListWidget(globalOption, options, () -> this.selected, this::selectOption, this::deleteOption);
  }

  public static Screen createConfigScreen(@Nullable Screen parent) {
    var player = Minecraft.getInstance().player;
    if (ShowMeYourSkinClient.canSyncToServer() && player != null) {
      var component = player.getComponent(Components.ARMOR_CONFIG);
      var option = new PlayerOption(component.getConfig(), player.getUUID(), player, Component.translatable("gui.showmeyourskin.armorScreen.synced"));
      return new ArmorConfigScreen(parent, true, option, null, List.of());
    } else {
      return createLocalConfigScreen(parent);
    }
  }

  public static Screen createLocalConfigScreen(@Nullable Screen parent) {
    var mc = Minecraft.getInstance();
    var player = mc.player;
    var storedOptions = ModConfig.INSTANCE.overrides.entrySet().stream()
      .map(entry -> new PlayerOption(
        entry.getValue().config(),
        entry.getKey(),
        null,
        Component.literal(entry.getValue().title())
      ))
      .toList();

    var options = new ArrayList<PlayerOption>();
    if (player != null) {
      //noinspection resource
      player.connection.getListedOnlinePlayers().stream()
        .map(info -> new PlayerOption(
          ModConfig.INSTANCE.overrides.containsKey(info.getProfile().id()) ?
            ModConfig.INSTANCE.overrides.get(info.getProfile().id()).config() :
            new ArmorConfig(),
          info.getProfile().id(),
          (AbstractClientPlayer) player.level().getPlayerByUUID(info.getProfile().id()),
          Component.literal(info.getProfile().name())
        ))
        .forEach(options::add);
    }
    // Add all stored options that aren't also in the online list
    storedOptions.stream()
      .filter(o -> options.stream().noneMatch(o2 -> o2.uuid().equals(o.uuid())))
      .forEach(options::add);

    var globalOption = new PlayerOption(
      ModConfig.INSTANCE.global, mc.getGameProfile().id(),
      player, Component.translatable("gui.showmeyourskin.armorScreen.global")
    );
    return new ArmorConfigScreen(parent, false, globalOption, globalOption, options);
  }

  @Override
  protected void init() {
    if (synced) {
      panelX = (width - PANEL_WIDTH) / 2;
      panelY = (height - PANEL_HEIGHT - TOP_BAR_HEIGHT) / 2 + TOP_BAR_HEIGHT;
      addTopBar(panelX, panelY - TOP_BAR_HEIGHT);
    } else {
      int totalWidth = LIST_WIDTH + GAP + PANEL_WIDTH;
      int left = (width - totalWidth) / 2;
      panelX = left + LIST_WIDTH + GAP;
      panelY = (height - PANEL_HEIGHT) / 2;

      //noinspection DataFlowIssue
      playerList.setRectangle(LIST_WIDTH, PANEL_HEIGHT, left, panelY);
      addRenderableWidget(playerList);
    }

    addPieceControls(panelX + PADDING, panelY + PADDING);
    addOptionControls(panelX + PADDING, panelY + PADDING + CONTROLS_HEIGHT + GAP);

    preview.setRectangle(PREVIEW_WIDTH, CONTROLS_HEIGHT + GAP + OPTIONS_HEIGHT,
      panelX + PADDING + CONTROLS_WIDTH + GAP, panelY + PADDING);
    addRenderableOnly(preview);
  }

  private void addTopBar(int x, int y) {
    addRenderableWidget(IconButton.toggle(x, y,
      Component.translatable("gui.showmeyourskin.armorScreen.globalToggleTooltip", ModKeyBindings.GLOBAL_TOGGLE.getTranslatedKeyMessage()),
      "enabled",
      () -> ModConfig.INSTANCE.globalEnabled,
      value -> {
        ModConfig.INSTANCE.globalEnabled = value;
        ModConfig.INSTANCE.save();
      }));

    addRenderableWidget(IconButton.toggle(x + IconButton.SIZE + 2, y,
      Component.translatable("gui.showmeyourskin.armorScreen.overridesEnabled"),
      "enable_local_overrides",
      () -> ModConfig.INSTANCE.overridesEnabledInServerMode,
      value -> {
        ModConfig.INSTANCE.overridesEnabledInServerMode = value;
        ModConfig.INSTANCE.save();
        // Shows or hides the button below
        rebuildWidgets();
      }));

    if (ModConfig.INSTANCE.overridesEnabledInServerMode) {
      addRenderableWidget(new IconButton(x + (IconButton.SIZE + 2) * 2, y,
        Component.translatable("gui.showmeyourskin.armorScreen.overridesConfigure"),
        () -> ShowMeYourSkin.id("button/edit_local_overrides"),
        () -> minecraft.gui.setScreen(createLocalConfigScreen(this))));
    }
  }

  private void addPieceControls(int x, int y) {
    var config = selected.armorConfig();
    int rowY = y + 2;

    addArmorPiece(x, rowY, config.head, "head");
    addArmorPiece(x, rowY += ROW_HEIGHT, config.chest, "chest");
    addArmorPiece(x, rowY += ROW_HEIGHT, config.legs, "legs");
    addArmorPiece(x, rowY += ROW_HEIGHT, config.feet, "feet");
    addOtherPiece(x, rowY += ROW_HEIGHT, config.elytra, "elytra");
    addOtherPiece(x, rowY += ROW_HEIGHT, config.shield, "shield");
    addOtherPiece(x, rowY + ROW_HEIGHT, config.hat, "hat");
  }

  /**
   * Button columns: base, trim, glint. Returns the x of the first column.
   */
  private int iconColumn(int x, int column) {
    return x + CONTROLS_WIDTH - 3 - (IconButton.SIZE * 3 + 2) + column * (IconButton.SIZE + 1);
  }

  private void addLabel(int x, int y, String piece, Runnable onPress) {
    int labelX = x + 4;
    addRenderableWidget(new PieceLabel(labelX, y + 1, iconColumn(x, 0) - 3 - labelX, IconButton.SIZE,
      Component.translatable("gui.showmeyourskin.armorScreen.piece." + piece), onPress));
  }

  private void addArmorPiece(int x, int y, ArmorConfig.ArmorPieceConfig piece, String name) {
    addLabel(x, y, name, () -> {
      piece.setAll(!(piece.base() || piece.trim()));
      onConfigChanged();
    });

    addRenderableWidget(IconButton.toggle(iconColumn(x, 0), y + 1,
      Component.translatable("gui.showmeyourskin.armorScreen.piece." + name), "armor",
      piece::base, value -> {
        piece.setBase(value);
        onConfigChanged();
      }));
    addRenderableWidget(IconButton.toggle(iconColumn(x, 1), y + 1,
      Component.translatable("gui.showmeyourskin.armorScreen.piece." + name), "trim",
      piece::trim, value -> {
        piece.setTrim(value);
        onConfigChanged();
      }));
    // Glint has nothing to show on when the rest of the piece is hidden, so turning it on shows the whole piece.
    addRenderableWidget(IconButton.toggle(iconColumn(x, 2), y + 1,
      Component.translatable("gui.showmeyourskin.armorScreen.glintTooltip"), "glint",
      () -> piece.glint() && (piece.base() || piece.trim()), value -> {
        piece.glint = value;
        if (!(piece.base() || piece.trim())) {
          piece.setAll(true);
        }
        onConfigChanged();
      }));

    addOpacitySlider(x, y, piece.opacity, value -> {
      piece.setOpacity(value);
      piece.setTrimOpacity(value);
    });
  }

  private void addOtherPiece(int x, int y, ArmorConfig.PieceConfig piece, String name) {
    addLabel(x, y, name, () -> {
      piece.setAll(!piece.base());
      onConfigChanged();
    });

    addRenderableWidget(IconButton.toggle(iconColumn(x, 0), y + 1,
      Component.translatable("gui.showmeyourskin.armorScreen.piece." + name), name,
      piece::base, value -> {
        piece.setBase(value);
        onConfigChanged();
      }));
    addRenderableWidget(IconButton.toggle(iconColumn(x, 2), y + 1,
      Component.translatable("gui.showmeyourskin.armorScreen.glintTooltip"), "glint",
      () -> piece.glint() && piece.base(), value -> {
        piece.glint = value;
        if (!piece.base()) {
          piece.setAll(true);
        }
        onConfigChanged();
      }));

    addOpacitySlider(x, y, piece.opacity, piece::setOpacity);
  }

  private void addOpacitySlider(int x, int y, int initialOpacity, IntConsumer setOpacity) {
    addRenderableWidget(new OpacitySlider(x + 4, y + 22, CONTROLS_WIDTH - 8, 14, initialOpacity, value -> {
      setOpacity.accept(value);
      onConfigChanged();
    }));
  }

  private static class OpacitySlider extends AbstractSliderButton {
    private final IntConsumer onValueChanged;
    private int lastAppliedValue;

    private OpacitySlider(int x, int y, int width, int height, int initialOpacity, IntConsumer onValueChanged) {
      super(x, y, width, height, Component.empty(), Math.max(0, Math.min(100, initialOpacity)) / 100.0);
      this.onValueChanged = onValueChanged;
      this.lastAppliedValue = getOpacity();
      updateMessage();
    }

    private int getOpacity() {
      return Math.max(0, Math.min(100, (int) Math.round(value * 100)));
    }

    @Override
    protected void updateMessage() {
      setMessage(Component.translatable("gui.showmeyourskin.armorScreen.opacity", getOpacity()));
    }

    @Override
    protected void applyValue() {
      int opacity = getOpacity();
      if (opacity != lastAppliedValue) {
        lastAppliedValue = opacity;
        onValueChanged.accept(opacity);
      }
    }
  }

  private void addOptionControls(int x, int y) {
    var config = selected.armorConfig();
    int buttonX = x + 4;
    int buttonY = y + (OPTIONS_HEIGHT - IconButton.SIZE) / 2;

    if (!synced || serverAllows(SyncedModConfig::allowNotShowInCombat)) {
      addRenderableWidget(IconButton.toggle(buttonX, buttonY,
        Component.translatable("gui.showmeyourskin.armorScreen.combatTooltip"), "show_in_combat",
        () -> config.showInCombat, value -> {
          config.showInCombat = value;
          onConfigChanged();
        }));
      buttonX += IconButton.SIZE + 2;
    }
    if (!synced || serverAllows(SyncedModConfig::allowNotShowNameTag)) {
      addRenderableWidget(IconButton.toggle(buttonX, buttonY,
        Component.translatable("gui.showmeyourskin.armorScreen.nameTagTooltip"), "show_nametag",
        () -> config.showNameTag, value -> {
          config.showNameTag = value;
          onConfigChanged();
        }));
      buttonX += IconButton.SIZE + 2;
    }
    if (!synced || serverAllows(SyncedModConfig::allowNotForceElytraWhenFlying)) {
      addRenderableWidget(IconButton.toggle(buttonX, buttonY,
        Component.translatable("gui.showmeyourskin.armorScreen.forceElytraWhenFlyingTooltip"), "force_elytra_when_flying",
        () -> config.forceElytraWhenFlying, value -> {
          config.forceElytraWhenFlying = value;
          onConfigChanged();
        }));
    }
  }

  private static boolean serverAllows(Function<SyncedModConfig, Boolean> getter) {
    return ShowMeYourSkinClient.serverConfig == null || getter.apply(ShowMeYourSkinClient.serverConfig);
  }

  private void onConfigChanged() {
    if (synced) {
      ShowMeYourSkinClient.syncToServer(selected.armorConfig());
    } else {
      if (selected != globalOption) {
        // Editing a player turns their entry into a saved override
        ModConfig.INSTANCE.overrides.put(selected.uuid(), new ModConfig.LocalOverride(selected.title().getString(), selected.armorConfig()));
      }
      ModConfig.INSTANCE.save();
    }
  }

  private void selectOption(PlayerOption option) {
    if (option == selected) {
      return;
    }
    selected = option;
    preview.setOption(option);
    rebuildWidgets();
  }

  private void deleteOption(PlayerOption option) {
    option.armorConfig().clear();
    ModConfig.INSTANCE.overrides.remove(option.uuid());
    ModConfig.INSTANCE.save();

    if (option == selected) {
      //noinspection DataFlowIssue
      selectOption(globalOption);
    } else {
      rebuildWidgets();
    }
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    super.extractBackground(graphics, mouseX, mouseY, delta);

    Panels.light(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    Panels.inset(graphics, panelX + PADDING, panelY + PADDING, CONTROLS_WIDTH, CONTROLS_HEIGHT);
    Panels.inset(graphics, panelX + PADDING, panelY + PADDING + CONTROLS_HEIGHT + GAP, CONTROLS_WIDTH, OPTIONS_HEIGHT);
  }

  @Override
  public void tick() {
    preview.tick();
  }

  @Override
  public void onClose() {
    this.minecraft.gui.setScreen(this.parent);
  }
}
