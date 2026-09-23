package plugin.siren.API;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.Utils.Sect.Sect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sect Guardian NPC events - stationing a guardian at a sect's hall and how
 * its life ends. See {@link CultivationEvents} for the conventions every
 * {@code *Events} class in this package shares.
 */
public final class GuardianEvents {
    private GuardianEvents(){}

    // --- Post-events ---

    /** A member successfully stationed a new guardian post at their sect's hall - the post is purchased; the live NPC body itself appears on {@code SectGuardianSystem}'s next heartbeat once its chunk is resident. */
    public record SectGuardianStationedEvent(@Nonnull Sect sect, @Nonnull PlayerRef actor, int postIndex) {}

    /**
     * One living guardian died. {@code killerPlayerRef} is null when the
     * killing blow never resolved to a player (environmental damage, a
     * formation trap, or the killer despawned before resolution). {@code
     * consumed} is true when the killer was the owning sect's own member or
     * an ally - that post is gone for good (no respawn, no refund); false
     * means the normal Guardian-Respawn-Seconds timer was armed instead.
     */
    public record SectGuardianFellEvent(@Nonnull Sect sect, int postIndex, @Nullable PlayerRef killerPlayerRef, boolean consumed) {}

    /** The LAST living guardian at this sect's hall just died (or vanished into a permanent consumed state) - no living guardian remains at this instant. Purely informational: no hold-time bonus, no penalty (see {@code SectGuardianManager}'s own class javadoc for why). */
    public record SectGuardiansFallenEvent(@Nonnull Sect sect) {}

    // --- Pre-events ---

    /** A member is about to station a new guardian post. Cancel to refuse it (reported to the actor as the station attempt simply failing) - fired AFTER every gate in {@code SectGuardianManager#station} passes but BEFORE the contribution cost is spent, so a veto never costs the actor anything. */
    public static final class PreSectGuardianStationEvent extends CancellableEvent {
        private final Sect sect;
        private final PlayerRef actor;
        private final int postIndex;

        public PreSectGuardianStationEvent(@Nonnull Sect sect, @Nonnull PlayerRef actor, int postIndex){
            this.sect = sect;
            this.actor = actor;
            this.postIndex = postIndex;
        }

        @Nonnull public Sect sect(){ return this.sect; }
        @Nonnull public PlayerRef actor(){ return this.actor; }
        /** The index this new post would take, 0-based - the sect's post COUNT before this station, not after. */
        public int postIndex(){ return this.postIndex; }
    }

    // --- Listener registration ---

    private static final List<Consumer<SectGuardianStationedEvent>> STATIONED = EventBus.newListenerList();
    private static final List<Consumer<SectGuardianFellEvent>> FELL = EventBus.newListenerList();
    private static final List<Consumer<SectGuardiansFallenEvent>> ALL_FALLEN = EventBus.newListenerList();
    private static final List<Consumer<PreSectGuardianStationEvent>> PRE_STATIONED = EventBus.newListenerList();

    public static void onSectGuardianStationed(@Nonnull Consumer<SectGuardianStationedEvent> listener){ STATIONED.add(listener); }
    public static void onSectGuardianFell(@Nonnull Consumer<SectGuardianFellEvent> listener){ FELL.add(listener); }
    public static void onSectGuardiansFallen(@Nonnull Consumer<SectGuardiansFallenEvent> listener){ ALL_FALLEN.add(listener); }
    public static void onPreSectGuardianStation(@Nonnull Consumer<PreSectGuardianStationEvent> listener){ PRE_STATIONED.add(listener); }

    // --- Internal dispatch (called by this mod's own systems; not API) ---

    public static void fireSectGuardianStationed(@Nonnull SectGuardianStationedEvent event){ EventBus.dispatch(STATIONED, event, "SectGuardianStationedEvent"); }
    public static void fireSectGuardianFell(@Nonnull SectGuardianFellEvent event){ EventBus.dispatch(FELL, event, "SectGuardianFellEvent"); }
    public static void fireSectGuardiansFallen(@Nonnull SectGuardiansFallenEvent event){ EventBus.dispatch(ALL_FALLEN, event, "SectGuardiansFallenEvent"); }
    public static boolean firePreSectGuardianStation(@Nonnull PreSectGuardianStationEvent event){ return EventBus.fire(PRE_STATIONED, event, "PreSectGuardianStationEvent"); }
}
