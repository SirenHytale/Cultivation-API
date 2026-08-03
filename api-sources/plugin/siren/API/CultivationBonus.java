package plugin.siren.API;

import javax.annotation.Nonnull;

/**
 * One line on the Overview page's bonus list, contributed by an addon.
 *
 * <p>Cultivation's own bonuses (realm, race, skill tree) are collected directly
 * by {@code BonusSummary}, which deliberately had no cross-source registry
 * behind it because nothing else needed one. A constitution addon does: what it
 * grants is spread across a dozen systems and would otherwise be entirely
 * invisible - a player would feel slightly tougher and have no way to learn
 * why.</p>
 *
 * @param sourceKey a server.lang key naming what granted this - your mod, or the
 *                  specific thing inside it ("Ancient Sacred Body"). Shown as
 *                  the row's source in the per-source breakdown.
 * @param statKey   a server.lang key naming the stat it moves. Reuse
 *                  Cultivation's own where one fits, so an addon's Qi bonus
 *                  sums with the mod's own rather than printing a second,
 *                  differently-worded Qi line.
 * @param amount    the size of it, in the unit {@code percent} describes.
 * @param percent   true when {@code amount} is a percentage rather than a flat
 *                  addition. Rows disagreeing on this are never summed
 *                  together - a flat +120 Health and a +15% Health stay separate
 *                  lines, because adding them would produce a number that means
 *                  nothing.
 */
public record CultivationBonus(@Nonnull String sourceKey, @Nonnull String statKey, float amount, boolean percent) {
}
