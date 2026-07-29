package plugin.siren.API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One option of an {@link AdminConfigField} of kind
 * {@link AdminConfigField.Kind#CHOICE} - the entries an admin picks between in
 * that row's dropdown.
 *
 * <p>Labelled the same way every other named thing in this API is (a technique,
 * a race, a codex entry): a {@code labelKey} when the text lives in a language
 * file, else a raw {@code displayName}. A dropdown entry CAN carry a localized
 * string, unlike a tooltip, so prefer the key.</p>
 *
 * @param id          what {@link AdminConfigField#getText()} returns and
 *                    {@link AdminConfigField#setText} is handed when this option
 *                    is picked. Stable, stored as-is, never shown to anyone.
 * @param labelKey    a {@code server.lang} key for the option's name, or
 *                    {@code null} to show {@code displayName} untranslated.
 * @param displayName shown when {@code labelKey} is null.
 */
public record AdminConfigChoice(@Nonnull String id, @Nullable String labelKey, @Nonnull String displayName) {

    /** An option whose text is translated. */
    @Nonnull
    public static AdminConfigChoice translated(@Nonnull String id, @Nonnull String labelKey) {
        return new AdminConfigChoice(id, labelKey, id);
    }

    /** An option shown as plain, untranslated text. */
    @Nonnull
    public static AdminConfigChoice raw(@Nonnull String id, @Nonnull String displayName) {
        return new AdminConfigChoice(id, null, displayName);
    }

    /**
     * An option backed by an enum constant, whose id is the constant's own name -
     * the shape almost every choice in Cultivation's own settings takes.
     */
    @Nonnull
    public static AdminConfigChoice of(@Nonnull Enum<?> constant, @Nullable String labelKey) {
        return new AdminConfigChoice(constant.name(), labelKey, constant.name());
    }
}
