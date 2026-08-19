package plugin.siren.API;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Weapon Spirit (器灵) events - a Life-Bound Treasure's spirit stirring awake,
 * gaining a level, or being fed Qi through {@code /cultivation spirit nurture}.
 * See {@link CultivationEvents} for the conventions every {@code *Events} class
 * in this package shares (pre vs post, cancellation, threading, registration).
 *
 * <p>All six events carry the current OWNER's {@link PlayerRef} (the same one
 * {@code LifeBoundMetadata} names) rather than a bare {@code Ref<EntityStore>} -
 * a spirit only ever answers to its bound wielder, so there is never a case
 * where the relevant player is offline or the entity is something other than
 * them.</p>
 */
public final class WeaponSpiritEvents {
    private WeaponSpiritEvents(){}

    // --- Post-events ---

    /** A weapon's spirit just stirred awake; {@code item} is the stack AFTER the awaken is written. */
    public record WeaponSpiritAwakenEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, int killsAtAwaken) {}

    /** A weapon's spirit just gained a level; {@code item} is the stack AFTER the level-up is written. */
    public record WeaponSpiritLevelUpEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, int newLevel, boolean reachedMaturity) {}

    /** A player just fed their weapon spirit Qi; fires once per successful {@code nurture} call, whether or not it also leveled the spirit up. */
    public record WeaponSpiritNurtureEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, float qiConverted, float xpGained) {}

    // --- Pre-events ---

    /**
     * A weapon's spirit is about to stir awake - both the refinement-tier and
     * kill bars are already met. Cancel to hold it right at the threshold; the
     * banked kill count is untouched, so the very next qualifying kill fires
     * this again.
     */
    public static final class PreWeaponSpiritAwakenEvent extends CancellableEvent {
        private final PlayerRef owner;
        private final ItemStack item;
        private final int killsAtAwaken;

        public PreWeaponSpiritAwakenEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, int killsAtAwaken){
            this.owner = owner;
            this.item = item;
            this.killsAtAwaken = killsAtAwaken;
        }

        @Nonnull public PlayerRef owner(){ return this.owner; }
        /** The stack as it stands BEFORE the awaken is written into its metadata. */
        @Nonnull public ItemStack item(){ return this.item; }
        public int killsAtAwaken(){ return this.killsAtAwaken; }
    }

    /** A weapon spirit is about to level up. Cancel to hold it at its current level (the Xp is still banked). */
    public static final class PreWeaponSpiritLevelUpEvent extends CancellableEvent {
        private final PlayerRef owner;
        private final ItemStack item;
        private final int oldLevel;
        private final int newLevel;

        public PreWeaponSpiritLevelUpEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, int oldLevel, int newLevel){
            this.owner = owner;
            this.item = item;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }

        @Nonnull public PlayerRef owner(){ return this.owner; }
        /** The stack as it stands BEFORE the level-up is written into its metadata. */
        @Nonnull public ItemStack item(){ return this.item; }
        public int oldLevel(){ return this.oldLevel; }
        public int newLevel(){ return this.newLevel; }
    }

    /**
     * A player is about to feed their weapon spirit Qi. Cancel to refuse the
     * feeding entirely (no Qi spent, no Xp gained); {@link #setQiToConvert} to
     * change how much of the offered Qi actually converts - this is where
     * {@code WeaponSpirit-Nurture-Max-Qi-Per-Use} is applied by default, and an
     * addon raising or lowering it (a VIP perk, a debuff) does so by adjusting
     * this field rather than the config itself.
     */
    public static final class PreWeaponSpiritNurtureEvent extends CancellableEvent {
        private final PlayerRef owner;
        private final ItemStack item;
        private float qiToConvert;

        public PreWeaponSpiritNurtureEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, float qiToConvert){
            this.owner = owner;
            this.item = item;
            this.qiToConvert = qiToConvert;
        }

        @Nonnull public PlayerRef owner(){ return this.owner; }
        @Nonnull public ItemStack item(){ return this.item; }
        /** Qi that will actually be converted to spirit Xp - already capped, before this event ran. */
        public float qiToConvert(){ return this.qiToConvert; }
        public void setQiToConvert(float qiToConvert){ this.qiToConvert = qiToConvert; }
    }

    // --- Registration ---

    private static final List<Consumer<WeaponSpiritAwakenEvent>> AWAKEN = EventBus.newListenerList();
    private static final List<Consumer<PreWeaponSpiritAwakenEvent>> PRE_AWAKEN = EventBus.newListenerList();
    private static final List<Consumer<WeaponSpiritLevelUpEvent>> LEVEL_UP = EventBus.newListenerList();
    private static final List<Consumer<PreWeaponSpiritLevelUpEvent>> PRE_LEVEL_UP = EventBus.newListenerList();
    private static final List<Consumer<WeaponSpiritNurtureEvent>> NURTURE = EventBus.newListenerList();
    private static final List<Consumer<PreWeaponSpiritNurtureEvent>> PRE_NURTURE = EventBus.newListenerList();

    public static void onWeaponSpiritAwaken(@Nonnull Consumer<WeaponSpiritAwakenEvent> listener){ AWAKEN.add(listener); }
    public static void onPreWeaponSpiritAwaken(@Nonnull Consumer<PreWeaponSpiritAwakenEvent> listener){ PRE_AWAKEN.add(listener); }
    public static void onWeaponSpiritLevelUp(@Nonnull Consumer<WeaponSpiritLevelUpEvent> listener){ LEVEL_UP.add(listener); }
    public static void onPreWeaponSpiritLevelUp(@Nonnull Consumer<PreWeaponSpiritLevelUpEvent> listener){ PRE_LEVEL_UP.add(listener); }
    public static void onWeaponSpiritNurture(@Nonnull Consumer<WeaponSpiritNurtureEvent> listener){ NURTURE.add(listener); }
    public static void onPreWeaponSpiritNurture(@Nonnull Consumer<PreWeaponSpiritNurtureEvent> listener){ PRE_NURTURE.add(listener); }

    // --- Internal dispatch (called by WeaponSpiritManager; not API) ---

    public static void fireWeaponSpiritAwaken(@Nonnull WeaponSpiritAwakenEvent event){ EventBus.dispatch(AWAKEN, event, "WeaponSpiritAwakenEvent"); }
    public static boolean firePreWeaponSpiritAwaken(@Nonnull PreWeaponSpiritAwakenEvent event){ return EventBus.fire(PRE_AWAKEN, event, "PreWeaponSpiritAwakenEvent"); }
    public static void fireWeaponSpiritLevelUp(@Nonnull WeaponSpiritLevelUpEvent event){ EventBus.dispatch(LEVEL_UP, event, "WeaponSpiritLevelUpEvent"); }
    public static boolean firePreWeaponSpiritLevelUp(@Nonnull PreWeaponSpiritLevelUpEvent event){ return EventBus.fire(PRE_LEVEL_UP, event, "PreWeaponSpiritLevelUpEvent"); }
    public static void fireWeaponSpiritNurture(@Nonnull WeaponSpiritNurtureEvent event){ EventBus.dispatch(NURTURE, event, "WeaponSpiritNurtureEvent"); }
    public static boolean firePreWeaponSpiritNurture(@Nonnull PreWeaponSpiritNurtureEvent event){ return EventBus.fire(PRE_NURTURE, event, "PreWeaponSpiritNurtureEvent"); }
}
