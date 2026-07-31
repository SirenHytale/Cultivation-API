package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.ECS.SkillTree.SkillTreeBranch;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One look a player can wear: the colors of the skill tree's branch halos, and
 * the {@code .ui} documents every Cultivation menu and the HUD are drawn from.
 *
 * <p>Register one with {@link CultivationAPI#registerPalette}. Cultivation ships
 * exactly one - the crimson-and-gold {@link #DEFAULT_KEY default} - and a mod may
 * add as many more as it likes:</p>
 *
 * <pre>{@code CultivationAPI.registerPalette(
 *         CultivationPalette.builder("myMod:frost")
 *                 .name("server.myMod.palette.frost")
 *                 .swatch(0x9FD8F0)
 *                 .documentRoot("Pages/MyMod/Frost/")
 *                 .documents(FROST_DOCUMENTS)
 *                 .halo(SkillTreeBranch.VITALITY, 0x8FB8D8)
 *                 // ... the other eight
 *                 .build());}</pre>
 *
 * <h2>Why a palette is a set of documents, not a set of colors</h2>
 *
 * <p>It would be tidier if a palette were twenty hex values pushed at the client.
 * The engine does not allow it. {@code PatchStyle} - the only style object Java
 * may push - carries a flat background fill and nothing else; there is no
 * {@code TextStyle} class at all, so a label's color cannot be changed once it is
 * drawn. Half of what this mod's UI is made of is colored text.</p>
 *
 * <p>So a palette works the way this mod already handles its locked/affordable/
 * owned skill nodes, its ordinary-versus-your-own ranking rows, and its two recipe
 * row shapes: <b>one static document per variant</b>, chosen when the page is
 * built. {@link #resolveDocument} is that choice. Everything else about the page -
 * every layout number, every id, every event binding - is identical across
 * palettes, because each variant is generated from the same source document with
 * only its colors substituted.</p>
 *
 * <h2>Halos are the exception</h2>
 *
 * <p>The nine skill-tree branch halos <i>are</i> plain hex, because they are bare
 * {@code Group} backgrounds that Cultivation already pushes at runtime through
 * {@code PatchStyle} - the one thing the engine does support. A palette therefore
 * carries them directly, and {@link #getHalo} answers for any branch.</p>
 *
 * <p>Set all nine or none. Nine hues encode <i>which branch a node belongs to</i>,
 * so they must stay distinguishable from each other; a palette that recolors four
 * and leaves five crimson does not read as a palette, it reads as a bug. Passing
 * some but not all is refused at {@link Builder#build}.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Register from your plugin's {@code setup()}; load order does not matter, since
 * nothing reads the registry until a player opens a menu. {@link #isAvailableTo} is
 * called on the world thread that owns the viewing player, so it may read that
 * player's components but must not write to the Store.</p>
 */
public final class CultivationPalette {

    /**
     * The key of Cultivation's own crimson-and-gold look - the one every player
     * has until they choose otherwise, and the one a palette falls back to when
     * the mod that registered theirs is uninstalled.
     */
    public static final String DEFAULT_KEY = "cultivation:default";

    private final String key;
    private final Supplier<Message> name;
    private final String sectionKey;
    private final int swatch;
    private final String documentRoot;
    private final Set<String> documents;
    private final Map<SkillTreeBranch, Integer> halos;
    private final String permission;
    private final Predicate<PlayerRef> visible;

    private CultivationPalette(Builder builder) {
        this.key = builder.key;
        this.name = builder.name;
        this.sectionKey = builder.sectionKey;
        this.swatch = builder.swatch;
        this.documentRoot = builder.documentRoot;
        this.documents = Set.copyOf(builder.documents);
        this.halos = Collections.unmodifiableMap(new EnumMap<>(builder.halos));
        this.permission = builder.permission;
        this.visible = builder.visible;
    }

    @Nonnull
    public static Builder builder(@Nonnull String key) {
        return new Builder(key);
    }

    /** The id this palette is registered under, and what is saved on the player. */
    @Nonnull
    public String getKey() {
        return this.key;
    }

    /** The picker tile's text, resolved fresh per draw so a {@link CultivationTheme} still applies. */
    @Nonnull
    public Message getName() {
        return this.name.get();
    }

    /** Translation key of the caption this palette groups under, or null for ungrouped. */
    @Nullable
    public String getSectionKey() {
        return this.sectionKey;
    }

    /** RGB shown on the picker tile, so the grid can be scanned by eye. */
    public int getSwatch() {
        return this.swatch;
    }

    /** Whether this palette recolors the skill tree's halos at all. */
    public boolean hasHalos() {
        return !this.halos.isEmpty();
    }

    /**
     * This palette's color for one branch's halo, or {@code fallback} if it does
     * not recolor halos. Brightness for locked/affordable/owned is applied by the
     * skill tree on top of whatever this returns.
     */
    public int getHalo(@Nonnull SkillTreeBranch branch, int fallback) {
        Integer rgb = this.halos.get(branch);
        return rgb == null ? fallback : rgb;
    }

    /**
     * The document this palette wants drawn in place of {@code basePath}, or
     * {@code basePath} itself when it ships no variant for it.
     *
     * <p>Falling back rather than guessing is the whole point. A path that does
     * not resolve fails the entire UI load on the client - not just the one
     * element - and no validator in this workspace checks {@code append()} paths,
     * so a typo would reach players silently. A palette therefore only ever
     * redirects documents it has explicitly declared through
     * {@link Builder#documents}, which its generator writes from the files it
     * actually emitted. A page added to Cultivation after a palette was generated
     * simply keeps its default look until that palette is regenerated.</p>
     *
     * @param basePath a document path as the base mod names it, e.g.
     *                 {@code "Pages/Cultivation/CultivationStatsPage.ui"}
     */
    @Nonnull
    public String resolveDocument(@Nonnull String basePath) {
        if (this.documentRoot == null) {
            return basePath;
        }

        int slash = basePath.lastIndexOf('/');
        String fileName = slash < 0 ? basePath : basePath.substring(slash + 1);

        return this.documents.contains(fileName) ? this.documentRoot + fileName : basePath;
    }

    /** The document file names this palette ships a variant of. */
    @Nonnull
    public Set<String> getDocuments() {
        return this.documents;
    }

    /**
     * Whether this player may wear this palette. False hides it from the picker
     * and refuses to apply it, so a hand-crafted event packet cannot reach a
     * palette its owner was never offered.
     */
    public boolean isAvailableTo(@Nonnull PlayerRef playerRef) {
        if (this.permission != null && !playerRef.hasPermission(this.permission)) {
            return false;
        }

        return this.visible == null || this.visible.test(playerRef);
    }

    public static final class Builder {
        private final String key;
        private Supplier<Message> name;
        private String sectionKey;
        private int swatch = 0xD9A63E;
        private String documentRoot;
        private Set<String> documents = new HashSet<>();
        private final Map<SkillTreeBranch, Integer> halos = new EnumMap<>(SkillTreeBranch.class);
        private String permission;
        private Predicate<PlayerRef> visible;

        private Builder(@Nonnull String key) {
            this.key = key;
            this.name = () -> Message.raw(key);
        }

        /** Sets the picker tile's text from a translation key - the only form that localises. */
        @Nonnull
        public Builder name(@Nonnull String translationKey) {
            this.name = () -> Text.of(translationKey);
            return this;
        }

        /** Groups this palette under a captioned section in the picker. */
        @Nonnull
        public Builder section(@Nonnull String sectionKey) {
            this.sectionKey = sectionKey;
            return this;
        }

        /** The color drawn on the picker tile. Pick the one that most says "this look". */
        @Nonnull
        public Builder swatch(int swatch) {
            this.swatch = swatch;
            return this;
        }

        /**
         * The folder this palette's document variants live in, relative to
         * {@code Common/UI/Custom/} and ending in a slash - e.g.
         * {@code "Pages/MyMod/Frost/"}.
         *
         * <p>Namespace it under your own mod's folder. Every mod's
         * {@code Common/UI/Custom/} merges into one tree, so two mods that both
         * ship {@code Pages/Cultivation/CultivationStatsPage.ui} would collide.</p>
         */
        @Nonnull
        public Builder documentRoot(@Nonnull String documentRoot) {
            this.documentRoot = documentRoot.endsWith("/") ? documentRoot : documentRoot + "/";
            return this;
        }

        /**
         * The document file names this palette ships - bare names, no folders
         * ({@code "CultivationStatsPage.ui"}). Anything not listed keeps its
         * default look rather than resolving to a file that might not exist.
         */
        @Nonnull
        public Builder documents(@Nonnull Set<String> documents) {
            this.documents = new HashSet<>(documents);
            return this;
        }

        /** Adds one branch's halo color. Set all nine, or none at all. */
        @Nonnull
        public Builder halo(@Nonnull SkillTreeBranch branch, int rgb) {
            this.halos.put(branch, rgb);
            return this;
        }

        /** Hides this palette from players without this permission. */
        @Nonnull
        public Builder permission(@Nonnull String permission) {
            this.permission = permission;
            return this;
        }

        /** Hides this palette unless this returns true. Combines with {@link #permission}; both must pass. */
        @Nonnull
        public Builder visible(@Nonnull Predicate<PlayerRef> visible) {
            this.visible = visible;
            return this;
        }

        @Nonnull
        public CultivationPalette build() {
            if (this.key == null || this.key.isEmpty()) {
                throw new IllegalStateException("A Cultivation palette needs an id.");
            }

            if (!this.halos.isEmpty() && this.halos.size() != SkillTreeBranch.values().length) {
                throw new IllegalStateException("Cultivation palette '" + this.key + "' sets "
                        + this.halos.size() + " of " + SkillTreeBranch.values().length
                        + " branch halos. Set all nine or none - a half-recolored tree reads as a bug,"
                        + " and the nine hues are what tell branches apart.");
            }

            if (this.documentRoot == null && !this.documents.isEmpty()) {
                throw new IllegalStateException("Cultivation palette '" + this.key
                        + "' lists documents but no documentRoot to find them under.");
            }

            return new CultivationPalette(this);
        }
    }
}
