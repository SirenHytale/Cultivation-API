package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Forging.ForgeGrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Forging (锻造) events - tempering an already-crafted Cultivation weapon/armor
 * at a Forge Anchor. See {@link CultivationEvents} for the conventions every
 * {@code *Events} class in this package shares (pre/post pairing, threading, a
 * throwing listener being logged and skipped).
 *
 * <p><b>Who fires what:</b> {@code ForgeCmd}'s "combine" subcommand fires
 * {@link PreForgeEvent} once every up-front check (enabled, anchor proximity,
 * held item is forgeable, target tier within Forge-Max-Tier, Qi floor,
 * materials available) has already passed but BEFORE anything is spent - a
 * veto here costs the cultivator nothing at all, unlike every other Pre-event
 * in this package that fires after its ritual's up-front cost is already sunk
 * (compare {@code TalismanEvents.PreInscribeStartEvent}, which fires after the
 * proximity/Qi-floor checks but still before its OWN materials are consumed -
 * the same placement, just described precisely for this ritual's own checks).
 * {@code ForgingRitualSystem} fires {@link ForgeCompleteEvent} once an attempt
 * resolves, whether that is a natural completion, a plain failure, a botch, or
 * an interruption (wandering off / running dry) - all four are "it happened"
 * from an observer's point of view, so none of them are vetoable.</p>
 */
public final class ForgingEvents {
    private ForgingEvents(){}

    /** How a completed (or interrupted) forging attempt resolved. */
    public enum ForgeOutcome {
        SUCCESS,
        /** Failed outright - wastes the spent materials, and demotes the item a tier if Forge-Demote-On-Fail-From-Tier says so. */
        FAILED,
        /** Failed AND the forge turned on the smith - see ForgingConfig's Forge-Botch-* fields. */
        BOTCH
    }

    // --- Post-event ---

    /**
     * A forging attempt resolved. {@code grade} is null unless {@code outcome}
     * is SUCCESS. {@code resultStack} is the item as it now stands (forged,
     * demoted, or unchanged) - null only if there was nowhere to place it back
     * into the cultivator's inventory (see {@code ForgingManager#placeItem}),
     * which the accompanying player message already reports. The item is never
     * destroyed outright by anything this event could be reporting.
     */
    public record ForgeCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, int targetTier,
                                     @Nonnull ForgeOutcome outcome, @Nullable ForgeGrade grade, @Nullable ItemStack resultStack) {}

    // --- Pre-event ---

    /**
     * A forging attempt is about to begin - every check has passed, but no
     * materials and no item have been touched yet. Cancel to refuse it
     * entirely; nothing is spent and no ritual starts.
     */
    public static final class PreForgeEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String itemId;
        private final int targetTier;

        public PreForgeEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull String itemId, int targetTier){
            this.ref = ref;
            this.player = player;
            this.itemId = itemId;
            this.targetTier = targetTier;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        /** The item id of the weapon/armor about to be forged. */
        @Nonnull public String itemId(){ return this.itemId; }
        public int targetTier(){ return this.targetTier; }
    }

    // --- Listener registration ---

    private static final List<Consumer<ForgeCompleteEvent>> FORGE_COMPLETE = EventBus.newListenerList();
    private static final List<Consumer<PreForgeEvent>> PRE_FORGE = EventBus.newListenerList();

    public static void onForgeComplete(@Nonnull Consumer<ForgeCompleteEvent> listener){ FORGE_COMPLETE.add(listener); }
    public static void onPreForge(@Nonnull Consumer<PreForgeEvent> listener){ PRE_FORGE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems/commands; not API) ---

    public static void fireForgeComplete(@Nonnull ForgeCompleteEvent event){ EventBus.dispatch(FORGE_COMPLETE, event, "ForgeCompleteEvent"); }
    public static boolean firePreForge(@Nonnull PreForgeEvent event){ return EventBus.fire(PRE_FORGE, event, "PreForgeEvent"); }
}
