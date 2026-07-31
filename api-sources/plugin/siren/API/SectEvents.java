package plugin.siren.API;

import plugin.siren.Utils.Sect.Sect;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sect (guild) events - founding, membership, ranks, halls and hall
 * inscriptions. See {@link CultivationEvents} for the conventions every
 * {@code *Events} class in this package shares (pre vs post, cancellation,
 * threading, registration).
 *
 * <p>Sect mutations run inside SectManager's synchronized methods, so a
 * listener here is holding that lock: read what you need, hand it off, and
 * return. Do not call back into SectManager's mutating methods from a listener.</p>
 *
 * <p>Players are identified by UUID rather than PlayerRef because sect
 * operations routinely touch offline members (a kicked player, a disbanded
 * sect's roster). Resolve one with
 * {@code Universe.get().getPlayer(uuid)} and check {@code isValid()}.</p>
 */
public final class SectEvents {
    private SectEvents(){}

    /** How a player came to be in a sect. */
    public enum JoinMethod {
        /** They accepted a pending invite. */
        INVITE,
        /** They walked into an OPEN sect. */
        OPEN,
        /** A manager approved their pending join request. */
        REQUEST
    }

    /** Why a player is no longer in a sect. */
    public enum LeaveReason {
        /** They left of their own accord. */
        LEFT,
        /** A leader or elder removed them. */
        KICKED
    }

    // --- Post-events ---

    /** A new sect was founded and indexed. */
    public record SectCreateEvent(@Nonnull UUID leader, @Nonnull Sect sect) {}

    /** A sect was disbanded; its members are already unindexed and its formations released. {@code sect} is the now-orphaned object, still readable for its final roster. */
    public record SectDisbandEvent(@Nonnull UUID leader, @Nonnull Sect sect) {}

    /** A manager invited a player. The invite is pending, not accepted. */
    public record SectInviteEvent(@Nonnull UUID inviter, @Nonnull UUID invitee, @Nonnull Sect sect) {}

    /** A player joined a sect and is now on its roster. */
    public record SectJoinEvent(@Nonnull UUID player, @Nonnull Sect sect, @Nonnull JoinMethod method) {}

    /** A player is off a sect's roster. {@code actor} is the kicker for KICKED, and the player themselves for LEFT. */
    public record SectLeaveEvent(@Nonnull UUID player, @Nonnull Sect sect, @Nonnull LeaveReason reason, @Nonnull UUID actor) {}

    /** A player queued a join request against a REQUEST-policy sect. */
    public record SectJoinRequestEvent(@Nonnull UUID player, @Nonnull Sect sect) {}

    /** A manager denied a pending join request. */
    public record SectJoinRequestDeniedEvent(@Nonnull UUID manager, @Nonnull UUID applicant, @Nonnull Sect sect) {}

    /** A member's elder rank changed. {@code promoted} true = plain member -> elder, false = elder -> plain member. */
    public record SectRankChangeEvent(@Nonnull UUID leader, @Nonnull UUID target, @Nonnull Sect sect, boolean promoted) {}

    /** A sect's motto was replaced. */
    public record SectMottoChangeEvent(@Nonnull UUID manager, @Nonnull Sect sect, @Nonnull String oldMotto, @Nonnull String newMotto) {}

    /** A sect changed the banner flown over its hall. */
    public record SectBannerChangeEvent(@Nonnull UUID manager, @Nonnull Sect sect, @Nonnull String oldBannerId, @Nonnull String newBannerId) {}

    /** A sect's join policy was changed. */
    public record SectJoinPolicyChangeEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull Sect.JoinPolicy oldPolicy, @Nonnull Sect.JoinPolicy newPolicy) {}

    /** A sect was renamed; formations, hall springs and pending invites have already been carried over. */
    public record SectRenameEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull String oldName, @Nonnull String newName) {}

    /** A sect's hall inscription changed. {@code newTechniqueId} is empty when the inscription was scoured away. */
    public record SectInscriptionChangeEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull String oldTechniqueId, @Nonnull String newTechniqueId) {}

    /** A sect claimed (or moved) its hall onto a spirit vein. */
    public record SectHallClaimEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull String world, int chunkX, int chunkZ, int veinTier) {}

    /** A won siege transferred a hall. The defender is now hall-less. */
    public record SectHallCaptureEvent(@Nonnull Sect attacker, @Nonnull Sect defender, @Nonnull String world, int chunkX, int chunkZ, int veinTier) {}

    // --- Pre-events ---

    /** A player is about to found a sect. Cancel to refuse (reported as a disabled/refused creation); {@link #setName} to force a different name - it is re-validated for shape and uniqueness afterward. */
    public static final class PreSectCreateEvent extends CancellableEvent {
        private final UUID leader;
        private String name;

        public PreSectCreateEvent(@Nonnull UUID leader, @Nonnull String name){
            this.leader = leader;
            this.name = name;
        }

        @Nonnull public UUID leader(){ return this.leader; }
        @Nonnull public String name(){ return this.name; }
        public void setName(@Nonnull String name){ this.name = name; }
    }

    /** A sect is about to be disbanded. Cancel to keep it standing. */
    public static final class PreSectDisbandEvent extends CancellableEvent {
        private final UUID leader;
        private final Sect sect;

        public PreSectDisbandEvent(@Nonnull UUID leader, @Nonnull Sect sect){
            this.leader = leader;
            this.sect = sect;
        }

        @Nonnull public UUID leader(){ return this.leader; }
        @Nonnull public Sect sect(){ return this.sect; }
    }

    /** An invite is about to be issued. Cancel to refuse it; {@link #setExpiryMillis} to change when it lapses. */
    public static final class PreSectInviteEvent extends CancellableEvent {
        private final UUID inviter;
        private final UUID invitee;
        private final Sect sect;
        private long expiryMillis;

        public PreSectInviteEvent(@Nonnull UUID inviter, @Nonnull UUID invitee, @Nonnull Sect sect, long expiryMillis){
            this.inviter = inviter;
            this.invitee = invitee;
            this.sect = sect;
            this.expiryMillis = expiryMillis;
        }

        @Nonnull public UUID inviter(){ return this.inviter; }
        @Nonnull public UUID invitee(){ return this.invitee; }
        @Nonnull public Sect sect(){ return this.sect; }
        /** Wall-clock millis at which this invite lapses. */
        public long expiryMillis(){ return this.expiryMillis; }
        public void setExpiryMillis(long expiryMillis){ this.expiryMillis = expiryMillis; }
    }

    /** A player is about to join a sect. Cancel to keep them out - the invite/request survives, so they can try again. */
    public static final class PreSectJoinEvent extends CancellableEvent {
        private final UUID player;
        private final Sect sect;
        private final JoinMethod method;

        public PreSectJoinEvent(@Nonnull UUID player, @Nonnull Sect sect, @Nonnull JoinMethod method){
            this.player = player;
            this.sect = sect;
            this.method = method;
        }

        @Nonnull public UUID player(){ return this.player; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public JoinMethod method(){ return this.method; }
    }

    /** A player is about to leave (or be kicked from) a sect. Cancel to keep them on the roster. */
    public static final class PreSectLeaveEvent extends CancellableEvent {
        private final UUID player;
        private final Sect sect;
        private final LeaveReason reason;
        private final UUID actor;

        public PreSectLeaveEvent(@Nonnull UUID player, @Nonnull Sect sect, @Nonnull LeaveReason reason, @Nonnull UUID actor){
            this.player = player;
            this.sect = sect;
            this.reason = reason;
            this.actor = actor;
        }

        @Nonnull public UUID player(){ return this.player; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public LeaveReason reason(){ return this.reason; }
        /** Who drove it - the kicker for KICKED, the player themselves for LEFT. */
        @Nonnull public UUID actor(){ return this.actor; }
    }

    /** A join request is about to be queued. Cancel to refuse it. */
    public static final class PreSectJoinRequestEvent extends CancellableEvent {
        private final UUID player;
        private final Sect sect;

        public PreSectJoinRequestEvent(@Nonnull UUID player, @Nonnull Sect sect){
            this.player = player;
            this.sect = sect;
        }

        @Nonnull public UUID player(){ return this.player; }
        @Nonnull public Sect sect(){ return this.sect; }
    }

    /** A member's elder rank is about to change. Cancel to leave their rank as it stands. */
    public static final class PreSectRankChangeEvent extends CancellableEvent {
        private final UUID leader;
        private final UUID target;
        private final Sect sect;
        private final boolean promoted;

        public PreSectRankChangeEvent(@Nonnull UUID leader, @Nonnull UUID target, @Nonnull Sect sect, boolean promoted){
            this.leader = leader;
            this.target = target;
            this.sect = sect;
            this.promoted = promoted;
        }

        @Nonnull public UUID leader(){ return this.leader; }
        @Nonnull public UUID target(){ return this.target; }
        @Nonnull public Sect sect(){ return this.sect; }
        /** True for member -> elder, false for elder -> member. */
        public boolean promoted(){ return this.promoted; }
    }

    /** A motto is about to be set. Cancel to refuse it; {@link #setMotto} to rewrite it (the 60-char cap still applies afterward). */
    public static final class PreSectMottoChangeEvent extends CancellableEvent {
        private final UUID manager;
        private final Sect sect;
        private final String oldMotto;
        private String motto;

        public PreSectMottoChangeEvent(@Nonnull UUID manager, @Nonnull Sect sect, @Nonnull String oldMotto, @Nonnull String motto){
            this.manager = manager;
            this.sect = sect;
            this.oldMotto = oldMotto;
            this.motto = motto;
        }

        @Nonnull public UUID manager(){ return this.manager; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public String oldMotto(){ return this.oldMotto; }
        @Nonnull public String motto(){ return this.motto; }
        public void setMotto(@Nonnull String motto){ this.motto = motto; }
    }

    /**
     * A hall banner is about to change. Cancel to refuse it; {@link #setBannerId}
     * to force a different one - useful for a server that wants a sect's banner
     * decided by something other than the sect's own taste (a war outcome, a
     * rank, an alliance).
     *
     * <p>The id is not validated after a listener rewrites it. An id nobody has
     * registered is not an error: it resolves to the vein-tier default light, the
     * same as an id whose mod has been uninstalled.</p>
     */
    public static final class PreSectBannerChangeEvent extends CancellableEvent {
        private final UUID manager;
        private final Sect sect;
        private final String oldBannerId;
        private String bannerId;

        public PreSectBannerChangeEvent(@Nonnull UUID manager, @Nonnull Sect sect, @Nonnull String oldBannerId, @Nonnull String bannerId){
            this.manager = manager;
            this.sect = sect;
            this.oldBannerId = oldBannerId;
            this.bannerId = bannerId;
        }

        @Nonnull public UUID manager(){ return this.manager; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public String oldBannerId(){ return this.oldBannerId; }
        @Nonnull public String bannerId(){ return this.bannerId; }
        public void setBannerId(@Nonnull String bannerId){ this.bannerId = bannerId; }
    }

    /** A join policy is about to change. Cancel to keep the current one; {@link #setPolicy} to force a different one. */
    public static final class PreSectJoinPolicyChangeEvent extends CancellableEvent {
        private final UUID leader;
        private final Sect sect;
        private final Sect.JoinPolicy oldPolicy;
        private Sect.JoinPolicy policy;

        public PreSectJoinPolicyChangeEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull Sect.JoinPolicy oldPolicy, @Nonnull Sect.JoinPolicy policy){
            this.leader = leader;
            this.sect = sect;
            this.oldPolicy = oldPolicy;
            this.policy = policy;
        }

        @Nonnull public UUID leader(){ return this.leader; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public Sect.JoinPolicy oldPolicy(){ return this.oldPolicy; }
        @Nonnull public Sect.JoinPolicy policy(){ return this.policy; }
        public void setPolicy(@Nonnull Sect.JoinPolicy policy){ this.policy = policy; }
    }

    /** A sect is about to be renamed. Cancel to keep the current name; {@link #setNewName} to force a different one - it is re-validated for shape and uniqueness afterward. */
    public static final class PreSectRenameEvent extends CancellableEvent {
        private final UUID leader;
        private final Sect sect;
        private final String oldName;
        private String newName;

        public PreSectRenameEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull String oldName, @Nonnull String newName){
            this.leader = leader;
            this.sect = sect;
            this.oldName = oldName;
            this.newName = newName;
        }

        @Nonnull public UUID leader(){ return this.leader; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public String oldName(){ return this.oldName; }
        @Nonnull public String newName(){ return this.newName; }
        public void setNewName(@Nonnull String newName){ this.newName = newName; }
    }

    /** A hall inscription is about to change. Cancel to leave it as it is; {@link #setNewTechniqueId} to carve something else (empty scours it away). */
    public static final class PreSectInscriptionChangeEvent extends CancellableEvent {
        private final UUID leader;
        private final Sect sect;
        private final String oldTechniqueId;
        private String newTechniqueId;

        public PreSectInscriptionChangeEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull String oldTechniqueId, @Nonnull String newTechniqueId){
            this.leader = leader;
            this.sect = sect;
            this.oldTechniqueId = oldTechniqueId;
            this.newTechniqueId = newTechniqueId;
        }

        @Nonnull public UUID leader(){ return this.leader; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public String oldTechniqueId(){ return this.oldTechniqueId; }
        @Nonnull public String newTechniqueId(){ return this.newTechniqueId; }
        public void setNewTechniqueId(@Nonnull String newTechniqueId){ this.newTechniqueId = newTechniqueId; }
    }

    /** A hall is about to be claimed. Cancel to refuse the claim (reported as a chunk already claimed). */
    public static final class PreSectHallClaimEvent extends CancellableEvent {
        private final UUID leader;
        private final Sect sect;
        private final String world;
        private final int chunkX;
        private final int chunkZ;
        private final int veinTier;

        public PreSectHallClaimEvent(@Nonnull UUID leader, @Nonnull Sect sect, @Nonnull String world, int chunkX, int chunkZ, int veinTier){
            this.leader = leader;
            this.sect = sect;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.veinTier = veinTier;
        }

        @Nonnull public UUID leader(){ return this.leader; }
        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public String world(){ return this.world; }
        public int chunkX(){ return this.chunkX; }
        public int chunkZ(){ return this.chunkZ; }
        public int veinTier(){ return this.veinTier; }
    }

    /** A hall is about to change hands to a victorious besieger. Cancel to leave it with its defender (the siege still resolves as won). */
    public static final class PreSectHallCaptureEvent extends CancellableEvent {
        private final Sect attacker;
        private final Sect defender;
        private final String world;
        private final int chunkX;
        private final int chunkZ;
        private final int veinTier;

        public PreSectHallCaptureEvent(@Nonnull Sect attacker, @Nonnull Sect defender, @Nonnull String world, int chunkX, int chunkZ, int veinTier){
            this.attacker = attacker;
            this.defender = defender;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.veinTier = veinTier;
        }

        @Nonnull public Sect attacker(){ return this.attacker; }
        @Nonnull public Sect defender(){ return this.defender; }
        @Nonnull public String world(){ return this.world; }
        public int chunkX(){ return this.chunkX; }
        public int chunkZ(){ return this.chunkZ; }
        public int veinTier(){ return this.veinTier; }
    }

    // --- Listener registration ---

    private static final List<Consumer<SectCreateEvent>> CREATE = EventBus.newListenerList();
    private static final List<Consumer<PreSectCreateEvent>> PRE_CREATE = EventBus.newListenerList();
    private static final List<Consumer<SectDisbandEvent>> DISBAND = EventBus.newListenerList();
    private static final List<Consumer<PreSectDisbandEvent>> PRE_DISBAND = EventBus.newListenerList();
    private static final List<Consumer<SectInviteEvent>> INVITE = EventBus.newListenerList();
    private static final List<Consumer<PreSectInviteEvent>> PRE_INVITE = EventBus.newListenerList();
    private static final List<Consumer<SectJoinEvent>> JOIN = EventBus.newListenerList();
    private static final List<Consumer<PreSectJoinEvent>> PRE_JOIN = EventBus.newListenerList();
    private static final List<Consumer<SectLeaveEvent>> LEAVE = EventBus.newListenerList();
    private static final List<Consumer<PreSectLeaveEvent>> PRE_LEAVE = EventBus.newListenerList();
    private static final List<Consumer<SectJoinRequestEvent>> JOIN_REQUEST = EventBus.newListenerList();
    private static final List<Consumer<PreSectJoinRequestEvent>> PRE_JOIN_REQUEST = EventBus.newListenerList();
    private static final List<Consumer<SectJoinRequestDeniedEvent>> JOIN_REQUEST_DENIED = EventBus.newListenerList();
    private static final List<Consumer<SectRankChangeEvent>> RANK_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreSectRankChangeEvent>> PRE_RANK_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<SectMottoChangeEvent>> MOTTO_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreSectMottoChangeEvent>> PRE_MOTTO_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<SectBannerChangeEvent>> BANNER_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreSectBannerChangeEvent>> PRE_BANNER_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<SectJoinPolicyChangeEvent>> JOIN_POLICY_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreSectJoinPolicyChangeEvent>> PRE_JOIN_POLICY_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<SectRenameEvent>> RENAME = EventBus.newListenerList();
    private static final List<Consumer<PreSectRenameEvent>> PRE_RENAME = EventBus.newListenerList();
    private static final List<Consumer<SectInscriptionChangeEvent>> INSCRIPTION_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreSectInscriptionChangeEvent>> PRE_INSCRIPTION_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<SectHallClaimEvent>> HALL_CLAIM = EventBus.newListenerList();
    private static final List<Consumer<PreSectHallClaimEvent>> PRE_HALL_CLAIM = EventBus.newListenerList();
    private static final List<Consumer<SectHallCaptureEvent>> HALL_CAPTURE = EventBus.newListenerList();
    private static final List<Consumer<PreSectHallCaptureEvent>> PRE_HALL_CAPTURE = EventBus.newListenerList();

    public static void onSectCreate(@Nonnull Consumer<SectCreateEvent> listener){ CREATE.add(listener); }
    public static void onPreSectCreate(@Nonnull Consumer<PreSectCreateEvent> listener){ PRE_CREATE.add(listener); }
    public static void onSectDisband(@Nonnull Consumer<SectDisbandEvent> listener){ DISBAND.add(listener); }
    public static void onPreSectDisband(@Nonnull Consumer<PreSectDisbandEvent> listener){ PRE_DISBAND.add(listener); }
    public static void onSectInvite(@Nonnull Consumer<SectInviteEvent> listener){ INVITE.add(listener); }
    public static void onPreSectInvite(@Nonnull Consumer<PreSectInviteEvent> listener){ PRE_INVITE.add(listener); }
    public static void onSectJoin(@Nonnull Consumer<SectJoinEvent> listener){ JOIN.add(listener); }
    public static void onPreSectJoin(@Nonnull Consumer<PreSectJoinEvent> listener){ PRE_JOIN.add(listener); }
    public static void onSectLeave(@Nonnull Consumer<SectLeaveEvent> listener){ LEAVE.add(listener); }
    public static void onPreSectLeave(@Nonnull Consumer<PreSectLeaveEvent> listener){ PRE_LEAVE.add(listener); }
    public static void onSectJoinRequest(@Nonnull Consumer<SectJoinRequestEvent> listener){ JOIN_REQUEST.add(listener); }
    public static void onPreSectJoinRequest(@Nonnull Consumer<PreSectJoinRequestEvent> listener){ PRE_JOIN_REQUEST.add(listener); }
    public static void onSectJoinRequestDenied(@Nonnull Consumer<SectJoinRequestDeniedEvent> listener){ JOIN_REQUEST_DENIED.add(listener); }
    public static void onSectRankChange(@Nonnull Consumer<SectRankChangeEvent> listener){ RANK_CHANGE.add(listener); }
    public static void onPreSectRankChange(@Nonnull Consumer<PreSectRankChangeEvent> listener){ PRE_RANK_CHANGE.add(listener); }
    public static void onSectMottoChange(@Nonnull Consumer<SectMottoChangeEvent> listener){ MOTTO_CHANGE.add(listener); }
    public static void onPreSectMottoChange(@Nonnull Consumer<PreSectMottoChangeEvent> listener){ PRE_MOTTO_CHANGE.add(listener); }
    public static void onSectBannerChange(@Nonnull Consumer<SectBannerChangeEvent> listener){ BANNER_CHANGE.add(listener); }
    public static void onPreSectBannerChange(@Nonnull Consumer<PreSectBannerChangeEvent> listener){ PRE_BANNER_CHANGE.add(listener); }
    public static void onSectJoinPolicyChange(@Nonnull Consumer<SectJoinPolicyChangeEvent> listener){ JOIN_POLICY_CHANGE.add(listener); }
    public static void onPreSectJoinPolicyChange(@Nonnull Consumer<PreSectJoinPolicyChangeEvent> listener){ PRE_JOIN_POLICY_CHANGE.add(listener); }
    public static void onSectRename(@Nonnull Consumer<SectRenameEvent> listener){ RENAME.add(listener); }
    public static void onPreSectRename(@Nonnull Consumer<PreSectRenameEvent> listener){ PRE_RENAME.add(listener); }
    public static void onSectInscriptionChange(@Nonnull Consumer<SectInscriptionChangeEvent> listener){ INSCRIPTION_CHANGE.add(listener); }
    public static void onPreSectInscriptionChange(@Nonnull Consumer<PreSectInscriptionChangeEvent> listener){ PRE_INSCRIPTION_CHANGE.add(listener); }
    public static void onSectHallClaim(@Nonnull Consumer<SectHallClaimEvent> listener){ HALL_CLAIM.add(listener); }
    public static void onPreSectHallClaim(@Nonnull Consumer<PreSectHallClaimEvent> listener){ PRE_HALL_CLAIM.add(listener); }
    public static void onSectHallCapture(@Nonnull Consumer<SectHallCaptureEvent> listener){ HALL_CAPTURE.add(listener); }
    public static void onPreSectHallCapture(@Nonnull Consumer<PreSectHallCaptureEvent> listener){ PRE_HALL_CAPTURE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireSectCreate(@Nonnull SectCreateEvent event){ EventBus.dispatch(CREATE, event, "SectCreateEvent"); }
    public static boolean firePreSectCreate(@Nonnull PreSectCreateEvent event){ return EventBus.fire(PRE_CREATE, event, "PreSectCreateEvent"); }
    public static void fireSectDisband(@Nonnull SectDisbandEvent event){ EventBus.dispatch(DISBAND, event, "SectDisbandEvent"); }
    public static boolean firePreSectDisband(@Nonnull PreSectDisbandEvent event){ return EventBus.fire(PRE_DISBAND, event, "PreSectDisbandEvent"); }
    public static void fireSectInvite(@Nonnull SectInviteEvent event){ EventBus.dispatch(INVITE, event, "SectInviteEvent"); }
    public static boolean firePreSectInvite(@Nonnull PreSectInviteEvent event){ return EventBus.fire(PRE_INVITE, event, "PreSectInviteEvent"); }
    public static void fireSectJoin(@Nonnull SectJoinEvent event){ EventBus.dispatch(JOIN, event, "SectJoinEvent"); }
    public static boolean firePreSectJoin(@Nonnull PreSectJoinEvent event){ return EventBus.fire(PRE_JOIN, event, "PreSectJoinEvent"); }
    public static void fireSectLeave(@Nonnull SectLeaveEvent event){ EventBus.dispatch(LEAVE, event, "SectLeaveEvent"); }
    public static boolean firePreSectLeave(@Nonnull PreSectLeaveEvent event){ return EventBus.fire(PRE_LEAVE, event, "PreSectLeaveEvent"); }
    public static void fireSectJoinRequest(@Nonnull SectJoinRequestEvent event){ EventBus.dispatch(JOIN_REQUEST, event, "SectJoinRequestEvent"); }
    public static boolean firePreSectJoinRequest(@Nonnull PreSectJoinRequestEvent event){ return EventBus.fire(PRE_JOIN_REQUEST, event, "PreSectJoinRequestEvent"); }
    public static void fireSectJoinRequestDenied(@Nonnull SectJoinRequestDeniedEvent event){ EventBus.dispatch(JOIN_REQUEST_DENIED, event, "SectJoinRequestDeniedEvent"); }
    public static void fireSectRankChange(@Nonnull SectRankChangeEvent event){ EventBus.dispatch(RANK_CHANGE, event, "SectRankChangeEvent"); }
    public static boolean firePreSectRankChange(@Nonnull PreSectRankChangeEvent event){ return EventBus.fire(PRE_RANK_CHANGE, event, "PreSectRankChangeEvent"); }
    public static void fireSectMottoChange(@Nonnull SectMottoChangeEvent event){ EventBus.dispatch(MOTTO_CHANGE, event, "SectMottoChangeEvent"); }
    public static boolean firePreSectMottoChange(@Nonnull PreSectMottoChangeEvent event){ return EventBus.fire(PRE_MOTTO_CHANGE, event, "PreSectMottoChangeEvent"); }
    public static void fireSectBannerChange(@Nonnull SectBannerChangeEvent event){ EventBus.dispatch(BANNER_CHANGE, event, "SectBannerChangeEvent"); }
    public static boolean firePreSectBannerChange(@Nonnull PreSectBannerChangeEvent event){ return EventBus.fire(PRE_BANNER_CHANGE, event, "PreSectBannerChangeEvent"); }
    public static void fireSectJoinPolicyChange(@Nonnull SectJoinPolicyChangeEvent event){ EventBus.dispatch(JOIN_POLICY_CHANGE, event, "SectJoinPolicyChangeEvent"); }
    public static boolean firePreSectJoinPolicyChange(@Nonnull PreSectJoinPolicyChangeEvent event){ return EventBus.fire(PRE_JOIN_POLICY_CHANGE, event, "PreSectJoinPolicyChangeEvent"); }
    public static void fireSectRename(@Nonnull SectRenameEvent event){ EventBus.dispatch(RENAME, event, "SectRenameEvent"); }
    public static boolean firePreSectRename(@Nonnull PreSectRenameEvent event){ return EventBus.fire(PRE_RENAME, event, "PreSectRenameEvent"); }
    public static void fireSectInscriptionChange(@Nonnull SectInscriptionChangeEvent event){ EventBus.dispatch(INSCRIPTION_CHANGE, event, "SectInscriptionChangeEvent"); }
    public static boolean firePreSectInscriptionChange(@Nonnull PreSectInscriptionChangeEvent event){ return EventBus.fire(PRE_INSCRIPTION_CHANGE, event, "PreSectInscriptionChangeEvent"); }
    public static void fireSectHallClaim(@Nonnull SectHallClaimEvent event){ EventBus.dispatch(HALL_CLAIM, event, "SectHallClaimEvent"); }
    public static boolean firePreSectHallClaim(@Nonnull PreSectHallClaimEvent event){ return EventBus.fire(PRE_HALL_CLAIM, event, "PreSectHallClaimEvent"); }
    public static void fireSectHallCapture(@Nonnull SectHallCaptureEvent event){ EventBus.dispatch(HALL_CAPTURE, event, "SectHallCaptureEvent"); }
    public static boolean firePreSectHallCapture(@Nonnull PreSectHallCaptureEvent event){ return EventBus.fire(PRE_HALL_CAPTURE, event, "PreSectHallCaptureEvent"); }
}
