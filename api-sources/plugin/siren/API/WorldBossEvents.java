package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import plugin.siren.Utils.Boss.WorldBossEncounter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Calamity Beast (灾劫兽) events - a wandering, solo world boss with no fixed
 * target (unlike {@link TideEvents}' place-anchored siege). See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares (pre vs post, cancellation, threading, registration).
 *
 * <h2>Why mostly post-only, like {@link DepthsEvents} - but not entirely</h2>
 *
 * <p>{@link TideEvents} carries a full cancellable pre-event for every phase
 * transition because a siege has a real, meaningfully-vetoable TARGET
 * ({@code /tide trigger sect <name>} lets an admin pick one, and
 * {@link TideEvents.PreTideStartEvent} lets an addon veto that specific pick).
 * A Calamity Beast has no such target: {@code WorldBossManager.startBoss}
 * always auto-picks a random online player as an anchor and a random point
 * near them - there is no "which target" decision for a listener to
 * meaningfully weigh in on, only "which random point", which is exactly the
 * kind of deterministic-outcome-of-the-run's-own-state-machine shape
 * {@link DepthsEvents}' own javadoc describes for its floor/extract/end
 * events. So {@link WorldBossSpawnEvent} (the boss NPC actually exists) and
 * {@link WorldBossResolveEvent} (the encounter is over, outcome already
 * applied) are post-only, exactly like Depths.</p>
 *
 * <p>{@link PreWorldBossStartEvent} is the one exception, kept for a
 * different reason than Tide's: cross-mod compatibility. A Calamity Beast can
 * still collide with something ANOTHER mod is doing at the moment the OMEN
 * would begin (a scripted event, a build phase, a PvP-off window) even though
 * nothing about ITS OWN target selection is vetoable - the same
 * "silence is a valid answer" shape {@link TideEvents.PreTideStartEvent}
 * gives Beast Tide's own pre-start veto, just narrower in scope: this event
 * carries no re-tunable numbers (no {@code waveCount}-style knob) because
 * there is nothing about the pick itself worth retuning, only whether it
 * should happen AT ALL right now.</p>
 *
 * <p>These are dispatched by {@code WorldBossManager}/whichever ECS system
 * actually spawns and resolves the encounter - this class only declares the
 * surface; nothing here calls into that manager, matching every other
 * standalone {@code *Events} class in this package.</p>
 */
public final class WorldBossEvents {
    private WorldBossEvents(){}

    // --- Post-events ---

    /** A Calamity Beast's OMEN phase has begun - its spawn point and species are locked and the omen sky/announcement is already live. */
    public record WorldBossStartEvent(@Nonnull String encounterId, @Nonnull String worldName,
                                      @Nonnull String roleId, @Nonnull Vector3d position){}

    /** The OMEN ended and the boss NPC now actually exists in the world (ACTIVE phase begun). */
    public record WorldBossSpawnEvent(@Nonnull String encounterId, @Nonnull String worldName, @Nonnull String roleId,
                                      @Nonnull Vector3d position, @Nonnull Ref<EntityStore> bossRef){}

    /** The encounter is over, for any reason - any reward payout has already been queued. */
    public record WorldBossResolveEvent(@Nonnull String encounterId, @Nonnull String worldName, @Nonnull String roleId,
                                        @Nonnull WorldBossEncounter.Result result, int contributorCount, float totalContribution){}

    // --- Pre-events ---

    /** A Calamity Beast is about to begin its OMEN phase. Cancel to abandon this start entirely - the scheduler simply waits for its next check, the same "silence is a valid answer" shape {@link TideEvents.PreTideStartEvent} has. See the class javadoc for why this carries no re-tunable numbers, unlike Tide's own pre-start event. */
    public static final class PreWorldBossStartEvent extends CancellableEvent {
        private final String worldName;
        private final String roleId;
        private final Vector3d position;

        public PreWorldBossStartEvent(@Nonnull String worldName, @Nonnull String roleId, @Nonnull Vector3d position){
            this.worldName = worldName;
            this.roleId = roleId;
            this.position = position;
        }

        @Nonnull public String worldName(){ return this.worldName; }
        @Nonnull public String roleId(){ return this.roleId; }
        @Nonnull public Vector3d position(){ return this.position; }
    }

    // --- Listener registration ---

    private static final List<Consumer<WorldBossStartEvent>> START = EventBus.newListenerList();
    private static final List<Consumer<PreWorldBossStartEvent>> PRE_START = EventBus.newListenerList();
    private static final List<Consumer<WorldBossSpawnEvent>> SPAWN = EventBus.newListenerList();
    private static final List<Consumer<WorldBossResolveEvent>> RESOLVE = EventBus.newListenerList();

    public static void onWorldBossStart(@Nonnull Consumer<WorldBossStartEvent> listener){ START.add(listener); }
    public static void onPreWorldBossStart(@Nonnull Consumer<PreWorldBossStartEvent> listener){ PRE_START.add(listener); }
    public static void onWorldBossSpawn(@Nonnull Consumer<WorldBossSpawnEvent> listener){ SPAWN.add(listener); }
    public static void onWorldBossResolve(@Nonnull Consumer<WorldBossResolveEvent> listener){ RESOLVE.add(listener); }

    // --- Internal dispatch (called by WorldBossManager/its ECS systems; not API) ---

    public static void fireWorldBossStart(@Nonnull WorldBossStartEvent event){ EventBus.dispatch(START, event, "WorldBossStartEvent"); }
    public static boolean firePreWorldBossStart(@Nonnull PreWorldBossStartEvent event){ return EventBus.fire(PRE_START, event, "PreWorldBossStartEvent"); }
    public static void fireWorldBossSpawn(@Nonnull WorldBossSpawnEvent event){ EventBus.dispatch(SPAWN, event, "WorldBossSpawnEvent"); }
    public static void fireWorldBossResolve(@Nonnull WorldBossResolveEvent event){ EventBus.dispatch(RESOLVE, event, "WorldBossResolveEvent"); }
}
