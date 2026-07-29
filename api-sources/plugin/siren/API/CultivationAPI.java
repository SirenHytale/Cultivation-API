package plugin.siren.API;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import plugin.siren.Cultivation;
import plugin.siren.ECS.Components.Chunk.SpiritVeinComponent;
import plugin.siren.ECS.Components.CultivationComponent;
import plugin.siren.ECS.Components.CultivationSettingsComponent;
import plugin.siren.ECS.Components.CultivationStateComponent;
import plugin.siren.ECS.Components.DaoComponent;
import plugin.siren.ECS.Components.RaceComponent;
import plugin.siren.ECS.Components.SkillTreeComponent;
import plugin.siren.ECS.Components.SpiritBeastComponent;
import plugin.siren.ECS.Components.TechniqueComponent;
import plugin.siren.ECS.Dao.CultivationPath;
import plugin.siren.ECS.Dao.DaoElement;
import plugin.siren.ECS.Races.PlayerRace;
import plugin.siren.ECS.Realms.CultivationRealm;
import plugin.siren.ECS.Realms.CultivationStage;
import plugin.siren.ECS.Technique.Technique;
import plugin.siren.ECS.Technique.TechniqueEffect;
import plugin.siren.Utils.CultivationManager;
import plugin.siren.Utils.DaoManager;
import plugin.siren.Utils.QiAbsorptionItemRegistry;
import plugin.siren.Utils.SkillTreeManager;
import plugin.siren.Utils.SpiritVeinManager;
import plugin.siren.Utils.TechniqueManager;
import plugin.siren.Utils.Config.RaceConfig;
import plugin.siren.Utils.Config.TechniqueRule;
import plugin.siren.Utils.Duel.DuelManager;
import plugin.siren.Utils.Dwelling.Dwelling;
import plugin.siren.Utils.Dwelling.DwellingManager;
import plugin.siren.Utils.Formation.FormationManager;
import plugin.siren.Utils.Sect.Sect;
import plugin.siren.Utils.Sect.SectManager;
import plugin.siren.Utils.UI.CultivationNav;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
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
     * @return every registered section - Cultivation's own included, since those
     * are ordinary registrations too - ordered by
     * {@link AdminConfigSection#getSortOrder()} and then registration order, with
     * anything currently hiding itself ({@link AdminConfigSection#isVisible()})
     * left out. A fresh list, safe to hold.
     */
    @Nonnull
    public static List<AdminConfigSection> getAdminConfigSections(){
        List<AdminConfigSection> sections = new ArrayList<>();
        for(AdminConfigSection section : ADMIN_CONFIG_SECTIONS){
            if(section.isVisible()){
                sections.add(section);
            }
        }

        sections.sort(Comparator.comparingInt(AdminConfigSection::getSortOrder));
        return sections;
    }

    /** @return the section registered under this key, or null if nothing claims it. */
    @Nullable
    public static AdminConfigSection getAdminConfigSection(@Nonnull String sectionKey){
        for(AdminConfigSection section : ADMIN_CONFIG_SECTIONS){
            if(section.getKey().equals(sectionKey)){
                return section;
            }
        }

        return null;
    }

    /**
     * Builds a section from its parts, so contributing settings is one call
     * rather than an interface implementation:
     *
     * <pre>{@code CultivationAPI.registerAdminConfigSection(
     *     CultivationAPI.newAdminConfigSection("MyMod:power",
     *             "server.mymod.admin.power", "server.mymod.admin.powerHint",
     *             AdminConfigSection.SORT_LAST, config::save,
     *             List.of(
     *                 CultivationAPI.newAdminConfigField("MyMod:BaseXp",
     *                         Message.translation("server.mymod.admin.baseXp"),
     *                         () -> config.get().getBaseXp(),
     *                         value -> config.get().setBaseXp((float) value)),
     *                 CultivationAPI.newAdminBooleanField("MyMod:Enabled",
     *                         Message.translation("server.mymod.admin.enabled"),
     *                         () -> config.get().isEnabled(),
     *                         config.get()::setEnabled))));}</pre>
     *
     * @param labelKey a {@code server.lang} key for the section's name.
     * @param hintKey  a {@code server.lang} key for its one-line explanation.
     * @param save     persists the section after edits are applied - normally
     *                 {@code yourConfigHolder::save}. Called at most once per
     *                 Save, and only if one of these fields actually changed.
     */
    @Nonnull
    public static AdminConfigSection newAdminConfigSection(@Nonnull String key, @Nonnull String labelKey,
                                                            @Nonnull String hintKey, int sortOrder,
                                                            @Nonnull Runnable save,
                                                            @Nonnull List<AdminConfigField> fields){
        return new AdminConfigSection(){
            @Nonnull
            @Override
            public String getKey(){
                return key;
            }

            @Nonnull
            @Override
            public Message getLabel(){
                return Message.translation(labelKey);
            }

            @Nonnull
            @Override
            public Message getHint(){
                return Message.translation(hintKey);
            }

            @Nonnull
            @Override
            public List<AdminConfigField> getFields(){
                return fields;
            }

            @Override
            public int getSortOrder(){
                return sortOrder;
            }

            @Override
            public void save(){
                save.run();
            }
        };
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

    /**
     * As {@link #newAdminConfigField}, but rendered as a whole-number field - the
     * right shape for a count, a level cap or a tier, where the two decimal
     * places a normal number row shows are noise.
     */
    @Nonnull
    public static AdminConfigField newAdminIntField(@Nonnull String key, @Nonnull Message label,
                                                    @Nonnull DoubleSupplier getter, @Nonnull DoubleConsumer setter){
        AdminConfigField numeric = newAdminConfigField(key, label, getter, setter);
        return new AdminConfigField(){
            @Nonnull
            @Override
            public String getKey(){
                return numeric.getKey();
            }

            @Nonnull
            @Override
            public Message getLabel(){
                return numeric.getLabel();
            }

            @Nonnull
            @Override
            public Kind getKind(){
                return Kind.INT;
            }

            @Override
            public double get(){
                return numeric.get();
            }

            @Override
            public void set(double value){
                numeric.set(Math.rint(value));
            }
        };
    }

    /**
     * A real checkbox row, so a master on/off switch no longer has to masquerade
     * as a 0/1 number - which is what every boolean in Cultivation's own settings
     * had to do before this existed, and why so many of them were config-file
     * only.
     */
    @Nonnull
    public static AdminConfigField newAdminBooleanField(@Nonnull String key, @Nonnull Message label,
                                                        @Nonnull BooleanSupplier getter, @Nonnull Consumer<Boolean> setter){
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

            @Nonnull
            @Override
            public Kind getKind(){
                return Kind.BOOLEAN;
            }

            @Override
            public boolean getBoolean(){
                return getter.getAsBoolean();
            }

            @Override
            public void setBoolean(boolean value){
                setter.accept(value);
            }
        };
    }

    /**
     * A dropdown row over a fixed set of options - the right shape for anything
     * enum-valued (a realm name, a dao element, a join policy), where a text
     * field would let an admin type something that resolves to nothing.
     *
     * <p>{@code choices} is re-read on every render, so a set that depends on
     * what other mods have registered stays current. The setter is handed the
     * chosen option's id; re-resolve it rather than trusting it, since the id
     * arrived from a client.</p>
     */
    @Nonnull
    public static AdminConfigField newAdminChoiceField(@Nonnull String key, @Nonnull Message label,
                                                       @Nonnull Supplier<List<AdminConfigChoice>> choices,
                                                       @Nonnull Supplier<String> getter, @Nonnull Consumer<String> setter){
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

            @Nonnull
            @Override
            public Kind getKind(){
                return Kind.CHOICE;
            }

            @Nonnull
            @Override
            public String getText(){
                String value = getter.get();
                return value == null ? "" : value;
            }

            @Override
            public void setText(@Nonnull String value){
                setter.accept(value);
            }

            @Nonnull
            @Override
            public List<AdminConfigChoice> getChoices(){
                return choices.get();
            }
        };
    }

    /**
     * A free-form text row. Use {@link #newAdminChoiceField} instead whenever the
     * valid values are a known set - a dropdown cannot be typed wrong.
     */
    @Nonnull
    public static AdminConfigField newAdminTextField(@Nonnull String key, @Nonnull Message label,
                                                     @Nonnull Supplier<String> getter, @Nonnull Consumer<String> setter){
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

            @Nonnull
            @Override
            public Kind getKind(){
                return Kind.TEXT;
            }

            @Nonnull
            @Override
            public String getText(){
                String value = getter.get();
                return value == null ? "" : value;
            }

            @Override
            public void setText(@Nonnull String value){
                setter.accept(value);
            }
        };
    }

    /**
     * Adds a one-line explanation to any field, shown as its row's tooltip.
     * Wraps rather than replaces, so it composes with every factory above:
     * {@code withTooltip(newAdminBooleanField(...), "What this setting does")}.
     *
     * <p>A plain String rather than a {@link Message}, and therefore not
     * translatable - {@code TooltipText} is a String property client-side and a
     * Message pushed at it disconnects the player. Put anything that must be
     * readable in every language in the field's LABEL instead.</p>
     */
    @Nonnull
    public static AdminConfigField withTooltip(@Nonnull AdminConfigField field, @Nonnull String tooltip){
        return new AdminConfigField(){
            @Nonnull
            @Override
            public String getKey(){
                return field.getKey();
            }

            @Nonnull
            @Override
            public Message getLabel(){
                return field.getLabel();
            }

            @Nonnull
            @Override
            public Kind getKind(){
                return field.getKind();
            }

            @Nonnull
            @Override
            public String getTooltip(){
                return tooltip;
            }

            @Override
            public double get(){
                return field.get();
            }

            @Override
            public void set(double value){
                field.set(value);
            }

            @Override
            public boolean getBoolean(){
                return field.getBoolean();
            }

            @Override
            public void setBoolean(boolean value){
                field.setBoolean(value);
            }

            @Nonnull
            @Override
            public String getText(){
                return field.getText();
            }

            @Override
            public void setText(@Nonnull String value){
                field.setText(value);
            }

            @Nonnull
            @Override
            public List<AdminConfigChoice> getChoices(){
                return field.getChoices();
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

    // --- Changing a cultivator's progression ---
    //
    // The write half of the reads at the top of this class. Every one of these
    // goes through the same code path the mod's own commands and rituals use, so
    // an addon granting Qi fires the same events, honours an installed
    // ProgressionProvider, and refreshes the HUD and rankings exactly as
    // meditating would - none of which is true of reaching into the component.
    //
    // All of them run on the caller's thread and touch only the given entity, so
    // they are safe from a system, a command, or an interaction on that entity's
    // own world thread. To act on a player who may be in ANOTHER world, hop to
    // their world thread first (Universe.get().getPlayer(uuid) -> PlayerRef ->
    // CompletableFuture.runAsync(..., theirWorld)), as this mod's own admin
    // tooling does.

    /**
     * Grants Qi, exactly as absorbing a core or meditating would: through every
     * multiplier (race, skill tree, pills, sect hall, Yin-Yang balance), through
     * the cancellable {@code PreQiGainEvent}, and into whatever progression is
     * installed. A no-op for an entity with no CultivationComponent.
     *
     * @param playerRef the gaining player, for the sect-hall bonus and the event.
     *                  May be null for a non-player cultivator.
     */
    public static void addQi(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                             float amount, @Nullable PlayerRef playerRef){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null){
            return;
        }

        CultivationManager.addQi(accessor, ref, component, amount, playerRef);
    }

    /**
     * Sets banked Qi outright, skipping every multiplier and event - the admin
     * {@code /cultivation admin setqi} path, not the gameplay one. Prefer
     * {@link #addQi} for anything a player earned. Does not rank anyone up: that
     * still requires the meditation ritual.
     */
    public static void setQi(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, float qi){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null){
            return;
        }

        component.setQi(Math.max(0f, qi));
        CultivationManager.refreshHud(accessor, ref, component);
    }

    /**
     * Moves a cultivator to a realm outright and re-applies their stat bonuses.
     * The admin path - it fires no breakthrough event and runs no ritual, because
     * nothing was broken through.
     */
    public static void setRealm(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                @Nonnull CultivationRealm realm){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null){
            return;
        }

        component.setRealm(realm);
        CultivationManager.applyRealmStats(accessor, ref, component);
    }

    /** As {@link #setRealm}, for the sub-stage. */
    public static void setStage(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                @Nonnull CultivationStage stage){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null){
            return;
        }

        component.setStage(stage);
        CultivationManager.applyRealmStats(accessor, ref, component);
    }

    /**
     * Completes a realm breakthrough right now - consuming the banked Qi,
     * granting the skill points, firing {@code PreBreakthroughEvent} and
     * {@code BreakthroughEvent}, and playing the celebration - as though the
     * ritual had just finished. What an addon offering its own path to a
     * breakthrough (a quest, an item, a boss kill) should call rather than
     * setting the realm directly.
     *
     * @return false if a listener vetoed it, or the entity has no CultivationComponent.
     */
    public static boolean completeBreakthrough(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                               @Nullable PlayerRef playerRef){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null || isMaxLevel(accessor, ref)){
            return false;
        }

        CultivationRealm before = component.getRealm();
        CultivationManager.completeBreakthrough(accessor, ref, component, playerRef);
        return component.getRealm() != before;
    }

    /** As {@link #completeBreakthrough}, for a single sub-stage advancement. */
    public static boolean completeAdvancement(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                              @Nullable PlayerRef playerRef){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null || isMaxLevel(accessor, ref)){
            return false;
        }

        CultivationStage before = component.getStage();
        CultivationManager.completeAdvancement(accessor, ref, component, playerRef);
        return component.getStage() != before;
    }

    /**
     * Applies the failed-ritual penalty: one sub-stage down (never below the
     * current realm's first stage) and the banked Qi wiped, with the demotion
     * events fired.
     *
     * @param wasBreakthrough only picks which message the cultivator is sent.
     */
    public static void demote(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                              @Nullable PlayerRef playerRef, boolean wasBreakthrough){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        if(component == null){
            return;
        }

        CultivationManager.demoteStage(accessor, ref, component, playerRef, wasBreakthrough);
    }

    /** @return true when this cultivator is at the top of whichever progression is live. */
    public static boolean isMaxLevel(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component != null && CultivationManager.isMaxLevel(accessor, ref, component);
    }

    /** @return the Qi this cultivator needs to bank before their next rank-up becomes possible. */
    public static float getQiRequiredForNext(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component == null ? 0f : CultivationManager.getQiRequiredForNext(accessor, ref, component);
    }

    /** @return true when this cultivator has banked enough for a realm breakthrough and only needs the ritual. */
    public static boolean isReadyForBreakthrough(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component != null && CultivationManager.isReadyForBreakthrough(accessor, ref, component);
    }

    /** @return true when this cultivator has banked enough for a sub-stage advancement and only needs the ritual. */
    public static boolean isReadyForAdvancement(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        CultivationComponent component = getCultivationComponent(accessor, ref);
        return component != null && CultivationManager.isReadyForAdvancement(accessor, ref, component);
    }

    /** Grants unspent skill tree points. A no-op for an entity with no SkillTreeComponent. */
    public static void grantSkillPoints(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, int points){
        SkillTreeComponent skillTree = accessor.getComponent(ref, getSkillTreeComponentType());
        if(skillTree != null){
            skillTree.addPoints(points);
        }
    }

    /**
     * Unlocks a skill tree node for this cultivator, spending their points and
     * re-applying the stat modifiers the node grants - the same path the skill
     * tree menu takes.
     *
     * @return true if the node was unlocked.
     */
    public static boolean unlockSkillNode(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                          @Nonnull String nodeId){
        SkillTreeComponent skillTree = accessor.getComponent(ref, getSkillTreeComponentType());
        return skillTree != null
                && SkillTreeManager.unlockNode(accessor, ref, skillTree, nodeId) == SkillTreeManager.UnlockResult.SUCCESS;
    }

    /**
     * Grants a skill tree node WITHOUT charging points - for an addon handing out
     * a node as a reward rather than as a purchase.
     *
     * @return true if the node was granted.
     */
    public static boolean grantSkillNode(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                                         @Nonnull String nodeId){
        SkillTreeComponent skillTree = accessor.getComponent(ref, getSkillTreeComponentType());
        return skillTree != null
                && SkillTreeManager.grantNode(accessor, ref, skillTree, nodeId) == SkillTreeManager.UnlockResult.SUCCESS;
    }

    // --- Meditation ---

    /**
     * Seats a cultivator in meditation, playing the cross-legged pose and letting
     * the meditation system start drawing Qi on the next tick - as
     * {@code /cultivation meditate} does.
     *
     * <p>Movement still cancels it, and a cultivator already meditating is left
     * alone.</p>
     */
    public static void startMeditating(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        CultivationStateComponent state = getCultivationStateComponent(accessor, ref);
        if(state == null || state.isMeditating()){
            return;
        }

        TransformComponent transform = accessor.getComponent(ref, TransformComponent.getComponentType());
        if(transform != null){
            // The anchor is what the movement-cancel system measures drift
            // against - without it a cultivator seated by an addon is cancelled
            // on the next check for having "moved" away from the origin.
            org.joml.Vector3d position = transform.getPosition();
            state.setAnchor(position.x, position.y, position.z);
        }

        state.setMeditating(true);
        CultivationManager.setMeditationAnimation(accessor, ref, true);
    }

    /**
     * Ends a cultivator's meditation and stops the pose. Note this does NOT apply
     * the failed-ritual penalty - call {@link #demote} as well if you are
     * interrupting a ritual and mean it to cost something.
     */
    public static void stopMeditating(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        CultivationStateComponent state = getCultivationStateComponent(accessor, ref);
        if(state == null || !state.isMeditating()){
            return;
        }

        state.setMeditating(false);
        CultivationManager.setMeditationAnimation(accessor, ref, false);
    }

    // --- The world's Qi ---

    /**
     * Reads the Spirit Vein of a chunk without touching it: no component is
     * created, nothing is written, and a chunk nobody has ever visited still
     * gives a truthful answer, because the seeding roll is a pure function of the
     * world seed and the chunk position.
     *
     * <p>This is what Spirit Sense reads, and the right call for a map overlay, a
     * divining item, or worldgen decoration that should follow the Qi.</p>
     *
     * @param world  the world whose seed and chunks are consulted.
     * @param chunkX chunk coordinates, not block coordinates - divide by 16, or
     *               use {@code ChunkUtil.chunkCoordinate(blockX)}.
     */
    @Nonnull
    public static SpiritVeinManager.VeinReading readSpiritVein(@Nonnull World world, int chunkX, int chunkZ){
        long chunkIndex = ChunkUtil.indexChunk(chunkX, chunkZ);
        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(chunkIndex);

        return SpiritVeinManager.read(world.getChunkStore().getStore(), chunkRef, chunkIndex,
                world.getWorldConfig().getSeed(), System.currentTimeMillis());
    }

    /**
     * Draws Qi out of a chunk's Spirit Vein, creating and seeding it if this is
     * the first time anything has touched it.
     *
     * <p>Must run on {@code world}'s own thread - it writes to that world's chunk
     * store. Regenerates the vein to now first, so the amount available is the
     * real one rather than whatever it held when last drained.</p>
     *
     * @return how much was actually drawn, which is less than requested when the
     * vein is running dry, and 0 when the chunk is not loaded.
     */
    public static float drainSpiritVein(@Nonnull World world, int chunkX, int chunkZ, float amount){
        if(amount <= 0f){
            return 0f;
        }

        long chunkIndex = ChunkUtil.indexChunk(chunkX, chunkZ);
        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
        if(chunkRef == null || !chunkRef.isValid()){
            return 0f;
        }

        SpiritVeinComponent vein = SpiritVeinManager.getOrCreateVein(world.getChunkStore().getStore(), chunkRef,
                chunkIndex, world.getWorldConfig().getSeed());
        SpiritVeinManager.regenerate(vein, System.currentTimeMillis());

        return SpiritVeinManager.drain(vein, amount);
    }

    // --- Sects, daos and the rest of the world ---
    //
    // Read-side entry points into the subsystems whose EVENTS this package
    // already exposes. Listening to SectEvents without being able to ask "what
    // sect is this player in" meant an addon had to shadow the whole registry
    // itself; these close that gap.

    /** @return the sect this player belongs to, or null. */
    @Nullable
    public static Sect getSect(@Nonnull UUID player){
        return SectManager.getSectOf(player);
    }

    /** @return the sect of this name (case-insensitively), or null. */
    @Nullable
    public static Sect getSectByName(@Nonnull String name){
        return SectManager.getByName(name);
    }

    /** @return the extra Qi percentage this player's sect hall is worth them, or 0. */
    public static float getSectQiBonusPercent(@Nonnull UUID player){
        return SectManager.getQiBonusPercent(player);
    }

    /** @return this cultivator's Dao component, creating it if they have never touched the Dao system. */
    @Nonnull
    public static DaoComponent getOrCreateDao(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        return DaoManager.getOrCreate(accessor, ref);
    }

    /** @return this cultivator's chosen element, or null when they walk no dao. */
    @Nullable
    public static DaoElement getDaoElement(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        DaoComponent dao = accessor.getComponent(ref, DaoComponent.getComponentType());
        return dao == null ? null : dao.getChosenElement();
    }

    /**
     * @return this cultivator's moral path (Righteous, Devil or neither) under
     * the live Yin-Yang balance, or null when they have no Dao component.
     */
    @Nullable
    public static CultivationPath getPath(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        DaoComponent dao = accessor.getComponent(ref, DaoComponent.getComponentType());
        CultivationComponent cultivation = getCultivationComponent(accessor, ref);
        return dao == null || cultivation == null ? null : DaoManager.getPath(dao, cultivation);
    }

    /** @return this cultivator's Yin percentage, 0 (wholly Yang) to 100 (wholly Yin). */
    public static float getYinPercent(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        DaoComponent dao = accessor.getComponent(ref, DaoComponent.getComponentType());
        return dao == null ? 50f : dao.getYinPercent();
    }

    /** @return this cultivator's karma - the blood on their ledger that deepens a tribulation. */
    public static float getKarma(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        DaoComponent dao = accessor.getComponent(ref, DaoComponent.getComponentType());
        return dao == null ? 0f : dao.getKarma();
    }

    /** @return this player's claimed Cave Abode, or null when they have none. */
    @Nullable
    public static Dwelling getAbode(@Nonnull UUID player){
        return DwellingManager.getPersonal(player);
    }

    /** @return whichever dwelling encloses this chunk (personal or a sect hall's), or null. */
    @Nullable
    public static Dwelling getDwellingAt(@Nonnull String world, int chunkX, int chunkZ){
        return DwellingManager.getDwellingAt(world, chunkX, chunkZ);
    }

    /** @return this player's bound spirit beast, or null when they have none. */
    @Nullable
    public static SpiritBeastComponent getBeast(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref){
        SpiritBeastComponent beast = accessor.getComponent(ref, SpiritBeastComponent.getComponentType());
        return beast != null && beast.hasBeast() ? beast : null;
    }

    /** @return the duel this player is currently in, or null. */
    @Nullable
    public static DuelManager.Duel getDuel(@Nonnull UUID player){
        return DuelManager.getDuel(player);
    }

    /**
     * @return the combined meditation multiplier the formations and dwelling
     * covering this chunk are worth this player - above 1 inside a Qi-gathering
     * array or their own abode, below 1 where a warding array chokes an outsider.
     */
    public static float getMeditationRegenMultiplier(@Nonnull String world, int chunkX, int chunkZ, @Nonnull UUID player){
        return FormationManager.getMeditationRegenMultiplier(world, chunkX, chunkZ, player)
                * DwellingManager.getMeditationRegenMultiplier(world, chunkX, chunkZ, player);
    }

    /**
     * @return whether the Marriage mod is installed, which is what Partnered
     * Cultivation is gated on. False on a server without it, in which case
     * nothing in {@link CultivationConfigs#partner()} has any effect.
     */
    public static boolean isMarriageInstalled(){
        return Cultivation.ifMarriage();
    }

    /**
     * @return whether Endless Leveling is installed. When it is, Cultivation
     * hands max health and outgoing damage to EL rather than applying them
     * itself, so the two progressions add rather than multiply - see
     * {@link CultivationConfigs#endlessLeveling()} for the switches.
     *
     * <p>Worth checking from an addon that applies stats of its own: on a server
     * running both, EL is where a bonus belongs.</p>
     */
    public static boolean isEndlessLevelingInstalled(){
        return Cultivation.ifEndlessLeveling();
    }

    /**
     * @return whether Cultivation's {@code %cultivation_...%} PlaceholderAPI
     * expansion is registered and answering - PAPI installed AND it accepted the
     * registration.
     *
     * <p>Worth checking before an addon registers an expansion of its own under a
     * colliding identifier, or before a format is written that assumes the
     * placeholders resolve.</p>
     */
    public static boolean isPlaceholderApiRegistered(){
        return Cultivation.ifPlaceholderAPI();
    }
}
