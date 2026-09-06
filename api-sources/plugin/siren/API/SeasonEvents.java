package plugin.siren.API;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * The shared season cadence - a season opening and a season closing. See
 * {@code plugin.siren.Utils.Season.SeasonManager} for the clock itself, and
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares.
 *
 * <h2>Post-only, deliberately</h2>
 *
 * <p>Neither event has a {@code Pre} twin. A season boundary is a deterministic
 * outcome of one persisted timestamp and one config value ({@code
 * Season-Length-Days}); there is nothing here a listener could usefully veto
 * that turning {@code Season-Enabled} off would not say more honestly, and a
 * cancelled close would leave the Hall of Fame, the leaderboard baselines and
 * Path War's season totals in three different seasons at once. This is the same
 * "reports the next state of a machine that is already running" shape {@code
 * DepthsEvents} and {@code SecretRealmEvents} document.</p>
 *
 * <h2>The boot self-heal cannot reach you</h2>
 *
 * <p>{@code SeasonManager.init} runs a quiet {@code tick(false)} of its own, so
 * a server that was offline across a whole season boundary closes and reopens
 * during Cultivation's own {@code setup()}. Cultivation's {@code setup()} runs
 * BEFORE any addon's, so the {@link SeasonOpenEvent} (and {@link
 * SeasonCloseEvent}) that boot-heal fires reaches NO addon listener - the
 * listener list is still empty at that moment. An addon that must know which
 * season is running has to ASK, via {@link CultivationAPI#getCurrentSeasonId()},
 * from its own {@code setup()} or first tick, and treat these events purely as
 * "it changed while I was watching". Designing around the event alone silently
 * strands every restart that crossed a boundary.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Both fire on whichever world thread reached {@code SeasonPumpSystem}
 * first - server-wide, not a particular player's world - with {@code
 * SeasonManager}'s own monitor held. Do not block, and hop per player
 * ({@code CompletableFuture.runAsync(task, theirWorld)}) before touching
 * anybody's components. The same constraint {@code CelestialEvents} documents.</p>
 *
 * <h2>Lock order</h2>
 *
 * <p>These two are the HEAD of this mod's only cross-manager lock chain:
 * <em>SeasonManager &rarr; addon &rarr; BountyManager</em>. A season listener
 * runs holding {@code SeasonManager}'s monitor, typically takes its own
 * manager's monitor next, and may then post a decree through {@code
 * BountyManager.post}, which takes that monitor last. Acquiring locks in that
 * order from here is safe and is what the chain is for. What is NOT safe is
 * running any part of it backwards - see {@link BountyEvents}' own lock-order
 * note, which is the tail of this same chain and forbids a bounty listener from
 * reaching back for an addon or season monitor.</p>
 */
public final class SeasonEvents {

    private SeasonEvents(){}

    // --- Post events ----------------------------------------------------------

    /**
     * A new season has opened and its start timestamp is already persisted.
     * {@code seasonId} is the season now RUNNING (the closing one's id plus
     * one, or 1 on a fresh install), and {@code startedAtMillis} is the wall
     * clock the new season's length is measured from.
     *
     * <p>On a rollover this fires immediately after {@link SeasonCloseEvent}
     * for {@code seasonId - 1}. On a fresh install (or the first tick after
     * {@code Season-Enabled} is turned on) it fires alone - there was no
     * previous season to close.</p>
     */
    public record SeasonOpenEvent(int seasonId, long startedAtMillis) {}

    /**
     * A season has closed: every Hall of Fame history row and champion-uuid set
     * for it is already written to disk, and the leaderboards' season baselines
     * are about to be invalidated by the next open. {@code seasonId} is the
     * season that just ENDED.
     *
     * <p>Fired after that persist and before {@code
     * PathWarManager.resetSeasonTotals()}, so a listener still sees the closing
     * season's Path War totals intact. Reads of {@code
     * CultivationLeaderboard.seasonDelta} are likewise still answering for the
     * closing season at this point - {@link SeasonOpenEvent} has not bumped the
     * id yet.</p>
     */
    public record SeasonCloseEvent(int seasonId, long closedAtMillis) {}

    // --- Listener lists -------------------------------------------------------

    private static final List<Consumer<SeasonOpenEvent>> OPEN = EventBus.newListenerList();
    private static final List<Consumer<SeasonCloseEvent>> CLOSE = EventBus.newListenerList();

    public static void onSeasonOpen(@Nonnull Consumer<SeasonOpenEvent> listener){ OPEN.add(listener); }
    public static void onSeasonClose(@Nonnull Consumer<SeasonCloseEvent> listener){ CLOSE.add(listener); }

    // --- Fired by SeasonManager ----------------------------------------------

    public static void fireSeasonOpen(@Nonnull SeasonOpenEvent event){ EventBus.dispatch(OPEN, event, "SeasonOpenEvent"); }
    public static void fireSeasonClose(@Nonnull SeasonCloseEvent event){ EventBus.dispatch(CLOSE, event, "SeasonCloseEvent"); }
}
