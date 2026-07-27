package plugin.siren.API;

import plugin.siren.Cultivation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Shared listener-list plumbing behind every {@code *Events} class in this
 * package. Not itself a stable API surface - register through
 * {@link CultivationEvents}, {@link SectEvents}, {@link WarEvents},
 * {@link DuelEvents}, {@link FormationEvents}, {@link DwellingEvents},
 * {@link BeastEvents}, {@link TechniqueEvents}, {@link DaoEvents} or
 * {@link ItemEvents} instead.
 *
 * <p>A listener that throws is logged and skipped so one broken addon can
 * neither break the mod's own systems nor the other addons listening to the
 * same event.</p>
 */
public final class EventBus {
    private EventBus(){}

    /** A fresh listener list, safe to register into from any plugin's setup() in any load order. */
    @Nonnull
    public static <T> List<Consumer<T>> newListenerList(){
        return new CopyOnWriteArrayList<>();
    }

    /** Notifies every listener of a post ("it happened") event. */
    public static <T> void dispatch(@Nonnull List<Consumer<T>> listeners, @Nonnull T event, @Nonnull String eventName){
        for(Consumer<T> listener : listeners){
            try{
                listener.accept(event);
            } catch(Throwable throwable){
                Cultivation.LOGGER.atWarning().withCause(throwable)
                        .log("A Cultivation %s listener threw - skipping it, remaining listeners still run.", eventName);
            }
        }
    }

    /**
     * Notifies every listener of a pre ("about to happen") event, letting each
     * one veto it or re-tune its numbers.
     *
     * @return {@code true} if the caller should go ahead, {@code false} if a
     * listener cancelled. Every listener runs either way, so a later one can
     * un-cancel what an earlier one vetoed.
     */
    public static <T extends CancellableEvent> boolean fire(@Nonnull List<Consumer<T>> listeners, @Nonnull T event, @Nonnull String eventName){
        dispatch(listeners, event, eventName);
        return !event.isCancelled();
    }
}
