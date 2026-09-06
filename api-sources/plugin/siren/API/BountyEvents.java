package plugin.siren.API;

import plugin.siren.Utils.Bounty.Bounty;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Bounty Board (悬赏令) events - a contract being posted, a kill about to
 * credit one, and a completed contract being paid out. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares.
 *
 * <p>{@link BountyPostedEvent} covers a contract an addon (or the mod itself)
 * pushed onto the board through {@code BountyManager.post}. It deliberately
 * does NOT fire for the ordinary rotation: {@code regenerateBoard} rolls a
 * whole board at once on a timer nobody asked for, and firing one event per
 * generated contract every rotation would be noise rather than news.</p>
 *
 * <h2>Threading, re-entrancy and lock order</h2>
 *
 * <p>Every one of these fires on the world thread of whatever caused it, with
 * {@code BountyManager}'s own monitor held. Do not block, and do not call back
 * into {@code BountyManager}'s mutators from a listener - {@code post},
 * {@code takeDown}, {@code retire}, {@code accept}, {@code claim},
 * {@code creditPlayerSlain} and {@code sweep} are all synchronized on that same
 * monitor, so re-entering one from inside a listener runs it in the middle of
 * the operation that fired the event and would persist a half-applied board.</p>
 *
 * <p><b>Nor may a listener take another manager's monitor.</b> The lock order
 * this mod actually establishes is <em>SeasonManager &rarr; addon &rarr;
 * BountyManager</em>: {@code SeasonManager} fires {@link SeasonEvents} with its
 * own monitor held, an addon's season listener takes its own manager's monitor,
 * and that listener posts a decree through {@code BountyManager.post}, which
 * takes this monitor last. A listener here runs with the LAST lock in that
 * order already held, so reaching for an addon monitor from inside one of these
 * events runs <em>BountyManager &rarr; addon</em> - the exact inversion - and
 * can deadlock against a season open happening on another world thread. Keep
 * listener bodies lock-free: read your own state through concurrent structures,
 * or defer the work.</p>
 */
public final class BountyEvents {

    private BountyEvents(){}

    // --- Post events ----------------------------------------------------------

    /**
     * A contract was explicitly posted to the board - it is already in the
     * board list and already persisted.
     *
     * <p>{@code bounty} is the live board object, not a copy. Read it; do not
     * mutate it. Its id is what every later {@code accept}/{@code claim}/{@code
     * takeDown} call refers to.</p>
     */
    public record BountyPostedEvent(@Nonnull Bounty bounty) {}

    /**
     * A contract was fully claimed - every promised reward component landed and
     * the acceptance has been marked claimed. Never fires for a
     * {@code BountyManager.Result#PARTIAL} claim, which leaves the acceptance
     * completed-but-unclaimed for a retry.
     *
     * @param rewardQi the Qi the contract promised, exactly as the board row
     *                 advertised it. What the cultivator's own multipliers
     *                 turned that into is {@code CultivationEvents.QiGainEvent}'s
     *                 business, not this event's.
     */
    public record BountyClaimedEvent(@Nonnull String bountyId, @Nonnull UUID claimantUuid, float rewardQi) {}

    // --- Pre event ------------------------------------------------------------

    /**
     * A completed contract is about to pay out. Cancel to refuse the claim
     * outright - nothing has been granted and nothing marked claimed yet, so
     * the acceptance simply stays completed-but-unclaimed and the player may
     * try again.
     *
     * <p>Fired inside {@code BountyManager.claim} once the claimant has been
     * confirmed to actually HOLD a live acceptance of that contract, and before
     * every other guard. A listener therefore only ever sees attempts that
     * could really have paid out - never a mistyped id or a stale UI row -
     * while still seeing the "tried to collect early" case, because a
     * not-yet-completed acceptance is one the player holds. Without that
     * ordering, every listener's first job would be re-deriving a fact the
     * board already knows, and any that skipped it would count rate limits and
     * audit rows against attempts that could never have paid anything.</p>
     *
     * <p>There is deliberately nothing re-tunable here: the reward is a
     * property of the posted contract, and re-pricing it at claim time would
     * let two claims of the same contract pay differently.</p>
     */
    public static final class PreBountyClaimEvent extends CancellableEvent {
        private final String bountyId;
        private final UUID claimantUuid;

        public PreBountyClaimEvent(@Nonnull String bountyId, @Nonnull UUID claimantUuid){
            this.bountyId = bountyId;
            this.claimantUuid = claimantUuid;
        }

        @Nonnull public String bountyId(){ return this.bountyId; }
        @Nonnull public UUID claimantUuid(){ return this.claimantUuid; }
    }

    /**
     * A player kill is about to move a {@code BountyType.SLAY} contract's
     * progress. Cancel to skip THIS acceptance entirely - no progress is
     * recorded, the contract is not completed, and nothing is said to the
     * killer.
     *
     * <h2>Why this exists: one verdict, two consumers</h2>
     *
     * <p>A SLAY decree has two halves that must never disagree. The contract is
     * the MONEY (a board reward the killer earns by holding the contract) and
     * whatever posted it usually also runs a STORY of its own (a title, a
     * transfer, a season award). Those two are decided by different rules
     * running in different places, and when they disagree the result is either a
     * payable contract standing against a state that no longer justifies it - an
     * unbounded faucet - or a reward destroyed underneath the player who just
     * earned it.</p>
     *
     * <p>This event is the single verdict both halves obey. The poster listens
     * here, applies <em>exactly</em> the rules it applies to its own reward
     * (pair cooldowns, per-season caps, sandbox and admin-bypass exclusions,
     * whatever it has), and cancels when its own answer is "no". The board then
     * pays only what the poster would itself have paid. A poster that refuses
     * its own reward but lets this event through has re-created the faucet on
     * purpose.</p>
     *
     * <p>Retiring the contract afterwards is {@code BountyManager.retire}'s job,
     * NOT this event's: {@code takeDown(id, true)} from inside a credit path
     * destroys the reward the killer just earned. See {@code
     * BountyManager.retire}'s own javadoc.</p>
     *
     * <p>Fired once per matching LIVE acceptance, after every board-side gate
     * ({@code isEnabled}, still-on-the-board, unexpired, target matches the
     * victim, the killer is not the contract's own quarry, and the killer is
     * neither on a sandbox profile nor admin-bypassing) and before any progress
     * is written. Read {@link PreBountyCreditEvent#bounty()}; do not mutate it.
     * Note the class-level lock-order warning above - this one fires from a
     * kill hook, so a listener that blocks stalls a world thread mid-death.</p>
     */
    public static final class PreBountyCreditEvent extends CancellableEvent {
        private final String bountyId;
        private final UUID killerUuid;
        private final UUID victimUuid;
        private final Bounty bounty;

        public PreBountyCreditEvent(@Nonnull String bountyId, @Nonnull UUID killerUuid,
                                    @Nonnull UUID victimUuid, @Nonnull Bounty bounty){
            this.bountyId = bountyId;
            this.killerUuid = killerUuid;
            this.victimUuid = victimUuid;
            this.bounty = bounty;
        }

        /** The contract about to be credited - the same id as {@code bounty().getId()}, hoisted for listeners that only match on it. */
        @Nonnull public String bountyId(){ return this.bountyId; }
        /** Who landed the kill, and who holds the acceptance about to progress. Never the victim, and never the contract's own quarry. */
        @Nonnull public UUID killerUuid(){ return this.killerUuid; }
        /** Who fell - already confirmed equal to {@code bounty().getTargetPlayerUuid()}. */
        @Nonnull public UUID victimUuid(){ return this.victimUuid; }
        /** The live board object, not a copy. Read it; do not mutate it. */
        @Nonnull public Bounty bounty(){ return this.bounty; }
    }

    // --- Listener lists -------------------------------------------------------

    private static final List<Consumer<BountyPostedEvent>> POSTED = EventBus.newListenerList();
    private static final List<Consumer<BountyClaimedEvent>> CLAIMED = EventBus.newListenerList();
    private static final List<Consumer<PreBountyClaimEvent>> PRE_CLAIM = EventBus.newListenerList();
    private static final List<Consumer<PreBountyCreditEvent>> PRE_CREDIT = EventBus.newListenerList();

    public static void onBountyPosted(@Nonnull Consumer<BountyPostedEvent> listener){ POSTED.add(listener); }
    public static void onBountyClaimed(@Nonnull Consumer<BountyClaimedEvent> listener){ CLAIMED.add(listener); }
    public static void onPreBountyClaim(@Nonnull Consumer<PreBountyClaimEvent> listener){ PRE_CLAIM.add(listener); }
    /** See {@link PreBountyCreditEvent} - the one verdict a SLAY decree's money and its poster's own story must share. */
    public static void onPreBountyCredit(@Nonnull Consumer<PreBountyCreditEvent> listener){ PRE_CREDIT.add(listener); }

    // --- Fired by BountyManager ----------------------------------------------

    public static void fireBountyPosted(@Nonnull BountyPostedEvent event){ EventBus.dispatch(POSTED, event, "BountyPostedEvent"); }
    public static void fireBountyClaimed(@Nonnull BountyClaimedEvent event){ EventBus.dispatch(CLAIMED, event, "BountyClaimedEvent"); }
    public static boolean firePreBountyClaim(@Nonnull PreBountyClaimEvent event){ return EventBus.fire(PRE_CLAIM, event, "PreBountyClaimEvent"); }
    public static boolean firePreBountyCredit(@Nonnull PreBountyCreditEvent event){ return EventBus.fire(PRE_CREDIT, event, "PreBountyCreditEvent"); }
}
