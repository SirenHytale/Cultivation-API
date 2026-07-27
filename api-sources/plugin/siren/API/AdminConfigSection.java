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
     * Persists this section after an admin's edits have been applied through
     * {@link AdminConfigField#set}. Called once per save, and only if at least
     * one of this section's fields actually changed.
     */
    void save();
}
