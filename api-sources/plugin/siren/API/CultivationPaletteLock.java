package plugin.siren.API;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A rule that can force a specific {@link CultivationPalette} on some
 * players, overriding their own theme choice entirely while it applies - an
 * off-ring Dao root claiming its own robes, say.
 *
 * <p>Register one with {@link CultivationAPI#registerPaletteLock}. More than
 * one may be registered; the first whose {@link #lockedPaletteKey} returns
 * non-null for a given player wins, in registration order.</p>
 *
 * <p>Evaluated on the world thread that owns the viewing player, so it may
 * read that player's components but must not write to the Store.</p>
 */
public interface CultivationPaletteLock {

    /**
     * @return the {@link CultivationPalette#getKey() key} of the palette this
     * lock forces on this player right now, or null if it does not apply to
     * them. A key naming a palette nobody has registered is treated as "does
     * not apply" by every caller - the same "fall through to normal behavior"
     * contract an unclaimed palette id has everywhere else in this API.
     */
    @Nullable
    String lockedPaletteKey(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);

    /**
     * @return the translation key of the hint shown in place of the theme
     * dropdown's usual hint while this lock applies, or null to show none.
     */
    @Nullable
    String reasonKey();
}
