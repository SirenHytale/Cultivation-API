package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wandering-NPC quest line events. See {@link CultivationEvents} for the
 * conventions every {@code *Events} class in this package shares (pre vs post,
 * cancellation, threading, registration).
 *
 * <h2>Post-only, with ONE pre-event - and why that split lands here</h2>
 *
 * <p>{@link WorldBossEvents}' javadoc states the test this package applies:
 * a pre-event is worth carrying where a listener has a real, meaningfully
 * vetoable DECISION in front of it; where the outcome is just the deterministic
 * next state of a machine that is already running, {@link DepthsEvents}' shape
 * (post-only) is the honest one. Applied to a quest chain:</p>
 *
 * <ul>
 *   <li><b>Accepting is a decision.</b> A player is choosing to take a specific
 *   chain from a specific giver right now, and an addon has real reasons to
 *   refuse that particular pick - a race or sect the chain should not be
 *   offered to, an event window, its own quest system already holding the
 *   player. That is the same "which target" shape that earned
 *   {@link TideEvents.PreTideStartEvent} its veto. Hence
 *   {@link PreQuestAcceptEvent}, fired AFTER every built-in check passes and
 *   BEFORE a single field is written (M-024: the veto runs before the mutation,
 *   never after).</li>
 *   <li><b>Finishing is not.</b> By the time the last step clears, the player
 *   has already spent the items, landed the kills and sat the vigil. A veto
 *   there could only take a reward the run already earned - precisely the
 *   destroy-an-earned-reward failure M-020 exists to prevent. So
 *   {@link QuestCompleteEvent} is post-only, fired once the payout is fully
 *   settled.</li>
 *   <li><b>Turning items in is not vetoable either</b>, for the same reason
 *   sharpened: a veto placed after the two-pass consume would eat the items and
 *   give nothing, and one placed before it is indistinguishable from the
 *   accept-time veto an addon already has. {@link QuestStepAdvanceEvent} is a
 *   plain observation.</li>
 * </ul>
 *
 * <p>These are dispatched by {@code plugin.siren.Utils.Quest.QuestManager};
 * this class only declares the surface, like every other standalone
 * {@code *Events} class here.</p>
 */
public final class QuestEvents {
    private QuestEvents(){}

    // --- Post-events ---

    /** A player has taken on a quest chain - the progress row is written and, for a site chain, its site is already rolled. */
    public record QuestAcceptEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                   @Nonnull String chainId, @Nonnull String giverRoleId){}

    /**
     * A step of a chain just cleared.
     *
     * @param completedStepIndex the 0-based index of the step that was finished.
     * @param nextStepIndex      the 0-based index now current, or
     *                           {@code completedStepIndex + 1} past the end when
     *                           the chain has run out of steps - check
     *                           {@link #finalStep()} rather than comparing
     *                           against a step count.
     * @param finalStep          true if this was the last step, so the run has
     *                           moved to awaiting its reward.
     */
    public record QuestStepAdvanceEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                        @Nonnull String chainId, int completedStepIndex,
                                        int nextStepIndex, boolean finalStep){}

    /** A chain is fully finished AND fully paid - every reward component landed. Fires exactly once per run, at the moment the run turns COMPLETED. */
    public record QuestCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                     @Nonnull String chainId, int completionCount){}

    /** A player gave up on an in-progress run. Never fires for a run that was awaiting a reward - that one cannot be abandoned. */
    public record QuestAbandonEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                    @Nonnull String chainId, int stepIndex){}

    // --- Pre-events ---

    /**
     * A player is about to take on a chain. Every built-in refusal (realm gate,
     * once-per-account, cooldown, chain cap) has already passed; cancel to
     * refuse it anyway. Nothing is written when a listener cancels - see the
     * class javadoc.
     */
    public static final class PreQuestAcceptEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String chainId;
        private final String giverRoleId;

        public PreQuestAcceptEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                   @Nonnull String chainId, @Nonnull String giverRoleId){
            this.ref = ref;
            this.player = player;
            this.chainId = chainId;
            this.giverRoleId = giverRoleId;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public String chainId(){ return this.chainId; }
        @Nonnull public String giverRoleId(){ return this.giverRoleId; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PreQuestAcceptEvent>> PRE_ACCEPT = EventBus.newListenerList();
    private static final List<Consumer<QuestAcceptEvent>> ACCEPT = EventBus.newListenerList();
    private static final List<Consumer<QuestStepAdvanceEvent>> STEP_ADVANCE = EventBus.newListenerList();
    private static final List<Consumer<QuestCompleteEvent>> COMPLETE = EventBus.newListenerList();
    private static final List<Consumer<QuestAbandonEvent>> ABANDON = EventBus.newListenerList();

    public static void onPreQuestAccept(@Nonnull Consumer<PreQuestAcceptEvent> listener){ PRE_ACCEPT.add(listener); }
    public static void onQuestAccept(@Nonnull Consumer<QuestAcceptEvent> listener){ ACCEPT.add(listener); }
    public static void onQuestStepAdvance(@Nonnull Consumer<QuestStepAdvanceEvent> listener){ STEP_ADVANCE.add(listener); }
    public static void onQuestComplete(@Nonnull Consumer<QuestCompleteEvent> listener){ COMPLETE.add(listener); }
    public static void onQuestAbandon(@Nonnull Consumer<QuestAbandonEvent> listener){ ABANDON.add(listener); }

    // --- Internal dispatch (called by QuestManager; not API) ---

    public static boolean firePreQuestAccept(@Nonnull PreQuestAcceptEvent event){ return EventBus.fire(PRE_ACCEPT, event, "PreQuestAcceptEvent"); }
    public static void fireQuestAccept(@Nonnull QuestAcceptEvent event){ EventBus.dispatch(ACCEPT, event, "QuestAcceptEvent"); }
    public static void fireQuestStepAdvance(@Nonnull QuestStepAdvanceEvent event){ EventBus.dispatch(STEP_ADVANCE, event, "QuestStepAdvanceEvent"); }
    public static void fireQuestComplete(@Nonnull QuestCompleteEvent event){ EventBus.dispatch(COMPLETE, event, "QuestCompleteEvent"); }
    public static void fireQuestAbandon(@Nonnull QuestAbandonEvent event){ EventBus.dispatch(ABANDON, event, "QuestAbandonEvent"); }
}
