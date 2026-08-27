package plugin.siren.API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The inter-sect diplomacy ladder, each rung a strict superset of the one
 * below it - see {@code AllianceManager} for how a standing relation is
 * formed/broken, {@code WarManager#declare} for the war veto every rung
 * grants, and {@code AuctionManager#buy} for TRADE/ALLIANCE's Auction House
 * cut discount.
 *
 * <ul>
 *   <li>{@link #NON_AGGRESSION} - neither side may declare war on the other.</li>
 *   <li>{@link #TRADE} - everything NON_AGGRESSION has, plus a discounted
 *       Auction House cut on sales between the two sects' members.</li>
 *   <li>{@link #ALLIANCE} - everything TRADE has, plus trusted-guest land
 *       access and tribulation-assist - the original, single-slot Alliance
 *       concept this ladder was built on top of. See {@code
 *       AllianceManager#areAllied}, which keeps meaning this rung
 *       specifically for its two pre-existing callers.</li>
 * </ul>
 */
public enum SectRelationKind {
    NON_AGGRESSION,
    TRADE,
    ALLIANCE;

    /**
     * Case-insensitive lookup by enum name, falling back to {@link #ALLIANCE} -
     * the same convention {@code Sect.JoinPolicy#fromName} uses. For decoding
     * a persisted {@code Alliance} record only: every record written before
     * the {@code Kind} codec key existed has no value for it and decodes as
     * ALLIANCE, byte-identical to how it behaved before this ladder existed.
     * Never use this for a player-typed command token - see {@link #fromToken}.
     */
    @Nonnull
    public static SectRelationKind fromName(@Nullable String name){
        if(name != null){
            for(SectRelationKind kind : values()){
                if(kind.name().equalsIgnoreCase(name.trim())){
                    return kind;
                }
            }
        }
        return ALLIANCE;
    }

    /**
     * Strict lookup by the exact lowercase token a player types at
     * {@code /sect ally propose <sect> <kind>} - "nonaggression", "trade" or
     * "alliance". Unlike {@link #fromName}, an unrecognized token returns
     * {@code null} rather than silently guessing ALLIANCE - the caller must
     * refuse clearly instead of granting a rung nobody asked for.
     */
    @Nullable
    public static SectRelationKind fromToken(@Nullable String token){
        if(token == null){
            return null;
        }
        return switch(token.trim().toLowerCase(Locale.ROOT)){
            case "nonaggression" -> NON_AGGRESSION;
            case "trade" -> TRADE;
            case "alliance" -> ALLIANCE;
            default -> null;
        };
    }

    /** The exact token {@link #fromToken} accepts for this kind - for command usage text and player-facing messages. */
    @Nonnull
    public String token(){
        return switch(this){
            case NON_AGGRESSION -> "nonaggression";
            case TRADE -> "trade";
            case ALLIANCE -> "alliance";
        };
    }
}
