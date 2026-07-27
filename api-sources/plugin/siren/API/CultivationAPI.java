package plugin.siren.API;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Cultivation;
import plugin.siren.ECS.Components.Chunk.SpiritVeinComponent;
import plugin.siren.ECS.Components.CultivationComponent;
import plugin.siren.ECS.Components.CultivationSettingsComponent;
import plugin.siren.ECS.Components.CultivationStateComponent;
import plugin.siren.ECS.Components.RaceComponent;
import plugin.siren.ECS.Components.SkillTreeComponent;
import plugin.siren.ECS.Components.TechniqueComponent;
import plugin.siren.ECS.Races.PlayerRace;
import plugin.siren.ECS.Realms.CultivationRealm;
import plugin.siren.ECS.Realms.CultivationStage;
import plugin.siren.ECS.Technique.Technique;
import plugin.siren.ECS.Technique.TechniqueEffect;
import plugin.siren.Utils.CultivationManager;
import plugin.siren.Utils.QiAbsorptionItemRegistry;
import plugin.siren.Utils.TechniqueManager;
import plugin.siren.Utils.Config.RaceConfig;
import plugin.siren.Utils.Config.TechniqueRule;
import plugin.siren.Utils.UI.CultivationNav;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Public integration surface for other Hytale mods that want to read or
 * extend Cultivation. Depend on this mod (plugin.siren:Cultivation) as a
 * compile-time Maven dependency, the same way this mod's own sibling
 * Mermaids depends on other mods' APIs (see EndlessLevelingRegistry for
 * precedent, in the opposite direction).
 *
 * <p>This class is the intended stable surface - prefer it over reaching
 * into CultivationManager/Cultivation/the ECS components directly, since
 * those are free to change shape between versions.</p>
 *
 * <p>Registration (registerRace, registerTechnique,
 * registerQiAbsorptionItemModifier) is safe to call from your own plugin's
 * setup() in any load order relative to Cultivation's own setup() - the
 * registries are plain static maps that nothing reads until a player actually
 * interacts (opens the race menu, meditates, performs a technique), which only
 * happens well after every plugin has finished loading.</p>
 *
 * <p>To REACT to or CHANGE what the mod does, register listeners on the event
 * classes in this package - same load-order guarantee as the registries here.
 * Nearly every mechanic is exposed twice: a cancellable {@code Pre*} event
 * fired before the change (which a listener may veto outright, or re-tune the
 * numbers of - a breakthrough's Qi cost, a technique's cooldown, a tribulation
 * bolt's damage, a tame's odds), and a plain post-event once it is committed.
 * That pre-event surface is the supported way to reshape a mechanic from an
 * addon without touching Cultivation's own config files. See
 * {@link CultivationEvents} for the full conventions - pre vs post, threading,
 * and what cancelling means - then the class covering your subsystem:</p>
 *
 * <ul>
 *   <li>{@link CultivationEvents} - Qi, meditation, rituals, breakthroughs,
 *       advancements, demotions, tribulations, the Heart-Devil Trial, Qi
 *       Deviation, races, the skill tree and respecs</li>
 *   <li>{@link DaoEvents} - elements, drift, Yin-Yang, moral paths, karma</li>
 *   <li>{@link TechniqueEvents} - performing and learning arts, Sword Flying,
 *       the timed combat buffs</li>
 *   <li>{@link ItemEvents} - drops, pills, cores, manuals, weapon refinement</li>
 *   <li>{@link BeastEvents} - taming, hatching, summoning, companion growth</li>
 *   <li>{@link SectEvents} - founding, membership, ranks, halls, inscriptions</li>
 *   <li>{@link WarEvents} - declaring sieges and how they resolve</li>
 *   <li>{@link DuelEvents} - challenges, duels, wager payouts</li>
 *   <li>{@link FormationEvents} - laying and dispersing spirit arrays, traps</li>
 *   <li>{@link DwellingEvents} - Cave Abodes, Spirit Springs, upkeep, seclusion</li>
 * </ul>
 */
public class CultivationAPI {
    private CultivationAPI(){}

    /**
     * The installed progression system, or null while the built-in realm/stage
     * ladder is in charge. Plain static (not volatile/atomic) for the same
     * reason the other registries in this class are: it is written once from a
     * plugin's setup() on the main thread, long before any world thread reads
     * it, and never again.
     */
    @Nullable
    private static ProgressionProvider progressionProvider;

    /** The installed theme, or null when the mod uses its own wording. Same threading rationale as above. */
    @Nullable
    private static CultivationTheme theme;

    /**
     * Config sections other mods have added to the admin menu, in registration
     * order. Copy-on-write because plugins register into it during setup while
     * world threads iterate it every time an admin opens a page - and because
     * iteration vastly outnumbers the handful of writes.
     */
    private static final List<AdminConfigSection> ADMIN_CONFIG_SECTIONS = new CopyOnWriteArrayList<>();

    /**
     * Every page on the menus' shared nav bar - Cultivation's own ten, put
     * there by {@code CultivationNav.registerBuiltinPages()} during setup, plus
     * whatever other mods contribute. Copy-on-write for the same reason as the
     * config sections above.
     */
    private static final List<CultivationMenuPage> MENU_PAGES = new CopyOnWriteArrayList<>();

    /**
     * The in-game Codex's articles and the groups they file under - this mod's
     * own, put there by {@code CultivationCodex.registerBuiltins()} during setup,
     * plus whatever other mods contribute. Copy-on-write for the same reason as
     * the registries above.
     */
    private static final List<CodexEntry> CODEX_ENTRIES = new CopyOnWriteArrayList<>();
    private static final List<CodexCategory> CODEX_CATEGORIES = new CopyOnWriteArrayList<>();

    // --- Component type getters ---

    public static ComponentType<EntityStore, CultivationComponent> getCultivationComponentType(){
        return Cultivation.get().getCultivationComponentType();
    }

    public static ComponentType<EntityStore, CultivationStateComponent> getCultivationStateComponentType(){
        return Cultivation.get().getCultivationStateComponentType();
    }

    public static ComponentType<EntityStore, CultivationSettingsComponent> getCultivationSettingsComponentType(){
        return Cultivation.get().getCultivationSettingsComponentType();
    }

    public static ComponentType<EntityStore, RaceComponent> getRaceComponentType(){
        return Cultivation.get().getRaceComponentType();
    }

    public static ComponentType<ChunkStore, SpiritVeinComponent> getSpiritVeinComponentType(){
        return Cultivation.get().getSpiritVeinComponentType();
    }

    public static ComponentType<EntityStore, SkillTreeComponent> getSkillTreeComponentType(){
        return Cultivation.get().getSkillTreeComponentType();
    }

    // --- Component/state reads ---

    @Nullable
    public static CultivationComponent getCultivationComponent(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        return accessor.getComponent(ref, getCultivationComponentType());
    }

    @Nullable
    public static CultivationStateComponent getCultivationStateComponent(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        return accessor.getComponent(ref, getCultivationStateComponentType());
    }

    @Nullable
    public static RaceComponent getRaceComponent(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        return accessor.getComponent(ref, getRaceComponentType());
    }

    /**
     * The realm this player counts as for every realm gate in the mod - which,
     * when a {@link ProgressionProvider} is installed, is that system's
     * equivalent realm rather than a stored one.
     *
     * @return the player's effective realm, or {@code null} if they don't have
     * a CultivationComponent (e.g. not a player entity).
     */
    @Nullable
    public static CultivationRealm getRealm(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component == null ? null : CultivationManager.getEffectiveRealm(accessor, ref, component);
    }

    /**
     * @return the player's stored sub-stage, or {@code null} if they don't have
     * a CultivationComponent. Always {@code null} while a
     * {@link ProgressionProvider} is installed - a replacement progression has no
     * sub-stages, only its own {@code getSubRankLabel}. Prefer
     * {@link #getGlobalLevel} for anything numeric.
     */
    @Nullable
    public static CultivationStage getStage(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        if(getProgressionProvider() != null){
            return null;
        }

        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component == null ? null : component.getStage();
    }

    /**
     * @return the player's global level - the installed
     * {@link ProgressionProvider}'s level if there is one, else the built-in
     * realm/stage flattened into one ever-increasing number. 0 if they don't
     * have a CultivationComponent.
     */
    public static int getGlobalLevel(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component == null ? 0 : CultivationManager.getGlobalLevel(accessor, ref, component);
    }

    /**
     * @return the player's current banked progress toward their next rank-up -
     * the installed {@link ProgressionProvider}'s progress if there is one, else
     * banked Qi. 0 if they don't have a CultivationComponent.
     */
    public static float getQi(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component == null ? 0f : CultivationManager.getQi(accessor, ref, component);
    }

    // --- Replacing the progression system ---

    /**
     * Hands this mod's entire realm/stage/Qi ladder over to another mod, while
     * every other subsystem here - sects, daos, techniques, spirit beasts,
     * formations, abodes, duels, alchemy, the skill tree - keeps working on top
     * of the replacement. See {@link ProgressionProvider} for the full contract.
     *
     * <p>Call this from your plugin's {@code setup()}. Load order does not
     * matter: nothing reads the provider until a player actually meditates,
     * opens a menu, or trips a realm gate, which is long after every plugin has
     * loaded. Passing {@code null} hands progression back to the built-in
     * system.</p>
     *
     * <p>Only one provider can be installed at a time - the last one to register
     * wins, and a warning naming both is logged, since two mods each believing
     * they own progression is a misconfiguration rather than something to
     * silently pick a winner for.</p>
     */
    public static void setProgressionProvider(@Nullable ProgressionProvider provider){
        ProgressionProvider previous = progressionProvider;
        if(previous != null && provider != null && previous != provider){
            Cultivation.LOGGER.atWarning().log(
                    "Two mods have installed a Cultivation ProgressionProvider - '%s' is being replaced by '%s'. "
                            + "Only one progression system can be live; disable one of them.",
                    previous.getId(), provider.getId());
        }

        progressionProvider = provider;

        if(provider != null){
            Cultivation.LOGGER.atInfo().log("Cultivation progression is now provided by '%s'.", provider.getId());
        } else if(previous != null){
            Cultivation.LOGGER.atInfo().log("Cultivation progression has returned to the built-in realm/stage system.");
        }
    }

    /**
     * @return the installed progression provider, or {@code null} when the
     * built-in realm/stage/Qi system is in charge.
     */
    @Nullable
    public static ProgressionProvider getProgressionProvider(){
        return progressionProvider;
    }

    // --- Contributing settings to the admin menu ---

    /**
     * Adds a group of tunable numbers to Cultivation's admin menu, where it
     * appears as its own section button on the Config tab beside Cultivation's
     * own - and, for admins, in the Cultivation settings menu as well. Editing
     * and saving work exactly as they do for Cultivation's own settings.
     *
     * <p>Call from your plugin's {@code setup()}. Load order does not matter:
     * nothing reads the registry until an admin actually opens a menu.
     * Registering the same section key twice replaces the first, so this is
     * safe across a reload of your plugin.</p>
     *
     * <p>Both host pages are gated on {@code cultivation.admin}, so a section
     * may safely expose real balance numbers.</p>
     *
     * @see #newAdminConfigField for building the rows without boilerplate.
     */
    public static void registerAdminConfigSection(@Nonnull AdminConfigSection section){
        ADMIN_CONFIG_SECTIONS.removeIf(existing -> existing.getKey().equals(section.getKey()));
        ADMIN_CONFIG_SECTIONS.add(section);
    }

    /** Removes a previously registered section - for a plugin unloading cleanly. */
    public static void unregisterAdminConfigSection(@Nonnull String sectionKey){
        ADMIN_CONFIG_SECTIONS.removeIf(existing -> existing.getKey().equals(sectionKey));
    }

    /**
     * @return every section other mods have contributed, in registration order.
     * Safe to iterate without locking.
     */
    @Nonnull
    public static List<AdminConfigSection> getAdminConfigSections(){
        return ADMIN_CONFIG_SECTIONS;
    }

    /**
     * Builds one editable row of an {@link AdminConfigSection} from a getter and
     * a setter, so a section body reads as a plain list of its settings:
     *
     * <pre>{@code List.of(
     *     CultivationAPI.newAdminConfigField("MyMod:BaseXp",
     *             Message.translation("server.mymod.admin.baseXp"),
     *             () -> config.get().getBaseXp(),
     *             value -> config.get().setBaseXp((float) value)))}</pre>
     *
     * <p>Read the config through the supplier rather than capturing the config
     * OBJECT, as above - a reload replaces the instance behind the holder, and a
     * captured one would then edit a discarded copy.</p>
     *
     * <p>For a value stored as a fraction but better edited as a percent, scale
     * in both directions here ({@code () -> c.getChance() * 100} /
     * {@code v -> c.setChance((float)(v / 100))}) - the row widget shows two
     * decimal places, so a raw 0.004 chance would display as 0.00.</p>
     */
    @Nonnull
    public static AdminConfigField newAdminConfigField(@Nonnull String key, @Nonnull Message label,
                                                       @Nonnull DoubleSupplier getter, @Nonnull DoubleConsumer setter){
        return new AdminConfigField(){
            @Nonnull
            @Override
            public String getKey(){
                return key;
            }

            @Nonnull
            @Override
            public Message getLabel(){
                return label;
            }

            @Override
            public double get(){
                return getter.getAsDouble();
            }

            @Override
            public void set(double value){
                setter.accept(value);
            }
        };
    }

    // --- Contributing a page to the menus ---

    /**
     * Adds a page to the nav bar every Cultivation menu carries, so a mod's own
     * UI sits alongside Overview, Race, Dao and the rest rather than behind a
     * command of its own. The bar scrolls horizontally, so there is no practical
     * limit on how many pages may be added.
     *
     * <p>Call from your plugin's {@code setup()}. Load order does not matter:
     * nothing reads the registry until a player opens a menu. Registering the
     * same id twice replaces the first, so this is safe across a reload of your
     * plugin - and is also how a mod takes over one of Cultivation's own entries
     * rather than adding beside it.</p>
     *
     * <p>To put the same bar on your own page, give it a
     * {@code Group #NavBar { LayoutMode: Left; Anchor: (Bottom: 4, Height: 46); }}
     * placeholder, call {@link #buildMenuNav} from its {@code build}, add a
     * {@link #MENU_NAV_EVENT_KEY} string field to its event codec, and hand that
     * field to {@link #handleMenuNav} from its {@code handleDataEvent}.</p>
     *
     * @see CultivationMenuPage#builder for building one without boilerplate.
     */
    public static void registerMenuPage(@Nonnull CultivationMenuPage page){
        MENU_PAGES.removeIf(existing -> existing.getKey().equals(page.getKey()));
        MENU_PAGES.add(page);
    }

    /** Removes a previously registered page - for a plugin unloading cleanly. */
    public static void unregisterMenuPage(@Nonnull String pageKey){
        MENU_PAGES.removeIf(existing -> existing.getKey().equals(pageKey));
    }

    /**
     * @return every menu page, in the order they appear on the bar: by
     * {@link CultivationMenuPage#getSortOrder()}, then registration order among
     * pages that declare the same one. A fresh list, safe to iterate and to hold.
     */
    @Nonnull
    public static List<CultivationMenuPage> getMenuPages(){
        List<CultivationMenuPage> pages = new ArrayList<>(MENU_PAGES);
        pages.sort(Comparator.comparingInt(CultivationMenuPage::getSortOrder));
        return pages;
    }

    /** @return the page registered under this id, or null if nothing claims it. */
    @Nullable
    public static CultivationMenuPage getMenuPage(@Nonnull String pageKey){
        for(CultivationMenuPage page : MENU_PAGES){
            if(page.getKey().equals(pageKey)){
                return page;
            }
        }

        return null;
    }

    /**
     * The event-data key a nav click arrives under. Add it to your page's codec
     * as a plain {@code Codec.STRING} field - it carries a literal page id, not
     * a selector, so it takes no {@code '@'} prefix:
     *
     * <pre>{@code .addField(new KeyedCodec<>(CultivationAPI.MENU_NAV_EVENT_KEY, Codec.STRING),
     *         (data, value) -> data.nav = value, data -> data.nav)}</pre>
     */
    public static final String MENU_NAV_EVENT_KEY = CultivationNav.KEY_NAV;

    /**
     * Draws the shared nav bar into your page's {@code #NavBar} placeholder,
     * with your own page's button disabled to mark where the player is.
     *
     * @param currentPageKey the id you registered this page under, so its own
     *                       button reads as "you are here". An id nothing claims
     *                       simply leaves every button clickable.
     */
    public static void buildMenuNav(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder,
                                    @Nonnull PlayerRef playerRef, @Nonnull String currentPageKey){
        CultivationNav.build(commandBuilder, eventBuilder, playerRef, currentPageKey);
    }

    /**
     * Handles a nav click by opening whichever page it names, in place of the
     * one the player has open. Safe to call with the null your codec field holds
     * when the event was one of your page's own buttons rather than a nav click,
     * and with an id the player is not allowed to open.
     */
    public static void handleMenuNav(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                     @Nonnull PlayerRef playerRef, @Nullable String navKey){
        CultivationNav.open(store, ref, playerRef, navKey);
    }

    /**
     * Swaps the player's open custom page for another one - what a
     * {@link CultivationMenuPage}'s {@code onOpen} normally does, and the same
     * page-swap the built-in menus use to navigate between themselves.
     */
    public static void openMenuPage(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                    @Nonnull CustomUIPage page){
        CultivationNav.openCustomPage(store, ref, page);
    }

    // --- Contributing to the Codex ---

    /**
     * Adds an article to the in-game Cultivation Codex, where it sits in the
     * index beside this mod's own and is read the same way.
     *
     * <p>Call from your plugin's {@code setup()}; load order does not matter,
     * since nothing reads the registry until a player opens the Codex.
     * Registering the same id twice replaces the first, so this is safe across a
     * reload - and is how a mod rewrites a built-in article rather than adding
     * beside it.</p>
     *
     * <p>Write the body against the reader (see {@link CodexPage}) rather than as
     * fixed prose. An article that states this server's real configured numbers
     * and what the reader has actually reached is the reason to have an in-game
     * codex at all - it is the one thing a wiki cannot do.</p>
     *
     * @see CodexEntry#builder for building one without boilerplate.
     */
    public static void registerCodexEntry(@Nonnull CodexEntry entry){
        CODEX_ENTRIES.removeIf(existing -> existing.getId().equals(entry.getId()));
        CODEX_ENTRIES.add(entry);
    }

    /** Removes a previously registered article - for a plugin unloading cleanly. */
    public static void unregisterCodexEntry(@Nonnull String entryId){
        CODEX_ENTRIES.removeIf(existing -> existing.getId().equals(entryId));
    }

    /**
     * @return every article, ordered by {@link CodexEntry#getSortOrder()} and then
     * registration order. Grouping them is the caller's job - see
     * {@link #getCodexCategories()}. A fresh list, safe to hold.
     */
    @Nonnull
    public static List<CodexEntry> getCodexEntries(){
        List<CodexEntry> entries = new ArrayList<>(CODEX_ENTRIES);
        entries.sort(Comparator.comparingInt(CodexEntry::getSortOrder));
        return entries;
    }

    /** @return the article registered under this id, or null if nothing claims it. */
    @Nullable
    public static CodexEntry getCodexEntry(@Nonnull String entryId){
        for(CodexEntry entry : CODEX_ENTRIES){
            if(entry.getId().equals(entryId)){
                return entry;
            }
        }

        return null;
    }

    /**
     * Adds a heading to the Codex index. Four ship with the mod
     * ({@link CodexCategory#PATH}, {@code SELF}, {@code WORLD}, {@code CRAFT});
     * filing under one of those is usually better for a reader than adding a
     * fifth. Registering an existing id replaces it, which is how a group gets
     * renamed or moved.
     */
    public static void registerCodexCategory(@Nonnull CodexCategory category){
        CODEX_CATEGORIES.removeIf(existing -> existing.getId().equals(category.getId()));
        CODEX_CATEGORIES.add(category);
    }

    public static void unregisterCodexCategory(@Nonnull String categoryId){
        CODEX_CATEGORIES.removeIf(existing -> existing.getId().equals(categoryId));
    }

    /** @return every index heading, in display order. A fresh list, safe to hold. */
    @Nonnull
    public static List<CodexCategory> getCodexCategories(){
        List<CodexCategory> categories = new ArrayList<>(CODEX_CATEGORIES);
        categories.sort(Comparator.comparingInt(CodexCategory::getSortOrder));
        return categories;
    }

    // --- Re-wording the mod ---

    /**
     * Re-words this mod's entire player-facing vocabulary, so a setting with
     * soul masters and academies rather than cultivators and sects reads as its
     * own game. See {@link CultivationTheme} for the full contract, and for why
     * this is a hook rather than a language file.
     *
     * <p>Call from your plugin's {@code setup()}; load order does not matter,
     * since nothing is rendered until a player is online. Passing {@code null}
     * restores Cultivation's own wording.</p>
     *
     * <p>Only one theme can be installed at a time - the last to register wins,
     * and a warning naming both is logged, since two mods each re-wording the
     * same strings is a misconfiguration rather than something to silently pick
     * a winner for.</p>
     */
    public static void setTheme(@Nullable CultivationTheme newTheme){
        CultivationTheme previous = theme;
        if(previous != null && newTheme != null && previous != newTheme){
            Cultivation.LOGGER.atWarning().log(
                    "Two mods have installed a Cultivation theme - '%s' is being replaced by '%s'. "
                            + "Only one can be live; disable one of them.",
                    previous.getId(), newTheme.getId());
        }

        theme = newTheme;

        if(newTheme != null){
            Cultivation.LOGGER.atInfo().log("Cultivation is now worded by the '%s' theme.", newTheme.getId());
        } else if(previous != null){
            Cultivation.LOGGER.atInfo().log("Cultivation has returned to its own wording.");
        }
    }

    /** @return the installed theme, or {@code null} when the mod uses its own wording. */
    @Nullable
    public static CultivationTheme getTheme(){
        return theme;
    }

    /**
     * Tells Cultivation that a player's progression numbers have changed
     * underneath it: re-applies their max-health modifier, refreshes their HUD,
     * updates the cross-player rankings, and re-reads the realm every gate in
     * the mod tests against.
     *
     * <p><b>A {@link ProgressionProvider} must call this whenever it changes a
     * player's level</b> - and, if it wants a live progress bar, whenever it
     * changes their banked progress. Cultivation cannot detect a change inside
     * somebody else's component on its own, so anything that skips this leaves
     * the HUD, the rankings and every realm gate showing that player's previous
     * standing until their next meditation tick.</p>
     *
     * <p>Also worth calling from a provider's own player-join handling, so a
     * player whose component loads after Cultivation's join hook ran is not
     * gated on last session's numbers for their first few seconds.</p>
     *
     * <p>Safe to call from a system, a command, or an interaction; a no-op for
     * an entity with no CultivationComponent.</p>
     */
    public static void refreshProgression(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null){
            return;
        }

        CultivationManager.applyRealmStats(accessor, ref, component);
        CultivationManager.refreshHud(accessor, ref, component);
    }

    /**
     * @return the player's current race, defaulting to Human if they don't
     * have a RaceComponent yet.
     */
    @Nonnull
    public static PlayerRace getRace(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        return CultivationManager.getRace(accessor, ref);
    }

    public static boolean isMeditating(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        CultivationStateComponent state = getCultivationStateComponent(accessor, ref);
        return state != null && state.isMeditating();
    }

    /**
     * @return the player's unspent skill tree points, or 0 if they don't have
     * a SkillTreeComponent yet.
     */
    public static int getAvailableSkillPoints(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        SkillTreeComponent skillTree = accessor.getComponent(ref, getSkillTreeComponentType());
        return skillTree == null ? 0 : skillTree.getAvailablePoints();
    }

    /**
     * @return whether the player has unlocked the given skill tree node id
     * (see plugin.siren.ECS.SkillTree.SkillTreeRegistry for the built-in
     * node ids, e.g. "VITALITY_1"). False if they don't have a
     * SkillTreeComponent yet, or the id doesn't match any unlocked node.
     */
    public static boolean isNodeUnlocked(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref, @Nonnull String nodeId){
        SkillTreeComponent skillTree = accessor.getComponent(ref, getSkillTreeComponentType());
        return skillTree != null && skillTree.isUnlocked(nodeId);
    }

    // --- Custom race registration ---

    /**
     * Registers a brand-new race that players can choose from the race menu
     * (/cultivation race) once their cultivation reaches {@code unlockRealm}.
     * Re-registering the same id is a no-op (returns the existing race
     * rather than erroring), so this is safe to call more than once, e.g.
     * across a reload of your own plugin.
     *
     * @param id             a stable, unique id for this race (not shown to
     *                       players) - namespacing it with your mod's name
     *                       (e.g. "MyMod:Vampire") avoids colliding with
     *                       another mod's race of the same short name.
     * @param displayName    the race's name shown in the UI when no
     *                       translationKey is given, or as a fallback if the
     *                       key doesn't resolve for a given player's locale.
     * @param translationKey a server.lang key for this race's localized
     *                       name, or {@code null} to always show
     *                       {@code displayName} as plain, untranslated text.
     * @param unlockRealm    the realm a player's cultivation must reach
     *                       before this race can be chosen from the menu.
     * @param stats          supplies this race's stat bonuses (health/damage/
     *                       Qi gain/breakthrough speed percentages) each time
     *                       they're needed - back it with your own plugin's
     *                       {@code withConfig(name, RaceConfig.codec(...))}
     *                       for a live-editable JSON file, or just
     *                       {@code () -> myConstantConfig} for a fixed one.
     * @return the registered PlayerRace, for convenience (e.g. to keep as a
     * static field the same way this mod exposes PlayerRace.HUMAN).
     */
    @Nonnull
    public static PlayerRace registerRace(@Nonnull String id, @Nonnull String displayName, @Nullable String translationKey,
                                           @Nonnull CultivationRealm unlockRealm, @Nonnull Supplier<RaceConfig> stats){
        PlayerRace race = PlayerRace.register(id, displayName, translationKey);

        // unlockRealm only seeds RaceConfig.UnlockRealm when the supplied config doesn't
        // already specify one, so a caller backing their stats with their own JSON config
        // file (which may itself set/override Unlock-Realm) stays server-owner-editable -
        // this is only a convenience default for callers who don't touch that field at all.
        Supplier<RaceConfig> seededStats = () -> {
            RaceConfig config = stats.get();
            String configuredUnlockRealm = config.getUnlockRealm();
            if(configuredUnlockRealm == null || configuredUnlockRealm.isEmpty()){
                config.setUnlockRealm(unlockRealm.name());
            }
            return config;
        };
        Cultivation.registerRaceConfig(race.getId(), seededStats);

        return race;
    }

    // --- Qi absorption item modifiers ---

    /**
     * Registers (or overwrites) the Spirit Vein absorption multiplier
     * granted while the given item id is held in a meditating player's
     * hotbar active slot - the same mechanism the built-in Qi Gathering
     * Talisman uses (see QiAbsorptionItemRegistry).
     */
    public static void registerQiAbsorptionItemModifier(@Nonnull String itemId, float multiplier){
        QiAbsorptionItemRegistry.register(itemId, multiplier);
    }

    // --- Custom techniques ---

    public static ComponentType<EntityStore, TechniqueComponent> getTechniqueComponentType(){
        return Cultivation.get().getTechniqueComponentType();
    }

    /**
     * Registers a brand-new technique that cultivators can perform - the same
     * system the built-in One Step, a Thousand Li uses. Once registered it is
     * usable through every technique trigger automatically: the
     * {@code /cultivation technique <id>} command (and its list), and any
     * activation item whose {@code CultivationActivateTechnique} interaction
     * carries this id as its {@code TechniqueId}. You may also perform it
     * yourself from your own trigger (a keybind, another item, an event...) via
     * {@link #performTechnique}.
     *
     * <p>Re-registering the same id is a no-op (returns the existing technique
     * rather than erroring), so this is safe across a reload of your plugin.
     * Call it from your plugin's {@code setup()} in any load order relative to
     * Cultivation's - nothing reads the technique registry until a player
     * actually performs one, well after every plugin has loaded.</p>
     *
     * @param id           a stable, unique id (also the config key and the
     *                     activation item's {@code TechniqueId}) - namespace it
     *                     with your mod's name (e.g. "MyMod:flame_step") to
     *                     avoid colliding with another mod's technique.
     * @param displayName  the technique's name shown when no nameKey is given,
     *                     or as a fallback if the key doesn't resolve.
     * @param nameKey      a server.lang key for the localized name, or
     *                     {@code null} to always show {@code displayName} raw.
     * @param descriptionKey a server.lang key for the description, or null.
     * @param defaultRule  the rule this technique runs by (dao-specificity,
     *                     carried elements, damage type, unlock realm, Qi cost,
     *                     cooldown, params). This is the ONLY source of rules for
     *                     your technique unless a server owner adds a matching
     *                     override entry to Cultivation's TechniqueConfig.json.
     *                     Build one with {@link #newTechniqueRule}.
     * @param effect       what performing the technique does - a lambda/method
     *                     reference over a {@code TechniqueContext} (see the
     *                     built-in {@code BuiltInTechniques.oneStepThousandLi}
     *                     for a worked example; the context has teleport /
     *                     particle / cultivation-state / message helpers). It is
     *                     only ever invoked AFTER all gates pass and the Qi
     *                     cost/cooldown have been applied.
     * @return the registered Technique, for convenience (e.g. to keep as a
     * static field and pass to {@link #performTechnique}).
     */
    @Nonnull
    public static Technique registerTechnique(@Nonnull String id, @Nonnull String displayName, @Nullable String nameKey,
                                              @Nullable String descriptionKey, @Nonnull TechniqueRule defaultRule,
                                              @Nonnull TechniqueEffect effect){
        return Technique.register(id, displayName, nameKey, descriptionKey, defaultRule, effect);
    }

    /**
     * Convenience builder for a {@link TechniqueRule} to pass to
     * {@link #registerTechnique}. All the tuning knobs of a technique in one
     * call; {@code params} are technique-specific named numbers your effect
     * reads via {@code context.getParam(key, fallback)}.
     *
     * @param requiredElement a DaoElement enum name (e.g. "WIND") when
     *                        {@code daoSpecific} is true, else "" / null.
     * @param elements        comma-separated DaoElement names the technique
     *                        "carries" (metadata/flavor), or "".
     * @param damageType      a DamageCause asset id for a damaging technique, or
     *                        "" for none.
     * @param unlockRealm     the CultivationRealm enum name required to use it
     *                        (e.g. "QI_CONDENSATION").
     * @param params          alternating key/value pairs, e.g.
     *                        {@code newTechniqueRule(..., "BaseDistance", 4f, "MaxDistance", 40f)}.
     *                        Must be an even number of arguments (String, float,
     *                        String, float, ...).
     */
    @Nonnull
    public static TechniqueRule newTechniqueRule(@Nonnull String id, boolean enabled, boolean daoSpecific,
                                                 @Nullable String requiredElement, @Nullable String elements,
                                                 @Nullable String damageType, @Nonnull String unlockRealm,
                                                 float qiCost, float cooldownSeconds, @Nonnull Object... params){
        plugin.siren.Utils.Config.TechniqueParam[] built = new plugin.siren.Utils.Config.TechniqueParam[params.length / 2];
        for(int i = 0; i + 1 < params.length; i += 2){
            String key = String.valueOf(params[i]);
            float value = ((Number) params[i + 1]).floatValue();
            built[i / 2] = new plugin.siren.Utils.Config.TechniqueParam(key, value);
        }
        return new TechniqueRule(id, enabled, daoSpecific,
                requiredElement == null ? "" : requiredElement,
                elements == null ? "" : elements,
                damageType == null ? "" : damageType,
                unlockRealm, qiCost, cooldownSeconds, built);
    }

    /**
     * Performs a technique for a player right now, running every gate (system
     * enabled, per-technique enabled, realm unlock, dao match, Qi cost,
     * cooldown), and on success deducting Qi, stamping the cooldown, and running
     * the effect. The player is messaged either way (success message from the
     * effect, or the failure reason). Use this to wire a technique to your own
     * trigger - a keybind, a different item, an event.
     *
     * @return {@code true} if the technique was performed, {@code false} if a
     * gate blocked it.
     */
    public static boolean performTechnique(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                           @Nonnull PlayerRef playerRef, @Nonnull Technique technique){
        return TechniqueManager.activate(accessor, ref, playerRef, technique) == TechniqueManager.ActivateResult.SUCCESS;
    }
}
