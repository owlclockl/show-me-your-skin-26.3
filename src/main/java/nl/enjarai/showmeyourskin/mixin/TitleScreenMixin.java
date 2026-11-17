package nl.enjarai.showmeyourskin.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import nl.enjarai.showmeyourskin.gui.ConfigScreenMenuButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
  @Inject(method = "init", at = @At("TAIL"))
  private void showMeYourSkin$addSettingsButton(CallbackInfo ci) {
    ConfigScreenMenuButton.addTo((Screen) (Object) this);
  }
}
