package plugin.siren.API;

import plugin.siren.Utils.Sect.Sect;
import plugin.siren.Utils.War.Siege;
import plugin.siren.Utils.War.SiegeBanner;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sect war events - declaring a siege on a rival's hall and how it resolves.
 * See {@link CultivationEvents} for the conventions every {@code *Events} class
 * in this package shares.
 *
 * <p>The hall actually changing hands is {@link SectEvents}'
 * {@code SectHallCaptureEvent} - {@code WarManager} never fires it (the
 * Sect Siege hardening pass's SUPPRESS mode never moves a hall at all), but
 * a future capture mode could without touching either event here: veto THAT
 * one to let a siege be won without the hall moving.</p>
 */
public final class WarEvents {
    private WarEvents(){}

    /** Why a siege ended without the attacker triggering SUPPRESS. */
    public enum SiegeFailReason {
        /** The CONTEST window ran out before the attacker held the hall long enough. */
        LAPSED,
        /** The attacking or defending sect (or the defender's hall) no longer existed when checked. */
        DEFENDER_GONE,
        /** MUSTER's deadline passed without both sides meeting their minimum presence - a total no-op, no cooldown for the defender. */
        MUSTER_FAILED,
        /** CONTEST hard-stopped because live defender presence in the hall's own world stayed at zero past the grace period - resolves in the defender's favor with no loss. */
        DEFENDER_ABSENT,
        /** An admin or the Wars-Enabled master switch cancelled the siege outright - no consequence to either side, and the attacker's declaration cost is refunded. */
        ABORTED
    }

    // --- Post-events ---

    /** A sect declared war on another's hall; the siege is live and both sides have been told. */
    public record WarDeclareEvent(@Nonnull Sect attacker, @Nonnull Sect defender, @Nonnull Siege siege) {}

    /** An attacker held a contested hall long enough to take it. The hall transfer has already been attempted (see SectEvents.SectHallCaptureEvent) and the defender's immunity cooldown started. */
    public record SiegeCaptureEvent(@Nonnull Sect attacker, @Nonnull Sect defender, @Nonnull Siege siege) {}

    /** A siege ended with the hall still in its defender's hands. Sect objects are null when the sect no longer resolves by name. */
    public record SiegeFailEvent(@Nonnull Siege siege, @Nonnull SiegeFailReason reason) {}

    /** Which side of a siege a Siege Banner's breaker belonged to - see {@link SiegeBannerBreakEvent}. */
    public enum BannerBreakSide {
        ATTACKER, DEFENDER, THIRD_PARTY
    }

    /** Formations 2.0: a Siege Banner was placed and is now standing. */
    public record SiegeBannerPlaceEvent(@Nonnull SiegeBanner banner) {}

    /** Formations 2.0: a Siege Banner was broken - by anyone, no protection. {@code breakerSide} tells which side (if any) the breaker belonged to. */
    public record SiegeBannerBreakEvent(@Nonnull SiegeBanner banner, @Nonnull UUID breaker, @Nonnull BannerBreakSide breakerSide) {}

    // --- Pre-events ---

    /** A siege is about to be declared. Cancel to refuse it (reported to the caller as wars being disabled); {@link #setWindowMillis} to give this siege a longer or shorter window than the config's. */
    public static final class PreWarDeclareEvent extends CancellableEvent {
        private final Sect attacker;
        private final Sect defender;
        private long windowMillis;

        public PreWarDeclareEvent(@Nonnull Sect attacker, @Nonnull Sect defender, long windowMillis){
            this.attacker = attacker;
            this.defender = defender;
            this.windowMillis = windowMillis;
        }

        @Nonnull public Sect attacker(){ return this.attacker; }
        @Nonnull public Sect defender(){ return this.defender; }
        /** How long the attacker has to complete their hold, in millis. */
        public long windowMillis(){ return this.windowMillis; }
        public void setWindowMillis(long windowMillis){ this.windowMillis = windowMillis; }
    }

    /** A siege is about to be won. Cancel to leave it running - the attacker keeps holding and will trip this again on their next presence tick, so cancel only while some condition of yours is unmet. */
    public static final class PreSiegeCaptureEvent extends CancellableEvent {
        private final Sect attacker;
        private final Sect defender;
        private final Siege siege;

        public PreSiegeCaptureEvent(@Nonnull Sect attacker, @Nonnull Sect defender, @Nonnull Siege siege){
            this.attacker = attacker;
            this.defender = defender;
            this.siege = siege;
        }

        @Nonnull public Sect attacker(){ return this.attacker; }
        @Nonnull public Sect defender(){ return this.defender; }
        @Nonnull public Siege siege(){ return this.siege; }
    }

    /**
     * An allied sect's reinforcement of a successfully-defended (LAPSED)
     * siege is about to be rewarded - see {@code WarManager#failSiege}'s
     * LAPSED branch, the only place this fires. Cancel to withhold THIS
     * specific ally's reward without affecting any other qualifying ally on
     * the same siege (each reinforcing sect gets its own event); {@link
     * #setContributionPerMember} to rescale the payout for this reward only,
     * mirroring {@link PreWarDeclareEvent#setWindowMillis}'s re-tuning
     * pattern.
     */
    public static final class PreWarReinforcementRewardEvent extends CancellableEvent {
        private final Sect reinforcingSect;
        private final Sect defender;
        private final Siege siege;
        private int contributionPerMember;

        public PreWarReinforcementRewardEvent(@Nonnull Sect reinforcingSect, @Nonnull Sect defender,
                                              @Nonnull Siege siege, int contributionPerMember){
            this.reinforcingSect = reinforcingSect;
            this.defender = defender;
            this.siege = siege;
            this.contributionPerMember = contributionPerMember;
        }

        @Nonnull public Sect reinforcingSect(){ return this.reinforcingSect; }
        @Nonnull public Sect defender(){ return this.defender; }
        @Nonnull public Siege siege(){ return this.siege; }
        /** Contribution paid to every current member of {@link #reinforcingSect}, unless cancelled. */
        public int contributionPerMember(){ return this.contributionPerMember; }
        public void setContributionPerMember(int contributionPerMember){ this.contributionPerMember = contributionPerMember; }
    }

    /**
     * Formations 2.0: a Siege Banner is about to be placed. Cancel to refuse
     * it (reported to the placer as something preventing the placement).
     * Fired from {@code SiegeBannerManager.tryPlace} BEFORE any monitor is
     * taken - no lock is held during dispatch (see that class's "Locking"
     * doc). A listener may therefore safely read {@code WarManager} state;
     * the registration cap and siege liveness are re-checked authoritatively
     * after this event, inside the synchronized registration step.
     */
    public static final class PreSiegeBannerPlaceEvent extends CancellableEvent {
        private final String attackerSect;
        private final String defenderSect;
        private final String world;
        private final int x;
        private final int y;
        private final int z;

        public PreSiegeBannerPlaceEvent(@Nonnull String attackerSect, @Nonnull String defenderSect,
                                        @Nonnull String world, int x, int y, int z){
            this.attackerSect = attackerSect;
            this.defenderSect = defenderSect;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Nonnull public String attackerSect(){ return this.attackerSect; }
        @Nonnull public String defenderSect(){ return this.defenderSect; }
        @Nonnull public String world(){ return this.world; }
        public int x(){ return this.x; }
        public int y(){ return this.y; }
        public int z(){ return this.z; }
    }

    // --- Listener registration ---

    private static final List<Consumer<WarDeclareEvent>> DECLARE = EventBus.newListenerList();
    private static final List<Consumer<PreWarDeclareEvent>> PRE_DECLARE = EventBus.newListenerList();
    private static final List<Consumer<SiegeCaptureEvent>> CAPTURE = EventBus.newListenerList();
    private static final List<Consumer<PreSiegeCaptureEvent>> PRE_CAPTURE = EventBus.newListenerList();
    private static final List<Consumer<SiegeFailEvent>> FAIL = EventBus.newListenerList();
    private static final List<Consumer<PreWarReinforcementRewardEvent>> PRE_REINFORCEMENT_REWARD = EventBus.newListenerList();
    private static final List<Consumer<SiegeBannerPlaceEvent>> BANNER_PLACE = EventBus.newListenerList();
    private static final List<Consumer<PreSiegeBannerPlaceEvent>> PRE_BANNER_PLACE = EventBus.newListenerList();
    private static final List<Consumer<SiegeBannerBreakEvent>> BANNER_BREAK = EventBus.newListenerList();

    public static void onWarDeclare(@Nonnull Consumer<WarDeclareEvent> listener){ DECLARE.add(listener); }
    public static void onPreWarDeclare(@Nonnull Consumer<PreWarDeclareEvent> listener){ PRE_DECLARE.add(listener); }
    public static void onSiegeCapture(@Nonnull Consumer<SiegeCaptureEvent> listener){ CAPTURE.add(listener); }
    public static void onPreSiegeCapture(@Nonnull Consumer<PreSiegeCaptureEvent> listener){ PRE_CAPTURE.add(listener); }
    public static void onSiegeFail(@Nonnull Consumer<SiegeFailEvent> listener){ FAIL.add(listener); }
    public static void onPreWarReinforcementReward(@Nonnull Consumer<PreWarReinforcementRewardEvent> listener){ PRE_REINFORCEMENT_REWARD.add(listener); }
    public static void onSiegeBannerPlace(@Nonnull Consumer<SiegeBannerPlaceEvent> listener){ BANNER_PLACE.add(listener); }
    public static void onPreSiegeBannerPlace(@Nonnull Consumer<PreSiegeBannerPlaceEvent> listener){ PRE_BANNER_PLACE.add(listener); }
    public static void onSiegeBannerBreak(@Nonnull Consumer<SiegeBannerBreakEvent> listener){ BANNER_BREAK.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireWarDeclare(@Nonnull WarDeclareEvent event){ EventBus.dispatch(DECLARE, event, "WarDeclareEvent"); }
    public static boolean firePreWarDeclare(@Nonnull PreWarDeclareEvent event){ return EventBus.fire(PRE_DECLARE, event, "PreWarDeclareEvent"); }
    public static void fireSiegeCapture(@Nonnull SiegeCaptureEvent event){ EventBus.dispatch(CAPTURE, event, "SiegeCaptureEvent"); }
    public static boolean firePreSiegeCapture(@Nonnull PreSiegeCaptureEvent event){ return EventBus.fire(PRE_CAPTURE, event, "PreSiegeCaptureEvent"); }
    public static void fireSiegeFail(@Nonnull SiegeFailEvent event){ EventBus.dispatch(FAIL, event, "SiegeFailEvent"); }
    public static boolean firePreWarReinforcementReward(@Nonnull PreWarReinforcementRewardEvent event){ return EventBus.fire(PRE_REINFORCEMENT_REWARD, event, "PreWarReinforcementRewardEvent"); }
    public static void fireSiegeBannerPlace(@Nonnull SiegeBannerPlaceEvent event){ EventBus.dispatch(BANNER_PLACE, event, "SiegeBannerPlaceEvent"); }
    public static boolean firePreSiegeBannerPlace(@Nonnull PreSiegeBannerPlaceEvent event){ return EventBus.fire(PRE_BANNER_PLACE, event, "PreSiegeBannerPlaceEvent"); }
    public static void fireSiegeBannerBreak(@Nonnull SiegeBannerBreakEvent event){ EventBus.dispatch(BANNER_BREAK, event, "SiegeBannerBreakEvent"); }
}
