package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Events for the fist art (拳道) - the ladder climbed by striking with nothing
 * in your hands.
 *
 * <h2>Why an addon would care</h2>
 *
 * <p>This is the one Cultivation track whose income is damage the player
 * <em>dealt bare-handed</em>, which makes it the natural hook for a monk school,
 * a no-weapons challenge mode, or anything that wants to reward going unarmed.
 * It is the mirror of {@link BodyTemperingEvents}, whose income is damage
 * received - together they are the two tracks a cultivator can climb without
 * ever gathering a point of Qi.</p>
 *
 * <p>{@link PreXpGainEvent} can scale or refuse what a blow is worth, which is
 * how an addon adds a multiplier without reimplementing the unarmed check.
 * {@link PreLevelUpEvent} can hold a cultivator at their current level - the XP
 * stays banked, so a refusal reads as "not yet".</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every listener here is called from inside the damage pipeline, on the world
 * thread that owns the entity being hit. Read components through the accessor
 * you are given and never write to the Store from one - queue writes on the
 * CommandBuffer, the same rule every ticking system in this mod follows.</p>
 *
 * <p>Keep them cheap. These fire on every bare-handed blow a cultivator lands.</p>
 */
public final class FistEvents {

    private FistEvents(){}

    // --- Post events ---------------------------------------------------------

    /** XP was banked. {@code amount} is what was actually granted, after any pre-event scaling. */
    public record XpGainEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                              int level, float amount) {}

    /** A cultivator's fists gained a level. Fires once per level when one blow crosses several. */
    public record LevelUpEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                               int fromLevel, int toLevel) {}

    // --- Pre events ----------------------------------------------------------

    /**
     * About to bank XP for a bare-handed blow. Like the tempering equivalent this
     * is not merely cancellable - it carries a mutable amount, so a listener can
     * scale the reward rather than being limited to allowing or forbidding it.
     *
     * <p>Setting the amount to 0 or below cancels the gain outright.</p>
     */
    public static final class PreXpGainEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final int level;
        private float amount;

        public PreXpGainEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, int level, float amount){
            this.ref = ref;
            this.player = player;
            this.level = level;
            this.amount = amount;
        }

        @Nonnull
        public Ref<EntityStore> ref(){
            return ref;
        }

        @Nullable
        public PlayerRef player(){
            return player;
        }

        /** The level the fists are on now - the XP is being banked toward the next one. */
        public int level(){
            return level;
        }

        /** The XP about to be banked, already scaled by the damage that landed and the config rate. */
        public float amount(){
            return amount;
        }

        /** Replaces the reward. Zero or less cancels the gain. */
        public void setAmount(float amount){
            this.amount = amount;
        }
    }

    /** About to gain a level. Cancelling holds the cultivator where they are; the XP stays banked. */
    public static final class PreLevelUpEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final int fromLevel;
        private final int toLevel;

        public PreLevelUpEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, int fromLevel, int toLevel){
            this.ref = ref;
            this.player = player;
            this.fromLevel = fromLevel;
            this.toLevel = toLevel;
        }

        @Nonnull
        public Ref<EntityStore> ref(){
            return ref;
        }

        @Nullable
        public PlayerRef player(){
            return player;
        }

        public int fromLevel(){
            return fromLevel;
        }

        public int toLevel(){
            return toLevel;
        }
    }

    // --- Registration --------------------------------------------------------

    private static final List<Consumer<XpGainEvent>> XP_GAIN = EventBus.newListenerList();
    private static final List<Consumer<LevelUpEvent>> LEVEL_UP = EventBus.newListenerList();
    private static final List<Consumer<PreXpGainEvent>> PRE_XP_GAIN = EventBus.newListenerList();
    private static final List<Consumer<PreLevelUpEvent>> PRE_LEVEL_UP = EventBus.newListenerList();

    public static void onXpGain(@Nonnull Consumer<XpGainEvent> listener){ XP_GAIN.add(listener); }
    public static void onLevelUp(@Nonnull Consumer<LevelUpEvent> listener){ LEVEL_UP.add(listener); }
    public static void onPreXpGain(@Nonnull Consumer<PreXpGainEvent> listener){ PRE_XP_GAIN.add(listener); }
    public static void onPreLevelUp(@Nonnull Consumer<PreLevelUpEvent> listener){ PRE_LEVEL_UP.add(listener); }

    public static void fireXpGain(@Nonnull XpGainEvent event){ EventBus.dispatch(XP_GAIN, event, "FistXpGainEvent"); }
    public static void fireLevelUp(@Nonnull LevelUpEvent event){ EventBus.dispatch(LEVEL_UP, event, "FistLevelUpEvent"); }
    public static boolean firePreLevelUp(@Nonnull PreLevelUpEvent event){ return EventBus.fire(PRE_LEVEL_UP, event, "FistPreLevelUpEvent"); }

    /**
     * Fires the pre-XP event and reports what survived it.
     *
     * @return the amount to bank, or 0 if a listener cancelled or zeroed it.
     */
    public static float firePreXpGain(@Nonnull PreXpGainEvent event){
        if(!EventBus.fire(PRE_XP_GAIN, event, "FistPreXpGainEvent")){
            return 0f;
        }

        return Math.max(0f, event.amount());
    }
}
