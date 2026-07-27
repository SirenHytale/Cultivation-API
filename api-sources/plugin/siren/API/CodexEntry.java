package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One article in the in-game Cultivation Codex.
 *
 * <p>Build one with {@link #builder(String)} and hand it to
 * {@link CultivationAPI#registerCodexEntry}:</p>
 *
 * <pre>{@code CultivationAPI.registerCodexEntry(
 *         CodexEntry.builder("myMod:soulRings")
 *                 .title("server.myMod.codex.soulRings.title")
 *                 .summary("server.myMod.codex.soulRings.summary")
 *                 .category(CodexCategory.SELF)
 *                 .body(page -> page
 *                         .paragraph("server.myMod.codex.soulRings.body")
 *                         .stat("server.myMod.codex.soulRings.maxRings", config.getMaxRings())));}</pre>
 *
 * <p>The body is written fresh for each reader (see {@link CodexPage}), which is
 * the whole point of an in-game codex over a wiki page: it can state the numbers
 * THIS server is actually running and what the reader has actually reached,
 * neither of which a wiki can know.</p>
 *
 * <h2>Ids and order</h2>
 *
 * <p>Namespace the id with your mod name; registering the same id twice replaces
 * the first, which is how a mod rewrites a built-in article rather than adding
 * beside it. Entries sort by {@link #getSortOrder()} within their category, and
 * the built-ins are spaced 100 apart so an entry can be slotted between two of
 * them.</p>
 */
public final class CodexEntry {

    /** Where an entry with no declared order goes: after the built-ins of its category. */
    public static final int SORT_DEFAULT = 10000;

    /** Writes an entry's body. See {@link CodexPage}. */
    @FunctionalInterface
    public interface Body {
        void write(@Nonnull CodexPage page);
    }

    private final String id;
    private final Supplier<Message> title;
    private final Supplier<Message> summary;
    private final String category;
    private final int sortOrder;
    private final Predicate<PlayerRef> visible;
    private final Body body;

    private CodexEntry(Builder builder){
        this.id = builder.id;
        this.title = builder.title;
        this.summary = builder.summary;
        this.category = builder.category;
        this.sortOrder = builder.sortOrder;
        this.visible = builder.visible;
        this.body = builder.body;
    }

    @Nonnull
    public static Builder builder(@Nonnull String id){
        return new Builder(id);
    }

    @Nonnull
    public String getId(){
        return this.id;
    }

    /** The article's name, in the index and at the top of its page. Resolved per draw. */
    @Nonnull
    public Message getTitle(){
        return this.title.get();
    }

    /** One line under the title saying what the article covers. Resolved per draw. */
    @Nonnull
    public Message getSummary(){
        return this.summary.get();
    }

    /** The id of the {@link CodexCategory} this article files under. */
    @Nonnull
    public String getCategory(){
        return this.category;
    }

    public int getSortOrder(){
        return this.sortOrder;
    }

    /**
     * Whether this reader sees the article at all. Default is everyone: a codex
     * that hides what you have not unlocked cannot tell you how to unlock it, so
     * prefer saying so in the body over hiding the article.
     */
    public boolean isVisibleTo(@Nonnull PlayerRef playerRef){
        return this.visible == null || this.visible.test(playerRef);
    }

    public void writeBody(@Nonnull CodexPage page){
        this.body.write(page);
    }

    public static final class Builder {
        private final String id;
        private Supplier<Message> title;
        private Supplier<Message> summary;
        private String category = CodexCategory.PATH;
        private int sortOrder = SORT_DEFAULT;
        private Predicate<PlayerRef> visible;
        private Body body;

        private Builder(@Nonnull String id){
            this.id = id;
            this.title = () -> Message.raw(id);
            this.summary = () -> Message.raw("");
        }

        @Nonnull
        public Builder title(@Nonnull String translationKey){
            this.title = () -> Text.of(translationKey);
            return this;
        }

        @Nonnull
        public Builder title(@Nonnull Supplier<Message> title){
            this.title = title;
            return this;
        }

        @Nonnull
        public Builder summary(@Nonnull String translationKey){
            this.summary = () -> Text.of(translationKey);
            return this;
        }

        @Nonnull
        public Builder summary(@Nonnull Supplier<Message> summary){
            this.summary = summary;
            return this;
        }

        /** @param category a {@link CodexCategory} id - one of the four built-ins, or your own. */
        @Nonnull
        public Builder category(@Nonnull String category){
            this.category = category;
            return this;
        }

        @Nonnull
        public Builder sortOrder(int sortOrder){
            this.sortOrder = sortOrder;
            return this;
        }

        /** Hides the article from readers this rejects. See {@link CodexEntry#isVisibleTo}. */
        @Nonnull
        public Builder visible(@Nonnull Predicate<PlayerRef> visible){
            this.visible = visible;
            return this;
        }

        /** Required. */
        @Nonnull
        public Builder body(@Nonnull Body body){
            this.body = body;
            return this;
        }

        @Nonnull
        public CodexEntry build(){
            if(this.id == null || this.id.isEmpty()){
                throw new IllegalStateException("A Codex entry needs an id.");
            }

            if(this.body == null){
                throw new IllegalStateException("Codex entry '" + this.id + "' has no body.");
            }

            return new CodexEntry(this);
        }
    }
}
