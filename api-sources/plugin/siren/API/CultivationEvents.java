package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Races.PlayerRace;
import plugin.siren.ECS.Realms.CultivationRealm;
import plugin.siren.ECS.Realms.CultivationStage;
import plugin.siren.ECS.SkillTree.SkillNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Core progression events - cultivation Qi, rituals, breakthroughs,
 * advancements, tribulations, races and the skill tree. Register a listener
 * once from your plugin's setup() and it fires every time the corresponding
 * thing happens to any player.
 *
 * <p>The other subsystems have their own classes in this package:
 * {@link SectEvents}, {@link WarEvents}, {@link DuelEvents},
 * {@link FormationEvents}, {@link DwellingEvents}, {@link BeastEvents},
 * {@link TechniqueEvents}, {@link DaoEvents} and {@link ItemEvents}. All of
 * them follow exactly the conventions described here.</p>
 *
 * <p><b>Pre vs post.</b> Nearly everything is exposed twice. A {@code Pre*}
 * event fires BEFORE the change, extends {@link CancellableEvent}, and lets a
 * listener veto it outright ({@code setCancelled(true)}) or re-tune the numbers
 * driving it (a breakthrough's Qi cost, a tribulation bolt's damage, the Qi a
 * meditation tick banks). Whatever the listeners leave in those fields is what
 * the mod then uses - that is the supported way to reshape a mechanic from an
 * addon. The matching post-event is a plain record fired once the change is
 * committed; it cannot be cancelled and is purely a notification. A cancelled
 * pre-event means the post-event never fires.</p>
 *
 * <p>Usage: {@code CultivationEvents.onPreBreakthrough(event -> {
 * if(!myPlugin.mayAscend(event.player())) event.setCancelled(true); });}</p>
 *
 * <p><b>Threading and safety.</b> Listeners are invoked synchronously on the
 * world thread of the player the event happened to. For a post-event the change
 * has already been applied (a BreakthroughEvent's component state already shows
 * the new realm); for a pre-event nothing has been applied yet. Component reads
 * via the usual accessors are safe inside a listener, but do NOT block, and hop
 * threads yourself (e.g. {@code CompletableFuture.runAsync(task, otherWorld)})
 * before touching anything that lives on another world. A listener that throws
 * is logged and skipped so one broken addon can't break the mod's own systems
 * (or other addons' listeners). Registration is a plain CopyOnWriteArrayList -
 * safe to call from any plugin's setup() in any load order, same guarantee as
 * CultivationAPI's registries; there is deliberately no unregister (listener
 * lifetime = server lifetime, matching how plugins load once and stay).</p>
 */
public final class CultivationEvents {
    private CultivationEvents(){}

    /** Which timed meditation ritual a ritual event refers to. */
    public enum RitualType {
        /** Peak stage -> next realm. */
        BREAKTHROUGH,
        /** A sub-stage step within the current realm. */
        ADVANCEMENT,
        /** Weapon refinement (炼器) - tempering the held weapon rather than the cultivator. */
        REFINEMENT
    }

    /** Why a player stopped meditating. */
    public enum MeditationStopReason {
        /** They toggled it off themselves with /cultivation meditate. */
        COMMAND,
        /** They wandered away from the spot they sat down at. */
        MOVEMENT
    }

    // --- Post-event payloads (notifications; cannot be cancelled) ---

    /** A player completed a realm breakthrough; {@code newRealm} is the realm they just entered (their stage is EARLY). {@code player} is null only if the PlayerRef component was unavailable. */
    public record BreakthroughEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull CultivationRealm newRealm) {}

    /** A player completed a sub-stage advancement within {@code realm}, landing on {@code newStage}. */
    public record AdvancementEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull CultivationRealm realm, @Nonnull CultivationStage newStage) {}

    /** A player's race changed - via the race menu ({@code adminOverride} false) or an admin tool ({@code adminOverride} true). Not fired when an admin "sets" the race the player already has. */
    public record RaceChangeEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull PlayerRace oldRace, @Nonnull PlayerRace newRace, boolean adminOverride) {}

    /** A player unlocked a skill tree node (points already spent, modifiers already re-applied). */
    public record SkillUnlockEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull SkillNode node) {}

    /** Tribulation lightning struck a mid-ritual cultivator. {@code damage} is the post-lethality-cap amount fed to the damage pipeline (pre-armor/reduction); {@code breakthroughRitual} distinguishes breakthrough strikes from (config-gated) advancement ones. */
    public record TribulationStrikeEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float damage, boolean breakthroughRitual) {}

    /** A Life-Bound Treasure gained a level from combat XP. {@code item} is the already-updated stack (its metadata reflects {@code newLevel}). */
    public record LifeBoundLevelUpEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, int newLevel) {}

    /** The Heart-Devil Trial tormented a deeply-leaned cultivator mid-ritual. {@code composureRemaining} is what's left after this pulse's drain (0 when it broke); {@code deviated} is true only on the pulse that shattered composure into Qi Deviation; {@code breakthroughRitual} distinguishes breakthrough trials from (opt-in) advancement ones. */
    public record HeartDevilTrialEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float composureRemaining, boolean deviated, boolean breakthroughRitual) {}

    /** Qi was just banked toward a player's next rank-up. {@code amount} is what was actually added (after every race/skill/pill/sect/dao multiplier and after any listener retune); {@code totalQi} is their new banked total. */
    public record QiGainEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float amount, float totalQi) {}

    /** A player sat down to meditate. */
    public record MeditationStartEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player) {}

    /** A player stopped meditating. Any ritual penalty for standing up mid-ritual has already been applied. */
    public record MeditationStopEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull MeditationStopReason reason) {}

    /** A timed meditation ritual just began (the tick that first accrued progress). */
    public record RitualStartEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull RitualType type) {}

    /** A player was demoted a sub-stage for abandoning a ritual (or for Qi Deviation). Banked Qi has been wiped and the granting skill points revoked. */
    public record DemotionEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull CultivationRealm realm,
                                @Nonnull CultivationStage oldStage, @Nonnull CultivationStage newStage, boolean wasBreakthrough) {}

    /** A cultivator's composure shattered into Qi Deviation (走火入魔). Exactly one of {@code demoted}/{@code qiLost} carries the penalty that was applied. */
    public record QiDeviationEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, boolean demoted, float qiLost, boolean breakthroughRitual) {}

    /** A player respecced their skill tree; every node was cleared and {@code refundedPoints} handed back. */
    public record RespecEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, int refundedPoints) {}

    // --- Pre-event payloads (cancellable; numbers are re-tunable) ---

    /** A player is about to complete a realm breakthrough. Cancel to hold them at Peak stage (their ritual progress resets and they may retry); adjust {@link #setQiCost} to change what the breakthrough consumes. */
    public static final class PreBreakthroughEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final CultivationRealm fromRealm;
        private final CultivationRealm toRealm;
        private float qiCost;

        public PreBreakthroughEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                    @Nonnull CultivationRealm fromRealm, @Nonnull CultivationRealm toRealm, float qiCost){
            this.ref = ref;
            this.player = player;
            this.fromRealm = fromRealm;
            this.toRealm = toRealm;
            this.qiCost = qiCost;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        /** The realm they are leaving. */
        @Nonnull public CultivationRealm fromRealm(){ return this.fromRealm; }
        /** The realm they are about to enter. */
        @Nonnull public CultivationRealm toRealm(){ return this.toRealm; }
        /** Banked Qi this breakthrough will consume. */
        public float qiCost(){ return this.qiCost; }
        public void setQiCost(float qiCost){ this.qiCost = qiCost; }
    }

    /** A player is about to complete a sub-stage advancement. Cancel to hold them where they are; adjust {@link #setQiCost} to change what it consumes. */
    public static final class PreAdvancementEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final CultivationRealm realm;
        private final CultivationStage fromStage;
        private final CultivationStage toStage;
        private float qiCost;

        public PreAdvancementEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull CultivationRealm realm,
                                   @Nonnull CultivationStage fromStage, @Nonnull CultivationStage toStage, float qiCost){
            this.ref = ref;
            this.player = player;
            this.realm = realm;
            this.fromStage = fromStage;
            this.toStage = toStage;
            this.qiCost = qiCost;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public CultivationRealm realm(){ return this.realm; }
        @Nonnull public CultivationStage fromStage(){ return this.fromStage; }
        @Nonnull public CultivationStage toStage(){ return this.toStage; }
        /** Banked Qi this advancement will consume. */
        public float qiCost(){ return this.qiCost; }
        public void setQiCost(float qiCost){ this.qiCost = qiCost; }
    }

    /** A player's race is about to change. Cancel to keep their current race (the race menu simply reports no change). */
    public static final class PreRaceChangeEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final PlayerRace oldRace;
        private final PlayerRace newRace;
        private final boolean adminOverride;

        public PreRaceChangeEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                  @Nonnull PlayerRace oldRace, @Nonnull PlayerRace newRace, boolean adminOverride){
            this.ref = ref;
            this.player = player;
            this.oldRace = oldRace;
            this.newRace = newRace;
            this.adminOverride = adminOverride;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public PlayerRace oldRace(){ return this.oldRace; }
        @Nonnull public PlayerRace newRace(){ return this.newRace; }
        /** True when an admin command drove the change rather than the player's own race menu. */
        public boolean adminOverride(){ return this.adminOverride; }
    }

    /** A player is about to unlock a skill tree node. Cancel to refuse it (their points are not spent); adjust {@link #setPointCost} to change the price. */
    public static final class PreSkillUnlockEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final SkillNode node;
        private int pointCost;

        public PreSkillUnlockEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull SkillNode node, int pointCost){
            this.ref = ref;
            this.player = player;
            this.node = node;
            this.pointCost = pointCost;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public SkillNode node(){ return this.node; }
        /** Skill points this unlock will spend. */
        public int pointCost(){ return this.pointCost; }
        public void setPointCost(int pointCost){ this.pointCost = pointCost; }
    }

    /** Tribulation lightning is about to strike a mid-ritual cultivator. Cancel to spare them entirely (no bolt, no thunder, no damage); set {@link #setDamage} to 0 to let the bolt fall harmlessly. The damage here is pre-armor/reduction. */
    public static final class PreTribulationStrikeEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final boolean breakthroughRitual;
        private float damage;

        public PreTribulationStrikeEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float damage, boolean breakthroughRitual){
            this.ref = ref;
            this.player = player;
            this.damage = damage;
            this.breakthroughRitual = breakthroughRitual;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        /** Post-lethality-cap damage the bolt will deal, before armor and reduction filters. */
        public float damage(){ return this.damage; }
        public void setDamage(float damage){ this.damage = damage; }
        public boolean breakthroughRitual(){ return this.breakthroughRitual; }
    }

    /** A Life-Bound Treasure is about to level up. Cancel to hold it at its current level (the XP is still banked). */
    public static final class PreLifeBoundLevelUpEvent extends CancellableEvent {
        private final PlayerRef owner;
        private final ItemStack item;
        private final int oldLevel;
        private final int newLevel;

        public PreLifeBoundLevelUpEvent(@Nonnull PlayerRef owner, @Nonnull ItemStack item, int oldLevel, int newLevel){
            this.owner = owner;
            this.item = item;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }

        @Nonnull public PlayerRef owner(){ return this.owner; }
        /** The stack as it stands BEFORE the level-up is written into its metadata. */
        @Nonnull public ItemStack item(){ return this.item; }
        public int oldLevel(){ return this.oldLevel; }
        public int newLevel(){ return this.newLevel; }
    }

    /** A Heart-Devil pulse is about to torment a mid-ritual cultivator. Cancel to skip the pulse entirely; adjust {@link #setComposureDrain} to change how hard it bites (0 makes the apparition purely cosmetic). */
    public static final class PreHeartDevilTrialEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final float leanFraction;
        private final int pulseIndex;
        private final boolean breakthroughRitual;
        private float composureDrain;

        public PreHeartDevilTrialEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float composureDrain,
                                       float leanFraction, int pulseIndex, boolean breakthroughRitual){
            this.ref = ref;
            this.player = player;
            this.composureDrain = composureDrain;
            this.leanFraction = leanFraction;
            this.pulseIndex = pulseIndex;
            this.breakthroughRitual = breakthroughRitual;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        /** Composure this pulse will drain; when it exceeds what's left, the cultivator deviates. */
        public float composureDrain(){ return this.composureDrain; }
        public void setComposureDrain(float composureDrain){ this.composureDrain = composureDrain; }
        /** How deep the cultivator's Yin/Yang lean runs (0-1) - what makes the demon fiercer. */
        public float leanFraction(){ return this.leanFraction; }
        /** Which pulse of this ritual this is, counting from 0. */
        public int pulseIndex(){ return this.pulseIndex; }
        public boolean breakthroughRitual(){ return this.breakthroughRitual; }
    }

    /** Qi is about to be banked toward a player's next rank-up. Cancel to deny the gain; adjust {@link #setAmount} to re-scale it. Fires for EVERY Qi source (meditation ticks, duel payouts, admin grants), after all of the mod's own multipliers. */
    public static final class PreQiGainEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final float baseAmount;
        private float amount;

        public PreQiGainEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, float amount){
            this.ref = ref;
            this.player = player;
            this.baseAmount = amount;
            this.amount = amount;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        /** Qi about to be banked, after race/skill/pill/sect/dao multipliers. */
        public float amount(){ return this.amount; }
        public void setAmount(float amount){ this.amount = amount; }
        /** What {@link #amount()} was before any listener touched it. */
        public float baseAmount(){ return this.baseAmount; }
    }

    /** A player is about to sit down to meditate. Cancel to keep them on their feet. */
    public static final class PreMeditationStartEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;

        public PreMeditationStartEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player){
            this.ref = ref;
            this.player = player;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
    }

    /** A player is about to stop meditating. Cancel to keep them seated - useful to make a ritual truly unbreakable, or to suppress the movement-cancel. */
    public static final class PreMeditationStopEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final MeditationStopReason reason;

        public PreMeditationStopEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull MeditationStopReason reason){
            this.ref = ref;
            this.player = player;
            this.reason = reason;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public MeditationStopReason reason(){ return this.reason; }
    }

    /** A timed meditation ritual is about to begin. Cancel to refuse it - the player keeps meditating (banking Qi) but never enters the ritual. */
    public static final class PreRitualStartEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final RitualType type;
        private float requiredSeconds;

        public PreRitualStartEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull RitualType type, float requiredSeconds){
            this.ref = ref;
            this.player = player;
            this.type = type;
            this.requiredSeconds = requiredSeconds;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public RitualType type(){ return this.type; }
        /** How long the ritual is scheduled to take, after race/skill/pill speed-ups. Informational: the duration is re-derived each tick from live config. */
        public float requiredSeconds(){ return this.requiredSeconds; }
        public void setRequiredSeconds(float requiredSeconds){ this.requiredSeconds = requiredSeconds; }
    }

    /** A player is about to be demoted a sub-stage for abandoning a ritual. Cancel to let them walk away free (their banked Qi survives too). */
    public static final class PreDemotionEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final CultivationRealm realm;
        private final CultivationStage oldStage;
        private final CultivationStage newStage;
        private final boolean wasBreakthrough;

        public PreDemotionEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull CultivationRealm realm,
                                @Nonnull CultivationStage oldStage, @Nonnull CultivationStage newStage, boolean wasBreakthrough){
            this.ref = ref;
            this.player = player;
            this.realm = realm;
            this.oldStage = oldStage;
            this.newStage = newStage;
            this.wasBreakthrough = wasBreakthrough;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public CultivationRealm realm(){ return this.realm; }
        @Nonnull public CultivationStage oldStage(){ return this.oldStage; }
        @Nonnull public CultivationStage newStage(){ return this.newStage; }
        public boolean wasBreakthrough(){ return this.wasBreakthrough; }
    }

    /** A cultivator's composure has shattered and Qi Deviation is about to be applied. Cancel to spare them the penalty (the ritual still ends); flip {@link #setDemotes} or re-scale {@link #setQiLoss} to change which penalty lands. */
    public static final class PreQiDeviationEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final boolean breakthroughRitual;
        private boolean demotes;
        private float qiLoss;

        public PreQiDeviationEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, boolean demotes, float qiLoss, boolean breakthroughRitual){
            this.ref = ref;
            this.player = player;
            this.demotes = demotes;
            this.qiLoss = qiLoss;
            this.breakthroughRitual = breakthroughRitual;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        /** True to demote a sub-stage (which also wipes banked Qi), false to only take {@link #qiLoss()}. */
        public boolean demotes(){ return this.demotes; }
        public void setDemotes(boolean demotes){ this.demotes = demotes; }
        /** Banked Qi taken when {@link #demotes()} is false. */
        public float qiLoss(){ return this.qiLoss; }
        public void setQiLoss(float qiLoss){ this.qiLoss = qiLoss; }
        public boolean breakthroughRitual(){ return this.breakthroughRitual; }
    }

    /** A player is about to respec their skill tree. Cancel to refuse; adjust {@link #setRefundedPoints} to change how many points come back. */
    public static final class PreRespecEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private int refundedPoints;

        public PreRespecEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, int refundedPoints){
            this.ref = ref;
            this.player = player;
            this.refundedPoints = refundedPoints;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        public int refundedPoints(){ return this.refundedPoints; }
        public void setRefundedPoints(int refundedPoints){ this.refundedPoints = refundedPoints; }
    }

    // --- Listener registration ---

    private static final List<Consumer<BreakthroughEvent>> BREAKTHROUGH = EventBus.newListenerList();
    private static final List<Consumer<PreBreakthroughEvent>> PRE_BREAKTHROUGH = EventBus.newListenerList();
    private static final List<Consumer<AdvancementEvent>> ADVANCEMENT = EventBus.newListenerList();
    private static final List<Consumer<PreAdvancementEvent>> PRE_ADVANCEMENT = EventBus.newListenerList();
    private static final List<Consumer<RaceChangeEvent>> RACE_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<PreRaceChangeEvent>> PRE_RACE_CHANGE = EventBus.newListenerList();
    private static final List<Consumer<SkillUnlockEvent>> SKILL_UNLOCK = EventBus.newListenerList();
    private static final List<Consumer<PreSkillUnlockEvent>> PRE_SKILL_UNLOCK = EventBus.newListenerList();
    private static final List<Consumer<TribulationStrikeEvent>> TRIBULATION_STRIKE = EventBus.newListenerList();
    private static final List<Consumer<PreTribulationStrikeEvent>> PRE_TRIBULATION_STRIKE = EventBus.newListenerList();
    private static final List<Consumer<LifeBoundLevelUpEvent>> LIFEBOUND_LEVEL_UP = EventBus.newListenerList();
    private static final List<Consumer<PreLifeBoundLevelUpEvent>> PRE_LIFEBOUND_LEVEL_UP = EventBus.newListenerList();
    private static final List<Consumer<HeartDevilTrialEvent>> HEART_DEVIL_TRIAL = EventBus.newListenerList();
    private static final List<Consumer<PreHeartDevilTrialEvent>> PRE_HEART_DEVIL_TRIAL = EventBus.newListenerList();
    private static final List<Consumer<QiGainEvent>> QI_GAIN = EventBus.newListenerList();
    private static final List<Consumer<PreQiGainEvent>> PRE_QI_GAIN = EventBus.newListenerList();
    private static final List<Consumer<MeditationStartEvent>> MEDITATION_START = EventBus.newListenerList();
    private static final List<Consumer<PreMeditationStartEvent>> PRE_MEDITATION_START = EventBus.newListenerList();
    private static final List<Consumer<MeditationStopEvent>> MEDITATION_STOP = EventBus.newListenerList();
    private static final List<Consumer<PreMeditationStopEvent>> PRE_MEDITATION_STOP = EventBus.newListenerList();
    private static final List<Consumer<RitualStartEvent>> RITUAL_START = EventBus.newListenerList();
    private static final List<Consumer<PreRitualStartEvent>> PRE_RITUAL_START = EventBus.newListenerList();
    private static final List<Consumer<DemotionEvent>> DEMOTION = EventBus.newListenerList();
    private static final List<Consumer<PreDemotionEvent>> PRE_DEMOTION = EventBus.newListenerList();
    private static final List<Consumer<QiDeviationEvent>> QI_DEVIATION = EventBus.newListenerList();
    private static final List<Consumer<PreQiDeviationEvent>> PRE_QI_DEVIATION = EventBus.newListenerList();
    private static final List<Consumer<RespecEvent>> RESPEC = EventBus.newListenerList();
    private static final List<Consumer<PreRespecEvent>> PRE_RESPEC = EventBus.newListenerList();

    public static void onBreakthrough(@Nonnull Consumer<BreakthroughEvent> listener){ BREAKTHROUGH.add(listener); }
    public static void onPreBreakthrough(@Nonnull Consumer<PreBreakthroughEvent> listener){ PRE_BREAKTHROUGH.add(listener); }
    public static void onAdvancement(@Nonnull Consumer<AdvancementEvent> listener){ ADVANCEMENT.add(listener); }
    public static void onPreAdvancement(@Nonnull Consumer<PreAdvancementEvent> listener){ PRE_ADVANCEMENT.add(listener); }
    public static void onRaceChange(@Nonnull Consumer<RaceChangeEvent> listener){ RACE_CHANGE.add(listener); }
    public static void onPreRaceChange(@Nonnull Consumer<PreRaceChangeEvent> listener){ PRE_RACE_CHANGE.add(listener); }
    public static void onSkillUnlock(@Nonnull Consumer<SkillUnlockEvent> listener){ SKILL_UNLOCK.add(listener); }
    public static void onPreSkillUnlock(@Nonnull Consumer<PreSkillUnlockEvent> listener){ PRE_SKILL_UNLOCK.add(listener); }
    public static void onTribulationStrike(@Nonnull Consumer<TribulationStrikeEvent> listener){ TRIBULATION_STRIKE.add(listener); }
    public static void onPreTribulationStrike(@Nonnull Consumer<PreTribulationStrikeEvent> listener){ PRE_TRIBULATION_STRIKE.add(listener); }
    public static void onLifeBoundLevelUp(@Nonnull Consumer<LifeBoundLevelUpEvent> listener){ LIFEBOUND_LEVEL_UP.add(listener); }
    public static void onPreLifeBoundLevelUp(@Nonnull Consumer<PreLifeBoundLevelUpEvent> listener){ PRE_LIFEBOUND_LEVEL_UP.add(listener); }
    public static void onHeartDevilTrial(@Nonnull Consumer<HeartDevilTrialEvent> listener){ HEART_DEVIL_TRIAL.add(listener); }
    public static void onPreHeartDevilTrial(@Nonnull Consumer<PreHeartDevilTrialEvent> listener){ PRE_HEART_DEVIL_TRIAL.add(listener); }
    public static void onQiGain(@Nonnull Consumer<QiGainEvent> listener){ QI_GAIN.add(listener); }
    public static void onPreQiGain(@Nonnull Consumer<PreQiGainEvent> listener){ PRE_QI_GAIN.add(listener); }
    public static void onMeditationStart(@Nonnull Consumer<MeditationStartEvent> listener){ MEDITATION_START.add(listener); }
    public static void onPreMeditationStart(@Nonnull Consumer<PreMeditationStartEvent> listener){ PRE_MEDITATION_START.add(listener); }
    public static void onMeditationStop(@Nonnull Consumer<MeditationStopEvent> listener){ MEDITATION_STOP.add(listener); }
    public static void onPreMeditationStop(@Nonnull Consumer<PreMeditationStopEvent> listener){ PRE_MEDITATION_STOP.add(listener); }
    public static void onRitualStart(@Nonnull Consumer<RitualStartEvent> listener){ RITUAL_START.add(listener); }
    public static void onPreRitualStart(@Nonnull Consumer<PreRitualStartEvent> listener){ PRE_RITUAL_START.add(listener); }
    public static void onDemotion(@Nonnull Consumer<DemotionEvent> listener){ DEMOTION.add(listener); }
    public static void onPreDemotion(@Nonnull Consumer<PreDemotionEvent> listener){ PRE_DEMOTION.add(listener); }
    public static void onQiDeviation(@Nonnull Consumer<QiDeviationEvent> listener){ QI_DEVIATION.add(listener); }
    public static void onPreQiDeviation(@Nonnull Consumer<PreQiDeviationEvent> listener){ PRE_QI_DEVIATION.add(listener); }
    public static void onRespec(@Nonnull Consumer<RespecEvent> listener){ RESPEC.add(listener); }
    public static void onPreRespec(@Nonnull Consumer<PreRespecEvent> listener){ PRE_RESPEC.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireBreakthrough(@Nonnull BreakthroughEvent event){ EventBus.dispatch(BREAKTHROUGH, event, "BreakthroughEvent"); }
    public static boolean firePreBreakthrough(@Nonnull PreBreakthroughEvent event){ return EventBus.fire(PRE_BREAKTHROUGH, event, "PreBreakthroughEvent"); }
    public static void fireAdvancement(@Nonnull AdvancementEvent event){ EventBus.dispatch(ADVANCEMENT, event, "AdvancementEvent"); }
    public static boolean firePreAdvancement(@Nonnull PreAdvancementEvent event){ return EventBus.fire(PRE_ADVANCEMENT, event, "PreAdvancementEvent"); }
    public static void fireRaceChange(@Nonnull RaceChangeEvent event){ EventBus.dispatch(RACE_CHANGE, event, "RaceChangeEvent"); }
    public static boolean firePreRaceChange(@Nonnull PreRaceChangeEvent event){ return EventBus.fire(PRE_RACE_CHANGE, event, "PreRaceChangeEvent"); }
    public static void fireSkillUnlock(@Nonnull SkillUnlockEvent event){ EventBus.dispatch(SKILL_UNLOCK, event, "SkillUnlockEvent"); }
    public static boolean firePreSkillUnlock(@Nonnull PreSkillUnlockEvent event){ return EventBus.fire(PRE_SKILL_UNLOCK, event, "PreSkillUnlockEvent"); }
    public static void fireTribulationStrike(@Nonnull TribulationStrikeEvent event){ EventBus.dispatch(TRIBULATION_STRIKE, event, "TribulationStrikeEvent"); }
    public static boolean firePreTribulationStrike(@Nonnull PreTribulationStrikeEvent event){ return EventBus.fire(PRE_TRIBULATION_STRIKE, event, "PreTribulationStrikeEvent"); }
    public static void fireLifeBoundLevelUp(@Nonnull LifeBoundLevelUpEvent event){ EventBus.dispatch(LIFEBOUND_LEVEL_UP, event, "LifeBoundLevelUpEvent"); }
    public static boolean firePreLifeBoundLevelUp(@Nonnull PreLifeBoundLevelUpEvent event){ return EventBus.fire(PRE_LIFEBOUND_LEVEL_UP, event, "PreLifeBoundLevelUpEvent"); }
    public static void fireHeartDevilTrial(@Nonnull HeartDevilTrialEvent event){ EventBus.dispatch(HEART_DEVIL_TRIAL, event, "HeartDevilTrialEvent"); }
    public static boolean firePreHeartDevilTrial(@Nonnull PreHeartDevilTrialEvent event){ return EventBus.fire(PRE_HEART_DEVIL_TRIAL, event, "PreHeartDevilTrialEvent"); }
    public static void fireQiGain(@Nonnull QiGainEvent event){ EventBus.dispatch(QI_GAIN, event, "QiGainEvent"); }
    public static boolean firePreQiGain(@Nonnull PreQiGainEvent event){ return EventBus.fire(PRE_QI_GAIN, event, "PreQiGainEvent"); }
    public static void fireMeditationStart(@Nonnull MeditationStartEvent event){ EventBus.dispatch(MEDITATION_START, event, "MeditationStartEvent"); }
    public static boolean firePreMeditationStart(@Nonnull PreMeditationStartEvent event){ return EventBus.fire(PRE_MEDITATION_START, event, "PreMeditationStartEvent"); }
    public static void fireMeditationStop(@Nonnull MeditationStopEvent event){ EventBus.dispatch(MEDITATION_STOP, event, "MeditationStopEvent"); }
    public static boolean firePreMeditationStop(@Nonnull PreMeditationStopEvent event){ return EventBus.fire(PRE_MEDITATION_STOP, event, "PreMeditationStopEvent"); }
    public static void fireRitualStart(@Nonnull RitualStartEvent event){ EventBus.dispatch(RITUAL_START, event, "RitualStartEvent"); }
    public static boolean firePreRitualStart(@Nonnull PreRitualStartEvent event){ return EventBus.fire(PRE_RITUAL_START, event, "PreRitualStartEvent"); }
    public static void fireDemotion(@Nonnull DemotionEvent event){ EventBus.dispatch(DEMOTION, event, "DemotionEvent"); }
    public static boolean firePreDemotion(@Nonnull PreDemotionEvent event){ return EventBus.fire(PRE_DEMOTION, event, "PreDemotionEvent"); }
    public static void fireQiDeviation(@Nonnull QiDeviationEvent event){ EventBus.dispatch(QI_DEVIATION, event, "QiDeviationEvent"); }
    public static boolean firePreQiDeviation(@Nonnull PreQiDeviationEvent event){ return EventBus.fire(PRE_QI_DEVIATION, event, "PreQiDeviationEvent"); }
    public static void fireRespec(@Nonnull RespecEvent event){ EventBus.dispatch(RESPEC, event, "RespecEvent"); }
    public static boolean firePreRespec(@Nonnull PreRespecEvent event){ return EventBus.fire(PRE_RESPEC, event, "PreRespecEvent"); }
}
