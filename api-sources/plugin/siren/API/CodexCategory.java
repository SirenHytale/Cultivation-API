package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * A heading in the Codex's index, grouping {@link CodexEntry articles} that
 * belong together.
 *
 * <p>Four ship with the mod, spaced 100 apart so a mod can slot its own group
 * between them. Filing under an existing one is usually better than adding a
 * fifth - a reader looking for spirit beasts wants them under Self, not under
 * the name of the mod that happened to add them.</p>
 */
public final class CodexCategory {

    /** Realms, Qi, breakthroughs, tribulation - the progression itself. */
    public static final String PATH = "path";
    /** What a cultivator becomes: race, dao, techniques, companions. */
    public static final String SELF = "self";
    /** What is out there: veins, sects, abodes, formations, other cultivators. */
    public static final String WORLD = "world";
    /** Making things: alchemy, refinement, manuals. */
    public static final String CRAFT = "craft";

    private final String id;
    private final Supplier<Message> label;
    private final int sortOrder;

    private CodexCategory(@Nonnull String id, @Nonnull Supplier<Message> label, int sortOrder){
        this.id = id;
        this.label = label;
        this.sortOrder = sortOrder;
    }

    @Nonnull
    public static CodexCategory of(@Nonnull String id, @Nonnull String translationKey, int sortOrder){
        return new CodexCategory(id, () -> Text.of(translationKey), sortOrder);
    }

    @Nonnull
    public static CodexCategory of(@Nonnull String id, @Nonnull Supplier<Message> label, int sortOrder){
        return new CodexCategory(id, label, sortOrder);
    }

    @Nonnull
    public String getId(){
        return this.id;
    }

    /** Resolved per draw, so an installed {@link CultivationTheme} can re-word it. */
    @Nonnull
    public Message getLabel(){
        return this.label.get();
    }

    public int getSortOrder(){
        return this.sortOrder;
    }
}
