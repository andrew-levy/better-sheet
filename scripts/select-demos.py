#!/usr/bin/env python3
"""Choose demo recordings from changed paths. Unknown runtime changes default to all."""
import argparse
import subprocess
from pathlib import Path

DEMOS = ("snapPoints", "chat", "contentFit", "stacked", "heavyContent", "scrollHandoff", "tray")
RULES = {
    "SheetSnapPoint.kt": ("snapPoints", "contentFit", "stacked", "tray"),
    "SheetGeometry.kt": ("snapPoints", "contentFit", "stacked", "tray"),
    "SheetState.kt": DEMOS,
    "SheetMotion.kt": DEMOS,
    "SheetNestedScroll.kt": ("chat", "heavyContent", "scrollHandoff", "tray"),
    "SheetEdgeOverscroll.kt": ("chat", "heavyContent", "scrollHandoff", "tray"),
    "BetterSheetStack.kt": ("stacked",),
    "SheetBehavior.kt": DEMOS,
    "SheetDrawing.kt": DEMOS,
    "SheetPresentation.kt": DEMOS,
    "BetterRootSheet.kt": DEMOS,
    "BetterSheet.kt": DEMOS,
}

def select(paths):
    selected = set()
    for path in paths:
        name = Path(path).name
        if path.startswith("docs/videos/") or path in ("README.md", "CHANGELOG.md"):
            continue
        if path.startswith("samples/findmy/"):
            continue  # This app is not the source for recorded README demos.
        if path.startswith("better-sheet/src/main/"):
            selected.update(RULES.get(name, DEMOS))
        elif path.startswith("samples/sample/") or path.startswith("scripts/") or path.startswith(".github/workflows/"):
            selected.update(DEMOS)
        elif path.startswith("better-sheet/") and ("/src/test/" in path or "/src/androidTest/" in path):
            continue
        elif path in ("settings.gradle.kts", "build.gradle.kts", "gradle.properties") or path.startswith("gradle/"):
            selected.update(DEMOS)
        elif path.startswith(".github/"):
            selected.update(DEMOS)
        else:
            # Changes without a runtime/demo impact (license, docs, etc.).
            continue
    return [name for name in DEMOS if name in selected]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", default="HEAD")
    args = parser.parse_args()
    changed = subprocess.check_output(
        ["git", "diff", "--name-only", "--diff-filter=ACMRTD", args.base, args.head],
        text=True,
    ).splitlines()
    print(" ".join(select(changed)))

if __name__ == "__main__":
    main()
