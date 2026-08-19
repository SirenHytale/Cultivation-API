package plugin.siren.API;

import plugin.siren.Utils.Tide.TideAssault;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Beast Tide (兽潮) events - a siege on a sect hall or a Cave Abode starting a
 * wave, or resolving win/lose. See {@link CultivationEvents} for the
 * conventions every {@code *Events} class in this package shares (pre vs
 * post, cancellation, threading, registration).
 *
 * <p>These fire ALONGSIDE {@link CelestialEvents#onCelestialEventStart}/
 * {@code onCelestialEventEnd} for the tide's own {@code CelestialEventType}
 * id (see {@code BeastTideManager}), not instead of them - the celestial pair
 * covers the server-wide sky/chat/title announcement generic to any celestial
 * event; {@link TideStartEvent} fires once that announcement is already live
 * and carries the siege-specific detail (which target, how many waves)
 * celestial events don't know about.</p>
 */
public final class TideEvents {
    private TideEvents(){}

    // --- Post-events ---

    /** A tide's WARNING phase has begun - its target is locked and its beacon will spawn shortly (RISK 2 headstart). */
    public record TideStartEvent(@Nonnull String assaultId, @Nonnull String worldName, boolean abode,
                                 @Nonnull String targetName, int waveCount){}

    /** One wave was just triggered (or attempted - see {@link PreTideWaveEvent}). */
    public record TideWaveEvent(@Nonnull String assaultId, int waveIndex, int waveCount){}

    /** The assault is over - the outcome (and any reward/suppression) has already been applied. */
    public record TideResolveEvent(@Nonnull String assaultId, @Nonnull String worldName, boolean abode,
                                   @Nonnull String targetName, @Nonnull TideAssault.Result result){}

    // --- Pre-events ---

    /** A target has been picked and a tide is about to enter WARNING. Cancel to abandon this pick entirely - the scheduler simply waits for its next check, the same "silence is a valid answer" shape {@code PreSectJoinEvent} has. */
    public static final class PreTideStartEvent extends CancellableEvent {
        private final String worldName;
        private final boolean abode;
        private final String targetName;
        private int waveCount;

        public PreTideStartEvent(@Nonnull String worldName, boolean abode, @Nonnull String targetName, int waveCount){
            this.worldName = worldName;
            this.abode = abode;
            this.targetName = targetName;
            this.waveCount = waveCount;
        }

        @Nonnull public String worldName(){ return this.worldName; }
        public boolean abode(){ return this.abode; }
        @Nonnull public String targetName(){ return this.targetName; }
        public int waveCount(){ return this.waveCount; }
        public void setWaveCount(int waveCount){ this.waveCount = waveCount; }
    }

    /** One wave is about to be triggered. Cancel to skip just this attempt (the timer and wave index still advance - defenders get a breather, not an extra wave). */
    public static final class PreTideWaveEvent extends CancellableEvent {
        private final String assaultId;
        private final int waveIndex;

        public PreTideWaveEvent(@Nonnull String assaultId, int waveIndex){
            this.assaultId = assaultId;
            this.waveIndex = waveIndex;
        }

        @Nonnull public String assaultId(){ return this.assaultId; }
        public int waveIndex(){ return this.waveIndex; }
    }

    /** The assault is about to resolve. Cancel to skip applying the reward (WON) or suppression/cooldown (LOST) - the assault still ends and cleans up either way, only the outcome-specific side effect is skipped. {@link #setSuppressionSteps}/{@link #setSuppressionDurationMinutes} retune a LOSS; {@link #setContributionReward} retunes a WIN. */
    public static final class PreTideResolveEvent extends CancellableEvent {
        private final String assaultId;
        private final TideAssault.Result result;
        private int suppressionSteps;
        private float suppressionDurationMinutes;
        private int contributionReward;

        public PreTideResolveEvent(@Nonnull String assaultId, @Nonnull TideAssault.Result result,
                                   int suppressionSteps, float suppressionDurationMinutes, int contributionReward){
            this.assaultId = assaultId;
            this.result = result;
            this.suppressionSteps = suppressionSteps;
            this.suppressionDurationMinutes = suppressionDurationMinutes;
            this.contributionReward = contributionReward;
        }

        @Nonnull public String assaultId(){ return this.assaultId; }
        @Nonnull public TideAssault.Result result(){ return this.result; }
        public int suppressionSteps(){ return this.suppressionSteps; }
        public void setSuppressionSteps(int suppressionSteps){ this.suppressionSteps = suppressionSteps; }
        public float suppressionDurationMinutes(){ return this.suppressionDurationMinutes; }
        public void setSuppressionDurationMinutes(float minutes){ this.suppressionDurationMinutes = minutes; }
        public int contributionReward(){ return this.contributionReward; }
        public void setContributionReward(int contributionReward){ this.contributionReward = contributionReward; }
    }

    // --- Listener registration ---

    private static final List<Consumer<TideStartEvent>> START = EventBus.newListenerList();
    private static final List<Consumer<PreTideStartEvent>> PRE_START = EventBus.newListenerList();
    private static final List<Consumer<TideWaveEvent>> WAVE = EventBus.newListenerList();
    private static final List<Consumer<PreTideWaveEvent>> PRE_WAVE = EventBus.newListenerList();
    private static final List<Consumer<TideResolveEvent>> RESOLVE = EventBus.newListenerList();
    private static final List<Consumer<PreTideResolveEvent>> PRE_RESOLVE = EventBus.newListenerList();

    public static void onTideStart(@Nonnull Consumer<TideStartEvent> listener){ START.add(listener); }
    public static void onPreTideStart(@Nonnull Consumer<PreTideStartEvent> listener){ PRE_START.add(listener); }
    public static void onTideWave(@Nonnull Consumer<TideWaveEvent> listener){ WAVE.add(listener); }
    public static void onPreTideWave(@Nonnull Consumer<PreTideWaveEvent> listener){ PRE_WAVE.add(listener); }
    public static void onTideResolve(@Nonnull Consumer<TideResolveEvent> listener){ RESOLVE.add(listener); }
    public static void onPreTideResolve(@Nonnull Consumer<PreTideResolveEvent> listener){ PRE_RESOLVE.add(listener); }

    // --- Internal dispatch (called by BeastTideManager/BeastTideWaveSystem; not API) ---

    public static void fireTideStart(@Nonnull TideStartEvent event){ EventBus.dispatch(START, event, "TideStartEvent"); }
    public static boolean firePreTideStart(@Nonnull PreTideStartEvent event){ return EventBus.fire(PRE_START, event, "PreTideStartEvent"); }
    public static void fireTideWave(@Nonnull TideWaveEvent event){ EventBus.dispatch(WAVE, event, "TideWaveEvent"); }
    public static boolean firePreTideWave(@Nonnull PreTideWaveEvent event){ return EventBus.fire(PRE_WAVE, event, "PreTideWaveEvent"); }
    public static void fireTideResolve(@Nonnull TideResolveEvent event){ EventBus.dispatch(RESOLVE, event, "TideResolveEvent"); }
    public static boolean firePreTideResolve(@Nonnull PreTideResolveEvent event){ return EventBus.fire(PRE_RESOLVE, event, "PreTideResolveEvent"); }
}
