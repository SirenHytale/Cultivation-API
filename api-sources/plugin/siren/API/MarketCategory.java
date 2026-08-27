package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A tab in the Auction House's category filter row (see {@code
 * plugin.siren.Utils.Market.MarketCategories} for the registry, and {@code
 * MarketPage.ui}'s {@code #CategoryRow} for the buttons themselves).
 * Deliberately shaped like {@link CodexCategory} - an id, a resolved-per-draw
 * label and a sort order - plus the one thing a market category needs that a
 * Codex heading does not: a {@link Predicate} deciding whether a given item
 * belongs on this tab.
 */
public final class MarketCategory {

    private final String id;
    private final Supplier<Message> label;
    private final int sortOrder;
    private final Predicate<ItemStack> matcher;

    private MarketCategory(@Nonnull String id, @Nonnull Supplier<Message> label, int sortOrder, @Nonnull Predicate<ItemStack> matcher){
        this.id = id;
        this.label = label;
        this.sortOrder = sortOrder;
        this.matcher = matcher;
    }

    @Nonnull
    public static MarketCategory of(@Nonnull String id, @Nonnull String translationKey, int sortOrder, @Nonnull Predicate<ItemStack> matcher){
        return new MarketCategory(id, () -> Text.of(translationKey), sortOrder, matcher);
    }

    @Nonnull
    public static MarketCategory of(@Nonnull String id, @Nonnull Supplier<Message> label, int sortOrder, @Nonnull Predicate<ItemStack> matcher){
        return new MarketCategory(id, label, sortOrder, matcher);
    }

    @Nonnull
    public String getId(){
        return this.id;
    }

    /** Resolved per draw, same contract as {@link CodexCategory#getLabel()}. */
    @Nonnull
    public Message getLabel(){
        return this.label.get();
    }

    public int getSortOrder(){
        return this.sortOrder;
    }

    public boolean matches(@Nonnull ItemStack stack){
        return this.matcher.test(stack);
    }
}
