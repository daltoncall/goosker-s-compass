package com.goosker.compass.client.mixin;

import com.goosker.compass.client.hud.BossBarPositioning;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moves title and bar together without replacing any of vanilla's boss-bar logic. */
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void gooskers$pushBossBarOffset(DrawContext context, CallbackInfo ci) {
        context.getMatrices().push();
        context.getMatrices().translate(0, BossBarPositioning.verticalOffset(), 0);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void gooskers$popBossBarOffset(DrawContext context, CallbackInfo ci) {
        context.getMatrices().pop();
    }
}
