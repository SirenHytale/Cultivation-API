package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One look a player's meditation can wear: the particles raised while they sit
 * drawing Qi, and (independently) the ones raised at the moment a breakthrough
 * or advancement completes.
 *
 * <p>Register one with {@link CultivationAPI#registerMeditationAura}. Cultivation
 * ships none by default - every cultivator meditates in the stock look until they
 * choose otherwise - and a mod may add as many as it likes:</p>
 *
 * <pre>{@code CultivationAPI.registerMeditationAura(
 *         CultivationMeditationAura.builder("myMod:frostBreath")
 *                 .name("server.myMod.meditationAura.frostBreath")
 *                 .section("server.myMod.meditationAura.section.winter")
 *                 .swatch(0x9FD8F0)
 *                 .auraPrefix("MyMod_MeditationAura_")
 *                 .build());}</pre>
 *
 * <h2>Why this is independent of {@link CultivationPalette}</h2>
 *
 * <p>A palette re-grades the menus and the HUD - the things a player is looking
 * <i>at</i>. A meditation aura is what a player looks <i>like</i> while sitting,
 * breaking through, or advancing, which has nothing to do with which documents a
 * page draws from. Keeping the two choices separate means a player can wear the
 * default crimson-and-gold menus and still meditate under someone else's aura, or
 * the reverse - neither choice constrains the other.</p>
 *
 * <h2>Why the meditation-tick particle is a prefix, not a fixed id</h2>
 *
 * <p>{@code CultivationMeditationSystem} does not raise one meditation particle -
 * it raises whichever of three tiers the current session has earned
 * ({@code Cultivation_MeditationAura_Stirring/Gathering/Converging}), the same way
 * {@link CultivationPalette#resolveAura} recolors the standing realm aura by
 * swapping only the suffix after a base id's last underscore. {@link #resolveMeditationTick}
 * copies that exact contract - the same prefix carries all three tiers, so a
 * player who chose one look sees it deepen through a session the same way the
 * built-in tiers do, rather than jumping to an unrelated asset partway through.</p>
 *
 * <h2>Why breakthrough and advancement are each a single, optional override</h2>
 *
 * <p>Unlike the tiered meditation particle, the ritual pulses at breakthrough and
 * advancement are each one fixed asset in the base mod - there is no suffix to
 * swap. An aura may override either, both, or neither; whichever it leaves unset
 * simply falls through to Cultivation's own constant for that moment, the same
 * "individually optional" contract {@link CultivationPalette.Builder#semantic}
 * uses for its own per-meaning colors.</p>
 *
 * <h2>Nothing here is validated against a real asset</h2>
 *
 * <p>Same reason as {@link CultivationPalette#resolveAura}: the client that would
 * confirm a SystemId resolves is not readable from here, so an id that names
 * nothing simply raises nothing at that moment rather than failing loudly. Test
 * a newly registered aura in-game before shipping it.</p>
 *
 * <h2>What happens when the mod that added one goes away</h2>
 *
 * <p>Nothing breaks. A player's chosen aura is stored as an id, never as this
 * object, and an id nobody claims resolves back to Cultivation's own built-in
 * particles at every one of the three moments - exactly how an unclaimed palette
 * or title id falls back today.</p>
 */
public final class CultivationMeditationAura {

    /**
     * A picker-only marker for "no aura chosen" - Cultivation's own built-in
     * particles. Never stored; the settings picker translates it back to null
     * before calling {@link CultivationAPI#setMeditationAura}, the same way
     * {@link CultivationPalette#DEFAULT_KEY} is translated back before
     * {@code setPaletteId}. Needed because a dropdown has to select
     * <i>something</i>, and "no aura" is not a registered key.
     */
    public static final String DEFAULT_KEY = "cultivation:meditationAuraDefault";

    private final String key;
    private final Supplier<Message> name;
    private final String nameKey;
    private final String sectionKey;
    private final int swatch;
    private final String auraPrefix;
    private final String breakthroughParticleId;
    private final String advancementParticleId;
    private final String permission;
    private final Predicate<PlayerRef> visible;

    private CultivationMeditationAura(Builder builder){
        this.key = builder.key;
        this.name = builder.name;
        this.nameKey = builder.nameKey;
        this.sectionKey = builder.sectionKey;
        this.swatch = builder.swatch;
        this.auraPrefix = builder.auraPrefix;
        this.breakthroughParticleId = builder.breakthroughParticleId;
        this.advancementParticleId = builder.advancementParticleId;
        this.permission = builder.permission;
        this.visible = builder.visible;
    }

    @Nonnull
    public static Builder builder(@Nonnull String key){
        return new Builder(key);
    }

    /** The id this aura is registered under, and what is saved on the player. */
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
     * The raw translation key behind {@link #getName()}, or null if this aura was
     * never given one - needed because a dropdown entry takes a message id the
     * client resolves itself, not a resolved {@link Message}. Same reasoning as
     * {@link SectBanner#getNameKey()}.
     */
    @Nullable
    public String getNameKey(){
        return this.nameKey;
    }

    /** Translation key of the caption this aura groups under, or null for ungrouped. */
    @Nullable
    public String getSectionKey(){
        return this.sectionKey;
    }

    /** RGB shown on the picker tile, so the grid can be scanned by eye. */
    public int getSwatch(){
        return this.swatch;
    }

    /** @return this aura's meditation-tick prefix, or null if it keeps Cultivation's own tiers. */
    @Nullable
    public String getAuraPrefix(){
        return this.auraPrefix;
    }

    /**
     * This aura's id for one of the three meditation-tick tiers, or {@code baseId}
     * unchanged when it ships no prefix.
     *
     * <p>Copies {@link CultivationPalette#resolveAura}'s exact contract: the suffix
     * after {@code baseId}'s last underscore - the part that names the tier - is
     * carried onto this aura's own prefix. Falls back to {@code baseId} itself,
     * never to null, so a caller that gets no aura back from
     * {@link CultivationAPI#getMeditationAura} and one whose aura declines to
     * override the tick can share the same call site.</p>
     *
     * @param baseId the id Cultivation would otherwise spawn for the current tier,
     *               e.g. {@code Cultivation_MeditationAura_Gathering}.
     */
    @Nonnull
    public String resolveMeditationTick(@Nonnull String baseId){
        if(this.auraPrefix == null){
            return baseId;
        }

        int underscore = baseId.lastIndexOf('_');
        if(underscore < 0 || underscore == baseId.length() - 1){
            return baseId;
        }

        return this.auraPrefix + baseId.substring(underscore + 1);
    }

    /**
     * This aura's breakthrough-ritual particle, or {@code fallback} when it states
     * no opinion - the same individually-optional contract
     * {@link CultivationPalette#getSemantic} uses.
     */
    @Nonnull
    public String resolveBreakthroughParticle(@Nonnull String fallback){
        return this.breakthroughParticleId == null ? fallback : this.breakthroughParticleId;
    }

    /** This aura's advancement-ritual particle, or {@code fallback} when it states no opinion. */
    @Nonnull
    public String resolveAdvancementParticle(@Nonnull String fallback){
        return this.advancementParticleId == null ? fallback : this.advancementParticleId;
    }

    /**
     * Whether this player may choose this aura. False hides it from the picker and
     * refuses to apply it, so a hand-crafted event packet cannot reach an aura its
     * owner was never offered.
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
        private String auraPrefix;
        private String breakthroughParticleId;
        private String advancementParticleId;
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

        /** Groups this aura under a captioned section in the picker. */
        @Nonnull
        public Builder section(@Nonnull String sectionKey){
            this.sectionKey = sectionKey;
            return this;
        }

        /** The colour drawn on the picker tile. Pick the one that most says "this aura". */
        @Nonnull
        public Builder swatch(int swatch){
            this.swatch = swatch;
            return this;
        }

        /**
         * The id prefix this aura's own meditation-tick particles are named with -
         * e.g. {@code "MyMod_MeditationAura_"} is asked for
         * {@code MyMod_MeditationAura_Stirring/Gathering/Converging} as a session
         * deepens. Omit to keep Cultivation's own three tiers.
         */
        @Nonnull
        public Builder auraPrefix(@Nonnull String auraPrefix){
            this.auraPrefix = auraPrefix;
            return this;
        }

        /** Overrides the particle raised when a breakthrough ritual starts or pulses. Omit to keep Cultivation's own. */
        @Nonnull
        public Builder breakthroughParticle(@Nonnull String particleId){
            this.breakthroughParticleId = particleId;
            return this;
        }

        /** Overrides the particle raised when an advancement ritual starts or pulses. Omit to keep Cultivation's own. */
        @Nonnull
        public Builder advancementParticle(@Nonnull String particleId){
            this.advancementParticleId = particleId;
            return this;
        }

        /** Hides this aura from players without this permission. */
        @Nonnull
        public Builder permission(@Nonnull String permission){
            this.permission = permission;
            return this;
        }

        /** Hides this aura unless this returns true. Combines with {@link #permission}; both must pass. */
        @Nonnull
        public Builder visible(@Nonnull Predicate<PlayerRef> visible){
            this.visible = visible;
            return this;
        }

        @Nonnull
        public CultivationMeditationAura build(){
            if(this.key == null || this.key.isEmpty()){
                throw new IllegalStateException("A Cultivation meditation aura needs an id.");
            }

            if(this.auraPrefix == null && this.breakthroughParticleId == null && this.advancementParticleId == null){
                throw new IllegalStateException("Cultivation meditation aura '" + this.key
                        + "' overrides nothing - set .auraPrefix(...), .breakthroughParticle(...) or"
                        + " .advancementParticle(...), or it would sit in the picker looking choosable"
                        + " and change nothing.");
            }

            return new CultivationMeditationAura(this);
        }
    }
}
