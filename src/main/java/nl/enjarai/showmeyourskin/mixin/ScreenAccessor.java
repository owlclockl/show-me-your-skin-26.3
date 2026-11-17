package nl.enjarai.showmeyourskin.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
  @Accessor("width")
  int showMeYourSkin$getWidth();

  @Accessor("height")
  int showMeYourSkin$getHeight();

  @Invoker("addRenderableWidget")
  GuiEventListener showMeYourSkin$addRenderableWidget(GuiEventListener widget);
}
