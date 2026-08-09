package plugin.siren.API;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Events from the Treasure Pavilion benefit sync - how an addon applies (and
 * un-applies) whatever its registered {@link StoreBenefit} actually does.
 *
 * <h2>Threading - read this one</h2>
 *
 * <p>Unlike this mod's gameplay events, these fire on the REMOTE CHECKER
 * thread, not on a world thread: grants and revokes are discovered by an HTTP
 * sweep, and there is no world in hand. A listener that touches a player must
 * find them ({@code Universe.get().getPlayer(uuid)}, check {@code isValid()})
 * and hop onto their world thread before reading or writing any component.
 * Purely bookkeeping listeners may run in place.</p>
 *
 * <p>Not cancellable, deliberately: the sync reports what the store says is
 * owned, and local policy already has its switch - a server refuses a product
 * via Disabled-Benefits, which revokes as if nobody owned it.</p>
 */
public final class StoreBenefitEvents {

    private StoreBenefitEvents(){
    }

    /** A player the last sweep did not list is now entitled to {@code benefit}. */
    public record BenefitGrantedEvent(@Nonnull UUID playerUuid, @Nonnull StoreBenefit benefit){
    }

    /**
     * A previously entitled player is no longer listed - a refund, a
     * chargeback, or the server disabling the product. Fired for each player,
     * whether or not they are online; the wearer of an auto-registered title
     * keeps it only until their next join, when it is re-validated.
     */
    public record BenefitRevokedEvent(@Nonnull UUID playerUuid, @Nonnull StoreBenefit benefit){
    }

    /**
     * One sweep finished - every registered product was fetched (or skipped as
     * disabled). {@code failedProducts} names the fetches that came to
     * nothing; their previous lists were kept, not cleared.
     */
    public record SyncCompletedEvent(int products, int granted, int revoked,
                                     @Nonnull List<String> failedProducts){
    }

    private static final List<Consumer<BenefitGrantedEvent>> GRANTED = EventBus.newListenerList();
    private static final List<Consumer<BenefitRevokedEvent>> REVOKED = EventBus.newListenerList();
    private static final List<Consumer<SyncCompletedEvent>> SYNC_COMPLETED = EventBus.newListenerList();

    public static void onBenefitGranted(@Nonnull Consumer<BenefitGrantedEvent> listener){
        GRANTED.add(listener);
    }

    public static void onBenefitRevoked(@Nonnull Consumer<BenefitRevokedEvent> listener){
        REVOKED.add(listener);
    }

    public static void onSyncCompleted(@Nonnull Consumer<SyncCompletedEvent> listener){
        SYNC_COMPLETED.add(listener);
    }

    public static void fireBenefitGranted(@Nonnull BenefitGrantedEvent event){
        EventBus.dispatch(GRANTED, event, "StoreBenefitGrantedEvent");
    }

    public static void fireBenefitRevoked(@Nonnull BenefitRevokedEvent event){
        EventBus.dispatch(REVOKED, event, "StoreBenefitRevokedEvent");
    }

    public static void fireSyncCompleted(@Nonnull SyncCompletedEvent event){
        EventBus.dispatch(SYNC_COMPLETED, event, "StoreBenefitSyncCompletedEvent");
    }
}
