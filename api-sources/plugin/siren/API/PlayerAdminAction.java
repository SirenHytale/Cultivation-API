package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * An action targeting whichever player an admin has selected on
 * {@code /cultivation admin}'s Players tab - the per-player analog of
 * {@link AdminConfigSection}, which is the server-wide equivalent.
 *
 * <p>Realm, Stage, Race, Qi, Ascension and Level are rendered as fixed rows
 * because Cultivation owns those concepts directly. A concept an ADDON owns
 * entirely - a bloodline, a constitution, a Heavenly Flame - has no such row,
 * and Cultivation must never be taught one specific addon's vocabulary to add
 * it one. Registering a {@link PlayerAdminAction} instead adds a generic
 * dropdown-plus-button row to the Players tab, appended below Cultivation's own
 * fixed rows, without this mod knowing anything about what the action does.</p>
 *
 * <h2>How to use it</h2>
 *
 * <pre>{@code
 * CultivationAPI.registerPlayerAdminAction(new PlayerAdminAction(){
 *     public String getKey(){ return "MyMod:setThing"; }
 *     public Message getLabel(){ return Message.translation("server.mymod.admin.fieldThing"); }
 *     public Message getButtonLabel(){ return Message.translation("server.mymod.admin.setThing"); }
 *     public String getTooltip(){ return null; }
 *     public List<AdminConfigChoice> getChoices(){ return Thing.allAsChoices(); }
 *     public String getDefaultValue(){ return Thing.NONE; }
 *     public void apply(Store<EntityStore> targetStore, Ref<EntityStore> targetRef,
 *             PlayerRef targetPlayerRef, PlayerRef actingAdmin, boolean targetingSelf, String value){
 *         ThingManager.applySet(targetStore, targetRef, targetPlayerRef, actingAdmin, targetingSelf, value);
 *     }
 * });
 * }</pre>
 *
 * <p>Call from your plugin's {@code setup()}. Load order does not matter:
 * nothing reads the registry until an admin actually opens the Players tab.
 * Registering the same key twice replaces the first, so this is safe across a
 * reload of your plugin. Withdraw it from your {@code shutdown()} with
 * {@link CultivationAPI#unregisterPlayerAdminAction}, or a reload leaves a row
 * behind pointing at classes that are gone.</p>
 *
 * <h2>Key namespacing</h2>
 *
 * <p>{@link #getKey()} doubles as the action string the row's button sends
 * back, riding the exact same channel Cultivation's own built-in actions
 * ({@code "setrealm"}, {@code "setlevel"}, ...) already use. It MUST contain a
 * namespace separator (a colon is the convention - {@code "HeavenlyFlames:setFlame"})
 * so it can never collide with a bare built-in key. Registration enforces this:
 * a key with no {@code ':'}, or one that exactly matches an entry in
 * {@code plugin.siren.Utils.UI.Admin.AdminPlayerActions#RESERVED_KEYS} (the full,
 * authoritative list of every reserved built-in key), is rejected with a logged
 * warning rather than registered - check that set before naming your key.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Same contract as Cultivation's own built-in player actions and
 * {@link CultivationModifierSource}: {@link #apply} runs already resolved onto
 * the TARGET player's own world thread (which may differ from the admin's), not
 * the admin's. {@code targetStore}/{@code targetRef} are the target's; report
 * outcomes purely through {@code PlayerRef#sendMessage} on
 * {@code targetPlayerRef}/{@code actingAdmin}, exactly as every built-in action
 * does - there is no page for this code to talk back to, since it may no longer
 * be open by the time this runs.</p>
 *
 * <p>{@link #apply} is handed a real {@code Store<EntityStore>}, not a
 * {@code CommandBuffer} - it is invoked from {@code AdminPlayerActions.run}
 * inside {@code CompletableFuture.runAsync(..., targetWorld)}, the same calling
 * context every built-in action (Realm, Stage, Qi, Level, ...) already writes
 * through directly in. This is NOT a ticking ECS system or event listener, so
 * the "route structural writes through a CommandBuffer" rule that applies there
 * does not apply here: writing through {@code targetStore} directly - including
 * {@code Store.putComponent} - is the established, correct pattern for this
 * interface. See {@code SetRealmCmd}, {@code SetLevelCmd} and
 * {@code HeavenlyFlameCmd.applyGive} for working examples.</p>
 */
public interface PlayerAdminAction {

    /**
     * A stable id, unique across every registered action, namespaced with your
     * mod name (e.g. {@code "HeavenlyFlames:setFlame"}) so it can never collide
     * with a built-in action's bare key. Also the literal action string sent
     * back by this row's button and dispatched to {@link #apply}.
     */
    @Nonnull
    String getKey();

    /** The row's label, to the left of the dropdown. Use {@code Message.translation(...)} so it localises. */
    @Nonnull
    Message getLabel();

    /** The inline button's text. Use {@code Message.translation(...)} so it localises. */
    @Nonnull
    Message getButtonLabel();

    /**
     * A one-line explanation shown as the row's tooltip, or {@code null} for
     * none. A plain String rather than a {@link Message}, and therefore not
     * translatable - {@code TooltipText} is a String property client-side and a
     * Message pushed at it disconnects the player, the same rule
     * {@link AdminConfigField#getTooltip()} follows.
     */
    @Nullable
    String getTooltip();

    /**
     * The dropdown's options, in display order. Re-read on every render, so a
     * set that depends on what your own mod has registered (e.g. every
     * currently-defined Heavenly Flame) stays current.
     */
    @Nonnull
    List<AdminConfigChoice> getChoices();

    /**
     * The option id pre-selected before an admin has touched this row - shown
     * the first time the Players tab is opened and after applying a different
     * action, same as every fixed row's own default.
     */
    @Nonnull
    String getDefaultValue();

    /**
     * Where this row sits among every registered action, lowest first. Rows
     * declaring the same order keep registration order.
     *
     * <p>Defaults to {@link AdminConfigSection#SORT_LAST}, the same "after
     * everything built-in" default {@link AdminConfigSection} uses - the Players
     * tab's own fixed rows are not part of this registry at all, so an addon row
     * always appears below them regardless of this value.</p>
     */
    default int getSortOrder(){
        return AdminConfigSection.SORT_LAST;
    }

    /**
     * @return whether this row should appear at all right now. Read on every
     * render, so an action belonging to a subsystem the server owner has
     * switched off can hide itself instead of offering a control that does
     * nothing.
     */
    default boolean isVisible(){
        return true;
    }

    /**
     * Applies this action to the target player, already resolved onto their own
     * world thread. See the class-level threading note before implementing this.
     *
     * @param targetStore   the target's component store.
     * @param targetRef     the target's entity reference.
     * @param targetPlayerRef the target's player reference - message them here to
     *                      tell them what changed.
     * @param actingAdmin   the admin who clicked the button - message them here
     *                      to confirm it worked, unless {@code targetingSelf}.
     * @param targetingSelf true when the admin targeted themselves, so a
     *                      confirmation is not sent twice to the same chat.
     * @param value         the chosen option's id - {@link #getDefaultValue()}
     *                      when nothing has been picked yet. Arrived from a
     *                      client; re-resolve it against your own known values
     *                      rather than trusting it outright.
     */
    void apply(@Nonnull Store<EntityStore> targetStore, @Nonnull Ref<EntityStore> targetRef,
              @Nonnull PlayerRef targetPlayerRef, @Nonnull PlayerRef actingAdmin,
              boolean targetingSelf, @Nonnull String value);
}
