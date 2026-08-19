package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Meridian.MeridianInjury;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Meridian Injury (see {@code MeridianInjuryComponent} and
 * {@code plugin.siren.Utils.Meridian.MeridianInjuryManager}) events - a named
 * injury being inflicted/deepened, cured, and a Cracked Dantian's Qi spill
 * being armed. See {@link CultivationEvents} for the conventions every
 * {@code *Events} class in this package shares.
 */
public final class MeridianEvents {
    private MeridianEvents(){}

    // --- Post-events ---

    /** An injury was inflicted (or deepened) - the write has already landed. */
    public record MeridianInjuryEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                      @Nonnull MeridianInjury injury, float magnitude, float durationSeconds) {}

    /** An injury fully cleared (wait-it-out, meditation recovery, or an explicit cure). */
    public record MeridianCureEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                    @Nonnull MeridianInjury injury, @Nonnull String cureSource) {}

    // --- Pre-events ---

    /**
     * An injury is about to be inflicted (or deepened, if already carried).
     * Cancel to refuse it outright; {@link #setMagnitude}/{@link #setDurationSeconds}
     * to re-scale the roll before it is written.
     */
    public static final class PreMeridianInjuryEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final MeridianInjury injury;
        private final String cause;
        private float magnitude;
        private float durationSeconds;

        public PreMeridianInjuryEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull MeridianInjury injury,
                                      @Nonnull String cause, float magnitude, float durationSeconds){
            this.ref = ref;
            this.player = player;
            this.injury = injury;
            this.cause = cause;
            this.magnitude = magnitude;
            this.durationSeconds = durationSeconds;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public MeridianInjury injury(){ return this.injury; }
        @Nonnull public String cause(){ return this.cause; }
        public float magnitude(){ return this.magnitude; }
        public void setMagnitude(float magnitude){ this.magnitude = magnitude; }
        public float durationSeconds(){ return this.durationSeconds; }
        public void setDurationSeconds(float durationSeconds){ this.durationSeconds = durationSeconds; }
    }

    /** An injury is about to be cured. Cancel to refuse it (it stays active). */
    public static final class PreMeridianCureEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final MeridianInjury injury;
        private final String cureSource;

        public PreMeridianCureEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull MeridianInjury injury,
                                    @Nonnull String cureSource){
            this.ref = ref;
            this.player = player;
            this.injury = injury;
            this.cureSource = cureSource;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public MeridianInjury injury(){ return this.injury; }
        @Nonnull public String cureSource(){ return this.cureSource; }
    }

    /**
     * A Cracked Dantian's Qi spill is about to be armed (fired once at
     * inflict/deepen, NOT per-tick). Cancel to cap future Qi gain at the new
     * ceiling without draining the excess already banked; {@link #setRatePerSecond}
     * to re-tune how fast the excess drains.
     */
    public static final class PreMeridianSpillEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final float excessQi;
        private float ratePerSecond;

        public PreMeridianSpillEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float excessQi, float ratePerSecond){
            this.ref = ref;
            this.player = player;
            this.excessQi = excessQi;
            this.ratePerSecond = ratePerSecond;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        public float excessQi(){ return this.excessQi; }
        public float ratePerSecond(){ return this.ratePerSecond; }
        public void setRatePerSecond(float ratePerSecond){ this.ratePerSecond = ratePerSecond; }
    }

    // --- Listener registration ---

    private static final List<Consumer<MeridianInjuryEvent>> INJURY = EventBus.newListenerList();
    private static final List<Consumer<PreMeridianInjuryEvent>> PRE_INJURY = EventBus.newListenerList();
    private static final List<Consumer<MeridianCureEvent>> CURE = EventBus.newListenerList();
    private static final List<Consumer<PreMeridianCureEvent>> PRE_CURE = EventBus.newListenerList();
    private static final List<Consumer<PreMeridianSpillEvent>> PRE_SPILL = EventBus.newListenerList();

    public static void onMeridianInjury(@Nonnull Consumer<MeridianInjuryEvent> listener){ INJURY.add(listener); }
    public static void onPreMeridianInjury(@Nonnull Consumer<PreMeridianInjuryEvent> listener){ PRE_INJURY.add(listener); }
    public static void onMeridianCure(@Nonnull Consumer<MeridianCureEvent> listener){ CURE.add(listener); }
    public static void onPreMeridianCure(@Nonnull Consumer<PreMeridianCureEvent> listener){ PRE_CURE.add(listener); }
    public static void onPreMeridianSpill(@Nonnull Consumer<PreMeridianSpillEvent> listener){ PRE_SPILL.add(listener); }

    // --- Internal dispatch (called by this mod's own systems/managers; not API) ---

    public static void fireMeridianInjury(@Nonnull MeridianInjuryEvent event){ EventBus.dispatch(INJURY, event, "MeridianInjuryEvent"); }
    public static boolean firePreMeridianInjury(@Nonnull PreMeridianInjuryEvent event){ return EventBus.fire(PRE_INJURY, event, "PreMeridianInjuryEvent"); }
    public static void fireMeridianCure(@Nonnull MeridianCureEvent event){ EventBus.dispatch(CURE, event, "MeridianCureEvent"); }
    public static boolean firePreMeridianCure(@Nonnull PreMeridianCureEvent event){ return EventBus.fire(PRE_CURE, event, "PreMeridianCureEvent"); }
    public static boolean firePreMeridianSpill(@Nonnull PreMeridianSpillEvent event){ return EventBus.fire(PRE_SPILL, event, "PreMeridianSpillEvent"); }
}
