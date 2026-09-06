package plugin.siren.API;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.ECS.Components.DaoComprehensionComponent;
import plugin.siren.ECS.Dao.HeavenlyDaoRank;
import plugin.siren.ECS.Dao.PersonalDao;
import plugin.siren.Utils.Dao.DaoComprehensionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Events for the Dao layer added on top of the Elemental Dao - the Heavenly
 * Dao (天道) understanding track, the open Personal Dao registry
 * (Sword/Slaughter/Space and whatever a mod adds beside them), and Dao
 * Enlightenment (悟道). See {@link DaoEvents} for the Elemental Dao / Yin-Yang
 * events this does not replace or collide with, and {@link CultivationEvents}
 * for the conventions every {@code *Events} class in this package shares.
 *
 * <p>{@link HeavenlyDaoGainEvent} fires once per meditation second per
 * meditating player (plus once per tribulation bolt survived, advancement and
 * breakthrough), and {@link PersonalDaoComprehensionEvent} fires on every
 * qualifying kill and technique use - keep listeners on either cheap, the
 * same rate warning {@link DaoEvents} gives for alignment and karma.</p>
 */
public final class DaoComprehensionEvents {
    private DaoComprehensionEvents(){}

    // --- Post-events ---

    /** The Heavenly Dao track advanced. {@code amount} is what was actually applied (after any listener re-scaled it); {@code total} is the value after. */
    public record HeavenlyDaoGainEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension, float amount, float total) {}

    /** The player's HeavenlyDaoRank changed (and was announced to them). */
    public record HeavenlyDaoRankChangeEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension,
                                             @Nonnull HeavenlyDaoRank oldRank, @Nonnull HeavenlyDaoRank newRank) {}

    /** A Personal Dao's comprehension advanced. {@code amount} is what was actually applied; {@code total} is the value after. */
    public record PersonalDaoComprehensionEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension,
                                                @Nonnull PersonalDao dao, float amount, float total) {}

    /** A Personal Dao manifested for this player. */
    public record PersonalDaoManifestEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension, @Nonnull PersonalDao dao) {}

    /** A manifested Personal Dao was set aside. */
    public record PersonalDaoSetAsideEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension, @Nonnull PersonalDao dao) {}

    /** A Dao Enlightenment (悟道) fired. {@code subject} is whichever Heavenly/Personal Dao triggered it. */
    public record DaoEnlightenmentEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension,
                                        @Nonnull DaoComprehensionManager.Subject subject, float comprehensionGain, float qiGain) {}

    // --- Pre-events ---

    /** The Heavenly Dao track is about to advance. Cancel to refuse it; {@link #setAmount} to re-scale. */
    public static final class PreHeavenlyDaoGainEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComprehensionComponent comprehension;
        private float amount;

        public PreHeavenlyDaoGainEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension, float amount){
            this.player = player;
            this.comprehension = comprehension;
            this.amount = amount;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComprehensionComponent comprehension(){ return this.comprehension; }
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
    }

    /** The player's HeavenlyDaoRank is about to change. Cancel to leave them on their current rank - the underlying value is untouched, so this only suppresses the reclassification/announcement. */
    public static final class PreHeavenlyDaoRankChangeEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComprehensionComponent comprehension;
        private final HeavenlyDaoRank oldRank;
        private final HeavenlyDaoRank newRank;

        public PreHeavenlyDaoRankChangeEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension,
                                             @Nonnull HeavenlyDaoRank oldRank, @Nonnull HeavenlyDaoRank newRank){
            this.player = player;
            this.comprehension = comprehension;
            this.oldRank = oldRank;
            this.newRank = newRank;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComprehensionComponent comprehension(){ return this.comprehension; }
        @Nonnull public HeavenlyDaoRank oldRank(){ return this.oldRank; }
        @Nonnull public HeavenlyDaoRank newRank(){ return this.newRank; }
    }

    /** A Personal Dao's comprehension is about to advance. Cancel to refuse it; {@link #setAmount} to re-scale. */
    public static final class PrePersonalDaoComprehensionEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComprehensionComponent comprehension;
        private final PersonalDao dao;
        private float amount;

        public PrePersonalDaoComprehensionEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension,
                                                 @Nonnull PersonalDao dao, float amount){
            this.player = player;
            this.comprehension = comprehension;
            this.dao = dao;
            this.amount = amount;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComprehensionComponent comprehension(){ return this.comprehension; }
        @Nonnull public PersonalDao dao(){ return this.dao; }
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
    }

    /** A Personal Dao is about to manifest. Cancel to leave it comprehended but unmanifested. */
    public static final class PrePersonalDaoManifestEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComprehensionComponent comprehension;
        private final PersonalDao dao;

        public PrePersonalDaoManifestEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension, @Nonnull PersonalDao dao){
            this.player = player;
            this.comprehension = comprehension;
            this.dao = dao;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComprehensionComponent comprehension(){ return this.comprehension; }
        @Nonnull public PersonalDao dao(){ return this.dao; }
    }

    /**
     * A Dao Enlightenment is about to fire. Cancel to refuse it (as if the roll
     * never happened); {@link PreDaoEnlightenmentEvent#setComprehensionGain}/
     * {@link PreDaoEnlightenmentEvent#setQiGain} to re-scale the reward.
     *
     * <p><b>{@code setQiGain} can lower the Qi burst but cannot raise it past the
     * server's absolute cap.</b> {@code Dao-Enlightenment-Qi-Max-Base} x
     * {@code Dao-Enlightenment-Qi-Max-Growth-Per-Realm ^ realmIndex} is applied
     * both before this event is fired and again immediately after
     * {@code qiGain()} is read back, so it is a hard rail rather than a default -
     * a listener that sets 10,000,000 on a Qi Gathering cultivator still grants
     * the cap. This is deliberate: an enlightenment is the mod's largest one-shot
     * Qi reward and an unbounded one reads to a player as a bug, not a blessing.
     * A server that genuinely wants no ceiling sets {@code Dao-Enlightenment-Qi-Max-Base}
     * to 0, which is the operator's decision to make, not a listener's.</p>
     *
     * <p>{@code setComprehensionGain} is not capped this way - comprehension is
     * clamped to the subject's own {@code getMaxComprehension()} downstream.</p>
     */
    public static final class PreDaoEnlightenmentEvent extends CancellableEvent {
        private final PlayerRef player;
        private final DaoComprehensionComponent comprehension;
        private final DaoComprehensionManager.Subject subject;
        private float comprehensionGain;
        private float qiGain;

        public PreDaoEnlightenmentEvent(@Nullable PlayerRef player, @Nonnull DaoComprehensionComponent comprehension,
                                        @Nonnull DaoComprehensionManager.Subject subject, float comprehensionGain, float qiGain){
            this.player = player;
            this.comprehension = comprehension;
            this.subject = subject;
            this.comprehensionGain = comprehensionGain;
            this.qiGain = qiGain;
        }

        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public DaoComprehensionComponent comprehension(){ return this.comprehension; }
        @Nonnull public DaoComprehensionManager.Subject subject(){ return this.subject; }
        public float comprehensionGain(){ return this.comprehensionGain; }
        public void setComprehensionGain(float comprehensionGain){ this.comprehensionGain = comprehensionGain; }
        public float qiGain(){ return this.qiGain; }
        public void setQiGain(float qiGain){ this.qiGain = qiGain; }
    }

    // --- Listener registration ---

    private static final List<Consumer<HeavenlyDaoGainEvent>> HEAVENLY_GAIN = EventBus.newListenerList();
    private static final List<Consumer<PreHeavenlyDaoGainEvent>> PRE_HEAVENLY_GAIN = EventBus.newListenerList();
    private static final List<Consumer<HeavenlyDaoRankChangeEvent>> HEAVENLY_RANK_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreHeavenlyDaoRankChangeEvent>> PRE_HEAVENLY_RANK_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PersonalDaoComprehensionEvent>> PERSONAL_COMPREHENSION = EventBus.newListenerList();
    private static final List<Consumer<PrePersonalDaoComprehensionEvent>> PRE_PERSONAL_COMPREHENSION = EventBus.newListenerList();
    private static final List<Consumer<PersonalDaoManifestEvent>> PERSONAL_MANIFEST = EventBus.newListenerList();
    private static final List<Consumer<PrePersonalDaoManifestEvent>> PRE_PERSONAL_MANIFEST = EventBus.newListenerList();
    private static final List<Consumer<PersonalDaoSetAsideEvent>> PERSONAL_SET_ASIDE = EventBus.newListenerList();
    private static final List<Consumer<DaoEnlightenmentEvent>> ENLIGHTENMENT = EventBus.newListenerList();
    private static final List<Consumer<PreDaoEnlightenmentEvent>> PRE_ENLIGHTENMENT = EventBus.newListenerList();

    public static void onHeavenlyDaoGain(@Nonnull Consumer<HeavenlyDaoGainEvent> listener){ HEAVENLY_GAIN.add(listener); }
    public static void onPreHeavenlyDaoGain(@Nonnull Consumer<PreHeavenlyDaoGainEvent> listener){ PRE_HEAVENLY_GAIN.add(listener); }
    public static void onHeavenlyDaoRankChange(@Nonnull Consumer<HeavenlyDaoRankChangeEvent> listener){ HEAVENLY_RANK_CHANGE.add(listener); }
    public static void onPreHeavenlyDaoRankChange(@Nonnull Consumer<PreHeavenlyDaoRankChangeEvent> listener){ PRE_HEAVENLY_RANK_CHANGE.add(listener); }
    public static void onPersonalDaoComprehension(@Nonnull Consumer<PersonalDaoComprehensionEvent> listener){ PERSONAL_COMPREHENSION.add(listener); }
    public static void onPrePersonalDaoComprehension(@Nonnull Consumer<PrePersonalDaoComprehensionEvent> listener){ PRE_PERSONAL_COMPREHENSION.add(listener); }
    public static void onPersonalDaoManifest(@Nonnull Consumer<PersonalDaoManifestEvent> listener){ PERSONAL_MANIFEST.add(listener); }
    public static void onPrePersonalDaoManifest(@Nonnull Consumer<PrePersonalDaoManifestEvent> listener){ PRE_PERSONAL_MANIFEST.add(listener); }
    public static void onPersonalDaoSetAside(@Nonnull Consumer<PersonalDaoSetAsideEvent> listener){ PERSONAL_SET_ASIDE.add(listener); }
    public static void onDaoEnlightenment(@Nonnull Consumer<DaoEnlightenmentEvent> listener){ ENLIGHTENMENT.add(listener); }
    public static void onPreDaoEnlightenment(@Nonnull Consumer<PreDaoEnlightenmentEvent> listener){ PRE_ENLIGHTENMENT.add(listener); }

    // --- Internal dispatch (called by this mod's own systems/managers; not API) ---

    public static void fireHeavenlyDaoGain(@Nonnull HeavenlyDaoGainEvent event){ EventBus.dispatch(HEAVENLY_GAIN, event, "HeavenlyDaoGainEvent"); }
    public static boolean firePreHeavenlyDaoGain(@Nonnull PreHeavenlyDaoGainEvent event){ return EventBus.fire(PRE_HEAVENLY_GAIN, event, "PreHeavenlyDaoGainEvent"); }
    public static void fireHeavenlyDaoRankChange(@Nonnull HeavenlyDaoRankChangeEvent event){ EventBus.dispatch(HEAVENLY_RANK_CHANGE, event, "HeavenlyDaoRankChangeEvent"); }
    public static boolean firePreHeavenlyDaoRankChange(@Nonnull PreHeavenlyDaoRankChangeEvent event){ return EventBus.fire(PRE_HEAVENLY_RANK_CHANGE, event, "PreHeavenlyDaoRankChangeEvent"); }
    public static void firePersonalDaoComprehension(@Nonnull PersonalDaoComprehensionEvent event){ EventBus.dispatch(PERSONAL_COMPREHENSION, event, "PersonalDaoComprehensionEvent"); }
    public static boolean firePrePersonalDaoComprehension(@Nonnull PrePersonalDaoComprehensionEvent event){ return EventBus.fire(PRE_PERSONAL_COMPREHENSION, event, "PrePersonalDaoComprehensionEvent"); }
    public static void firePersonalDaoManifest(@Nonnull PersonalDaoManifestEvent event){ EventBus.dispatch(PERSONAL_MANIFEST, event, "PersonalDaoManifestEvent"); }
    public static boolean firePrePersonalDaoManifest(@Nonnull PrePersonalDaoManifestEvent event){ return EventBus.fire(PRE_PERSONAL_MANIFEST, event, "PrePersonalDaoManifestEvent"); }
    public static void firePersonalDaoSetAside(@Nonnull PersonalDaoSetAsideEvent event){ EventBus.dispatch(PERSONAL_SET_ASIDE, event, "PersonalDaoSetAsideEvent"); }
    public static void fireDaoEnlightenment(@Nonnull DaoEnlightenmentEvent event){ EventBus.dispatch(ENLIGHTENMENT, event, "DaoEnlightenmentEvent"); }
    public static boolean firePreDaoEnlightenment(@Nonnull PreDaoEnlightenmentEvent event){ return EventBus.fire(PRE_ENLIGHTENMENT, event, "PreDaoEnlightenmentEvent"); }
}
