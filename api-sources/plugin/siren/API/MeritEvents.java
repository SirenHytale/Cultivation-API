package plugin.siren.API;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.Utils.Merit.MeritDeed;
import plugin.siren.Utils.Merit.MeritRank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Merit (功德) events. See {@link CultivationEvents} for the conventions
 * every {@code *Events} class in this package shares (pre vs post,
 * cancellation, threading, registration). Structure copied verbatim from
 * {@link RivalEvents}.
 *
 * <p>Dispatched by {@code MeritManager} - this class only declares the
 * surface; nothing here calls into that manager, matching every other
 * standalone {@code *Events} class in this package.</p>
 */
public final class MeritEvents {
    private MeritEvents(){}

    // --- Pre-events ---

    /** Merit is about to be credited for a deed. Cancel to pay nothing; {@link #setAmount} to re-weigh the deed. */
    public static final class PreMeritGainEvent extends CancellableEvent {
        private final PlayerRef player;
        private final MeritDeed deed;
        private float amount;

        public PreMeritGainEvent(@Nullable PlayerRef player, @Nonnull MeritDeed deed, float amount){
            this.player = player;
            this.deed = deed;
            this.amount = amount;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public MeritDeed deed(){ return this.deed; }
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
    }

    // --- Post-events ---

    /** Merit was actually credited - {@code total}/{@code multiplier} are the values AFTER the throttle and caps were applied. */
    public record MeritGainEvent(@Nullable PlayerRef player, @Nonnull MeritDeed deed, float amount, float total, float multiplier){}

    /** This player's Merit rank changed - fired once per crossing, de-duplicated the same way {@code DaoEvents.PathChangeEvent} is. */
    public record MeritRankUpEvent(@Nullable PlayerRef player, @Nonnull MeritRank from, @Nonnull MeritRank to){}

    // --- Listener registration ---

    private static final List<Consumer<PreMeritGainEvent>> PRE_GAIN = EventBus.newListenerList();
    private static final List<Consumer<MeritGainEvent>> GAIN = EventBus.newListenerList();
    private static final List<Consumer<MeritRankUpEvent>> RANK_UP = EventBus.newListenerList();

    public static void onPreMeritGain(@Nonnull Consumer<PreMeritGainEvent> listener){ PRE_GAIN.add(listener); }
    public static void onMeritGain(@Nonnull Consumer<MeritGainEvent> listener){ GAIN.add(listener); }
    public static void onMeritRankUp(@Nonnull Consumer<MeritRankUpEvent> listener){ RANK_UP.add(listener); }

    // --- Internal dispatch (called by MeritManager; not API) ---

    public static boolean firePreMeritGain(@Nonnull PreMeritGainEvent event){ return EventBus.fire(PRE_GAIN, event, "PreMeritGainEvent"); }
    public static void fireMeritGain(@Nonnull MeritGainEvent event){ EventBus.dispatch(GAIN, event, "MeritGainEvent"); }
    public static void fireMeritRankUp(@Nonnull MeritRankUpEvent event){ EventBus.dispatch(RANK_UP, event, "MeritRankUpEvent"); }
}
