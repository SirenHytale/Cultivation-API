package plugin.siren.API;

import plugin.siren.Utils.Sect.Sect;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sect Array Domain (宗门大阵) events - the raise and the end. Exactly the {@link
 * DaoDomainEvents} shape: post-events are records, the pre-event is a {@code static
 * final class X extends CancellableEvent} with settable tuning fields, one {@link
 * EventBus#newListenerList()} per event, {@code onX(Consumer<X>)} registration,
 * internal {@code fireX} dispatchers. See {@link CultivationEvents} for the
 * conventions every {@code *Events} class in this package shares.
 *
 * <p>A Sect Array Domain is the wartime ward a sect raises over every chunk it has
 * claimed once a siege is declared against it: its Patriarch or an Elder pays 100
 * (by default) of their <em>own</em> sect contribution, and the array then holds until
 * the siege ends. It places no block and spawns no entity - it is entity effects,
 * border particles and sounds only. Inside it the sect's members, allied sects and the
 * siege's reinforcements take less damage; the besieging sect's members are slowed and
 * deal less. The array also adds to the siege's slow budget under the shared cap, is
 * muted by a Siege Banner in its radius, and clashes with an enemy Dao Domain unfolded
 * inside it.</p>
 *
 * <p>The sect is the live {@link Sect} object, exactly as {@link SectEvents} hands it
 * out: read what you need and return. Do not call back into the array manager's
 * raise or lower from a listener. Players are identified by UUID because the raiser
 * may be offline by the time the end event fires.</p>
 *
 * <p>An array an admin raises with the test command skips the wartime and cost gates;
 * it fires {@link SectArrayRaisedEvent} (with {@code adminRaised} true) but never
 * {@link PreSectArrayRaiseEvent}, which is only for the paid, in-game raise.</p>
 */
public final class SectArrayEvents {
    private SectArrayEvents(){}

    /** Why a sect array ended. */
    public enum EndReason {
        /**
         * The siege against the sect ended - won, lost, lapsed or aborted. The usual end. An aborted siege (wars were
         * switched off - no admin command cancels a siege yet) also refunds the raiser what they paid.
         */
        SIEGE_ENDED,
        /** The sect no longer holds a hall (a conquest took it) or no longer exists (it was disbanded). */
        HALL_LOST,
        /** A Patriarch or Elder lowered it with {@code /sect array lower}. No refund. */
        LOWERED,
        /**
         * An admin lowered it with {@code /cultivation admin array lower}, or an admin-raised test array reached its
         * configured lifetime ({@code Sect-Array-Admin-Test-Minutes}).
         */
        ADMIN,
        /** Sect arrays (or sects) were switched off in the config. */
        DISABLED
    }

    // --- Pre-event (cancellable; the price re-tunable) ---

    /**
     * A Patriarch or Elder is about to raise the sect's array; every gate has passed (the
     * feature is on, a siege stands against the sect, the sect has a hall and no array,
     * the raiser holds enough contribution) but nothing has been spent. Cancel -&gt; the
     * raise fails with a generic "something prevents that" line, nothing charged, no
     * array. The cost may be re-tuned (a discount for a sect that holds a relic, say);
     * the value left in the event is clamped to {@code [0, 1000000]} and the raiser must
     * still hold it, or the raise fails as insufficient contribution.
     *
     * <p>Not fired for an admin raise.</p>
     */
    public static final class PreSectArrayRaiseEvent extends CancellableEvent {
        private final Sect sect;
        private final UUID raiser;
        private int cost;

        public PreSectArrayRaiseEvent(@Nonnull Sect sect, @Nonnull UUID raiser, int cost){
            this.sect = sect;
            this.raiser = raiser;
            this.cost = cost;
        }

        /** @return the sect whose array is about to rise. */
        @Nonnull public Sect sect(){ return this.sect; }
        /** @return the Patriarch or Elder who is paying for it. */
        @Nonnull public UUID raiser(){ return this.raiser; }
        /** @return the contribution that will be taken from the raiser's own balance. */
        public int cost(){ return this.cost; }
        public void setCost(int cost){ this.cost = cost; }
    }

    // --- Post-events ---

    /**
     * A sect's array rose.
     *
     * @param sect       the sect
     * @param raiser     whoever paid for it, or {@code null} when an admin raised it
     * @param cost       the contribution actually charged (0 for an admin raise)
     * @param adminRaised true if the admin test command raised it
     */
    public record SectArrayRaisedEvent(@Nonnull Sect sect, @javax.annotation.Nullable UUID raiser, int cost,
                                       boolean adminRaised) {}

    /**
     * A sect's array came down.
     *
     * @param sect          the sect (its name may already be gone from the roster for {@link EndReason#HALL_LOST})
     * @param reason        why it ended
     * @param secondsActive how long it stood, in seconds
     */
    public record SectArrayEndedEvent(@Nonnull Sect sect, @Nonnull EndReason reason, float secondsActive) {}

    // --- Listener registration ---

    private static final List<Consumer<PreSectArrayRaiseEvent>> PRE_RAISE = EventBus.newListenerList();
    private static final List<Consumer<SectArrayRaisedEvent>> RAISED = EventBus.newListenerList();
    private static final List<Consumer<SectArrayEndedEvent>> ENDED = EventBus.newListenerList();

    public static void onPreSectArrayRaise(@Nonnull Consumer<PreSectArrayRaiseEvent> listener){ PRE_RAISE.add(listener); }
    public static void onSectArrayRaised(@Nonnull Consumer<SectArrayRaisedEvent> listener){ RAISED.add(listener); }
    public static void onSectArrayEnded(@Nonnull Consumer<SectArrayEndedEvent> listener){ ENDED.add(listener); }

    // --- Internal dispatch (called by SectArrayManager; not API) ---

    public static boolean firePreSectArrayRaise(@Nonnull PreSectArrayRaiseEvent event){ return EventBus.fire(PRE_RAISE, event, "PreSectArrayRaiseEvent"); }
    public static void fireSectArrayRaised(@Nonnull SectArrayRaisedEvent event){ EventBus.dispatch(RAISED, event, "SectArrayRaisedEvent"); }
    public static void fireSectArrayEnded(@Nonnull SectArrayEndedEvent event){ EventBus.dispatch(ENDED, event, "SectArrayEndedEvent"); }
}
