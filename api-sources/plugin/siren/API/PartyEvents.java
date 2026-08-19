package plugin.siren.API;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Party events - the ad-hoc, session-only grouping that is the foundation for
 * a later multiplayer dungeon feature (that feature is not built yet; this is
 * purely party formation). See {@link CultivationEvents} for the conventions
 * every {@code *Events} class in this package shares.
 *
 * <p>Players are identified by UUID because a party routinely outlives one
 * member's session (a disconnected member is simply offline, not removed) -
 * resolve one with {@code Universe.get().getPlayer(uuid)} and check
 * {@code isValid()}.</p>
 *
 * <p>Post-notification only for v1, aside from {@link PrePartyInviteEvent} -
 * there is no other clear veto point a party-formation-only pass needs; a
 * dungeon feature built on top of this is free to add its own pre-events for
 * whatever it gates.</p>
 */
public final class PartyEvents {
    private PartyEvents(){}

    // --- Post-events ---

    /** A solo leader's first invite was accepted - the party now exists as a group, not just a leader waiting alone. */
    public record PartyFormedEvent(@Nonnull UUID leaderUuid, @Nonnull UUID firstMemberUuid) {}

    /** A cultivator joined an already-formed party (i.e. not the party's very first member - see {@link PartyFormedEvent}). */
    public record PartyMemberJoinedEvent(@Nonnull UUID leaderUuid, @Nonnull UUID memberUuid) {}

    /** A cultivator left a party that still has members remaining afterward. If the leader left, {@code leaderUuid} is the newly promoted leader. */
    public record PartyMemberLeftEvent(@Nonnull UUID leaderUuid, @Nonnull UUID memberUuid) {}

    /** A party stopped existing - either the leader disbanded it outright, or its last member left. {@code formerMembers} is a snapshot, not a live view. */
    public record PartyDisbandedEvent(@Nonnull UUID leaderUuid, @Nonnull Set<UUID> formerMembers) {}

    // --- Pre-events ---

    /** An invite is about to be sent. Cancel to refuse it silently (the inviter still receives the manager's own result). */
    public static final class PrePartyInviteEvent extends CancellableEvent {
        private final UUID inviterUuid;
        private final UUID targetUuid;

        public PrePartyInviteEvent(@Nonnull UUID inviterUuid, @Nonnull UUID targetUuid){
            this.inviterUuid = inviterUuid;
            this.targetUuid = targetUuid;
        }

        @Nonnull public UUID inviterUuid(){ return this.inviterUuid; }
        @Nonnull public UUID targetUuid(){ return this.targetUuid; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PartyFormedEvent>> FORMED = EventBus.newListenerList();
    private static final List<Consumer<PartyMemberJoinedEvent>> MEMBER_JOINED = EventBus.newListenerList();
    private static final List<Consumer<PartyMemberLeftEvent>> MEMBER_LEFT = EventBus.newListenerList();
    private static final List<Consumer<PartyDisbandedEvent>> DISBANDED = EventBus.newListenerList();
    private static final List<Consumer<PrePartyInviteEvent>> PRE_INVITE = EventBus.newListenerList();

    public static void onPartyFormed(@Nonnull Consumer<PartyFormedEvent> listener){ FORMED.add(listener); }
    public static void onPartyMemberJoined(@Nonnull Consumer<PartyMemberJoinedEvent> listener){ MEMBER_JOINED.add(listener); }
    public static void onPartyMemberLeft(@Nonnull Consumer<PartyMemberLeftEvent> listener){ MEMBER_LEFT.add(listener); }
    public static void onPartyDisbanded(@Nonnull Consumer<PartyDisbandedEvent> listener){ DISBANDED.add(listener); }
    public static void onPrePartyInvite(@Nonnull Consumer<PrePartyInviteEvent> listener){ PRE_INVITE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void firePartyFormed(@Nonnull PartyFormedEvent event){ EventBus.dispatch(FORMED, event, "PartyFormedEvent"); }
    public static void firePartyMemberJoined(@Nonnull PartyMemberJoinedEvent event){ EventBus.dispatch(MEMBER_JOINED, event, "PartyMemberJoinedEvent"); }
    public static void firePartyMemberLeft(@Nonnull PartyMemberLeftEvent event){ EventBus.dispatch(MEMBER_LEFT, event, "PartyMemberLeftEvent"); }
    public static void firePartyDisbanded(@Nonnull PartyDisbandedEvent event){ EventBus.dispatch(DISBANDED, event, "PartyDisbandedEvent"); }
    public static boolean firePrePartyInvite(@Nonnull PrePartyInviteEvent event){ return EventBus.fire(PRE_INVITE, event, "PrePartyInviteEvent"); }
}
