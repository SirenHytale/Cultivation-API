package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wandering Rival Cultivator events. See {@link CultivationEvents} for the
 * conventions every {@code *Events} class in this package shares (pre vs
 * post, cancellation, threading, registration).
 *
 * <p>Dispatched by {@code RivalManager}/{@code RivalCultivatorSystem} - this
 * class only declares the surface; nothing here calls into that manager,
 * matching every other standalone {@code *Events} class in this package.</p>
 */
public final class RivalEvents {
    private RivalEvents(){}

    // --- Pre-events ---

    /**
     * A player has right-clicked to challenge a Wandering Rival Cultivator.
     * Cancel to refuse the challenge entirely - the rival stays
     * {@code ROAMING} and the interaction is a no-op, the same "silence is a
     * valid answer" shape {@code DuelEvents.PreDuelChallengeEvent} gives a
     * duel challenge.
     */
    public static final class PreRivalChallengeEvent extends CancellableEvent {
        private final String encounterId;
        private final UUID challengerUuid;
        private final Ref<EntityStore> npcRef;

        public PreRivalChallengeEvent(@Nonnull String encounterId, @Nonnull UUID challengerUuid, @Nonnull Ref<EntityStore> npcRef){
            this.encounterId = encounterId;
            this.challengerUuid = challengerUuid;
            this.npcRef = npcRef;
        }

        @Nonnull public String encounterId(){ return this.encounterId; }
        @Nonnull public UUID challengerUuid(){ return this.challengerUuid; }
        @Nonnull public Ref<EntityStore> npcRef(){ return this.npcRef; }
    }

    // --- Post-events ---

    /** A challenged Wandering Rival Cultivator has been defeated - the winner's reward has already been paid. */
    public record RivalDefeatedEvent(@Nonnull String encounterId, @Nonnull UUID winnerUuid, @Nonnull String worldName,
                                     float qiAwarded, boolean manualAwarded, boolean materialAwarded){}

    // --- Listener registration ---

    private static final List<Consumer<PreRivalChallengeEvent>> PRE_CHALLENGE = EventBus.newListenerList();
    private static final List<Consumer<RivalDefeatedEvent>> DEFEATED = EventBus.newListenerList();

    public static void onPreRivalChallenge(@Nonnull Consumer<PreRivalChallengeEvent> listener){ PRE_CHALLENGE.add(listener); }
    public static void onRivalDefeated(@Nonnull Consumer<RivalDefeatedEvent> listener){ DEFEATED.add(listener); }

    // --- Internal dispatch (called by RivalManager/its ECS systems; not API) ---

    public static boolean firePreRivalChallenge(@Nonnull PreRivalChallengeEvent event){ return EventBus.fire(PRE_CHALLENGE, event, "PreRivalChallengeEvent"); }
    public static void fireRivalDefeated(@Nonnull RivalDefeatedEvent event){ EventBus.dispatch(DEFEATED, event, "RivalDefeatedEvent"); }
}
