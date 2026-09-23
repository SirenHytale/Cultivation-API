package plugin.siren.API;

import plugin.siren.Utils.Tea.TeaHarmonyBand;
import plugin.siren.Utils.Tea.TeaStep;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tea Ceremony (茶道) events - the two-player Fire-Watch-style reflex-timing
 * duet reached through {@code /cultivation tea invite}/{@code accept}/{@code
 * decline}. See {@link CultivationEvents} for the conventions every {@code
 * *Events} class in this package shares (pre vs post, cancellation,
 * threading, registration).
 *
 * <p>Players are identified by UUID, the same convention {@link OathEvents}
 * uses and for the same reason: resolve one with {@code
 * Universe.get().getPlayer(uuid)} and check {@code isValid()} before touching
 * their components.</p>
 *
 * <p>Dispatched by {@code plugin.siren.Utils.Tea.TeaCeremonyManager} - this
 * class only declares the surface; nothing here calls into that manager,
 * matching every other standalone {@code *Events} class in this package.</p>
 */
public final class TeaEvents {
    private TeaEvents(){}

    /** Why a ceremony ended - carried on {@link TeaCeremonyEndEvent} so a listener can tell a clean finish from a voided one without re-deriving it. */
    public enum EndReason {
        /** Every prompt was delivered and scored - the normal finish. */
        COMPLETED,
        /** A participant ran {@code /cultivation tea leave}. */
        LEFT,
        /** The two participants drifted past {@code Tea-Radius-Blocks} for longer than {@code Tea-Grace-Seconds}. */
        DRIFTED,
        /** A participant disconnected. */
        DISCONNECTED,
        /** A participant entered combat (a live {@code PunishWindowComponent}). */
        COMBAT,
        /** {@code Tea-Enabled} was switched off mid-ceremony. */
        DISABLED
    }

    // --- Post-events ---

    /** A ceremony actually began - herbs already spent, the session already live. */
    public record TeaCeremonyStartEvent(@Nonnull String sessionId, @Nonnull UUID playerA, @Nonnull UUID playerB) {}

    /** One prompt (a single BOIL/STEEP/POUR/SERVE step) was scored. */
    public record TeaStepResolvedEvent(@Nonnull String sessionId, int round, @Nonnull TeaStep step,
                                        boolean playerACorrect, boolean playerBCorrect, float scoreDelta) {}

    /**
     * A ceremony ended, one way or another - {@code harmony}/{@code band} are
     * only meaningful when {@code reason} is {@link EndReason#COMPLETED};
     * every voided reason reports {@code harmony=0f}/{@code band=DISCORDANT}
     * and {@code rewarded=false}, since no reward is ever paid on an abnormal
     * end (herbs still stay spent either way - this event does not cover
     * that, it is applied unconditionally at ceremony start).
     */
    public record TeaCeremonyEndEvent(@Nonnull String sessionId, @Nonnull UUID playerA, @Nonnull UUID playerB,
                                       @Nonnull EndReason reason, float harmony, @Nonnull TeaHarmonyBand band,
                                       boolean rewarded) {}

    // --- Pre-events (cancellable; numbers are re-tunable) ---

    /**
     * A pending invite is about to be accepted and a ceremony is about to
     * begin (herbs not yet spent). Cancel to refuse it - the invite is
     * consumed either way, mirroring {@code OathEvents.PreOathSwearEvent}, so
     * the offerer must send a fresh one.
     */
    public static final class PreTeaCeremonyStartEvent extends CancellableEvent {
        private final UUID offerer;
        private final UUID accepter;
        private int herbCost;

        public PreTeaCeremonyStartEvent(@Nonnull UUID offerer, @Nonnull UUID accepter, int herbCost){
            this.offerer = offerer;
            this.accepter = accepter;
            this.herbCost = herbCost;
        }

        @Nonnull public UUID offerer(){ return this.offerer; }
        @Nonnull public UUID accepter(){ return this.accepter; }
        /** Spirit Herbs about to be spent from the accepter's own inventory. */
        public int herbCost(){ return this.herbCost; }
        public void setHerbCost(int herbCost){ this.herbCost = herbCost; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PreTeaCeremonyStartEvent>> PRE_START = EventBus.newListenerList();
    private static final List<Consumer<TeaCeremonyStartEvent>> START = EventBus.newListenerList();
    private static final List<Consumer<TeaStepResolvedEvent>> STEP_RESOLVED = EventBus.newListenerList();
    private static final List<Consumer<TeaCeremonyEndEvent>> END = EventBus.newListenerList();

    public static void onPreTeaCeremonyStart(@Nonnull Consumer<PreTeaCeremonyStartEvent> listener){ PRE_START.add(listener); }
    public static void onTeaCeremonyStart(@Nonnull Consumer<TeaCeremonyStartEvent> listener){ START.add(listener); }
    public static void onTeaStepResolved(@Nonnull Consumer<TeaStepResolvedEvent> listener){ STEP_RESOLVED.add(listener); }
    public static void onTeaCeremonyEnd(@Nonnull Consumer<TeaCeremonyEndEvent> listener){ END.add(listener); }

    // --- Internal dispatch (called by TeaCeremonyManager; not API) ---

    public static boolean firePreTeaCeremonyStart(@Nonnull PreTeaCeremonyStartEvent event){ return EventBus.fire(PRE_START, event, "PreTeaCeremonyStartEvent"); }
    public static void fireTeaCeremonyStart(@Nonnull TeaCeremonyStartEvent event){ EventBus.dispatch(START, event, "TeaCeremonyStartEvent"); }
    public static void fireTeaStepResolved(@Nonnull TeaStepResolvedEvent event){ EventBus.dispatch(STEP_RESOLVED, event, "TeaStepResolvedEvent"); }
    public static void fireTeaCeremonyEnd(@Nonnull TeaCeremonyEndEvent event){ EventBus.dispatch(END, event, "TeaCeremonyEndEvent"); }
}
