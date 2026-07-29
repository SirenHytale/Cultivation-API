package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A group of tunable numbers another mod contributes to Cultivation's admin
 * menu, appearing as its own section button on the Config tab alongside
 * Cultivation's own nine - and, for admins, in the Cultivation settings menu
 * too.
 *
 * <p>Register from your plugin's {@code setup()} with
 * {@link CultivationAPI#registerAdminConfigSection}. One section per config
 * file is the shape Cultivation uses for itself and the one that reads best.</p>
 *
 * <p>Both pages that render a section are admin-gated on
 * {@code cultivation.admin}, so a section may expose genuine balance numbers
 * without exposing them to ordinary players.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>{@link #getFields} and each field's {@code get} are called while building a
 * page; {@code set} and {@link #save} only when an admin presses Save. All of it
 * happens on that admin's world thread. Return a stable field list - the page
 * matches an admin's in-flight edits to fields by key, so a list that changes
 * shape between render and save silently drops those edits.</p>
 */
public interface AdminConfigSection {

    /**
     * A stable id, unique across every registered section, which is what a
     * click on this section's button sends back. Namespace it with your mod
     * name (e.g. {@code "SoulRings:spiritPower"}). Deliberately independent of
     * list order, so registering another section cannot silently retarget a
     * click.
     */
    @Nonnull
    String getKey();

    /** The section button and heading. Use a {@code Message.translation(...)} so it localises. */
    @Nonnull
    Message getLabel();

    /** One line explaining what this section tunes - the button tooltip and the text under the heading. */
    @Nonnull
    Message getHint();

    /** The rows, in display order. */
    @Nonnull
    List<AdminConfigField> getFields();

    /**
     * Where this section sits on the rail, lowest first. Cultivation's own nine
     * occupy {@link #SORT_BUILTIN_FIRST} upward in steps of 100, so a value
     * between two of them slots a section in among them rather than after them.
     * Sections declaring the same order keep registration order.
     *
     * <p>Defaults to {@link #SORT_LAST}, which is where every section registered
     * before ordering existed has always appeared: after Cultivation's own.</p>
     */
    default int getSortOrder() {
        return SORT_LAST;
    }

    /**
     * @return whether this section should appear at all right now. Read on every
     * render, so a section belonging to a subsystem the server owner has switched
     * off can hide itself instead of offering settings that do nothing.
     */
    default boolean isVisible() {
        return true;
    }

    /** Where Cultivation's own first section sits; its own are spaced 100 apart from here. */
    int SORT_BUILTIN_FIRST = 100;

    /**
     * The end of the range Cultivation reserves for its own sections. A section
     * ordered above this counts as contributed by another mod, which is what the
     * settings menu lists (the admin page lists everything either way).
     */
    int SORT_BUILTIN_LAST = 10_000;

    /** The default - after every built-in section. */
    int SORT_LAST = 100_000;

    /**
     * Persists this section after an admin's edits have been applied through
     * {@link AdminConfigField#set}. Called once per save, and only if at least
     * one of this section's fields actually changed.
     */
    void save();
}
