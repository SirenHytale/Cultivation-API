package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Combat Depth events - technique interrupts and the general Wu Xing PvP
 * reward. See {@link CultivationEvents} for the conventions every {@code
 * *Events} class in this package shares. Punish Windows themselves fire no
 * events of their own - they are a pure damage-multiplier consumed inside
 * {@code PunishWindowCombatSystem}, with nothing an addon would re-scale
 * beyond what the two events below already expose (the interrupt that opened
 * one, and the reward a favorable matchup pays out).
 */
public final class CombatDepthEvents {
    private CombatDepthEvents(){}

    // --- Post-events ---

    /** A charging cultivator's gathering was broken by a hard-enough hit. {@code damageAmount} is the (possibly re-scaled) figure {@link PreTechniqueInterruptEvent} settled on. */
    public record TechniqueInterruptEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float damageAmount) {}

    /** A favorable-matchup PvP kill paid its Wu Xing reward. */
    public record WuxingPvpRewardEvent(@Nonnull Ref<EntityStore> killer, @Nonnull PlayerRef killerPlayer, int spiritStones) {}

    // --- Pre-events ---

    /** A charging cultivator's gathering is about to be broken by a hard-enough hit. Cancel to let the gathering continue uninterrupted; {@link #setDamageAmount} re-scales the figure carried into the paired post-event (the interrupt itself still happens once this fires - the damage that triggered it has already landed). */
    public static final class PreTechniqueInterruptEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private float damageAmount;

        public PreTechniqueInterruptEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float damageAmount){
            this.ref = ref;
            this.player = player;
            this.damageAmount = damageAmount;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        public float damageAmount(){ return this.damageAmount; }
        public void setDamageAmount(float damageAmount){ this.damageAmount = damageAmount; }
    }

    /** A favorable-matchup PvP kill is about to pay its Wu Xing reward. Cancel to deny it; {@link #setSpiritStones} to re-scale how many are actually granted. */
    public static final class PreWuxingPvpRewardEvent extends CancellableEvent {
        private final Ref<EntityStore> killer;
        private final PlayerRef killerPlayer;
        private int spiritStones;

        public PreWuxingPvpRewardEvent(@Nonnull Ref<EntityStore> killer, @Nonnull PlayerRef killerPlayer, int spiritStones){
            this.killer = killer;
            this.killerPlayer = killerPlayer;
            this.spiritStones = spiritStones;
        }

        @Nonnull public Ref<EntityStore> killer(){ return this.killer; }
        @Nonnull public PlayerRef killerPlayer(){ return this.killerPlayer; }
        public int spiritStones(){ return this.spiritStones; }
        public void setSpiritStones(int spiritStones){ this.spiritStones = spiritStones; }
    }

    // --- Listener registration ---

    private static final List<Consumer<TechniqueInterruptEvent>> TECHNIQUE_INTERRUPT = EventBus.newListenerList();
    private static final List<Consumer<PreTechniqueInterruptEvent>> PRE_TECHNIQUE_INTERRUPT = EventBus.newListenerList();
    private static final List<Consumer<WuxingPvpRewardEvent>> WUXING_PVP_REWARD = EventBus.newListenerList();
    private static final List<Consumer<PreWuxingPvpRewardEvent>> PRE_WUXING_PVP_REWARD = EventBus.newListenerList();

    public static void onTechniqueInterrupt(@Nonnull Consumer<TechniqueInterruptEvent> listener){ TECHNIQUE_INTERRUPT.add(listener); }
    public static void onPreTechniqueInterrupt(@Nonnull Consumer<PreTechniqueInterruptEvent> listener){ PRE_TECHNIQUE_INTERRUPT.add(listener); }
    public static void onWuxingPvpReward(@Nonnull Consumer<WuxingPvpRewardEvent> listener){ WUXING_PVP_REWARD.add(listener); }
    public static void onPreWuxingPvpReward(@Nonnull Consumer<PreWuxingPvpRewardEvent> listener){ PRE_WUXING_PVP_REWARD.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireTechniqueInterrupt(@Nonnull TechniqueInterruptEvent event){ EventBus.dispatch(TECHNIQUE_INTERRUPT, event, "TechniqueInterruptEvent"); }
    public static boolean firePreTechniqueInterrupt(@Nonnull PreTechniqueInterruptEvent event){ return EventBus.fire(PRE_TECHNIQUE_INTERRUPT, event, "PreTechniqueInterruptEvent"); }
    public static void fireWuxingPvpReward(@Nonnull WuxingPvpRewardEvent event){ EventBus.dispatch(WUXING_PVP_REWARD, event, "WuxingPvpRewardEvent"); }
    public static boolean firePreWuxingPvpReward(@Nonnull PreWuxingPvpRewardEvent event){ return EventBus.fire(PRE_WUXING_PVP_REWARD, event, "PreWuxingPvpRewardEvent"); }
}
