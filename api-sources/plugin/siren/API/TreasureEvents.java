package plugin.siren.API;

import plugin.siren.Utils.Treasure.TreasureTier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Treasure/Ruin Exploration events - covers BOTH tiers (a Buried Cache claim
 * and a Ruin Vault entry are both "claiming" a {@code TreasureCacheSite}, see
 * {@link TreasureTier}). See {@link CultivationEvents} for the conventions
 * every {@code *Events} class in this package shares (pre vs post,
 * cancellation, threading, registration).
 *
 * <p>Dispatched by {@code TreasureCacheManager} - this class only declares
 * the surface; nothing here calls into that manager, matching every other
 * standalone {@code *Events} class in this package.</p>
 */
public final class TreasureEvents {
    private TreasureEvents(){}

    // --- Pre-events ---

    /**
     * A player is about to claim a Buried Cache or enter a Ruin Vault. Cancel
     * to refuse it entirely - the site stays unclaimed and the command is a
     * no-op, the same "silence is a valid answer" shape
     * {@code RivalEvents.PreRivalChallengeEvent} gives a rival challenge.
     */
    public static final class PreTreasureClaimEvent extends CancellableEvent {
        private final String siteId;
        private final TreasureTier tier;
        private final UUID playerUuid;

        public PreTreasureClaimEvent(@Nonnull String siteId, @Nonnull TreasureTier tier, @Nonnull UUID playerUuid){
            this.siteId = siteId;
            this.tier = tier;
            this.playerUuid = playerUuid;
        }

        @Nonnull public String siteId(){ return this.siteId; }
        @Nonnull public TreasureTier tier(){ return this.tier; }
        @Nonnull public UUID playerUuid(){ return this.playerUuid; }
    }

    // --- Post-events ---

    /** A Treasure site has been claimed/entered - the reward has already been paid. */
    public record TreasureClaimedEvent(@Nonnull String siteId, @Nonnull TreasureTier tier, @Nonnull UUID playerUuid,
                                       @Nonnull String worldName, float qiAwarded, boolean manualAwarded, boolean materialAwarded){}

    // --- Listener registration ---

    private static final List<Consumer<PreTreasureClaimEvent>> PRE_CLAIM = EventBus.newListenerList();
    private static final List<Consumer<TreasureClaimedEvent>> CLAIMED = EventBus.newListenerList();

    public static void onPreTreasureClaim(@Nonnull Consumer<PreTreasureClaimEvent> listener){ PRE_CLAIM.add(listener); }
    public static void onTreasureClaimed(@Nonnull Consumer<TreasureClaimedEvent> listener){ CLAIMED.add(listener); }

    // --- Internal dispatch (called by TreasureCacheManager; not API) ---

    public static boolean firePreTreasureClaim(@Nonnull PreTreasureClaimEvent event){ return EventBus.fire(PRE_CLAIM, event, "PreTreasureClaimEvent"); }
    public static void fireTreasureClaimed(@Nonnull TreasureClaimedEvent event){ EventBus.dispatch(CLAIMED, event, "TreasureClaimedEvent"); }
}
