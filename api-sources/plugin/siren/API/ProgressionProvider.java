package plugin.siren.API;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Components.CultivationComponent;
import plugin.siren.ECS.Realms.CultivationRealm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Replaces Cultivation's own realm/stage/Qi ladder with a different progression
 * system entirely, while keeping every other subsystem in this mod - sects,
 * daos, techniques, spirit beasts, formations, abodes, duels, alchemy, the skill
 * tree - working unchanged on top of it.
 *
 * <p>Install one from your plugin's {@code setup()} with
 * {@link CultivationAPI#setProgressionProvider}. From that moment on, every
 * command, UI page, HUD line and gameplay gate in Cultivation reads its numbers
 * from you instead of from {@link CultivationComponent}: {@code /cultivation}
 * shows your rank names, the HUD shows your XP bar, the meditation ritual runs
 * your rank-ups, and a technique that requires {@code GOLDEN_CORE_FORMATION}
 * asks {@link #getEquivalentRealm} whether the player qualifies.</p>
 *
 * <h2>What you have to store yourself</h2>
 *
 * <p>This interface is deliberately stateless - every method is handed the
 * accessor and ref of the player being asked about, so your own persisted
 * component is the single source of truth for their level and progress. The
 * player's {@link CultivationComponent} still exists (it is what the built-in
 * system uses when no provider is installed, and it keeps a server's pre-addon
 * save data intact if the addon is later removed), but nothing in Cultivation
 * reads its realm/stage/Qi while you are installed. Do not mirror your numbers
 * into it; leave it alone.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every method is called on the world thread of the player in question, from
 * inside a ticking system, a command, or a UI build. Treat the accessor as
 * valid only for the duration of the call. The accessor may be a
 * {@code CommandBuffer} rather than a {@code Store} - so component CREATION must
 * go through it rather than through {@code ref.getStore().putComponent}, which
 * throws "Store is currently processing!" and takes the world down. Reads are
 * fine either way.</p>
 *
 * <h2>Two kinds of rank-up</h2>
 *
 * <p>Cultivation drives rank-ups through a timed meditation ritual, and offers
 * two flavours of it. A provider maps its own progression onto them however it
 * likes:</p>
 *
 * <ul>
 *   <li><b>Advancement</b> - the routine step. Shorter ritual, lower spirit-vein
 *       requirement, no tribulation lightning by default.</li>
 *   <li><b>Breakthrough</b> - the milestone step. Longer ritual, higher
 *       spirit-vein requirement, tribulation lightning (or the Heart-Devil
 *       Trial for a deeply-leaned cultivator).</li>
 * </ul>
 *
 * <p>Only one may be ready at a time; {@link #isReadyForBreakthrough} is tested
 * first. Returning true from neither simply means meditation does its ordinary
 * thing that tick.</p>
 *
 * @see CultivationAPI#setProgressionProvider
 */
public interface ProgressionProvider {

    /**
     * A stable, unique id for this progression system, namespaced with your
     * mod's name (e.g. {@code "SoulRings:spirit_power"}). Only used for logging
     * and for {@code /cultivation admin} to report which system is live.
     */
    @Nonnull
    String getId();

    // --- Level ---

    /**
     * The player's flat, ever-increasing power number. Cultivation uses it for
     * the max-health and damage curves, the cross-player rankings, and anywhere
     * a single "how strong is this cultivator" scalar is needed.
     *
     * <p>It does not have to match the built-in scale (0-27); the config knobs
     * that scale off it (Health-Bonus-Per-Level, Damage-Percent-Bonus-Per-Level)
     * are server-owner tunable, and {@link #getHealthBonus} /
     * {@link #getDamageMultiplier} let you take those curves over outright.</p>
     */
    int getLevel(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /** The highest level obtainable, for progress bars and "%d / %d" displays. */
    int getMaxLevel();

    /**
     * Whether the player is fully maxed out. Meditation stops granting progress
     * and both rank-up rituals stop being offered once this is true.
     */
    boolean isMaxLevel(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    // --- Progress within a level ---

    /** The player's banked progress toward their next level. Shown as "Qi" by the built-in system. */
    float getProgress(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /**
     * How much banked progress the next level costs, AFTER any of your own
     * discounts. Return {@link Float#MAX_VALUE} at max level.
     */
    float getProgressRequiredForNext(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /**
     * Banks progress from one of Cultivation's own sources - meditating on a
     * spirit vein, a Devil-path player kill, an admin {@code /cultivation
     * addqi}. Your system decides what, if anything, those are worth: return
     * without doing anything to refuse the source entirely (which is the right
     * answer for a progression that only advances on kills, for instance).
     *
     * <p>The amount has already been through Cultivation's own multipliers (race,
     * skill tree, pills, sect hall, Yin-Yang) and the cancellable
     * {@code PreQiGainEvent}.</p>
     */
    void addProgress(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                     float amount, @Nullable PlayerRef playerRef);

    // --- Display ---

    /**
     * What to show where the built-in system shows a realm name ("Golden Core
     * Formation") - a rank title, a class, a tier. Appears in the HUD, the stats
     * page, {@code /cultivation info}, and the rankings.
     */
    @Nonnull
    Message getRankLabel(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /**
     * What to show where the built-in system shows a stage name ("Late-Stage") -
     * typically the level number within the rank.
     *
     * @return the label, or {@code null} to show nothing at all in that slot.
     */
    @Nullable
    Message getSubRankLabel(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /**
     * The same rank label as {@link #getRankLabel}, but for a player who is not
     * loaded - the server-wide rankings list every cultivator who has ever been
     * seen, including everyone currently offline, so they have only that
     * player's recorded {@link #getLevel} to go on.
     *
     * <p>Implement this whenever level alone determines the rank, which it
     * usually does. The default returns {@code null}, which leaves the rankings
     * falling back to the realm names recorded alongside the level.</p>
     */
    @Nullable
    default Message getRankLabelForLevel(int level){
        return null;
    }

    /**
     * The offline counterpart of {@link #getSubRankLabel}, for the same reason
     * as {@link #getRankLabelForLevel} - and only consulted when that method
     * returned a label, since the two fill the two halves of one "{rank}
     * ({subRank})" line.
     */
    @Nullable
    default Message getSubRankLabelForLevel(int level){
        return null;
    }

    // --- Profiles ---

    /**
     * Whether this provider can keep its own progression in step with
     * Cultivation's profiles - the separate saves a player keeps of their
     * progress.
     *
     * <p><b>Returns false by default, and that default refuses profile switching
     * outright while this provider is installed.</b> The reason is that a switch
     * replaces the components describing what a cultivator has become, and when
     * a provider owns progression the player's real level is not in those
     * components at all - it is in the provider. Swapping without the provider's
     * participation would leave the two disagreeing: Cultivation showing a fresh
     * cultivator while the provider still had them at their old level. Refusing
     * is the safe reading, and it is what a provider that has not thought about
     * profiles gets for free.</p>
     *
     * <p>Return true once you handle {@link ProfileEvents.PreProfileSwitchEvent}
     * (save your state for the profile being left) and
     * {@link ProfileEvents.ProfileSwitchEvent} (load it for the one arriving).
     * With those two in place a switch is safe, and Cultivation stops standing in
     * the way of it.</p>
     *
     * <p>The permission-gated sandbox profile is allowed either way - its realm
     * is set by hand rather than earned, and it is kept off every ranking - but
     * a provider that returns false is told, through the message the player sees,
     * that setting a realm there changes Cultivation's view alone.</p>
     */
    default boolean supportsProfiles(){
        return false;
    }

    // --- Gating ---

    /**
     * Which realm this player counts as having reached, for every realm gate in
     * Cultivation and its config files: a technique's or race's Unlock-Realm, a
     * beast species' Min-Realm, {@code Pk-Min-Victim-Realm}, the Dao's
     * switch-cost scaling, Sword Flying's per-realm speed, abode quality, and
     * anything an addon gates with {@link CultivationAPI#getRealm}.
     *
     * <p>Map your own ladder onto the seven realms however suits it - a straight
     * proportional split of your level range is usually right. Returning a
     * constant works too, but then every realm-gated feature in the mod is
     * either permanently open or permanently shut.</p>
     */
    @Nonnull
    CultivationRealm getEquivalentRealm(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    // --- Rank-up rituals ---

    /** Whether the player has banked enough to attempt the milestone ritual. Tested before {@link #isReadyForAdvancement}. */
    boolean isReadyForBreakthrough(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /** Whether the player has banked enough to attempt the routine ritual. */
    boolean isReadyForAdvancement(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /** How long the milestone ritual should take, in seconds of uninterrupted meditation. */
    float getBreakthroughDurationSeconds(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /** How long the routine ritual should take, in seconds of uninterrupted meditation. */
    float getAdvancementDurationSeconds(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /**
     * The milestone ritual finished: spend the banked progress and rank the
     * player up. Cultivation still handles what surrounds it - skill points, the
     * celebration particle, the HUD refresh, and firing {@code BreakthroughEvent}
     * - so this method only has to move your own numbers and tell the player.
     */
    void completeBreakthrough(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                              @Nullable PlayerRef playerRef);

    /** The routine ritual finished. Same contract as {@link #completeBreakthrough}. */
    void completeAdvancement(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                             @Nullable PlayerRef playerRef);

    /**
     * A ritual was failed - walked out of, or lost to Qi Deviation. The built-in
     * system drops the player one sub-stage and wipes their banked Qi; do
     * whatever the equivalent punishment is for yours.
     *
     * @param wasBreakthrough true if the failed ritual was the milestone one.
     */
    void demote(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                @Nullable PlayerRef playerRef, boolean wasBreakthrough);

    // --- Stat curves ---

    /**
     * Additive max health for this player's progression, replacing the built-in
     * {@code level * Health-Bonus-Per-Level}. Applied as a keyed stat modifier,
     * so returning a smaller number later correctly shrinks the bonus.
     *
     * <p>The default keeps Cultivation's own curve, driven by
     * {@link #getLevel} - override only if your levels want a different shape.</p>
     */
    default float getHealthBonus(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                 float configuredHealthPerLevel){
        return getLevel(accessor, ref) * configuredHealthPerLevel;
    }

    /**
     * This player's outgoing damage multiplier from progression alone, replacing
     * the built-in {@code 1 + level * Damage-Percent-Bonus-Per-Level}. Race,
     * skill tree and technique multipliers are applied by Cultivation on top of
     * whatever you return, so return 1.0 for "no bonus", never 0.
     */
    default float getDamageMultiplier(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                      float configuredDamagePercentPerLevel){
        return 1f + (getLevel(accessor, ref) * (configuredDamagePercentPerLevel / 100f));
    }
}
