package com.example.exampleaddon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.API.BeastEvents;
import plugin.siren.API.CultivationAPI;
import plugin.siren.API.CultivationConfigs;
import plugin.siren.API.CultivationEvents;
import plugin.siren.API.DaoEvents;
import plugin.siren.API.ItemEvents;
import plugin.siren.API.SectEvents;
import plugin.siren.API.TechniqueEvents;
import plugin.siren.API.WarEvents;
import plugin.siren.ECS.Dao.CultivationPath;
import plugin.siren.ECS.Realms.CultivationRealm;

/**
 * Every kind of Cultivation event listener, in one place.
 *
 * <p><b>Threading.</b> Listeners run synchronously on the world thread of the
 * player the event happened to. Reading that player's components is safe; do NOT
 * block, and hop threads yourself before touching anything on another world (see
 * {@link #onRaceChanged} for the shape). A listener that throws is logged and
 * skipped, so one broken addon cannot break the mod or other addons - but that
 * hides your bug rather than fixing it.</p>
 *
 * <p><b>Registration.</b> Once, from setup(), in any load order. There is
 * deliberately no unregister: listener lifetime is server lifetime.</p>
 */
public final class CultivationHooks {
    private CultivationHooks() {}

    public static void registerAll() {
        registerObservers();
        registerVetoes();
        registerRetunes();
        registerCompatibility();
    }

    // ------------------------------------------------------------------
    // Post-events: notifications. Already committed, cannot be cancelled.
    // ------------------------------------------------------------------

    private static void registerObservers() {
        CultivationEvents.onBreakthrough(event -> {
            // ref() is always non-null; player() is nullable - it is null when
            // the PlayerRef component was unavailable at fire time. Guard it.
            PlayerRef player = event.player();
            if (player == null) {
                return;
            }

            ExampleAddon.LOGGER.atInfo().log("A cultivator reached %s.", event.newRealm().name());

            if (event.newRealm() == CultivationRealm.GOLDEN_CORE_FORMATION) {
                // grant your own reward here
            }
        });

        // Cross-world work: hop to the subject's world before touching them.
        CultivationEvents.onRaceChange(CultivationHooks::onRaceChanged);

        CultivationEvents.onQiDeviation(event ->
                ExampleAddon.LOGGER.atInfo().log(
                        "Qi Deviation: demoted=%b qiLost=%.1f", event.demoted(), event.qiLost()));

        DaoEvents.onPathChange(event -> {
            if (event.newPath() == CultivationPath.DEVIL) {
                ExampleAddon.LOGGER.atInfo().log("A cultivator has fallen to the Devil path.");
            }
        });

        SectEvents.onSectCreate(event ->
                ExampleAddon.LOGGER.atInfo().log("Sect founded by %s.", event.leader()));

        TechniqueEvents.onTechniquePerform(event ->
                ExampleAddon.LOGGER.atInfo().log("Technique performed: %s", event.technique().getId()));
    }

    /**
     * The shape for doing work on the subject's world from a listener. The
     * listener itself runs on that world's thread already, but a PlayerRef held
     * across an async boundary needs re-resolving - and re-validating, since the
     * entity may be gone by then.
     */
    private static void onRaceChanged(CultivationEvents.RaceChangeEvent event) {
        PlayerRef player = event.player();
        if (player == null) {
            return;
        }

        World world = Universe.get().getWorld(player.getWorldUuid());
        if (world == null) {
            return;
        }

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                return;   // logged out, died, or changed world since the event
            }

            CultivationRealm realm = CultivationAPI.getRealm(ref.getStore(), ref);
            ExampleAddon.LOGGER.atInfo().log("%s is now %s at %s.",
                    event.newRace().getDisplayName(), event.newRace().getId(),
                    realm == null ? "no realm" : realm.name());
        });
    }

    // ------------------------------------------------------------------
    // Pre-events, part 1: vetoing. setCancelled(true) abandons the operation
    // entirely - no partial application, and the post-event never fires.
    // ------------------------------------------------------------------

    private static void registerVetoes() {
        // Hold everyone at Peak stage during a server-wide lockout.
        CultivationEvents.onPreBreakthrough(event -> {
            if (isAscensionLocked()) {
                event.setCancelled(true);
            }
        });

        // Make a ritual unbreakable by walking away (but still cancellable by
        // the player's own /cultivation meditate).
        CultivationEvents.onPreMeditationStop(event -> {
            if (event.reason() == CultivationEvents.MeditationStopReason.MOVEMENT) {
                event.setCancelled(true);
            }
        });

        // Confine sect wars to a scheduled window.
        WarEvents.onPreWarDeclare(event -> {
            if (!isWarWindowOpen()) {
                event.setCancelled(true);
            }
        });

        // Every listener runs even after one cancels, so a later listener may
        // call setCancelled(false) and let it through. Plugin load order decides
        // who wins a disagreement.
    }

    // ------------------------------------------------------------------
    // Pre-events, part 2: re-tuning. Whatever the listeners leave in these
    // fields is what the mod actually uses - this is the supported way to
    // reshape a mechanic without touching Cultivation's config files.
    // ------------------------------------------------------------------

    private static void registerRetunes() {
        // Double Qi during a server event. amount() is the gain after
        // Cultivation's own race/skill/pill/sect/dao multipliers; baseAmount()
        // is what it was before any listener touched it.
        CultivationEvents.onPreQiGain(event -> {
            if (ExampleAddon.get().getSettings().isEnabled() && isDoubleQiWeekend()) {
                event.setAmount(event.amount() * ExampleAddon.get().getSettings().getQiEventMultiplier());
            }
        });

        // Scaling to THIS server's tuning rather than to the defaults: read
        // Cultivation's own config through the holder, at the point of use.
        // Capturing the config object instead would silently detach on reload.
        CultivationEvents.onPreRitualStart(event -> {
            float serverBase = CultivationConfigs.breakthrough().get().getBreakthroughBaseSeconds();
            if (serverBase > 60f) {
                // A server that has already lengthened its rituals does not need
                // this mod lengthening them further.
                return;
            }

            event.setRequiredSeconds(event.requiredSeconds() * 1.5f);
        });

        // Soften tribulation lightning. damage() is the post-lethality-cap
        // amount fed to the damage pipeline, before armor and reduction.
        CultivationEvents.onPreTribulationStrike(event -> {
            if (!event.breakthroughRitual()) {
                event.setDamage(event.damage() * 0.5f);
            }
        });

        // Make a technique cheaper and faster for players who have earned it.
        TechniqueEvents.onPreTechniquePerform(event -> {
            if (hasMastery(event.player(), event.technique().getId())) {
                event.setQiCost(event.qiCost() * 0.75f);
                event.setCooldownSeconds(event.cooldownSeconds() * 0.5f);
            }
        });

        // Improve taming odds for a species this mod cares about.
        BeastEvents.onPreBeastTameAttempt(event -> event.setChance(Math.min(1f, event.chance() * 1.25f)));

        // Swap what a loot table drops.
        ItemEvents.onPreLootDrop(event -> {
            if (event.type() == ItemEvents.LootType.SPIRIT_STONE && rollRareDrop()) {
                event.setItemId("ExampleAddon_RadiantSpiritStone");
            }
        });

        // Shorten the siege window.
        WarEvents.onPreWarDeclare(event -> event.setWindowMillis(event.windowMillis() / 2));
    }

    // ------------------------------------------------------------------
    // Behaving correctly beside the mods Cultivation itself detects.
    // ------------------------------------------------------------------

    private static void registerCompatibility() {
        // Endless Leveling: when it is installed, Cultivation hands max health
        // and outgoing damage to EL rather than applying them itself, so the two
        // progressions add rather than multiply. An addon applying stats of its
        // own belongs on the same side of that line.
        if (CultivationAPI.isEndlessLevelingInstalled()) {
            ExampleAddon.LOGGER.atInfo().log(
                    "Endless Leveling owns the stat sheet here - registering bonuses through EL.");
        }

        // PlaceholderAPI: true only when PAPI is installed AND it accepted
        // Cultivation's registration. Check before registering an expansion of
        // your own under a colliding identifier.
        if (CultivationAPI.isPlaceholderApiRegistered()) {
            ExampleAddon.LOGGER.atInfo().log("Cultivation's PlaceholderAPI expansion is answering.");
        }

        // Marriage: what Partnered Cultivation is gated on. Without it, nothing
        // in CultivationConfigs.partner() has any effect.
        if (!CultivationAPI.isMarriageInstalled()) {
            ExampleAddon.LOGGER.atInfo().log("No Marriage mod - skipping the partnered-meditation bonus.");
        }
    }

    // ------------------------------------------------------------------
    // Stand-ins for this example's own logic.
    // ------------------------------------------------------------------

    private static boolean isAscensionLocked() { return false; }
    private static boolean isWarWindowOpen()   { return true; }
    private static boolean isDoubleQiWeekend() { return false; }
    private static boolean hasMastery(PlayerRef player, String techniqueId) { return false; }

    private static boolean rollRareDrop() {
        return Math.random() < ExampleAddon.get().getSettings().getRareDropChance();
    }
}
