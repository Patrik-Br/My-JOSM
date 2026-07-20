# BetterWorkspace – JOSM Plugin

Workspace tweaks for JOSM:
- Rotate the whole map view (data + imagery) via toolbar buttons.
- Arrange the docked side panels (**Windows → Arrange side panels...**), remembered across restarts.
- Adds a **"Select objects"** entry to the right-click menu of JOSM's built-in **Authors** panel
  (which otherwise only offers "Copy").

## License

GPLv3 — see [`LICENSE`](LICENSE). JOSM core is "GPLv2 or later," which makes GPLv3 a compatible
choice for plugins built against it.

Source was recovered by decompiling the previously jar-only plugin with
[CFR](https://www.benf.org/other/cfr/) on 2026-07-20, since no `.java` source existed in this repo
before then. `BetterWorkspacePlugin.java`, `RotatingProjection.java`, `ArrangePanelsDialog.java`
and `PanelReorderer.java` are that recovered original code (compiles and packages identically to
the original jar, `AuthorSelectHook.class` aside). `AuthorSelectHook.java` is new.

## Build

From within this `BetterWorkspace/` folder (needs JDK 17+):

1. Download `josm-tested.jar` from https://josm.openstreetmap.de/josm-tested.jar into `lib/` (gitignored, not committed).
2. **Windows:** `build.bat`
   **Linux/Mac:** `./build.sh`

This produces `BetterWorkspace.jar` in this folder — copy to JOSM's plugins folder and restart JOSM.

## The Authors-panel "Select objects" hook

JOSM's built-in Authors panel is `org.openstreetmap.josm.gui.dialogs.UserListDialog` — a core
class, not a plugin, and it exposes **no public extension point** for its right-click menu.
`AuthorSelectHook.java` reaches it via reflection:

- Gets the live instance via `MapFrame.getToggleDialog(UserListDialog.class)` (public API).
- Reflectively reads three **private** fields: `userTable` (`JTable`), `popupMenu` (`JPopupMenu`),
  `model` (package-private `UserTableModel`) — confirmed present with these exact names/types via
  `javap -p` against `josm-tested.jar`.
- Adds one `JMenuItem` directly to the live `popupMenu`.
- On click, delegates to the model's own `selectPrimitivesOwnedBy(int...)` — the same method the
  dialog's built-in "Select" button already calls — rather than reimplementing selection logic.
- Hooked from `BetterWorkspacePlugin.mapFrameInitialized`, with the same retry-timer pattern
  already used there for `PanelReorderer` (`UserListDialog` may not be ready the instant the map
  frame is created).

**This is inherently version-fragile** since it depends on private JOSM internals rather than a
documented API. If a future JOSM version renames/removes `userTable`, `popupMenu`, `model`, or
`selectPrimitivesOwnedBy`, the hook fails silently (caught, logged via `Logging.warn`, no crash) —
the Authors panel just goes back to only offering "Copy". If that happens, the fix is to re-run
`javap -p` against the new `josm-tested.jar` on `UserListDialog` and `UserListDialog$UserTableModel`
to find the new names.
