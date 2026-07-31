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
 * One cosmetic label a player may wear next to their name - on the Rankings
 * board, a sect roster, in chat, and the overhead nameplate. Purely
 * decorative: nothing here or in its registry grants a stat, a permission
 * side-effect, or any gameplay change.
 *
 * <p>Register one with {@link CultivationAPI#registerTitle}. Cultivation ships
 * a tasteful built-in set derived from realm, stage, dao, moral path and sect
 * rank (see {@code CultivationTitles.registerBuiltins}), and a mod may add as
 * many more as it likes:</p>
 *
 * <pre>{@code CultivationAPI.registerTitle(
 *         CultivationTitle.builder("myMod:dragonSlayer")
 *                 .name("server.myMod.title.dragonSlayer")
 *                 .section("server.myMod.title.section")
 *                 .hint("server.myMod.title.hint.dragonSlayer")
 *                 .unlocked((store, ref, playerRef) -> MyMod.hasSlainDragon(playerRef.getUuid()))
 *                 .build());}</pre>
 *
 * <h2>Two different gates</h2>
 *
 * <p>{@link #isVisibleTo} is an operator/feature switch - a permission node or
 * a server-config predicate - and hides the title from the picker entirely,
 * exactly like {@link CultivationMenuPage#isVisibleTo} and
 * {@link CultivationPalette#isAvailableTo}. {@link #isUnlockedFor} asks a
 * different question: whether THIS player has actually EARNED it. An unearned
 * title stays on the picker - greyed, with {@link #getHint} explaining what
 * unlocks it, this mod's own convention for a locked race card or skill node
 * (see RaceCard.ui, SkillTreeNodeLocked.ui) - rather than vanishing, so a
 * player can see what to aim for. A title with no {@link Builder#unlocked} at
 * all is available to everyone who can see it - fine for a purely
 * permission-gated title such as a donor perk, where "visible" already says
 * everything that matters.</p>
 *
 * <h2>Why unlocking takes a Store/Ref, not just a PlayerRef</h2>
 *
 * <p>{@link CultivationPalette}'s and {@link CultivationMenuPage}'s gates only
 * ever ask "who is this player" - a permission node or a server-config
 * predicate needs nothing more. A title's built-in set is the opposite: EVERY
 * one of them is a question about that player's own components (their realm,
 * their dao, their sect rank), which a bare {@link PlayerRef} cannot answer.
 * {@link UnlockCheck} therefore takes the same (Store, Ref, PlayerRef) triple
 * {@link CultivationMenuPage.Opener} already does, which lets a mod's own
 * unlock condition read any component on the wearer - never on anyone else,
 * since a player can only equip a title for themselves.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Register from your plugin's {@code setup()}; load order does not matter,
 * since nothing reads the registry until a player opens the titles menu or
 * equips one. {@link #isVisibleTo} and {@link #isUnlockedFor} are called on
 * the world thread that owns the wearer, both while their picker page is
 * being drawn and again when their equip click is handled - so they may read
 * that player's own components, but must not write to the Store.</p>
 */
public final class CultivationTitle {

    /**
     * Decides whether a player has actually EARNED a title - see the class doc
     * for why this needs more than a {@code Predicate<PlayerRef>}.
     */
    @FunctionalInterface
    public interface UnlockCheck {
        boolean test(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef);
    }

    private final String key;
    private final Supplier<Message> name;
    private final String sectionKey;
    private final String permission;
    private final Predicate<PlayerRef> visible;
    private final UnlockCheck unlocked;
    private final Supplier<Message> hint;

    private CultivationTitle(Builder builder) {
        this.key = builder.key;
        this.name = builder.name;
        this.sectionKey = builder.sectionKey;
        this.permission = builder.permission;
        this.visible = builder.visible;
        this.unlocked = builder.unlocked;
        this.hint = builder.hint;
    }

    @Nonnull
    public static Builder builder(@Nonnull String key) {
        return new Builder(key);
    }

    /** The id this title is registered under, and what is saved on the player. */
    @Nonnull
    public String getKey() {
        return this.key;
    }

    /** The title's text, resolved fresh per draw so a {@link CultivationTheme} still applies. */
    @Nonnull
    public Message getName() {
        return this.name.get();
    }

    /** Translation key of the caption this title groups under on the picker, or null for ungrouped. */
    @Nullable
    public String getSectionKey() {
        return this.sectionKey;
    }

    /**
     * Whether this title appears on the picker at all. False hides it
     * outright - an operator/feature gate, not a progress gate; see
     * {@link #isUnlockedFor} for whether the wearer has actually earned it.
     */
    public boolean isVisibleTo(@Nonnull PlayerRef playerRef) {
        if (this.permission != null && !playerRef.hasPermission(this.permission)) {
            return false;
        }

        return this.visible == null || this.visible.test(playerRef);
    }

    /**
     * Whether {@code playerRef} has earned this title. True with no
     * {@link Builder#unlocked} condition set at all - a title need not be an
     * achievement, just something a mod wants everyone who can see it to be
     * able to wear.
     */
    public boolean isUnlockedFor(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef) {
        return this.unlocked == null || this.unlocked.test(store, ref, playerRef);
    }

    /**
     * What the picker shows a player who hasn't earned this title yet - e.g.
     * "Requires reaching Golden Core Formation or beyond." Null for a title
     * with no {@link Builder#unlocked} condition (always earned, so never
     * shown) or one that simply didn't set a hint.
     */
    @Nullable
    public Message getHint() {
        return this.hint == null ? null : this.hint.get();
    }

    public static final class Builder {
        private final String key;
        private Supplier<Message> name;
        private String sectionKey;
        private String permission;
        private Predicate<PlayerRef> visible;
        private UnlockCheck unlocked;
        private Supplier<Message> hint;

        private Builder(@Nonnull String key) {
            this.key = key;
            this.name = () -> Message.raw(key);
        }

        /** Sets the title's text from a translation key - the only form that localises. */
        @Nonnull
        public Builder name(@Nonnull String translationKey) {
            this.name = () -> Text.of(translationKey);
            return this;
        }

        /**
         * Sets the title's text from a Message computed per draw - for a title
         * whose name itself carries a parameter (e.g. the built-in Dao Element
         * titles, one "{element} Dao" key shared across ten elements) rather
         * than a plain translation key.
         */
        @Nonnull
        public Builder name(@Nonnull Supplier<Message> name) {
            this.name = name;
            return this;
        }

        /** Groups this title under a captioned section in the picker. */
        @Nonnull
        public Builder section(@Nonnull String sectionKey) {
            this.sectionKey = sectionKey;
            return this;
        }

        /** Hides this title from players without this permission. */
        @Nonnull
        public Builder permission(@Nonnull String permission) {
            this.permission = permission;
            return this;
        }

        /** Hides this title unless this returns true. Combines with {@link #permission}; both must pass. */
        @Nonnull
        public Builder visible(@Nonnull Predicate<PlayerRef> visible) {
            this.visible = visible;
            return this;
        }

        /**
         * What decides whether a player has earned this title. Leaving this
         * unset makes it available to everyone who can see it at all (see
         * {@link #isVisibleTo}).
         */
        @Nonnull
        public Builder unlocked(@Nonnull UnlockCheck unlocked) {
            this.unlocked = unlocked;
            return this;
        }

        /** Sets the locked-tile hint from a translation key. See {@link #getHint}. */
        @Nonnull
        public Builder hint(@Nonnull String translationKey) {
            this.hint = () -> Text.of(translationKey);
            return this;
        }

        /** As {@link #hint(String)}, for a hint that itself carries a parameter. */
        @Nonnull
        public Builder hint(@Nonnull Supplier<Message> hint) {
            this.hint = hint;
            return this;
        }

        @Nonnull
        public CultivationTitle build() {
            if (this.key == null || this.key.isEmpty()) {
                throw new IllegalStateException("A Cultivation title needs an id.");
            }

            return new CultivationTitle(this);
        }
    }
}
