package plugin.siren.API;

import plugin.siren.ECS.Components.TechniqueComponent;
import plugin.siren.ECS.Technique.HotkeyKey;
import plugin.siren.ECS.Technique.HotkeyModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One keybind layout, detached from the player who wrote it.
 *
 * <p>A {@code TechniqueComponent.Preset} only ever exists inside a cultivator.
 * This is the same four bindings as a value: immutable, safe to hold, and able
 * to travel - onto an item, into a config, across a save. Produce one with
 * {@link CultivationAPI#exportTechniquePreset(com.hypixel.hytale.component.ComponentAccessor,
 * com.hypixel.hytale.component.Ref, int)} and put it back with
 * {@link CultivationAPI#importTechniquePreset}.</p>
 *
 * <p><b>A snapshot is not a grant.</b> It records which arts a layout pointed
 * at; it never confers them. The import filter refuses every art the receiving
 * cultivator has not come by, and nothing on this class or on the import path
 * touches a learned set - see {@link CultivationAPI#importTechniquePreset} for
 * the exact rule and why it lives there rather than in whatever addon is
 * carrying the snapshot.</p>
 *
 * <h2>Carrying one in a single string</h2>
 *
 * <p>{@link #serialize()} and {@link #parse(String)} round-trip the whole
 * snapshot - format version, name, author and all four slots - through one
 * string, which is what lets an addon keep a layout in a single
 * {@code Codec.STRING} item metadata field. The per-slot part of that string is
 * the component's own {@code MODIFIER:KEY:techniqueId} form, written and read by
 * {@link TechniqueComponent#bindsString} and
 * {@link TechniqueComponent#readBindsString} rather than by a second copy of the
 * grammar here. {@link #parse(String)} is total: it answers null for anything it
 * cannot read and never throws, because the string it is handed came off an item
 * a client can edit.</p>
 *
 * @param name       what the layout is called. Trimmed and stripped of the
 *                   delimiters the wire form uses, by
 *                   {@link TechniqueComponent#sanitisePresetName}
 * @param authorName who it came from, for display. Sanitised the same way, and
 *                   empty when unknown - it is a label, never an identity: do
 *                   not authorise anything off it
 * @param slots      exactly {@link TechniqueComponent#BIND_COUNT} bindings, in
 *                   slot order
 */
public record TechniquePresetSnapshot(@Nonnull String name, @Nonnull String authorName, @Nonnull List<Slot> slots) {

    /** The separator between the version, the name, the author and the bindings in {@link #serialize()}. */
    private static final char FIELD_SEPARATOR = '|';

    /**
     * The format version {@link #serialize()} writes and {@link #parse(String)}
     * accepts. Leading, so a later build can tell at a glance whether it knows
     * how to read a slip somebody is still carrying - and refuse it cleanly
     * rather than mis-reading it - without having to guess from the shape.
     */
    private static final String FORMAT_VERSION = "1";

    /**
     * One binding of a snapshot: which key, held under which modifier, fires
     * which art.
     *
     * @param modifier   the modifier that must be held. {@link HotkeyModifier#NONE}
     *                   is a switched-off slot, not a bare key - see that enum
     * @param key        the key it fires on
     * @param techniqueId the art it fires, or null for an empty slot
     */
    public record Slot(@Nonnull HotkeyModifier modifier, @Nonnull HotkeyKey key, @Nullable String techniqueId) {
        public Slot {
            modifier = modifier == null ? HotkeyModifier.NONE : modifier;
            key = Objects.requireNonNull(key, "key");
            techniqueId = techniqueId == null || techniqueId.isBlank() ? null : techniqueId.trim();
        }

        /** @return true if this slot points at an art at all. */
        public boolean isBound() {
            return this.techniqueId != null;
        }
    }

    /**
     * The result of an import, with enough detail to tell the player what
     * happened to each art rather than only whether it worked.
     *
     * @param status          what happened
     * @param index           the index of the layout that was added, or -1 if
     *                        none was
     * @param finalName       what the new layout ended up called - after the
     *                        collision suffix and the length clamp - or "" if
     *                        none was added
     * @param imported        how many of the four bindings carry an art on the
     *                        new layout
     * @param droppedUnknown  how many arts were refused because this server does
     *                        not register them, or this cultivator has not come
     *                        by them
     * @param droppedDisabled how many arts were refused because the server owner
     *                        has switched that technique off
     */
    public record ImportResult(@Nonnull Status status, int index, @Nonnull String finalName,
                               int imported, int droppedUnknown, int droppedDisabled) {

        /** What {@link CultivationAPI#importTechniquePreset} did with a snapshot. */
        public enum Status {
            /** A new layout was added and filled. {@link ImportResult#index()} names it. */
            ADDED,
            /**
             * The cultivator is already at {@link CultivationAPI#getMaxTechniquePresets()}
             * layouts. Nothing was added, nothing was changed, and nothing about
             * the snapshot is lost - it can be imported again once they delete
             * one.
             */
            AT_CAP,
            /**
             * Every art on the snapshot was filtered out, so the layout would
             * have arrived empty. Refused rather than added blank; the two
             * dropped counts say why.
             */
            EMPTY_AFTER_FILTER,
            /**
             * The server owner has the technique system switched off entirely
             * ({@code Techniques-Enabled}, read through
             * {@code TechniqueManager.isSystemEnabled()}).
             */
            DISABLED
        }

        public ImportResult {
            Objects.requireNonNull(status, "status");
            finalName = finalName == null ? "" : finalName;
        }

        /** @return true if a layout was added. */
        public boolean isAdded() {
            return this.status == Status.ADDED;
        }

        /** @return how many arts the filter refused, for whatever reason. */
        public int dropped() {
            return this.droppedUnknown + this.droppedDisabled;
        }
    }

    public TechniquePresetSnapshot {
        name = TechniqueComponent.sanitisePresetName(name);
        authorName = TechniqueComponent.sanitisePresetName(authorName);
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        if (slots.size() != TechniqueComponent.BIND_COUNT) {
            throw new IllegalArgumentException(
                    "a technique preset snapshot has exactly " + TechniqueComponent.BIND_COUNT + " slots, got " + slots.size());
        }
    }

    /**
     * Builds a snapshot, padding a short list with empty slots rather than
     * refusing it - the forgiving door, where the canonical constructor insists
     * on exactly {@link TechniqueComponent#BIND_COUNT}.
     *
     * <p>A list longer than that is cut, not rejected: the extra bindings could
     * never be applied to a cultivator anyway.</p>
     */
    @Nonnull
    public static TechniquePresetSnapshot of(@Nullable String name, @Nullable String authorName,
                                             @Nullable List<Slot> slots) {
        List<Slot> padded = new ArrayList<>(TechniqueComponent.BIND_COUNT);
        for (int index = 0; index < TechniqueComponent.BIND_COUNT; index++) {
            Slot slot = slots != null && index < slots.size() ? slots.get(index) : null;
            padded.add(slot != null
                    ? slot
                    : new Slot(HotkeyModifier.NONE, TechniqueComponent.defaultBindKey(index), null));
        }

        return new TechniquePresetSnapshot(name == null ? "" : name, authorName == null ? "" : authorName, padded);
    }

    /** @return how many of the four slots actually point at an art. */
    public int boundSlotCount() {
        int bound = 0;
        for (Slot slot : this.slots) {
            if (slot.isBound()) {
                bound++;
            }
        }
        return bound;
    }

    /** @return every art id on this snapshot, in slot order, skipping empty slots. */
    @Nonnull
    public List<String> techniqueIds() {
        List<String> ids = new ArrayList<>(TechniqueComponent.BIND_COUNT);
        for (Slot slot : this.slots) {
            if (slot.techniqueId() != null) {
                ids.add(slot.techniqueId());
            }
        }
        return ids;
    }

    /**
     * The whole snapshot as one string, so it fits in a single
     * {@code Codec.STRING} field:
     * {@code 1|name|author|NONE:KEY_6:,ALT:KEY_7:cloud_step,NONE:KEY_8:,NONE:KEY_9:}
     *
     * <p>The leading {@code 1} is the format version, and it is there so that a
     * slip written by today's build can still be read - or knowingly refused - by
     * a later one. The bindings field is written by
     * {@link TechniqueComponent#bindsString}, the component's own serialiser, not
     * by a second copy of that grammar here. Splitting is safe because
     * {@link TechniqueComponent#sanitisePresetName} has already taken the
     * separators out of both the name and the author.</p>
     *
     * @see #parse(String)
     */
    @Nonnull
    public String serialize() {
        return FORMAT_VERSION + String.valueOf(FIELD_SEPARATOR) + this.name
                + FIELD_SEPARATOR + this.authorName
                + FIELD_SEPARATOR + slotsString();
    }

    /**
     * Reads back a string written by {@link #serialize()}.
     *
     * <p><b>Total.</b> It never throws, whatever it is handed. The string comes
     * off an item's metadata, which a determined client can put anything into, so
     * every failure - a blank string, a missing field, a version this build does
     * not know, a bindings field that is nonsense - is the same answer: null. Do
     * not let a caller distinguish them; there is nothing useful in the
     * difference. Nothing is authorised off the contents either way, since the
     * import filter re-decides every art from scratch (see
     * {@link CultivationAPI#importTechniquePreset}).</p>
     *
     * <p>An unreadable bindings field inside an otherwise well-formed version 1
     * string decodes as four empty slots rather than null - which the import
     * filter then refuses as
     * {@link ImportResult.Status#EMPTY_AFTER_FILTER}, an outcome a caller can
     * actually explain to a player.</p>
     *
     * @return the snapshot, or null if {@code wire} is not one this build wrote
     */
    @Nullable
    public static TechniquePresetSnapshot parse(@Nullable String wire) {
        if (wire == null || wire.isBlank()) {
            return null;
        }

        String[] fields = wire.split("\\" + FIELD_SEPARATOR, 4);
        if (fields.length < 3) {
            return null;
        }

        if (!FORMAT_VERSION.equals(fields[0].trim())) {
            return null;
        }

        // Version 1 is "1|name|author|bindings". A three-field string is read as
        // "1|name|bindings" with no author, which is the only sane reading of a
        // slip somebody assembled by hand from the format above.
        String name = fields[1];
        String author = fields.length == 4 ? fields[2] : "";
        String bindings = fields.length == 4 ? fields[3] : fields[2];

        try {
            return fromSlots(name, author, bindings);
        } catch (RuntimeException error) {
            // Belt and braces. Nothing below is expected to throw - readBindsString
            // tolerates every malformed field it can be handed - but parse() is
            // documented as total and a forged slip must never be able to take a
            // page build down with it.
            return null;
        }
    }

    /**
     * The four bindings in the component's own {@code MODIFIER:KEY:techniqueId}
     * per-slot form joined by commas - the same text one preset occupies inside a
     * saved {@code TechniqueComponent}.
     */
    @Nonnull
    private String slotsString() {
        HotkeyModifier[] modifiers = new HotkeyModifier[TechniqueComponent.BIND_COUNT];
        HotkeyKey[] keys = new HotkeyKey[TechniqueComponent.BIND_COUNT];
        String[] techniques = new String[TechniqueComponent.BIND_COUNT];
        for (int index = 0; index < TechniqueComponent.BIND_COUNT; index++) {
            Slot slot = this.slots.get(index);
            modifiers[index] = slot.modifier();
            keys[index] = slot.key();
            techniques[index] = slot.techniqueId();
        }

        return TechniqueComponent.bindsString(modifiers, keys, techniques);
    }

    /** Builds a snapshot from a name, an author and the per-slot bindings string. */
    @Nonnull
    private static TechniquePresetSnapshot fromSlots(@Nullable String name, @Nullable String authorName,
                                                     @Nullable String slotWire) {
        HotkeyModifier[] modifiers = new HotkeyModifier[TechniqueComponent.BIND_COUNT];
        HotkeyKey[] keys = new HotkeyKey[TechniqueComponent.BIND_COUNT];
        String[] techniques = new String[TechniqueComponent.BIND_COUNT];
        TechniqueComponent.readBindsString(slotWire, modifiers, keys, techniques);

        List<Slot> slots = new ArrayList<>(TechniqueComponent.BIND_COUNT);
        for (int index = 0; index < TechniqueComponent.BIND_COUNT; index++) {
            slots.add(new Slot(modifiers[index], keys[index], techniques[index]));
        }

        return new TechniquePresetSnapshot(name == null ? "" : name, authorName == null ? "" : authorName, slots);
    }
}
