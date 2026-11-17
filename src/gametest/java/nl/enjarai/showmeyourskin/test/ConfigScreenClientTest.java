package nl.enjarai.showmeyourskin.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.wildfire.api.Gender;
import com.wildfire.api.client.WildfireClientAPI;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.AbstractWidget;
import nl.enjarai.showmeyourskin.Components;
import nl.enjarai.showmeyourskin.gui.ArmorConfigScreen;
import nl.enjarai.showmeyourskin.config.ModConfig;
import nl.enjarai.showmeyourskin.gui.PieceLabel;
import nl.enjarai.showmeyourskin.gui.PlayerListWidget;

import java.util.function.Predicate;

/**
 * Opens the config screen in each mode and takes screenshots, so the UI can be checked without clicking through it by hand.
 * Run with {@code ./gradlew runClientGameTest}; screenshots end up in {@code build/run/clientGameTest/screenshots}.
 */
public class ConfigScreenClientTest implements FabricClientGameTest {
  @Override
  public void runTest(ClientGameTestContext context) {
    // Title screen: local mode, no player, so no preview and an empty player list
    context.setScreen(() -> ArmorConfigScreen.createConfigScreen(null));
    context.waitTicks(5);
    context.takeScreenshot("smys_01_title_local");
    context.setScreen(() -> null);

    try (var singleplayer = context.worldBuilder().create()) {
      var server = singleplayer.getServer();
      server.runCommand("time set noon");
      server.runCommand("item replace entity @a armor.head with diamond_helmet[enchantments={unbreaking:1},trim={pattern:flow,material:gold}]");
      server.runCommand("item replace entity @a armor.chest with netherite_chestplate[enchantments={unbreaking:1},trim={pattern:flow,material:gold}]");
      server.runCommand("item replace entity @a armor.legs with iron_leggings[trim={pattern:flow,material:gold}]");
      server.runCommand("item replace entity @a armor.feet with golden_boots[enchantments={unbreaking:1}]");
      server.runCommand("item replace entity @a weapon.offhand with shield");
      context.waitTicks(20);

      // The integrated server has the mod, so this opens in synced mode
      context.setScreen(() -> ArmorConfigScreen.createConfigScreen(null));
      context.waitTicks(60);
      context.takeScreenshot("smys_02_synced");

      clickWidget(context, w -> w instanceof PieceLabel && w.getMessage().getString().equals("Chestplate"));
      context.waitTicks(5);
      boolean chestShown = context.computeOnClient(mc -> mc.player.getComponent(Components.ARMOR_CONFIG).getConfig().chest.base);
      if (chestShown) {
        throw new AssertionError("Clicking the Chestplate label should hide the chestplate");
      }
      context.takeScreenshot("smys_03_synced_chest_hidden");

      // The chestplate should now be hidden in the world too.
      // Female Gender Mod is loaded in this test, so with the player set to female the breasts should be unarmored as well.
      context.runOnClient(mc -> WildfireClientAPI.players().getOrCreate(mc.player).gender().update(Gender.FEMALE));
      context.setScreen(() -> null);
      context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
      context.waitTicks(20);
      context.takeScreenshot("smys_04_world_chest_hidden");

      // Show the chestplate again, for comparison
      context.setScreen(() -> ArmorConfigScreen.createConfigScreen(null));
      context.waitTicks(5);
      clickWidget(context, w -> w instanceof PieceLabel && w.getMessage().getString().equals("Chestplate"));
      context.setScreen(() -> null);
      context.waitTicks(20);
      context.takeScreenshot("smys_04b_world_chest_shown");
      context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

      context.setScreen(() -> ArmorConfigScreen.createLocalConfigScreen(null));
      context.waitTicks(60);
      context.takeScreenshot("smys_05_local");

      // Select Player0 in the list (first row below the global entry), then hide their helmet
      double[] row = context.computeOnClient(mc -> {
        var list = mc.gui.screen().children().stream().filter(c -> c instanceof PlayerListWidget).map(c -> (PlayerListWidget) c).findFirst().orElseThrow();
        int scale = mc.getWindow().getGuiScale();
        return new double[]{(list.getX() + list.getWidth() / 2.0) * scale, (list.getY() + 6 + 22 + 4 + 1 + 10) * scale};
      });
      context.getInput().setCursorPos(row[0], row[1]);
      context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
      context.waitTicks(5);
      clickWidget(context, w -> w instanceof PieceLabel && w.getMessage().getString().equals("Helmet"));
      context.waitTicks(60);
      context.takeScreenshot("smys_06_local_player_override");

      boolean overrideSaved = context.computeOnClient(mc -> {
        var override = ModConfig.INSTANCE.overrides.get(mc.player.getUUID());
        return override != null && !override.config().head.base;
      });
      if (!overrideSaved) {
        throw new AssertionError("Editing Player0 should save an override with the helmet hidden");
      }
    }
  }

  private static void clickWidget(ClientGameTestContext context, Predicate<AbstractWidget> predicate) {
    double[] pos = context.computeOnClient(mc -> {
      var widget = mc.gui.screen().children().stream()
        .filter(c -> c instanceof AbstractWidget w && predicate.test(w))
        .map(c -> (AbstractWidget) c)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No matching widget on " + mc.gui.screen()));
      int scale = mc.getWindow().getGuiScale();
      return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
    });
    context.getInput().setCursorPos(pos[0], pos[1]);
    context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
  }
}
