package com.example.rezerodeath;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = RezeroDeathMod.MODID, value = Dist.CLIENT)
public final class DeathListener {
    /** Defined by the resource pack in assets/rezero/sounds.json */
    private static final SoundEvent DEATH_SOUND =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("rezero", "death"));

    private static long lastPlayedMs = 0L;
    private static boolean wasDead = false;
    private static boolean wasSpectator = false;
    private static boolean haveSpectatorBaseline = false;
    private static boolean debug = false;
    private static long overlayStartMs = -1L;

    private DeathListener() {}

    // ---- Real deaths (the player's health actually hits 0) ----
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            wasDead = false;
            wasSpectator = false;
            haveSpectatorBaseline = false;
            return;
        }
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead && Config.PLAY_ON_REAL_DEATH.get()) {
            trigger("real death");
        }
        wasDead = dead;

        // Fake deaths: servers like Hypixel often switch you to spectator when you die.
        boolean spectator = player.isSpectator();
        if (haveSpectatorBaseline && spectator && !wasSpectator && Config.PLAY_ON_SPECTATOR.get()) {
            trigger("switched to spectator");
        }
        wasSpectator = spectator;
        haveSpectatorBaseline = true;
    }

    // ---- Fake deaths (Hypixel etc.): look for a death message in server chat ----
    @SubscribeEvent
    static void onSystemChat(ClientChatReceivedEvent.System event) {
        if (event.isOverlay()) {
            return; // action bar text, ignore
        }
        String line = clean(event.getMessage().getString());
        if (line.isEmpty()) {
            return;
        }
        if (debug || Config.LOG_CHAT.get()) {
            RezeroDeathMod.LOGGER.info("[RezeroDeath] chat: {}", line);
        }
        if (!Config.PLAY_ON_CHAT_TRIGGER.get()) {
            return;
        }
        if (Config.IGNORE_COLON.get() && line.contains(":")) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        String name = player != null
                ? player.getName().getString().toLowerCase(Locale.ROOT)
                : Minecraft.getInstance().getUser().getName().toLowerCase(Locale.ROOT);

        String lower = line.toLowerCase(Locale.ROOT);
        for (String raw : Config.CHAT_TRIGGERS.get()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String phrase = raw.toLowerCase(Locale.ROOT).replace("{player}", name).trim();
            if (!phrase.isEmpty() && lower.contains(phrase)) {
                trigger("chat match '" + raw + "'");
                return;
            }
        }
    }

    // ---- /rezerodeath test | debug ----
    @SubscribeEvent
    static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("rezerodeath")
                        .then(Commands.literal("test").executes(ctx -> {
                            play();
                            return 1;
                        }))
                        .then(Commands.literal("debug").executes(ctx -> {
                            debug = !debug;
                            say("Chat logging " + (debug ? "ON" : "OFF") + " (see logs/latest.log, search for [RezeroDeath])");
                            return 1;
                        })));
    }

    // ---- helpers ----
    private static void trigger(String reason) {
        long now = System.currentTimeMillis();
        long cooldownMs = Config.COOLDOWN_SECONDS.get() * 1000L;
        if (now - lastPlayedMs < cooldownMs) {
            return;
        }
        lastPlayedMs = now;
        if (debug || Config.LOG_CHAT.get()) {
            RezeroDeathMod.LOGGER.info("[RezeroDeath] playing sound: {}", reason);
        }
        play();
        if (Config.DEATH_MESSAGE_ENABLED.get()) {
            printOwnChat();
        }
        if (Config.SCREEN_TEXT_ENABLED.get()) {
            overlayStartMs = System.currentTimeMillis();
        }
    }

    static void play() {
        Minecraft mc = Minecraft.getInstance();
        float volume = Config.VOLUME.get().floatValue();
        if (Config.FULL_LENGTH_PLAYBACK.get()) {
            float master = mc.options.getSoundSourceVolume(SoundSource.MASTER);
            IndependentPlayer.play(volume * master, () -> mc.execute(() -> playWithGameEngine(volume)));
        } else {
            playWithGameEngine(volume);
        }
    }

    private static void playWithGameEngine(float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(DEATH_SOUND, 1.0F, volume));
    }

    /** Prints the death message into the local chat window only (never sent to the server). */
    private static void printOwnChat() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Component line = Component.literal(Config.DEATH_MESSAGE_TEXT.get()).withStyle(chatColor());
        int count = Config.DEATH_MESSAGE_COUNT.get();
        for (int i = 0; i < count; i++) {
            player.displayClientMessage(line, false);
        }
    }

    private static ChatFormatting chatColor() {
        ChatFormatting color = ChatFormatting.getByName(Config.DEATH_MESSAGE_COLOR.get());
        if (color == null || !color.isColor()) {
            color = ChatFormatting.DARK_PURPLE;
        }
        return color;
    }

    // ---- Big text tiled across the whole screen for a few seconds ----
    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        if (overlayStartMs < 0L) {
            return;
        }
        long elapsed = System.currentTimeMillis() - overlayStartMs;
        long total = Config.SCREEN_TEXT_SECONDS.get() * 1000L;
        if (elapsed >= total) {
            overlayStartMs = -1L;
            return;
        }
        long fadeMs = 600L;
        float alpha = elapsed > total - fadeMs ? (total - elapsed) / (float) fadeMs : 1.0F;
        int a = (int) (alpha * 255.0F);
        if (a < 8) {
            return;
        }
        Integer rgb = chatColor().getColor();
        int argb = (a << 24) | ((rgb != null ? rgb : 0xAA00AA) & 0xFFFFFF);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        String text = Config.DEATH_MESSAGE_TEXT.get();
        float scale = Config.SCREEN_TEXT_SCALE.get().floatValue();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        float cellW = Math.max(1.0F, font.width(text) * scale * 1.15F);
        float cellH = Math.max(1.0F, font.lineHeight * scale * 1.3F);

        int row = 0;
        for (float y = 0.0F; y < screenH; y += cellH, row++) {
            float startX = (row % 2 == 0) ? 0.0F : -cellW * 0.5F; // brick pattern
            for (float x = startX; x < screenW; x += cellW) {
                graphics.pose().pushMatrix();
                graphics.pose().scale(scale, scale);
                graphics.drawString(font, text, Math.round(x / scale), Math.round(y / scale), argb, true);
                graphics.pose().popMatrix();
            }
        }
    }

    private static void say(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    /** Strip legacy colour codes and collapse whitespace. */
    private static String clean(String text) {
        return text.replaceAll("\u00a7[0-9A-FK-ORa-fk-or]", "").replaceAll("\\s+", " ").trim();
    }
}
