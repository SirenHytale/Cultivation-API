package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Formation.FormationType;
import plugin.siren.Utils.Formation.Formation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Spirit-array (formation) events - laying and dispersing chunk-anchored
 * arrays, and the Trapping array wounding an intruder. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares.
 */
public final class FormationEvents {
    private FormationEvents(){}

    // --- Post-events ---

    /** A spirit array was laid down and is now live on its chunk. */
    public record FormationPlaceEvent(@Nonnull UUID owner, @Nonnull String sectName, @Nonnull Formation formation) {}

    /** A spirit array was dispersed by its controller. {@code formation} is the now-removed object. */
    public record FormationRemoveEvent(@Nonnull UUID owner, @Nonnull String sectName, @Nonnull Formation formation) {}

    /** A Trapping array wounded an intruder standing inside it. {@code damage} is the post-lethality-cap amount fed to the damage pipeline (pre-armor/reduction). */
    public record FormationTrapStrikeEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                           @Nonnull String world, int chunkX, int chunkZ, float damage) {}

    // --- Pre-events ---

    /** An array is about to be laid. Cancel to refuse it (reported as the ground being warded); {@link #setRadiusChunks} to change how far it reaches. */
    public static final class PreFormationPlaceEvent extends CancellableEvent {
        private final UUID owner;
        private final String sectName;
        private final String world;
        private final int chunkX;
        private final int chunkZ;
        private final FormationType type;
        private int radiusChunks;

        public PreFormationPlaceEvent(@Nonnull UUID owner, @Nonnull String sectName, @Nonnull String world,
                                      int chunkX, int chunkZ, @Nonnull FormationType type, int radiusChunks){
            this.owner = owner;
            this.sectName = sectName;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.type = type;
            this.radiusChunks = radiusChunks;
        }

        @Nonnull public UUID owner(){ return this.owner; }
        /** The placer's sect, or empty when the array is theirs personally. */
        @Nonnull public String sectName(){ return this.sectName; }
        @Nonnull public String world(){ return this.world; }
        public int chunkX(){ return this.chunkX; }
        public int chunkZ(){ return this.chunkZ; }
        @Nonnull public FormationType type(){ return this.type; }
        /** How many chunk rings out from its anchor the array reaches. */
        public int radiusChunks(){ return this.radiusChunks; }
        public void setRadiusChunks(int radiusChunks){ this.radiusChunks = radiusChunks; }
    }

    /** An array is about to be dispersed by its controller. Cancel to leave it standing. */
    public static final class PreFormationRemoveEvent extends CancellableEvent {
        private final UUID owner;
        private final String sectName;
        private final Formation formation;

        public PreFormationRemoveEvent(@Nonnull UUID owner, @Nonnull String sectName, @Nonnull Formation formation){
            this.owner = owner;
            this.sectName = sectName;
            this.formation = formation;
        }

        @Nonnull public UUID owner(){ return this.owner; }
        @Nonnull public String sectName(){ return this.sectName; }
        @Nonnull public Formation formation(){ return this.formation; }
    }

    /** A Trapping array is about to wound an intruder. Cancel to spare them this tick entirely (no particle, no debuff, no damage); set {@link #setDamage} to 0 to root them harmlessly. */
    public static final class PreFormationTrapStrikeEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String world;
        private final int chunkX;
        private final int chunkZ;
        private float damage;

        public PreFormationTrapStrikeEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                           @Nonnull String world, int chunkX, int chunkZ, float damage){
            this.ref = ref;
            this.player = player;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.damage = damage;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public String world(){ return this.world; }
        public int chunkX(){ return this.chunkX; }
        public int chunkZ(){ return this.chunkZ; }
        /** Post-lethality-cap damage, before armor and reduction filters. 0 when the intruder's health couldn't be resolved. */
        public float damage(){ return this.damage; }
        public void setDamage(float damage){ this.damage = damage; }
    }

    // --- Listener registration ---

    private static final List<Consumer<FormationPlaceEvent>> PLACE = EventBus.newListenerList();
    private static final List<Consumer<PreFormationPlaceEvent>> PRE_PLACE = EventBus.newListenerList();
    private static final List<Consumer<FormationRemoveEvent>> REMOVE = EventBus.newListenerList();
    private static final List<Consumer<PreFormationRemoveEvent>> PRE_REMOVE = EventBus.newListenerList();
    private static final List<Consumer<FormationTrapStrikeEvent>> TRAP_STRIKE = EventBus.newListenerList();
    private static final List<Consumer<PreFormationTrapStrikeEvent>> PRE_TRAP_STRIKE = EventBus.newListenerList();

    public static void onFormationPlace(@Nonnull Consumer<FormationPlaceEvent> listener){ PLACE.add(listener); }
    public static void onPreFormationPlace(@Nonnull Consumer<PreFormationPlaceEvent> listener){ PRE_PLACE.add(listener); }
    public static void onFormationRemove(@Nonnull Consumer<FormationRemoveEvent> listener){ REMOVE.add(listener); }
    public static void onPreFormationRemove(@Nonnull Consumer<PreFormationRemoveEvent> listener){ PRE_REMOVE.add(listener); }
    public static void onFormationTrapStrike(@Nonnull Consumer<FormationTrapStrikeEvent> listener){ TRAP_STRIKE.add(listener); }
    public static void onPreFormationTrapStrike(@Nonnull Consumer<PreFormationTrapStrikeEvent> listener){ PRE_TRAP_STRIKE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireFormationPlace(@Nonnull FormationPlaceEvent event){ EventBus.dispatch(PLACE, event, "FormationPlaceEvent"); }
    public static boolean firePreFormationPlace(@Nonnull PreFormationPlaceEvent event){ return EventBus.fire(PRE_PLACE, event, "PreFormationPlaceEvent"); }
    public static void fireFormationRemove(@Nonnull FormationRemoveEvent event){ EventBus.dispatch(REMOVE, event, "FormationRemoveEvent"); }
    public static boolean firePreFormationRemove(@Nonnull PreFormationRemoveEvent event){ return EventBus.fire(PRE_REMOVE, event, "PreFormationRemoveEvent"); }
    public static void fireFormationTrapStrike(@Nonnull FormationTrapStrikeEvent event){ EventBus.dispatch(TRAP_STRIKE, event, "FormationTrapStrikeEvent"); }
    public static boolean firePreFormationTrapStrike(@Nonnull PreFormationTrapStrikeEvent event){ return EventBus.fire(PRE_TRAP_STRIKE, event, "PreFormationTrapStrikeEvent"); }
}
