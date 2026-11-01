package plugin.siren.API;

import plugin.siren.ECS.Dao.DaoElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dao Domain (道域) events - the unfold, the collapse and the clash. Exactly the {@link
 * SoulEscapeEvents} shape: post-events are records, the pre-event is a {@code
 * static final class X extends CancellableEvent} with settable tuning fields,
 * one {@link EventBus#newListenerList()} per event, {@code onX(Consumer<X>)}
 * registration, internal {@code fireX} dispatchers. See {@link CultivationEvents}
 * for the conventions every {@code *Events} class in this package shares.
 *
 * <p>Players are identified by UUID, matching {@link SoulEscapeEvents} and {@link
 * DuelEvents}: a domain can collapse because its caster disconnected, and by then
 * there is no entity to hand over. The element is the caster's Dao element, or
 * {@code null} for a domain cast in Heavenly Qi (no Dao, or an element with no
 * flavor of its own).</p>
 *
 * <p>A collapse caused by the caster <em>disconnecting</em> fires nothing: the
 * entity that held the domain is already gone and nothing ticks to notice. The
 * domain's effects lapse within a second on their own.</p>
 */
public final class DaoDomainEvents {
    private DaoDomainEvents(){}

    /** Why a domain ended. */
    public enum CollapseReason {
        /** The duration ran out. */
        EXPIRED,
        /** The caster stepped outside it. */
        LEFT_AREA,
        /** The caster died (or became a fleeing soul). */
        DIED,
        /** The caster changed world; the anchor is not in the world they are in. */
        LEFT_WORLD,
        /** A refusal gate tripped mid-domain: meditating, mid-breakthrough, a trial, the Sea, a sanctuary. */
        GATE,
        /** Dao Domains (or technique use) was switched off. */
        DISABLED,
        /**
         * The domain lost a clash with a hostile domain or a sect array, or tied one (a tie shatters
         * both domains). A {@link DomainClashEvent} fires for the same clash, just before this.
         */
        CLASHED
    }

    /** How a clash came out, from the point of view of {@link DomainClashEvent#first()}. */
    public enum Outcome {
        /** {@code first} had the higher clash power (against an array: the domain beat the array). */
        FIRST_WON,
        /** {@code second} had the higher clash power (against an array: the array beat the domain). */
        SECOND_WON,
        /** Equal power. Two domains both shatter; a domain against an array shatters alone and the array holds. */
        TIE
    }

    // --- Pre-event (cancellable; numbers re-tunable) ---

    /**
     * A domain is about to unfold; the cast's gates have all passed but no Qi has
     * been spent. Cancel -&gt; the cast fails exactly like a refusal: nothing
     * charged, no cooldown, no domain. Radius, duration and Qi cost may be
     * re-tuned; the values left in the event are clamped to the engine's sane
     * range before use.
     */
    public static final class PreDomainUnfoldEvent extends CancellableEvent {
        private final UUID caster;
        @Nullable private final DaoElement element;
        private final int tier;
        private float radius;
        private float durationSeconds;
        private float qiCost;

        public PreDomainUnfoldEvent(@Nonnull UUID caster, @Nullable DaoElement element, int tier,
                                    float radius, float durationSeconds, float qiCost){
            this.caster = caster;
            this.element = element;
            this.tier = tier;
            this.radius = radius;
            this.durationSeconds = durationSeconds;
            this.qiCost = qiCost;
        }

        @Nonnull public UUID caster(){ return this.caster; }
        @Nullable public DaoElement element(){ return this.element; }
        /** Zero-based index: 0 at the floor realm, 1 one realm up, 2 two or more up (players see these as tiers 1-3). */
        public int tier(){ return this.tier; }
        public float radius(){ return this.radius; }
        public void setRadius(float radius){ this.radius = radius; }
        public float durationSeconds(){ return this.durationSeconds; }
        public void setDurationSeconds(float durationSeconds){ this.durationSeconds = durationSeconds; }
        /** The Qi that will be charged (0 for an admin bypass or a Creative caster, who pay nothing). */
        public float qiCost(){ return this.qiCost; }
        public void setQiCost(float qiCost){ this.qiCost = qiCost; }
    }

    // --- Post-events ---

    public record DomainUnfoldEvent(@Nonnull UUID caster, @Nullable DaoElement element, int tier, float radius,
                                    float durationSeconds, float qiSpent) {}

    public record DomainCollapseEvent(@Nonnull UUID caster, @Nullable DaoElement element,
                                      @Nonnull CollapseReason reason, float secondsActive) {}

    /**
     * Two hostile domains, or a domain and a hostile sect array, overlapped and a clash was settled. Fires
     * once per clash, on the world thread of the casters, and before the {@link DomainCollapseEvent}s the
     * clash causes.
     *
     * <p><b>Power.</b> A domain's clash power is its caster's realm ordinal (Body Refinement 0 ... Void
     * Refinement 6) plus {@code Domain-Clash-Counter-Bonus} (default 1) if the domain's Dao element overcomes
     * the opponent's in the Wu Xing cycles ({@code DaoElement.counters}); a Heavenly Qi domain (no element)
     * never counters and is never countered. A sect array's power is the higher of its configured base and the
     * strongest defender standing in its territory, plus the same bonus if its sect's element overcomes the
     * domain's. The higher power wins; equal power is a {@link Outcome#TIE}.</p>
     *
     * <p><b>Who is first.</b> {@code first} is the caster whose pulse found the overlap - in the ordinary case
     * the one who unfolded last, because a domain is anchored and an overlap begins when one of the two is
     * cast. Clashes only happen between domains whose owners count each other enemy (world PvP, a duel, an
     * opposed Path War side, or a live siege between their sects), never between allies or neutrals.</p>
     *
     * @param first          the caster whose pulse settled the clash
     * @param second         the other domain's caster; {@code null} when the opponent was a sect array. May be
     *                       a synthetic id that matches no player when an admin summoned a test rival
     *                       ({@code /cultivation admin domain rival}).
     * @param arraySect      the defending sect's name when the opponent was a sect array, else {@code null}
     * @param firstPower     {@code first}'s clash power, counter bonus included
     * @param secondPower    the opponent's clash power, counter bonus included
     * @param outcome        who won
     * @param counterDecided true if a Wu Xing counter changed the result: with the bonuses removed the outcome
     *                       would have been different
     */
    public record DomainClashEvent(@Nonnull UUID first, @Nullable UUID second, @Nullable String arraySect,
                                   int firstPower, int secondPower, @Nonnull Outcome outcome,
                                   boolean counterDecided) {}

    // --- Listener registration ---

    private static final List<Consumer<PreDomainUnfoldEvent>> PRE_UNFOLD = EventBus.newListenerList();
    private static final List<Consumer<DomainUnfoldEvent>> UNFOLD = EventBus.newListenerList();
    private static final List<Consumer<DomainCollapseEvent>> COLLAPSE = EventBus.newListenerList();
    private static final List<Consumer<DomainClashEvent>> CLASH = EventBus.newListenerList();

    public static void onPreDomainUnfold(@Nonnull Consumer<PreDomainUnfoldEvent> listener){ PRE_UNFOLD.add(listener); }
    public static void onDomainUnfold(@Nonnull Consumer<DomainUnfoldEvent> listener){ UNFOLD.add(listener); }
    public static void onDomainCollapse(@Nonnull Consumer<DomainCollapseEvent> listener){ COLLAPSE.add(listener); }
    public static void onDomainClash(@Nonnull Consumer<DomainClashEvent> listener){ CLASH.add(listener); }

    // --- Internal dispatch (called by DaoDomainManager; not API) ---

    public static boolean firePreDomainUnfold(@Nonnull PreDomainUnfoldEvent event){ return EventBus.fire(PRE_UNFOLD, event, "PreDomainUnfoldEvent"); }
    public static void fireDomainUnfold(@Nonnull DomainUnfoldEvent event){ EventBus.dispatch(UNFOLD, event, "DomainUnfoldEvent"); }
    public static void fireDomainCollapse(@Nonnull DomainCollapseEvent event){ EventBus.dispatch(COLLAPSE, event, "DomainCollapseEvent"); }
    public static void fireDomainClash(@Nonnull DomainClashEvent event){ EventBus.dispatch(CLASH, event, "DomainClashEvent"); }
}
