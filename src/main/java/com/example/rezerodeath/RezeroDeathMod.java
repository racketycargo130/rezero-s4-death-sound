package com.example.rezerodeath;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only helper for the Re:Zero death sound resource pack.
 *
 * The resource pack supplies the audio (event id "rezero:death").
 * This mod decides WHEN to play it: on a real death, and on servers like Hypixel
 * (where the server fakes deaths) when a death message shows up in chat.
 */
@Mod(value = RezeroDeathMod.MODID, dist = Dist.CLIENT)
public class RezeroDeathMod {
    public static final String MODID = "rezerodeath";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RezeroDeathMod(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        // Adds a "Config" button for this mod in the Mods screen.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
