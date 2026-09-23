package plugin.siren.API;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Events for Cultivation Legacy - the temporary breakthrough-cost-reduction
 * buff a retiring cultivator passes to a chosen heir. See LegacyManager for
 * the full designate/payout/mailbox flow.
 */
public final class LegacyEvents {

    private LegacyEvents(){}

    // --- Post event -------------------------------------------------------

    /**
     * A legacy buff has actually reached (or been queued for) an heir - the
     * reduction described here is the FINAL amount, after the recent-profile
     * scale-down for an immediate payout, or the pre-scale base amount for a
     * queued one (see {@link #queued}).
     *
     * @param queued true if this was deferred to the offline-heir mailbox
     *               rather than applied immediately - the reduction/expiry
     *               here are the values as queued, not yet scaled for
     *               profile age; that scaling only happens at actual
     *               delivery and is not separately observable through this
     *               event.
     */
    public record LegacyPayoutEvent(@Nonnull UUID benefactorUuid, @Nonnull String benefactorName,
                                    @Nonnull UUID heirUuid, float reductionPercent, long expiresAtMillis,
                                    @Nonnull String sourceProfileName, boolean queued) {}

    // --- Pre event ----------------------------------------------------------

    /**
     * A legacy buff is about to be granted or queued. Cancel to refuse it
     * outright - nothing has been written or queued yet. A listener may also
     * rescale {@link #setReductionPercent} / {@link #setExpiresAtMillis}
     * before dispatch finishes; whatever is left in those fields is what
     * actually gets granted or queued.
     */
    public static final class PreLegacyPayoutEvent extends CancellableEvent {
        private final UUID benefactorUuid;
        private final String benefactorName;
        private final UUID heirUuid;
        private final int retiringRealmOrdinal;
        private final String sourceProfileName;
        private float reductionPercent;
        private long expiresAtMillis;

        public PreLegacyPayoutEvent(@Nonnull UUID benefactorUuid, @Nonnull String benefactorName,
                                    @Nonnull UUID heirUuid, int retiringRealmOrdinal,
                                    @Nonnull String sourceProfileName, float reductionPercent, long expiresAtMillis){
            this.benefactorUuid = benefactorUuid;
            this.benefactorName = benefactorName;
            this.heirUuid = heirUuid;
            this.retiringRealmOrdinal = retiringRealmOrdinal;
            this.sourceProfileName = sourceProfileName;
            this.reductionPercent = reductionPercent;
            this.expiresAtMillis = expiresAtMillis;
        }

        @Nonnull public UUID benefactorUuid(){ return this.benefactorUuid; }
        @Nonnull public String benefactorName(){ return this.benefactorName; }
        @Nonnull public UUID heirUuid(){ return this.heirUuid; }
        public int retiringRealmOrdinal(){ return this.retiringRealmOrdinal; }
        @Nonnull public String sourceProfileName(){ return this.sourceProfileName; }

        public float reductionPercent(){ return this.reductionPercent; }
        public void setReductionPercent(float reductionPercent){ this.reductionPercent = reductionPercent; }

        public long expiresAtMillis(){ return this.expiresAtMillis; }
        public void setExpiresAtMillis(long expiresAtMillis){ this.expiresAtMillis = expiresAtMillis; }
    }

    // --- Listener lists -------------------------------------------------------

    private static final List<Consumer<LegacyPayoutEvent>> PAYOUT = EventBus.newListenerList();
    private static final List<Consumer<PreLegacyPayoutEvent>> PRE_PAYOUT = EventBus.newListenerList();

    public static void onLegacyPayout(@Nonnull Consumer<LegacyPayoutEvent> listener){ PAYOUT.add(listener); }
    public static void onPreLegacyPayout(@Nonnull Consumer<PreLegacyPayoutEvent> listener){ PRE_PAYOUT.add(listener); }

    // --- Fired by LegacyManager ----------------------------------------------

    public static void fireLegacyPayout(@Nonnull LegacyPayoutEvent event){ EventBus.dispatch(PAYOUT, event, "LegacyPayoutEvent"); }
    public static boolean firePreLegacyPayout(@Nonnull PreLegacyPayoutEvent event){ return EventBus.fire(PRE_PAYOUT, event, "PreLegacyPayoutEvent"); }
}
