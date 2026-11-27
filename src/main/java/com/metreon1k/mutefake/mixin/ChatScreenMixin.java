package com.metreon1k.mutefake.mixin;

import com.metreon1k.mutefake.chat.ChatInteraction;
import com.metreon1k.mutefake.ui.MuteMenuScreen;
import com.metreon1k.mutefake.screenshot.ScreenshotUploader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mutefake$onChatClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 1) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return;

        String line = ChatInteraction.lineAt(mouseX, mouseY);
        if (line == null || line.isBlank()) return;

        String player = ChatInteraction.authorAt(mouseX, mouseY);
        if (player == null || player.isBlank()) return;

        java.nio.file.Path screenshot = ScreenshotUploader.captureChatLine(mouseX, mouseY);
        mc.execute(() -> mc.setScreen(new MuteMenuScreen(player, line, (ChatScreen) (Object) this, screenshot)));
        cir.setReturnValue(true);
    }
}
