package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * One editable setting in an {@link AdminConfigSection} - a single row of the
 * admin Config tab, rendered with the same widgets Cultivation's own settings
 * use.
 *
 * <p>Build one with {@link CultivationAPI#newAdminConfigField} and friends;
 * there is rarely a reason to implement this interface directly.</p>
 *
 * <h2>Kinds</h2>
 *
 * <p>A field is one of five {@link Kind}s, each backed by the vanilla row widget
 * of the same shape. Which accessors matter depends on the kind, and everything
 * that does not apply has a harmless default, so an implementation only ever
 * writes the half it uses:</p>
 *
 * <ul>
 *   <li>{@link Kind#NUMBER} and {@link Kind#INT} - {@link #get()} /
 *       {@link #set(double)}. INT is the same value restricted to whole numbers
 *       by its widget.</li>
 *   <li>{@link Kind#BOOLEAN} - {@link #getBoolean()} / {@link #setBoolean}. A
 *       real checkbox, so a master on/off toggle no longer has to masquerade as
 *       a 0/1 number.</li>
 *   <li>{@link Kind#CHOICE} - {@link #getText()} / {@link #setText}, with
 *       {@link #getChoices()} supplying the dropdown's options. The right shape
 *       for anything enum-valued (a realm name, a dao element, a policy).</li>
 *   <li>{@link Kind#TEXT} - {@link #getText()} / {@link #setText}, free-form.</li>
 * </ul>
 *
 * <p>Anything list-shaped still belongs in the config file rather than this
 * editor - Cultivation makes the same call for its own Qi-absorption item table
 * and its technique rule set.</p>
 */
public interface AdminConfigField {

    /** What kind of widget this row is, and therefore which accessors carry its value. */
    enum Kind {
        /** A decimal number field. The default, and what every pre-kind field is. */
        NUMBER,
        /** A whole-number field - same storage as {@link #NUMBER}, no decimal places. */
        INT,
        /** A checkbox. */
        BOOLEAN,
        /** A dropdown over {@link AdminConfigField#getChoices()}. */
        CHOICE,
        /** A free-form text field. */
        TEXT
    }

    /**
     * A stable id, unique across EVERY section registered by EVERY mod - the
     * page keys an admin's in-flight edits by it. Namespace it with your mod
     * name (e.g. {@code "SoulRings:BaseXp"}).
     */
    @Nonnull
    String getKey();

    /** The row's label. Use a {@code Message.translation(...)} so it localises. */
    @Nonnull
    Message getLabel();

    /**
     * @return which widget renders this row. Defaults to {@link Kind#NUMBER}, so
     * a field written before kinds existed keeps behaving exactly as it did.
     */
    @Nonnull
    default Kind getKind() {
        return Kind.NUMBER;
    }

    /**
     * @return an optional line explaining what this setting does, shown as the
     * row's tooltip. Null for no tooltip, which is the default.
     *
     * <p><b>A plain String, deliberately - a tooltip cannot be translated.</b>
     * {@code TooltipText} is a String property client-side, and handing it a
     * {@code Message} disconnects the player mid-session rather than failing
     * quietly. Anything the player must be able to read in their own language
     * belongs in {@link #getLabel()}, which is rendered through {@code TextSpans}
     * and does take a Message.</p>
     */
    @Nullable
    default String getTooltip() {
        return null;
    }

    /**
     * The value to show for a {@link Kind#NUMBER} or {@link Kind#INT} field.
     * Read fresh on every render, so a config reload is picked up.
     */
    default double get() {
        return 0d;
    }

    /**
     * Applies an admin's edit to a {@link Kind#NUMBER} or {@link Kind#INT} field.
     * Called only when Save is pressed, on the world thread of the admin doing
     * the saving.
     *
     * <p>Clamp here if the value has a valid range - the page re-displays
     * whatever {@link #get} returns afterwards, so a coerced value is shown back
     * to the admin rather than silently disagreeing with what they typed.
     * Persisting is {@link AdminConfigSection#save}'s job, not this method's.</p>
     */
    default void set(double value) {
    }

    /** The value to show for a {@link Kind#BOOLEAN} field. */
    default boolean getBoolean() {
        return false;
    }

    /** Applies an admin's edit to a {@link Kind#BOOLEAN} field. Same contract as {@link #set(double)}. */
    default void setBoolean(boolean value) {
    }

    /**
     * The value to show for a {@link Kind#CHOICE} or {@link Kind#TEXT} field. For
     * a CHOICE this must be one of {@link #getChoices()}' ids, or the dropdown
     * opens with nothing selected.
     */
    @Nonnull
    default String getText() {
        return "";
    }

    /**
     * Applies an admin's edit to a {@link Kind#CHOICE} or {@link Kind#TEXT}
     * field. Same contract as {@link #set(double)} - and for a CHOICE, re-resolve
     * rather than trust: the id arrived from a client, and an option that has
     * since stopped being valid must not be accepted just because it was once
     * offered.
     */
    default void setText(@Nonnull String value) {
    }

    /**
     * The options a {@link Kind#CHOICE} field offers, in display order. Read on
     * every render, so a set that depends on what other mods have registered
     * stays current. Ignored for every other kind.
     */
    @Nonnull
    default List<AdminConfigChoice> getChoices() {
        return List.of();
    }
}
