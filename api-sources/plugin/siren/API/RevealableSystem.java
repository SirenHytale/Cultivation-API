package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.siren.Cultivation;
import plugin.siren.Utils.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * One player-facing system the Progressive Reveal feature can keep out of the
 * menus until it becomes relevant - see {@code Utils/Reveal/RevealManager}
 * for the evaluation/announcement pipeline this only describes.
 *
 * <p>Build one with {@link #builder(String)} and hand it to
 * {@link CultivationAPI#registerRevealableSystem}, exactly the way an addon
 * contributes a {@link CodexEntry}:</p>
 *
 * <pre>{@code CultivationAPI.registerRevealableSystem(
 *         RevealableSystem.builder("myMod:soulRings")
 *                 .name("server.myMod.reveal.soulRings.name")
 *                 .hint("server.myMod.reveal.soulRings.hint")
 *                 .revealRealm(CultivationRealm.QI_CONDENSATION)
 *                 .codexEntries("myMod:soulRings"));}</pre>
 *
 * <h2>Ids and stability</h2>
 *
 * <p>{@code id} is persisted verbatim inside {@code CultivationAchievementsComponent}'s
 * {@code RevealedSystems}/{@code RevealUnread} sets, so it must match
 * {@code ^[A-Za-z0-9_.:-]{1,64}$} and never be renamed once shipped - a rename
 * silently un-reveals the system for every account that had already earned
 * it. Registering the same id twice replaces the first, the same "last
 * registration wins" rule every other registry in this API follows.</p>
 *
 * <h2>Predicates are cheap, read-only, and never create state</h2>
 *
 * <p>{@code unlockedBy}/{@code engagedBy} are consulted from a plain
 * {@code store.getComponent}-style read against the player's own entity - the
 * same discipline {@code CompassManager}'s own suggestion checks already
 * follow (see that class's own doc). Never call a {@code getOrCreate}-style
 * accessor from one of these: {@code RevealManager.evaluate} runs outside a
 * system tick, but an addon's predicate has no way to know that from here, and
 * this contract has to hold regardless. A predicate that throws is treated as
 * having passed (the system reveals) and is logged once per id - never twice,
 * so a broken addon predicate cannot spam the log every evaluation.</p>
 */
public final class RevealableSystem {

    /** {@code revealRealm} value meaning "never reveal by realm alone" - only {@code unlockedBy}/{@code engagedBy} can surface it. */
    public static final int NEVER = Integer.MAX_VALUE;

    /** Where a system with no declared order goes. */
    public static final int SORT_DEFAULT = 10000;

    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_.:-]{1,64}$");

    /** One id + predicate-kind pair per warning - never re-logs the same broken predicate. */
    private static final java.util.Set<String> THREW_LOGGED = ConcurrentHashMap.newKeySet();

    private final String id;
    private final String nameKey;
    private final String hintKey;
    private final IntSupplier revealRealm;
    private final Predicate<PlayerRef> unlockedBy;
    private final Predicate<PlayerRef> engagedBy;
    private final BooleanSupplier enabledWhen;
    private final List<String> codexEntryIds;
    private final String opensCodexId;
    private final String opensMenuPageId;
    private final String openedNoteKey;
    private final int sortOrder;

    private RevealableSystem(Builder builder){
        this.id = builder.id;
        this.nameKey = builder.nameKey;
        this.hintKey = builder.hintKey;
        this.revealRealm = builder.revealRealm;
        this.unlockedBy = builder.unlockedBy;
        this.engagedBy = builder.engagedBy;
        this.enabledWhen = builder.enabledWhen;
        this.codexEntryIds = builder.codexEntryIds == null
                ? Collections.emptyList() : Collections.unmodifiableList(builder.codexEntryIds);
        this.opensCodexId = builder.opensCodexId;
        this.opensMenuPageId = builder.opensMenuPageId;
        this.openedNoteKey = builder.openedNoteKey;
        this.sortOrder = builder.sortOrder;
    }

    @Nonnull
    public static Builder builder(@Nonnull String id){
        return new Builder(id);
    }

    @Nonnull
    public String getId(){
        return this.id;
    }

    /** The system's short name - the Misc row label / Compass "Newly Opened" title. */
    @Nonnull
    public Message getName(){
        return Text.of(this.nameKey);
    }

    /** One line saying why it matters - the Compass/toast/chat hint. */
    @Nonnull
    public Message getHint(){
        return Text.of(this.hintKey);
    }

    /**
     * The realm ordinal (against {@code CultivationRealm.ordinal()}) at which
     * this system reveals on its own, or {@link #NEVER}. Read live - a config
     * admin override (see {@code Utils/Reveal/RevealConfig}) is applied by the
     * caller, not here.
     */
    public int getRevealRealmOrdinal(){
        return this.revealRealm.getAsInt();
    }

    /**
     * Whether {@code playerRef} just unlocked this system outright (reveals
     * AND announces, sticky once true). False if nothing was declared.
     */
    public boolean testUnlockedBy(@Nonnull PlayerRef playerRef){
        return safeTest(this.unlockedBy, playerRef, "unlockedBy");
    }

    /**
     * Whether {@code playerRef} has already engaged this system regardless of
     * realm (reveals silently, sticky once true, never announced on its own).
     * False if nothing was declared.
     */
    public boolean testEngagedBy(@Nonnull PlayerRef playerRef){
        return safeTest(this.engagedBy, playerRef, "engagedBy");
    }

    private boolean safeTest(@Nullable Predicate<PlayerRef> predicate, @Nonnull PlayerRef playerRef, @Nonnull String kind){
        if(predicate == null){
            return false;
        }

        try{
            return predicate.test(playerRef);
        }catch(Throwable throwable){
            if(THREW_LOGGED.add(this.id + ':' + kind)){
                Cultivation.LOGGER.atWarning().withCause(throwable).log(
                        "RevealableSystem '%s' threw from its %s predicate - treating it as revealed. "
                                + "Logged once for this id.", this.id, kind);
            }
            return true;
        }
    }

    /** Whether this system is switched on at all right now. Disabled = never announced, never counted as hidden either. */
    public boolean isEnabled(){
        return this.enabledWhen == null || this.enabledWhen.getAsBoolean();
    }

    /** Codex entry ids this system's visibility contributes to hiding (an entry hides only when EVERY claiming system is hidden). */
    @Nonnull
    public List<String> getCodexEntryIds(){
        return this.codexEntryIds;
    }

    /** The Codex entry id the Compass/notice "open" action deep-links to, or null. */
    @Nullable
    public String getOpensCodexId(){
        return this.opensCodexId;
    }

    /** The menu page id the Compass/notice "open" action opens, or null. */
    @Nullable
    public String getOpensMenuPageId(){
        return this.opensMenuPageId;
    }

    /** A {@code server.lang} key for how this system was opened (e.g. "Opened by Ascension"), or null for the plain "opened at {realm}" line. */
    @Nullable
    public String getOpenedNoteKey(){
        return this.openedNoteKey;
    }

    public int getSortOrder(){
        return this.sortOrder;
    }

    public static final class Builder {
        private final String id;
        private String nameKey;
        private String hintKey;
        private IntSupplier revealRealm = () -> 0;
        private Predicate<PlayerRef> unlockedBy;
        private Predicate<PlayerRef> engagedBy;
        private BooleanSupplier enabledWhen = () -> true;
        private List<String> codexEntryIds;
        private String opensCodexId;
        private String opensMenuPageId;
        private String openedNoteKey;
        private int sortOrder = SORT_DEFAULT;

        private Builder(@Nonnull String id){
            this.id = id;
        }

        @Nonnull
        public Builder name(@Nonnull String translationKey){
            this.nameKey = translationKey;
            return this;
        }

        @Nonnull
        public Builder hint(@Nonnull String translationKey){
            this.hintKey = translationKey;
            return this;
        }

        /** A fixed realm gate. */
        @Nonnull
        public Builder revealRealm(@Nonnull plugin.siren.ECS.Realms.CultivationRealm realm){
            this.revealRealm = realm::ordinal;
            return this;
        }

        /** A live-tunable realm gate (a config getter, so an admin edit takes effect without a restart). */
        @Nonnull
        public Builder revealRealm(@Nonnull IntSupplier realmOrdinalSupplier){
            this.revealRealm = realmOrdinalSupplier;
            return this;
        }

        /** {@link #NEVER} - only {@code unlockedBy}/{@code engagedBy} can reveal it. */
        @Nonnull
        public Builder neverByRealm(){
            this.revealRealm = () -> NEVER;
            return this;
        }

        @Nonnull
        public Builder unlockedBy(@Nonnull Predicate<PlayerRef> predicate){
            this.unlockedBy = predicate;
            return this;
        }

        @Nonnull
        public Builder engagedBy(@Nonnull Predicate<PlayerRef> predicate){
            this.engagedBy = predicate;
            return this;
        }

        @Nonnull
        public Builder enabledWhen(@Nonnull BooleanSupplier supplier){
            this.enabledWhen = supplier;
            return this;
        }

        @Nonnull
        public Builder codexEntries(@Nonnull String... entryIds){
            this.codexEntryIds = List.of(entryIds);
            return this;
        }

        @Nonnull
        public Builder opensCodex(@Nonnull String entryId){
            this.opensCodexId = entryId;
            return this;
        }

        @Nonnull
        public Builder opensMenuPage(@Nonnull String pageId){
            this.opensMenuPageId = pageId;
            return this;
        }

        @Nonnull
        public Builder openedNote(@Nonnull String translationKey){
            this.openedNoteKey = translationKey;
            return this;
        }

        @Nonnull
        public Builder sortOrder(int sortOrder){
            this.sortOrder = sortOrder;
            return this;
        }

        @Nonnull
        public RevealableSystem build(){
            if(this.id == null || !ID_PATTERN.matcher(this.id).matches()){
                throw new IllegalStateException("RevealableSystem id '" + this.id
                        + "' must match ^[A-Za-z0-9_.:-]{1,64}$.");
            }

            if(this.nameKey == null || this.hintKey == null){
                throw new IllegalStateException("RevealableSystem '" + this.id + "' needs both a name and a hint key.");
            }

            return new RevealableSystem(this);
        }
    }
}
