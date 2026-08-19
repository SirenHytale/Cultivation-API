package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Dao.DaoElement;
import plugin.siren.Utils.Talisman.TalismanGrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Talisman (符箓) events - inscribing at a Talisman Desk and using a finished
 * talisman. See {@link CultivationEvents} for the conventions every
 * {@code *Events} class in this package shares (pre/post pairing, threading,
 * a throwing listener being logged and skipped).
 *
 * <p>Alchemy's own pill ritual fires no events at all today, so this class is
 * shaped instead after {@link ItemEvents}' weapon-refinement pair
 * ({@code RefinementStartEvent}/{@code RefinementCompleteEvent}), the other
 * timed-ritual precedent in this package - {@code PreInscribeCompleteEvent}
 * even follows {@code PreRefinementCompleteEvent}'s "re-weight the roll's
 * inputs before it happens" shape, adapted to the two inputs
 * {@code TalismanManager#rollOutcome} actually takes.</p>
 *
 * <p><b>Who fires what:</b> {@code TalismanCmd} fires the Inscribe-Start pair
 * when a ritual begins; {@code TalismanInscribeSystem} fires the Inscribe-
 * Complete pair when one resolves (naturally or by interruption - see that
 * system's own remarks on why {@code PreInscribeCompleteEvent} only fires on a
 * NATURAL completion, never an interruption). The Use pair is NOT fired by
 * anything in this engine slice - using a talisman is slice S2's
 * use-interaction, which should fire {@link #firePreTalismanUse}/
 * {@link #fireTalismanUse} itself once it exists; see the handoff doc.</p>
 */
public final class TalismanEvents {
    private TalismanEvents(){}

    /** How a completed (or interrupted) inscription ritual resolved. */
    public enum InscribeOutcome {
        SUCCESS,
        /** Failed outright - wastes the spent materials and Qi, nothing else happens. */
        FAILED,
        /** Failed AND the flame turned on the inscriber - see TalismanConfig's Inscribe-Botch-* fields. */
        BOTCH
    }

    // --- Post-events ---

    /** An inscription ritual began; materials and the Qi floor check already passed. */
    public record InscribeStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nonnull String talismanId, @Nonnull DaoElement element, float qiDrainPerSecond) {}

    /** An inscription ritual resolved. {@code grade}/{@code stack} are null unless {@code outcome} is SUCCESS. */
    public record InscribeCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                        @Nonnull String talismanId, @Nonnull DaoElement element,
                                        @Nonnull InscribeOutcome outcome, @Nullable TalismanGrade grade,
                                        @Nullable ItemStack stack) {}

    /**
     * A talisman was used and its effect applied. {@code remainingCharges} is
     * what is left AFTER this use - the stack is gone once it reaches 0. Not
     * fired by anything in this engine slice; see this class's own doc.
     */
    public record TalismanUseEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                   @Nonnull String talismanId, @Nonnull TalismanGrade grade, int remainingCharges) {}

    // --- Pre-events ---

    /** An inscription ritual is about to begin. Cancel to refuse it (no materials or Qi are spent). */
    public static final class PreInscribeStartEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String talismanId;
        private final DaoElement element;

        public PreInscribeStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nonnull String talismanId, @Nonnull DaoElement element){
            this.ref = ref;
            this.player = player;
            this.talismanId = talismanId;
            this.element = element;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public String talismanId(){ return this.talismanId; }
        @Nonnull public DaoElement element(){ return this.element; }
    }

    /**
     * An inscription ritual is about to resolve into a grade. Cancel to
     * abandon it silently - the same "materials/Qi already sunk, nothing is
     * produced" shape {@link ItemEvents.PreRefinementCompleteEvent} uses.
     * {@link #setMasteryLadderFraction} and {@link #setAffinityFraction}
     * re-weight the two inputs {@code TalismanManager#rollOutcome} actually
     * rolls against - the supported way to reshape a talisman's grade odds
     * from an addon (e.g. a race or Sacred Body constitution granting a flat
     * affinity bonus for the roll ONLY, without touching the persisted
     * DaoComponent). Fires only on a natural completion, never an
     * interruption - see this class's own doc.
     */
    public static final class PreInscribeCompleteEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String talismanId;
        private final DaoElement element;
        private float masteryLadderFraction;
        private float affinityFraction;

        public PreInscribeCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                        @Nonnull String talismanId, @Nonnull DaoElement element,
                                        float masteryLadderFraction, float affinityFraction){
            this.ref = ref;
            this.player = player;
            this.talismanId = talismanId;
            this.element = element;
            this.masteryLadderFraction = masteryLadderFraction;
            this.affinityFraction = affinityFraction;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public String talismanId(){ return this.talismanId; }
        @Nonnull public DaoElement element(){ return this.element; }
        /** 0..1, as fed to {@code TalismanManager#rollOutcome}. */
        public float masteryLadderFraction(){ return this.masteryLadderFraction; }
        public void setMasteryLadderFraction(float value){ this.masteryLadderFraction = value; }
        /** 0..1, as fed to {@code TalismanManager#rollOutcome}. */
        public float affinityFraction(){ return this.affinityFraction; }
        public void setAffinityFraction(float value){ this.affinityFraction = value; }
    }

    /** A talisman is about to be used. Cancel to refuse it (the charge is not spent). Not fired by this engine slice; see this class's own doc. */
    public static final class PreTalismanUseEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String talismanId;
        private final TalismanGrade grade;

        public PreTalismanUseEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                   @Nonnull String talismanId, @Nonnull TalismanGrade grade){
            this.ref = ref;
            this.player = player;
            this.talismanId = talismanId;
            this.grade = grade;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public String talismanId(){ return this.talismanId; }
        @Nonnull public TalismanGrade grade(){ return this.grade; }
    }

    // --- Listener registration ---

    private static final List<Consumer<InscribeStartEvent>> INSCRIBE_START = EventBus.newListenerList();
    private static final List<Consumer<PreInscribeStartEvent>> PRE_INSCRIBE_START = EventBus.newListenerList();
    private static final List<Consumer<InscribeCompleteEvent>> INSCRIBE_COMPLETE = EventBus.newListenerList();
    private static final List<Consumer<PreInscribeCompleteEvent>> PRE_INSCRIBE_COMPLETE = EventBus.newListenerList();
    private static final List<Consumer<TalismanUseEvent>> TALISMAN_USE = EventBus.newListenerList();
    private static final List<Consumer<PreTalismanUseEvent>> PRE_TALISMAN_USE = EventBus.newListenerList();

    public static void onInscribeStart(@Nonnull Consumer<InscribeStartEvent> listener){ INSCRIBE_START.add(listener); }
    public static void onPreInscribeStart(@Nonnull Consumer<PreInscribeStartEvent> listener){ PRE_INSCRIBE_START.add(listener); }
    public static void onInscribeComplete(@Nonnull Consumer<InscribeCompleteEvent> listener){ INSCRIBE_COMPLETE.add(listener); }
    public static void onPreInscribeComplete(@Nonnull Consumer<PreInscribeCompleteEvent> listener){ PRE_INSCRIBE_COMPLETE.add(listener); }
    public static void onTalismanUse(@Nonnull Consumer<TalismanUseEvent> listener){ TALISMAN_USE.add(listener); }
    public static void onPreTalismanUse(@Nonnull Consumer<PreTalismanUseEvent> listener){ PRE_TALISMAN_USE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems/commands; not API) ---

    public static void fireInscribeStart(@Nonnull InscribeStartEvent event){ EventBus.dispatch(INSCRIBE_START, event, "InscribeStartEvent"); }
    public static boolean firePreInscribeStart(@Nonnull PreInscribeStartEvent event){ return EventBus.fire(PRE_INSCRIBE_START, event, "PreInscribeStartEvent"); }
    public static void fireInscribeComplete(@Nonnull InscribeCompleteEvent event){ EventBus.dispatch(INSCRIBE_COMPLETE, event, "InscribeCompleteEvent"); }
    public static boolean firePreInscribeComplete(@Nonnull PreInscribeCompleteEvent event){ return EventBus.fire(PRE_INSCRIBE_COMPLETE, event, "PreInscribeCompleteEvent"); }
    public static void fireTalismanUse(@Nonnull TalismanUseEvent event){ EventBus.dispatch(TALISMAN_USE, event, "TalismanUseEvent"); }
    public static boolean firePreTalismanUse(@Nonnull PreTalismanUseEvent event){ return EventBus.fire(PRE_TALISMAN_USE, event, "PreTalismanUseEvent"); }
}
