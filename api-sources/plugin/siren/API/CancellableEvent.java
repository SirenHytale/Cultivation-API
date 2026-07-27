package plugin.siren.API;

/**
 * Base class for every "pre" event in Cultivation's API - the ones fired
 * BEFORE something happens, which a listener may veto.
 *
 * <p>Cancelling means "don't do this": the mod's own code checks the flag the
 * instant dispatch returns and abandons the operation, leaving no state
 * changed. Nothing is rolled back, because nothing was applied yet - that's
 * the whole reason these fire before the fact.</p>
 *
 * <p>Many pre-events also expose setters for the numbers feeding the operation
 * (a breakthrough's Qi cost, a technique's cooldown, a tribulation bolt's
 * damage). Whatever the listeners leave in those fields when dispatch finishes
 * is what the mod actually uses, so this is the supported way to re-tune any
 * mechanic from an addon without touching Cultivation's own config files.</p>
 *
 * <p>Every listener runs, even after one cancels - a later listener is free to
 * call {@code setCancelled(false)} and let the operation through, so plugin
 * load order decides who wins a disagreement. If you only want to observe,
 * listen for the matching post-event instead; those fire once the change is
 * committed and cannot be vetoed.</p>
 */
public abstract class CancellableEvent {
    private boolean cancelled;

    /** @return whether some listener has vetoed this operation. */
    public final boolean isCancelled(){
        return this.cancelled;
    }

    /**
     * Vetoes (or, with {@code false}, un-vetoes) the operation about to happen.
     * The caller abandons it entirely - no partial application, no post-event.
     */
    public final void setCancelled(boolean cancelled){
        this.cancelled = cancelled;
    }
}
