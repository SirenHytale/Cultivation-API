package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One button on the nav bar shared by every Cultivation menu - Cultivation's
 * own ten pages, and any page another mod contributes.
 *
 * <p>Build one with {@link #builder(String)} and hand it to
 * {@link CultivationAPI#registerMenuPage}. The only required parts are an id, a
 * label, and what to open:</p>
 *
 * <pre>{@code CultivationAPI.registerMenuPage(
 *         CultivationMenuPage.builder("myMod:alchemy")
 *                 .label("server.myMod.nav.alchemy")
 *                 .permission("myMod.alchemy")
 *                 .onOpen((store, ref, playerRef) ->
 *                         CultivationAPI.openMenuPage(store, ref, new MyAlchemyUIPage(playerRef)))
 *                 .build());}</pre>
 *
 * <p>The bar scrolls horizontally, so adding pages never crowds the existing
 * ones out - there is no practical limit on how many may be registered.</p>
 *
 * <h2>Ids</h2>
 *
 * <p>The id is what a click sends back, so namespace it with your mod name
 * ({@code "myMod:alchemy"}) exactly as {@link AdminConfigSection#getKey} asks.
 * Registering an id twice replaces the first, which is how a mod may take over a
 * built-in entry (point Cultivation's {@code "race"} button at its own page, say)
 * rather than adding beside it. Cultivation's own ids are the bare words
 * {@code overview}, {@code settings}, {@code race}, {@code skilltree},
 * {@code dao}, {@code rankings}, {@code codex}, {@code sense}, {@code admin}
 * and {@code info}.</p>
 *
 * <h2>Ids without a button</h2>
 *
 * <p>Four more ids exist and open real pages, but Cultivation no longer puts
 * them on the bar - {@code bonuses} was folded into {@code overview},
 * {@code titles} into {@code race} (which is why that button now reads
 * "Identity"), and {@code profiles} and {@code keybinds} are reached from
 * {@code settings}. Registering any of them yourself puts a button back, since
 * the bar is drawn purely from what is in this registry.</p>
 *
 * <p>Note also that {@code sense} carries a {@link Builder#visible} gate rather
 * than a permission: its button is hidden until the viewer's realm actually
 * unlocks the spirit sense.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Register from your plugin's {@code setup()}; load order does not matter,
 * since nothing reads the registry until a player opens a menu. {@link #getLabel},
 * {@link #isVisibleTo} and {@link #open} are all called on the world thread that
 * owns the viewing player, while their page is being built or swapped - so they
 * may read that player's components directly, but must not write to the Store
 * (use a CommandBuffer for that, per the ECS rules the rest of this API follows).</p>
 */
public final class CultivationMenuPage {

    /**
     * Where Cultivation's own first page sits. Its ten built-ins are spaced 100
     * apart from here (bar Keybinds, slotted in at 650, and Admin at
     * {@link #SORT_LAST}), so a mod may slot a page between two of them
     * ({@code sortOrder(450)} lands between the Skill Tree and Bonuses) without
     * anything needing to be renumbered.
     */
    public static final int SORT_FIRST = 100;

    /**
     * Where a page with no declared order goes: after every built-in except Admin,
     * in registration order among its peers.
     */
    public static final int SORT_DEFAULT = 1000;

    /** Where the built-in Admin button sits - deliberately last on the bar. */
    public static final int SORT_LAST = 9000;

    /** What a nav button does when it is clicked. */
    @FunctionalInterface
    public interface Opener {
        /**
         * Opens this page in place of whichever Cultivation menu the player has
         * open. Usually one call to
         * {@link CultivationAPI#openMenuPage(Store, Ref, com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage)}.
         */
        void open(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef);
    }

    private final String key;
    private final Supplier<Message> label;
    private final int sortOrder;
    private final String permission;
    private final Predicate<PlayerRef> visible;
    private final Opener opener;

    private CultivationMenuPage(Builder builder) {
        this.key = builder.key;
        this.label = builder.label;
        this.sortOrder = builder.sortOrder;
        this.permission = builder.permission;
        this.visible = builder.visible;
        this.opener = builder.opener;
    }

    @Nonnull
    public static Builder builder(@Nonnull String key) {
        return new Builder(key);
    }

    /** The id this page is registered under, and what its click sends back. */
    @Nonnull
    public String getKey() {
        return this.key;
    }

    /**
     * The button's text, resolved fresh every time the bar is drawn - so a label
     * built from a translation key still picks up a {@link CultivationTheme}
     * installed after this page was registered.
     */
    @Nonnull
    public Message getLabel() {
        return this.label.get();
    }

    /** Position on the bar; lower is further left. See {@link #SORT_FIRST}. */
    public int getSortOrder() {
        return this.sortOrder;
    }

    /**
     * Whether this player gets a button at all. False hides it outright, which is
     * what a permission gate should do for a page the player cannot use - there is
     * no greyed-out state here, since the bar already uses "disabled" to mark the
     * page you are currently on.
     *
     * <p>Checked again before the page actually opens, so a hand-crafted event
     * packet cannot reach a page the button was hidden for.</p>
     */
    public boolean isVisibleTo(@Nonnull PlayerRef playerRef) {
        if (this.permission != null && !playerRef.hasPermission(this.permission)) {
            return false;
        }

        return this.visible == null || this.visible.test(playerRef);
    }

    /** Runs this page's opener. Callers should check {@link #isVisibleTo} first. */
    public void open(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef) {
        this.opener.open(store, ref, playerRef);
    }

    public static final class Builder {
        private final String key;
        private Supplier<Message> label;
        private int sortOrder = SORT_DEFAULT;
        private String permission;
        private Predicate<PlayerRef> visible;
        private Opener opener;

        private Builder(@Nonnull String key) {
            this.key = key;
            this.label = () -> Message.raw(key);
        }

        /**
         * Sets the button text from a translation key - the normal case, and the
         * only one that localises. Resolved per draw, and routed through any
         * installed {@link CultivationTheme} first.
         */
        @Nonnull
        public Builder label(@Nonnull String translationKey) {
            this.label = () -> Text.of(translationKey);
            return this;
        }

        /** Sets a fixed button text. Prefer {@link #label(String)} so it localises. */
        @Nonnull
        public Builder label(@Nonnull Message label) {
            this.label = () -> label;
            return this;
        }

        /** Sets a button text computed per draw, e.g. one carrying a live count. */
        @Nonnull
        public Builder label(@Nonnull Supplier<Message> label) {
            this.label = label;
            return this;
        }

        /**
         * Position on the bar. Defaults to {@link #SORT_DEFAULT} - after every
         * built-in but before Admin. See {@link CultivationMenuPage#SORT_FIRST}
         * for slotting between two built-ins.
         */
        @Nonnull
        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        /**
         * Hides the button from players without this permission, and refuses to
         * open the page for them. This is what gates Cultivation's own Admin
         * button on {@code cultivation.admin}.
         */
        @Nonnull
        public Builder permission(@Nonnull String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Hides the button unless this returns true - for a gate a permission
         * cannot express, such as a page that only means anything once the player
         * has joined a sect. Runs on the viewing player's world thread; combine
         * freely with {@link #permission}, both must pass.
         */
        @Nonnull
        public Builder visible(@Nonnull Predicate<PlayerRef> visible) {
            this.visible = visible;
            return this;
        }

        /** What the button does. Required. */
        @Nonnull
        public Builder onOpen(@Nonnull Opener opener) {
            this.opener = opener;
            return this;
        }

        @Nonnull
        public CultivationMenuPage build() {
            if (this.key == null || this.key.isEmpty()) {
                throw new IllegalStateException("A Cultivation menu page needs an id.");
            }

            if (this.opener == null) {
                throw new IllegalStateException("Cultivation menu page '" + this.key + "' has no onOpen action.");
            }

            return new CultivationMenuPage(this);
        }
    }
}
