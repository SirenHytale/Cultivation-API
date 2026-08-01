package plugin.siren.API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One mod's registration with Cultivation's update checker - the mod's own
 * version, the manifest URL that says what the current one is, and whatever the
 * last fetch of that manifest concluded.
 *
 * <p>Register from your plugin's {@code setup()} with
 * {@link CultivationAPI#registerUpdateCheck}; you get one of these back, and
 * Cultivation does the rest. There is one HTTP fetch per registered mod, on one
 * shared background thread, on the interval the server owner sets - and one
 * combined message to administrators as they join, naming every mod that is
 * behind rather than one message per mod.</p>
 *
 * <h2>The manifest</h2>
 *
 * <p>A single JSON object, served static. Cultivation's own lives at
 * {@code https://xianxia.dev/api/get/version/cultivation.json}:</p>
 *
 * <pre>{@code {
 *   "release": "0.7.1",
 *   "ignore": ["0.7.0"]
 * }}</pre>
 *
 * <p>{@code release} is the version you consider current. {@code ignore} lists
 * installed versions that are never notified even though they are behind it -
 * a beta you would rather leave alone, or a build whose upgrade path is not
 * ready. Both are read leniently: a missing field, a malformed body, a dead
 * host or a timeout all mean "no update", never an error anyone sees.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every getter here is safe to call from any thread. The three that carry a
 * fetch result ({@link #getLatestVersion}, {@link #isUpdateAvailable},
 * {@link #isSuppressed}) are written by the checker's background thread and read
 * wherever you ask, so they answer for the most recent completed fetch - and
 * answer as though nothing were available until the first one lands.</p>
 */
public interface UpdateCheck {

    /**
     * A stable id, unique across every registered mod, used to unregister and to
     * keep two mods' results apart. Your plugin's name is the obvious choice
     * ({@code "SoulRings"}).
     */
    @Nonnull
    String getModId();

    /**
     * How this mod is named in the message to administrators - written out
     * rather than derived from {@link #getModId()}, so "Cultivation: Jade Slip"
     * need not be spelled {@code CultivationJadeSlip} to a reader.
     *
     * <p>Plain text, not a language key: these are mod names, and a mod name
     * does not translate.</p>
     */
    @Nonnull
    String getDisplayName();

    /**
     * The version actually installed - normally
     * {@code getManifest().getVersion().toString()}, so it can never drift from
     * your manifest.json.
     */
    @Nonnull
    String getCurrentVersion();

    /** The URL the manifest above is fetched from. */
    @Nonnull
    String getManifestUrl();

    /**
     * Where a server owner should go to get the new version - a project page,
     * not a file. Deliberately part of the registration rather than of the
     * manifest: the manifest answers "what is current", and a mod already knows
     * where it lives.
     *
     * @return that page, or null if this mod would rather name no destination,
     * in which case the message simply states the versions.
     */
    @Nullable
    String getDownloadUrl();

    /**
     * @return the {@code release} the last successful fetch reported, or null if
     * no fetch has succeeded yet. Not in itself a statement that an update
     * exists - see {@link #isUpdateAvailable()}.
     */
    @Nullable
    String getLatestVersion();

    /**
     * @return whether the installed version is behind the published one AND is
     * not being suppressed. This is the flag the join message is built from;
     * everything else here is detail behind it.
     */
    boolean isUpdateAvailable();

    /**
     * @return whether an update exists but the installed version sits in the
     * manifest's {@code ignore} list, so nobody is being told about it. False
     * whenever there is no update to suppress in the first place.
     */
    boolean isSuppressed();

    /**
     * @return when the last fetch - successful or not - finished, as
     * {@code System.currentTimeMillis()}, or 0 if none has.
     */
    long getLastCheckedMillis();
}
