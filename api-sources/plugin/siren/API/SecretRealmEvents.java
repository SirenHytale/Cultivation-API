package plugin.siren.API;

import plugin.siren.Utils.Realm.SecretRealmSite;
import plugin.siren.Utils.Realm.SecretRealmTier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Secret Realm (秘境) events - a site's barrier coming down (openable) or
 * going back up (closed). See {@link CultivationEvents} for the conventions
 * every {@code *Events} class in this package shares (pre vs post,
 * cancellation, threading, registration).
 *
 * <p>Post-only, deliberately: opening or closing a site is a deterministic
 * outcome of the scheduler (or an admin's own override) rather than a request
 * anything downstream could meaningfully veto, so there is no Pre/cancellable
 * pair here the way {@link TideEvents} or {@link CelestialEvents} have one.</p>
 */
public final class SecretRealmEvents {
    private SecretRealmEvents(){}

    // --- Post-events ---

    /** A Secret Realm site opened - the barrier is already down and it can be entered. */
    public record SecretRealmOpenEvent(@Nonnull String siteId, @Nonnull SecretRealmTier tier, @Nonnull String world,
                                       int chunkX, int chunkZ, @Nonnull SecretRealmSite.Source source,
                                       @Nonnull String sectName, long closesAtMillis) {}

    /** A Secret Realm site closed. {@code forced} is true only for an admin's immediate override (SecretRealmManager.forceClose); false for the realm's own natural close after its duration/grace window. */
    public record SecretRealmCloseEvent(@Nonnull String siteId, @Nonnull SecretRealmTier tier, @Nonnull String world,
                                        int chunkX, int chunkZ, boolean forced) {}

    // --- Listener registration ---

    private static final List<Consumer<SecretRealmOpenEvent>> OPEN = EventBus.newListenerList();
    private static final List<Consumer<SecretRealmCloseEvent>> CLOSE = EventBus.newListenerList();

    public static void onSecretRealmOpen(@Nonnull Consumer<SecretRealmOpenEvent> listener){ OPEN.add(listener); }
    public static void onSecretRealmClose(@Nonnull Consumer<SecretRealmCloseEvent> listener){ CLOSE.add(listener); }

    // --- Internal dispatch (called by SecretRealmManager; not API) ---

    public static void fireSecretRealmOpen(@Nonnull SecretRealmOpenEvent event){ EventBus.dispatch(OPEN, event, "SecretRealmOpenEvent"); }
    public static void fireSecretRealmClose(@Nonnull SecretRealmCloseEvent event){ EventBus.dispatch(CLOSE, event, "SecretRealmCloseEvent"); }
}
