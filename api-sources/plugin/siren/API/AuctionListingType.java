package plugin.siren.API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The two ways an item can be offered on the Auction House shelf (see
 * {@code plugin.siren.Utils.Market.AuctionManager}): a fixed-price buy-now
 * listing, or a timed competitive-bidding auction.
 *
 * <p>Closed rather than an open registry like {@code MarketCategories} -
 * {@code AuctionManager} branches on this value throughout its bid/buy/close
 * logic (auto-extend, refund-on-outbid, the bid-vs-buy-now charge path), so a
 * registered third type would advertise an extension point with no behavior
 * actually wired to it. An addon that wants to observe or veto a bid should
 * listen to {@code MarketEvents.PreAuctionBidEvent} instead - that is the
 * real, supported extension surface for this feature.</p>
 */
public enum AuctionListingType {
    FIXED("fixed", "server.customUI.cultivation.market.type.fixed"),
    TIMED("timed", "server.customUI.cultivation.market.type.timed");

    private final String id;
    private final String translationKey;

    AuctionListingType(@Nonnull String id, @Nonnull String translationKey){
        this.id = id;
        this.translationKey = translationKey;
    }

    /** @return the stable string persisted in {@code AuctionListing.CODEC}'s {@code "ListingType"} key. */
    @Nonnull
    public String getId(){
        return this.id;
    }

    @Nonnull
    public String getTranslationKey(){
        return this.translationKey;
    }

    /**
     * @return the type matching {@code id}, or {@link #FIXED} for {@code null}
     * or any unrecognized string - the same fallback value a pre-0.10.2
     * {@code AuctionListing} row decodes to, since its {@code listingType}
     * field's initializer is {@code "fixed"} (see {@code AuctionListing}'s own
     * class doc on additive-codec-key back-compat).
     */
    @Nonnull
    public static AuctionListingType fromId(@Nullable String id){
        for(AuctionListingType type : values()){
            if(type.id.equals(id)){
                return type;
            }
        }
        return FIXED;
    }
}
