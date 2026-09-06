package plugin.siren.API;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Says which {@link CultivationPalette} a player should be drawn in when they
 * have not chosen one themselves.
 *
 * <p>Register one with {@link CultivationAPI#registerPaletteDefault}. This is
 * the soft half of the palette overrides: a {@link CultivationPaletteLock}
 * <i>takes</i> a player's choice away and wins over everything, while a default
 * only fills the gap where there is no choice to override.
 * {@link CultivationAPI#getPalette(ComponentAccessor, Ref)} consults it only
 * when the player's stored palette id is absent or empty - the moment they pick
 * any real look, or explicitly pick {@link CultivationPalette#NONE_KEY} to turn
 * a suggested look down, every default stops being asked.</p>
 *
 * <p><b>The default must gate itself.</b>
 * {@link CultivationPalette#isAvailableTo} is <i>not</i> re-checked on the
 * answer. That check is a listing and equip gate by design - it runs when a look
 * is offered in the picker and again when one is applied, never on every later
 * read (see {@code CultivationPalette#isAvailableTo}, and the same note on
 * {@code CultivationAPI#getTitle}) - and this path runs on <i>every draw</i>, so
 * re-running a permission lookup here would put one on each themed document of
 * every page build. Return null for anybody your look is not for; do not lean on
 * the palette's own availability rule to do it for you.</p>
 *
 * <h2>Threading and cost</h2>
 *
 * <p>Called on the world thread that owns the <b>viewing</b> player, from inside
 * a page or HUD build. Read that player's components; never write to the Store,
 * never touch another world, never block. It runs once per palette resolution -
 * at least once per page build and once per HUD refresh - so keep it to a
 * component read and a comparison. Anything more expensive belongs behind a
 * value you cached when it last changed.</p>
 */
@FunctionalInterface
public interface CultivationPaletteDefault {

    /**
     * @return the {@link CultivationPalette#getKey() key} of the palette this
     * player should fall back to, or null for "no opinion" - which hands the
     * question to the next registered default, and to Cultivation's own look if
     * none of them answers. A key naming a palette nobody has registered is
     * treated as "no opinion" too, the same "fall through to normal behavior"
     * contract an unclaimed palette id has everywhere else in this API, so a
     * default whose own palette registers later (or not at all) can never leave
     * a player looking at a document root that does not exist.
     */
    @Nullable
    String defaultPaletteKey(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref);
}
