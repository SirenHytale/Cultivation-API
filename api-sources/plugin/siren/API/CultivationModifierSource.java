package plugin.siren.API;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Dao.DaoElement;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A standing, per-player modifier an addon contributes to Cultivation's own
 * systems - the hook for anything that changes what a cultivator IS rather than
 * what they do.
 *
 * <p>Cultivation already lets an addon react to events and register new content
 * (races, techniques, palettes, titles). What it had no answer for was a
 * permanent quality of a particular cultivator that quietly re-prices a dozen
 * unrelated systems at once - a bloodline, a constitution, a physique. Doing
 * that through events would have meant a listener on every one of them, each
 * re-deriving the same fact; this is one registration answering one question per
 * channel.</p>
 *
 * <h2>How to use it</h2>
 *
 * <p>Implement only the channels you care about - every method has a default
 * that means "no opinion" - and register one instance:</p>
 *
 * <pre>{@code
 * CultivationAPI.registerModifierSource("sacredBodies", new CultivationModifierSource(){
 *     public float qiGainMultiplier(ComponentAccessor<EntityStore> a, Ref<EntityStore> r){
 *         return SacredBody.of(a, r) == null ? 1f : 1.05f;
 *     }
 * });
 * }</pre>
 *
 * <h2>How the numbers combine</h2>
 *
 * <p>Every multiplier channel is combined by MULTIPLICATION across all
 * registered sources, and the neutral value is {@code 1}. Two addons each
 * granting +10% produce ×1.21, not ×1.20 - which is the same way Cultivation's
 * own internal multipliers already stack, and it is what keeps a stack of
 * bonuses from reaching a hard 100% of anything. Boolean channels are combined
 * with OR: one source saying yes is enough.</p>
 *
 * <h2>Threading and cost</h2>
 *
 * <p>Every method is called on the world thread that owns the entity, and may be
 * called from inside the damage pipeline or a per-tick system. Read components
 * through the accessor you are handed, never write through it, and keep them
 * cheap - a lookup and some arithmetic. Anything expensive belongs on your own
 * component, computed when it changes rather than when it is asked for.</p>
 *
 * <p>A source that throws is caught, logged once and treated as having no
 * opinion, so a broken addon cannot take the damage pipeline down with it.</p>
 */
public interface CultivationModifierSource {

    // --- Qi -------------------------------------------------------------------

    /**
     * Scales every point of Qi this cultivator gains, from any source.
     *
     * <p>Applied inside {@code CultivationManager.addQi} alongside race, skill
     * tree, pills, sect and Yin-Yang bonuses. For a Qi gain that should only
     * apply while meditating on particular ground, prefer
     * {@link #meditationQiMultiplier}.</p>
     */
    default float qiGainMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    /**
     * Scales Qi drawn specifically by meditating, on top of
     * {@link #qiGainMultiplier}.
     *
     * @param inLava true when the cultivator is meditating in lava - which is
     *               normally impossible, and is only reachable by a source that
     *               also returns true from {@link #canMeditateInLava}.
     */
    default float meditationQiMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                          boolean inLava){
        return 1f;
    }

    // --- Combat ---------------------------------------------------------------

    /** Scales all damage this cultivator DEALS - weapons, techniques and fists alike. */
    default float damageDealtMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    /**
     * Scales damage this cultivator deals BARE-HANDED, on top of
     * {@link #damageDealtMultiplier}. Applies to real unarmed blows and to the
     * fist arts, and never to a weapon.
     */
    default float unarmedDamageMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    /**
     * Scales damage this cultivator TAKES. Below 1 is protection, above 1 is
     * fragility - a constitution that trades toughness for power returns
     * something over 1 here and over 1 from {@link #damageDealtMultiplier}.
     */
    default float damageTakenMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    /**
     * Whether this cultivator ignores a damage cause outright.
     *
     * @param damageCauseId the DamageCause asset id, e.g. {@code "Fire"}.
     * @return true to cancel the damage entirely. Use sparingly: this is a hard
     * immunity, not a reduction, and a source that returns true for PHYSICAL
     * makes its holder invulnerable to almost everything.
     */
    default boolean isImmuneToDamageCause(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                           @Nonnull String damageCauseId){
        return false;
    }

    // --- Techniques -----------------------------------------------------------

    /**
     * Scales how long a charged art must be gathered for. Below 1 is faster -
     * both the minimum and the maximum of the band shrink together, so the art
     * still reaches full power, just sooner.
     */
    default float chargeSecondsMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    /**
     * Whether this cultivator may perform a dao-locked art belonging to an
     * element they have not comprehended.
     *
     * <p>The one channel that removes a gate rather than moving a number, and it
     * is deliberately all-or-nothing: a constitution that commands every element
     * is not a cultivator with a slightly wider dao, it is one the lock does not
     * describe.</p>
     */
    default boolean ignoresDaoElementLock(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return false;
    }

    // --- Dao ------------------------------------------------------------------

    /**
     * Scales affinity gained toward one dao element - how fast this cultivator
     * walks that particular path.
     *
     * @param element the element affinity is being gained in.
     */
    default float daoAffinityMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                         @Nonnull DaoElement element){
        return 1f;
    }

    /**
     * Scales how far a single alignment shift moves this cultivator, per
     * direction. Below 1 resists that pole; above 1 leans into it.
     *
     * @param towardYin true when the shift is toward Yin, false toward Yang.
     */
    default float alignmentShiftMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                            boolean towardYin){
        return 1f;
    }

    /**
     * Widens the band that counts as a balanced Yin-Yang. Above 1 makes perfect
     * balance easier to hold and its blessing easier to keep.
     */
    default float yinYangBalanceToleranceMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    // --- Ritual ---------------------------------------------------------------

    /**
     * Scales how hard a breakthrough or advancement ritual is. Below 1 is
     * easier: it shortens the ritual and softens the tribulation strikes that
     * punctuate it, which together are what "passing more easily" means.
     */
    default float ritualDifficultyMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    /** Whether this cultivator may meditate while standing in lava. */
    default boolean canMeditateInLava(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return false;
    }

    // --- Presentation ---------------------------------------------------------

    /**
     * Scales the cultivator's realm aura - a larger number is a physically
     * bigger, denser aura. Cosmetic only.
     */
    default float auraScaleMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return 1f;
    }

    /**
     * Extra lines for the Overview page's bonus list, so whatever this source
     * grants is visible to the player rather than being an invisible number.
     *
     * <p>Build them with {@link CultivationAPI#newBonus}. Return an empty list -
     * the default - when this source grants nothing to the cultivator asked
     * about; a source that returns a row of zeroes prints a row of zeroes.</p>
     */
    @Nonnull
    default List<CultivationBonus> bonuses(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return List.of();
    }

    /**
     * Named lines for the Overview page's own Constitution section - what this
     * cultivator IS, as opposed to what it is worth in percentages.
     *
     * <p>Separate from {@link #bonuses} because the two answer different
     * questions and a player wants both: the bonus rows say "+8% Qi", this says
     * "Ancient Sacred Body". Each entry is rendered as its own line, in the order
     * returned.</p>
     *
     * <p>Build them with {@code Message} - typically a translated name, or a
     * name and a qualifier such as a tier. Return an empty list, the default,
     * when this cultivator has nothing from you; the whole section hides itself
     * when no source contributes a line.</p>
     */
    @Nonnull
    default List<com.hypixel.hytale.server.core.Message> traits(@Nonnull ComponentAccessor<EntityStore> accessor,
                                                                @Nonnull Ref<EntityStore> ref){
        return List.of();
    }
}
