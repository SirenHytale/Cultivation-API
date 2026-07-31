package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.siren.ECS.Components.CultivationProfilesComponent.Profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Events for cultivation profiles - the separate saves a player keeps of their
 * own progress.
 *
 * <h2>Why an addon would care</h2>
 *
 * <p>A profile switch replaces the components that describe <i>what a cultivator
 * has become</i>: realm, race, dao, skill tree, techniques. Anything an addon
 * keeps alongside those - its own levels, its own unlocks, a cached view of the
 * player's rank - is not swapped by Cultivation and will be left describing the
 * cultivator who just left.</p>
 *
 * <p>{@link PreProfileSwitchEvent} is where to save that state, and
 * {@link ProfileSwitchEvent} is where to load it back. Between them an addon can
 * make its own progression profile-aware without Cultivation knowing anything
 * about it.</p>
 *
 * <h2>The sandbox slot</h2>
 *
 * <p>A temp ("test") profile is a permission-gated sandbox whose realm is set by
 * hand rather than earned. {@link Profile#isTest()} tells the two apart, and it
 * is worth checking: an addon that mirrors Cultivation's realm into a ranking of
 * its own almost certainly wants to skip that slot, exactly as Cultivation keeps
 * it off its own leaderboard.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every listener here is called on the world thread that owns the player, and
 * outside any ticking system - so it may read <i>and write</i> that player's
 * components directly through the Store. That is a stronger guarantee than most
 * of this API gives, and it exists because a profile switch is itself a
 * component rewrite; an addon that could only queue its own save would always be
 * one tick late.</p>
 */
public final class ProfileEvents {

    private ProfileEvents(){}

    // --- Post events ----------------------------------------------------------

    /**
     * A player is now on a different profile. Their components have already been
     * replaced, so anything read here describes the cultivator they switched TO.
     *
     * @param from the profile they left, or null when they were placed on one
     *             without leaving another (a first-time backfill)
     */
    public record ProfileSwitchEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nullable Profile from, @Nonnull Profile to) {}

    /** A new, empty profile was created and switched to. */
    public record ProfileCreateEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nonnull Profile profile) {}

    /** A profile was erased. It is already off the player's list. */
    public record ProfileDeleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nonnull Profile profile) {}

    /**
     * A temp profile's time ran out and it was removed.
     *
     * @param wasActive whether the player was playing it, and so has just been
     *                  put back on a real one
     */
    public record ProfileExpireEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nonnull Profile profile, boolean wasActive) {}

    // --- Pre events -----------------------------------------------------------

    /**
     * A profile is about to be swapped in. Cancel to refuse the switch.
     *
     * <p>Fired BEFORE the outgoing profile is saved, so this is the point at
     * which an addon's own state still belongs to the cultivator being left -
     * save it here, keyed by {@link #from()}, and restore it in
     * {@link ProfileSwitchEvent}.</p>
     */
    public static final class PreProfileSwitchEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final Profile from;
        private final Profile to;

        public PreProfileSwitchEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nullable Profile from, @Nonnull Profile to){
            this.ref = ref;
            this.player = player;
            this.from = from;
            this.to = to;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nullable public Profile from(){ return this.from; }
        @Nonnull public Profile to(){ return this.to; }
    }

    /** A new profile is about to be created. Cancel to refuse it. */
    public static final class PreProfileCreateEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;

        public PreProfileCreateEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player){
            this.ref = ref;
            this.player = player;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
    }

    /** A profile is about to be erased. Cancel to keep it. */
    public static final class PreProfileDeleteEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final Profile profile;

        public PreProfileDeleteEvent(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player,
                                     @Nonnull Profile profile){
            this.ref = ref;
            this.player = player;
            this.profile = profile;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nonnull public PlayerRef player(){ return this.player; }
        @Nonnull public Profile profile(){ return this.profile; }
    }

    // --- Listener lists -------------------------------------------------------

    private static final List<Consumer<ProfileSwitchEvent>> SWITCH = EventBus.newListenerList();
    private static final List<Consumer<PreProfileSwitchEvent>> PRE_SWITCH = EventBus.newListenerList();
    private static final List<Consumer<ProfileCreateEvent>> CREATE = EventBus.newListenerList();
    private static final List<Consumer<PreProfileCreateEvent>> PRE_CREATE = EventBus.newListenerList();
    private static final List<Consumer<ProfileDeleteEvent>> DELETE = EventBus.newListenerList();
    private static final List<Consumer<PreProfileDeleteEvent>> PRE_DELETE = EventBus.newListenerList();
    private static final List<Consumer<ProfileExpireEvent>> EXPIRE = EventBus.newListenerList();

    public static void onProfileSwitch(@Nonnull Consumer<ProfileSwitchEvent> listener){ SWITCH.add(listener); }
    public static void onPreProfileSwitch(@Nonnull Consumer<PreProfileSwitchEvent> listener){ PRE_SWITCH.add(listener); }
    public static void onProfileCreate(@Nonnull Consumer<ProfileCreateEvent> listener){ CREATE.add(listener); }
    public static void onPreProfileCreate(@Nonnull Consumer<PreProfileCreateEvent> listener){ PRE_CREATE.add(listener); }
    public static void onProfileDelete(@Nonnull Consumer<ProfileDeleteEvent> listener){ DELETE.add(listener); }
    public static void onPreProfileDelete(@Nonnull Consumer<PreProfileDeleteEvent> listener){ PRE_DELETE.add(listener); }
    public static void onProfileExpire(@Nonnull Consumer<ProfileExpireEvent> listener){ EXPIRE.add(listener); }

    // --- Fired by ProfileManager ----------------------------------------------

    public static void fireProfileSwitch(@Nonnull ProfileSwitchEvent event){ EventBus.dispatch(SWITCH, event, "ProfileSwitchEvent"); }
    public static boolean firePreProfileSwitch(@Nonnull PreProfileSwitchEvent event){ return EventBus.fire(PRE_SWITCH, event, "PreProfileSwitchEvent"); }
    public static void fireProfileCreate(@Nonnull ProfileCreateEvent event){ EventBus.dispatch(CREATE, event, "ProfileCreateEvent"); }
    public static boolean firePreProfileCreate(@Nonnull PreProfileCreateEvent event){ return EventBus.fire(PRE_CREATE, event, "PreProfileCreateEvent"); }
    public static void fireProfileDelete(@Nonnull ProfileDeleteEvent event){ EventBus.dispatch(DELETE, event, "ProfileDeleteEvent"); }
    public static boolean firePreProfileDelete(@Nonnull PreProfileDeleteEvent event){ return EventBus.fire(PRE_DELETE, event, "PreProfileDeleteEvent"); }
    public static void fireProfileExpire(@Nonnull ProfileExpireEvent event){ EventBus.dispatch(EXPIRE, event, "ProfileExpireEvent"); }
}
