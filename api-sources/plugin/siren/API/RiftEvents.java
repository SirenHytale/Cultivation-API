package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import plugin.siren.Utils.Rift.RiftPhase;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Void Rift events - a randomly-triggered, server-wide world event: a rift
 * opens (OPENING grace period), throws a fixed number of corrupted-beast
 * waves (ASSAULT), then spawns a boss-tier Warden (WARDEN), before resolving
 * to either SEALED (Warden defeated) or COLLAPSED (timed out/abandoned) - see
 * {@code RiftPhase} for the phase state machine these events track. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class
 * in this package shares (pre vs post, cancellation, threading,
 * registration).
 *
 * <h2>Why mostly post-only, like {@link WorldBossEvents} - but not entirely</h2>
 *
 * <p>Same shape as {@link WorldBossEvents}: a Void Rift auto-picks its own
 * anchor and spawn point with no meaningfully-vetoable "which target"
 * decision, so {@link RiftWaveEvent} (a wave was just thrown), {@link
 * RiftWardenSpawnEvent} (the Warden NPC actually exists) and {@link
 * RiftResolveEvent} (the encounter is over, any reward payout already
 * queued) are post-only notifications. {@link PreRiftOpenEvent} is the one
 * exception, kept for the same cross-mod-compatibility reason {@link
 * WorldBossEvents.PreWorldBossStartEvent} is: a rift can still collide with
 * something ANOTHER mod is doing at the moment it would open (a scripted
 * event, a build phase, a PvP-off window) even though nothing about its own
 * placement is worth retuning - "silence is a valid answer", and it carries
 * no re-tunable numbers, only whether the open should happen AT ALL right
 * now.</p>
 *
 * <p>These are dispatched by whichever manager/ECS system actually runs the
 * rift's phase state machine (not written yet this slice) - this class only
 * declares the surface; nothing here calls into that manager, matching every
 * other standalone {@code *Events} class in this package.</p>
 *
 * <p><b>K-025:</b> like every {@code *Events} class in this package, listener
 * registration is a plain {@code CopyOnWriteArrayList} with no unregister
 * mechanism - listener lifetime is server lifetime, matching how plugins load
 * once and stay. See {@code docs/ai/KNOWN_ISSUES.md} K-025.</p>
 */
public final class RiftEvents {
    private RiftEvents(){}

    /** Why a Void Rift resolved. Exactly one of {@link RiftPhase#SEALED}/{@link RiftPhase#COLLAPSED} - see {@link RiftResolveEvent}. */
    // (Reuses RiftPhase rather than a bespoke Result enum: SEALED/COLLAPSED
    // are already RiftPhase's own two terminal phases, and no RiftEncounter/
    // RiftManager class exists yet this slice for a Result enum to live on,
    // the way WorldBossEncounter.Result/TideAssault.Result do for their
    // events.)

    // --- Post-events ---

    /** A Void Rift's OPENING phase has begun - its anchor and spawn point are locked and the world-event marker/announcement is already live. */
    public record RiftOpenEvent(@Nonnull String riftId, @Nonnull String worldName, @Nonnull Vector3d position){}

    /** One corrupted-beast wave was just thrown during the ASSAULT phase. {@code waveIndex} counts from 0; {@code waveCount} is the total for this rift. */
    public record RiftWaveEvent(@Nonnull String riftId, @Nonnull String worldName, @Nonnull Vector3d position,
                                int waveIndex, int waveCount, @Nonnull String spawnConfigId){}

    /** The ASSAULT phase ended and the Warden NPC now actually exists in the world (WARDEN phase begun). */
    public record RiftWardenSpawnEvent(@Nonnull String riftId, @Nonnull String worldName, @Nonnull String roleId,
                                       @Nonnull Vector3d position, @Nonnull Ref<EntityStore> wardenRef){}

    /** The encounter is over, for any reason - any reward payout has already been queued. {@code outcome} is always {@link RiftPhase#SEALED} or {@link RiftPhase#COLLAPSED}. */
    public record RiftResolveEvent(@Nonnull String riftId, @Nonnull String worldName, @Nonnull Vector3d position,
                                   @Nonnull RiftPhase outcome, int contributorCount, float totalContribution){}

    // --- Pre-events ---

    /** A Void Rift is about to begin its OPENING phase. Cancel to abandon this open entirely - the scheduler simply waits for its next check, the same "silence is a valid answer" shape {@link WorldBossEvents.PreWorldBossStartEvent} has. See the class javadoc for why this carries no re-tunable numbers. */
    public static final class PreRiftOpenEvent extends CancellableEvent {
        private final String worldName;
        private final Vector3d position;

        public PreRiftOpenEvent(@Nonnull String worldName, @Nonnull Vector3d position){
            this.worldName = worldName;
            this.position = position;
        }

        @Nonnull public String worldName(){ return this.worldName; }
        @Nonnull public Vector3d position(){ return this.position; }
    }

    // --- Listener registration ---

    private static final List<Consumer<RiftOpenEvent>> OPEN = EventBus.newListenerList();
    private static final List<Consumer<PreRiftOpenEvent>> PRE_OPEN = EventBus.newListenerList();
    private static final List<Consumer<RiftWaveEvent>> WAVE = EventBus.newListenerList();
    private static final List<Consumer<RiftWardenSpawnEvent>> WARDEN_SPAWN = EventBus.newListenerList();
    private static final List<Consumer<RiftResolveEvent>> RESOLVE = EventBus.newListenerList();

    public static void onRiftOpen(@Nonnull Consumer<RiftOpenEvent> listener){ OPEN.add(listener); }
    public static void onPreRiftOpen(@Nonnull Consumer<PreRiftOpenEvent> listener){ PRE_OPEN.add(listener); }
    public static void onRiftWave(@Nonnull Consumer<RiftWaveEvent> listener){ WAVE.add(listener); }
    public static void onRiftWardenSpawn(@Nonnull Consumer<RiftWardenSpawnEvent> listener){ WARDEN_SPAWN.add(listener); }
    public static void onRiftResolve(@Nonnull Consumer<RiftResolveEvent> listener){ RESOLVE.add(listener); }

    // --- Internal dispatch (called by the rift manager/its ECS systems; not API) ---

    public static void fireRiftOpen(@Nonnull RiftOpenEvent event){ EventBus.dispatch(OPEN, event, "RiftOpenEvent"); }
    public static boolean firePreRiftOpen(@Nonnull PreRiftOpenEvent event){ return EventBus.fire(PRE_OPEN, event, "PreRiftOpenEvent"); }
    public static void fireRiftWave(@Nonnull RiftWaveEvent event){ EventBus.dispatch(WAVE, event, "RiftWaveEvent"); }
    public static void fireRiftWardenSpawn(@Nonnull RiftWardenSpawnEvent event){ EventBus.dispatch(WARDEN_SPAWN, event, "RiftWardenSpawnEvent"); }
    public static void fireRiftResolve(@Nonnull RiftResolveEvent event){ EventBus.dispatch(RESOLVE, event, "RiftResolveEvent"); }
}
