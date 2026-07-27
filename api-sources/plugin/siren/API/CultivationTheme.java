package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Re-words this mod's entire player-facing vocabulary without touching its
 * behaviour - so a setting that has soul masters and academies rather than
 * cultivators and sects reads as its own game instead of a re-skin.
 *
 * <p>Install one from your plugin's {@code setup()} with
 * {@link CultivationAPI#setTheme}. Every string Cultivation shows a player -
 * chat, commands, HUD, menus, item names, tooltips - is routed through
 * {@link #translate} first, and whatever you return is used verbatim.</p>
 *
 * <h2>Why a hook rather than a language file</h2>
 *
 * <p>Because the engine will not let you do it with one. Language files from
 * every asset pack are merged into a single catalog by {@code I18nModule}, and
 * that merge is <b>first-writer-wins</b>: a duplicate key from a later pack is
 * discarded and logged as {@code "'x' has multiple definitions"}. Cultivation's
 * own pack loads before any addon that depends on it, so an addon shipping
 * {@code server.cultivation.playerMsg.breakthrough} in its own
 * {@code server.lang} would be silently ignored, and even winning the race would
 * only mean winning it on that particular boot.</p>
 *
 * <p>So a theme keeps its strings under <b>its own keys</b> - which collide with
 * nothing - and maps Cultivation's keys onto them here.</p>
 *
 * <h2>Contract</h2>
 *
 * <p>{@link #translate} is called on whatever thread is building the message,
 * and often several times per rendered line, so it must be fast, thread-safe,
 * and free of side effects - a lookup in an immutable map is the intended
 * shape. Return {@code null} for any key you do not re-word and Cultivation's
 * own wording is used, so a theme may cover as much or as little as it likes.</p>
 *
 * <p>Return a {@code Message.translation(...)} of your own key rather than
 * {@code Message.raw(...)} wherever you can: raw text is the same in every
 * language, and Cultivation is translated.</p>
 */
public interface CultivationTheme {

    /**
     * A stable, unique id for this theme, namespaced with your mod's name (e.g.
     * {@code "SoulRings:soul_land"}). Only used for logging and for
     * {@code /cultivation admin} to report which theme is live.
     */
    @Nonnull
    String getId();

    /**
     * Re-words one of Cultivation's translation keys.
     *
     * @param key the full key Cultivation was about to show, always including
     *            its {@code server.} prefix (e.g.
     *            {@code "server.cultivation.playerMsg.breakthrough"}).
     * @return the message to show instead, or {@code null} to leave this one
     * alone. Any {@code .param(...)} placeholders Cultivation fills in are
     * applied to whatever you return, so a replacement must keep the same
     * placeholder names - drop one and it simply renders empty.
     */
    @Nullable
    Message translate(@Nonnull String key);
}
