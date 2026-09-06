#!/usr/bin/env python3
"""Regenerates docs/events-reference.md from the API sources in api-sources/.

Every event in this API is either a `public record XEvent(...)` (a post-event)
or a `public static final class PreXEvent extends CancellableEvent` (a
cancellable pre-event), and each carries a one-paragraph javadoc saying what it
means. This script lifts both out of the source so the reference can never drift
from the code it documents.

Usage (from the repository root):

    python tools/gen_events_reference.py
"""

from __future__ import annotations

import html
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCES = ROOT / "api-sources" / "plugin" / "siren" / "API"
OUTPUT = ROOT / "docs" / "events-reference.md"

# Display order + the blurb that heads each section. Anything not listed here is
# not an event class and is documented elsewhere.
EVENT_FILES: list[tuple[str, str, str]] = [
    (
        "CultivationEvents",
        "Core progression",
        "Qi, meditation, rituals, breakthroughs, advancements, demotions, "
        "tribulations, the Heart-Devil Trial, Qi Deviation, the Ascension "
        "capstone, races, the skill tree and respecs.",
    ),
    (
        "DaoEvents",
        "Dao, alignment and karma",
        "Elemental daos, affinity drift, Yin-Yang lean, the Righteous/Devil "
        "path split, karma and Devil-path Qi harvesting.",
    ),
    (
        "DaoComprehensionEvents",
        "Dao comprehension (0.9.x)",
        "The layer on top of the Elemental Dao: the Heavenly Dao (天道) "
        "understanding track, the open Personal Dao registry (Sword/Slaughter/"
        "Space and whatever a mod adds beside them), and Dao Enlightenment "
        "(悟道). Does not replace or collide with `DaoEvents` above.",
    ),
    (
        "TechniqueEvents",
        "Techniques",
        "Performing and learning arts, fusing two into a third, Sword Flying, "
        "and the timed combat buffs.",
    ),
    (
        "ItemEvents",
        "Items, alchemy and refinement",
        "Loot drops, pills, spirit cores, manuals and weapon refinement.",
    ),
    (
        "AlchemyEvents",
        "Alchemy (0.10.0)",
        "The Pill Cauldron refining RITUAL - starting, resolving, and the Fire "
        "Watch (火候) tending prompts along the way. Complements `ItemEvents`' "
        "`PillConsumeEvent` above, which covers drinking a finished pill, not "
        "brewing one.",
    ),
    (
        "ForgingEvents",
        "Forging (0.9.x)",
        "Tempering an already-crafted Cultivation weapon/armor at a Forge "
        "Anchor - success, failure and botch outcomes.",
    ),
    (
        "TalismanEvents",
        "Talismans (0.9.x)",
        "Inscribing at a Talisman Desk (start and complete, with success/"
        "failed/botch outcomes) and using a finished talisman.",
    ),
    (
        "WeaponSpiritEvents",
        "Weapon Spirits (0.9.x)",
        "A Life-Bound Treasure's spirit (器灵) stirring awake, gaining a "
        "level, and being fed Qi through `/cultivation spirit nurture`.",
    ),
    (
        "BeastEvents",
        "Spirit beasts",
        "Taming, hatching, binding, summoning and companion growth.",
    ),
    (
        "BreedingEvents",
        "Spirit beast breeding (0.9.x)",
        "The two-cultivator ritual at a Beast Pen that produces an egg, and "
        "hatching a bred egg into a bound companion. Complements `BeastEvents` "
        "above - a bred egg still fires its `onBeastBind` when it hatches.",
    ),
    (
        "SectEvents",
        "Sects",
        "Founding, disbanding, membership, ranks, abbreviations, halls and "
        "inscriptions.",
    ),
    (
        "WarEvents",
        "Sect wars",
        "Declaring sieges and how they resolve.",
    ),
    (
        "DuelEvents",
        "Duels",
        "Challenges, duel start/end and Qi wager payouts.",
    ),
    (
        "FormationEvents",
        "Formations",
        "Laying and dispersing spirit arrays, and trap strikes.",
    ),
    (
        "DwellingEvents",
        "Cave Abodes",
        "Claiming, abandoning and lapsing an abode, Spirit Spring collection, "
        "upkeep and seclusion.",
    ),
    (
        "CelestialEvents",
        "Celestial events",
        "Server-wide phenomena - Spirit Tide, Meteor Shower, Blood Moon, and "
        "any an addon registered through `CelestialManager.registerEventType`. "
        "Both hooks are generic to every type rather than one pair per "
        "phenomenon, so switch on `type().id()` to react to a particular one.",
    ),
    (
        "BodyTemperingEvents",
        "Body tempering",
        "The second ladder, climbed by taking blows rather than by gathering "
        "Qi: XP earned from damage that reached the body, and the levels it "
        "buys. The pre-XP event carries a MUTABLE amount, so a listener can "
        "scale the reward rather than only allow or forbid it.",
    ),
    (
        "FistEvents",
        "Fist arts",
        "The third ladder, climbed by landing blows bare-handed - the mirror of "
        "body tempering, whose income is damage received. XP is measured by the "
        "damage that actually got through, so a punch a shield ate whole teaches "
        "nothing. The pre-XP event carries a MUTABLE amount, so a listener can "
        "scale the reward rather than only allow or forbid it.",
    ),
    (
        "MeridianEvents",
        "Meridian injuries (0.9.x)",
        "A named injury being inflicted or deepened, cured, and a Cracked "
        "Dantian's Qi spill being armed.",
    ),
    (
        "ProfileEvents",
        "Cultivation profiles",
        "Switching, creating and erasing the separate saves a player keeps of "
        "their own progress, and the expiry of a temporary sandbox profile.",
    ),
    (
        "PartyEvents",
        "Parties (0.9.x)",
        "Ad-hoc, session-only grouping - the foundation for a later "
        "multiplayer dungeon feature that is not built yet. Forming, joining, "
        "leaving, disbanding, and inviting.",
    ),
    (
        "PartnerEvents",
        "Partnered Cultivation (0.9.x)",
        "Two married cultivators drawing on the same spirit vein together, "
        "resolved every meditation tick - pairing, unpairing, and the Qi bonus "
        "the pairing grants. Requires Marriage; see `docs/compatibility.md`.",
    ),
    (
        "OathEvents",
        "Heavenly Oaths (0.9.x)",
        "Swearing, breaching and peacefully dissolving a Heavenly Oath (天道"
        "誓言), and cleansing the Dao-Heart Flaw a breach leaves behind.",
    ),
    (
        "CampaignEvents",
        "Narrative Campaign (0.9.x)",
        "The quest-line system's chapter advances (including a campaign's "
        "very first chapter) and campaign completion.",
    ),
    (
        "QuestEvents",
        "Wandering-NPC quests (0.9.x)",
        "Accepting a quest chain from a wandering NPC giver, advancing through "
        "its steps, completing it (reward fully paid), or abandoning it.",
    ),
    (
        "DepthsEvents",
        "Secret Realm Depths (0.9.x)",
        "A solo Depths run: starting, a floor clearing (with the escrow reward "
        "it just rolled), extraction actually paying out, and the run ending "
        "for any reason. Post-only - every one of these is a deterministic "
        "outcome of the run's own state machine.",
    ),
    (
        "SecretRealmEvents",
        "Secret Realms (0.9.x)",
        "A site's barrier coming down (openable) or going back up (closed). "
        "Post-only - opening/closing is a deterministic scheduler outcome, not "
        "a request anything downstream could meaningfully veto.",
    ),
    (
        "TreasureEvents",
        "Treasure and Ruin Exploration (0.9.x)",
        "Claiming a Buried Cache or entering a Ruin Vault - covers both "
        "Treasure tiers, since both are \"claiming\" the same kind of site.",
    ),
    (
        "MarketEvents",
        "Auction House and Traveling Merchant (0.9.x)",
        "Listing, buying, cancelling and expiring auction listings, plus the "
        "Traveling Merchant NPC opening and closing for business. Players are "
        "identified by UUID - a sold listing routinely pays out to a seller "
        "who is offline at the moment of sale.",
    ),
    (
        "TideEvents",
        "Beast Tides (0.9.x)",
        "A siege (兽潮) on a sect hall or a Cave Abode - starting a wave and "
        "resolving win/lose. Fires ALONGSIDE `CelestialEvents` for the tide's "
        "own `CelestialEventType` id, carrying the siege-specific detail "
        "(which target, how many waves) celestial events don't know about.",
    ),
    (
        "RivalEvents",
        "Wandering Rival Cultivators (0.9.x)",
        "Challenging a Wandering Rival Cultivator NPC, and its defeat payout.",
    ),
    (
        "WorldBossEvents",
        "Calamity Beasts / world boss (0.9.x)",
        "A wandering, solo world boss (灾劫兽) with no fixed target, unlike "
        "Beast Tide's place-anchored siege - its OMEN phase beginning, the "
        "boss NPC actually spawning, and the encounter resolving.",
    ),
    (
        "RiftEvents",
        "Void Rifts (0.10.0)",
        "A randomly-triggered, server-wide world event: a rift opens, throws "
        "a fixed number of corrupted-beast waves, spawns a boss-tier Warden, "
        "then resolves SEALED or COLLAPSED. Mostly post-only, the same "
        "\"auto-picked target, nothing to re-tune\" shape as `WorldBossEvents` "
        "- only the open itself is cancellable.",
    ),
    (
        "SeasonEvents",
        "Seasons (0.10.2)",
        "The shared season cadence opening and closing a season. Post-only - a "
        "season boundary is a deterministic outcome of one timestamp and one "
        "config value, with nothing usefully vetoable. **The boot self-heal's "
        "own open cannot reach an addon listener** (Cultivation's `setup()` "
        "runs first), so ask `CultivationAPI.getCurrentSeasonId()` for the "
        "current season rather than waiting for the event.",
    ),
    (
        "BountyEvents",
        "Bounty Board (0.10.2)",
        "A contract being posted to the board through `BountyManager.post` - "
        "the rotation's own generated contracts deliberately do NOT fire it - "
        "and a completed contract paying out. The claim is cancellable; a "
        "PARTIAL claim (a reward that did not fit) never fires the post-event.",
    ),
    (
        "StoreBenefitEvents",
        "Treasure Pavilion benefits",
        "Entitlements bought on xianxia.dev arriving and leaving. **These fire "
        "on the remote checker thread, not on a world thread**, and none of "
        "them is cancellable - both departures from every other class here, so "
        "read the class javadoc before a listener touches a player.",
    ),
]

RECORD_RE = re.compile(
    r"public record (?P<name>\w+Event)\s*\((?P<params>.*?)\)\s*\{",
    re.DOTALL,
)
PRE_CLASS_RE = re.compile(
    r"public static final class (?P<name>\w+Event) extends CancellableEvent\s*\{"
)
ENUM_RE = re.compile(r"public enum (?P<name>\w+)\s*\{")
LISTENER_RE = re.compile(
    r"public static void (?P<method>on\w+)\(@Nonnull Consumer<(?P<event>\w+)>"
)
# Accessors are often written as annotated one-liners
# (`@Nullable public PlayerRef player(){ ... }`), so the annotations have to be
# allowed both before `public` and captured, since a @Nullable accessor is
# something a consumer needs to know about.
ACCESSOR_RE = re.compile(
    r"^\s{8}(?P<annotations>(?:@\w+\s+)*)public (?:final )?"
    r"(?P<type>[\w.<>\[\], ?]+?) (?P<name>\w+)\(\)\s*\{",
    re.MULTILINE,
)
MUTATOR_RE = re.compile(
    r"^\s{8}(?:@\w+\s+)*public (?:final )?void (?P<name>set\w+)\((?P<params>[^)]*)\)\s*\{",
    re.MULTILINE,
)


def strip_javadoc(block: str) -> str:
    """Turns a raw /** ... */ block into one line of plain markdown."""
    body = block.strip()
    body = body.removeprefix("/**").removesuffix("*/")
    lines = [re.sub(r"^\s*\*ate?\s?", "", line).lstrip("* \t") for line in body.splitlines()]
    text = " ".join(line.strip() for line in lines if line.strip())

    # Javadoc inline tags -> markdown. {@code x} and {@link X#y} both become code.
    text = re.sub(r"\{@(?:code|literal)\s+([^}]*)\}", r"`\1`", text)
    text = re.sub(r"\{@link\s+#?([^}\s]+)(?:\s+[^}]*)?\}", r"`\1`", text)
    text = re.sub(r"</?p>", " ", text)
    text = re.sub(r"<b>(.*?)</b>", r"**\1**", text)
    text = re.sub(r"<[^>]+>", "", text)
    text = html.unescape(text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def javadoc_before(text: str, index: int) -> str:
    """The javadoc block immediately preceding `index`, or ''."""
    head = text[:index]
    end = head.rfind("*/")
    if end == -1:
        return ""
    # Nothing but whitespace may sit between the comment and the declaration,
    # otherwise it belongs to something else.
    if head[end + 2:].strip():
        return ""
    start = head.rfind("/**", 0, end)
    if start == -1:
        return ""
    return strip_javadoc(head[start:end + 2])


def body_of(text: str, open_brace_index: int) -> str:
    """The source between a declaration's `{` and its matching `}`."""
    depth = 0
    for i in range(open_brace_index, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[open_brace_index + 1:i]
    return ""


def format_record_params(params: str) -> list[str]:
    """`@Nonnull Ref<EntityStore> ref, float damage` -> ['Ref<EntityStore> ref', ...]"""
    out: list[str] = []
    depth = 0
    current = ""
    for char in params:
        if char in "<(":
            depth += 1
        elif char in ">)":
            depth -= 1
        if char == "," and depth == 0:
            out.append(current)
            current = ""
        else:
            current += char
    out.append(current)

    cleaned = []
    for param in out:
        param = re.sub(r"@\w+\s*", "", param).strip()
        param = re.sub(r"\s+", " ", param)
        if param:
            cleaned.append(param)
    return cleaned


def render_file(name: str, heading: str, blurb: str) -> str:
    path = SOURCES / f"{name}.java"
    text = path.read_text(encoding="utf-8")

    listeners = {match.group("event"): match.group("method") for match in LISTENER_RE.finditer(text)}

    lines = [f"## {heading}", "", f"`plugin.siren.API.{name}` — {blurb}", ""]

    enums = list(ENUM_RE.finditer(text))
    if enums:
        lines.append("**Enums declared here**")
        lines.append("")
        for match in enums:
            doc = javadoc_before(text, match.start())
            # The last constant in an enum carries neither a comma nor a
            # semicolon, so both have to be optional.
            constants = re.findall(
                r"^\s{8}([A-Z][A-Z0-9_]*)\s*[,;]?\s*$", body_of(text, match.end() - 1), re.MULTILINE
            )
            values = ", ".join(f"`{value}`" for value in constants)
            lines.append(f"- `{name}.{match.group('name')}` — {doc} Values: {values}")
        lines.append("")

    # Post-events first, then the cancellable pre-events, matching how the
    # source files themselves are laid out.
    lines.append("**Post-events** — fired once the change is committed; cannot be cancelled.")
    lines.append("")
    for match in RECORD_RE.finditer(text):
        event = match.group("name")
        doc = javadoc_before(text, match.start())
        params = format_record_params(match.group("params"))
        listener = listeners.get(event)
        lines.append(f"### `{event}`")
        lines.append("")
        if listener:
            lines.append(f"```java\n{name}.{listener}(event -> {{ /* ... */ }});\n```")
            lines.append("")
        if doc:
            lines.append(doc)
            lines.append("")
        if params:
            lines.append("| Accessor | Type |")
            lines.append("| --- | --- |")
            for param in params:
                parts = param.rsplit(" ", 1)
                if len(parts) == 2:
                    lines.append(f"| `{parts[1]}()` | `{parts[0]}` |")
            lines.append("")

    pre_matches = list(PRE_CLASS_RE.finditer(text))
    if pre_matches:
        lines.append("**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, "
                     "and any setter below re-tunes the numbers the mod then uses.")
        lines.append("")
        for match in pre_matches:
            event = match.group("name")
            doc = javadoc_before(text, match.start())
            body = body_of(text, match.end() - 1)
            listener = listeners.get(event)

            lines.append(f"### `{event}`")
            lines.append("")
            if listener:
                lines.append(f"```java\n{name}.{listener}(event -> {{ /* ... */ }});\n```")
                lines.append("")
            if doc:
                lines.append(doc)
                lines.append("")

            rows = []
            for accessor in ACCESSOR_RE.finditer(body):
                kind = "read (may be null)" if "@Nullable" in accessor.group("annotations") else "read"
                rows.append((f"{accessor.group('name')}()", accessor.group("type"), kind))
            for mutator in MUTATOR_RE.finditer(body):
                params = re.sub(r"@\w+\s*", "", mutator.group("params")).strip()
                param_type = params.rsplit(" ", 1)[0] if params else ""
                rows.append((f"{mutator.group('name')}({param_type})", "void", "re-tune"))

            if rows:
                lines.append("| Member | Type | |")
                lines.append("| --- | --- | --- |")
                for member, type_name, kind in rows:
                    lines.append(f"| `{member}` | `{type_name}` | {kind} |")
                lines.append("")

    return "\n".join(lines)


def main() -> int:
    if not SOURCES.is_dir():
        print(f"No API sources at {SOURCES}", file=sys.stderr)
        return 1

    total_listeners = 0
    sections = []
    for name, heading, blurb in EVENT_FILES:
        path = SOURCES / f"{name}.java"
        if not path.is_file():
            print(f"warning: {path.name} is missing, skipping", file=sys.stderr)
            continue
        total_listeners += len(LISTENER_RE.findall(path.read_text(encoding="utf-8")))
        sections.append(render_file(name, heading, blurb))

    header = f"""# Event reference

Every event Cultivation fires, grouped by the class that declares it.
**{total_listeners} listener hooks** across {len(sections)} subsystems.

> Generated from `api-sources/` by `tools/gen_events_reference.py`. Do not edit
> by hand — re-run the script instead. The prose in each entry is the javadoc on
> the event itself.

Read [events.md](events.md) first for the rules that apply to all of them: pre
vs post, threading, cancellation, and what a listener may safely do.

Every listener is registered the same way, once, from your plugin's `setup()`:

```java
CultivationEvents.onBreakthrough(event -> {{
    // event.ref(), event.player(), event.newRealm()
}});
```

---

"""

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(header + "\n\n---\n\n".join(sections) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT.relative_to(ROOT)} — {total_listeners} listeners, {len(sections)} sections.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
