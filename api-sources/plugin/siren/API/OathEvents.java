package plugin.siren.API;

import plugin.siren.Utils.Oath.OathType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Heavenly Oath (天道誓言) events - swearing, breaching, and cleansing the
 * Dao-Heart Flaw a breach leaves behind. See {@link CultivationEvents} for
 * the conventions every {@code *Events} class in this package shares.
 *
 * <p>Players are identified by UUID, the same convention {@link DuelEvents}
 * uses and for the same reason: an oath routinely outlives one participant's
 * session, and breach detection (a later slice) may run against a party who
 * is offline at the moment it fires. Resolve one with
 * {@code Universe.get().getPlayer(uuid)} and check {@code isValid()}.</p>
 */
public final class OathEvents {
    private OathEvents(){}

    /** How a Dao-Heart Flaw stopped being active - see {@code OathManager}'s three cleanse routes. */
    public enum FlawCleanseRoute {
        /** Its wall-clock timer simply ran out. */
        EXPIRED,
        /** A cleansing item was consumed. */
        ITEM,
        /** Meditating beside the party the broken oath was owed to burned it down early. */
        COMPANION
    }

    // --- Post-events ---

    /** A pending offer was accepted and the oath is now sworn (binding). */
    public record OathSwornEvent(@Nonnull String oathId, @Nonnull OathType type, @Nonnull UUID partyA,
                                  @Nonnull UUID partyB, int stake, @Nonnull String sectId) {}

    /** A sworn oath was broken and its penalty has already been applied. */
    public record OathBreachEvent(@Nonnull String oathId, @Nonnull OathType type, @Nonnull UUID breaker,
                                   @Nonnull UUID victim, float qiLost, float karmaGained) {}

    /** A cultivator's Dao-Heart Flaw was cleansed by {@code route}. */
    public record OathFlawCleanseEvent(@Nonnull UUID player, @Nonnull FlawCleanseRoute route) {}

    /** A sworn oath was peacefully DISSOLVED - no penalty, either via a mutual {@code /cultivation oath dissolve} or the one system-triggered no-fault case (see {@code OathManager#dissolveActiveOath}). */
    public record OathDissolveEvent(@Nonnull String oathId, @Nonnull OathType type, @Nonnull UUID partyA, @Nonnull UUID partyB) {}

    // --- Pre-events (cancellable; numbers are re-tunable) ---

    /** A pending offer is about to be accepted and become a binding oath. Cancel to refuse it - the offer is consumed either way, mirroring {@code DuelEvents.PreDuelStartEvent}, so the offerer must send a fresh one. */
    public static final class PreOathSwearEvent extends CancellableEvent {
        private final UUID offerer;
        private final UUID accepter;
        private final OathType type;
        private int stake;
        private final String sectId;

        public PreOathSwearEvent(@Nonnull UUID offerer, @Nonnull UUID accepter, @Nonnull OathType type,
                                 int stake, @Nonnull String sectId){
            this.offerer = offerer;
            this.accepter = accepter;
            this.type = type;
            this.stake = stake;
            this.sectId = sectId;
        }

        @Nonnull public UUID offerer(){ return this.offerer; }
        @Nonnull public UUID accepter(){ return this.accepter; }
        @Nonnull public OathType type(){ return this.type; }
        /** Qi each party is risking (WAGERED_DUEL only; 0 otherwise). */
        public int stake(){ return this.stake; }
        public void setStake(int stake){ this.stake = stake; }
        /** The sect loyalty is being sworn to (SECT_LOYALTY only; "" otherwise). */
        @Nonnull public String sectId(){ return this.sectId; }
    }

    /** A sworn oath is about to be recorded as broken and its penalty applied. Cancel to pardon the breach outright (nothing changes - no Qi loss, no karma, no flaw); adjust the setters to re-tune the penalty instead. */
    public static final class PreOathBreachEvent extends CancellableEvent {
        private final String oathId;
        private final OathType type;
        private final UUID breaker;
        private final UUID victim;
        private float qiLossPercent;
        private float karmaSwing;
        private float flawDurationMinutes;
        private float flawSeverity;

        public PreOathBreachEvent(@Nonnull String oathId, @Nonnull OathType type, @Nonnull UUID breaker, @Nonnull UUID victim,
                                  float qiLossPercent, float karmaSwing, float flawDurationMinutes, float flawSeverity){
            this.oathId = oathId;
            this.type = type;
            this.breaker = breaker;
            this.victim = victim;
            this.qiLossPercent = qiLossPercent;
            this.karmaSwing = karmaSwing;
            this.flawDurationMinutes = flawDurationMinutes;
            this.flawSeverity = flawSeverity;
        }

        @Nonnull public String oathId(){ return this.oathId; }
        @Nonnull public OathType type(){ return this.type; }
        @Nonnull public UUID breaker(){ return this.breaker; }
        @Nonnull public UUID victim(){ return this.victim; }
        /** Percent (0-100) of the breaker's CURRENT banked Qi about to be lost. */
        public float qiLossPercent(){ return this.qiLossPercent; }
        public void setQiLossPercent(float qiLossPercent){ this.qiLossPercent = qiLossPercent; }
        /** Karma about to be added to the breaker via {@code DaoComponent.addKarma}. */
        public float karmaSwing(){ return this.karmaSwing; }
        public void setKarmaSwing(float karmaSwing){ this.karmaSwing = karmaSwing; }
        /** Minutes the Dao-Heart Flaw is about to last. */
        public float flawDurationMinutes(){ return this.flawDurationMinutes; }
        public void setFlawDurationMinutes(float flawDurationMinutes){ this.flawDurationMinutes = flawDurationMinutes; }
        /** 0-100, snapshotted onto the flaw component; scales its Heart-Devil risk increase. */
        public float flawSeverity(){ return this.flawSeverity; }
        public void setFlawSeverity(float flawSeverity){ this.flawSeverity = flawSeverity; }
    }

    /** A Dao-Heart Flaw is about to be cleansed. Cancel to refuse the attempt (the item, if any, is still the caller's to decide whether to consume - see the call site). */
    public static final class PreOathFlawCleanseEvent extends CancellableEvent {
        private final UUID player;
        private final FlawCleanseRoute route;

        public PreOathFlawCleanseEvent(@Nonnull UUID player, @Nonnull FlawCleanseRoute route){
            this.player = player;
            this.route = route;
        }

        @Nonnull public UUID player(){ return this.player; }
        @Nonnull public FlawCleanseRoute route(){ return this.route; }
    }

    /** A sworn oath is about to be peacefully DISSOLVED. Cancel to keep it active (no change - the requesting side must ask again). */
    public static final class PreOathDissolveEvent extends CancellableEvent {
        private final String oathId;
        private final OathType type;
        private final UUID partyA;
        private final UUID partyB;

        public PreOathDissolveEvent(@Nonnull String oathId, @Nonnull OathType type, @Nonnull UUID partyA, @Nonnull UUID partyB){
            this.oathId = oathId;
            this.type = type;
            this.partyA = partyA;
            this.partyB = partyB;
        }

        @Nonnull public String oathId(){ return this.oathId; }
        @Nonnull public OathType type(){ return this.type; }
        @Nonnull public UUID partyA(){ return this.partyA; }
        @Nonnull public UUID partyB(){ return this.partyB; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PreOathSwearEvent>> PRE_SWEAR = EventBus.newListenerList();
    private static final List<Consumer<OathSwornEvent>> SWORN = EventBus.newListenerList();
    private static final List<Consumer<PreOathBreachEvent>> PRE_BREACH = EventBus.newListenerList();
    private static final List<Consumer<OathBreachEvent>> BREACH = EventBus.newListenerList();
    private static final List<Consumer<PreOathFlawCleanseEvent>> PRE_FLAW_CLEANSE = EventBus.newListenerList();
    private static final List<Consumer<OathFlawCleanseEvent>> FLAW_CLEANSE = EventBus.newListenerList();
    private static final List<Consumer<PreOathDissolveEvent>> PRE_DISSOLVE = EventBus.newListenerList();
    private static final List<Consumer<OathDissolveEvent>> DISSOLVE = EventBus.newListenerList();

    public static void onPreOathSwear(@Nonnull Consumer<PreOathSwearEvent> listener){ PRE_SWEAR.add(listener); }
    public static void onOathSworn(@Nonnull Consumer<OathSwornEvent> listener){ SWORN.add(listener); }
    public static void onPreOathBreach(@Nonnull Consumer<PreOathBreachEvent> listener){ PRE_BREACH.add(listener); }
    public static void onOathBreach(@Nonnull Consumer<OathBreachEvent> listener){ BREACH.add(listener); }
    public static void onPreOathFlawCleanse(@Nonnull Consumer<PreOathFlawCleanseEvent> listener){ PRE_FLAW_CLEANSE.add(listener); }
    public static void onOathFlawCleanse(@Nonnull Consumer<OathFlawCleanseEvent> listener){ FLAW_CLEANSE.add(listener); }
    public static void onPreOathDissolve(@Nonnull Consumer<PreOathDissolveEvent> listener){ PRE_DISSOLVE.add(listener); }
    public static void onOathDissolve(@Nonnull Consumer<OathDissolveEvent> listener){ DISSOLVE.add(listener); }

    // --- Internal dispatch (called by OathManager; not API) ---

    public static boolean firePreOathSwear(@Nonnull PreOathSwearEvent event){ return EventBus.fire(PRE_SWEAR, event, "PreOathSwearEvent"); }
    public static void fireOathSworn(@Nonnull OathSwornEvent event){ EventBus.dispatch(SWORN, event, "OathSwornEvent"); }
    public static boolean firePreOathBreach(@Nonnull PreOathBreachEvent event){ return EventBus.fire(PRE_BREACH, event, "PreOathBreachEvent"); }
    public static void fireOathBreach(@Nonnull OathBreachEvent event){ EventBus.dispatch(BREACH, event, "OathBreachEvent"); }
    public static boolean firePreOathFlawCleanse(@Nonnull PreOathFlawCleanseEvent event){ return EventBus.fire(PRE_FLAW_CLEANSE, event, "PreOathFlawCleanseEvent"); }
    public static void fireOathFlawCleanse(@Nonnull OathFlawCleanseEvent event){ EventBus.dispatch(FLAW_CLEANSE, event, "OathFlawCleanseEvent"); }
    public static boolean firePreOathDissolve(@Nonnull PreOathDissolveEvent event){ return EventBus.fire(PRE_DISSOLVE, event, "PreOathDissolveEvent"); }
    public static void fireOathDissolve(@Nonnull OathDissolveEvent event){ EventBus.dispatch(DISSOLVE, event, "OathDissolveEvent"); }
}
