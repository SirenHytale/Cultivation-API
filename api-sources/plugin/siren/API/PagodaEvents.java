package plugin.siren.API;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Trial Pagoda events - copied from {@link DaoDuelEvents}'s own conventions:
 * players identified by UUID, pre-events cancellable/re-scalable, post-events
 * observational only.
 */
public final class PagodaEvents {
    private PagodaEvents(){}

    /** Why a run ended - mirrors {@code plugin.siren.Utils.Pagoda.PagodaRun.EndReason}, restated here as its own public enum so an addon never needs to import the internal run class. */
    public enum PagodaRunEndReason { DIED, LEFT, TIMED_OUT, DISCONNECTED }

    // --- Post-events ---

    /** A Trial Pagoda floor was cleared - {@code newRecord} is true only on a first clear (the highest-floor record actually advanced), false on a replay. */
    public record PagodaFloorClearedEvent(@Nonnull UUID playerUuid, int floor, boolean newRecord){}

    /** A Trial Pagoda run ended, for any reason - {@code floorReached} is the floor the run was ON when it ended, not necessarily a cleared floor (a death mid-floor never advances the player's own record). Mirrors {@code DepthsEvents.DepthsRunEndEvent}'s own shape (no live component read needed to fire it). */
    public record PagodaRunEndedEvent(@Nonnull UUID playerUuid, int floorReached, @Nonnull PagodaRunEndReason reason){}

    // --- Pre-events ---

    /** A floor-clear reward is about to be granted. Cancel to grant nothing at all; the caller still advances the run and the record either way. */
    public static final class PrePagodaRewardEvent extends CancellableEvent {
        private final UUID playerUuid;
        private final int floor;
        private long spiritStoneAmount;
        private float qiAmount;

        public PrePagodaRewardEvent(@Nonnull UUID playerUuid, int floor, long spiritStoneAmount, float qiAmount){
            this.playerUuid = playerUuid;
            this.floor = floor;
            this.spiritStoneAmount = spiritStoneAmount;
            this.qiAmount = qiAmount;
        }

        @Nonnull public UUID playerUuid(){ return this.playerUuid; }
        public int floor(){ return this.floor; }
        public long spiritStoneAmount(){ return this.spiritStoneAmount; }
        public void setSpiritStoneAmount(long amount){ this.spiritStoneAmount = amount; }
        public float qiAmount(){ return this.qiAmount; }
        public void setQiAmount(float amount){ this.qiAmount = amount; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PagodaFloorClearedEvent>> FLOOR_CLEARED = EventBus.newListenerList();
    private static final List<Consumer<PagodaRunEndedEvent>> RUN_ENDED = EventBus.newListenerList();
    private static final List<Consumer<PrePagodaRewardEvent>> PRE_REWARD = EventBus.newListenerList();

    public static void onPagodaFloorCleared(@Nonnull Consumer<PagodaFloorClearedEvent> listener){ FLOOR_CLEARED.add(listener); }
    public static void onPagodaRunEnded(@Nonnull Consumer<PagodaRunEndedEvent> listener){ RUN_ENDED.add(listener); }
    public static void onPrePagodaReward(@Nonnull Consumer<PrePagodaRewardEvent> listener){ PRE_REWARD.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void firePagodaFloorCleared(@Nonnull PagodaFloorClearedEvent event){ EventBus.dispatch(FLOOR_CLEARED, event, "PagodaFloorClearedEvent"); }
    public static void firePagodaRunEnded(@Nonnull PagodaRunEndedEvent event){ EventBus.dispatch(RUN_ENDED, event, "PagodaRunEndedEvent"); }
    public static boolean firePrePagodaReward(@Nonnull PrePagodaRewardEvent event){ return EventBus.fire(PRE_REWARD, event, "PrePagodaRewardEvent"); }
}
