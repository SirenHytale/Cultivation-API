package plugin.siren.API;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.ECS.Dao.DaoElement;
import plugin.siren.Utils.Sect.Sect;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dao Sermon (讲道) events - a cultivator lecturing at their own sect's hall,
 * a meditating listener's first qualifying pulse, and how a sermon ends. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares, and {@link GuardianEvents} for the closest sibling
 * shape this one mirrors.
 */
public final class SermonEvents {
    private SermonEvents(){}

    // --- Post-events ---

    /** A sect member successfully started a sermon at their own sect's hall. */
    public record SermonStartEvent(@Nonnull Sect sect, @Nonnull PlayerRef lecturer, @Nonnull DaoElement element,
                                   long endsAtMillis){}

    /**
     * One listener's FIRST qualifying pulse of this sermon - fired once per
     * (sermon, listener) pair, never again for the same pair even across a
     * diminished-rate or hard-stop transition. {@code lecturerUuid} rather
     * than a {@code PlayerRef}/{@code Sect} - the lecturer may be resolved on
     * a different call path than the listener's own tick that fires this.
     */
    public record SermonListenerQualifiedEvent(@Nonnull UUID lecturerUuid, @Nonnull PlayerRef listener,
                                                @Nonnull DaoElement element, float amountApplied){}

    /**
     * A sermon ended - naturally (duration elapsed), early ({@code
     * /cultivation sermon stop}), or abnormally (lecturer left the hall
     * chunk/world, hall lost, sect besieged, feature disabled). {@code
     * forfeited} is true only for the disconnect path ({@code
     * SermonManager#forget}) - see that method's own doc for why a
     * disconnect forfeits the Merit a graceful stop still pays out.
     */
    public record SermonEndEvent(@Nonnull UUID lecturerUuid, @Nonnull String sectName, int qualifiedListenerCount,
                                 float meritAwarded, boolean forfeited){}

    // --- Pre-events ---

    /**
     * A sect member is about to start a sermon. Cancel to refuse it (reported
     * to the actor as the start attempt simply failing) - fired AFTER every
     * gate in {@code SermonManager#start} passes but BEFORE the contribution
     * cost is spent or the cooldown is stamped, so a veto never costs the
     * actor anything.
     */
    public static final class PreSermonStartEvent extends CancellableEvent {
        private final Sect sect;
        private final PlayerRef lecturer;
        private final DaoElement element;

        public PreSermonStartEvent(@Nonnull Sect sect, @Nonnull PlayerRef lecturer, @Nonnull DaoElement element){
            this.sect = sect;
            this.lecturer = lecturer;
            this.element = element;
        }

        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public PlayerRef lecturer(){ return this.lecturer; }
        @Nonnull public DaoElement element(){ return this.element; }
    }

    // --- Listener registration ---

    private static final List<Consumer<SermonStartEvent>> STARTED = EventBus.newListenerList();
    private static final List<Consumer<SermonListenerQualifiedEvent>> LISTENER_QUALIFIED = EventBus.newListenerList();
    private static final List<Consumer<SermonEndEvent>> ENDED = EventBus.newListenerList();
    private static final List<Consumer<PreSermonStartEvent>> PRE_STARTED = EventBus.newListenerList();

    public static void onSermonStart(@Nonnull Consumer<SermonStartEvent> listener){ STARTED.add(listener); }
    public static void onSermonListenerQualified(@Nonnull Consumer<SermonListenerQualifiedEvent> listener){ LISTENER_QUALIFIED.add(listener); }
    public static void onSermonEnd(@Nonnull Consumer<SermonEndEvent> listener){ ENDED.add(listener); }
    public static void onPreSermonStart(@Nonnull Consumer<PreSermonStartEvent> listener){ PRE_STARTED.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireSermonStart(@Nonnull SermonStartEvent event){ EventBus.dispatch(STARTED, event, "SermonStartEvent"); }
    public static void fireSermonListenerQualified(@Nonnull SermonListenerQualifiedEvent event){ EventBus.dispatch(LISTENER_QUALIFIED, event, "SermonListenerQualifiedEvent"); }
    public static void fireSermonEnd(@Nonnull SermonEndEvent event){ EventBus.dispatch(ENDED, event, "SermonEndEvent"); }
    public static boolean firePreSermonStart(@Nonnull PreSermonStartEvent event){ return EventBus.fire(PRE_STARTED, event, "PreSermonStartEvent"); }
}
