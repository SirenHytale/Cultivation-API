package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Array.ArrayDestination;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Teleportation Array events - a cultivator hopping between a Cave Abode, a
 * sect hall, a Secret Realm site, their Sea of Consciousness, or the Heavenly
 * Realm through the network. See {@link CultivationEvents} for the
 * conventions every {@code *Events} class in this package shares.
 */
public final class ArrayEvents {
    private ArrayEvents(){}

    // --- Post-events ---

    /** A cultivator successfully traveled through the array. {@code qiCost} is the FINAL amount actually charged (0 for an admin bypass). */
    public record ArrayTravelEvent(@Nullable Ref<EntityStore> ref, @Nullable PlayerRef player,
                                   @Nonnull ArrayDestination.Kind destinationKind, @Nonnull String destinationId,
                                   float qiCost) {}

    // --- Pre-events ---

    /**
     * A cultivator is about to travel through the array. Cancel to refuse the
     * trip (nothing is charged, nothing moves); {@link #setQiCost} to
     * re-price it - it is charged (and re-checked against the traveler's
     * banked Qi) after this fires, mirroring {@code DaoEvents.PreDaoElementChangeEvent}'s
     * own mutable-cost shape exactly.
     */
    public static final class PreArrayTravelEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final ArrayDestination.Kind destinationKind;
        private final String destinationId;
        private float qiCost;

        public PreArrayTravelEvent(@Nullable Ref<EntityStore> ref, @Nullable PlayerRef player,
                                   @Nonnull ArrayDestination.Kind destinationKind, @Nonnull String destinationId, float qiCost){
            this.ref = ref;
            this.player = player;
            this.destinationKind = destinationKind;
            this.destinationId = destinationId;
            this.qiCost = qiCost;
        }

        @Nullable public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public ArrayDestination.Kind destinationKind(){ return this.destinationKind; }
        @Nonnull public String destinationId(){ return this.destinationId; }
        public float qiCost(){ return this.qiCost; }
        public void setQiCost(float qiCost){ this.qiCost = qiCost; }
    }

    // --- Listener registration ---

    private static final List<Consumer<ArrayTravelEvent>> TRAVEL = EventBus.newListenerList();
    private static final List<Consumer<PreArrayTravelEvent>> PRE_TRAVEL = EventBus.newListenerList();

    public static void onArrayTravel(@Nonnull Consumer<ArrayTravelEvent> listener){ TRAVEL.add(listener); }
    public static void onPreArrayTravel(@Nonnull Consumer<PreArrayTravelEvent> listener){ PRE_TRAVEL.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireArrayTravel(@Nonnull ArrayTravelEvent event){ EventBus.dispatch(TRAVEL, event, "ArrayTravelEvent"); }
    public static boolean firePreArrayTravel(@Nonnull PreArrayTravelEvent event){ return EventBus.fire(PRE_TRAVEL, event, "PreArrayTravelEvent"); }
}
