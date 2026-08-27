package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import plugin.siren.ECS.Dao.DaoElement;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * One off-ring elemental Dao root a mod has claimed - a display name, and an
 * optional flat counter-cycle bonus percent used in place of DaoConfig's
 * Dao-Counter-Bonus-Percent whenever this element counters another in combat
 * (see {@code plugin.siren.ECS.Systems.DaoCombatSystem}).
 *
 * <p>Register one with {@link CultivationAPI#registerOffRingRoot}. Cultivation
 * ships none - {@link DaoElement}'s off-ring slots (currently just
 * {@link DaoElement#DUCK}) sit inert, unselectable by any player regardless of
 * Dao-Mutant-Roots-Enabled (see {@code plugin.siren.Utils.DaoManager#isRootSelectable}),
 * until a mod claims one:</p>
 *
 * <pre>{@code CultivationAPI.registerOffRingRoot(
 *         DaoRoot.builder(DaoElement.DUCK)
 *                 .name("server.myMod.dao.element.duck")
 *                 .counterBonusPercent(35f)
 *                 .build());}</pre>
 *
 * <h2>Why Core ships the enum slot but not the identity</h2>
 *
 * <p>{@link DaoElement} is a plain Java enum - a mod cannot add a new constant
 * to it. So Core pre-declares the off-ring slot(s) a mod may claim, but takes
 * no further opinion on them: the element-title loop, the "X beats Y" Codex
 * listing and both Dao-page ring rows all skip any
 * {@link DaoElement#isOffRing()} element outright, leaving naming, titles,
 * codex entries and iconography entirely to whichever mod claims the slot
 * through this class.</p>
 */
public final class DaoRoot {

    private final DaoElement element;
    private final Supplier<Message> name;
    private final String nameKey;
    private final Float counterBonusPercent;

    private DaoRoot(Builder builder){
        this.element = builder.element;
        this.name = builder.name;
        this.nameKey = builder.nameKey;
        this.counterBonusPercent = builder.counterBonusPercent;
    }

    @Nonnull
    public static Builder builder(@Nonnull DaoElement element){
        return new Builder(element);
    }

    /** The off-ring element this root claims. */
    @Nonnull
    public DaoElement getElement(){
        return this.element;
    }

    /** This root's display name, resolved fresh per draw so a {@link CultivationTheme} still applies. */
    @Nonnull
    public Message getName(){
        return this.name.get();
    }

    /**
     * The raw translation key behind {@link #getName()}, or null when this
     * root was registered without one - needed anywhere a dropdown entry or
     * title takes a message id rather than an already-resolved {@link Message}.
     */
    @Nullable
    public String getNameKey(){
        return this.nameKey;
    }

    /**
     * This root's flat counter-cycle bonus percent, substituted for
     * DaoConfig's Dao-Counter-Bonus-Percent whenever this element counters
     * another in combat, or {@code fallback} when it states no opinion - the
     * same individually-optional contract {@link CultivationPalette#getSemantic}
     * uses.
     */
    public float getCounterBonusPercent(float fallback){
        return this.counterBonusPercent == null ? fallback : this.counterBonusPercent;
    }

    public static final class Builder {
        private final DaoElement element;
        private Supplier<Message> name;
        private String nameKey;
        private Float counterBonusPercent;

        private Builder(@Nonnull DaoElement element){
            this.element = element;
            this.name = () -> Message.raw(element == null ? "" : element.name());
        }

        /** Sets the display name from a translation key - the only form that localises. */
        @Nonnull
        public Builder name(@Nonnull String translationKey){
            this.nameKey = translationKey;
            this.name = () -> Text.of(translationKey);
            return this;
        }

        /**
         * States this root's own flat counter-cycle bonus percent. Omit to use
         * the server's own Dao-Counter-Bonus-Percent figure.
         */
        @Nonnull
        public Builder counterBonusPercent(float counterBonusPercent){
            this.counterBonusPercent = counterBonusPercent;
            return this;
        }

        @Nonnull
        public DaoRoot build(){
            if(this.element == null){
                throw new IllegalStateException("A Cultivation off-ring Dao root needs an element.");
            }

            if(!this.element.isOffRing()){
                throw new IllegalStateException("Cultivation off-ring Dao root claims '" + this.element.name()
                        + "', which is not an off-ring element - only a DaoElement whose Ring is OFF may be claimed.");
            }

            return new DaoRoot(this);
        }
    }
}
