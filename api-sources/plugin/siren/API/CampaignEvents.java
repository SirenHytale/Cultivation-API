package plugin.siren.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Narrative Campaign events. See {@link CultivationEvents} for the
 * conventions every {@code *Events} class in this package shares (pre vs
 * post, cancellation, threading, registration) - this class follows {@link
 * QuestEvents}'s own split most closely, for the identical reasoning: {@link
 * PreCampaignChapterAdvanceEvent} is the one genuinely vetoable DECISION
 * ("should THIS player move into THIS chapter right now" - a race/sect/event-
 * window gate an addon may have real reasons for), fired for the very first
 * chapter too ({@code fromChapterIndex} of {@code -1}) so an addon can refuse
 * a campaign's start the same way it refuses any later advance. {@link
 * CampaignChapterAdvanceEvent} and {@link CampaignCompleteEvent} are plain
 * observations of a machine already running, the same post-only shape {@link
 * QuestEvents}' own {@code QuestStepAdvanceEvent}/{@code QuestCompleteEvent}
 * are.
 *
 * <p>Dispatched by {@code plugin.siren.Utils.Campaign.CampaignManager}; this
 * class only declares the surface.</p>
 */
public final class CampaignEvents {
    private CampaignEvents(){}

    // --- Post-events ---

    /**
     * A chapter just became current for a player - either the campaign's very
     * first chapter (via {@code CampaignManager.start}) or an advance off a
     * finished one. {@code newChapterIndex} may still be locked behind its own
     * realm floor; check {@code CampaignManager.getProgress} if that matters
     * to a listener.
     */
    public record CampaignChapterAdvanceEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                               @Nonnull String campaignId, int previousChapterIndex, int newChapterIndex){}

    /** Every chapter of a campaign is finished. Fires exactly once per run, the moment the last chapter's chain(s) complete. */
    public record CampaignCompleteEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player, @Nonnull String campaignId){}

    // --- Pre-events ---

    /**
     * A player is about to move into a new chapter (or begin the campaign's
     * first one, when {@code fromChapterIndex} is {@code -1}). Every built-in
     * refusal (realm gate, already active/completed) has already passed;
     * cancel to refuse it anyway. Nothing is written when a listener cancels -
     * the chapter index is not advanced and no chain is accepted.
     */
    public static final class PreCampaignChapterAdvanceEvent extends CancellableEvent {
        private final Ref<EntityStore> ref;
        private final PlayerRef player;
        private final String campaignId;
        private final int fromChapterIndex;
        private final int toChapterIndex;

        public PreCampaignChapterAdvanceEvent(@Nonnull Ref<EntityStore> ref, @Nullable PlayerRef player,
                                              @Nonnull String campaignId, int fromChapterIndex, int toChapterIndex){
            this.ref = ref;
            this.player = player;
            this.campaignId = campaignId;
            this.fromChapterIndex = fromChapterIndex;
            this.toChapterIndex = toChapterIndex;
        }

        @Nonnull public Ref<EntityStore> ref(){ return this.ref; }
        @Nullable public PlayerRef player(){ return this.player; }
        @Nonnull public String campaignId(){ return this.campaignId; }
        /** {@code -1} for the campaign's very first chapter. */
        public int fromChapterIndex(){ return this.fromChapterIndex; }
        public int toChapterIndex(){ return this.toChapterIndex; }
    }

    // --- Listener registration ---

    private static final List<Consumer<PreCampaignChapterAdvanceEvent>> PRE_ADVANCE = EventBus.newListenerList();
    private static final List<Consumer<CampaignChapterAdvanceEvent>> ADVANCE = EventBus.newListenerList();
    private static final List<Consumer<CampaignCompleteEvent>> COMPLETE = EventBus.newListenerList();

    public static void onPreCampaignChapterAdvance(@Nonnull Consumer<PreCampaignChapterAdvanceEvent> listener){ PRE_ADVANCE.add(listener); }
    public static void onCampaignChapterAdvance(@Nonnull Consumer<CampaignChapterAdvanceEvent> listener){ ADVANCE.add(listener); }
    public static void onCampaignComplete(@Nonnull Consumer<CampaignCompleteEvent> listener){ COMPLETE.add(listener); }

    // --- Internal dispatch (called by CampaignManager; not API) ---

    public static boolean firePreCampaignChapterAdvance(@Nonnull PreCampaignChapterAdvanceEvent event){
        return EventBus.fire(PRE_ADVANCE, event, "PreCampaignChapterAdvanceEvent");
    }
    public static void fireCampaignChapterAdvance(@Nonnull CampaignChapterAdvanceEvent event){
        EventBus.dispatch(ADVANCE, event, "CampaignChapterAdvanceEvent");
    }
    public static void fireCampaignComplete(@Nonnull CampaignCompleteEvent event){
        EventBus.dispatch(COMPLETE, event, "CampaignCompleteEvent");
    }
}
