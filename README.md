<img src="docs/images/logo.png" alt="vogella Eclipse Themes logo" width="128" align="right">

# vogella Eclipse Themes

Additional themes for the Eclipse IDE, shipped as an installable p2 update site.
The themes are pure resource plugins: CSS and preference definitions only, no Java code.

## Installing

In Eclipse: *Help > Install New Software*, and add this update site:

```
https://vogellacompany.github.io/eclipse-themes/
```

Every theme is a feature of its own, so install one, several or all seven.
Then switch under *Preferences > General > Appearance* to the installed theme.

The site carries the newest build and nothing else, published from `main` by the [Release workflow](.github/workflows/release.yml).
Older versions are not supported: the previous build is dropped when a new one is published, so update rather than pin.

To install from your own build rather than the hosted site, see [Building](#building), then point *Add > Local* at

```
update-site/com.vogella.eclipse.themes.repository/target/repository
```

Note on preference based colors: when you switch to a theme, the theme writes its values into the affected preferences (editor colors, Java syntax highlighting, console colors), overwriting whatever was set before, including hand tuned settings.
The built-in dark theme behaves the same way.
If you want your own colors back, re-tune them under *Preferences > General > Appearance > Colors and Fonts* after switching.

The Linux stylesheets are the ones exercised by this repository's build and testing.
The Windows and macOS variants ship untested until someone runs them there.

## Themes

Every theme builds on a platform stylesheet of `org.eclipse.ui.themes`, the dark one or the light one, and recolors it through shared `ColorDefinition` tokens, so a theme change is a palette change, not a rewrite.

### GitHub Dark

GitHub's Primer dark palette: blue-black surfaces with the familiar blue accent and GitHub syntax colors.

![GitHub Dark](docs/images/github-dark.png)

### Dracula

The official Dracula palette on purple tinted surfaces with pink and purple accents.

![Dracula](docs/images/dracula.png)

### One Light

Atom's One Light palette: a near white editor on a soft grey chrome, with purple keywords, green strings and the blue accent.

![One Light](docs/images/one-light.png)

Known issue on GTK: One Light currently renders the whole IDE with a noticeably larger UI font than the dark themes, which is why fewer views fit in its screenshot above.
It is reproducible from a freshly started IDE and it flips the moment you switch between One Light and any of the dark themes, so it is the theme and not the workspace.
None of the stylesheets set a font size, so the cause is not in this repository's CSS; the working theory is the GTK theme reload that `ThemeEngine` triggers through `Display.setDarkThemePreferred` for a theme whose id does not contain `dark`.

### Nord

Calm arctic blues and frost accents on Polar Night surfaces, following the official Nord palette.

![Nord](docs/images/nord.png)

### Gruvbox Material

Sainnhe's Gruvbox Material: warm retro colours toned down on soft dark brown surfaces, with red keywords, yellow strings and green functions.

### AI Neon

Deep blue-violet surfaces with cyan links, tab titles and keylines, and magenta on errors and active links.

![AI Neon](docs/images/ai-neon.png)

### VS Code Dark

The VS Code Dark Modern look, including its tab styling and Dark+ syntax colors.

![VS Code Dark](docs/images/vscode-dark.png)

The six screenshots are the same workspace, the same file and the same maximized window, captured on GTK at 200% scaling.
The dark ones differ from each other only in the theme; One Light differs in layout too, for the reason noted above.
The orange row selection in the tree and the outline is the desktop accent color rather than the theme: GTK owns tree selection and no stylesheet can set it, see [styling-limits.md](docs/styling-limits.md).

## Building

```
mvn clean verify
```

Maven has to run on JDK 25 or newer, because the target platform is resolved against the
`JavaSE-25` execution environment configured in `pom.xml`.
The build is pomless Tycho (5.0.4) against the Eclipse 2026-06 release train target platform, with the modern CSS variant bundle and the update site resolved against 2026-09 (see [AGENTS.md](AGENTS.md)).
The resulting p2 repository lands in
`update-site/com.vogella.eclipse.themes.repository/target/repository/`.

Pushing to `main` runs that same build and publishes the result to the hosted update site, on the `gh-pages` branch.
The site carries one build at a time: `releng/update-composite-site.sh` writes the p2 composite metadata that points the root URL at it, and drops what came before.
A tag of the form `v*` additionally attaches the repository archive to a GitHub release.

The published artifacts are PGP signed with the vogella release key, held in the `MAVEN_GPG_KEY`
and `MAVEN_GPG_PASSPHRASE` organization secrets. Signing is off in a plain `mvn clean verify`; to
exercise it locally, point Tycho at an exported secret key:

```
mvn clean verify -Dgpg.skip=false -Dtycho.pgp.signer.bc.secretKeys=/path/to/signing-key.asc
```

with the passphrase in `MAVEN_GPG_PASSPHRASE`.

## Adding a theme

Copy an existing bundle under `plugins/` and replace the old bundle symbolic name everywhere
in the copy.
Copy a dark theme for a dark one and `com.vogella.eclipse.themes.onelight` for a light one:
the two differ in which platform stylesheet the `*_gtk.css`, `*_win.css` and `*_mac.css` files
import, and in whether `javadocElementsStyling.darkModeDefaultColors` is `true` or `false`.
That last part is the step that is easy to miss: the base stylesheets import the palette and
the preference sheets through `platform:/plugin/<bundle>/css/...` URIs, and every
`ColorDefinition` label is a `platform:/plugin/<bundle>?message=...` URI.
A copy that keeps the old name silently renders with the old theme's palette when that bundle
is installed, and renders black when it is not.

Then rename the theme id and the `%theme.*` key pair in `plugin.xml` and `plugin.properties`.
Set `isDarkTheme` on every `<theme>` element, which the platform reads from 2026-12 on.
Older releases still test whether the theme id contains `dark`, so a dark theme keeps `dark` in its id as well and a light theme must not have it.
Then rename the `.project` name and `Automatic-Module-Name`, replace the palette values and the
preference stylesheets (`*_preferences.css` and `*_jdt.css`, plus `vscode_tabs.css` if you
copied the VS Code theme), add a feature under `features/` and list it in the update site
`category.xml`.
Run `./releng/check-tokens.sh` afterwards, it verifies the token contract for every palette it
finds.
No pom changes are needed, the pomless aggregator picks up new directories automatically.

## License

Eclipse Public License 2.0, see [LICENSE](LICENSE).

## Commercial support

[vogella GmbH](https://vogella.com/services/) builds and maintains these themes and offers training and consulting around Eclipse, the Eclipse platform and its tooling.

If you use these themes in earnest and something looks wrong, saying so is welcome either way: the issues and pull requests here are open, and the commercial route exists for work that wants a schedule attached.
