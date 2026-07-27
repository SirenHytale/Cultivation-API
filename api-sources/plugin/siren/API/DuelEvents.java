package plugin.siren.API;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Qi-wager duel events - challenging, accepting, and how a duel resolves. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares.
 *
 * <p>Players are identified by UUID because a duel routinely outlives one
 * participant's session (that is exactly what voids it). Resolve one with
 * {@code Universe.get().getPlayer(uuid)} and check {@code isValid()}.</p>
 */
public final class DuelEvents {
    private DuelEvents(){}

    /** How a duel stopped being active. */
    public enum DuelEndReason {
        /** One duelist died; the wager transfers. */
        DEATH,
        /** Undecidable - a participant left, or the duel ran past its max duration. No wager moves. */
        VOIDED
    }

    // --- Post-events ---

    /** A challenge was issued and is now pending the other player's answer. */
    public record DuelChallengeEvent(@Nonnull UUID challenger, @Nonnull UUID challenged, int wager) {}

    /** A challenge was declined; no duel started. */
    public record DuelDeclineEvent(@Nonnull UUID challenger, @Nonnull UUID challenged) {}

    /** A duel is now live - both players are flagged as dueling. */
    public record DuelStartEvent(@Nonnull UUID challenger, @Nonnull UUID challenged, int wager) {}

    /** A duel ended. For DEATH, {@code winner}/{@code loser} are meaningful and the payout has been queued; for VOIDED they are simply the two participants and nothing changes hands. */
    public record DuelEndEvent(@Nonnull UUID winner, @Nonnull UUID loser, int wager, @Nonnull DuelEndReason reason) {}

    /** A decided duel's wager actually moved: {@code amount} is what the loser could cover, which is exactly what the winner gained. */
    public record DuelPayoutEvent(@Nonnull UUID winner, @Nonnull UUID loser, int amount) {}

    // --- Pre-events ---

    /** A challenge is about to be issued. Cancel to refuse it; {@link #setWager} to force a different stake (the configured maximum is re-checked afterward). */
    public static final class PreDuelChallengeEvent extends CancellableEvent {
        private final UUID challenger;
        private final UUID challenged;
        private int wager;

        public PreDuelChallengeEvent(@Nonnull UUID challenger, @Nonnull UUID challenged, int wager){
            this.challenger = challenger;
            this.challenged = challenged;
            this.wager = wager;
        }

        @Nonnull public UUID challenger(){ return this.challenger; }
        @Nonnull public UUID challenged(){ return this.challenged; }
        /** Banked Qi at stake for each side. */
        public int wager(){ return this.wager; }
        public void setWager(int wager){ this.wager = wager; }
    }

    /** A duel is about to start. Cancel to refuse it - the challenge is consumed either way, so the challenger must issue a fresh one. */
    public static final class PreDuelStartEvent extends CancellableEvent {
        private final UUID challenger;
        private final UUID challenged;
        private int wager;

        public PreDuelStartEvent(@Nonnull UUID challenger, @Nonnull UUID challenged, int wager){
            this.challenger = challenger;
            this.challenged = challenged;
            this.wager = wager;
        }

        @Nonnull public UUID challenger(){ return this.challenger; }
        @Nonnull public UUID challenged(){ return this.challenged; }
        /** Banked Qi at stake; re-tuning it here is what the live duel will actually pay out. */
        public int wager(){ return this.wager; }
        public void setWager(int wager){ this.wager = wager; }
    }

    /** A decided duel's wager is about to move. Cancel to let the winner take nothing; {@link #setAmount} to re-scale the transfer (the loser can still only forfeit what they actually hold). */
    public static final class PreDuelPayoutEvent extends CancellableEvent {
        private final UUID winner;
        private final UUID loser;
        private int amount;

        public PreDuelPayoutEvent(@Nonnull UUID winner, @Nonnull UUID loser, int amount){
            this.winner = winner;
            this.loser = loser;
            this.amount = amount;
        }

        @Nonnull public UUID winner(){ return this.winner; }
        @Nonnull public UUID loser(){ return this.loser; }
        /** Qi the loser is asked to forfeit. What actually moves is capped by what they hold. */
        public int amount(){ return this.amount; }
        public void setAmount(int amount){ this.amount = amount; }
    }

    // --- Listener registration ---

    private static final List<Consumer<DuelChallengeEvent>> CHALLENGE = EventBus.newListenerList();
    private static final List<Consumer<PreDuelChallengeEvent>> PRE_CHALLENGE = EventBus.newListenerList();
    private static final List<Consumer<DuelDeclineEvent>> DECLINE = EventBus.newListenerList();
    private static final List<Consumer<DuelStartEvent>> START = EventBus.newListenerList();
    private static final List<Consumer<PreDuelStartEvent>> PRE_START = EventBus.newListenerList();
    private static final List<Consumer<DuelEndEvent>> END = EventBus.newListenerList();
    private static final List<Consumer<DuelPayoutEvent>> PAYOUT = EventBus.newListenerList();
    private static final List<Consumer<PreDuelPayoutEvent>> PRE_PAYOUT = EventBus.newListenerList();

    public static void onDuelChallenge(@Nonnull Consumer<DuelChallengeEvent> listener){ CHALLENGE.add(listener); }
    public static void onPreDuelChallenge(@Nonnull Consumer<PreDuelChallengeEvent> listener){ PRE_CHALLENGE.add(listener); }
    public static void onDuelDecline(@Nonnull Consumer<DuelDeclineEvent> listener){ DECLINE.add(listener); }
    public static void onDuelStart(@Nonnull Consumer<DuelStartEvent> listener){ START.add(listener); }
    public static void onPreDuelStart(@Nonnull Consumer<PreDuelStartEvent> listener){ PRE_START.add(listener); }
    public static void onDuelEnd(@Nonnull Consumer<DuelEndEvent> listener){ END.add(listener); }
    public static void onDuelPayout(@Nonnull Consumer<DuelPayoutEvent> listener){ PAYOUT.add(listener); }
    public static void onPreDuelPayout(@Nonnull Consumer<PreDuelPayoutEvent> listener){ PRE_PAYOUT.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireDuelChallenge(@Nonnull DuelChallengeEvent event){ EventBus.dispatch(CHALLENGE, event, "DuelChallengeEvent"); }
    public static boolean firePreDuelChallenge(@Nonnull PreDuelChallengeEvent event){ return EventBus.fire(PRE_CHALLENGE, event, "PreDuelChallengeEvent"); }
    public static void fireDuelDecline(@Nonnull DuelDeclineEvent event){ EventBus.dispatch(DECLINE, event, "DuelDeclineEvent"); }
    public static void fireDuelStart(@Nonnull DuelStartEvent event){ EventBus.dispatch(START, event, "DuelStartEvent"); }
    public static boolean firePreDuelStart(@Nonnull PreDuelStartEvent event){ return EventBus.fire(PRE_START, event, "PreDuelStartEvent"); }
    public static void fireDuelEnd(@Nonnull DuelEndEvent event){ EventBus.dispatch(END, event, "DuelEndEvent"); }
    public static void fireDuelPayout(@Nonnull DuelPayoutEvent event){ EventBus.dispatch(PAYOUT, event, "DuelPayoutEvent"); }
    public static boolean firePreDuelPayout(@Nonnull PreDuelPayoutEvent event){ return EventBus.fire(PRE_PAYOUT, event, "PreDuelPayoutEvent"); }
}
