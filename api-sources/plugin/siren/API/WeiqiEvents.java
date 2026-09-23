package plugin.siren.API;

import plugin.siren.Utils.Weiqi.WeiqiMatch;
import plugin.siren.Utils.Weiqi.WeiqiStone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Weiqi (围棋 / Go) events - inviting, starting, every move, and how a match
 * resolves. See {@link CultivationEvents} for the conventions every
 * {@code *Events} class in this package shares, and {@link DuelEvents} for
 * the closest sibling in shape (a mutual-consent, UUID-keyed, cross-world
 * contest between two players).
 *
 * <p>Players are identified by UUID because a match routinely outlives one
 * participant's session (that is exactly what starts the reconnect-grace
 * clock). Resolve one with {@code Universe.get().getPlayer(uuid)} and check
 * {@code isValid()}.</p>
 */
public final class WeiqiEvents {
    private WeiqiEvents(){}

    // --- Post-events ---

    /** An invite was sent and is now pending the other player's answer. */
    public record WeiqiInviteEvent(@Nonnull UUID inviter, @Nonnull UUID invitee){}

    /** An invite was declined; no match started. */
    public record WeiqiDeclineEvent(@Nonnull UUID inviter, @Nonnull UUID invitee){}

    /**
     * A match is now live. {@code blackUuid} is always the ACCEPTER (moves
     * first, a courtesy to the invited player); {@code whiteUuid} is the
     * original inviter.
     */
    public record WeiqiMatchStartEvent(@Nonnull String matchId, @Nonnull UUID blackUuid, @Nonnull UUID whiteUuid,
                                       int boardSize, float komi){}

    /**
     * One ply resolved - either a stone placed at {@code index} (a flat
     * {@code y * boardSize + x} board index, decodable via
     * {@code WeiqiBoard.xOf}/{@code yOf}) or a pass, never both.
     *
     * @param index meaningless (always {@code -1}) when {@code pass} is true.
     * @param capturedCount how many enemy stones this move captured - always {@code 0} for a pass.
     */
    public record WeiqiMoveEvent(@Nonnull String matchId, @Nonnull UUID player, @Nonnull WeiqiStone color,
                                 int index, boolean pass, int capturedCount){}

    /**
     * A match ended. {@code winner} is null for {@link WeiqiMatch.Outcome#ABANDONED}/
     * {@link WeiqiMatch.Outcome#TIMED_OUT} (nobody won an undecided match);
     * for {@link WeiqiMatch.Outcome#TWO_PASS}/{@link WeiqiMatch.Outcome#RESIGNED}
     * it is always set. {@code blackScore}/{@code whiteScore} are only
     * meaningful for {@code TWO_PASS} (a resignation or an abandonment never
     * runs area scoring) - both are {@code 0} otherwise.
     */
    public record WeiqiMatchEndEvent(@Nonnull String matchId, @Nonnull UUID blackUuid, @Nonnull UUID whiteUuid,
                                     @Nullable UUID winner, @Nonnull WeiqiMatch.Outcome outcome,
                                     float blackScore, float whiteScore, int totalMoves){}

    // --- Pre-events ---

    /** An invite is about to be sent. Cancel to refuse it outright (e.g. an addon-enforced cooldown or block list). */
    public static final class PreWeiqiInviteEvent extends CancellableEvent {
        private final UUID inviter;
        private final UUID invitee;

        public PreWeiqiInviteEvent(@Nonnull UUID inviter, @Nonnull UUID invitee){
            this.inviter = inviter;
            this.invitee = invitee;
        }

        @Nonnull public UUID inviter(){ return this.inviter; }
        @Nonnull public UUID invitee(){ return this.invitee; }
    }

    /**
     * A match is about to start (the accepter just accepted a pending
     * invite). Cancel to refuse it - the invite is consumed either way, so
     * the inviter must send a fresh one. {@link #setKomi} re-tunes the komi
     * this ONE match will actually be latched with; {@code boardSize} is not
     * mutable here since it is already clamped from config before this event
     * fires.
     */
    public static final class PreWeiqiMatchStartEvent extends CancellableEvent {
        private final UUID blackUuid;
        private final UUID whiteUuid;
        private final int boardSize;
        private float komi;

        public PreWeiqiMatchStartEvent(@Nonnull UUID blackUuid, @Nonnull UUID whiteUuid, int boardSize, float komi){
            this.blackUuid = blackUuid;
            this.whiteUuid = whiteUuid;
            this.boardSize = boardSize;
            this.komi = komi;
        }

        /** The accepter - moves first. */
        @Nonnull public UUID blackUuid(){ return this.blackUuid; }
        /** The original inviter. */
        @Nonnull public UUID whiteUuid(){ return this.whiteUuid; }
        public int boardSize(){ return this.boardSize; }
        public float komi(){ return this.komi; }
        public void setKomi(float komi){ this.komi = komi; }
    }

    // --- Listener registration ---

    private static final List<Consumer<WeiqiInviteEvent>> INVITE = EventBus.newListenerList();
    private static final List<Consumer<PreWeiqiInviteEvent>> PRE_INVITE = EventBus.newListenerList();
    private static final List<Consumer<WeiqiDeclineEvent>> DECLINE = EventBus.newListenerList();
    private static final List<Consumer<WeiqiMatchStartEvent>> START = EventBus.newListenerList();
    private static final List<Consumer<PreWeiqiMatchStartEvent>> PRE_START = EventBus.newListenerList();
    private static final List<Consumer<WeiqiMoveEvent>> MOVE = EventBus.newListenerList();
    private static final List<Consumer<WeiqiMatchEndEvent>> END = EventBus.newListenerList();

    public static void onWeiqiInvite(@Nonnull Consumer<WeiqiInviteEvent> listener){ INVITE.add(listener); }
    public static void onPreWeiqiInvite(@Nonnull Consumer<PreWeiqiInviteEvent> listener){ PRE_INVITE.add(listener); }
    public static void onWeiqiDecline(@Nonnull Consumer<WeiqiDeclineEvent> listener){ DECLINE.add(listener); }
    public static void onWeiqiMatchStart(@Nonnull Consumer<WeiqiMatchStartEvent> listener){ START.add(listener); }
    public static void onPreWeiqiMatchStart(@Nonnull Consumer<PreWeiqiMatchStartEvent> listener){ PRE_START.add(listener); }
    public static void onWeiqiMove(@Nonnull Consumer<WeiqiMoveEvent> listener){ MOVE.add(listener); }
    public static void onWeiqiMatchEnd(@Nonnull Consumer<WeiqiMatchEndEvent> listener){ END.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireWeiqiInvite(@Nonnull WeiqiInviteEvent event){ EventBus.dispatch(INVITE, event, "WeiqiInviteEvent"); }
    public static boolean firePreWeiqiInvite(@Nonnull PreWeiqiInviteEvent event){ return EventBus.fire(PRE_INVITE, event, "PreWeiqiInviteEvent"); }
    public static void fireWeiqiDecline(@Nonnull WeiqiDeclineEvent event){ EventBus.dispatch(DECLINE, event, "WeiqiDeclineEvent"); }
    public static void fireWeiqiMatchStart(@Nonnull WeiqiMatchStartEvent event){ EventBus.dispatch(START, event, "WeiqiMatchStartEvent"); }
    public static boolean firePreWeiqiMatchStart(@Nonnull PreWeiqiMatchStartEvent event){ return EventBus.fire(PRE_START, event, "PreWeiqiMatchStartEvent"); }
    public static void fireWeiqiMove(@Nonnull WeiqiMoveEvent event){ EventBus.dispatch(MOVE, event, "WeiqiMoveEvent"); }
    public static void fireWeiqiMatchEnd(@Nonnull WeiqiMatchEndEvent event){ EventBus.dispatch(END, event, "WeiqiMatchEndEvent"); }
}
