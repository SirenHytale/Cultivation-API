package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Config.LifespanConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Events for Lifespan (寿元) - the per-profile online-play-hour budget keyed
 * to the highest realm ever reached. Mirrors {@link BodyTemperingEvents}'
 * shape: pre-events are cancellable/settable, post-events are pure readouts.
 *
 * <h2>Threading</h2>
 *
 * <p>Every listener here is called from {@code LifespanSystem}'s tick or from
 * a command handler on the world thread that owns the entity. Read through the
 * accessor you are given and never write to the Store from one - queue writes
 * on a {@code CommandBuffer}, the same rule every ticking system in this mod
 * follows.</p>
 */
public final class LifespanEvents {

    private LifespanEvents(){}

    /** Where an {@link LifespanExtendEvent}/{@link PreLifespanExtendEvent} came from. */
    public enum ExtendSource {
        /** A realm high-water-mark rise - purely informational (see {@code LifespanManager}'s own doc on why this skips the Pre event). */
        BREAKTHROUGH,
        /** The Longevity Pill. */
        PILL,
        /** {@code /cultivation admin extendlifespan}. */
        ADMIN
    }

    // --- Post events ---------------------------------------------------------

    /**
     * Bonus hours were actually banked (or, for {@link ExtendSource#BREAKTHROUGH},
     * the realm-mark budget rose). {@code hours} is what actually applied, after
     * any {@link PreLifespanExtendEvent} scaling.
     */
    public record LifespanExtendEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                      float hours, @Nonnull ExtendSource source) {}

    /** A cultivator's Lifespan budget hit 0 and they entered the Withering grace state. */
    public record LifespanWitherEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player) {}

    /**
     * A cultivator's Lifespan clock was reset to a fresh budget - a real
     * Reincarnation, a real Ascension, {@link LifespanConfig.ExpiryAction#NOTHING}
     * clearing Withering, or Mortal Passing's own reset (which additionally
     * fires {@link LifespanExpireEvent}).
     */
    public record LifespanRestoreEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player) {}

    // --- Pre events ----------------------------------------------------------

    /**
     * About to grant bonus hours ({@link ExtendSource#PILL}/{@link ExtendSource#ADMIN}
     * only - a {@link ExtendSource#BREAKTHROUGH} realm-mark rise has already
     * happened by the time Lifespan notices it and fires only the post event).
     * Cancel to refuse the grant outright, or scale {@link #setHours} to change
     * how much is actually banked.
     */
    public static final class PreLifespanExtendEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final ExtendSource source;
        private float hours;

        public PreLifespanExtendEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                      float hours, @Nonnull ExtendSource source){
            this.ref = ref;
            this.player = player;
            this.hours = hours;
            this.source = source;
        }

        @Nonnull
        public Ref<EntityStore> ref(){
            return ref;
        }

        @Nullable
        public PlayerRef player(){
            return player;
        }

        @Nonnull
        public ExtendSource source(){
            return source;
        }

        public float hours(){
            return hours;
        }

        /** Replaces the grant. Zero or less cancels it outright. */
        public void setHours(float hours){
            this.hours = hours;
        }
    }

    /**
     * A cultivator's Lifespan budget is about to reach 0 and enter Withering.
     * Cancelling here vetoes Withering entirely for this crossing - the clock
     * stays at 0 and this fires again the next tick, so an addon meaning to
     * grant a reprieve should also extend the budget (or it will simply be
     * asked again immediately).
     */
    public static final class PreLifespanExpireEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;

        public PreLifespanExpireEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player){
            this.ref = ref;
            this.player = player;
        }

        @Nonnull
        public Ref<EntityStore> ref(){
            return ref;
        }

        @Nullable
        public PlayerRef player(){
            return player;
        }
    }

    /**
     * A cultivator's Withering grace ran out and {@code Lifespan-Expiry-Action}
     * is about to run. NOT cancellable through this event - by the time this
     * fires the grace period has already elapsed; use {@link PreLifespanExpireEvent}
     * to veto Withering itself, further upstream.
     */
    public record LifespanExpireEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                      @Nonnull LifespanConfig.ExpiryAction action) {}

    // --- Registration --------------------------------------------------------

    private static final List<Consumer<LifespanExtendEvent>> EXTEND = EventBus.newListenerList();
    private static final List<Consumer<LifespanWitherEvent>> WITHER = EventBus.newListenerList();
    private static final List<Consumer<LifespanRestoreEvent>> RESTORE = EventBus.newListenerList();
    private static final List<Consumer<LifespanExpireEvent>> EXPIRE = EventBus.newListenerList();
    private static final List<Consumer<PreLifespanExtendEvent>> PRE_EXTEND = EventBus.newListenerList();
    private static final List<Consumer<PreLifespanExpireEvent>> PRE_EXPIRE = EventBus.newListenerList();

    public static void onExtend(@Nonnull Consumer<LifespanExtendEvent> listener){ EXTEND.add(listener); }
    public static void onWither(@Nonnull Consumer<LifespanWitherEvent> listener){ WITHER.add(listener); }
    public static void onRestore(@Nonnull Consumer<LifespanRestoreEvent> listener){ RESTORE.add(listener); }
    public static void onExpire(@Nonnull Consumer<LifespanExpireEvent> listener){ EXPIRE.add(listener); }
    public static void onPreExtend(@Nonnull Consumer<PreLifespanExtendEvent> listener){ PRE_EXTEND.add(listener); }
    public static void onPreExpire(@Nonnull Consumer<PreLifespanExpireEvent> listener){ PRE_EXPIRE.add(listener); }

    public static void fireExtend(@Nonnull LifespanExtendEvent event){ EventBus.dispatch(EXTEND, event, "LifespanExtendEvent"); }
    public static void fireWither(@Nonnull LifespanWitherEvent event){ EventBus.dispatch(WITHER, event, "LifespanWitherEvent"); }
    public static void fireRestore(@Nonnull LifespanRestoreEvent event){ EventBus.dispatch(RESTORE, event, "LifespanRestoreEvent"); }
    public static void fireExpire(@Nonnull LifespanExpireEvent event){ EventBus.dispatch(EXPIRE, event, "LifespanExpireEvent"); }

    /** @return whether Withering may proceed - false if a listener vetoed it. */
    public static boolean firePreExpire(@Nonnull PreLifespanExpireEvent event){
        return EventBus.fire(PRE_EXPIRE, event, "PreLifespanExpireEvent");
    }

    /**
     * Fires the pre-extend event and reports what survived it.
     *
     * @return the hours to actually grant, or 0 if a listener cancelled it, zeroed it, or left it a non-finite value (NaN/Infinity - a listener bug should not brick the profile's bonus hours forever).
     */
    public static float firePreExtend(@Nonnull PreLifespanExtendEvent event){
        if(!EventBus.fire(PRE_EXTEND, event, "PreLifespanExtendEvent")){
            return 0f;
        }

        float hours = event.hours();
        return Float.isFinite(hours) ? Math.max(0f, hours) : 0f;
    }
}
