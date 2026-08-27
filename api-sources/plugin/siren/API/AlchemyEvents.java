package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Alchemy.AlchemyTendAction;
import plugin.siren.Utils.Alchemy.AlchemyTendOutcome;
import plugin.siren.Utils.Alchemy.PillGrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Alchemy (丹道) events - the Pill Cauldron refining ritual. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares (pre/post pairing, threading, a throwing listener being
 * logged and skipped).
 *
 * <p>Shaped after {@link ItemEvents}' weapon-refinement pair
 * ({@code RefinementStartEvent}/{@code RefinementCompleteEvent}) - the same
 * "re-weight the roll's inputs before it happens" shape
 * {@link TalismanEvents}' Inscribe pair already follows for its own ritual,
 * adapted here to the two inputs {@code AlchemyManager#rollOutcome} actually
 * takes.</p>
 *
 * <p><b>Who fires what:</b> {@code AlchemyCmd} fires the Refine-Start pair
 * when a ritual begins; {@code AlchemyRefiningSystem} fires the Refine-
 * Complete pair when one resolves (naturally or by interruption -
 * {@code PreRefineCompleteEvent} only fires on a NATURAL completion, never an
 * interruption, matching {@code PreInscribeCompleteEvent}'s own rule).</p>
 */
public final class AlchemyEvents {
    private AlchemyEvents(){}

    /** How a completed (or interrupted) refining ritual resolved. */
    public enum RefineOutcome {
        SUCCESS,
        /** Failed outright - wastes the spent herbs and Qi, nothing else happens. */
        FAILED,
        /** Failed AND the cauldron turned on the alchemist - see AlchemyConfig's Refine-Botch-* fields. */
        BOTCH
    }

    // --- Post-events ---

    /** A refining ritual began; the herbs and Qi floor check already passed. */
    public record RefineStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                   @Nonnull String effect, float qiDrainPerSecond) {}

    /** A refining ritual resolved. {@code grade} is null unless {@code outcome} is SUCCESS. */
    public record RefineCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                      @Nonnull String effect, @Nonnull RefineOutcome outcome, @Nullable PillGrade grade) {}

    /**
     * A Fire Watch (火候) tending prompt was resolved - answered (sharp/steady),
     * missed, or fumbled - during a running refining ritual. {@code promptIndex}
     * is 0-based (this is the Nth prompt this ritual delivered); {@code promptCount}
     * is the ritual's configured {@code Tend-Prompts-Max} ceiling, not how many
     * have fired so far. Post-only, like {@link RefineStartEvent}/
     * {@link RefineCompleteEvent} above - nothing about a single prompt's
     * resolution is meant to be vetoed or re-weighted from outside; only the
     * ritual-end {@code tendDelta} it eventually folds into is (see
     * {@link PreRefineCompleteEvent#tendDelta}).
     */
    public record TendPromptResolvedEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                          @Nonnull AlchemyTendAction action, @Nonnull AlchemyTendOutcome outcome,
                                          int promptIndex, int promptCount) {}

    // --- Pre-events ---

    /** A refining ritual is about to begin. Cancel to refuse it (no herbs or Qi are spent). */
    public static final class PreRefineStartEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String effect;
        private float durationSeconds;

        public PreRefineStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                   @Nonnull String effect, float durationSeconds){
            this.ref = ref;
            this.player = player;
            this.effect = effect;
            this.durationSeconds = durationSeconds;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public String effect(){ return this.effect; }
        /** How long the ritual is scheduled to take, as fed to {@code AlchemyRitualComponent#start}. */
        public float durationSeconds(){ return this.durationSeconds; }
        public void setDurationSeconds(float durationSeconds){ this.durationSeconds = durationSeconds; }
    }

    /**
     * A refining ritual is about to resolve into a grade. Cancel to abandon it
     * silently - the herbs and Qi stay spent, nothing is produced, the same
     * shape {@link ItemEvents.PreRefinementCompleteEvent} and
     * {@link TalismanEvents.PreInscribeCompleteEvent} both use.
     * {@link #setHerbQualityAvg} and {@link #setMasteryLadderFraction}
     * re-weight the two inputs {@code AlchemyManager#rollOutcome} actually
     * rolls against. Fires only on a natural completion, never an
     * interruption - see this class's own doc.
     */
    public static final class PreRefineCompleteEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String effect;
        private float herbQualityAvg;
        private float masteryLadderFraction;
        private float tendDelta;

        /**
         * Pre-Fire Watch (v3) shape - retained exactly as-is for existing callers.
         * Delegates to the 6-arg constructor with {@code tendDelta=0f}, matching
         * {@link plugin.siren.Utils.Alchemy.AlchemyTendingManager#computeTendDelta}'s
         * own no-prompts-delivered zero.
         */
        public PreRefineCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull String effect,
                                      float herbQualityAvg, float masteryLadderFraction){
            this(ref, player, effect, herbQualityAvg, masteryLadderFraction, 0f);
        }

        /**
         * v4 (Fire Watch) shape - adds {@code tendDelta}, the same third input
         * {@code AlchemyManager#rollOutcome(float, float, float)} takes.
         */
        public PreRefineCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull String effect,
                                      float herbQualityAvg, float masteryLadderFraction, float tendDelta){
            this.ref = ref;
            this.player = player;
            this.effect = effect;
            this.herbQualityAvg = herbQualityAvg;
            this.masteryLadderFraction = masteryLadderFraction;
            this.tendDelta = tendDelta;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public String effect(){ return this.effect; }
        /** 0..1, as fed to {@code AlchemyManager#rollOutcome}. */
        public float herbQualityAvg(){ return this.herbQualityAvg; }
        public void setHerbQualityAvg(float herbQualityAvg){ this.herbQualityAvg = herbQualityAvg; }
        /** 0..1, as fed to {@code AlchemyManager#rollOutcome}. */
        public float masteryLadderFraction(){ return this.masteryLadderFraction; }
        public void setMasteryLadderFraction(float masteryLadderFraction){ this.masteryLadderFraction = masteryLadderFraction; }
        /**
         * The Fire Watch (火候) tending performance's additive nudge to the grade
         * roll, as computed by {@code AlchemyTendingManager#computeTendDelta} -
         * exactly {@code 0f} for a ritual with no delivered prompts (Tending-
         * Enabled off, or the ritual ended before its first prompt). Settable, same
         * as the other two inputs, so a listener can re-weight it before the roll.
         */
        public float tendDelta(){ return this.tendDelta; }
        public void setTendDelta(float tendDelta){ this.tendDelta = tendDelta; }
    }

    // --- Listener registration ---

    private static final List<Consumer<RefineStartEvent>> REFINE_START = EventBus.newListenerList();
    private static final List<Consumer<PreRefineStartEvent>> PRE_REFINE_START = EventBus.newListenerList();
    private static final List<Consumer<RefineCompleteEvent>> REFINE_COMPLETE = EventBus.newListenerList();
    private static final List<Consumer<PreRefineCompleteEvent>> PRE_REFINE_COMPLETE = EventBus.newListenerList();
    // No unregister exists for any listener list in this class - see K-025 in
    // docs/ai/KNOWN_ISSUES.md ("Cultivation core's *Events classes have no
    // listener-unregister capability"). Same register-only, server-lifetime
    // guarantee CultivationEvents' own class doc describes.
    private static final List<Consumer<TendPromptResolvedEvent>> TEND_PROMPT_RESOLVED = EventBus.newListenerList();

    public static void onRefineStart(@Nonnull Consumer<RefineStartEvent> listener){ REFINE_START.add(listener); }
    public static void onPreRefineStart(@Nonnull Consumer<PreRefineStartEvent> listener){ PRE_REFINE_START.add(listener); }
    public static void onRefineComplete(@Nonnull Consumer<RefineCompleteEvent> listener){ REFINE_COMPLETE.add(listener); }
    public static void onPreRefineComplete(@Nonnull Consumer<PreRefineCompleteEvent> listener){ PRE_REFINE_COMPLETE.add(listener); }
    public static void onTendPromptResolved(@Nonnull Consumer<TendPromptResolvedEvent> listener){ TEND_PROMPT_RESOLVED.add(listener); }

    // --- Internal dispatch (called by this mod's own systems/commands; not API) ---

    public static void fireRefineStart(@Nonnull RefineStartEvent event){ EventBus.dispatch(REFINE_START, event, "RefineStartEvent"); }
    public static boolean firePreRefineStart(@Nonnull PreRefineStartEvent event){ return EventBus.fire(PRE_REFINE_START, event, "PreRefineStartEvent"); }
    public static void fireRefineComplete(@Nonnull RefineCompleteEvent event){ EventBus.dispatch(REFINE_COMPLETE, event, "RefineCompleteEvent"); }
    public static boolean firePreRefineComplete(@Nonnull PreRefineCompleteEvent event){ return EventBus.fire(PRE_REFINE_COMPLETE, event, "PreRefineCompleteEvent"); }
    public static void fireTendPromptResolved(@Nonnull TendPromptResolvedEvent event){ EventBus.dispatch(TEND_PROMPT_RESOLVED, event, "TendPromptResolvedEvent"); }
}
