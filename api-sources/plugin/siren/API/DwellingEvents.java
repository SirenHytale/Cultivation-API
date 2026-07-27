package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Dwelling.Dwelling;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cave Abode (洞府) events - claiming a dwelling, its Spirit Spring, upkeep,
 * and closed-door seclusion. See {@link CultivationEvents} for the conventions
 * every {@code *Events} class in this package shares.
 *
 * <p>Sect hall springs are created and moved automatically to follow their
 * sect's hall; that housekeeping deliberately fires nothing. Listen for
 * {@link SectEvents}' hall claim/capture events instead - they are what causes
 * it.</p>
 */
public final class DwellingEvents {
    private DwellingEvents(){}

    // --- Post-events ---

    /** A cultivator claimed an abode, or moved an existing one. {@code moved} distinguishes the two; a move keeps the banked spring and paid upkeep. */
    public record DwellingClaimEvent(@Nonnull UUID owner, @Nonnull Dwelling dwelling, boolean moved) {}

    /** A cultivator gave up their abode; whatever the spring held went with it. */
    public record DwellingAbandonEvent(@Nonnull UUID owner, @Nonnull Dwelling dwelling) {}

    /** A personal abode was reclaimed by the world for unpaid upkeep, past its grace period. */
    public record DwellingLapseEvent(@Nonnull Dwelling dwelling) {}

    /** A Spirit Spring was emptied. {@code amount} is the Qi handed over - the caller credits it. */
    public record SpringCollectEvent(@Nonnull Dwelling dwelling, float amount) {}

    /** Upkeep was paid into an abode. {@code hoursGranted} is what was actually banked, which is less than what was offered once the cap is hit. */
    public record UpkeepDepositEvent(@Nonnull Dwelling dwelling, @Nonnull String itemId, int quantity, float hoursGranted) {}

    /** A cultivator emerged from closed-door seclusion and was paid for their absence. {@code hours} is the capped absence; {@code qi} is what was actually credited. */
    public record SeclusionSettleEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                       @Nonnull Dwelling dwelling, float hours, float qi) {}

    // --- Pre-events ---

    /** An abode is about to be claimed or moved. Cancel to refuse it (reported as warded ground); {@link #setRadiusChunks} to change how far it reaches. */
    public static final class PreDwellingClaimEvent extends CancellableEvent {
        private final UUID owner;
        private final String world;
        private final int chunkX;
        private final int chunkZ;
        private final int veinTier;
        private final boolean moved;
        private int radiusChunks;

        public PreDwellingClaimEvent(@Nonnull UUID owner, @Nonnull String world, int chunkX, int chunkZ,
                                     int veinTier, boolean moved, int radiusChunks){
            this.owner = owner;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.veinTier = veinTier;
            this.moved = moved;
            this.radiusChunks = radiusChunks;
        }

        @Nonnull public UUID owner(){ return this.owner; }
        @Nonnull public String world(){ return this.world; }
        public int chunkX(){ return this.chunkX; }
        public int chunkZ(){ return this.chunkZ; }
        public int veinTier(){ return this.veinTier; }
        /** True when this relocates an existing abode rather than founding one. */
        public boolean moved(){ return this.moved; }
        public int radiusChunks(){ return this.radiusChunks; }
        public void setRadiusChunks(int radiusChunks){ this.radiusChunks = radiusChunks; }
    }

    /** An abode is about to be given up. Cancel to keep it standing. */
    public static final class PreDwellingAbandonEvent extends CancellableEvent {
        private final UUID owner;
        private final Dwelling dwelling;

        public PreDwellingAbandonEvent(@Nonnull UUID owner, @Nonnull Dwelling dwelling){
            this.owner = owner;
            this.dwelling = dwelling;
        }

        @Nonnull public UUID owner(){ return this.owner; }
        @Nonnull public Dwelling dwelling(){ return this.dwelling; }
    }

    /** An abode is about to be reclaimed for unpaid upkeep. Cancel to reprieve it - it survives until the next sweep re-tests it, so cancel from a listener that keeps deciding, not a one-off. */
    public static final class PreDwellingLapseEvent extends CancellableEvent {
        private final Dwelling dwelling;

        public PreDwellingLapseEvent(@Nonnull Dwelling dwelling){
            this.dwelling = dwelling;
        }

        @Nonnull public Dwelling dwelling(){ return this.dwelling; }
    }

    /** A Spirit Spring is about to be emptied. Cancel to leave it full; {@link #setAmount} to change what the collector walks away with (the spring is emptied regardless). */
    public static final class PreSpringCollectEvent extends CancellableEvent {
        private final Dwelling dwelling;
        private float amount;

        public PreSpringCollectEvent(@Nonnull Dwelling dwelling, float amount){
            this.dwelling = dwelling;
            this.amount = amount;
        }

        @Nonnull public Dwelling dwelling(){ return this.dwelling; }
        /** Qi the spring will hand over. */
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
    }

    /** Upkeep is about to be paid. Cancel to refuse the payment (reported as nothing banked); {@link #setHours} to change how much time the offering buys. */
    public static final class PreUpkeepDepositEvent extends CancellableEvent {
        private final Dwelling dwelling;
        private final String itemId;
        private final int quantity;
        private float hours;

        public PreUpkeepDepositEvent(@Nonnull Dwelling dwelling, @Nonnull String itemId, int quantity, float hours){
            this.dwelling = dwelling;
            this.itemId = itemId;
            this.quantity = quantity;
            this.hours = hours;
        }

        @Nonnull public Dwelling dwelling(){ return this.dwelling; }
        @Nonnull public String itemId(){ return this.itemId; }
        public int quantity(){ return this.quantity; }
        /** Upkeep hours the whole offering is worth, before the banked-hours cap is applied. */
        public float hours(){ return this.hours; }
        public void setHours(float hours){ this.hours = hours; }
    }

    /** A seclusion retreat is about to pay out. Cancel to forfeit it (reported to the player as a dry spring); {@link #setQi} to re-scale the reward. */
    public static final class PreSeclusionSettleEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final Dwelling dwelling;
        private final float hours;
        private float qi;

        public PreSeclusionSettleEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                       @Nonnull Dwelling dwelling, float hours, float qi){
            this.ref = ref;
            this.player = player;
            this.dwelling = dwelling;
            this.hours = hours;
            this.qi = qi;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public Dwelling dwelling(){ return this.dwelling; }
        /** Their absence in hours, already capped by Seclusion-Max-Hours. */
        public float hours(){ return this.hours; }
        /** Qi the retreat will pay, already limited to what the spring could cover when Seclusion-Drains-Spring is on. */
        public float qi(){ return this.qi; }
        public void setQi(float qi){ this.qi = qi; }
    }

    // --- Listener registration ---

    private static final List<Consumer<DwellingClaimEvent>> CLAIM = EventBus.newListenerList();
    private static final List<Consumer<PreDwellingClaimEvent>> PRE_CLAIM = EventBus.newListenerList();
    private static final List<Consumer<DwellingAbandonEvent>> ABANDON = EventBus.newListenerList();
    private static final List<Consumer<PreDwellingAbandonEvent>> PRE_ABANDON = EventBus.newListenerList();
    private static final List<Consumer<DwellingLapseEvent>> LAPSE = EventBus.newListenerList();
    private static final List<Consumer<PreDwellingLapseEvent>> PRE_LAPSE = EventBus.newListenerList();
    private static final List<Consumer<SpringCollectEvent>> SPRING_COLLECT = EventBus.newListenerList();
    private static final List<Consumer<PreSpringCollectEvent>> PRE_SPRING_COLLECT = EventBus.newListenerList();
    private static final List<Consumer<UpkeepDepositEvent>> UPKEEP_DEPOSIT = EventBus.newListenerList();
    private static final List<Consumer<PreUpkeepDepositEvent>> PRE_UPKEEP_DEPOSIT = EventBus.newListenerList();
    private static final List<Consumer<SeclusionSettleEvent>> SECLUSION_SETTLE = EventBus.newListenerList();
    private static final List<Consumer<PreSeclusionSettleEvent>> PRE_SECLUSION_SETTLE = EventBus.newListenerList();

    public static void onDwellingClaim(@Nonnull Consumer<DwellingClaimEvent> listener){ CLAIM.add(listener); }
    public static void onPreDwellingClaim(@Nonnull Consumer<PreDwellingClaimEvent> listener){ PRE_CLAIM.add(listener); }
    public static void onDwellingAbandon(@Nonnull Consumer<DwellingAbandonEvent> listener){ ABANDON.add(listener); }
    public static void onPreDwellingAbandon(@Nonnull Consumer<PreDwellingAbandonEvent> listener){ PRE_ABANDON.add(listener); }
    public static void onDwellingLapse(@Nonnull Consumer<DwellingLapseEvent> listener){ LAPSE.add(listener); }
    public static void onPreDwellingLapse(@Nonnull Consumer<PreDwellingLapseEvent> listener){ PRE_LAPSE.add(listener); }
    public static void onSpringCollect(@Nonnull Consumer<SpringCollectEvent> listener){ SPRING_COLLECT.add(listener); }
    public static void onPreSpringCollect(@Nonnull Consumer<PreSpringCollectEvent> listener){ PRE_SPRING_COLLECT.add(listener); }
    public static void onUpkeepDeposit(@Nonnull Consumer<UpkeepDepositEvent> listener){ UPKEEP_DEPOSIT.add(listener); }
    public static void onPreUpkeepDeposit(@Nonnull Consumer<PreUpkeepDepositEvent> listener){ PRE_UPKEEP_DEPOSIT.add(listener); }
    public static void onSeclusionSettle(@Nonnull Consumer<SeclusionSettleEvent> listener){ SECLUSION_SETTLE.add(listener); }
    public static void onPreSeclusionSettle(@Nonnull Consumer<PreSeclusionSettleEvent> listener){ PRE_SECLUSION_SETTLE.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireDwellingClaim(@Nonnull DwellingClaimEvent event){ EventBus.dispatch(CLAIM, event, "DwellingClaimEvent"); }
    public static boolean firePreDwellingClaim(@Nonnull PreDwellingClaimEvent event){ return EventBus.fire(PRE_CLAIM, event, "PreDwellingClaimEvent"); }
    public static void fireDwellingAbandon(@Nonnull DwellingAbandonEvent event){ EventBus.dispatch(ABANDON, event, "DwellingAbandonEvent"); }
    public static boolean firePreDwellingAbandon(@Nonnull PreDwellingAbandonEvent event){ return EventBus.fire(PRE_ABANDON, event, "PreDwellingAbandonEvent"); }
    public static void fireDwellingLapse(@Nonnull DwellingLapseEvent event){ EventBus.dispatch(LAPSE, event, "DwellingLapseEvent"); }
    public static boolean firePreDwellingLapse(@Nonnull PreDwellingLapseEvent event){ return EventBus.fire(PRE_LAPSE, event, "PreDwellingLapseEvent"); }
    public static void fireSpringCollect(@Nonnull SpringCollectEvent event){ EventBus.dispatch(SPRING_COLLECT, event, "SpringCollectEvent"); }
    public static boolean firePreSpringCollect(@Nonnull PreSpringCollectEvent event){ return EventBus.fire(PRE_SPRING_COLLECT, event, "PreSpringCollectEvent"); }
    public static void fireUpkeepDeposit(@Nonnull UpkeepDepositEvent event){ EventBus.dispatch(UPKEEP_DEPOSIT, event, "UpkeepDepositEvent"); }
    public static boolean firePreUpkeepDeposit(@Nonnull PreUpkeepDepositEvent event){ return EventBus.fire(PRE_UPKEEP_DEPOSIT, event, "PreUpkeepDepositEvent"); }
    public static void fireSeclusionSettle(@Nonnull SeclusionSettleEvent event){ EventBus.dispatch(SECLUSION_SETTLE, event, "SeclusionSettleEvent"); }
    public static boolean firePreSeclusionSettle(@Nonnull PreSeclusionSettleEvent event){ return EventBus.fire(PRE_SECLUSION_SETTLE, event, "PreSeclusionSettleEvent"); }
}
