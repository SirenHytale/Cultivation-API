package com.example.exampleaddon;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import plugin.siren.API.CultivationAPI;

import javax.annotation.Nonnull;

/**
 * A worked example of integrating with the Cultivation API.
 *
 * <p>Everything happens in {@link #setup()}, and load order relative to
 * Cultivation does not matter: every registry in that API is a plain static
 * collection that nothing reads until a player actually meditates, opens a menu
 * or trips a realm gate - all long after every plugin has finished loading. So
 * there is no need for LoadBefore, retry loops or deferred initialization.</p>
 */
public class ExampleAddon extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ExampleAddon plugin;

    /**
     * Stand-in for a real config file. A production mod would use
     * {@code this.withConfig("Config", ExampleConfig.CODEC)} in the constructor
     * and read through the returned holder - never capture the config OBJECT,
     * since a reload replaces the instance behind the holder and a captured one
     * then edits a discarded copy.
     */
    private final ExampleSettings settings = new ExampleSettings();

    public ExampleAddon(@Nonnull JavaPluginInit init) {
        super(init);
        plugin = this;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Example Addon is loading.");

        // Listeners: react to, veto, or re-tune what Cultivation does.
        CultivationHooks.registerAll();

        // Content: a race, a technique, a Qi item, a codex article, an admin
        // config section and a menu page.
        ExampleContent.registerAll();

        LOGGER.atInfo().log("Example Addon has loaded.");
    }

    @Override
    protected void shutdown() {
        // Only the registries with an unregister need cleaning up. Event
        // listeners deliberately have none - listener lifetime is server
        // lifetime, matching how plugins load once and stay.
        ExampleContent.unregisterAll();

        // If this mod had installed either of these, hand them back so a server
        // unloading only this mod returns to a working ladder and Cultivation's
        // own wording rather than to a provider whose components are gone.
        CultivationAPI.setProgressionProvider(null);
        CultivationAPI.setTheme(null);

        LOGGER.atInfo().log("Example Addon has shut down.");
    }

    public static ExampleAddon get() {
        return plugin;
    }

    public ExampleSettings getSettings() {
        return this.settings;
    }

    /** Stand-in for a real config class. See the note on {@link #settings}. */
    public static final class ExampleSettings {
        private float qiEventMultiplier = 2.0f;
        private float charmMultiplier = 1.5f;
        /** Stored as a 0-1 fraction. The admin row scales it to a percent. */
        private float rareDropChance = 0.004f;

        public float getQiEventMultiplier() { return this.qiEventMultiplier; }
        public void setQiEventMultiplier(float value) { this.qiEventMultiplier = Math.max(1f, value); }

        public float getCharmMultiplier() { return this.charmMultiplier; }
        public void setCharmMultiplier(float value) { this.charmMultiplier = Math.max(1f, value); }

        public float getRareDropChance() { return this.rareDropChance; }
        public void setRareDropChance(float value) { this.rareDropChance = Math.clamp(value, 0f, 1f); }
    }
}
