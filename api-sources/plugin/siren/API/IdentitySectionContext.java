package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Everything an {@link IdentitySection} needs to build its content or handle
 * its own event, handed in by {@code IdentityUIPage} rather than assembled by
 * the section itself.
 *
 * <h2>Addressing your own widgets</h2>
 *
 * <p>{@link #getContainerSelector()} is the section's own indexed slot in the
 * page's {@code #RegisteredSectionList} - build into it with
 * {@link UICommandBuilder#append} exactly as {@code RaceCard.ui} is appended
 * into {@code #RaceList}, and address anything inside it through
 * {@link #selector} rather than a bare id. UI ids are global to the loaded
 * page; another registered section's content sits in the same document.</p>
 *
 * <h2>Wiring a click back to your own {@link IdentitySection#handleAction}</h2>
 *
 * <p>{@link #bindAction} and {@link #bindValueAction} route through one shared
 * event channel every registered section uses, keyed by
 * {@link #getSectionId()} so the host page dispatches the click to the right
 * section without a per-section codec field. Use {@link #bindAction} for a
 * plain click (a select or equip button); use {@link #bindValueAction} for a
 * dropdown or text field whose current value should arrive with the click -
 * the same "push on change" shape {@code CultivationAdminUIPage}'s own rows
 * use. Do not build the {@code SectionId}/{@code SectionAction} event binding
 * by hand; the {@code '@'}-prefix on the value key that makes the client
 * resolve a selector into its live value is easy to get wrong and this
 * codebase has been burned by it before.</p>
 */
public final class IdentitySectionContext {

    /** Literal key carrying which registered section a click or push came from. Do not use directly - see {@link #bindAction}. */
    public static final String KEY_SECTION_ID = "SectionId";
    /** Literal key carrying the action string the section's own binding was registered with. */
    public static final String KEY_SECTION_ACTION = "SectionAction";
    /** {@code '@'}-prefixed key: the client resolves this to the bound widget's live value before sending it. */
    public static final String KEY_SECTION_VALUE = "@SectionValue";

    private final UICommandBuilder commandBuilder;
    private final UIEventBuilder eventBuilder;
    private final Store<EntityStore> store;
    private final Ref<EntityStore> ref;
    private final PlayerRef playerRef;
    private final CultivationPalette palette;
    private final String containerSelector;
    private final String sectionId;

    public IdentitySectionContext(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder,
                                  @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                  @Nonnull PlayerRef playerRef, @Nullable CultivationPalette palette,
                                  @Nonnull String containerSelector, @Nonnull String sectionId) {
        this.commandBuilder = commandBuilder;
        this.eventBuilder = eventBuilder;
        this.store = store;
        this.ref = ref;
        this.playerRef = playerRef;
        this.palette = palette;
        this.containerSelector = containerSelector;
        this.sectionId = sectionId;
    }

    @Nonnull
    public UICommandBuilder getCommandBuilder() {
        return this.commandBuilder;
    }

    @Nonnull
    public UIEventBuilder getEventBuilder() {
        return this.eventBuilder;
    }

    @Nonnull
    public Store<EntityStore> getStore() {
        return this.store;
    }

    @Nonnull
    public Ref<EntityStore> getRef() {
        return this.ref;
    }

    @Nonnull
    public PlayerRef getPlayerRef() {
        return this.playerRef;
    }

    /** The viewer's resolved palette, for resolving any of your own documents through {@link CultivationAPI#document}. */
    @Nullable
    public CultivationPalette getPalette() {
        return this.palette;
    }

    /** This section's own indexed slot in {@code #RegisteredSectionList} - build your content into this. */
    @Nonnull
    public String getContainerSelector() {
        return this.containerSelector;
    }

    /** The id this section was registered under - what {@link #bindAction} stamps onto every binding it makes. */
    @Nonnull
    public String getSectionId() {
        return this.sectionId;
    }

    /** @return {@code getContainerSelector() + " " + childSelector} - a selector scoped to this section's own container. */
    @Nonnull
    public String selector(@Nonnull String childSelector) {
        return this.containerSelector + " " + childSelector;
    }

    /**
     * Binds an {@code Activating} (or other) event on a widget inside this
     * section's container so it routes to
     * {@link IdentitySection#handleAction} with this {@code action} string
     * and a null value - the shape a select/equip button uses.
     *
     * @param childSelector a selector relative to this section's container
     *                      (see {@link #selector}), e.g. {@code "#SelectButton"}.
     */
    public void bindAction(@Nonnull CustomUIEventBindingType type, @Nonnull String childSelector, @Nonnull String action) {
        this.eventBuilder.addEventBinding(type, selector(childSelector), new EventData()
                .append(KEY_SECTION_ID, this.sectionId)
                .append(KEY_SECTION_ACTION, action));
    }

    /**
     * Binds a {@code ValueChanged} event on a widget inside this section's
     * container so it routes to {@link IdentitySection#handleAction} with
     * this {@code action} string and the widget's own {@code .Value} at the
     * time of the push - the shape a dropdown or text field uses.
     * {@code locksInterface} is false, matching every other push-on-change
     * binding in this mod, since there is nothing to display in response.
     *
     * @param childSelector a selector relative to this section's container
     *                      (see {@link #selector}), e.g. {@code "#Input"}.
     */
    public void bindValueAction(@Nonnull String childSelector, @Nonnull String action) {
        String widgetSelector = selector(childSelector);
        this.eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, widgetSelector, new EventData()
                .append(KEY_SECTION_ID, this.sectionId)
                .append(KEY_SECTION_ACTION, action)
                .append(KEY_SECTION_VALUE, widgetSelector + ".Value"), false);
    }
}
