package plugin.siren.API;

import com.hypixel.hytale.server.core.util.Config;
import plugin.siren.Cultivation;
import plugin.siren.Utils.Config.AlchemyConfig;
import plugin.siren.Utils.Config.BeastConfig;
import plugin.siren.Utils.Config.BreakthroughConfig;
import plugin.siren.Utils.Config.CultivationConfig;
import plugin.siren.Utils.Config.DaoConfig;
import plugin.siren.Utils.Config.DuelConfig;
import plugin.siren.Utils.Config.DwellingConfig;
import plugin.siren.Utils.Config.EndlessLevelingConfig;
import plugin.siren.Utils.Config.FormationConfig;
import plugin.siren.Utils.Config.LifeBoundConfig;
import plugin.siren.Utils.Config.ManualConfig;
import plugin.siren.Utils.Config.PartnerConfig;
import plugin.siren.Utils.Config.RaceConfig;
import plugin.siren.Utils.Config.RaceSystemConfig;
import plugin.siren.Utils.Config.RefinementConfig;
import plugin.siren.Utils.Config.SectConfig;
import plugin.siren.Utils.Config.SkillTreeConfig;
import plugin.siren.Utils.Config.SpiritCoreConfig;
import plugin.siren.Utils.Config.SpiritVeinConfig;
import plugin.siren.Utils.Config.TechniqueConfig;
import plugin.siren.Utils.Config.WarConfig;
import plugin.siren.ECS.Races.PlayerRace;

import javax.annotation.Nonnull;

/**
 * Every one of Cultivation's config files, reachable from an addon.
 *
 * <p>This is the read/write half of the integration surface that
 * {@link CultivationAPI}'s events are the react half of. An addon that wants to
 * KNOW a number ("how much Qi does a breakthrough need on this server?") reads
 * it here; one that wants to CHANGE a number permanently writes it here and
 * calls {@code save()} on the holder; one that wants to change it only for one
 * player, or only this once, uses the matching {@code Pre*} event instead.</p>
 *
 * <h2>Read through the holder, never around it</h2>
 *
 * <p>Each accessor returns the live {@code Config<T>} HOLDER rather than the
 * config object. Hold the holder, not what {@code get()} returned - a config
 * reload (an admin pressing Save, or the file being edited on disk) replaces the
 * instance behind it, and a captured instance would then be a discarded copy
 * whose edits go nowhere. In practice that means writing
 * {@code CultivationConfigs.dao().get().isDaoEnabled()} at the point of use
 * rather than caching a {@code DaoConfig} field.</p>
 *
 * <h2>Writing</h2>
 *
 * <pre>{@code var holder = CultivationConfigs.spiritVein();
 * holder.get().setSpiritVeinRegenPerSecond(2.5f);
 * holder.save();}</pre>
 *
 * <p>Persisting is the caller's job, so a batch of edits costs one file write
 * rather than one per setter. A value changed without saving is live for this
 * session and lost on restart, which is occasionally what you want.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Reads are safe from anywhere. Writes are safe from a plugin's
 * {@code setup()} and from any world thread, on the same terms Cultivation's own
 * admin menu writes them: the config objects are plain mutable holders with no
 * cross-thread coordination, so two threads racing to write the same field is a
 * last-writer-wins situation rather than a corrupt one.</p>
 */
public final class CultivationConfigs {

    private CultivationConfigs() {
    }

    // --- Progression ---

    /** The XP curve and the per-level health/damage bonuses. */
    @Nonnull
    public static Config<CultivationConfig> cultivation() {
        return Cultivation.getCultivationConfig();
    }

    /** The three core tiers' drop chances and Qi values. */
    @Nonnull
    public static Config<SpiritCoreConfig> spiritCores() {
        return Cultivation.getSpiritCoreConfig();
    }

    /** Vein seeding, regeneration, drain, tiers, and Spirit Sense. */
    @Nonnull
    public static Config<SpiritVeinConfig> spiritVein() {
        return Cultivation.getSpiritVeinConfig();
    }

    /** Breakthrough and advancement rituals, tribulation, and the Heart-Devil Trial. */
    @Nonnull
    public static Config<BreakthroughConfig> breakthrough() {
        return Cultivation.getBreakthroughConfig();
    }

    /** Skill point grants, the stat caps, and respec. */
    @Nonnull
    public static Config<SkillTreeConfig> skillTree() {
        return Cultivation.getSkillTreeConfig();
    }

    /** Cross-race behaviour - notably whether an admin's setrace bypasses the realm gate. */
    @Nonnull
    public static Config<RaceSystemConfig> raceSystem() {
        return Cultivation.getRaceSystemConfig();
    }

    /**
     * One race's own stat block. Every registered race has one, including races
     * another mod added through {@link CultivationAPI#registerRace}.
     */
    @Nonnull
    public static RaceConfig race(@Nonnull PlayerRace race) {
        return Cultivation.getRaceConfig(race);
    }

    // --- Arts ---

    /** Elements, drift, Yin-Yang, moral paths, karma, and weather resonance. */
    @Nonnull
    public static Config<DaoConfig> dao() {
        return Cultivation.getDaoConfig();
    }

    /** The technique system's master switches, keybind gating, and per-technique rule overrides. */
    @Nonnull
    public static Config<TechniqueConfig> technique() {
        return Cultivation.getTechniqueConfig();
    }

    /** Lootable manuals and what they teach. */
    @Nonnull
    public static Config<ManualConfig> manual() {
        return Cultivation.getManualConfig();
    }

    /** Pills - their effects, durations and charges. */
    @Nonnull
    public static Config<AlchemyConfig> alchemy() {
        return Cultivation.getAlchemyConfig();
    }

    /** Weapon refinement and dao affinity tempering. */
    @Nonnull
    public static Config<RefinementConfig> refinement() {
        return Cultivation.getRefinementConfig();
    }

    /** Life-Bound Treasures - per-item weapon and armor growth. */
    @Nonnull
    public static Config<LifeBoundConfig> lifeBound() {
        return Cultivation.getLifeBoundConfig();
    }

    /** Spirit beasts - species, taming, growth, and the companion roles. */
    @Nonnull
    public static Config<BeastConfig> beast() {
        return Cultivation.getBeastConfig();
    }

    // --- Society ---

    /** Sects - size, halls, bonuses, invites, inscriptions. */
    @Nonnull
    public static Config<SectConfig> sect() {
        return Cultivation.getSectConfig();
    }

    /** Spirit arrays - what each formation costs and does. */
    @Nonnull
    public static Config<FormationConfig> formation() {
        return Cultivation.getFormationConfig();
    }

    /** Cave Abodes, Spirit Springs, upkeep and seclusion. */
    @Nonnull
    public static Config<DwellingConfig> dwelling() {
        return Cultivation.getDwellingConfig();
    }

    /** Sect wars and vein sieges. */
    @Nonnull
    public static Config<WarConfig> war() {
        return Cultivation.getWarConfig();
    }

    /** Duels and their wagers. */
    @Nonnull
    public static Config<DuelConfig> duel() {
        return Cultivation.getDuelConfig();
    }

    /** Partnered Cultivation. Inert unless the Marriage mod is installed - see {@link CultivationAPI#isMarriageInstalled()}. */
    @Nonnull
    public static Config<PartnerConfig> partner() {
        return Cultivation.getPartnerConfig();
    }

    // --- Compatibility ---

    /**
     * How Cultivation shares the stats it raises with Endless Leveling. Inert
     * unless EL is installed - see {@link CultivationAPI#isEndlessLevelingInstalled()}.
     */
    @Nonnull
    public static Config<EndlessLevelingConfig> endlessLeveling() {
        return Cultivation.getEndlessLevelingConfig();
    }

    /**
     * Writes every config file to disk. Rarely what you want - prefer saving the
     * one holder you edited - but useful after a bulk rewrite.
     */
    public static void saveAll() {
        cultivation().save();
        spiritCores().save();
        spiritVein().save();
        breakthrough().save();
        skillTree().save();
        raceSystem().save();
        dao().save();
        technique().save();
        manual().save();
        alchemy().save();
        refinement().save();
        lifeBound().save();
        beast().save();
        sect().save();
        formation().save();
        dwelling().save();
        war().save();
        duel().save();
        partner().save();
        endlessLeveling().save();
    }
}
