package plugin.siren.API;

import plugin.siren.Utils.Sect.Sect;
import plugin.siren.Utils.War.Siege;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sect war events - declaring a siege on a rival's hall and how it resolves.
 * See {@link CultivationEvents} for the conventions every {@code *Events} class
 * in this package shares.
 *
 * <p>The hall actually changing hands is {@link SectEvents}'
 * {@code SectHallCaptureEvent}, fired from inside the capture below - veto
 * THAT one to let a siege be won without the hall moving.</p>
 */
public final class WarEvents {
    private WarEvents(){}

    /** Why a siege ended without the attacker taking the hall. */
    public enum SiegeFailReason {
        /** The war window ran out before the attacker held the hall long enough. */
        LAPSED,
        /** The defending sect (or its hall) no longer existed when the hold completed. */
        DEFENDER_GONE
    }

    // --- Post-events ---

    /** A sect declared war on another's hall; the siege is live and both sides have been told. */
    public record WarDeclareEvent(@Nonnull Sect attacker, @Nonnull Sect defender, @Nonnull Siege siege) {}

    /** An attacker held a contested hall long enough to take it. The hall transfer has already been attempted (see SectEvents.SectHallCaptureEvent) and the defender's immunity cooldown started. */
    public record SiegeCaptureEvent(@Nonnull Sect attacker, @Nonnull Sect defender, @Nonnull Siege siege) {}

    /** A siege ended with the hall still in its defender's hands. Sect objects are null when the sect no longer resolves by name. */
    public record SiegeFailEvent(@Nonnull Siege siege, @Nonnull SiegeFailReason reason) {}

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

    // --- Listener registration ---

    private static final List<Consumer<WarDeclareEvent>> DECLARE = EventBus.newListenerList();
    private static final List<Consumer<PreWarDeclareEvent>> PRE_DECLARE = EventBus.newListenerList();
    private static final List<Consumer<SiegeCaptureEvent>> CAPTURE = EventBus.newListenerList();
    private static final List<Consumer<PreSiegeCaptureEvent>> PRE_CAPTURE = EventBus.newListenerList();
    private static final List<Consumer<SiegeFailEvent>> FAIL = EventBus.newListenerList();

    public static void onWarDeclare(@Nonnull Consumer<WarDeclareEvent> listener){ DECLARE.add(listener); }
    public static void onPreWarDeclare(@Nonnull Consumer<PreWarDeclareEvent> listener){ PRE_DECLARE.add(listener); }
    public static void onSiegeCapture(@Nonnull Consumer<SiegeCaptureEvent> listener){ CAPTURE.add(listener); }
    public static void onPreSiegeCapture(@Nonnull Consumer<PreSiegeCaptureEvent> listener){ PRE_CAPTURE.add(listener); }
    public static void onSiegeFail(@Nonnull Consumer<SiegeFailEvent> listener){ FAIL.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireWarDeclare(@Nonnull WarDeclareEvent event){ EventBus.dispatch(DECLARE, event, "WarDeclareEvent"); }
    public static boolean firePreWarDeclare(@Nonnull PreWarDeclareEvent event){ return EventBus.fire(PRE_DECLARE, event, "PreWarDeclareEvent"); }
    public static void fireSiegeCapture(@Nonnull SiegeCaptureEvent event){ EventBus.dispatch(CAPTURE, event, "SiegeCaptureEvent"); }
    public static boolean firePreSiegeCapture(@Nonnull PreSiegeCaptureEvent event){ return EventBus.fire(PRE_CAPTURE, event, "PreSiegeCaptureEvent"); }
    public static void fireSiegeFail(@Nonnull SiegeFailEvent event){ EventBus.dispatch(FAIL, event, "SiegeFailEvent"); }
}
