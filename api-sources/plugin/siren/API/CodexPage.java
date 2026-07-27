package plugin.siren.API;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The body of one Codex entry, written a block at a time.
 *
 * <p>An entry describes itself rather than drawing itself: it calls
 * {@link #heading}, {@link #paragraph}, {@link #stat} and friends, and the Codex
 * page renders whatever comes out. That keeps entries free of any UI knowledge,
 * which is what lets another mod contribute one - and lets the Codex's own look
 * change without touching a single entry.</p>
 *
 * <p>A body is written fresh every time somebody opens the entry, and this page
 * carries the reader with it ({@link #getAccessor}, {@link #getRef},
 * {@link #getPlayerRef}), so an entry can - and should - report the server's
 * real configured numbers and the reader's own progress rather than prose that
 * quietly goes stale:</p>
 *
 * <pre>{@code page.heading("server.myMod.codex.cost.heading")
 *     .paragraph("server.myMod.codex.cost.body")
 *     .stat("server.myMod.codex.cost.perUse", config.getCost())
 *     .noteIf(playerHasNotUnlockedIt, "server.myMod.codex.cost.locked");}</pre>
 *
 * <h2>Threading</h2>
 *
 * <p>Written on the reader's world thread while their page is being built. Read
 * their components freely; do not write to the Store.</p>
 */
public final class CodexPage {

    /** What a block is. The Codex page owns how each one actually looks. */
    public enum BlockType {
        /** A section heading inside the entry. */
        HEADING,
        /** A paragraph of body text. Wraps. */
        PARAGRAPH,
        /** A label/value row, for a configured number. */
        STAT,
        /** A dimmed aside - a caveat, or something true only for this reader. */
        NOTE,
        /** A crafting recipe, resolved live from the item's own asset. */
        RECIPE,
        /** A thin rule. */
        DIVIDER
    }

    /**
     * One rendered element. {@code primary} is the heading/paragraph/note text or
     * a stat's label; {@code secondary} is a stat's value; {@code data} is a
     * recipe's output item id.
     */
    public record Block(@Nonnull BlockType type, @Nullable Message primary, @Nullable Message secondary, @Nullable String data) {}

    private final ComponentAccessor<EntityStore> accessor;
    private final Ref<EntityStore> ref;
    private final PlayerRef playerRef;
    private final List<Block> blocks = new ArrayList<>();

    public CodexPage(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                     @Nonnull PlayerRef playerRef){
        this.accessor = accessor;
        this.ref = ref;
        this.playerRef = playerRef;
    }

    // --- The reader ---

    @Nonnull
    public ComponentAccessor<EntityStore> getAccessor(){
        return this.accessor;
    }

    @Nonnull
    public Ref<EntityStore> getRef(){
        return this.ref;
    }

    @Nonnull
    public PlayerRef getPlayerRef(){
        return this.playerRef;
    }

    // --- Writing ---

    @Nonnull
    public CodexPage heading(@Nonnull Message text){
        return add(BlockType.HEADING, text, null, null);
    }

    /** @param translationKey a full key including its {@code server.} prefix. */
    @Nonnull
    public CodexPage heading(@Nonnull String translationKey){
        return heading(Text.of(translationKey));
    }

    @Nonnull
    public CodexPage paragraph(@Nonnull Message text){
        return add(BlockType.PARAGRAPH, text, null, null);
    }

    @Nonnull
    public CodexPage paragraph(@Nonnull String translationKey){
        return paragraph(Text.of(translationKey));
    }

    @Nonnull
    public CodexPage note(@Nonnull Message text){
        return add(BlockType.NOTE, text, null, null);
    }

    @Nonnull
    public CodexPage note(@Nonnull String translationKey){
        return note(Text.of(translationKey));
    }

    /** Adds the note only when {@code condition} holds - for a line that is true of this reader only. */
    @Nonnull
    public CodexPage noteIf(boolean condition, @Nonnull Message text){
        return condition ? note(text) : this;
    }

    @Nonnull
    public CodexPage noteIf(boolean condition, @Nonnull String translationKey){
        return condition ? note(translationKey) : this;
    }

    /** A label and the number or word it resolves to on THIS server. */
    @Nonnull
    public CodexPage stat(@Nonnull Message label, @Nonnull Message value){
        return add(BlockType.STAT, label, value, null);
    }

    @Nonnull
    public CodexPage stat(@Nonnull String labelKey, @Nonnull Message value){
        return stat(Text.of(labelKey), value);
    }

    /** Values that are already text (a range, a formatted number) go through here unchanged. */
    @Nonnull
    public CodexPage stat(@Nonnull String labelKey, @Nonnull String value){
        return stat(Text.of(labelKey), Message.raw(value));
    }

    @Nonnull
    public CodexPage stat(@Nonnull String labelKey, int value){
        return stat(labelKey, String.valueOf(value));
    }

    /** Trims a float to as few decimals as it actually needs, so 1.50 reads as "1.5" and 2.00 as "2". */
    @Nonnull
    public CodexPage stat(@Nonnull String labelKey, float value){
        return stat(labelKey, formatNumber(value));
    }

    /**
     * A crafting recipe, looked up live from the item's own asset when the page
     * is drawn - so an edited recipe is right in the Codex without anything here
     * changing. An item with no recipe renders nothing at all.
     *
     * @param itemId the id of the item that gets CRAFTED, not an ingredient.
     */
    @Nonnull
    public CodexPage recipe(@Nonnull String itemId){
        return add(BlockType.RECIPE, null, null, itemId);
    }

    @Nonnull
    public CodexPage divider(){
        return add(BlockType.DIVIDER, null, null, null);
    }

    @Nonnull
    private CodexPage add(@Nonnull BlockType type, @Nullable Message primary, @Nullable Message secondary, @Nullable String data){
        this.blocks.add(new Block(type, primary, secondary, data));
        return this;
    }

    /** Everything written to this page, in order. */
    @Nonnull
    public List<Block> getBlocks(){
        return this.blocks;
    }

    /**
     * Formats a configured number the way the Codex shows them: no trailing
     * zeroes, since a config full of 1.0 and 2.50 reads as noise beside prose.
     */
    @Nonnull
    public static String formatNumber(float value){
        if(value == Math.rint(value) && !Float.isInfinite(value)){
            return String.valueOf((long) value);
        }

        return String.valueOf(Math.round(value * 100f) / 100f);
    }

    /** As {@link #formatNumber}, with a percent sign - for the many 0-1 multipliers in this mod's configs. */
    @Nonnull
    public static String formatPercent(float fraction){
        return formatNumber(fraction * 100f) + "%";
    }
}
