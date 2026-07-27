package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Dao.DaoElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Item events - the cultivation drops (cores, eggs, Spirit Stones, manuals),
 * consuming pills and cores, reading manuals, and weapon refinement (炼器). See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares.
 *
 * <p>Learning a technique from a manual fires {@link TechniqueEvents}'
 * learn events; a manual that teaches a skill node goes through
 * {@link CultivationEvents}' skill-unlock events. This class covers the manual
 * ITEM itself.</p>
 */
public final class ItemEvents {
    private ItemEvents(){}

    /** What a cultivation drop is. */
    public enum LootType {
        /** A cultivation core of some tier - Spirit, Profound or Divine (read {@code itemId} for which). */
        CULTIVATION_CORE,
        /** A Spirit Beast Egg. */
        BEAST_EGG,
        /** A Spirit Stone - the abode upkeep currency. */
        SPIRIT_STONE,
        /** A cultivation manual. */
        MANUAL
    }

    /** How a refinement attempt resolved. */
    public enum RefinementOutcome {
        /** The weapon took the element and reached its target tier. */
        SUCCESS,
        /** The attempt failed and the weapon was destroyed. */
        DESTROYED,
        /** The attempt failed and the weapon lost a tier. */
        DEMOTED,
        /** The attempt failed harmlessly - only the Qi was lost. */
        FAILED
    }

    // --- Post-events ---

    /** A cultivation drop landed in a player's inventory and was announced. Never fires when the roll missed or the item didn't fit. */
    public record LootDropEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                @Nonnull LootType type, @Nonnull String itemId) {}

    /** A manual was read and its teaching applied. */
    public record ManualReadEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                  @Nullable String techniqueId, @Nullable String skillNodeId) {}

    /** A spirit pill was consumed and its effect applied. {@code effect} is the interaction's configured effect id. */
    public record PillConsumeEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull String effect) {}

    /** A cultivation core was absorbed. {@code qi} is what was actually banked, meditation bonus included. */
    public record SpiritCoreConsumeEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, float qi) {}

    /** A refinement ritual began; the Qi is already spent and the cultivator seated. */
    public record RefinementStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                       @Nonnull DaoElement element, int targetTier, float qiCost) {}

    /** A refinement ritual resolved. {@code stack} is the weapon as it stands afterward, or null when it was destroyed. */
    public record RefinementCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                          @Nonnull DaoElement element, int targetTier,
                                          @Nonnull RefinementOutcome outcome, @Nullable ItemStack stack) {}

    // --- Pre-events ---

    /** A cultivation drop is about to be handed over. Cancel to deny it; {@link #setItemId} to substitute a different item entirely. */
    public static final class PreLootDropEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final LootType type;
        private String itemId;

        public PreLootDropEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                @Nonnull LootType type, @Nonnull String itemId){
            this.ref = ref;
            this.player = player;
            this.type = type;
            this.itemId = itemId;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public LootType type(){ return this.type; }
        @Nonnull public String itemId(){ return this.itemId; }
        public void setItemId(@Nonnull String itemId){ this.itemId = itemId; }
    }

    /** A manual is about to teach. Cancel to refuse it - the manual is consumed either way, matching how one for an already-known art is spent. */
    public static final class PreManualReadEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String techniqueId;
        private final String skillNodeId;

        public PreManualReadEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                  @Nullable String techniqueId, @Nullable String skillNodeId){
            this.ref = ref;
            this.player = player;
            this.techniqueId = techniqueId;
            this.skillNodeId = skillNodeId;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        /** Set when this manual teaches a technique. */
        @Nullable public String techniqueId(){ return this.techniqueId; }
        /** Set when this manual teaches a skill tree node. */
        @Nullable public String skillNodeId(){ return this.skillNodeId; }
    }

    /** A spirit pill is about to take effect. Cancel to refuse it (the pill is not consumed). */
    public static final class PrePillConsumeEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String effect;

        public PrePillConsumeEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull String effect){
            this.ref = ref;
            this.player = player;
            this.effect = effect;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        /** The interaction's configured effect id, e.g. the Qi-gain or ritual-speed buff. */
        @Nonnull public String effect(){ return this.effect; }
    }

    /** A cultivation core is about to be absorbed. Cancel to refuse it (the core is not consumed); {@link #setQi} to re-value it. */
    public static final class PreSpiritCoreConsumeEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final boolean meditating;
        private float qi;

        public PreSpiritCoreConsumeEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, float qi, boolean meditating){
            this.ref = ref;
            this.player = player;
            this.qi = qi;
            this.meditating = meditating;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        /** Qi the core is worth, meditation bonus already folded in. */
        public float qi(){ return this.qi; }
        public void setQi(float qi){ this.qi = qi; }
        /** Whether they were already meditating, which is what earned the bonus. */
        public boolean meditating(){ return this.meditating; }
    }

    /** A refinement ritual is about to begin. Cancel to refuse it (no Qi is spent); {@link #setQiCost} to re-price it. */
    public static final class PreRefinementStartEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final DaoElement element;
        private final int targetTier;
        private float qiCost;

        public PreRefinementStartEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                       @Nonnull DaoElement element, int targetTier, float qiCost){
            this.ref = ref;
            this.player = player;
            this.element = element;
            this.targetTier = targetTier;
            this.qiCost = qiCost;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public DaoElement element(){ return this.element; }
        /** The tier the weapon is reaching for. */
        public int targetTier(){ return this.targetTier; }
        public float qiCost(){ return this.qiCost; }
        public void setQiCost(float qiCost){ this.qiCost = qiCost; }
    }

    /** A refinement ritual is about to resolve. Cancel to abandon it silently (the weapon is untouched; the up-front Qi stays spent); {@link #setSuccessChance} to re-weight the roll - 1 guarantees it, 0 dooms it. */
    public static final class PreRefinementCompleteEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final DaoElement element;
        private final int targetTier;
        private float successChance;

        public PreRefinementCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                          @Nonnull DaoElement element, int targetTier, float successChance){
            this.ref = ref;
            this.player = player;
            this.element = element;
            this.targetTier = targetTier;
            this.successChance = successChance;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public DaoElement element(){ return this.element; }
        public int targetTier(){ return this.targetTier; }
        /** Odds the temper takes, 0-1. */
        public float successChance(){ return this.successChance; }
        public void setSuccessChance(float successChance){ this.successChance = successChance; }
    }

    // --- Listener registration ---

    private static final List<Consumer<LootDropEvent>> LOOT_DROP = EventBus.newListenerList();
    private static final List<Consumer<PreLootDropEvent>> PRE_LOOT_DROP = EventBus.newListenerList();
    private static final List<Consumer<ManualReadEvent>> MANUAL_READ = EventBus.newListenerList();
    private static final List<Consumer<PreManualReadEvent>> PRE_MANUAL_READ = EventBus.newListenerList();
    private static final List<Consumer<PillConsumeEvent>> PILL_CONSUME = EventBus.newListenerList();
    private static final List<Consumer<PrePillConsumeEvent>> PRE_PILL_CONSUME = EventBus.newListenerList();
    private static final List<Consumer<SpiritCoreConsumeEvent>> CORE_CONSUME = EventBus.newListenerList();
    private static final List<Consumer<PreSpiritCoreConsumeEvent>> PRE_CORE_CONSUME = EventBus.newListenerList();
    private static final List<Consumer<RefinementStartEvent>> REFINEMENT_START = EventBus.newListenerList();
    private static final List<Consumer<PreRefinementStartEvent>> PRE_REFINEMENT_START = EventBus.newListenerList();
    private static final List<Consumer<RefinementCompleteEvent>> REFINEMENT_COMPLETE = EventBus.newListenerList();
    private static final List<Consumer<PreRefinementCompleteEvent>> PRE_REFINEMENT_COMPLETE = EventBus.newListenerList();

    public static void onLootDrop(@Nonnull Consumer<LootDropEvent> listener){ LOOT_DROP.add(listener); }
    public static void onPreLootDrop(@Nonnull Consumer<PreLootDropEvent> listener){ PRE_LOOT_DROP.add(listener); }
    public static void onManualRead(@Nonnull Consumer<ManualReadEvent> listener){ MANUAL_READ.add(listener); }
    public static void onPreManualRead(@Nonnull Consumer<PreManualReadEvent> listener){ PRE_MANUAL_READ.add(listener); }
    public static void onPillConsume(@Nonnull Consumer<PillConsumeEvent> listener){ PILL_CONSUME.add(listener); }
    public static void onPrePillConsume(@Nonnull Consumer<PrePillConsumeEvent> listener){ PRE_PILL_CONSUME.add(listener); }
    public static void onSpiritCoreConsume(@Nonnull Consumer<SpiritCoreConsumeEvent> listener){ CORE_CONSUME.add(listener); }
    public static void onPreSpiritCoreConsume(@Nonnull Consumer<PreSpiritCoreConsumeEvent> listener){ PRE_CORE_CONSUME.add(listener); }
    public static void onRefinementStart(@Nonnull Consumer<RefinementStartEvent> listener){ REFINEMENT_START.add(listener); }
    public static void onPreRefinementStart(@Nonnull Consumer<PreRefinementStartEvent> listener){ PRE_REFINEMENT_START.add(listener); }
    public static void onRefinementComplete(@Nonnull Consumer<RefinementCompleteEvent> listener){ REFINEMENT_COMPLETE.add(listener); }
    public static void onPreRefinementComplete(@Nonnull Consumer<PreRefinementCompleteEvent> listener){ PRE_REFINEMENT_COMPLETE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireLootDrop(@Nonnull LootDropEvent event){ EventBus.dispatch(LOOT_DROP, event, "LootDropEvent"); }
    public static boolean firePreLootDrop(@Nonnull PreLootDropEvent event){ return EventBus.fire(PRE_LOOT_DROP, event, "PreLootDropEvent"); }
    public static void fireManualRead(@Nonnull ManualReadEvent event){ EventBus.dispatch(MANUAL_READ, event, "ManualReadEvent"); }
    public static boolean firePreManualRead(@Nonnull PreManualReadEvent event){ return EventBus.fire(PRE_MANUAL_READ, event, "PreManualReadEvent"); }
    public static void firePillConsume(@Nonnull PillConsumeEvent event){ EventBus.dispatch(PILL_CONSUME, event, "PillConsumeEvent"); }
    public static boolean firePrePillConsume(@Nonnull PrePillConsumeEvent event){ return EventBus.fire(PRE_PILL_CONSUME, event, "PrePillConsumeEvent"); }
    public static void fireSpiritCoreConsume(@Nonnull SpiritCoreConsumeEvent event){ EventBus.dispatch(CORE_CONSUME, event, "SpiritCoreConsumeEvent"); }
    public static boolean firePreSpiritCoreConsume(@Nonnull PreSpiritCoreConsumeEvent event){ return EventBus.fire(PRE_CORE_CONSUME, event, "PreSpiritCoreConsumeEvent"); }
    public static void fireRefinementStart(@Nonnull RefinementStartEvent event){ EventBus.dispatch(REFINEMENT_START, event, "RefinementStartEvent"); }
    public static boolean firePreRefinementStart(@Nonnull PreRefinementStartEvent event){ return EventBus.fire(PRE_REFINEMENT_START, event, "PreRefinementStartEvent"); }
    public static void fireRefinementComplete(@Nonnull RefinementCompleteEvent event){ EventBus.dispatch(REFINEMENT_COMPLETE, event, "RefinementCompleteEvent"); }
    public static boolean firePreRefinementComplete(@Nonnull PreRefinementCompleteEvent event){ return EventBus.fire(PRE_REFINEMENT_COMPLETE, event, "PreRefinementCompleteEvent"); }
}
