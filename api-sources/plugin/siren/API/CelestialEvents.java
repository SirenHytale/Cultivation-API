package plugin.siren.API;

import plugin.siren.Utils.Celestial.CelestialEventType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Celestial Event (天象) events - a server-wide phenomenon (Spirit Tide,
 * Meteor Shower, Blood Moon, or an addon-registered one) starting or ending.
 * See {@link CultivationEvents} for the conventions every {@code *Events}
 * class in this package shares (pre vs post, cancellation, threading,
 * registration).
 *
 * <p>An addon that registers its OWN {@link CelestialEventType} has no
 * bespoke pre/post pair of its own to fire - these two cover every event,
 * built-in or not, identified by {@link CelestialEventType#id()}. Listen here
 * and switch on the id to react to a specific one, the same way a listener on
 * {@code CultivationEvents.onAdvancement} switches on the new stage.</p>
 */
public final class CelestialEvents {
    private CelestialEvents(){}

    // --- Post-events ---

    /** A celestial event just started; its sky is already live. */
    public record CelestialEventStartEvent(@Nonnull CelestialEventType type, long startedAtMillis, long endsAtMillis,
                                           boolean forcedByAdmin) {}

    /** A celestial event just ended; its sky is already clearing. */
    public record CelestialEventEndEvent(@Nonnull CelestialEventType type, long startedAtMillis, long endedAtMillis,
                                         boolean forcedByAdmin) {}

    // --- Pre-events ---

    /**
     * A celestial event is about to start. Cancel to skip this pick entirely
     * (the scheduler simply waits for its next check rather than substituting
     * another event, so a listener that vetoes every pick would leave none
     * running - the same "silence is a valid answer" shape
     * {@code PreSectJoinEvent} has); {@link #setDurationMinutes} to run it
     * longer or shorter than {@link CelestialEventType#durationMinutes()}.
     */
    public static final class PreCelestialEventStartEvent extends CancellableEvent {
        private final CelestialEventType type;
        private float durationMinutes;
        private final boolean forcedByAdmin;

        public PreCelestialEventStartEvent(@Nonnull CelestialEventType type, float durationMinutes, boolean forcedByAdmin){
            this.type = type;
            this.durationMinutes = durationMinutes;
            this.forcedByAdmin = forcedByAdmin;
        }

        @Nonnull public CelestialEventType type(){ return this.type; }
        public float durationMinutes(){ return this.durationMinutes; }
        public void setDurationMinutes(float durationMinutes){ this.durationMinutes = durationMinutes; }
        /** True for {@code /celestial start}, false for the scheduler's own pick. */
        public boolean forcedByAdmin(){ return this.forcedByAdmin; }
    }

    // --- Registration ---

    private static final List<Consumer<CelestialEventStartEvent>> START = EventBus.newListenerList();
    private static final List<Consumer<PreCelestialEventStartEvent>> PRE_START = EventBus.newListenerList();
    private static final List<Consumer<CelestialEventEndEvent>> END = EventBus.newListenerList();

    public static void onCelestialEventStart(@Nonnull Consumer<CelestialEventStartEvent> listener){ START.add(listener); }
    public static void onPreCelestialEventStart(@Nonnull Consumer<PreCelestialEventStartEvent> listener){ PRE_START.add(listener); }
    public static void onCelestialEventEnd(@Nonnull Consumer<CelestialEventEndEvent> listener){ END.add(listener); }

    // --- Internal dispatch (called by CelestialManager; not API) ---

    public static void fireCelestialEventStart(@Nonnull CelestialEventStartEvent event){ EventBus.dispatch(START, event, "CelestialEventStartEvent"); }
    public static boolean firePreCelestialEventStart(@Nonnull PreCelestialEventStartEvent event){ return EventBus.fire(PRE_START, event, "PreCelestialEventStartEvent"); }
    public static void fireCelestialEventEnd(@Nonnull CelestialEventEndEvent event){ EventBus.dispatch(END, event, "CelestialEventEndEvent"); }
}
