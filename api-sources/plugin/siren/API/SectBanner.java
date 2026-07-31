package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One banner a sect can fly over its hall - the standing column of light that
 * tells anyone walking past whose ground they are on.
 *
 * <p>Register one with {@link CultivationAPI#registerSectBanner}. Cultivation
 * ships a handful; a mod may add as many more as it likes:</p>
 *
 * <pre>{@code CultivationAPI.registerSectBanner(
 *         SectBanner.builder("myMod:frostLotus")
 *                 .name("server.myMod.banner.frostLotus")
 *                 .section("server.myMod.banner.section.winter")
 *                 .swatch(0x9FD8F0)
 *                 .particle("MyMod_HallBanner_FrostLotus")
 *                 .build());}</pre>
 *
 * <h2>Why a banner is a particle id, not a colour</h2>
 *
 * <p>The same constraint that makes a {@link CultivationPalette} a set of
 * documents rather than a set of hex values. The spawn packet does carry a
 * {@code Color} field, but nothing in the engine ever sends a non-null one and
 * the client that would interpret it is not readable, so tinting one shared
 * asset at spawn time is unverifiable. A banner therefore names its own
 * {@code .particlesystem}, with its colours baked into the spawner - the one
 * mechanism this mod has actually proven works.</p>
 *
 * <h2>The asset must be capped</h2>
 *
 * <p>A hall beacon is re-spawned on a pulse for as long as the hall stands, so a
 * banner's asset has the same hard requirement every looping effect in this mod
 * has: a finite {@code TotalParticles} <i>and</i> an explicit system
 * {@code LifeSpan}, sized to burn out inside
 * {@code Sect-Hall-Beacon-Interval-Seconds}. Vanilla's own looping auras declare
 * {@code TotalParticles: -1} and no "stop" call exists anywhere in the engine,
 * so an uncapped banner would leak an instance on every client that ever walked
 * past the hall, forever. See {@code tools/gen_sect_banners.py}.</p>
 *
 * <h2>What happens when the mod that added one goes away</h2>
 *
 * <p>Nothing breaks. A sect stores its banner's <i>id</i>, never the banner, and
 * an id nobody claims resolves back to the vein-tier default the hall had before
 * banners existed - exactly how a palette falls back. Re-installing the mod
 * gives the sect its banner back, because the choice was never discarded.</p>
 */
public final class SectBanner {

    private final String key;
    private final Supplier<Message> name;
    private final String nameKey;
    private final String sectionKey;
    private final int swatch;
    private final String particleId;
    private final String permission;
    private final Predicate<PlayerRef> visible;

    private SectBanner(Builder builder){
        this.key = builder.key;
        this.name = builder.name;
        this.nameKey = builder.nameKey;
        this.sectionKey = builder.sectionKey;
        this.swatch = builder.swatch;
        this.particleId = builder.particleId;
        this.permission = builder.permission;
        this.visible = builder.visible;
    }

    @Nonnull
    public static Builder builder(@Nonnull String key){
        return new Builder(key);
    }

    /** The id this banner is registered under, and what is saved on the sect. */
    @Nonnull
    public String getKey(){
        return this.key;
    }

    /** The picker tile's text, resolved fresh per draw so a {@link CultivationTheme} still applies. */
    @Nonnull
    public Message getName(){
        return this.name.get();
    }

    /**
     * The raw translation key behind {@link #getName()}, or null if this banner
     * was never given one.
     *
     * <p>Needed because a dropdown entry takes a message <i>id</i> that the
     * client resolves itself ({@code LocalizableString.fromMessageId}), not a
     * resolved {@link Message} - so the picker cannot go through
     * {@link #getName()} the way a label can. The trade is that a
     * {@link CultivationTheme}'s re-wording does not reach the dropdown, only
     * the lines around it; the engine gives no way to push a Message into a
     * dropdown entry.</p>
     */
    @Nullable
    public String getNameKey(){
        return this.nameKey;
    }

    /** Translation key of the caption this banner groups under, or null for ungrouped. */
    @Nullable
    public String getSectionKey(){
        return this.sectionKey;
    }

    /** RGB shown on the picker tile, so the grid can be scanned by eye. */
    public int getSwatch(){
        return this.swatch;
    }

    /** The {@code .particlesystem} SystemId raised over the hall. */
    @Nonnull
    public String getParticleId(){
        return this.particleId;
    }

    /**
     * Whether this player may choose this banner. False hides it from the picker
     * and refuses to apply it, so a hand-crafted event packet cannot reach a
     * banner its sect was never offered.
     */
    public boolean isAvailableTo(@Nonnull PlayerRef playerRef){
        if(this.permission != null && !playerRef.hasPermission(this.permission)){
            return false;
        }

        return this.visible == null || this.visible.test(playerRef);
    }

    public static final class Builder {
        private final String key;
        private Supplier<Message> name;
        private String nameKey;
        private String sectionKey;
        private int swatch = 0xD9A63E;
        private String particleId;
        private String permission;
        private Predicate<PlayerRef> visible;

        private Builder(@Nonnull String key){
            this.key = key;
            this.name = () -> Message.raw(key);
        }

        /** Sets the picker tile's text from a translation key - the only form that localises. */
        @Nonnull
        public Builder name(@Nonnull String translationKey){
            this.nameKey = translationKey;
            this.name = () -> Text.of(translationKey);
            return this;
        }

        /** Groups this banner under a captioned section in the picker. */
        @Nonnull
        public Builder section(@Nonnull String sectionKey){
            this.sectionKey = sectionKey;
            return this;
        }

        /** The colour drawn on the picker tile. Pick the one that most says "this banner". */
        @Nonnull
        public Builder swatch(int swatch){
            this.swatch = swatch;
            return this;
        }

        /**
         * The SystemId of the {@code .particlesystem} this banner raises. Must be
         * a real asset - an unknown SystemId hard-fails the referencing asset at
         * boot - and must be capped; see this class's note.
         */
        @Nonnull
        public Builder particle(@Nonnull String particleId){
            this.particleId = particleId;
            return this;
        }

        /** Hides this banner from players without this permission. */
        @Nonnull
        public Builder permission(@Nonnull String permission){
            this.permission = permission;
            return this;
        }

        /** Hides this banner unless this returns true. Combines with {@link #permission}; both must pass. */
        @Nonnull
        public Builder visible(@Nonnull Predicate<PlayerRef> visible){
            this.visible = visible;
            return this;
        }

        @Nonnull
        public SectBanner build(){
            if(this.key == null || this.key.isEmpty()){
                throw new IllegalStateException("A sect banner needs an id.");
            }

            // Refused here rather than falling back at spawn time: a banner with
            // no asset would sit in the picker looking choosable and then raise
            // nothing over the hall, which reads as the hall being lost.
            if(this.particleId == null || this.particleId.isEmpty()){
                throw new IllegalStateException("Sect banner '" + this.key
                        + "' names no particle to raise. Give it .particle(\"...\").");
            }

            return new SectBanner(this);
        }
    }
}
