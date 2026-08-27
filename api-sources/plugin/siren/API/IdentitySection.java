package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A third (or fourth, ...) tab another mod contributes to Cultivation's
 * Identity page, alongside the built-in Race and Titles tabs.
 *
 * <p>Register from your plugin's {@code setup()} with
 * {@link CultivationAPI#registerIdentitySection}. See
 * {@link CultivationAPI#newIdentitySection} for building one from a build
 * callback (and an optional action handler) without implementing this
 * interface directly.</p>
 *
 * <h2>Where this sits relative to Race and Titles</h2>
 *
 * <p>Race and Titles are NOT entries in this registry - they stay the two
 * hardcoded sections {@code IdentityUIPage} has always had, because both carry
 * partial-update logic (re-stating one card's button after a select, without
 * rebuilding the whole list) that is specific to their own content and would
 * gain nothing from being forced through a generic callback. What IS generic
 * is the tab bar itself: every registered section gets its own tab button
 * alongside Race and Titles, toggled the same way. A registered section's tab
 * always renders after both built-ins, in {@link #getSortOrder()} order among
 * itself and whatever else is registered.</p>
 *
 * <h2>Built once, shown or hidden</h2>
 *
 * <p>{@link #build} is called exactly once per page open (or reopen), the same
 * as every other section on this page - never on every tab switch. Switching
 * tabs only flips {@code .Visible} on the section's container and
 * {@code .Disabled} on its tab button; re-building on every switch would drop
 * the event bindings {@link #build} registered, the mistake this page's own
 * Race/Titles sections were already written to avoid.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>{@link #build}, {@link #handleAction} and {@link #isVisible} are all
 * called on the viewing player's world thread, while their page is being
 * built or has just received a click - so they may read that player's
 * components directly, but must not write to the Store (use a CommandBuffer
 * for that, per the ECS rules the rest of this API follows).</p>
 *
 * <h2>A section that throws does not take the page down</h2>
 *
 * <p>Every call into a registered section is caught by the host page and
 * logged rather than allowed to fail the whole Identity page for every
 * player - the same guard {@code CultivationNav} puts around opening an
 * addon's own nav page and {@code CultivationAdminUIPage} puts around a
 * registered {@link PlayerAdminAction}'s row.</p>
 */
public interface IdentitySection {

    /**
     * A stable id, unique across every registered section, which is what a
     * click on this section's tab sends back and what {@link #handleAction}
     * is dispatched by. Namespace it with your mod name (e.g.
     * {@code "CultivationClasses:classes"}) - the same convention
     * {@link AdminConfigSection#getKey()} and {@link CultivationMenuPage}
     * ask for. Must not equal {@code "race"} or {@code "titles"}, the two
     * ids the built-in tabs have always used.
     */
    @Nonnull
    String getId();

    /** The tab button's text. Use a {@code Message.translation(...)} so it localises. */
    @Nonnull
    Message getLabel();

    /**
     * Builds this section's content once, into the container
     * {@link IdentitySectionContext#getContainerSelector()} points at. Append
     * into that container (or a selector built from it via
     * {@link IdentitySectionContext#selector}) exactly as {@code RaceCard.ui}
     * is appended into {@code #RaceList} - never address a bare, unscoped id,
     * since UI ids are global to the loaded page and another section (or
     * another registered section) may reuse the same widget name.
     */
    void build(@Nonnull IdentitySectionContext context);

    /**
     * Handles a click or value push from a widget this section bound with
     * {@link IdentitySectionContext#bindAction} or
     * {@link IdentitySectionContext#bindValueAction}. Called on the same
     * event round-trip the click arrived on; the default does nothing, for a
     * section whose content is informational only.
     *
     * @param action the action string the binding was registered with.
     * @param value  the resolved widget value for a {@code bindValueAction}
     *               binding, or null for a plain {@code bindAction} click.
     */
    default void handleAction(@Nonnull IdentitySectionContext context, @Nonnull String action, @Nullable String value) {
    }

    /**
     * @return whether this player gets this tab at all. Checked on every
     * build, so a section belonging to something the viewer hasn't unlocked
     * (or that the server owner has switched off) can hide itself rather than
     * offering a tab that does nothing. Defaults to true.
     */
    default boolean isVisible(@Nonnull PlayerRef playerRef) {
        return true;
    }

    /**
     * Where this tab sits among other REGISTERED sections - Race and Titles
     * are not part of this ordering, since a registered tab always renders
     * after both. Lower goes first; sections declaring the same order keep
     * registration order. Defaults to {@link #SORT_DEFAULT}.
     */
    default int getSortOrder() {
        return SORT_DEFAULT;
    }

    /** The default sort order - registration order among other defaulted sections. */
    int SORT_DEFAULT = 1000;

    /** A callback for {@link #handleAction}, for building a section with {@link CultivationAPI#newIdentitySection}. */
    @FunctionalInterface
    interface ActionHandler {
        void handle(@Nonnull IdentitySectionContext context, @Nonnull String action, @Nullable String value);
    }
}
