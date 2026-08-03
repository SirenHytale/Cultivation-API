package plugin.siren.API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One addon's registration in the compatibility check: which Cultivation
 * versions its installed version is known to work against.
 *
 * <p>Register with {@link CultivationAPI#registerCompatCheck}. The published
 * matrix is a single static JSON object; Jade Slip's lives at
 * {@code https://xianxia.dev/api/get/compat/JadeSlip.json}:</p>
 *
 * <pre>{@code {
 *   "compatible": {
 *     "0.1.2": {
 *       "min": "0.7.4",
 *       "below": "0.8.0",
 *       "blocked": []
 *     }
 *   }
 * }}</pre>
 *
 * <p>Keyed by <em>your</em> version. {@code min} is inclusive, {@code below}
 * exclusive, and {@code blocked} names individual Cultivation versions inside
 * that band which turned out not to work after all. Every field is optional:
 * a range with only {@code min} has no ceiling, and one with neither bound is a
 * way of saying "any Cultivation, except the blocked ones".</p>
 *
 * <h2>Why this exists when the manifest already gates loading</h2>
 *
 * <p>A manifest's {@code Dependencies} range is the real gate - the engine
 * refuses to load an addon whose declared range excludes the installed
 * Cultivation, before a line of the addon runs, with no network involved. That
 * is the mechanism to rely on, and every addon in this family declares one.</p>
 *
 * <p>What a manifest cannot do is change its mind. It is baked into a shipped
 * jar, so it can only encode what was known on release day. When Cultivation
 * 0.7.6 turns out to break an addon that honestly declared {@code <0.8.0}
 * months earlier, the manifest happily loads the pairing and something subtly
 * breaks. This matrix is the part that can be edited afterwards - one line in a
 * JSON file, no jar, no release - so a bad pairing can be called out on servers
 * that already have both installed.</p>
 *
 * <h2>Failing open</h2>
 *
 * <p>{@link CompatStatus#INCOMPATIBLE} is only ever reached by a positive
 * statement about this exact pairing. An unreachable host, a timeout, a
 * malformed body, or an addon version absent from the matrix all leave the
 * verdict {@link CompatStatus#UNKNOWN}. A server with no outbound network is
 * therefore never told its mods do not work together, which is what keeps this
 * safe to act on.</p>
 */
public interface CompatCheck {

    /** A stable id, unique across registered mods - your plugin's name. */
    @Nonnull
    String getModId();

    /** How this mod is named in the log line. Plain text, not a language key. */
    @Nonnull
    String getDisplayName();

    /** The installed addon version, which is the key looked up in the published matrix. */
    @Nonnull
    String getVersion();

    /** The URL the matrix is fetched from. */
    @Nonnull
    String getManifestUrl();

    /** The Cultivation version this was judged against - the one actually running. */
    @Nonnull
    String getCultivationVersion();

    /**
     * The range the matrix named for this addon version, as text fit for a log
     * line (e.g. {@code ">=0.7.4 <0.8.0"}), or null when the matrix named none.
     */
    @Nullable
    String getRequiredRange();

    /** @return what the check concluded. Never null; {@link CompatStatus#UNKNOWN} until it runs. */
    @Nonnull
    CompatStatus getStatus();

    long getLastCheckedMillis();
}
