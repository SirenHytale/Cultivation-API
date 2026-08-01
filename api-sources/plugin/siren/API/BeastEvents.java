package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Beast.BeastArt;
import plugin.siren.ECS.Components.SpiritBeastComponent;
import plugin.siren.ECS.Realms.CultivationRealm;
import plugin.siren.ECS.Realms.CultivationStage;
import plugin.siren.Utils.Config.BeastSpecies;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Spirit beast (灵兽) companion events - taming, hatching, summoning, and a
 * companion's own cultivation. See {@link CultivationEvents} for the
 * conventions every {@code *Events} class in this package shares.
 */
public final class BeastEvents {
    private BeastEvents(){}

    /** How a cultivator came by their companion. */
    public enum BindSource {
        /** Subdued and tamed in the wild. */
        TAME,
        /** Hatched from a Spirit Beast Egg. */
        HATCH
    }

    /** Why a companion's body left the world. */
    public enum DismissReason {
        /** Sent home; it keeps everything it has cultivated. */
        DISMISSED,
        /** Set free for good - the bond itself is broken. */
        RELEASED
    }

    // --- Post-events ---

    /** A tame was attempted. {@code success} says whether the beast was actually bound (the bind event follows when it was). */
    public record BeastTameAttemptEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                        @Nonnull BeastSpecies species, float chance, boolean success) {}

    /** A companion is now bound to a cultivator, replacing whatever they had before. */
    public record BeastBindEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                 @Nonnull BeastSpecies species, @Nonnull BindSource source) {}

    /** A companion's body was spawned beside its master. */
    public record BeastSummonEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                   @Nonnull Ref<EntityStore> beast, @Nonnull BeastSpecies species) {}

    /** A companion's body left the world - sent home, or freed for good. */
    public record BeastDismissEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player, @Nonnull DismissReason reason) {}

    /** A companion gained cultivation XP. {@code stagesGained} is how far that carried it (0 when it only banked progress). */
    public record BeastXpGainEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player, float amount, int stagesGained) {}

    /** A companion advanced a stage (or rolled into the next realm). Fires once per stage. */
    public record BeastAdvanceEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                    @Nonnull CultivationRealm realm, @Nonnull CultivationStage stage) {}

    /** A companion performed one of its arts. */
    public record BeastArtEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                @Nonnull SpiritBeastComponent beast, @Nonnull BeastArt art, boolean commanded) {}

    /** A companion's evolution ritual resolved - {@code succeeded} says which way, and {@code to} is null on failure. */
    public record BeastEvolveEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                   @Nonnull SpiritBeastComponent beast, @Nonnull BeastSpecies from,
                                   @Nullable BeastSpecies to, boolean succeeded) {}

    /** A companion was summoned in its rideable body. */
    public record BeastMountEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                  @Nonnull SpiritBeastComponent beast, @Nonnull BeastSpecies species) {}

    // --- Pre-events ---

    /** A tame is about to be rolled. Cancel to refuse the attempt outright (no talisman is spent); {@link #setChance} to re-weight the odds - 1 guarantees it, 0 dooms it. */
    public static final class PreBeastTameAttemptEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final BeastSpecies species;
        private float chance;

        public PreBeastTameAttemptEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                        @Nonnull BeastSpecies species, float chance){
            this.ref = ref;
            this.player = player;
            this.species = species;
            this.chance = chance;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public BeastSpecies species(){ return this.species; }
        /** Odds of success, 0-1, after realm and dao-resonance weighting. */
        public float chance(){ return this.chance; }
        public void setChance(float chance){ this.chance = chance; }
    }

    /** A companion is about to be bound. Cancel to refuse the bond - the cultivator keeps whatever beast they already had, and the talisman/egg is still spent. */
    public static final class PreBeastBindEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final BeastSpecies species;
        private final BindSource source;

        public PreBeastBindEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                 @Nonnull BeastSpecies species, @Nonnull BindSource source){
            this.ref = ref;
            this.player = player;
            this.species = species;
            this.source = source;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public BeastSpecies species(){ return this.species; }
        @Nonnull public BindSource source(){ return this.source; }
    }

    /** A companion's body is about to be spawned. Cancel to refuse (reported to the player as a failed summon). */
    public static final class PreBeastSummonEvent extends CancellableEvent {
        private final Ref<EntityStore> owner;
        private final PlayerRef player;
        private final BeastSpecies species;

        public PreBeastSummonEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player, @Nonnull BeastSpecies species){
            this.owner = owner;
            this.player = player;
            this.species = species;
        }

        @Nonnull public Ref<EntityStore> owner(){ return this.owner; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public BeastSpecies species(){ return this.species; }
    }

    /** A companion is about to be sent home or freed. Cancel to keep it where it is. */
    public static final class PreBeastDismissEvent extends CancellableEvent {
        private final Ref<EntityStore> owner;
        private final PlayerRef player;
        private final DismissReason reason;

        public PreBeastDismissEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player, @Nonnull DismissReason reason){
            this.owner = owner;
            this.player = player;
            this.reason = reason;
        }

        @Nonnull public Ref<EntityStore> owner(){ return this.owner; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DismissReason reason(){ return this.reason; }
    }

    /** A companion is about to gain XP. Cancel to deny it; {@link #setAmount} to re-scale. Fires for every source - meditation shares, kills, and hand-feeding alike. */
    public static final class PreBeastXpGainEvent extends CancellableEvent {
        private final Ref<EntityStore> owner;
        private final PlayerRef player;
        private final SpiritBeastComponent beast;
        private float amount;

        public PreBeastXpGainEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                   @Nonnull SpiritBeastComponent beast, float amount){
            this.owner = owner;
            this.player = player;
            this.beast = beast;
            this.amount = amount;
        }

        @Nonnull public Ref<EntityStore> owner(){ return this.owner; }
        @Nullable public PlayerRef player(){ return this.player; }
        /** The companion's live state - its realm, stage and banked XP as they stand. */
        @Nonnull public SpiritBeastComponent beast(){ return this.beast; }
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
    }

    /** A companion is about to advance a stage. Cancel to hold it where it is - the XP for that stage is already spent, so this costs it the progress. */
    public static final class PreBeastAdvanceEvent extends CancellableEvent {
        private final Ref<EntityStore> owner;
        private final PlayerRef player;
        private final CultivationRealm fromRealm;
        private final CultivationStage fromStage;

        public PreBeastAdvanceEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                    @Nonnull CultivationRealm fromRealm, @Nonnull CultivationStage fromStage){
            this.owner = owner;
            this.player = player;
            this.fromRealm = fromRealm;
            this.fromStage = fromStage;
        }

        @Nonnull public Ref<EntityStore> owner(){ return this.owner; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public CultivationRealm fromRealm(){ return this.fromRealm; }
        @Nonnull public CultivationStage fromStage(){ return this.fromStage; }
    }

    /**
     * A companion is about to perform an art. Cancel to stop it - the cooldown is
     * only stamped once the effect has actually run, so a vetoed art costs the
     * beast nothing and it will try again on its next opening.
     */
    public static final class PreBeastArtEvent extends CancellableEvent {
        private final Ref<EntityStore> owner;
        private final PlayerRef player;
        private final SpiritBeastComponent beast;
        private final BeastArt art;
        private final boolean commanded;

        public PreBeastArtEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                @Nonnull SpiritBeastComponent beast, @Nonnull BeastArt art, boolean commanded){
            this.owner = owner;
            this.player = player;
            this.beast = beast;
            this.art = art;
            this.commanded = commanded;
        }

        @Nonnull public Ref<EntityStore> owner(){ return this.owner; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public SpiritBeastComponent beast(){ return this.beast; }
        @Nonnull public BeastArt art(){ return this.art; }
        /** True when the owner named this art, false when the beast chose it itself. */
        public boolean commanded(){ return this.commanded; }
    }

    /**
     * A companion is about to be put through the evolution ritual. Cancel to
     * refuse it - the Qi has NOT been taken at this point, so a veto here costs
     * the cultivator nothing.
     */
    public static final class PreBeastEvolveEvent extends CancellableEvent {
        private final Ref<EntityStore> owner;
        private final PlayerRef player;
        private final SpiritBeastComponent beast;
        private final BeastSpecies from;
        private final BeastSpecies to;
        private float successChance;

        public PreBeastEvolveEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                   @Nonnull SpiritBeastComponent beast, @Nonnull BeastSpecies from,
                                   @Nonnull BeastSpecies to, float successChance){
            this.owner = owner;
            this.player = player;
            this.beast = beast;
            this.from = from;
            this.to = to;
            this.successChance = successChance;
        }

        @Nonnull public Ref<EntityStore> owner(){ return this.owner; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public SpiritBeastComponent beast(){ return this.beast; }
        @Nonnull public BeastSpecies from(){ return this.from; }
        @Nonnull public BeastSpecies to(){ return this.to; }
        /** The odds the ritual takes. Mutable, so an addon can make its own creature safer or wilder to evolve. */
        public float successChance(){ return this.successChance; }
        public void setSuccessChance(float successChance){ this.successChance = successChance; }
    }

    /** A companion is about to be summoned in its rideable body. Cancel to refuse the mount. */
    public static final class PreBeastMountEvent extends CancellableEvent {
        private final Ref<EntityStore> owner;
        private final PlayerRef player;
        private final SpiritBeastComponent beast;
        private final BeastSpecies species;

        public PreBeastMountEvent(@Nonnull Ref<EntityStore> owner, @Nullable PlayerRef player,
                                  @Nonnull SpiritBeastComponent beast, @Nonnull BeastSpecies species){
            this.owner = owner;
            this.player = player;
            this.beast = beast;
            this.species = species;
        }

        @Nonnull public Ref<EntityStore> owner(){ return this.owner; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public SpiritBeastComponent beast(){ return this.beast; }
        @Nonnull public BeastSpecies species(){ return this.species; }
    }

    // --- Listener registration ---

    private static final List<Consumer<BeastTameAttemptEvent>> TAME = EventBus.newListenerList();
    private static final List<Consumer<PreBeastTameAttemptEvent>> PRE_TAME = EventBus.newListenerList();
    private static final List<Consumer<BeastBindEvent>> BIND = EventBus.newListenerList();
    private static final List<Consumer<PreBeastBindEvent>> PRE_BIND = EventBus.newListenerList();
    private static final List<Consumer<BeastSummonEvent>> SUMMON = EventBus.newListenerList();
    private static final List<Consumer<PreBeastSummonEvent>> PRE_SUMMON = EventBus.newListenerList();
    private static final List<Consumer<BeastDismissEvent>> DISMISS = EventBus.newListenerList();
    private static final List<Consumer<PreBeastDismissEvent>> PRE_DISMISS = EventBus.newListenerList();
    private static final List<Consumer<BeastXpGainEvent>> XP_GAIN = EventBus.newListenerList();
    private static final List<Consumer<PreBeastXpGainEvent>> PRE_XP_GAIN = EventBus.newListenerList();
    private static final List<Consumer<BeastArtEvent>> ART = EventBus.newListenerList();
    private static final List<Consumer<PreBeastArtEvent>> PRE_ART = EventBus.newListenerList();
    private static final List<Consumer<BeastEvolveEvent>> EVOLVE = EventBus.newListenerList();
    private static final List<Consumer<PreBeastEvolveEvent>> PRE_EVOLVE = EventBus.newListenerList();
    private static final List<Consumer<BeastMountEvent>> MOUNT = EventBus.newListenerList();
    private static final List<Consumer<PreBeastMountEvent>> PRE_MOUNT = EventBus.newListenerList();
    private static final List<Consumer<BeastAdvanceEvent>> ADVANCE = EventBus.newListenerList();
    private static final List<Consumer<PreBeastAdvanceEvent>> PRE_ADVANCE = EventBus.newListenerList();

    public static void onBeastTameAttempt(@Nonnull Consumer<BeastTameAttemptEvent> listener){ TAME.add(listener); }
    public static void onPreBeastTameAttempt(@Nonnull Consumer<PreBeastTameAttemptEvent> listener){ PRE_TAME.add(listener); }
    public static void onBeastBind(@Nonnull Consumer<BeastBindEvent> listener){ BIND.add(listener); }
    public static void onPreBeastBind(@Nonnull Consumer<PreBeastBindEvent> listener){ PRE_BIND.add(listener); }
    public static void onBeastSummon(@Nonnull Consumer<BeastSummonEvent> listener){ SUMMON.add(listener); }
    public static void onPreBeastSummon(@Nonnull Consumer<PreBeastSummonEvent> listener){ PRE_SUMMON.add(listener); }
    public static void onBeastDismiss(@Nonnull Consumer<BeastDismissEvent> listener){ DISMISS.add(listener); }
    public static void onPreBeastDismiss(@Nonnull Consumer<PreBeastDismissEvent> listener){ PRE_DISMISS.add(listener); }
    public static void onBeastXpGain(@Nonnull Consumer<BeastXpGainEvent> listener){ XP_GAIN.add(listener); }
    public static void onPreBeastXpGain(@Nonnull Consumer<PreBeastXpGainEvent> listener){ PRE_XP_GAIN.add(listener); }
    public static void onBeastAdvance(@Nonnull Consumer<BeastAdvanceEvent> listener){ ADVANCE.add(listener); }
    public static void onPreBeastAdvance(@Nonnull Consumer<PreBeastAdvanceEvent> listener){ PRE_ADVANCE.add(listener); }
    public static void onBeastArt(@Nonnull Consumer<BeastArtEvent> listener){ ART.add(listener); }
    public static void onPreBeastArt(@Nonnull Consumer<PreBeastArtEvent> listener){ PRE_ART.add(listener); }
    public static void onBeastEvolve(@Nonnull Consumer<BeastEvolveEvent> listener){ EVOLVE.add(listener); }
    public static void onPreBeastEvolve(@Nonnull Consumer<PreBeastEvolveEvent> listener){ PRE_EVOLVE.add(listener); }
    public static void onBeastMount(@Nonnull Consumer<BeastMountEvent> listener){ MOUNT.add(listener); }
    public static void onPreBeastMount(@Nonnull Consumer<PreBeastMountEvent> listener){ PRE_MOUNT.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireBeastTameAttempt(@Nonnull BeastTameAttemptEvent event){ EventBus.dispatch(TAME, event, "BeastTameAttemptEvent"); }
    public static boolean firePreBeastTameAttempt(@Nonnull PreBeastTameAttemptEvent event){ return EventBus.fire(PRE_TAME, event, "PreBeastTameAttemptEvent"); }
    public static void fireBeastBind(@Nonnull BeastBindEvent event){ EventBus.dispatch(BIND, event, "BeastBindEvent"); }
    public static boolean firePreBeastBind(@Nonnull PreBeastBindEvent event){ return EventBus.fire(PRE_BIND, event, "PreBeastBindEvent"); }
    public static void fireBeastSummon(@Nonnull BeastSummonEvent event){ EventBus.dispatch(SUMMON, event, "BeastSummonEvent"); }
    public static boolean firePreBeastSummon(@Nonnull PreBeastSummonEvent event){ return EventBus.fire(PRE_SUMMON, event, "PreBeastSummonEvent"); }
    public static void fireBeastDismiss(@Nonnull BeastDismissEvent event){ EventBus.dispatch(DISMISS, event, "BeastDismissEvent"); }
    public static boolean firePreBeastDismiss(@Nonnull PreBeastDismissEvent event){ return EventBus.fire(PRE_DISMISS, event, "PreBeastDismissEvent"); }
    public static void fireBeastXpGain(@Nonnull BeastXpGainEvent event){ EventBus.dispatch(XP_GAIN, event, "BeastXpGainEvent"); }
    public static boolean firePreBeastXpGain(@Nonnull PreBeastXpGainEvent event){ return EventBus.fire(PRE_XP_GAIN, event, "PreBeastXpGainEvent"); }
    public static void fireBeastAdvance(@Nonnull BeastAdvanceEvent event){ EventBus.dispatch(ADVANCE, event, "BeastAdvanceEvent"); }
    public static boolean firePreBeastAdvance(@Nonnull PreBeastAdvanceEvent event){ return EventBus.fire(PRE_ADVANCE, event, "PreBeastAdvanceEvent"); }
    public static void fireBeastArt(@Nonnull BeastArtEvent event){ EventBus.dispatch(ART, event, "BeastArtEvent"); }
    public static boolean firePreBeastArt(@Nonnull PreBeastArtEvent event){ return EventBus.fire(PRE_ART, event, "PreBeastArtEvent"); }
    public static void fireBeastEvolve(@Nonnull BeastEvolveEvent event){ EventBus.dispatch(EVOLVE, event, "BeastEvolveEvent"); }
    public static boolean firePreBeastEvolve(@Nonnull PreBeastEvolveEvent event){ return EventBus.fire(PRE_EVOLVE, event, "PreBeastEvolveEvent"); }
    public static void fireBeastMount(@Nonnull BeastMountEvent event){ EventBus.dispatch(MOUNT, event, "BeastMountEvent"); }
    public static boolean firePreBeastMount(@Nonnull PreBeastMountEvent event){ return EventBus.fire(PRE_MOUNT, event, "PreBeastMountEvent"); }
}
