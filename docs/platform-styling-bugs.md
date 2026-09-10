# Styling bugs found outside this repository

Problems that show up while running these themes but whose cause is in the platform or in
another bundle, so they cannot be fixed here.
Each entry says what breaks, where the cause is, and what this repository does about it.

## EGit paints the staging view white in every theme but its own dark one

`org.eclipse.egit.ui/plugin.xml` contributes two stylesheets:

```xml
<stylesheet uri="css/egit.css" />
<stylesheet uri="css/e4-dark_egit_prefstyle.css">
   <themeid refid="org.eclipse.e4.ui.css.theme.e4_dark"/>
</stylesheet>
```

`css/egit.css` has no `themeid`, so it applies to every theme, and it hardcodes white:

```css
#org-eclipse-egit-ui-StagingView.active Tree { background-color: #FFFFFF; }
.MPart ScrolledComposite StyledText.org-eclipse-egit-ui-CommitAndDiffComponent { background-color: #FFFFFF; }
```

The correction that cancels those two rules lives in `css/e4-dark_egit_prefstyle.css`, which is
bound to `org.eclipse.e4.ui.css.theme.e4_dark` alone.
So every third party dark theme gets large white areas in the Git Staging view and in the
commit and diff component, while the platform's own dark theme does not.

The comment in `egit.css` explains the intent: restore white because the light theme's grey
looked jarring next to the commit message editor.
The mistake is the scope, not the intent.
An unconditional stylesheet is the wrong place for a color that only holds for light themes.

Fix in EGit would be to move those rules into a light-theme-scoped stylesheet, or to use
`inherit` in the unconditional one and let each theme decide.

Worked around here in `plugins/com.vogella.eclipse.themes.common/css/egit.css`, by mirroring
EGit's own dark selectors, which are more specific than the white ones.

## EGit's dark preference colors are bound to one theme id

Same file, same cause, different mechanism.
`e4-dark_egit_prefstyle.css` carries the `IEclipsePreferences` block that sets
`UncommittedChangeForegroundColor`, `IgnoredResourceForegroundColor` and the diff colors.
Being bound to `e4_dark`, a third party theme falls back to the defaults declared in EGit's
`plugin.xml`, which are `COLOR_LIST_FOREGROUND` on `COLOR_LIST_BACKGROUND`.
On a dark theme that is black text on a near black background.

Measured in the Project Explorer under AI Neon: project labels rendered `#060402` on
`#10141F`, a contrast ratio of about 1.1:1, which is unreadable.

Worked around here by writing the same preference keys from each theme's `*_preferences.css`
under an own pseudo attribute, so EGit's own block still applies when it is active.

## Tree and Table selection colors come from GTK, not from CSS

Selecting a row in any tree paints it in the desktop accent color rather than anything the
theme controls.
Measured on Ubuntu: the selected row renders `#E95420`, the Yaru orange, inside an otherwise
near black theme.

There is no `swt-selection-*` property handler in `org.eclipse.e4.ui.css.swt`, and neither the
platform dark stylesheets nor any theme can set it.
`swt-header-color` and `swt-header-background-color` are the only related properties, and they
cover the column header only.

Not fixable from a theme, and not fixed by a dark GTK theme either: measured under
`GTK_THEME=Yaru-dark`, the selection is still `#E95420`, because the orange is Yaru's accent in
both variants.
`Tree.setBackground`, `TreeItem.setBackground` and their foreground counterparts all leave the
selected row untouched; `Tree.java` skips its own fill for selected cells on purpose.

There is one lever, outside CSS. `-Dorg.eclipse.swt.internal.gtk.cssFile=<file>` in
`eclipse.ini` is appended to SWT's own GTK style provider at `PRIORITY_APPLICATION`, which
outranks the theme, and this does work:

```css
treeview.view:selected, treeview.view:selected:focus,
list row:selected, iconview:selected { background-color: #2A4A8A; color: #FFFFFF; }
```

`COLOR_LIST_SELECTION` then reports the new colour too, so anything reading it stays consistent.
Redefining `@define-color theme_selected_bg_color` alone does NOT work, because Yaru resolves
its accent inside a compiled gresource.

The limitation is therefore not impossibility but scope: the property is read once during
`Device` init, so it lives in `eclipse.ini` and cannot follow a theme switch.
Clearing `SWT.SELECTED` in an `SWT.EraseItem` listener also suppresses GTK's paint, but that
needs every viewer in the IDE to opt in, so it is not a theming strategy.

What to ask the platform for: an SWT API for selection colours, or a GTK CSS provider owned by
the theme engine. SWT's own theming-fix CSS says colour information does not belong in it.

Recorded in [styling-limits.md](styling-limits.md) as a limit for theme authors.

## The ToolItem ':checked' pseudo-class is dead unless a system property is set

`ToolItemElement` offers a `:checked` pseudo-class, and `CSSPropertyBackgroundSWTHandler`
explicitly supports `ToolItem.setBackground`, so `ToolItem:checked { background-color: ... }`
looks like the way to style a toggled toolbar button.
It never matches.

```java
boolean dynamicEnabled = Boolean.getBoolean("org.eclipse.e4.ui.css.dynamic");

public ToolItemElement(ToolItem toolItem, CSSEngine engine) {
	super(toolItem, engine);
	if (!dynamicEnabled) {
		return;
	}
	toolItem.addSelectionListener(selectionListener);
}
```

`isSelected` starts `false` and is assigned only inside that listener, so without
`-Dorg.eclipse.e4.ui.css.dynamic=true` the pseudo-class is always false.
Nothing reports this: the rule parses, the selector is valid, and it silently matches nothing.
`WidgetElement` is gated on the same property.

The consequence for theme authors is that a toggled toolbar button is painted by GTK, so on a
light desktop theme it comes out light inside a dark IDE.
No shipped theme uses `:checked`, which is consistent with it never having worked.

Worked around here by setting the background on `ToolItem` unconditionally rather than on the
checked state.
Verified: the fill follows the palette and GTK does not paint over it.
GTK still draws its own border around a toggled item, which is left alone deliberately, since
once the fill matches the toolbar that outline is the only thing marking the item as toggled.

Why the workaround holds: `ToolItem.updateStyle` emits `button { background-image: none;
background-color: <rgba>; }` at `PRIORITY_APPLICATION` on the inner button's style context.
The selector carries no state qualifier, so it covers `:checked`, `:hover` and `:active`
alike, and provider priority beats the theme. It is stable, not a happy accident.
SWT never emits any `border-*` property, so the theme's `button:checked { border-color }`
survives, and the border is unreachable from SWT. It is reachable through the same
`gtk.cssFile` hook as the selection colours above.

One rule that follows, and it is absolute: never colour `ToolItem` differently from its
`ToolBar`. `ToolItem.updateStyle` returns early for `SWT.SEPARATOR`, so a separator can never
carry a background and always shows the `ToolBar` colour straight through the strip. The same
goes for any item the CSS engine did not reach.

A fix in the platform would be to maintain the selection listener regardless of the property,
or to drop the pseudo-class rather than ship one that cannot work by default.

## The dark theme styles the progress bar's container, never the bar

`e4-dark_globalstyle.css` names `ProgressMonitorPart` (line 34) and `ProgressIndicator`
(line 145, `background-color: #777`), and no `ProgressBar` anywhere.

The tempting assumption is that `Composite > *` (line 40) catches the bars inside.
It does not.
`WidgetElement.computeLocalName` returns the exact simple class name of the widget with no
walk up the superclasses:

```java
Class<?> clazz = widget.getClass();
return ClassUtils.getSimpleName(clazz);
```

Element selectors in this engine are exact match, so subclassing a `Composite` drops the
widget out of every `Composite` rule in the theme.
That is what the hand maintained list of `ProxyEntriesComposite`, `NonProxyHostsComposite`,
`DelayedFilterCheckboxTree` and the rest at lines 27 to 37 is for: every subclass has to be
enumerated by name, forever.

A JFace progress bar sits two subclasses deep and is therefore unreachable:

```
ProgressMonitorPart   (Composite)   ProgressMonitorPart.java:254
  ProgressIndicator   (Composite)   creates two bars in its constructor
    ProgressBar       determinate   ProgressIndicator.java:72
    ProgressBar       indeterminate ProgressIndicator.java:73
```

Every `ProgressBar` in the platform is unmatched by the dark theme except the Progress
view's, and that one only by accident, through the `ProgressInfoItem > *` wildcard at line
138.
The `#777` on the container is the strategy made visible: darken the box so the trough edges
and the stack layout gap do not glow white around a bar that keeps its own colours.

Measured under AI Neon on Windows: the SmartImport wizard bar rendered `#F0F0F0` with a
classic sunken `#A0A0A0` / `#FFFFFF` edge, `COLOR_BTNFACE` on an unthemed `msctls_progress32`,
across the full width of a `#0A0E17` dialog.
The p2 Installing Software wizard is the same widget chain and the same result.

The platform fix is a rule that names the leaf widget, or a selector model that matches
superclasses.

Handled here by naming `ProgressBar` and `ProgressIndicator > *` in
`plugins/com.vogella.eclipse.themes.common/css/structure.css`, which reaches the bar whatever
its containers are called.

## Whether GTK goes dark is decided by a substring match on the theme id

`ThemeEngine.setTheme` decides whether to put GTK itself into dark mode like this:

```java
boolean isDark = theme.getId().contains("dark"); //$NON-NLS-1$
display.setDarkThemePreferred(isDark);
```

`org.eclipse.e4.ui.css.swt.theme` has no dark or light attribute in its schema, so the id is
the only signal, and it is matched as a literal substring.
A dark theme whose id does not happen to contain the five characters `dark` leaves GTK light,
and every state GTK owns rather than CSS stays light with it: toggle button fills, native
scrollbars, native dialogs.

This repository shipped exactly that bug.
`com.vogella.eclipse.themes.neon` failed the test while
`com.vogella.eclipse.themes.vscodedark` passed it by accident, which is why the two themes
behaved differently on the same widgets.
Fixed here by renaming the id to `com.vogella.eclipse.themes.neondark`.

Fixed upstream in
[eclipse.platform.ui#4354](https://github.com/eclipse-platform/eclipse.platform.ui/pull/4354)
for 2026-12: the `<theme>` element takes an `isDarkTheme` attribute and `ITheme.isDark()`
returns it, with the substring match kept only for themes that do not set it.
Every theme here declares it, and keeps `dark` in the id for the releases before that.

