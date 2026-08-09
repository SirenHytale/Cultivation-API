package com.example.exampleaddon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.API.BeastEvents;
import plugin.siren.API.CelestialEvents;
import plugin.siren.API.CultivationAPI;
import plugin.siren.API.CultivationConfigs;
import plugin.siren.API.CultivationEvents;
import plugin.siren.API.DaoEvents;
import plugin.siren.API.ItemEvents;
import plugin.siren.API.SectEvents;
import plugin.siren.API.StoreBenefit;
import plugin.siren.API.StoreBenefitEvents;
import plugin.siren.API.TechniqueEvents;
import plugin.siren.API.WarEvents;
import plugin.siren.ECS.Dao.CultivationPath;
import plugin.siren.ECS.Realms.CultivationRealm;
import plugin.siren.Utils.Celestial.CelestialEventType;

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
 * <p><b>Two 0.8.0 classes break that rule</b>, because neither has a subject
 * player. {@code StoreBenefitEvents} fires on the remote checker thread with only
 * a UUID in hand, for a player who is often offline - see
 * {@link #applyOnTheirOwnThread}. {@code CelestialEvents} is server-wide: still on
 * a world thread, but whichever one reached the shared scheduler first, and with
 * no {@code ref()} in the payload at all.</p>
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
        registerCelestial();
        registerStoreBenefits();
        registerCompatibility();
    }

    /**
     * Almost nothing here needs undoing - listeners have no unregister by design.
     * The one exception is the store benefit, which is a registry entry rather
     * than a listener, so a mod unloading cleanly hands it back.
     */
    public static void unregisterAll() {
        CultivationAPI.unregisterStoreBenefit(CROWN_KEY);
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

        // 0.8.0: a sect renaming its abbreviation. oldAbbreviation() is empty for
        // a sect that never had one.
        SectEvents.onSectAbbreviationChange(event ->
                ExampleAddon.LOGGER.atInfo().log("Sect abbreviation: '%s' -> '%s'",
                        event.oldAbbreviation(), event.newAbbreviation()));

        // 0.8.0: the Ascension Capstone - the end of the ladder. Deliberately NOT
        // a BreakthroughEvent, so a listener counting rank-ups does not credit one
        // here; decide separately what an ascension is worth.
        CultivationEvents.onAscension(event -> {
            // ascensionCount() INCLUDES this one. If prestiged() is true the player
            // has ALREADY been reset to the first realm by the time this fires, so
            // reading their realm here shows the bottom of the ladder, not the top.
            // Capture pre-ascension standing in onPreAscension instead.
            ExampleAddon.LOGGER.atInfo().log("Ascension #%d (prestiged=%b).",
                    event.ascensionCount(), event.prestiged());
        });

        CultivationEvents.onAscensionFailed(event ->
                ExampleAddon.LOGGER.atInfo().log("Ascension failed (abandoned=%b).", event.abandoned()));

        // 0.8.0: two arts melded into a third. This does NOT say whether the
        // parents were consumed - the removals fire no event of their own, so
        // re-read a player's learned set rather than applying a delta.
        TechniqueEvents.onTechniqueFusion(event ->
                ExampleAddon.LOGGER.atInfo().log("Fusion '%s' produced '%s'.",
                        event.fusionId(), event.resultTechniqueId()));
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
            if (isBreakthroughLocked()) {
                event.setCancelled(true);
            }
        });

        // 0.8.0: gate the END of the ladder behind something of your own - the one
        // reason this pre-event exists. Cancelling leaves them at the peak untried.
        CultivationEvents.onPreAscension(event -> {
            if (!hasHeavenlyMandate(event.player())) {
                event.setCancelled(true);
            }
        });

        // Make a ritual unbreakable by walking away (but still cancellable by
        // the player's own /cultivation meditate).
        //
        // ALWAYS switch on the reason. 0.8.0 added a third value, RITUAL_COMPLETE
        // (rising from a seat a rank was just earned in), and a blanket
        // setCancelled(true) would pin players to the ground after a breakthrough.
        // That reason is also the one that fires AFTER the change it reports, so
        // cancelling it cannot undo the breakthrough - only leave them seated.
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

        // 0.8.0: re-tune one fusion attempt. Every gate has already passed by the
        // time this fires (both parents mastered, the realm floor, the Qi, the
        // cooldown), and the failure roll happens AFTER the Qi is spent and the
        // cooldown stamped - so setFailureChancePercent changes the odds, not the
        // price of losing.
        TechniqueEvents.onPreTechniqueFusion(event -> {
            if (isDoubleQiWeekend()) {
                event.setQiCost(event.qiCost() * 0.5f);
                event.setConsumeParents(false);   // let them keep both parents
                event.setFailureChancePercent(0f);
            }
        });
    }

    // ------------------------------------------------------------------
    // 0.8.0: celestial events. Server-wide, so there is NO subject player -
    // the payload carries a CelestialEventType rather than a ref(), and the
    // listener runs on whichever world thread reached the shared scheduler
    // first. Do not block, and hop per player before touching anybody.
    // ------------------------------------------------------------------

    private static void registerCelestial() {
        // One pair of hooks covers every type, built-in or addon-registered, so
        // switch on the id the way an advancement listener switches on the stage.
        CelestialEvents.onCelestialEventStart(event ->
                ExampleAddon.LOGGER.atInfo().log("Celestial event '%s' began, ends at %d.",
                        event.type().id(), event.endsAtMillis()));

        CelestialEvents.onCelestialEventEnd(event ->
                ExampleAddon.LOGGER.atInfo().log("Celestial event '%s' ended.", event.type().id()));

        // Cancelling skips THIS pick entirely rather than substituting another
        // event, so a listener that vetoes everything leaves nothing running.
        CelestialEvents.onPreCelestialEventStart(event -> {
            if (event.forcedByAdmin()) {
                return;   // never second-guess an explicit /celestial start
            }

            if (CelestialEventType.BLOOD_MOON_ID.equals(event.type().id()) && isWarWindowOpen()) {
                event.setCancelled(true);   // no Blood Moon during the siege window
                return;
            }

            event.setDurationMinutes(event.durationMinutes() * 1.5f);
        });
    }

    // ------------------------------------------------------------------
    // 0.8.0: Treasure Pavilion benefits. The ONE event family in this API that
    // does not run on a world thread - see the hops below.
    // ------------------------------------------------------------------

    /**
     * The store's product slug, kept in one place. This is NOT the registry key
     * below it: {@code hasStoreBenefit} is queried by SLUG, and passing the key
     * instead returns false forever without a word.
     */
    private static final String CROWN_SLUG = "example-addon-crown";
    private static final String CROWN_KEY = "ExampleAddon:store:crown";

    private static void registerStoreBenefits() {
        // Registering gets the HTTP sweep, the caching, the operator's config
        // switches and /cultivation store recheck for free. Before or after the
        // sync has started both work - a late registration is fetched at once.
        CultivationAPI.registerStoreBenefit(
                StoreBenefit.builder(CROWN_KEY, CROWN_SLUG)
                        .name("server.exampleAddon.store.crown")
                        // Also registers a title, unlocked for exactly the owners
                        // and shown greyed to everybody else.
                        .title("server.exampleAddon.title.crown")
                        .build());

        StoreBenefitEvents.onBenefitGranted(event -> {
            // Bookkeeping may run right here. Touching the player may not: this is
            // the remote checker thread, the payload is a bare UUID, and the
            // player is very often offline - which is the NORMAL case, not an edge
            // case, so doing nothing has to be a correct outcome.
            ExampleAddon.LOGGER.atInfo().log("Store benefit granted: %s to %s",
                    event.benefit().getKey(), event.playerUuid());

            applyOnTheirOwnThread(event.playerUuid());
        });

        StoreBenefitEvents.onBenefitRevoked(event ->
                ExampleAddon.LOGGER.atInfo().log("Store benefit revoked: %s from %s",
                        event.benefit().getKey(), event.playerUuid()));

        // A failed fetch KEEPS the previous list rather than clearing it, so a
        // network blip never mass-revokes. Check this before treating a sweep as
        // authoritative for anything you mirror into your own storage.
        StoreBenefitEvents.onSyncCompleted(event -> {
            if (!event.failedProducts().isEmpty()) {
                ExampleAddon.LOGGER.atWarning().log("Store sweep incomplete: %s failed.",
                        event.failedProducts());
            }
        });
    }

    /** The hop a store-benefit listener has to make before reading a component. */
    private static void applyOnTheirOwnThread(java.util.UUID playerUuid) {
        PlayerRef player = Universe.get().getPlayer(playerUuid);
        if (player == null || !player.isValid()) {
            return;   // offline - re-derive on their next join instead
        }

        World world = Universe.get().getWorld(player.getWorldUuid());
        if (world == null) {
            return;
        }

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                return;
            }

            // Queried by SLUG, never by CROWN_KEY. And a false answer means "do
            // not apply this" - it is also what a disabled system, a disabled
            // product, or a sweep that has not landed looks like - so never
            // persist it as "they did not buy it".
            if (CultivationAPI.hasStoreBenefit(playerUuid, CROWN_SLUG)) {
                ExampleAddon.LOGGER.atInfo().log("Crown confirmed for %s.", playerUuid);
            }
        });
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

    private static boolean isBreakthroughLocked() { return false; }
    private static boolean isWarWindowOpen()      { return true; }
    private static boolean isDoubleQiWeekend()    { return false; }
    private static boolean hasMastery(PlayerRef player, String techniqueId) { return false; }
    private static boolean hasHeavenlyMandate(PlayerRef player) { return true; }

    private static boolean rollRareDrop() {
        return Math.random() < ExampleAddon.get().getSettings().getRareDropChance();
    }
}
