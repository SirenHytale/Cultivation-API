package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Partnered Cultivation (双修) - two married cultivators drawing on the same
 * spirit vein together, resolved and re-resolved every meditation tick by
 * {@code PartnerManager.resolvePartner}. Register a listener once from your
 * plugin's setup() and it fires every time the corresponding thing happens to
 * any player.
 *
 * <p>Follows the same pre/post shape {@link CultivationEvents} documents in
 * full - a {@code Pre*} event fires BEFORE the change, extends
 * {@link CancellableEvent}, and lets a listener veto it or re-tune the numbers
 * driving it; the matching post-event is a plain record fired once the change
 * is committed and cannot be cancelled. See that class's javadoc for the
 * threading and safety guarantees, which apply here unchanged.</p>
 */
public final class PartnerEvents {
    private PartnerEvents(){}

    // --- Post-event payloads (notifications; cannot be cancelled) ---

    /**
     * A cultivator transitioned from unpartnered to partnered - both spouses
     * sat down to meditate within Partner-Radius-Blocks of each other in the
     * same world. Fired once per side (each spouse gets their own event, with
     * {@code ref}/{@code player} naming THEM and {@code partnerUuid} naming
     * their spouse), the moment the transition is detected rather than on a
     * timer.
     */
    public record PartnerPairedEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull UUID partnerUuid) {}

    /**
     * A cultivator transitioned from partnered back to unpartnered - their
     * spouse stood up, wandered out of radius, changed world, or the pairing
     * otherwise lapsed. {@code formerPartnerUuid} is who they were partnered
     * with a moment ago. Not fired for the spouse who themselves stood up
     * first; see {@code PartnerManager.announceTransition}'s own javadoc for
     * why only the side still seated is told.
     */
    public record PartnerUnpairedEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull UUID formerPartnerUuid) {}

    // --- Pre-event payloads (cancellable; numbers are re-tunable) ---

    /**
     * A partnered cultivator's meditation Qi bonus is about to apply. Cancel
     * to deny the bonus entirely for this tick (equivalent to sitting alone);
     * adjust {@link #setMultiplier} to re-scale how much extra Qi this
     * specific pairing draws.
     */
    public static final class PrePartnerQiBonusEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final UUID partnerUuid;
        private float multiplier;

        public PrePartnerQiBonusEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                      @Nullable UUID partnerUuid, float multiplier){
            this.ref = ref;
            this.player = player;
            this.partnerUuid = partnerUuid;
            this.multiplier = multiplier;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        /** Who this cultivator is currently partnered with - null only if the pairing could not be resolved to a UUID. */
        @Nullable public UUID partnerUuid(){ return this.partnerUuid; }
        /** The Qi multiplier about to be applied on top of every other source (vein tier, weather, formations, abode...). */
        public float multiplier(){ return this.multiplier; }
        public void setMultiplier(float multiplier){ this.multiplier = multiplier; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PartnerPairedEvent>> PARTNER_PAIRED = EventBus.newListenerList();
    private static final List<Consumer<PartnerUnpairedEvent>> PARTNER_UNPAIRED = EventBus.newListenerList();
    private static final List<Consumer<PrePartnerQiBonusEvent>> PRE_PARTNER_QI_BONUS = EventBus.newListenerList();

    public static void onPartnerPaired(@Nonnull Consumer<PartnerPairedEvent> listener){ PARTNER_PAIRED.add(listener); }
    public static void onPartnerUnpaired(@Nonnull Consumer<PartnerUnpairedEvent> listener){ PARTNER_UNPAIRED.add(listener); }
    public static void onPrePartnerQiBonus(@Nonnull Consumer<PrePartnerQiBonusEvent> listener){ PRE_PARTNER_QI_BONUS.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void firePartnerPaired(@Nonnull PartnerPairedEvent event){ EventBus.dispatch(PARTNER_PAIRED, event, "PartnerPairedEvent"); }
    public static void firePartnerUnpaired(@Nonnull PartnerUnpairedEvent event){ EventBus.dispatch(PARTNER_UNPAIRED, event, "PartnerUnpairedEvent"); }
    public static boolean firePrePartnerQiBonus(@Nonnull PrePartnerQiBonusEvent event){ return EventBus.fire(PRE_PARTNER_QI_BONUS, event, "PrePartnerQiBonusEvent"); }
}
