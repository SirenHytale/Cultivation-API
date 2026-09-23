package plugin.siren.API;

import plugin.siren.ECS.Realms.CultivationRealm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Nascent Soul Escape (元婴遁走) events - the fatal-blow reprieve, the chase,
 * and its four resolutions. Exactly the {@link OathEvents} shape: post-events
 * are records, pre-events are {@code static final class X extends
 * CancellableEvent} with settable tuning fields, one {@link
 * EventBus#newListenerList()} per event, {@code onX(Consumer<X>)}
 * registration, internal {@code firePreX}/{@code fireX} dispatchers. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class
 * in this package shares.
 *
 * <p>Players are identified by UUID, matching {@link OathEvents} and {@link
 * DuelEvents}, because a soul's killer may be offline by the time the
 * session resolves - resolve one with {@code Universe.get().getPlayer(uuid)}
 * and check {@code isValid()}.</p>
 *
 * <p>M-049 applies: whatever internal path fires these must fire ALL of a
 * transition's steps. There is no public facade for beginning an escape in
 * this design - if one is ever added, it must call {@code
 * SoulEscapeManager.begin} and nothing less.</p>
 */
public final class SoulEscapeEvents {
    private SoulEscapeEvents(){}

    /** Mirrors {@code SoulSanctuary.Kind} - kept as its own enum here so this event never has to import Utils.SoulEscape just to name a sanctuary. */
    public enum SanctuaryKind { ABODE, SECT_HALL, FORMATION }

    /** How a fleeing soul's window ended without ever resolving into a survive/extinguish/timeout. */
    public enum ForfeitReason { DISCONNECT, RELOG_RESOLVED, WORLD_CHANGE, PLUGIN_RELOAD }

    // --- Pre-events (cancellable; numbers re-tunable) ---

    /** The fatal blow is about to be cancelled and the soul about to start fleeing. Cancel -&gt; no escape, nothing charged, the blow stays fatal. */
    public static final class PreSoulEscapeBeginEvent extends CancellableEvent {
        private final UUID soul;
        @Nullable private final UUID killer;
        private final CultivationRealm realm;
        private float windowSeconds;
        private float qiCostPercent;
        private float lifespanHours;

        public PreSoulEscapeBeginEvent(@Nonnull UUID soul, @Nullable UUID killer, @Nonnull CultivationRealm realm,
                                       float windowSeconds, float qiCostPercent, float lifespanHours){
            this.soul = soul;
            this.killer = killer;
            this.realm = realm;
            this.windowSeconds = windowSeconds;
            this.qiCostPercent = qiCostPercent;
            this.lifespanHours = lifespanHours;
        }

        @Nonnull public UUID soul(){ return this.soul; }
        @Nullable public UUID killer(){ return this.killer; }
        @Nonnull public CultivationRealm realm(){ return this.realm; }
        public float windowSeconds(){ return this.windowSeconds; }
        public void setWindowSeconds(float windowSeconds){ this.windowSeconds = windowSeconds; }
        public float qiCostPercent(){ return this.qiCostPercent; }
        public void setQiCostPercent(float qiCostPercent){ this.qiCostPercent = qiCostPercent; }
        public float lifespanHours(){ return this.lifespanHours; }
        public void setLifespanHours(float lifespanHours){ this.lifespanHours = lifespanHours; }
    }

    /** A blow just landed on a fleeing soul. Cancel -&gt; this blow does nothing; the soul keeps fleeing. */
    public static final class PreSoulExtinguishEvent extends CancellableEvent {
        private final UUID soul;
        private final UUID extinguisher;
        private final int hitsSoFar;
        private final int hitsRequired;
        private float extraKarma;
        private float extraYinShift;

        public PreSoulExtinguishEvent(@Nonnull UUID soul, @Nonnull UUID extinguisher, int hitsSoFar, int hitsRequired,
                                      float extraKarma, float extraYinShift){
            this.soul = soul;
            this.extinguisher = extinguisher;
            this.hitsSoFar = hitsSoFar;
            this.hitsRequired = hitsRequired;
            this.extraKarma = extraKarma;
            this.extraYinShift = extraYinShift;
        }

        @Nonnull public UUID soul(){ return this.soul; }
        @Nonnull public UUID extinguisher(){ return this.extinguisher; }
        public int hitsSoFar(){ return this.hitsSoFar; }
        public int hitsRequired(){ return this.hitsRequired; }
        public float extraKarma(){ return this.extraKarma; }
        public void setExtraKarma(float extraKarma){ this.extraKarma = extraKarma; }
        public float extraYinShift(){ return this.extraYinShift; }
        public void setExtraYinShift(float extraYinShift){ this.extraYinShift = extraYinShift; }
    }

    /** A fleeing soul just entered sanctuary. Cancel -&gt; the sanctuary does not count; the session is re-inserted and the soul keeps fleeing. */
    public static final class PreSoulSurviveEvent extends CancellableEvent {
        private final UUID soul;
        @Nullable private final UUID killer;
        private final SanctuaryKind kind;
        private final String world;
        private final int chunkX;
        private final int chunkZ;
        private float restoreHealthPercent;
        private float weakenedMinutes;
        private float spareMerit;

        public PreSoulSurviveEvent(@Nonnull UUID soul, @Nullable UUID killer, @Nonnull SanctuaryKind kind,
                                   @Nonnull String world, int chunkX, int chunkZ,
                                   float restoreHealthPercent, float weakenedMinutes, float spareMerit){
            this.soul = soul;
            this.killer = killer;
            this.kind = kind;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.restoreHealthPercent = restoreHealthPercent;
            this.weakenedMinutes = weakenedMinutes;
            this.spareMerit = spareMerit;
        }

        @Nonnull public UUID soul(){ return this.soul; }
        @Nullable public UUID killer(){ return this.killer; }
        @Nonnull public SanctuaryKind kind(){ return this.kind; }
        @Nonnull public String world(){ return this.world; }
        public int chunkX(){ return this.chunkX; }
        public int chunkZ(){ return this.chunkZ; }
        public float restoreHealthPercent(){ return this.restoreHealthPercent; }
        public void setRestoreHealthPercent(float restoreHealthPercent){ this.restoreHealthPercent = restoreHealthPercent; }
        public float weakenedMinutes(){ return this.weakenedMinutes; }
        public void setWeakenedMinutes(float weakenedMinutes){ this.weakenedMinutes = weakenedMinutes; }
        public float spareMerit(){ return this.spareMerit; }
        public void setSpareMerit(float spareMerit){ this.spareMerit = spareMerit; }
    }

    // --- Post-events ---

    public record SoulEscapeBeginEvent(@Nonnull UUID soul, @Nullable UUID killer, @Nonnull CultivationRealm realm,
                                       float windowSeconds, float qiSpent, float lifespanHoursSpent) {}

    public record SoulEscapeSurviveEvent(@Nonnull UUID soul, @Nullable UUID killer, @Nonnull SanctuaryKind kind,
                                         @Nonnull String world, int chunkX, int chunkZ,
                                         float weakenedMinutes, float meritToKiller) {}

    public record SoulEscapeExtinguishEvent(@Nonnull UUID soul, @Nonnull UUID extinguisher,
                                            float extraKarma, float extraYinShift) {}

    public record SoulEscapeTimeoutEvent(@Nonnull UUID soul, @Nullable UUID killer) {}

    public record SoulEscapeForfeitEvent(@Nonnull UUID soul, @Nullable UUID killer, @Nonnull ForfeitReason reason) {}

    // --- Listener registration ---

    private static final List<Consumer<PreSoulEscapeBeginEvent>> PRE_BEGIN = EventBus.newListenerList();
    private static final List<Consumer<SoulEscapeBeginEvent>> BEGIN = EventBus.newListenerList();
    private static final List<Consumer<PreSoulExtinguishEvent>> PRE_EXTINGUISH = EventBus.newListenerList();
    private static final List<Consumer<SoulEscapeExtinguishEvent>> EXTINGUISH = EventBus.newListenerList();
    private static final List<Consumer<PreSoulSurviveEvent>> PRE_SURVIVE = EventBus.newListenerList();
    private static final List<Consumer<SoulEscapeSurviveEvent>> SURVIVE = EventBus.newListenerList();
    private static final List<Consumer<SoulEscapeTimeoutEvent>> TIMEOUT = EventBus.newListenerList();
    private static final List<Consumer<SoulEscapeForfeitEvent>> FORFEIT = EventBus.newListenerList();

    public static void onPreSoulEscapeBegin(@Nonnull Consumer<PreSoulEscapeBeginEvent> listener){ PRE_BEGIN.add(listener); }
    public static void onSoulEscapeBegin(@Nonnull Consumer<SoulEscapeBeginEvent> listener){ BEGIN.add(listener); }
    public static void onPreSoulExtinguish(@Nonnull Consumer<PreSoulExtinguishEvent> listener){ PRE_EXTINGUISH.add(listener); }
    public static void onSoulEscapeExtinguish(@Nonnull Consumer<SoulEscapeExtinguishEvent> listener){ EXTINGUISH.add(listener); }
    public static void onPreSoulSurvive(@Nonnull Consumer<PreSoulSurviveEvent> listener){ PRE_SURVIVE.add(listener); }
    public static void onSoulEscapeSurvive(@Nonnull Consumer<SoulEscapeSurviveEvent> listener){ SURVIVE.add(listener); }
    public static void onSoulEscapeTimeout(@Nonnull Consumer<SoulEscapeTimeoutEvent> listener){ TIMEOUT.add(listener); }
    public static void onSoulEscapeForfeit(@Nonnull Consumer<SoulEscapeForfeitEvent> listener){ FORFEIT.add(listener); }

    // --- Internal dispatch (called by SoulEscapeManager; not API) ---

    public static boolean firePreSoulEscapeBegin(@Nonnull PreSoulEscapeBeginEvent event){ return EventBus.fire(PRE_BEGIN, event, "PreSoulEscapeBeginEvent"); }
    public static void fireSoulEscapeBegin(@Nonnull SoulEscapeBeginEvent event){ EventBus.dispatch(BEGIN, event, "SoulEscapeBeginEvent"); }
    public static boolean firePreSoulExtinguish(@Nonnull PreSoulExtinguishEvent event){ return EventBus.fire(PRE_EXTINGUISH, event, "PreSoulExtinguishEvent"); }
    public static void fireSoulEscapeExtinguish(@Nonnull SoulEscapeExtinguishEvent event){ EventBus.dispatch(EXTINGUISH, event, "SoulEscapeExtinguishEvent"); }
    public static boolean firePreSoulSurvive(@Nonnull PreSoulSurviveEvent event){ return EventBus.fire(PRE_SURVIVE, event, "PreSoulSurviveEvent"); }
    public static void fireSoulEscapeSurvive(@Nonnull SoulEscapeSurviveEvent event){ EventBus.dispatch(SURVIVE, event, "SoulEscapeSurviveEvent"); }
    public static void fireSoulEscapeTimeout(@Nonnull SoulEscapeTimeoutEvent event){ EventBus.dispatch(TIMEOUT, event, "SoulEscapeTimeoutEvent"); }
    public static void fireSoulEscapeForfeit(@Nonnull SoulEscapeForfeitEvent event){ EventBus.dispatch(FORFEIT, event, "SoulEscapeForfeitEvent"); }
}
