package plugin.siren.API;

import plugin.siren.Utils.Market.AuctionListing;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Auction House and Traveling Merchant events. See {@link CultivationEvents}
 * for the conventions every {@code *Events} class in this package shares (pre
 * vs post, cancellation, threading, registration).
 *
 * <p>Auction mutations run inside {@code AuctionManager}'s synchronized
 * methods, so a listener here is holding that lock: read what you need, hand
 * it off, and return. Do not call back into AuctionManager's mutating methods
 * from a listener.</p>
 *
 * <p>Players are identified by UUID rather than PlayerRef, matching
 * {@link SectEvents}'s own reasoning - a sold listing routinely pays out to a
 * seller who is not online at the moment of sale.</p>
 */
public final class MarketEvents {
    private MarketEvents(){}

    // --- Post-events ---

    /** A listing was created and is now on the shelf. */
    public record AuctionListingCreatedEvent(@Nonnull UUID seller, @Nonnull AuctionListing listing) {}

    /** A listing sold. {@code listing} is the now-removed shelf entry; {@code sellerProceeds} is what the seller was credited after the house cut. */
    public record AuctionListingSoldEvent(@Nonnull AuctionListing listing, @Nonnull UUID buyer, long sellerProceeds) {}

    /** A seller pulled their own still-active listing. */
    public record AuctionListingCancelledEvent(@Nonnull AuctionListing listing) {}

    /** An unsold listing aged past Market-Auction-Listing-Duration-Hours and was returned to its seller as a claimable parcel. */
    public record AuctionListingExpiredEvent(@Nonnull AuctionListing listing) {}

    /** The Traveling Merchant opened for business in a world. */
    public record MerchantOpenedEvent(@Nonnull String world, double x, double y, double z) {}

    /** The Traveling Merchant's visit ended and the NPC despawned. */
    public record MerchantClosedEvent(@Nonnull String world) {}

    /** A bid was placed on a timed auction and is now the standing high bid. */
    public record AuctionBidPlacedEvent(@Nonnull AuctionListing listing, @Nonnull UUID bidder, long amount) {}

    /** A standing high bidder was outbid, or their bid lost to an early buyout - their stones just landed in their claimable parcels (see {@code /market claim}), even while offline. */
    public record AuctionOutbidEvent(@Nonnull AuctionListing listing, @Nonnull UUID outbidBidder, long refundedAmount) {}

    /** A timed auction closed with at least one bid - the highest bidder won the item and the seller was credited {@code sellerProceeds} after the house cut. Mirrors {@link AuctionListingSoldEvent}'s shape for the timed path; a zero-bid close fires the existing {@link AuctionListingExpiredEvent} instead, unchanged. */
    public record AuctionClosedEvent(@Nonnull AuctionListing listing, @Nonnull UUID winner, long sellerProceeds) {}

    // --- Pre-events ---

    /** A player is about to list an item. Cancel to refuse it (reported as blocked) - nothing has been touched in the seller's inventory yet. */
    public static final class PreAuctionListEvent extends CancellableEvent {
        private final UUID seller;
        private final String itemId;
        private final int quantity;
        private long price;

        public PreAuctionListEvent(@Nonnull UUID seller, @Nonnull String itemId, int quantity, long price){
            this.seller = seller;
            this.itemId = itemId;
            this.quantity = quantity;
            this.price = price;
        }

        @Nonnull public UUID seller(){ return this.seller; }
        @Nonnull public String itemId(){ return this.itemId; }
        public int quantity(){ return this.quantity; }
        public long price(){ return this.price; }
        public void setPrice(long price){ this.price = price; }
    }

    /** A player is about to buy a listing. Cancel to refuse it - nothing has moved yet, the listing stays on the shelf. */
    public static final class PreAuctionBuyEvent extends CancellableEvent {
        private final UUID buyer;
        private final AuctionListing listing;

        public PreAuctionBuyEvent(@Nonnull UUID buyer, @Nonnull AuctionListing listing){
            this.buyer = buyer;
            this.listing = listing;
        }

        @Nonnull public UUID buyer(){ return this.buyer; }
        @Nonnull public AuctionListing listing(){ return this.listing; }
    }

    /**
     * A player is about to bid on a timed auction. Cancel to refuse it -
     * nothing has moved yet, and the previous high bidder (if any) has not
     * been refunded. Deliberately no setter, unlike {@link PreAuctionListEvent}'s
     * {@code setPrice} - a listener silently changing the amount a player just
     * committed to would be a surprise. Add one only if a real addon need
     * shows up; until then, the supported way to re-tune a bid is to cancel it.
     */
    public static final class PreAuctionBidEvent extends CancellableEvent {
        private final UUID bidder;
        private final AuctionListing listing;
        private final long amount;

        public PreAuctionBidEvent(@Nonnull UUID bidder, @Nonnull AuctionListing listing, long amount){
            this.bidder = bidder;
            this.listing = listing;
            this.amount = amount;
        }

        @Nonnull public UUID bidder(){ return this.bidder; }
        @Nonnull public AuctionListing listing(){ return this.listing; }
        public long amount(){ return this.amount; }
    }

    // --- Listener registration ---

    private static final List<Consumer<AuctionListingCreatedEvent>> LISTING_CREATED = EventBus.newListenerList();
    private static final List<Consumer<PreAuctionListEvent>> PRE_LISTING_CREATE = EventBus.newListenerList();
    private static final List<Consumer<AuctionListingSoldEvent>> LISTING_SOLD = EventBus.newListenerList();
    private static final List<Consumer<PreAuctionBuyEvent>> PRE_LISTING_BUY = EventBus.newListenerList();
    private static final List<Consumer<AuctionListingCancelledEvent>> LISTING_CANCELLED = EventBus.newListenerList();
    private static final List<Consumer<AuctionListingExpiredEvent>> LISTING_EXPIRED = EventBus.newListenerList();
    private static final List<Consumer<MerchantOpenedEvent>> MERCHANT_OPENED = EventBus.newListenerList();
    private static final List<Consumer<MerchantClosedEvent>> MERCHANT_CLOSED = EventBus.newListenerList();
    private static final List<Consumer<AuctionBidPlacedEvent>> BID_PLACED = EventBus.newListenerList();
    private static final List<Consumer<AuctionOutbidEvent>> OUTBID = EventBus.newListenerList();
    private static final List<Consumer<AuctionClosedEvent>> AUCTION_CLOSED = EventBus.newListenerList();
    private static final List<Consumer<PreAuctionBidEvent>> PRE_BID = EventBus.newListenerList();

    public static void onAuctionListingCreated(@Nonnull Consumer<AuctionListingCreatedEvent> listener){ LISTING_CREATED.add(listener); }
    public static void onPreAuctionList(@Nonnull Consumer<PreAuctionListEvent> listener){ PRE_LISTING_CREATE.add(listener); }
    public static void onAuctionListingSold(@Nonnull Consumer<AuctionListingSoldEvent> listener){ LISTING_SOLD.add(listener); }
    public static void onPreAuctionBuy(@Nonnull Consumer<PreAuctionBuyEvent> listener){ PRE_LISTING_BUY.add(listener); }
    public static void onAuctionListingCancelled(@Nonnull Consumer<AuctionListingCancelledEvent> listener){ LISTING_CANCELLED.add(listener); }
    public static void onAuctionListingExpired(@Nonnull Consumer<AuctionListingExpiredEvent> listener){ LISTING_EXPIRED.add(listener); }
    public static void onMerchantOpened(@Nonnull Consumer<MerchantOpenedEvent> listener){ MERCHANT_OPENED.add(listener); }
    public static void onMerchantClosed(@Nonnull Consumer<MerchantClosedEvent> listener){ MERCHANT_CLOSED.add(listener); }
    public static void onAuctionBidPlaced(@Nonnull Consumer<AuctionBidPlacedEvent> listener){ BID_PLACED.add(listener); }
    public static void onAuctionOutbid(@Nonnull Consumer<AuctionOutbidEvent> listener){ OUTBID.add(listener); }
    public static void onAuctionClosed(@Nonnull Consumer<AuctionClosedEvent> listener){ AUCTION_CLOSED.add(listener); }
    public static void onPreAuctionBid(@Nonnull Consumer<PreAuctionBidEvent> listener){ PRE_BID.add(listener); }

    // --- Internal dispatch (called by this mod's own code; not API) ---

    public static void fireAuctionListingCreated(@Nonnull AuctionListingCreatedEvent event){ EventBus.dispatch(LISTING_CREATED, event, "AuctionListingCreatedEvent"); }
    public static boolean firePreAuctionList(@Nonnull PreAuctionListEvent event){ return EventBus.fire(PRE_LISTING_CREATE, event, "PreAuctionListEvent"); }
    public static void fireAuctionListingSold(@Nonnull AuctionListingSoldEvent event){ EventBus.dispatch(LISTING_SOLD, event, "AuctionListingSoldEvent"); }
    public static boolean firePreAuctionBuy(@Nonnull PreAuctionBuyEvent event){ return EventBus.fire(PRE_LISTING_BUY, event, "PreAuctionBuyEvent"); }
    public static void fireAuctionListingCancelled(@Nonnull AuctionListingCancelledEvent event){ EventBus.dispatch(LISTING_CANCELLED, event, "AuctionListingCancelledEvent"); }
    public static void fireAuctionListingExpired(@Nonnull AuctionListingExpiredEvent event){ EventBus.dispatch(LISTING_EXPIRED, event, "AuctionListingExpiredEvent"); }
    public static void fireMerchantOpened(@Nonnull MerchantOpenedEvent event){ EventBus.dispatch(MERCHANT_OPENED, event, "MerchantOpenedEvent"); }
    public static void fireMerchantClosed(@Nonnull MerchantClosedEvent event){ EventBus.dispatch(MERCHANT_CLOSED, event, "MerchantClosedEvent"); }
    public static void fireAuctionBidPlaced(@Nonnull AuctionBidPlacedEvent event){ EventBus.dispatch(BID_PLACED, event, "AuctionBidPlacedEvent"); }
    public static void fireAuctionOutbid(@Nonnull AuctionOutbidEvent event){ EventBus.dispatch(OUTBID, event, "AuctionOutbidEvent"); }
    public static void fireAuctionClosed(@Nonnull AuctionClosedEvent event){ EventBus.dispatch(AUCTION_CLOSED, event, "AuctionClosedEvent"); }
    public static boolean firePreAuctionBid(@Nonnull PreAuctionBidEvent event){ return EventBus.fire(PRE_BID, event, "PreAuctionBidEvent"); }
}
