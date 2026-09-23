package plugin.siren.API;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tournament Wager events - spectators staking Spirit Stones on individual Dao
 * Duel Tournament matches. See {@code
 * plugin.siren.Utils.DaoDuelTournament.Wager.TournamentWagerManager} for the
 * parimutuel market engine these fire from, {@link DaoDuelEvents} for the
 * underlying elemental duel, and {@link DuelEvents} for the conventions every
 * {@code *Events} class in this package shares (players identified by UUID,
 * pre-events cancellable, post-events observational only).
 *
 * <h2>Execution context - read this before writing a listener</h2>
 *
 * <p><b>{@link PreWagerPlacedEvent} and {@link WagerPlacedEvent} fire with NO
 * monitor of this mod's held at all.</b> Both run on the betting player's own
 * command thread, in {@code TournamentWagerManager#placeBet}'s phase 1 and
 * after its phase 2 respectively - deliberately outside that class's monitor,
 * because a listener that called any {@code synchronized
 * DaoDuelTournamentManager} method while the wager monitor was held would
 * deadlock against the tournament pump thread. Everything a
 * {@code PreWagerPlacedEvent} listener can see is re-validated under the lock
 * after it returns, so cancelling is honored but a listener cannot rely on any
 * gate still holding at the moment the stones actually move.</p>
 *
 * <p><b>{@link WagerMarketResolvedEvent} and {@link WagerMarketVoidedEvent}
 * fire while {@code DaoDuelTournamentManager}'s monitor IS held.</b> They are
 * reached from that class's match-resolution path ({@code
 * DaoDuelTournamentManager#resolveDecided} / {@code #resolveAmbiguous}, both
 * inside its {@code synchronized pump()}); {@code TournamentWagerManager} has
 * released its OWN monitor by then, but the tournament's is still held. This
 * is exactly the precedent {@code DaoDuelEvents.DaoDuelEndEvent} already sets:
 * {@code plugin/siren/Utils/DaoDuel/DaoDuelManager.java:754} fires it from
 * {@code fireEndEvent}, reached only from the {@code synchronized}
 * {@code handleDecided} ({@code DaoDuelManager.java:610}) and {@code
 * reportYield} ({@code :685}). D-061 records the same shape for {@code
 * DuelEndEvent} and D-063 for {@code CelestialEvents}.</p>
 *
 * <p>The rule for a listener on those two is therefore absolute: <b>zero
 * locking and zero cross-manager calls</b>. Do not synchronize, do not touch a
 * concurrency primitive that another thread could be holding, and never call
 * back into {@code DaoDuelTournamentManager}, {@code TournamentWagerManager},
 * {@code DaoDuelManager} or {@code DuelManager}. If a listener needs to do
 * real work, copy what it needs out of the event and hand it to your own
 * queue, the way {@code DaoDuelTournamentManager#onDaoDuelEndRaw} does.</p>
 *
 * <h2>No event fires for the server-restart refund</h2>
 *
 * <p>{@code TournamentWagerManager#init} voids and refunds every leftover
 * wager from a previous run, but deliberately fires NOTHING - it runs
 * synchronously inside {@code Cultivation.setup()}, before addons have had the
 * chance to register a listener and before {@code Universe} is usable at all.
 * A listener that needs to know about restart refunds should read the
 * claimable parcels instead.</p>
 */
public final class WagerEvents {
    private WagerEvents(){}

    /** Why a market paid nobody and returned every stake untouched. */
    public enum WagerVoidReason {
        /** One duelist was declared the winner without a real duel being fought (a no-show / deadline forfeit). */
        FORFEIT,
        /** Neither duelist was eligible at the deadline, so both were eliminated. */
        DOUBLE_ELIMINATION,
        /** The underlying duel was voided - a disconnect, or {@code Duel-Max-Duration-Seconds}. */
        VOIDED_DUEL,
        /**
         * The loser yielded ({@code /cultivation duel yield}) instead of being
         * beaten. The bracket still advances the winner, but no market pays:
         * yielding is free and instant, so paying on one would make
         * "arrange for your friend to yield" the cheapest fix in the game.
         */
        YIELD,
        /**
         * The loser died, but not to the opponent's own damage - lava, a fall,
         * drowning, a formation trap, a third party, or a source the death
         * path could not attribute at all. The bracket still advances the
         * surviving duelist (a dead duelist always loses their match), but a
         * market only pays for a fight actually won.
         */
        NOT_OPPONENT_KILL,
        /** The match was genuinely decided, but a side ended up with fewer than {@code Wager-Min-Bettors-Per-Side} distinct bettors - the anti-collusion rule. */
        TOO_FEW_BETTORS,
        /** The match was genuinely decided, but a side's combined stake was below {@code Wager-Min-Pool-Per-Side} - the anti-collusion rule that costs an attacker real Spirit Stones. */
        POOL_TOO_THIN,
        /** The whole tournament was reset, cancelled or re-opened while this market was live. */
        TOURNAMENT_RESET,
        /**
         * The conservation invariant failed its self-check, so nothing was
         * paid and every stake was returned. This should be unreachable; if a
         * listener ever sees it, the server log carries a SEVERE line with the
         * full arithmetic.
         */
        ACCOUNTING_FAILURE
    }

    // --- Post-events ---

    /** A spectator's Spirit Stones have been charged and their stake recorded. {@code backed} is the duelist they are backing. */
    public record WagerPlacedEvent(int matchNumber, @Nonnull UUID bettor, @Nonnull UUID backed, long amount) {}

    /**
     * A market resolved and paid out.
     *
     * @param payouts bettor uuid to the TOTAL Spirit Stones handed back to
     * them - their own returned stake plus their share of the losing pool.
     * A bettor who backed the loser is present with a payout of 0 only if
     * they are also in {@code stakes}; read {@code stakes} for what each one
     * put in. Both maps are unmodifiable.
     * @param stakes bettor uuid to what they originally staked.
     * @param burned the house cut plus the indivisible remainder - Spirit
     * Stones destroyed rather than paid to anyone. There is no house account
     * and no organizer; see {@code TournamentWagerManager}'s own doc.
     */
    public record WagerMarketResolvedEvent(int matchNumber, @Nonnull UUID winner,
                                           @Nonnull Map<UUID, Long> stakes,
                                           @Nonnull Map<UUID, Long> payouts,
                                           long winnersPool, long losersPool, long burned) {}

    /** A market paid nobody; every stake in {@code refunds} was returned in full. Unmodifiable. */
    public record WagerMarketVoidedEvent(int matchNumber, @Nonnull WagerVoidReason reason,
                                         @Nonnull Map<UUID, Long> refunds) {}

    // --- Pre-events ---

    /**
     * A spectator is about to be charged for a stake. Every configured gate
     * (feature enabled, market open, not a duelist, not a live entrant, not a
     * sect-mate, per-bet / per-tournament / per-pool limits) has ALREADY
     * passed by the time this fires; cancelling refuses the bet with no
     * Spirit Stones moved and nothing recorded.
     *
     * <p>The amount is deliberately not settable. Re-tuning a stake here would
     * silently move a player's stones by an amount they never typed, and the
     * per-bet and per-pool ceilings have already been validated against the
     * typed figure.</p>
     */
    public static final class PreWagerPlacedEvent extends CancellableEvent {
        private final int matchNumber;
        private final UUID bettor;
        private final UUID backed;
        private final long amount;

        public PreWagerPlacedEvent(int matchNumber, @Nonnull UUID bettor, @Nonnull UUID backed, long amount){
            this.matchNumber = matchNumber;
            this.bettor = bettor;
            this.backed = backed;
            this.amount = amount;
        }

        public int matchNumber(){ return this.matchNumber; }
        @Nonnull public UUID bettor(){ return this.bettor; }
        /** The duelist this stake backs. */
        @Nonnull public UUID backed(){ return this.backed; }
        /** Spirit Stones about to be charged. */
        public long amount(){ return this.amount; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PreWagerPlacedEvent>> PRE_PLACED = EventBus.newListenerList();
    private static final List<Consumer<WagerPlacedEvent>> PLACED = EventBus.newListenerList();
    private static final List<Consumer<WagerMarketResolvedEvent>> RESOLVED = EventBus.newListenerList();
    private static final List<Consumer<WagerMarketVoidedEvent>> VOIDED = EventBus.newListenerList();

    public static void onPreWagerPlaced(@Nonnull Consumer<PreWagerPlacedEvent> listener){ PRE_PLACED.add(listener); }
    public static void onWagerPlaced(@Nonnull Consumer<WagerPlacedEvent> listener){ PLACED.add(listener); }
    public static void onWagerMarketResolved(@Nonnull Consumer<WagerMarketResolvedEvent> listener){ RESOLVED.add(listener); }
    public static void onWagerMarketVoided(@Nonnull Consumer<WagerMarketVoidedEvent> listener){ VOIDED.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static boolean firePreWagerPlaced(@Nonnull PreWagerPlacedEvent event){ return EventBus.fire(PRE_PLACED, event, "PreWagerPlacedEvent"); }
    public static void fireWagerPlaced(@Nonnull WagerPlacedEvent event){ EventBus.dispatch(PLACED, event, "WagerPlacedEvent"); }
    public static void fireWagerMarketResolved(@Nonnull WagerMarketResolvedEvent event){ EventBus.dispatch(RESOLVED, event, "WagerMarketResolvedEvent"); }
    public static void fireWagerMarketVoided(@Nonnull WagerMarketVoidedEvent event){ EventBus.dispatch(VOIDED, event, "WagerMarketVoidedEvent"); }
}
