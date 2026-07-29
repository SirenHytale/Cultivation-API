package com.example.exampleaddon;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import plugin.siren.API.AdminConfigChoice;
import plugin.siren.API.AdminConfigField;
import plugin.siren.API.AdminConfigSection;
import plugin.siren.API.CodexCategory;
import plugin.siren.API.CodexEntry;
import plugin.siren.API.CultivationAPI;
import plugin.siren.API.CultivationMenuPage;
import plugin.siren.ECS.Races.PlayerRace;
import plugin.siren.ECS.Realms.CultivationRealm;
import plugin.siren.ECS.Technique.Technique;
import plugin.siren.ECS.Technique.TechniqueContext;
import plugin.siren.Utils.Config.RaceConfig;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Content this mod contributes to Cultivation: a race, a technique, a
 * Qi-absorption item, a Codex article, an admin config section and a menu page.
 *
 * <p>Every id here is namespaced with the mod's name. Ids are global across
 * every mod on the server, and registering an existing one REPLACES the previous
 * holder rather than erroring - so an un-namespaced id silently steals another
 * mod's feature (or loses its own to one).</p>
 */
public final class ExampleContent {
    private static final String PREFIX = "ExampleAddon:";

    /**
     * Held so later code compares against the race that was actually registered
     * rather than re-deriving its id. The registry hands out singletons, so
     * reference equality works.
     */
    private static PlayerRace stargazerRace;

    private static Technique flameStep;

    private ExampleContent() {}

    public static void registerAll() {
        registerRace();
        registerTechnique();
        registerQiItem();
        registerCodex();
        registerAdminSection();
        registerMenuPage();
    }

    public static void unregisterAll() {
        // Races, techniques and Qi-item modifiers have no unregister - they are
        // permanent for the server's lifetime, like event listeners.
        CultivationAPI.unregisterCodexEntry(PREFIX + "stargazing");
        CultivationAPI.unregisterAdminConfigSection(PREFIX + "balance");
        CultivationAPI.unregisterMenuPage("exampleAddon:stargazing");
    }

    // ------------------------------------------------------------------
    // A playable race
    // ------------------------------------------------------------------

    private static void registerRace() {
        stargazerRace = CultivationAPI.registerRace(
                PREFIX + "Stargazer",
                "Stargazer",                                   // fallback name
                "server.exampleaddon.race.stargazer.name",     // lang key, or null
                CultivationRealm.FOUNDATION_ESTABLISHMENT,     // unlock realm
                ExampleContent::buildStargazerConfig);         // Supplier, not a snapshot
    }

    /**
     * A SUPPLIER, called every time Cultivation needs the race's numbers - so a
     * server owner editing the backing config sees the change without a restart.
     * Reading through the holder rather than capturing the config object is the
     * whole point; a reload replaces the instance behind it.
     */
    private static RaceConfig buildStargazerConfig() {
        RaceConfig config = new RaceConfig();
        config.setDescription("server.exampleaddon.race.stargazer.description");
        config.setHealthBonusPercent(-5f);
        config.setDamageBonusPercent(0f);
        config.setQiGainRatePercentBonus(20f);
        config.setBreakthroughDurationPercentReduction(10f);
        config.setQiAlignmentYinBiasPercent(15f);

        // Deliberately NOT setting UnlockRealm: leaving it empty lets the realm
        // passed to registerRace stand as the default, while a server owner who
        // does set it in their own config file stays in charge of the gate.
        return config;
    }

    public static PlayerRace getStargazerRace() {
        return stargazerRace;
    }

    // ------------------------------------------------------------------
    // An active technique
    // ------------------------------------------------------------------

    private static void registerTechnique() {
        flameStep = CultivationAPI.registerTechnique(
                PREFIX + "flame_step",
                "Flame Step",
                "server.exampleaddon.technique.flame_step.name",
                "server.exampleaddon.technique.flame_step.description",
                CultivationAPI.newTechniqueRule(
                        PREFIX + "flame_step",
                        true,                  // enabled
                        true,                  // daoSpecific
                        "FIRE",                // requiredElement (daoSpecific only)
                        "FIRE",                // elements carried (flavor)
                        "",                    // damageType - a DamageCause asset id
                        "QI_CONDENSATION",     // unlock realm
                        30f,                   // Qi cost
                        8f,                    // cooldown, seconds
                        // params: alternating String/float pairs. An odd number
                        // of arguments here is a bug - it must divide evenly.
                        "BaseDistance", 6f,
                        "DistancePerRealm", 2f),
                ExampleContent::flameStep);
    }

    /**
     * Runs only AFTER every gate has passed and the Qi cost and cooldown have
     * been applied, so it never has to re-check availability itself.
     */
    private static void flameStep(@Nonnull TechniqueContext context) {
        Vector3d position = context.getPosition();
        Vector3d look = context.getLookDirection();
        if (position == null || look == null) {
            return;
        }

        float distance = context.getParam("BaseDistance", 6f)
                + context.getParam("DistancePerRealm", 2f) * context.getRealmIndex();

        // Copy before mutating - joml operations write in place, and both
        // vectors belong to the caller.
        Vector3d destination = new Vector3d(look).normalize().mul(distance).add(position);

        context.teleport(destination);
        context.spawnParticle("ExampleAddon_FlameBurst", position);
        context.sendMessage(Message.translation("server.exampleaddon.technique.flame_step.playerMsg.used"));
    }

    /** Perform it from your own trigger - a keybind, another item, an event. */
    public static boolean performFlameStep(@Nonnull ComponentAccessor<EntityStore> accessor,
                                           @Nonnull Ref<EntityStore> ref,
                                           @Nonnull PlayerRef playerRef) {
        // Runs every gate; false means one blocked it. The player is messaged
        // either way - the effect's success message, or the failure reason.
        return CultivationAPI.performTechnique(accessor, ref, playerRef, flameStep);
    }

    // ------------------------------------------------------------------
    // A meditation-boosting item
    // ------------------------------------------------------------------

    private static void registerQiItem() {
        // While this is in a meditating player's active hotbar slot, their
        // Spirit Vein absorption is multiplied. Same mechanism as the built-in
        // Qi Gathering Talisman.
        CultivationAPI.registerQiAbsorptionItemModifier(
                "ExampleAddon_StarCharm",
                ExampleAddon.get().getSettings().getCharmMultiplier());
    }

    // ------------------------------------------------------------------
    // A Codex article
    // ------------------------------------------------------------------

    private static void registerCodex() {
        CultivationAPI.registerCodexEntry(
                CodexEntry.builder(PREFIX + "stargazing")
                        .title("server.exampleaddon.codex.stargazing.title")
                        .summary("server.exampleaddon.codex.stargazing.summary")
                        .category(CodexCategory.SELF)
                        .sortOrder(450)
                        // The body is written fresh for each reader, and the page
                        // carries them (getAccessor/getRef/getPlayerRef). State
                        // THIS server's real numbers and what the reader has
                        // actually reached - that is the one thing a wiki cannot do.
                        .body(page -> {
                            int level = CultivationAPI.getGlobalLevel(page.getAccessor(), page.getRef());

                            page.heading("server.exampleaddon.codex.stargazing.heading")
                                    .paragraph("server.exampleaddon.codex.stargazing.intro")
                                    .stat("server.exampleaddon.codex.stargazing.charmBonus",
                                            ExampleAddon.get().getSettings().getCharmMultiplier())
                                    .divider()
                                    .recipe("ExampleAddon_StarCharm")
                                    // Say what is locked rather than hiding the
                                    // article - a codex that hides what you have
                                    // not unlocked cannot tell you how to unlock it.
                                    .noteIf(level < 10, "server.exampleaddon.codex.stargazing.locked");
                        })
                        .build());
    }

    // ------------------------------------------------------------------
    // A section in Cultivation's admin menu
    // ------------------------------------------------------------------

    private static void registerAdminSection() {
        CultivationAPI.registerAdminConfigSection(new AdminConfigSection() {
            @Nonnull
            @Override
            public String getKey() {
                return PREFIX + "balance";
            }

            @Nonnull
            @Override
            public Message getLabel() {
                return Message.translation("server.exampleaddon.admin.balance");
            }

            @Nonnull
            @Override
            public Message getHint() {
                return Message.translation("server.exampleaddon.admin.balance.hint");
            }

            @Override
            public int getSortOrder() {
                // SORT_LAST is the default: after every one of Cultivation's own
                // sections. A value between two of theirs (they sit at
                // SORT_BUILTIN_FIRST upward in steps of 100) would slot this in
                // among them instead.
                return AdminConfigSection.SORT_LAST;
            }

            @Nonnull
            @Override
            public List<AdminConfigField> getFields() {
                // A STABLE list: the page matches an admin's in-flight edits to
                // fields by key, so a list that changes shape between render and
                // save silently drops those edits.
                return List.of(
                        // BOOLEAN - a real checkbox, not a 0/1 number.
                        CultivationAPI.withTooltip(
                                CultivationAPI.newAdminBooleanField(PREFIX + "Enabled",
                                        Message.translation("server.exampleaddon.admin.enabled"),
                                        () -> ExampleAddon.get().getSettings().isEnabled(),
                                        ExampleAddon.get().getSettings()::setEnabled),
                                // A plain String, deliberately: TooltipText is a
                                // String property client-side and a Message
                                // pushed at it DISCONNECTS the player. Anything
                                // that must be localized belongs in the label.
                                "Turns off every Stargazer mechanic without unloading the mod."),

                        // NUMBER - the default kind, two decimal places.
                        CultivationAPI.newAdminConfigField(PREFIX + "QiEventMultiplier",
                                Message.translation("server.exampleaddon.admin.qiEventMultiplier"),
                                () -> ExampleAddon.get().getSettings().getQiEventMultiplier(),
                                value -> ExampleAddon.get().getSettings().setQiEventMultiplier((float) value)),

                        CultivationAPI.newAdminConfigField(PREFIX + "CharmMultiplier",
                                Message.translation("server.exampleaddon.admin.charmMultiplier"),
                                () -> ExampleAddon.get().getSettings().getCharmMultiplier(),
                                value -> ExampleAddon.get().getSettings().setCharmMultiplier((float) value)),

                        // Stored as a 0-1 fraction but edited as a percent: the
                        // row widget shows two decimals, so a raw 0.004 would
                        // display as 0.00 and be uneditable.
                        CultivationAPI.newAdminConfigField(PREFIX + "RareDropChance",
                                Message.translation("server.exampleaddon.admin.rareDropChance"),
                                () -> ExampleAddon.get().getSettings().getRareDropChance() * 100f,
                                value -> ExampleAddon.get().getSettings().setRareDropChance((float) (value / 100f))),

                        // INT - a count, where decimal places would be noise.
                        CultivationAPI.newAdminIntField(PREFIX + "MaxStargazers",
                                Message.translation("server.exampleaddon.admin.maxStargazers"),
                                () -> ExampleAddon.get().getSettings().getMaxStargazers(),
                                value -> ExampleAddon.get().getSettings().setMaxStargazers((int) value)),

                        // CHOICE - a dropdown cannot be typed wrong, which is why
                        // an enum-valued setting should never be a TEXT field.
                        CultivationAPI.newAdminChoiceField(PREFIX + "UnlockRealm",
                                Message.translation("server.exampleaddon.admin.unlockRealm"),
                                ExampleContent::realmChoices,
                                () -> ExampleAddon.get().getSettings().getUnlockRealm(),
                                ExampleContent::setUnlockRealm),

                        // TEXT - free-form, for the cases with no known value set.
                        CultivationAPI.newAdminTextField(PREFIX + "WelcomeMessage",
                                Message.translation("server.exampleaddon.admin.welcomeMessage"),
                                () -> ExampleAddon.get().getSettings().getWelcomeMessage(),
                                ExampleAddon.get().getSettings()::setWelcomeMessage));
            }

            @Override
            public void save() {
                // A real mod persists its config here, e.g. myConfig.save().
                ExampleAddon.LOGGER.atInfo().log("Example Addon settings saved from the admin menu.");
            }
        });
    }

    /**
     * Re-read on every render, so a choice set that depends on what other mods
     * have registered stays current.
     */
    @Nonnull
    private static List<AdminConfigChoice> realmChoices() {
        return Arrays.stream(CultivationRealm.values())
                .map(realm -> AdminConfigChoice.of(realm, realm.getTranslationKey()))
                .toList();
    }

    /**
     * Re-resolve rather than trust: the id arrived from a client, and an option
     * that has since stopped being valid must not be accepted just because it was
     * once offered.
     */
    private static void setUnlockRealm(String id) {
        CultivationRealm realm = CultivationRealm.fromName(id);
        if (realm != null) {
            ExampleAddon.get().getSettings().setUnlockRealm(realm.name());
        }
    }

    // ------------------------------------------------------------------
    // A page on the shared menu nav bar
    // ------------------------------------------------------------------

    private static void registerMenuPage() {
        CultivationAPI.registerMenuPage(
                CultivationMenuPage.builder("exampleAddon:stargazing")
                        .label("server.exampleaddon.nav.stargazing")
                        // The eight built-ins are spaced 100 apart from
                        // SORT_FIRST (100), so 450 lands between Skill Tree and
                        // Bonuses. SORT_DEFAULT (1000) puts it after them all.
                        .sortOrder(450)
                        .permission("exampleaddon.stargazing")
                        // A gate a permission cannot express. Both must pass.
                        .visible(playerRef -> true)
                        .onOpen((store, ref, playerRef) -> {
                            // Swap the player's open page for your own CustomUIPage.
                            // CultivationAPI.openMenuPage(store, ref, new MyStargazingPage(playerRef));
                            ExampleAddon.LOGGER.atInfo().log("Stargazing page opened.");
                        })
                        .build());
    }
}
