package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Components.DaoComponent;
import plugin.siren.ECS.Dao.CultivationPath;
import plugin.siren.ECS.Dao.DaoElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dao events - choosing an element, elemental drift, the Yin-Yang balance, the
 * moral path it decides, and the karma a cultivator's kills accrue. See
 * {@link CultivationEvents} for the conventions every {@code *Events} class in
 * this package shares.
 *
 * <p>Alignment and karma move on nearly every meditation tick and every kill,
 * so their events fire often. Keep those listeners cheap, and prefer the path
 * and drift events when you only care about the outcome.</p>
 */
public final class DaoEvents {
    private DaoEvents(){}

    /** Why a cultivator's element changed. */
    public enum ElementChangeReason {
        /** They picked it themselves - the free first choice, or a paid switch. */
        CHOSEN,
        /** Their deeds overwhelmed the chosen path and it converted on its own. */
        DRIFT
    }

    // --- Post-events ---

    /** A cultivator's elemental dao changed. {@code oldElement} is null on their very first choice. */
    public record DaoElementChangeEvent(@Nullable Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull DaoComponent dao,
                                        @Nullable DaoElement oldElement, @Nonnull DaoElement newElement,
                                        @Nonnull ElementChangeReason reason, float qiCost) {}

    /** Deed affinity was added toward an element - the pressure that eventually causes drift. */
    public record DaoAffinityGainEvent(@Nullable PlayerRef player, @Nonnull DaoComponent dao, @Nonnull DaoElement element, float amount) {}

    /** A cultivator was warned their dao is drifting toward another element. Fires once per newly-threatening element. */
    public record DaoDriftWarningEvent(@Nullable PlayerRef player, @Nonnull DaoComponent dao,
                                       @Nonnull DaoElement chosenElement, @Nonnull DaoElement driftingTo) {}

    /** A cultivator's Yin-Yang balance moved. {@code yin}/{@code yang} are the amounts actually added after race bias split the shift. */
    public record AlignmentShiftEvent(@Nonnull DaoComponent dao, float yin, float yang) {}

    /** A cultivator's moral path changed (and was announced to them). */
    public record PathChangeEvent(@Nullable PlayerRef player, @Nonnull DaoComponent dao,
                                  @Nonnull CultivationPath oldPath, @Nonnull CultivationPath newPath) {}

    /** Karma was charged for a kill. {@code total} is the ledger after the charge and the Karma-Max cap. */
    public record KarmaGainEvent(@Nullable PlayerRef player, @Nonnull DaoComponent dao, float amount, float total, boolean farmedKill) {}

    /** Karma was worked off - by enduring a tribulation strike, or by the wall-clock decay of simply not killing anyone. */
    public record KarmaClearedEvent(@Nonnull DaoComponent dao, float amount, float total, boolean fromTribulation) {}

    /** A Devil-path cultivator harvested banked Qi from slaying another player. */
    public record DevilHarvestEvent(@Nonnull Ref<EntityStore> killer, @Nonnull PlayerRef killerPlayer, float qi) {}

    // --- Pre-events ---

    /** A cultivator is about to take (or switch to) an element. Cancel to refuse it (reported as an unchanged dao); {@link #setQiCost} to re-price the switch - it is charged after this, so a listener can make switching free or ruinous. */
    public static final class PreDaoElementChangeEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final DaoComponent dao;
        private final DaoElement oldElement;
        private final ElementChangeReason reason;
        private DaoElement newElement;
        private float qiCost;

        public PreDaoElementChangeEvent(@Nullable Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull DaoComponent dao,
                                        @Nullable DaoElement oldElement, @Nonnull DaoElement newElement,
                                        @Nonnull ElementChangeReason reason, float qiCost){
            this.ref = ref;
            this.player = player;
            this.dao = dao;
            this.oldElement = oldElement;
            this.newElement = newElement;
            this.reason = reason;
            this.qiCost = qiCost;
        }

        @Nullable public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComponent dao(){ return this.dao; }
        /** Null on their very first choice. */
        @Nullable public DaoElement oldElement(){ return this.oldElement; }
        @Nonnull public DaoElement newElement(){ return this.newElement; }
        public void setNewElement(@Nonnull DaoElement newElement){ this.newElement = newElement; }
        @Nonnull public ElementChangeReason reason(){ return this.reason; }
        /** Banked Qi the switch will cost. Always 0 for a first choice and for DRIFT. */
        public float qiCost(){ return this.qiCost; }
        public void setQiCost(float qiCost){ this.qiCost = qiCost; }
    }

    /** Deed affinity is about to be added. Cancel to deny it; {@link #setAmount} to re-scale how fast this element pulls at them. */
    public static final class PreDaoAffinityGainEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComponent dao;
        private final DaoElement element;
        private float amount;

        public PreDaoAffinityGainEvent(@Nullable PlayerRef player, @Nonnull DaoComponent dao, @Nonnull DaoElement element, float amount){
            this.player = player;
            this.dao = dao;
            this.element = element;
            this.amount = amount;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComponent dao(){ return this.dao; }
        @Nonnull public DaoElement element(){ return this.element; }
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
    }

    /** A Yin-Yang shift is about to be applied. Cancel to deny it; {@link #setAmount} to re-scale. The race-bias split happens after this. */
    public static final class PreAlignmentShiftEvent extends CancellableEvent {
        private final DaoComponent dao;
        private final boolean towardYin;
        private float amount;

        public PreAlignmentShiftEvent(@Nonnull DaoComponent dao, float amount, boolean towardYin){
            this.dao = dao;
            this.amount = amount;
            this.towardYin = towardYin;
        }

        @Nonnull public DaoComponent dao(){ return this.dao; }
        /** The raw shift, before the player's race bias splits it between Yin and Yang. */
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
        public boolean towardYin(){ return this.towardYin; }
    }

    /** A cultivator's moral path is about to change. Cancel to leave them on their current path - the underlying balance is untouched, so this only suppresses the reclassification. */
    public static final class PrePathChangeEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComponent dao;
        private final CultivationPath oldPath;
        private final CultivationPath newPath;

        public PrePathChangeEvent(@Nullable PlayerRef player, @Nonnull DaoComponent dao,
                                  @Nonnull CultivationPath oldPath, @Nonnull CultivationPath newPath){
            this.player = player;
            this.dao = dao;
            this.oldPath = oldPath;
            this.newPath = newPath;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComponent dao(){ return this.dao; }
        @Nonnull public CultivationPath oldPath(){ return this.oldPath; }
        @Nonnull public CultivationPath newPath(){ return this.newPath; }
    }

    /** Karma is about to be charged for a kill. Cancel to leave the ledger clean; {@link #setAmount} to re-weigh what this life cost. */
    public static final class PreKarmaGainEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComponent dao;
        private final boolean farmedKill;
        private float amount;

        public PreKarmaGainEvent(@Nullable PlayerRef player, @Nonnull DaoComponent dao, float amount, boolean farmedKill){
            this.player = player;
            this.dao = dao;
            this.amount = amount;
            this.farmedKill = farmedKill;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComponent dao(){ return this.dao; }
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
        /** True when this was a repeat kill inside the anti-farm window - worth no Qi, but more karma. */
        public boolean farmedKill(){ return this.farmedKill; }
    }

    /** A Devil-path cultivator is about to harvest Qi from a slain player. Cancel to deny the harvest; {@link #setQi} to re-scale it. */
    public static final class PreDevilHarvestEvent extends CancellableEvent {
        private final Ref<EntityStore> killer;
        private final PlayerRef killerPlayer;
        private float qi;

        public PreDevilHarvestEvent(@Nonnull Ref<EntityStore> killer, @Nonnull PlayerRef killerPlayer, float qi){
            this.killer = killer;
            this.killerPlayer = killerPlayer;
            this.qi = qi;
        }

        @Nonnull public Ref<EntityStore> killer(){ return this.killer; }
        @Nonnull public PlayerRef killerPlayer(){ return this.killerPlayer; }
        public float qi(){ return this.qi; }
        public void setQi(float qi){ this.qi = qi; }
    }

    // --- Listener registration ---

    private static final List<Consumer<DaoElementChangeEvent>> ELEMENT_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreDaoElementChangeEvent>> PRE_ELEMENT_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<DaoAffinityGainEvent>> AFFINITY_GAIN = EventBus.newListenerList();
    private static final List<Consumer<PreDaoAffinityGainEvent>> PRE_AFFINITY_GAIN = EventBus.newListenerList();
    private static final List<Consumer<DaoDriftWarningEvent>> DRIFT_WARNING = EventBus.newListenerList();
    private static final List<Consumer<AlignmentShiftEvent>> ALIGNMENT_SHIFT = EventBus.newListenerList();
    private static final List<Consumer<PreAlignmentShiftEvent>> PRE_ALIGNMENT_SHIFT = EventBus.newListenerList();
    private static final List<Consumer<PathChangeEvent>> PATH_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PrePathChangeEvent>> PRE_PATH_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<KarmaGainEvent>> KARMA_GAIN = EventBus.newListenerList();
    private static final List<Consumer<PreKarmaGainEvent>> PRE_KARMA_GAIN = EventBus.newListenerList();
    private static final List<Consumer<KarmaClearedEvent>> KARMA_CLEARED = EventBus.newListenerList();
    private static final List<Consumer<DevilHarvestEvent>> DEVIL_HARVEST = EventBus.newListenerList();
    private static final List<Consumer<PreDevilHarvestEvent>> PRE_DEVIL_HARVEST = EventBus.newListenerList();

    public static void onDaoElementChange(@Nonnull Consumer<DaoElementChangeEvent> listener){ ELEMENT_CHANGE.add(listener); }
    public static void onPreDaoElementChange(@Nonnull Consumer<PreDaoElementChangeEvent> listener){ PRE_ELEMENT_CHANGE.add(listener); }
    public static void onDaoAffinityGain(@Nonnull Consumer<DaoAffinityGainEvent> listener){ AFFINITY_GAIN.add(listener); }
    public static void onPreDaoAffinityGain(@Nonnull Consumer<PreDaoAffinityGainEvent> listener){ PRE_AFFINITY_GAIN.add(listener); }
    public static void onDaoDriftWarning(@Nonnull Consumer<DaoDriftWarningEvent> listener){ DRIFT_WARNING.add(listener); }
    public static void onAlignmentShift(@Nonnull Consumer<AlignmentShiftEvent> listener){ ALIGNMENT_SHIFT.add(listener); }
    public static void onPreAlignmentShift(@Nonnull Consumer<PreAlignmentShiftEvent> listener){ PRE_ALIGNMENT_SHIFT.add(listener); }
    public static void onPathChange(@Nonnull Consumer<PathChangeEvent> listener){ PATH_CHANGE.add(listener); }
    public static void onPrePathChange(@Nonnull Consumer<PrePathChangeEvent> listener){ PRE_PATH_CHANGE.add(listener); }
    public static void onKarmaGain(@Nonnull Consumer<KarmaGainEvent> listener){ KARMA_GAIN.add(listener); }
    public static void onPreKarmaGain(@Nonnull Consumer<PreKarmaGainEvent> listener){ PRE_KARMA_GAIN.add(listener); }
    public static void onKarmaCleared(@Nonnull Consumer<KarmaClearedEvent> listener){ KARMA_CLEARED.add(listener); }
    public static void onDevilHarvest(@Nonnull Consumer<DevilHarvestEvent> listener){ DEVIL_HARVEST.add(listener); }
    public static void onPreDevilHarvest(@Nonnull Consumer<PreDevilHarvestEvent> listener){ PRE_DEVIL_HARVEST.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireDaoElementChange(@Nonnull DaoElementChangeEvent event){ EventBus.dispatch(ELEMENT_CHANGE, event, "DaoElementChangeEvent"); }
    public static boolean firePreDaoElementChange(@Nonnull PreDaoElementChangeEvent event){ return EventBus.fire(PRE_ELEMENT_CHANGE, event, "PreDaoElementChangeEvent"); }
    public static void fireDaoAffinityGain(@Nonnull DaoAffinityGainEvent event){ EventBus.dispatch(AFFINITY_GAIN, event, "DaoAffinityGainEvent"); }
    public static boolean firePreDaoAffinityGain(@Nonnull PreDaoAffinityGainEvent event){ return EventBus.fire(PRE_AFFINITY_GAIN, event, "PreDaoAffinityGainEvent"); }
    public static void fireDaoDriftWarning(@Nonnull DaoDriftWarningEvent event){ EventBus.dispatch(DRIFT_WARNING, event, "DaoDriftWarningEvent"); }
    public static void fireAlignmentShift(@Nonnull AlignmentShiftEvent event){ EventBus.dispatch(ALIGNMENT_SHIFT, event, "AlignmentShiftEvent"); }
    public static boolean firePreAlignmentShift(@Nonnull PreAlignmentShiftEvent event){ return EventBus.fire(PRE_ALIGNMENT_SHIFT, event, "PreAlignmentShiftEvent"); }
    public static void firePathChange(@Nonnull PathChangeEvent event){ EventBus.dispatch(PATH_CHANGE, event, "PathChangeEvent"); }
    public static boolean firePrePathChange(@Nonnull PrePathChangeEvent event){ return EventBus.fire(PRE_PATH_CHANGE, event, "PrePathChangeEvent"); }
    public static void fireKarmaGain(@Nonnull KarmaGainEvent event){ EventBus.dispatch(KARMA_GAIN, event, "KarmaGainEvent"); }
    public static boolean firePreKarmaGain(@Nonnull PreKarmaGainEvent event){ return EventBus.fire(PRE_KARMA_GAIN, event, "PreKarmaGainEvent"); }
    public static void fireKarmaCleared(@Nonnull KarmaClearedEvent event){ EventBus.dispatch(KARMA_CLEARED, event, "KarmaClearedEvent"); }
    public static void fireDevilHarvest(@Nonnull DevilHarvestEvent event){ EventBus.dispatch(DEVIL_HARVEST, event, "DevilHarvestEvent"); }
    public static boolean firePreDevilHarvest(@Nonnull PreDevilHarvestEvent event){ return EventBus.fire(PRE_DEVIL_HARVEST, event, "PreDevilHarvestEvent"); }
}
