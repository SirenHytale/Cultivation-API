package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3dc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Rogue Cultivator events. See {@link CultivationEvents} for the conventions
 * every {@code *Events} class in this package shares (pre vs post,
 * cancellation, threading, registration).
 *
 * <p>Dispatched by {@code RogueManager}/{@code RogueCultivatorSystem} - this
 * class only declares the surface; nothing here calls into that manager,
 * matching every other standalone {@code *Events} class in this package.</p>
 */
public final class RogueEvents {
    private RogueEvents(){}

    // --- Pre-events ---

    /**
     * A Rogue Cultivator is about to spawn near {@code anchorPosition} in
     * {@code worldName}. Cancel to refuse the spawn entirely - the world's
     * next-spawn-due clock still advances normally, exactly like a refused
     * roll that found no valid ground.
     */
    public static final class PreRogueCultivatorSpawnEvent extends CancellableEvent {
        private final String worldName;
        private final Vector3dc anchorPosition;
        private final UUID anchorPlayerUuid;
        private int realmOrdinal;
        private String daoElementName;

        public PreRogueCultivatorSpawnEvent(@Nonnull String worldName, @Nonnull Vector3dc anchorPosition,
                                            @Nonnull UUID anchorPlayerUuid, int realmOrdinal, @Nonnull String daoElementName){
            this.worldName = worldName;
            this.anchorPosition = anchorPosition;
            this.anchorPlayerUuid = anchorPlayerUuid;
            this.realmOrdinal = realmOrdinal;
            this.daoElementName = daoElementName;
        }

        @Nonnull public String worldName(){ return this.worldName; }
        @Nonnull public Vector3dc anchorPosition(){ return this.anchorPosition; }
        @Nonnull public UUID anchorPlayerUuid(){ return this.anchorPlayerUuid; }
        public int realmOrdinal(){ return this.realmOrdinal; }
        /** A listener may re-tune the realm the rogue latches at spawn (D-044) before it is applied. */
        public void setRealmOrdinal(int realmOrdinal){ this.realmOrdinal = realmOrdinal; }
        @Nonnull public String daoElementName(){ return this.daoElementName; }
        /** A listener may re-tune the {@code DaoElement} name the rogue rolls before it is applied. */
        public void setDaoElementName(@Nonnull String daoElementName){ this.daoElementName = daoElementName; }
    }

    // --- Post-events ---

    /** A Rogue Cultivator has finished spawning and is tagged/live in the world. */
    public record RogueCultivatorSpawnedEvent(@Nonnull String encounterId, @Nonnull Ref<EntityStore> npcRef,
                                              @Nonnull String worldName, int realmOrdinal, @Nonnull String daoElementName){}

    /** A Rogue Cultivator has been slain - any manual/core/Testament roll has already resolved by the time this fires. */
    public record RogueCultivatorSlainEvent(@Nonnull String encounterId, @Nullable UUID killerUuid, @Nonnull String worldName,
                                            boolean manualAwarded, boolean bonusCoreAwarded, boolean testamentAwarded){}

    // --- Listener registration ---

    private static final List<Consumer<PreRogueCultivatorSpawnEvent>> PRE_SPAWN = EventBus.newListenerList();
    private static final List<Consumer<RogueCultivatorSpawnedEvent>> SPAWNED = EventBus.newListenerList();
    private static final List<Consumer<RogueCultivatorSlainEvent>> SLAIN = EventBus.newListenerList();

    public static void onPreRogueCultivatorSpawn(@Nonnull Consumer<PreRogueCultivatorSpawnEvent> listener){ PRE_SPAWN.add(listener); }
    public static void onRogueCultivatorSpawned(@Nonnull Consumer<RogueCultivatorSpawnedEvent> listener){ SPAWNED.add(listener); }
    public static void onRogueCultivatorSlain(@Nonnull Consumer<RogueCultivatorSlainEvent> listener){ SLAIN.add(listener); }

    // --- Internal dispatch (called by RogueManager/its ECS systems; not API) ---

    public static boolean firePreRogueCultivatorSpawn(@Nonnull PreRogueCultivatorSpawnEvent event){ return EventBus.fire(PRE_SPAWN, event, "PreRogueCultivatorSpawnEvent"); }
    public static void fireRogueCultivatorSpawned(@Nonnull RogueCultivatorSpawnedEvent event){ EventBus.dispatch(SPAWNED, event, "RogueCultivatorSpawnedEvent"); }
    public static void fireRogueCultivatorSlain(@Nonnull RogueCultivatorSlainEvent event){ EventBus.dispatch(SLAIN, event, "RogueCultivatorSlainEvent"); }
}
