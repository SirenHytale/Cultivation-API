package plugin.siren.API;

import plugin.siren.ECS.Dao.DaoElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Elemental Dao Duel events - the Spirit Stone escrow layered on top of
 * {@link DuelEvents}' plain Qi-wager duels. See {@code
 * plugin.siren.Utils.DaoDuel.DaoDuelManager} for the challenge/accept/payout
 * flow these fire from, and {@link DuelEvents} for the conventions every
 * {@code *Events} class in this package shares (players identified by UUID,
 * pre-events cancellable/rescalable, post-events observational only).
 */
public final class DaoDuelEvents {
    private DaoDuelEvents(){}

    /**
     * How a decided Dao Duel was resolved. {@code DaoDuelManager}'s allowed
     * {@code DuelManager} surface ({@code onDuelEnd} only, no {@code
     * onDuelPayout}) never observes a yield directly, but {@code
     * DuelYieldCmd} now calls {@code DaoDuelManager#reportYield} explicitly
     * right after ending the underlying Qi duel (Fix 2 - see {@code
     * DaoDuelManager}'s own class doc, "The known gap this still leaves:
     * duel-yield"), which attributes the yielder as the loser and fires this
     * event with {@link #YIELD}.
     */
    public enum DaoDuelEndReason {
        /** One duelist died; the Spirit Stone pot was settled. */
        DEATH,
        /** The loser yielded ({@code /cultivation duel yield}) rather than being killed; the Spirit Stone pot was still settled against them. */
        YIELD
    }

    // --- Post-events ---

    /**
     * A decided Dao Duel's Spirit Stone pot has been settled - {@code
     * spiritStoneAmountPaid} is what actually moved from the loser's escrow
     * (plus any elemental bonus that could be charged) to the winner; the
     * winner's own stake is always returned to them separately and is not
     * counted here. {@code winnerElement}/{@code loserElement} may be null if
     * a side had no chosen element at resolution time (only possible when
     * {@code DaoDuel-Requires-Chosen-Element} is off) - {@code
     * winnerCountered} is false whenever either is null.
     *
     * <p>{@code decidedByOpponentDamage} is true ONLY when the loser died to
     * the winner's own damage (their weapon, their technique, or their spirit
     * beast, which is credited to its owner). It is false for a {@link
     * DaoDuelEndReason#YIELD}, and false for a death to lava, a fall,
     * drowning, a formation trap, a third party, or a source that could not be
     * attributed. It exists for {@code TournamentWagerManager}, which pays
     * real Spirit Stones to spectators and must not pay on a "win" the winner
     * did not cause - the duel itself still resolves identically either way
     * (see {@code DuelManager#endDuel(UUID, DuelEvents.DuelEndReason,
     * UUID)}).</p>
     */
    public record DaoDuelEndEvent(@Nonnull UUID winner, @Nonnull UUID loser, long spiritStoneAmountPaid,
                                   @Nullable DaoElement winnerElement, @Nullable DaoElement loserElement,
                                   boolean winnerCountered, @Nonnull DaoDuelEndReason reason,
                                   boolean decidedByOpponentDamage) {

        /**
         * The pre-attribution form - {@code decidedByOpponentDamage} defaults
         * to <b>false</b>, the conservative answer. Kept so a listener or
         * caller written before Tournament Wagers still compiles unchanged.
         */
        public DaoDuelEndEvent(@Nonnull UUID winner, @Nonnull UUID loser, long spiritStoneAmountPaid,
                               @Nullable DaoElement winnerElement, @Nullable DaoElement loserElement,
                               boolean winnerCountered, @Nonnull DaoDuelEndReason reason){
            this(winner, loser, spiritStoneAmountPaid, winnerElement, loserElement, winnerCountered, reason, false);
        }
    }

    // --- Pre-events ---

    /** A Dao Duel challenge is about to be issued (the Qi wager, if any, is handled entirely by {@code DuelEvents.PreDuelChallengeEvent} - this is the Spirit Stone side only). Cancel to refuse it; {@link #setSpiritStoneAmount} to re-tune the stake (the configured maximum is re-checked afterward). */
    public static final class PreDaoDuelChallengeEvent extends CancellableEvent {
        private final UUID challenger;
        private final UUID challenged;
        private long spiritStoneAmount;

        public PreDaoDuelChallengeEvent(@Nonnull UUID challenger, @Nonnull UUID challenged, long spiritStoneAmount){
            this.challenger = challenger;
            this.challenged = challenged;
            this.spiritStoneAmount = spiritStoneAmount;
        }

        @Nonnull public UUID challenger(){ return this.challenger; }
        @Nonnull public UUID challenged(){ return this.challenged; }
        /** Spirit Stones each side will escrow if the challenge is accepted. */
        public long spiritStoneAmount(){ return this.spiritStoneAmount; }
        public void setSpiritStoneAmount(long amount){ this.spiritStoneAmount = amount; }
    }

    /** A decided Dao Duel's Spirit Stone pot is about to move. Cancel to return the loser's stake to the loser untouched; {@link #setAmount} to re-scale how much of the loser's escrowed stake is forfeited (capped by what was actually escrowed - the winner's own stake is unaffected either way, see {@code DaoDuelEndEvent}'s own doc). */
    public static final class PreDaoDuelPayoutEvent extends CancellableEvent {
        private final UUID winner;
        private final UUID loser;
        private long amount;

        public PreDaoDuelPayoutEvent(@Nonnull UUID winner, @Nonnull UUID loser, long amount){
            this.winner = winner;
            this.loser = loser;
            this.amount = amount;
        }

        @Nonnull public UUID winner(){ return this.winner; }
        @Nonnull public UUID loser(){ return this.loser; }
        /** Spirit Stones the loser is asked to forfeit from their escrowed stake. What actually moves is capped by what they escrowed. */
        public long amount(){ return this.amount; }
        public void setAmount(long amount){ this.amount = amount; }
    }

    // --- Listener registration ---

    private static final List<Consumer<DaoDuelEndEvent>> END = EventBus.newListenerList();
    private static final List<Consumer<PreDaoDuelChallengeEvent>> PRE_CHALLENGE = EventBus.newListenerList();
    private static final List<Consumer<PreDaoDuelPayoutEvent>> PRE_PAYOUT = EventBus.newListenerList();

    public static void onDaoDuelEnd(@Nonnull Consumer<DaoDuelEndEvent> listener){ END.add(listener); }
    public static void onPreDaoDuelChallenge(@Nonnull Consumer<PreDaoDuelChallengeEvent> listener){ PRE_CHALLENGE.add(listener); }
    public static void onPreDaoDuelPayout(@Nonnull Consumer<PreDaoDuelPayoutEvent> listener){ PRE_PAYOUT.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireDaoDuelEnd(@Nonnull DaoDuelEndEvent event){ EventBus.dispatch(END, event, "DaoDuelEndEvent"); }
    public static boolean firePreDaoDuelChallenge(@Nonnull PreDaoDuelChallengeEvent event){ return EventBus.fire(PRE_CHALLENGE, event, "PreDaoDuelChallengeEvent"); }
    public static boolean firePreDaoDuelPayout(@Nonnull PreDaoDuelPayoutEvent event){ return EventBus.fire(PRE_PAYOUT, event, "PreDaoDuelPayoutEvent"); }
}
