#!/usr/bin/env python3
"""Builds a GitHub-wiki-ready copy of the docs into wiki/.

A GitHub wiki is a SEPARATE git repository with a flat page namespace, so the
docs cannot be pushed to it as they are:

  * pages are addressed by filename, with no `docs/` folder in the path
  * the landing page must be called `Home`
  * internal links are `[text](Page-Name)`, not `[text](docs/page.md)`
  * hyphens in a filename render as spaces in the page title

This script rewrites all of that mechanically, so `docs/` stays the single
source of truth and `wiki/` is disposable generated output - the same
arrangement as `gen_events_reference.py`.

Usage (from the repository root):

    python tools/build_wiki.py
    python tools/build_wiki.py --repo https://github.com/you/cultivation-api

Then push the result to the wiki repo. The wiki must already have one page
created through the web UI, otherwise there is nothing to clone:

    git clone https://github.com/you/cultivation-api.wiki.git /tmp/wiki
    cp wiki/*.md /tmp/wiki/
    cd /tmp/wiki && git add -A && git commit -m "Update docs" && git push
"""

from __future__ import annotations

import argparse
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "wiki"

# Default only. Override with --repo; it is used for links to files that stay in
# the code repository (the Java sources and the example addon), since a wiki
# cannot usefully hold either.
DEFAULT_REPO = "https://github.com/YOUR-USERNAME/cultivation-api"

# source path (relative to root) -> wiki page name (no extension).
# Order matters only for readability; the sidebar below drives display order.
PAGES: list[tuple[str, str, str]] = [
    # (source, wiki page name, sidebar label)
    ("README.md", "Home", "Home"),
    ("docs/getting-started.md", "Getting-Started", "Getting started"),
    ("docs/reading-state.md", "Reading-Player-State", "Reading player state"),
    ("docs/driving-progression.md", "Driving-Progression", "Driving progression"),
    ("docs/config-access.md", "Config-Access", "Config access"),
    ("docs/events.md", "Events", "Events"),
    ("docs/events-reference.md", "Event-Reference", "Event reference"),
    ("docs/registries.md", "Registries", "Registries"),
    # Added when the builder was found to be silently dropping this one -
    # docs/profiles.md existed and was linked from Home, but no wiki page
    # claimed it, so the link rendered dead on the published wiki.
    ("docs/profiles.md", "Profiles", "Profiles"),
    ("docs/store-benefits.md", "Store-Benefits", "Store benefits"),
    ("docs/ui.md", "UI-Integration", "UI integration"),
    ("docs/compatibility.md", "Compatibility", "Compatibility"),
    ("docs/progression-provider.md", "Progression-Provider", "Progression provider"),
    ("docs/theming.md", "Theming", "Theming"),
    ("docs/palettes.md", "Palettes", "Palettes"),
    ("docs/types.md", "Types", "Types"),
    ("docs/pitfalls.md", "Pitfalls", "Pitfalls"),
    ("examples/README.md", "Examples", "Examples"),
    ("AGENTS.md", "AI-Assistant-Guide", "AI assistant guide"),
]

# Paths that have no wiki equivalent and must point back at the code repository.
# Matched as a prefix against the link target.
REPO_PREFIXES = ("api-sources/", "examples/ExampleAddon", "tools/", "CLAUDE.md")


def build_link_map() -> dict[str, str]:
    """Every way a doc might refer to another doc -> its wiki page name."""
    mapping: dict[str, str] = {}
    for source, page, _label in PAGES:
        source_path = Path(source)
        # As written from the repo root: "docs/events.md"
        mapping[source] = page
        # As written from a sibling in docs/: "events.md"
        mapping[source_path.name] = page
        # As written from examples/ or docs/ with a parent hop: "../docs/events.md"
        mapping[f"../{source}"] = page
        # A directory link, e.g. "examples/" for examples/README.md
        if source_path.name == "README.md" and source_path.parent != Path("."):
            mapping[f"{source_path.parent.as_posix()}/"] = page
            mapping[source_path.parent.as_posix()] = page
    return mapping


def rewrite_links(text: str, link_map: dict[str, str], repo: str, page: str) -> tuple[str, list[str]]:
    """Rewrites relative markdown links for the flat wiki namespace."""
    unresolved: list[str] = []

    def replace(match: re.Match[str]) -> str:
        label, target = match.group(1), match.group(2)

        if target.startswith(("http://", "https://", "#", "mailto:")):
            return match.group(0)

        path, _, anchor = target.partition("#")

        # Files that stay in the code repo get an absolute link to it.
        if any(path.startswith(prefix) or path.lstrip("./").startswith(prefix)
               for prefix in REPO_PREFIXES):
            clean = path.lstrip("./")
            return f"[{label}]({repo}/blob/main/{clean})"

        wiki_page = link_map.get(path) or link_map.get(path.lstrip("./"))
        if wiki_page is None:
            unresolved.append(f"{page}: {target}")
            return match.group(0)

        # A wiki page links to a sibling by bare page name. Home is the one
        # page whose own anchor-only self-links should stay put.
        return f"[{label}]({wiki_page}{'#' + anchor if anchor else ''})"

    rewritten = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", replace, text)
    return rewritten, unresolved


def build_sidebar(repo: str) -> str:
    lines = ["### Cultivation API", ""]
    for _source, page, label in PAGES:
        if page == "Home":
            lines.append(f"[{label}]({page})")
        else:
            lines.append(f"- [{label}]({page})")
    lines += [
        "",
        "---",
        "",
        f"[API sources]({repo}/tree/main/api-sources/plugin/siren/API)",
        "",
        f"[Code repository]({repo})",
        "",
    ]
    return "\n".join(lines)


def build_footer(repo: str) -> str:
    return (
        f"Generated from the [cultivation-api]({repo}) repository's `docs/` — "
        "edit there, not here, or your changes are overwritten on the next build.\n"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=DEFAULT_REPO,
                        help="Code repository URL, for links to files the wiki cannot hold.")
    args = parser.parse_args()
    repo = args.repo.rstrip("/")

    if repo == DEFAULT_REPO:
        print(f"note: using placeholder repo URL {DEFAULT_REPO} — pass --repo to set the real one",
              file=sys.stderr)

    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    OUTPUT.mkdir(parents=True)

    link_map = build_link_map()
    all_unresolved: list[str] = []

    for source, page, _label in PAGES:
        source_path = ROOT / source
        if not source_path.is_file():
            print(f"warning: {source} is missing, skipping", file=sys.stderr)
            continue

        text = source_path.read_text(encoding="utf-8")
        text, unresolved = rewrite_links(text, link_map, repo, page)
        all_unresolved.extend(unresolved)

        (OUTPUT / f"{page}.md").write_text(text, encoding="utf-8")

    (OUTPUT / "_Sidebar.md").write_text(build_sidebar(repo), encoding="utf-8")
    (OUTPUT / "_Footer.md").write_text(build_footer(repo), encoding="utf-8")

    written = len(list(OUTPUT.glob("*.md")))
    print(f"Wrote {written} pages to {OUTPUT.relative_to(ROOT)}/ (including _Sidebar and _Footer).")

    if all_unresolved:
        print(f"\n{len(all_unresolved)} link(s) left untouched - no wiki page claims them:", file=sys.stderr)
        for item in all_unresolved:
            print(f"  {item}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
