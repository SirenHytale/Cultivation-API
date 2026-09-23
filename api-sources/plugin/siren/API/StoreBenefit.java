package plugin.siren.API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One purchasable benefit from the Treasure Pavilion (xianxia.dev/store),
 * delivered in game to the Hytale player it was bought for.
 *
 * <p>Register one with {@link CultivationAPI#registerStoreBenefit}. The sync
 * (see {@code WebStoreConfig.json}) then keeps a per-product list of entitled
 * player UUIDs current, and anything on this server can ask
 * {@link CultivationAPI#hasStoreBenefit} - including another mod entirely,
 * which is the point: an addon registers its product here and gets the
 * fetching, caching, config gating and the recheck command for free.</p>
 *
 * <pre>{@code CultivationAPI.registerStoreBenefit(
 *         StoreBenefit.builder("myMod:store:crown", "my-mod-crown")
 *                 .name("server.myMod.store.crown")
 *                 .title("server.myMod.title.crown")
 *                 .hint("server.myMod.title.hint.crown")
 *                 .build());}</pre>
 *
 * <h2>What a benefit is, and is not</h2>
 *
 * <p>The website's list answers exactly one question: which players bought
 * this product. What that MEANS on a server is the registrant's business - a
 * title (see {@link Builder#title}), a cosmetic, a perk an addon applies from
 * the granted/revoked events in {@link StoreBenefitEvents}. Nothing here
 * grants gameplay power by itself, and a server can refuse any product by
 * slug via Disabled-Benefits without touching the mod that registered it.</p>
 */
public final class StoreBenefit {

    private final String key;
    private final String productSlug;
    private final String nameKey;
    private final String titleNameKey;
    private final String hintKey;

    private StoreBenefit(Builder builder){
        this.key = builder.key;
        this.productSlug = builder.productSlug;
        this.nameKey = builder.nameKey;
        this.titleNameKey = builder.titleNameKey;
        this.hintKey = builder.hintKey;
    }

    @Nonnull
    public static Builder builder(@Nonnull String key, @Nonnull String productSlug){
        return new Builder(key, productSlug);
    }

    /** The id this benefit is registered under - also the key of its auto-registered title, if any. */
    @Nonnull
    public String getKey(){
        return this.key;
    }

    /** The store's product slug - the `<slug>` in {@code /api/get/entitlements/<slug>.json}. */
    @Nonnull
    public String getProductSlug(){
        return this.productSlug;
    }

    /** Translation key for the benefit's display name, used by /cultivation store status. */
    @Nonnull
    public String getNameKey(){
        return this.nameKey;
    }

    /** Translation key of the auto-registered title's text, or null for no title. */
    @Nullable
    public String getTitleNameKey(){
        return this.titleNameKey;
    }

    /**
     * Translation key of the auto-registered title's locked-tile hint, or null
     * to fall back to the shared {@code server.customUI.cultivation.title.hint.store}
     * hint every other store title uses.
     */
    @Nullable
    public String getHintKey(){
        return this.hintKey;
    }

    public static final class Builder {
        private final String key;
        private final String productSlug;
        private String nameKey;
        private String titleNameKey;
        private String hintKey;

        private Builder(@Nonnull String key, @Nonnull String productSlug){
            this.key = key;
            this.productSlug = productSlug;
            this.nameKey = key;
        }

        /** Sets the display name used when the benefit is listed - a translation key. */
        @Nonnull
        public Builder name(@Nonnull String translationKey){
            this.nameKey = translationKey;
            return this;
        }

        /**
         * Also registers a {@link CultivationTitle} for this benefit, unlocked
         * for exactly the players who bought it. The title appears on the
         * picker greyed for everyone else, hinting at the Pavilion - which is
         * both how every locked thing in this mod presents itself and the only
         * advertising this system does.
         */
        @Nonnull
        public Builder title(@Nonnull String titleNameTranslationKey){
            this.titleNameKey = titleNameTranslationKey;
            return this;
        }

        /**
         * Overrides the auto-registered title's locked-tile hint for this
         * benefit specifically, rather than the shared "Bought from the
         * Treasure Pavilion..." hint every other store title falls back to -
         * for a benefit whose hint should name what it comes bundled with, or
         * otherwise say something more specific than the generic one.
         */
        @Nonnull
        public Builder hint(@Nonnull String translationKey){
            this.hintKey = translationKey;
            return this;
        }

        @Nonnull
        public StoreBenefit build(){
            if(this.key == null || this.key.isEmpty()){
                throw new IllegalStateException("A store benefit needs an id.");
            }
            if(this.productSlug == null || this.productSlug.isEmpty()){
                throw new IllegalStateException("A store benefit needs the product slug it is sold as.");
            }

            return new StoreBenefit(this);
        }
    }
}
