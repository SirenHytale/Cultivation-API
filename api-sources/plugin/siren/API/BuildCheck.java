package plugin.siren.API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One mod's registration with Cultivation's build check - the jar to hash, the
 * URL listing the digests published as official, and what the comparison
 * concluded.
 *
 * <p>Register from your plugin's {@code setup()} with
 * {@link CultivationAPI#registerBuildCheck}. It rides the same background
 * thread and the same {@code Update-Check-Enabled} switch as the update check.</p>
 *
 * <h2>What this is worth</h2>
 *
 * <p>It detects a jar whose code is not the code that was shipped: a
 * repackaged upload, a redistributed paid addon, a damaged download, and yes, a
 * decompile-and-recompile. It does <strong>not</strong> defend against anybody
 * willing to edit the mod, because the same edit that rebuilds the code can
 * delete this check. Nothing running on someone else's machine can do better
 * than that, so treat {@link BuildStatus#UNOFFICIAL} as a signal worth having
 * and never as a lock.</p>
 *
 * <h2>The manifest</h2>
 *
 * <pre>{@code {
 *   "builds": {
 *     "0.7.1": ["a3f2...9c"],
 *     "0.7.0": ["71bd...04", "5e90...11"]
 *   }
 * }}</pre>
 *
 * <p>An array per version, so a version you rebuilt and still consider official
 * can carry more than one digest. A version absent from the map leaves that
 * build {@link BuildStatus#UNKNOWN} rather than condemning it - which is what
 * makes forgetting to publish a digest harmless.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every getter is safe to call from any thread and answers for the most
 * recent completed check.</p>
 */
public interface BuildCheck {

    /** A stable id, unique across registered mods - your plugin's name. */
    @Nonnull
    String getModId();

    /** How this mod is named in the log line. Plain text, not a language key. */
    @Nonnull
    String getDisplayName();

    /** The installed version, which is the key looked up in the published map. */
    @Nonnull
    String getVersion();

    /** The URL the digest map is fetched from. */
    @Nonnull
    String getManifestUrl();

    /**
     * @return this jar's computed digest, or null if it could not be read at
     * all - an unreadable jar is a broken installation, not an unofficial one,
     * and leaves the status {@link BuildStatus#UNKNOWN}.
     */
    @Nullable
    String getLocalDigest();

    /** @return what the check concluded. Never null; {@link BuildStatus#UNKNOWN} until it runs. */
    @Nonnull
    BuildStatus getStatus();

    /**
     * @return when the last check finished, as {@code System.currentTimeMillis()},
     * or 0 if none has.
     */
    long getLastCheckedMillis();
}
