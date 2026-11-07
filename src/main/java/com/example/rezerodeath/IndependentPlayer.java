package com.example.rezerodeath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * Plays the death clip through Java's own audio system instead of Minecraft's sound engine.
 * Minecraft cuts its sounds off when you respawn / change level; this keeps playing to the end.
 * The audio is read from the resource pack: assets/rezero/death.wav (16-bit PCM WAV).
 */
final class IndependentPlayer {
    private static final Identifier WAV = Identifier.fromNamespaceAndPath("rezero", "death.wav");

    private IndependentPlayer() {}

    /** Starts playback on a background thread. Runs onFailure (on that thread) if anything goes wrong. */
    static void play(float volume, Runnable onFailure) {
        Thread thread = new Thread(() -> {
            try {
                byte[] data = load();
                try (AudioInputStream audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audio); // copies the audio into the clip, so the stream can be closed
                    setVolume(clip, volume);
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                    clip.start();
                }
            } catch (Exception e) {
                RezeroDeathMod.LOGGER.warn("[RezeroDeath] full-length playback failed, using the game's sound engine instead", e);
                onFailure.run();
            }
        }, "RezeroDeath-Audio");
        thread.setDaemon(true);
        thread.start();
    }

    private static byte[] load() throws IOException {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(WAV);
        if (resource.isEmpty()) {
            throw new IOException("rezero:death.wav not found - is the ReZeroDeathSound resource pack enabled?");
        }
        try (InputStream in = resource.get().open()) {
            return in.readAllBytes();
        }
    }

    private static void setVolume(Clip clip, float volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float linear = Math.max(volume, 0.0001F);
            float db = 20.0F * (float) Math.log10(linear);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        }
    }
}
