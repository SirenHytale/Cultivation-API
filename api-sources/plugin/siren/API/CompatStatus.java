package plugin.siren.API;

/**
 * What the compatibility check concluded about one addon against the
 * Cultivation it is actually running on.
 *
 * @see CompatCheck
 */
public enum CompatStatus {

    /**
     * Nothing is known yet, or nothing could be learned.
     *
     * <p>The state before the first check lands, and the state it stays in
     * whenever the answer cannot be trusted: a dead host, a timeout, a malformed
     * body, or an addon version the published matrix says nothing about. An addon
     * MUST treat this as "carry on" - it is the state every offline server sits
     * in permanently.</p>
     */
    UNKNOWN,

    /** The matrix names a range for this addon version, and Cultivation is inside it. */
    COMPATIBLE,

    /**
     * The matrix names a range for this addon version and Cultivation is outside
     * it, or names this exact Cultivation version as blocked.
     *
     * <p>The only verdict an addon should act on. Reached only by a positive
     * statement about this exact pairing - never by absence, and never by a
     * failed request.</p>
     */
    INCOMPATIBLE
}
