package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Technique.Technique;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Technique (功法) events - performing arts, learning them, the Sword Flying
 * toggle, and the timed combat buffs several techniques grant. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares.
 *
 * <p>These fire for third-party techniques registered through
 * {@code CultivationAPI.registerTechnique} exactly as they do for the built-in
 * ones - the gate/cost/cooldown pipeline is shared.</p>
 */
public final class TechniqueEvents {
    private TechniqueEvents(){}

    /** Which timed buff a technique granted. */
    public enum BuffType {
        /** Iron Body - flat incoming-damage reduction. */
        IRON_BODY,
        /** Qi Infusion - outgoing damage bonus. */
        QI_INFUSION,
        /** Qi Barrier - an absorbing shield pool. */
        QI_BARRIER,
        /** Cloud Step - a movement speed multiplier. */
        CLOUD_STEP
    }

    /** Why a cultivator came down from sword flight. */
    public enum FlightStopReason {
        /** They toggled it off themselves. */
        TOGGLE,
        /** Their Qi ran out mid-flight. */
        QI_EXHAUSTED,
        /** They died; flight is cleaned up defensively. */
        DEATH
    }

    // --- Post-events ---

    /** A technique was performed: the Qi is spent, the cooldown stamped, and the effect has run. */
    public record TechniquePerformEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                        @Nonnull Technique technique, float qiCost) {}

    /** A cultivator learned a technique for good (from a manual). Sect-taught arts are resolved live and never fire this - listen for {@link SectEvents}' inscription events instead. */
    public record TechniqueLearnEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull String techniqueId) {}

    /** A cultivator took to the sky on their sword. */
    public record SwordFlightStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                        float horizontalSpeed, float verticalSpeed) {}

    /** A cultivator came down; their mount (if any) has already despawned. */
    public record SwordFlightStopEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull FlightStopReason reason) {}

    /** A timed technique buff was applied. {@code magnitude} means whatever that buff measures - a reduction percent, a damage percent, a shield pool, a speed multiplier. */
    public record TechniqueBuffApplyEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                          @Nonnull BuffType type, float durationSeconds, float magnitude) {}

    /** Cloud Step's speed multiplier was reverted, either on expiry or on cleanup. */
    public record TechniqueBuffExpireEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull BuffType type) {}

    // --- Pre-events ---

    /** A technique is about to be performed - every gate has already passed. Cancel to refuse it silently (no Qi spent, no cooldown); {@link #setQiCost} and {@link #setCooldownSeconds} re-price this one performance without touching the config. */
    public static final class PreTechniquePerformEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final Technique technique;
        private float qiCost;
        private float cooldownSeconds;

        public PreTechniquePerformEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                        @Nonnull Technique technique, float qiCost, float cooldownSeconds){
            this.ref = ref;
            this.player = player;
            this.technique = technique;
            this.qiCost = qiCost;
            this.cooldownSeconds = cooldownSeconds;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public Technique technique(){ return this.technique; }
        /** Banked Qi this performance will spend. */
        public float qiCost(){ return this.qiCost; }
        public void setQiCost(float qiCost){ this.qiCost = qiCost; }
        /** Seconds before it can be performed again; 0 or less stamps no cooldown. */
        public float cooldownSeconds(){ return this.cooldownSeconds; }
        public void setCooldownSeconds(float cooldownSeconds){ this.cooldownSeconds = cooldownSeconds; }
    }

    /** A technique is about to be learned. Cancel to refuse it (the manual is consumed either way, matching how a manual for an already-known art is spent). */
    public static final class PreTechniqueLearnEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String techniqueId;

        public PreTechniqueLearnEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull String techniqueId){
            this.ref = ref;
            this.player = player;
            this.techniqueId = techniqueId;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public String techniqueId(){ return this.techniqueId; }
    }

    /** A cultivator is about to take flight. Cancel to keep them grounded; the speed setters re-tune how fast this flight is. */
    public static final class PreSwordFlightStartEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private float horizontalSpeed;
        private float verticalSpeed;

        public PreSwordFlightStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                        float horizontalSpeed, float verticalSpeed){
            this.ref = ref;
            this.player = player;
            this.horizontalSpeed = horizontalSpeed;
            this.verticalSpeed = verticalSpeed;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        public float horizontalSpeed(){ return this.horizontalSpeed; }
        public void setHorizontalSpeed(float horizontalSpeed){ this.horizontalSpeed = horizontalSpeed; }
        public float verticalSpeed(){ return this.verticalSpeed; }
        public void setVerticalSpeed(float verticalSpeed){ this.verticalSpeed = verticalSpeed; }
    }

    /** A cultivator is about to come down. Cancel to keep them airborne - safe for TOGGLE, but cancelling a DEATH stop leaves flight state on a corpse, so gate on {@link #reason()}. */
    public static final class PreSwordFlightStopEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final FlightStopReason reason;

        public PreSwordFlightStopEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull FlightStopReason reason){
            this.ref = ref;
            this.player = player;
            this.reason = reason;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public FlightStopReason reason(){ return this.reason; }
    }

    /** A timed technique buff is about to be applied. Cancel to deny it; the setters re-tune how long and how strong it is. */
    public static final class PreTechniqueBuffApplyEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final BuffType type;
        private float durationSeconds;
        private float magnitude;

        public PreTechniqueBuffApplyEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                          @Nonnull BuffType type, float durationSeconds, float magnitude){
            this.ref = ref;
            this.player = player;
            this.type = type;
            this.durationSeconds = durationSeconds;
            this.magnitude = magnitude;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public BuffType type(){ return this.type; }
        public float durationSeconds(){ return this.durationSeconds; }
        public void setDurationSeconds(float durationSeconds){ this.durationSeconds = durationSeconds; }
        /** Whatever this buff measures - a reduction percent, a damage percent, a shield pool, a speed multiplier. */
        public float magnitude(){ return this.magnitude; }
        public void setMagnitude(float magnitude){ this.magnitude = magnitude; }
    }

    /** An art's mastery rose a rung. */
    public record TechniqueMasteryAdvanceEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                               @Nonnull String techniqueId, int stage) {}

    /**
     * An art is about to rise a rung. Cancel to hold it where it is - the XP and
     * the studied manuals stay banked, so it simply tries again the next time
     * anything about it changes.
     */
    public static final class PreTechniqueMasteryAdvanceEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String techniqueId;
        private final int stage;

        public PreTechniqueMasteryAdvanceEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                               @Nonnull String techniqueId, int stage){
            this.ref = ref;
            this.player = player;
            this.techniqueId = techniqueId;
            this.stage = stage;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public String techniqueId(){ return this.techniqueId; }
        /** The rung it is about to reach, 0-based. */
        public int stage(){ return this.stage; }
    }

    // --- Listener registration ---

    private static final List<Consumer<TechniquePerformEvent>> PERFORM = EventBus.newListenerList();
    private static final List<Consumer<PreTechniquePerformEvent>> PRE_PERFORM = EventBus.newListenerList();
    private static final List<Consumer<TechniqueLearnEvent>> LEARN = EventBus.newListenerList();
    private static final List<Consumer<PreTechniqueLearnEvent>> PRE_LEARN = EventBus.newListenerList();
    private static final List<Consumer<SwordFlightStartEvent>> FLIGHT_START = EventBus.newListenerList();
    private static final List<Consumer<PreSwordFlightStartEvent>> PRE_FLIGHT_START = EventBus.newListenerList();
    private static final List<Consumer<SwordFlightStopEvent>> FLIGHT_STOP = EventBus.newListenerList();
    private static final List<Consumer<PreSwordFlightStopEvent>> PRE_FLIGHT_STOP = EventBus.newListenerList();
    private static final List<Consumer<TechniqueBuffApplyEvent>> BUFF_APPLY = EventBus.newListenerList();
    private static final List<Consumer<PreTechniqueBuffApplyEvent>> PRE_BUFF_APPLY = EventBus.newListenerList();
    private static final List<Consumer<TechniqueBuffExpireEvent>> BUFF_EXPIRE = EventBus.newListenerList();
    private static final List<Consumer<TechniqueMasteryAdvanceEvent>> MASTERY_ADVANCE = EventBus.newListenerList();
    private static final List<Consumer<PreTechniqueMasteryAdvanceEvent>> PRE_MASTERY_ADVANCE = EventBus.newListenerList();

    public static void onTechniquePerform(@Nonnull Consumer<TechniquePerformEvent> listener){ PERFORM.add(listener); }
    public static void onPreTechniquePerform(@Nonnull Consumer<PreTechniquePerformEvent> listener){ PRE_PERFORM.add(listener); }
    public static void onTechniqueLearn(@Nonnull Consumer<TechniqueLearnEvent> listener){ LEARN.add(listener); }
    public static void onPreTechniqueLearn(@Nonnull Consumer<PreTechniqueLearnEvent> listener){ PRE_LEARN.add(listener); }
    public static void onSwordFlightStart(@Nonnull Consumer<SwordFlightStartEvent> listener){ FLIGHT_START.add(listener); }
    public static void onPreSwordFlightStart(@Nonnull Consumer<PreSwordFlightStartEvent> listener){ PRE_FLIGHT_START.add(listener); }
    public static void onSwordFlightStop(@Nonnull Consumer<SwordFlightStopEvent> listener){ FLIGHT_STOP.add(listener); }
    public static void onPreSwordFlightStop(@Nonnull Consumer<PreSwordFlightStopEvent> listener){ PRE_FLIGHT_STOP.add(listener); }
    public static void onTechniqueBuffApply(@Nonnull Consumer<TechniqueBuffApplyEvent> listener){ BUFF_APPLY.add(listener); }
    public static void onPreTechniqueBuffApply(@Nonnull Consumer<PreTechniqueBuffApplyEvent> listener){ PRE_BUFF_APPLY.add(listener); }
    public static void onTechniqueBuffExpire(@Nonnull Consumer<TechniqueBuffExpireEvent> listener){ BUFF_EXPIRE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireTechniquePerform(@Nonnull TechniquePerformEvent event){ EventBus.dispatch(PERFORM, event, "TechniquePerformEvent"); }
    public static boolean firePreTechniquePerform(@Nonnull PreTechniquePerformEvent event){ return EventBus.fire(PRE_PERFORM, event, "PreTechniquePerformEvent"); }
    public static void fireTechniqueLearn(@Nonnull TechniqueLearnEvent event){ EventBus.dispatch(LEARN, event, "TechniqueLearnEvent"); }
    public static boolean firePreTechniqueLearn(@Nonnull PreTechniqueLearnEvent event){ return EventBus.fire(PRE_LEARN, event, "PreTechniqueLearnEvent"); }
    public static void fireSwordFlightStart(@Nonnull SwordFlightStartEvent event){ EventBus.dispatch(FLIGHT_START, event, "SwordFlightStartEvent"); }
    public static boolean firePreSwordFlightStart(@Nonnull PreSwordFlightStartEvent event){ return EventBus.fire(PRE_FLIGHT_START, event, "PreSwordFlightStartEvent"); }
    public static void fireSwordFlightStop(@Nonnull SwordFlightStopEvent event){ EventBus.dispatch(FLIGHT_STOP, event, "SwordFlightStopEvent"); }
    public static boolean firePreSwordFlightStop(@Nonnull PreSwordFlightStopEvent event){ return EventBus.fire(PRE_FLIGHT_STOP, event, "PreSwordFlightStopEvent"); }
    public static void fireTechniqueBuffApply(@Nonnull TechniqueBuffApplyEvent event){ EventBus.dispatch(BUFF_APPLY, event, "TechniqueBuffApplyEvent"); }
    public static boolean firePreTechniqueBuffApply(@Nonnull PreTechniqueBuffApplyEvent event){ return EventBus.fire(PRE_BUFF_APPLY, event, "PreTechniqueBuffApplyEvent"); }
    public static void fireTechniqueBuffExpire(@Nonnull TechniqueBuffExpireEvent event){ EventBus.dispatch(BUFF_EXPIRE, event, "TechniqueBuffExpireEvent"); }

    public static void onTechniqueMasteryAdvance(@Nonnull Consumer<TechniqueMasteryAdvanceEvent> listener){ MASTERY_ADVANCE.add(listener); }
    public static void onPreTechniqueMasteryAdvance(@Nonnull Consumer<PreTechniqueMasteryAdvanceEvent> listener){ PRE_MASTERY_ADVANCE.add(listener); }

    public static void fireTechniqueMasteryAdvance(@Nonnull TechniqueMasteryAdvanceEvent event){ EventBus.dispatch(MASTERY_ADVANCE, event, "TechniqueMasteryAdvanceEvent"); }
    public static boolean firePreTechniqueMasteryAdvance(@Nonnull PreTechniqueMasteryAdvanceEvent event){ return EventBus.fire(PRE_MASTERY_ADVANCE, event, "PreTechniqueMasteryAdvanceEvent"); }
}
