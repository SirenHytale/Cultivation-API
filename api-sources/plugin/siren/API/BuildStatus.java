package plugin.siren.API;

/**
 * What Cultivation's build check has concluded about one installed mod.
 *
 * <p>Three states rather than a boolean, because "we could not tell" is a real
 * and common answer - the check needs a network round trip, and a server behind
 * a firewall or running before its version's digest was published is not making
 * any claim about itself either way.</p>
 *
 * @see CultivationAPI#registerBuildCheck
 */
public enum BuildStatus {

    /**
     * Nothing has been established. The check has not run yet, the request
     * failed, or the published list has no entry for this mod's version.
     *
     * <p>This is the state a jar is in for the first seconds of every server's
     * life, and permanently on a server with no outbound network. It is never
     * evidence of anything.</p>
     */
    UNKNOWN,

    /** The jar's code matches a digest published as official for its version. */
    OFFICIAL,

    /**
     * The published list carries digests for this version and the installed
     * jar's code matches none of them - so this code was built by somebody else.
     *
     * <p>The honest reading of this state is "this jar's classes are not the
     * ones that were shipped". That covers a decompile-and-recompile, which is
     * what it exists to notice, but also a jar somebody repacked for a modpack
     * and a download that arrived damaged. It is a fact about bytes, not an
     * accusation about a person.</p>
     */
    UNOFFICIAL
}
