package nl.enjarai.showmeyourskin.mixin;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import nl.enjarai.showmeyourskin.gui.ConfigScreenMenuButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
  @Inject(method = "init", at = @At("TAIL"))
  private void showMeYourSkin$addSettingsButton(CallbackInfo ci) {
    ConfigScreenMenuButton.addTo((Screen) (Object) this);
  }
}
