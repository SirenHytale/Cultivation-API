package plugin.siren.API;

import plugin.siren.Utils.Depths.DepthsRun;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Secret Realm Depths (秘境深处) events - a solo run starting, a floor
 * clearing (with the escrow reward it just rolled), an extraction actually
 * paying out, and a run ending for any reason. See {@link CultivationEvents}
 * for the conventions every {@code *Events} class in this package shares
 * (pre vs post, cancellation, threading, registration).
 *
 * <p>Post-only, deliberately, mirroring {@link SecretRealmEvents}'s own
 * reasoning: every one of these is a deterministic outcome of the run's own
 * state machine (a floor actually clearing, a player actually choosing to
 * extract, a player actually dying or disconnecting) rather than a request
 * anything downstream could meaningfully veto - so there is no Pre/cancellable
 * pair here the way {@link TideEvents} has one.</p>
 */
public final class DepthsEvents {
    private DepthsEvents(){}

    // --- Post-events ---

    /** A solo Depths run has started, floor 1 about to spawn. */
    public record DepthsRunStartEvent(@Nonnull String runId, @Nonnull UUID ownerUuid, @Nonnull String siteId, @Nonnull String world){}

    /** A floor's beasts are all dead - {@code escrowSize} is the run's TOTAL unbanked escrow count after this floor's roll (0 or 1 higher than before it, since a roll can miss). */
    public record DepthsFloorClearEvent(@Nonnull String runId, @Nonnull UUID ownerUuid, int floor, int escrowSize){}

    /** The run's escrow was just actually granted - fired only when {@code rewardsGranted} is above zero, whether the player chose Extract or the run auto-extracted (logout/realm-close). */
    public record DepthsExtractEvent(@Nonnull String runId, @Nonnull UUID ownerUuid, int depthReached, int rewardsGranted){}

    /** The run is over, for any reason - fired once, after any {@link DepthsExtractEvent} the same ending also produced. */
    public record DepthsRunEndEvent(@Nonnull String runId, @Nonnull UUID ownerUuid, @Nonnull DepthsRun.EndReason reason, int depthReached){}

    // --- Listener registration ---

    private static final List<Consumer<DepthsRunStartEvent>> RUN_START = EventBus.newListenerList();
    private static final List<Consumer<DepthsFloorClearEvent>> FLOOR_CLEAR = EventBus.newListenerList();
    private static final List<Consumer<DepthsExtractEvent>> EXTRACT = EventBus.newListenerList();
    private static final List<Consumer<DepthsRunEndEvent>> RUN_END = EventBus.newListenerList();

    public static void onDepthsRunStart(@Nonnull Consumer<DepthsRunStartEvent> listener){ RUN_START.add(listener); }
    public static void onDepthsFloorClear(@Nonnull Consumer<DepthsFloorClearEvent> listener){ FLOOR_CLEAR.add(listener); }
    public static void onDepthsExtract(@Nonnull Consumer<DepthsExtractEvent> listener){ EXTRACT.add(listener); }
    public static void onDepthsRunEnd(@Nonnull Consumer<DepthsRunEndEvent> listener){ RUN_END.add(listener); }

    // --- Internal dispatch (called by DepthsManager/DepthsFloorSystem; not API) ---

    public static void fireDepthsRunStart(@Nonnull DepthsRunStartEvent event){ EventBus.dispatch(RUN_START, event, "DepthsRunStartEvent"); }
    public static void fireDepthsFloorClear(@Nonnull DepthsFloorClearEvent event){ EventBus.dispatch(FLOOR_CLEAR, event, "DepthsFloorClearEvent"); }
    public static void fireDepthsExtract(@Nonnull DepthsExtractEvent event){ EventBus.dispatch(EXTRACT, event, "DepthsExtractEvent"); }
    public static void fireDepthsRunEnd(@Nonnull DepthsRunEndEvent event){ EventBus.dispatch(RUN_END, event, "DepthsRunEndEvent"); }
}
