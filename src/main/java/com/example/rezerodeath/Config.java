package com.example.rezerodeath;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config, saved to config/rezerodeath-client.toml
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Matched case-insensitively against each server chat line (colour codes stripped).
    // {player} is replaced by your Minecraft username.
    // These are best guesses for Hypixel-style death messages. Tune them with /rezerodeath debug.
    private static final List<String> DEFAULT_TRIGGERS = List.of(
            "you died",
            "you were killed",
            "you have been eliminated",
            "{player} was killed",
            "{player} was slain",
            "{player} was shot",
            "{player} was knocked",
            "{player} was thrown",
            "{player} was blown up",
            "{player} was fireballed",
            "{player} was snowballed",
            "{player} fell",
            "{player} died",
            "{player} burned",
            "{player} drowned",
            "{player} starved",
            "{player} suffocated",
            "{player} hit the ground",
            "{player} withered away");

    public static final ModConfigSpec.BooleanValue PLAY_ON_REAL_DEATH = BUILDER
            .comment("Play the sound when your character really dies (singleplayer, vanilla-style servers).")
            .define("playOnRealDeath", true);

    public static final ModConfigSpec.BooleanValue PLAY_ON_SPECTATOR = BUILDER
            .comment("Play when you get switched into spectator mode. Many Hypixel minigames (e.g. Bedwars) put you in",
                    "spectator when you die, so this works even if the chat wording is different.",
                    "Side effect: also fires if you switch yourself to spectator (F3+N or /gamemode).")
            .define("playOnSpectatorSwitch", true);

    public static final ModConfigSpec.BooleanValue PLAY_ON_CHAT_TRIGGER = BUILDER
            .comment("Play the sound when a server chat line matches one of the chatTriggers (for servers that fake deaths, like Hypixel).")
            .define("playOnChatTrigger", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CHAT_TRIGGERS = BUILDER
            .comment("Phrases that count as 'you died'. Case-insensitive 'contains' match. {player} = your username.",
                    "If you are nicked on a server, add phrases using your nick instead.")
            .defineListAllowEmpty("chatTriggers", DEFAULT_TRIGGERS, () -> "", Config::isString);

    public static final ModConfigSpec.BooleanValue IGNORE_COLON = BUILDER
            .comment("Ignore chat lines containing ':' so other players typing 'you died lol' can't trigger the sound.")
            .define("ignoreLinesWithColon", true);

    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS = BUILDER
            .comment("Minimum seconds between plays (stops double-plays, e.g. real death + death message).")
            .defineInRange("cooldownSeconds", 4, 0, 60);

    public static final ModConfigSpec.BooleanValue FULL_LENGTH_PLAYBACK = BUILDER
            .comment("Play the clip through Java's own audio system so it is NOT cut off when you click Respawn.",
                    "Needs assets/rezero/death.wav from the resource pack. Falls back to Minecraft's sound engine if that fails.")
            .define("playThroughRespawn", true);

    public static final ModConfigSpec.DoubleValue VOLUME = BUILDER
            .comment("Volume of the sound (0.0 - 1.0). Also scaled by your Master volume.")
            .defineInRange("volume", 1.0D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue LOG_CHAT = BUILDER
            .comment("Write every server chat line to logs/latest.log as '[RezeroDeath] chat: ...' (for tuning triggers).",
                    "You can also toggle this in-game with /rezerodeath debug.")
            .define("logChatLines", false);

    public static final ModConfigSpec.BooleanValue DEATH_MESSAGE_ENABLED = BUILDER
            .comment("On death, print a message several times in YOUR OWN chat window.",
                    "Only you can see it - nothing is sent to the server.")
            .define("deathMessageEnabled", true);

    public static final ModConfigSpec.ConfigValue<String> DEATH_MESSAGE_TEXT = BUILDER
            .comment("The text to print.")
            .define("deathMessageText", "I LOVE YOU");

    public static final ModConfigSpec.IntValue DEATH_MESSAGE_COUNT = BUILDER
            .comment("How many times to print it.")
            .defineInRange("deathMessageCount", 15, 1, 50);

    public static final ModConfigSpec.ConfigValue<String> DEATH_MESSAGE_COLOR = BUILDER
            .comment("Chat colour name, e.g. dark_purple, light_purple, red, gold, aqua.")
            .define("deathMessageColor", "dark_purple");

    public static final ModConfigSpec.BooleanValue SCREEN_TEXT_ENABLED = BUILDER
            .comment("On death, also splash the message across your screen in big letters for a few seconds (local only).")
            .define("screenTextEnabled", true);

    public static final ModConfigSpec.IntValue SCREEN_TEXT_SECONDS = BUILDER
            .comment("How long the big on-screen text stays (the last 0.6s fades out).")
            .defineInRange("screenTextSeconds", 3, 1, 10);

    public static final ModConfigSpec.DoubleValue SCREEN_TEXT_SCALE = BUILDER
            .comment("Text size multiplier for the on-screen text. Bigger = fewer repeats on screen.")
            .defineInRange("screenTextScale", 5.0D, 1.0D, 12.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}

    private static boolean isString(final Object obj) {
        return obj instanceof String;
    }
}
