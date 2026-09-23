package plugin.siren.API;

import com.hypixel.hytale.server.core.util.Config;
import plugin.siren.Cultivation;
import plugin.siren.Utils.Config.AlchemyConfig;
import plugin.siren.Utils.Config.AllianceConfig;
import plugin.siren.Utils.Config.BeastConfig;
import plugin.siren.Utils.Config.BodyTemperingConfig;
import plugin.siren.Utils.Config.BreakthroughConfig;
import plugin.siren.Utils.Config.BreedingConfig;
import plugin.siren.Utils.Config.CampaignConfig;
import plugin.siren.Utils.Config.CelestialConfig;
import plugin.siren.Utils.Config.CultivationConfig;
import plugin.siren.Utils.Config.DaoConfig;
import plugin.siren.Utils.Config.DaoComprehensionConfig;
import plugin.siren.Utils.Config.DuelConfig;
import plugin.siren.Utils.Config.DreamTrialConfig;
import plugin.siren.Utils.Config.DungeonConfig;
import plugin.siren.Utils.Config.DwellingConfig;
import plugin.siren.Utils.Config.EndlessLevelingConfig;
import plugin.siren.Utils.Config.ForgingConfig;
import plugin.siren.Utils.Config.FormationConfig;
import plugin.siren.Utils.Config.LifeBoundConfig;
import plugin.siren.Utils.Config.ManualConfig;
import plugin.siren.Utils.Config.MarketConfig;
import plugin.siren.Utils.Config.MasterDiscipleConfig;
import plugin.siren.Utils.Config.OathConfig;
import plugin.siren.Utils.Config.PartnerConfig;
import plugin.siren.Utils.Config.PathWarConfig;
import plugin.siren.Utils.Config.QiDeviationConfig;
import plugin.siren.Utils.Config.RaceConfig;
import plugin.siren.Utils.Config.RaceSystemConfig;
import plugin.siren.Utils.Config.RefinementConfig;
import plugin.siren.Utils.Config.RivalConfig;
import plugin.siren.Utils.Config.SectConfig;
import plugin.siren.Utils.Config.SecretRealmConfig;
import plugin.siren.Utils.Season.SeasonConfig;
import plugin.siren.Utils.Config.SkillTreeConfig;
import plugin.siren.Utils.Config.SpiritCoreConfig;
import plugin.siren.Utils.Config.SpiritVeinConfig;
import plugin.siren.Utils.Config.TalismanConfig;
import plugin.siren.Utils.Config.TeaConfig;
import plugin.siren.Utils.Config.TechniqueConfig;
import plugin.siren.Utils.Config.TideConfig;
import plugin.siren.Utils.Config.TreasureConfig;
import plugin.siren.Utils.Config.TournamentConfig;
import plugin.siren.Utils.Config.UpdateConfig;
import plugin.siren.Utils.Config.WebStoreConfig;
import plugin.siren.Utils.Config.WeaponSpiritConfig;
import plugin.siren.Utils.Config.WarConfig;
import plugin.siren.Utils.Config.WeiqiConfig;
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

    /** The body-tempering ladder: its curve, how armor blunts it, and the reduction it pays back. */
    @Nonnull
    public static Config<BodyTemperingConfig> bodyTempering() {
        return Cultivation.getBodyTemperingConfig();
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

    /** Heavenly Dao (天道), the open Personal Dao registry, and Dao Enlightenment (悟道). */
    public static Config<DaoComprehensionConfig> daoComprehension() {
        return Cultivation.getDaoComprehensionConfig();
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

    /** Talismans - the desk ritual, the grade roll, the mastery ladder. */
    @Nonnull
    public static Config<TalismanConfig> talisman() {
        return Cultivation.getTalismanConfig();
    }

    /**
     * The Teleportation Array network - the master switch, the realm gate,
     * per-destination-kind toggles (Dwelling/Sect-Hall/Secret-Realm/Sea/Heaven),
     * the Qi cost/cooldown model for its two activation surfaces (the placed
     * Array Platform and the reusable Array Compass), and the Platform/hall/
     * dwelling activation radius. See {@link plugin.siren.Utils.Array.ArrayManager}.
     */
    @Nonnull
    public static Config<plugin.siren.Utils.Config.ArrayConfig> array() {
        return Cultivation.getArrayConfig();
    }

    /** Forging - the Forge Anchor ritual, the material ladder, the grade roll. */
    @Nonnull
    public static Config<ForgingConfig> forging() {
        return Cultivation.getForgingConfig();
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

    /** Weapon Spirits - a Life-Bound Treasure's own awakening, level ladder, and bonded art. */
    @Nonnull
    public static Config<WeaponSpiritConfig> weaponSpirit() {
        return Cultivation.getWeaponSpiritConfig();
    }

    /** Spirit beasts - species, taming, growth, and the companion roles. */
    @Nonnull
    public static Config<BeastConfig> beast() {
        return Cultivation.getBeastConfig();
    }

    /** Spirit beast breeding - the pen, the two-cultivator ritual, and incubation. */
    @Nonnull
    public static Config<BreedingConfig> breeding() {
        return Cultivation.getBreedingConfig();
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

    /**
     * Inter-sect diplomacy - the NON_AGGRESSION/TRADE/ALLIANCE relation
     * ladder and the separate unilateral Rivalry table. See
     * {@link plugin.siren.Utils.Sect.AllianceManager} and {@link SectRelationKind}.
     */
    @Nonnull
    public static Config<AllianceConfig> alliance() {
        return Cultivation.getAllianceConfig();
    }

    /** The ad-hoc party system - max size and invite expiry. See PartyManager. */
    public static Config<plugin.siren.Utils.Config.PartyConfig> party() {
        return Cultivation.getPartyConfig();
    }

    /**
     * The party Dungeon/Raid system ("The Sunken Foundry" / "The Foundry
     * Warden") - the master switch, the entrance gate, party sizing, the
     * instance asset name, and the 3-phase boss encounter's HP thresholds
     * and per-mechanic magnitudes. See {@link plugin.siren.Utils.Dungeon.DungeonManager}.
     */
    @Nonnull
    public static Config<DungeonConfig> dungeon() {
        return Cultivation.getDungeonConfig();
    }

    /**
     * Wandering Rival Cultivators - roaming, right-click-challenge, reward-only
     * 1v1 NPC duels. Spawn schedule, lifetime, max health, and the winner's
     * Qi/manual/material payout. See {@link plugin.siren.Utils.Rival.RivalManager}.
     */
    @Nonnull
    public static Config<RivalConfig> rival() {
        return Cultivation.getRivalConfig();
    }

    /**
     * Treasure/Ruin Exploration - Buried Caches (common, in-place claim) and
     * Ruin Vaults (rarer, private-instance entry with 0-2 native guardians).
     * Both tiers discoverable only once Spirit Sense reaches
     * {@code SpiritSenseStage.PRECISE}. See
     * {@link plugin.siren.Utils.Treasure.TreasureCacheManager}.
     */
    @Nonnull
    public static Config<TreasureConfig> treasure() {
        return Cultivation.getTreasureConfig();
    }

    /** The NPC-faction reputation system - tiers, rivalry pairs, opposing-path gain. See FactionManager. */
    public static Config<plugin.siren.Utils.Config.FactionConfig> faction() {
        return Cultivation.getFactionConfig();
    }

    /** Heavenly Oath offer/accept + the breach penalty (Qi loss, karma, the Dao-Heart Flaw). See OathManager. */
    @Nonnull
    public static Config<OathConfig> oath() {
        return Cultivation.getOathConfig();
    }

    /** Partnered Cultivation. Inert unless the Marriage mod is installed - see {@link CultivationAPI#isMarriageInstalled()}. */
    @Nonnull
    public static Config<PartnerConfig> partner() {
        return Cultivation.getPartnerConfig();
    }

    /** The Tea Ceremony (茶道) - rounds, timing windows, herb cost, harmony bands, and the post-ceremony Qi buff. See {@link plugin.siren.Utils.Tea.TeaCeremonyManager}. */
    @Nonnull
    public static Config<TeaConfig> tea() {
        return Cultivation.getTeaConfig();
    }

    /** Weiqi (围棋) - the master switch, turn/reconnect timing, and the Merit payout for a finished match. See {@link plugin.siren.Utils.Weiqi.WeiqiManager}. */
    @Nonnull
    public static Config<WeiqiConfig> weiqi() {
        return Cultivation.getWeiqiConfig();
    }

    /**
     * Secret Realms and the ascension-gated Immortal Court (仙庭) tier on top
     * of them - schedule cadence, duration, Qi multipliers, beast-stocking
     * floors and manual-drop bonuses for BOTH tiers, plus each tier's own
     * atmosphere Weather-Id. See docs/handoff/immortal-court.md for the
     * Court-specific (Court-*) keys and docs/handoff/secret-realms.md for
     * the ordinary tier's own.
     */
    @Nonnull
    public static Config<SecretRealmConfig> secretRealm() {
        return Cultivation.getSecretRealmConfig();
    }

    /**
     * The Dream Trial (幻境劫) at the Hollow Mirror: the site anchor, entry
     * gating, the composure/pulse/failure-mode dials, the Waking Bell charm's
     * charge economy, and the atmosphere Weather-Id. A single hand-built site
     * rather than a rolled Secret Realm - see {@link plugin.siren.Utils.Realm.DreamTrialManager}.
     */
    @Nonnull
    public static Config<DreamTrialConfig> dreamTrial() {
        return Cultivation.getDreamTrialConfig();
    }

    /**
     * The narrative Campaign system ("Roots of Ruin"): the master switch, each
     * chapter's underlying quest-chain numbers, and the fixed world coordinate
     * each named recurring character appears at. See {@link
     * plugin.siren.Utils.Campaign.CampaignManager} and {@link
     * plugin.siren.Utils.Campaign.BuiltInCampaign}.
     */
    @Nonnull
    public static Config<CampaignConfig> campaign() {
        return Cultivation.getCampaignConfig();
    }

    /** The player-run Auction House and the scheduled Traveling Merchant NPC. */
    @Nonnull
    public static Config<MarketConfig> market() {
        return Cultivation.getMarketConfig();
    }

    /**
     * The Beast Tide (兽潮): the automatic scheduler, the warning/wave/resolution
     * phases a siege drives, assault integrity, and the suppression penalty or
     * contribution reward a resolved assault pays out.
     */
    @Nonnull
    public static Config<TideConfig> tide() {
        return Cultivation.getTideConfig();
    }

    /** Master-Disciple bonds (师徒) - offer/accept, the radius-gated Qi bonus, teaching, and breakthrough rewards. */
    @Nonnull
    public static Config<MasterDiscipleConfig> masterDisciple() {
        return Cultivation.getMasterDiscipleConfig();
    }

    /** Qi Deviation (走火入魔) - the lingering post-Heart-Devil affliction, its penalties, and its cures. */
    @Nonnull
    public static Config<QiDeviationConfig> qiDeviation() {
        return Cultivation.getQiDeviationConfig();
    }

    /** The sect tournament (论道大会) - bracket size, the champion payout, and manual/auto-start cadence. */
    @Nonnull
    public static Config<TournamentConfig> tournament() {
        return Cultivation.getTournamentConfig();
    }

    /** Celestial Events (天象) - the shared scheduler, plus Spirit Tide, Meteor Shower, and Blood Moon. */
    @Nonnull
    public static Config<CelestialConfig> celestial() {
        return Cultivation.getCelestialConfig();
    }

    /**
     * Path War (道途之战) - the server-wide, standing Righteous vs Devil
     * kill-count contest: scoring, its own anti-farm floor/cooldown, the
     * Heavenly Schism celestial window's own weight/duration/weather, and the
     * veteran/champion title thresholds. See {@link plugin.siren.Utils.PathWar.PathWarManager}.
     */
    @Nonnull
    public static Config<PathWarConfig> pathWar() {
        return Cultivation.getPathWarConfig();
    }

    /**
     * The Seasonal Cycle: the master switch, season length, and the Hall of
     * Fame's own top-N-per-ladder/history-cap tuning. See {@link
     * plugin.siren.Utils.Season.SeasonManager}.
     */
    @Nonnull
    public static Config<SeasonConfig> season() {
        return Cultivation.getSeasonConfig();
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
     * Whether this server checks for mod updates, how often, and whether
     * administrators are told on join. Governs every mod registered through
     * {@link CultivationAPI#registerUpdateCheck}, not only Cultivation's own.
     */
    @Nonnull
    public static Config<UpdateConfig> update() {
        return Cultivation.getUpdateConfig();
    }

    /**
     * The Treasure Pavilion benefit sync: whether purchases on xianxia.dev are
     * applied to players here, how often the lists are re-read, and whether
     * the recheck command is available.
     */
    @Nonnull
    public static Config<WebStoreConfig> webStore() {
        return Cultivation.getWebStoreConfig();
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
        bodyTempering().save();
        skillTree().save();
        raceSystem().save();
        dao().save();
        technique().save();
        manual().save();
        alchemy().save();
        talisman().save();
        array().save();
        forging().save();
        refinement().save();
        lifeBound().save();
        weaponSpirit().save();
        beast().save();
        breeding().save();
        sect().save();
        formation().save();
        dwelling().save();
        war().save();
        duel().save();
        alliance().save();
        party().save();
        dungeon().save();
        rival().save();
        treasure().save();
        faction().save();
        dreamTrial().save();
        campaign().save();
        oath().save();
        tea().save();
        weiqi().save();
        market().save();
        tide().save();
        partner().save();
        masterDisciple().save();
        qiDeviation().save();
        tournament().save();
        celestial().save();
        pathWar().save();
        season().save();
        endlessLeveling().save();
        update().save();
    }
}
