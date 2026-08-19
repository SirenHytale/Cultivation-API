package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Beast.BeastEggMetadata;
import plugin.siren.Utils.Config.BeastSpecies;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Spirit beast breeding (灵兽繁育) events - the two-cultivator ritual at a
 * Beast Pen, and hatching the bred egg it produces. See {@link CultivationEvents}
 * for the conventions every {@code *Events} class in this package shares, and
 * {@link BeastEvents} for the taming/hatching/summoning events this
 * complements (a bred egg still fires {@link BeastEvents#onBeastBind} when it
 * hatches - {@code BeastManager.bind} is unchanged by this feature).
 */
public final class BreedingEvents {
    private BreedingEvents(){}

    // --- Post-events ---

    /**
     * A breeding ritual completed and produced an egg for {@code recipient}
     * (whoever sent the offer). Fires once, from the ritual's own completion,
     * not from either offer or accept.
     */
    public record BeastBreedEvent(@Nonnull PlayerRef recipient, @Nullable PlayerRef partner,
                                  @Nonnull BeastSpecies parentA, @Nonnull BeastSpecies parentB,
                                  @Nonnull BeastSpecies offspring, float quality) {}

    /** A bred egg hatched into a bound companion. */
    public record BeastEggHatchEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                     @Nonnull BeastSpecies offspring, @Nonnull BeastEggMetadata metadata) {}

    // --- Pre-events ---

    /**
     * A breeding ritual is about to be seated on both cultivators - fires once
     * both are confirmed within a shared pen with enough Qi, before either
     * cultivator's Qi is touched. Cancel to refuse it outright; nothing has been
     * spent yet.
     */
    public static final class PreBeastBreedEvent extends CancellableEvent {
        private final PlayerRef offerer;
        private final PlayerRef accepter;
        private final BeastSpecies parentA;
        private final BeastSpecies parentB;

        public PreBeastBreedEvent(@Nonnull PlayerRef offerer, @Nonnull PlayerRef accepter,
                                  @Nonnull BeastSpecies parentA, @Nonnull BeastSpecies parentB){
            this.offerer = offerer;
            this.accepter = accepter;
            this.parentA = parentA;
            this.parentB = parentB;
        }

        @Nonnull public PlayerRef offerer(){ return this.offerer; }
        @Nonnull public PlayerRef accepter(){ return this.accepter; }
        /** The species that will be Parent A - the offerer's own bound beast. */
        @Nonnull public BeastSpecies parentA(){ return this.parentA; }
        /** The species that will be Parent B - the accepter's own bound beast. */
        @Nonnull public BeastSpecies parentB(){ return this.parentB; }
    }

    /**
     * A bred egg is about to hatch. Cancel to refuse it - the egg is NOT
     * consumed, matching a dormant wild-hatched egg (see
     * {@code BeastEggHatchInteraction}'s bred branch).
     */
    public static final class PreBeastEggHatchEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final BeastSpecies offspring;
        private final BeastEggMetadata metadata;

        public PreBeastEggHatchEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                     @Nonnull BeastSpecies offspring, @Nonnull BeastEggMetadata metadata){
            this.ref = ref;
            this.player = player;
            this.offspring = offspring;
            this.metadata = metadata;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public BeastSpecies offspring(){ return this.offspring; }
        @Nonnull public BeastEggMetadata metadata(){ return this.metadata; }
    }

    // --- Listener registration ---

    private static final List<Consumer<BeastBreedEvent>> BREED = EventBus.newListenerList();
    private static final List<Consumer<PreBeastBreedEvent>> PRE_BREED = EventBus.newListenerList();
    private static final List<Consumer<BeastEggHatchEvent>> EGG_HATCH = EventBus.newListenerList();
    private static final List<Consumer<PreBeastEggHatchEvent>> PRE_EGG_HATCH = EventBus.newListenerList();

    public static void onBeastBreed(@Nonnull Consumer<BeastBreedEvent> listener){ BREED.add(listener); }
    public static void onPreBeastBreed(@Nonnull Consumer<PreBeastBreedEvent> listener){ PRE_BREED.add(listener); }
    public static void onBeastEggHatch(@Nonnull Consumer<BeastEggHatchEvent> listener){ EGG_HATCH.add(listener); }
    public static void onPreBeastEggHatch(@Nonnull Consumer<PreBeastEggHatchEvent> listener){ PRE_EGG_HATCH.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireBeastBreed(@Nonnull BeastBreedEvent event){ EventBus.dispatch(BREED, event, "BeastBreedEvent"); }
    public static boolean firePreBeastBreed(@Nonnull PreBeastBreedEvent event){ return EventBus.fire(PRE_BREED, event, "PreBeastBreedEvent"); }
    public static void fireBeastEggHatch(@Nonnull BeastEggHatchEvent event){ EventBus.dispatch(EGG_HATCH, event, "BeastEggHatchEvent"); }
    public static boolean firePreBeastEggHatch(@Nonnull PreBeastEggHatchEvent event){ return EventBus.fire(PRE_EGG_HATCH, event, "PreBeastEggHatchEvent"); }
}
