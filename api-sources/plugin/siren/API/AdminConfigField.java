package plugin.siren.API;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;

/**
 * One editable number in an {@link AdminConfigSection} - a single row of the
 * admin Config tab, rendered with the same widget Cultivation's own settings
 * use.
 *
 * <p>Build one with {@link CultivationAPI#newAdminConfigField}; there is rarely
 * a reason to implement this interface directly.</p>
 *
 * <p>Only numbers are supported, because the row widget behind it is a number
 * input. A boolean is best expressed as a 0/1 field with a label saying so, and
 * anything list-shaped belongs in the config file rather than this editor -
 * Cultivation makes the same call for its own booleans and its Qi-absorption
 * item table.</p>
 */
public interface AdminConfigField {

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

    /** The value to show. Read fresh on every render, so a config reload is picked up. */
    double get();

    /**
     * Applies an admin's edit. Called only when Save is pressed, on the world
     * thread of the admin doing the saving.
     *
     * <p>Clamp here if the value has a valid range - the page re-displays
     * whatever {@link #get} returns afterwards, so a coerced value is shown back
     * to the admin rather than silently disagreeing with what they typed.
     * Persisting is {@link AdminConfigSection#save}'s job, not this method's.</p>
     */
    void set(double value);
}
