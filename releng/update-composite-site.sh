#!/usr/bin/env bash
#
# Regenerates the p2 composite metadata at the root of the update site from the
# directories present under <site>/releases, optionally dropping every release
# but one first.
#
# Usage: releng/update-composite-site.sh [--only <version>] <site-directory>

set -euo pipefail

usage="Usage: $0 [--only <version>] <site-directory>"
only=

while [ $# -gt 0 ]; do
	case $1 in
	--only)
		only=${2:?$usage}
		shift 2
		;;
	-*)
		echo "$usage" >&2
		exit 1
		;;
	*)
		[ -n "${site:-}" ] && { echo "$usage" >&2; exit 1; }
		site=$1
		shift
		;;
	esac
done

site=${site:?$usage}
releases_dir="$site/releases"

if [ ! -d "$releases_dir" ]; then
	echo "No releases directory in $site" >&2
	exit 1
fi

mapfile -t versions < <(find "$releases_dir" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -V)

if [ ${#versions[@]} -eq 0 ]; then
	echo "No releases below $releases_dir" >&2
	exit 1
fi

if [ -n "$only" ]; then
	found=
	for version in "${versions[@]}"; do
		if [ "$version" = "$only" ]; then
			found=yes
		fi
	done
	if [ -z "$found" ]; then
		echo "No release '$only' below $releases_dir, refusing to delete the others" >&2
		exit 1
	fi
	for version in "${versions[@]}"; do
		if [ "$version" != "$only" ]; then
			echo "Dropping release $version"
			rm -rf "${releases_dir:?}/$version"
		fi
	done
	versions=("$only")
fi

# p2 expects milliseconds; %3N is not honoured by every coreutils implementation
timestamp=$(( $(date +%s) * 1000 ))

write_composite() {
	local file=$1 processing_instruction=$2 type=$3 name=$4
	{
		printf "<?xml version='1.0' encoding='UTF-8'?>\n"
		printf "<?%s version='1.0.0'?>\n" "$processing_instruction"
		printf "<repository name='%s' type='%s' version='1.0.0'>\n" "$name" "$type"
		printf "  <properties size='2'>\n"
		printf "    <property name='p2.timestamp' value='%s'/>\n" "$timestamp"
		printf "    <property name='p2.atomic.composite.loading' value='true'/>\n"
		printf "  </properties>\n"
		printf "  <children size='%s'>\n" "${#versions[@]}"
		for version in "${versions[@]}"; do
			printf "    <child location='releases/%s'/>\n" "$version"
		done
		printf "  </children>\n"
		printf "</repository>\n"
	} > "$file"
}

write_composite "$site/compositeContent.xml" compositeMetadataRepository \
	org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository \
	'vogella Eclipse Themes'
write_composite "$site/compositeArtifacts.xml" compositeArtifactRepository \
	org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository \
	'vogella Eclipse Themes'

cat > "$site/p2.index" <<'EOF'
version=1
metadata.repository.factory.order=compositeContent.xml,!
artifact.repository.factory.order=compositeArtifacts.xml,!
EOF

# keeps GitHub Pages from running the content through Jekyll
touch "$site/.nojekyll"

base_url=https://vogellacompany.github.io/eclipse-themes
latest=${versions[${#versions[@]}-1]}
{
	cat <<EOF
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>vogella Eclipse Themes Update Site</title>
<link rel="icon" href="logo.png">
<style>
  :root {
    --bg: #07060f;
    --panel: #120f24;
    --border: #2c2255;
    --text: #d9d6f2;
    --muted: #8f8bb3;
    --orange: #f7941e;
    --cyan: #22e4ff;
    --magenta: #ff2bd6;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    min-height: 100vh;
    background:
      radial-gradient(60rem 30rem at 15% -10%, #2c225588, transparent 70%),
      radial-gradient(40rem 25rem at 110% 20%, #ff2bd622, transparent 70%),
      linear-gradient(#ffffff05 1px, transparent 1px) 0 0 / 100% 2.5rem,
      linear-gradient(90deg, #ffffff05 1px, transparent 1px) 0 0 / 2.5rem 100%,
      var(--bg);
    color: var(--text);
    font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
    line-height: 1.6;
  }
  main { max-width: 46rem; margin: 0 auto; padding: 4rem 1rem 3rem; }
  header { display: flex; align-items: center; gap: 1.5rem; margin-bottom: 2rem; }
  header img { width: 6rem; height: 6rem; border-radius: 1.2rem; filter: drop-shadow(0 0 1.2rem #f7941e88); }
  h1 {
    margin: 0;
    font-size: clamp(2rem, 6vw, 3.2rem);
    line-height: 1.1;
    color: #fff;
    text-shadow: 0 0 .4rem var(--orange), 0 0 1.6rem #f7941e99, 0 0 3rem #ff2bd655;
  }
  .tagline { margin: .4rem 0 0; color: var(--muted); }
  h2 {
    font-size: .8rem;
    letter-spacing: .2em;
    text-transform: uppercase;
    color: var(--cyan);
    text-shadow: 0 0 .6rem #22e4ff99;
    margin: 2.5rem 0 .8rem;
  }
  .url {
    display: flex;
    align-items: center;
    gap: .8rem;
    padding: .9rem 1rem;
    background: var(--panel);
    border: 1px solid var(--cyan);
    border-radius: .6rem;
    box-shadow: 0 0 1rem #22e4ff44, inset 0 0 1rem #22e4ff14;
  }
  .url code { flex: 1; font-size: 1.05rem; color: #fff; overflow-wrap: anywhere; }
  button {
    font: inherit;
    font-size: .85rem;
    padding: .4rem .9rem;
    color: var(--bg);
    background: var(--orange);
    border: 0;
    border-radius: .4rem;
    cursor: pointer;
    box-shadow: 0 0 .8rem #f7941e99;
  }
  button:hover { box-shadow: 0 0 1.4rem var(--orange); }
  ol { padding-left: 1.2rem; }
  li { margin: .3rem 0; }
  em { color: #fff; font-style: normal; }
  .version {
    display: inline-block;
    padding: .2rem .7rem;
    border: 1px solid var(--magenta);
    border-radius: 999px;
    color: #fff;
    font-family: ui-monospace, monospace;
    box-shadow: 0 0 .8rem #ff2bd655;
  }
  .note { color: var(--muted); font-size: .9rem; }
  a { color: var(--cyan); }
  @media (max-width: 32rem) {
    header { flex-direction: column; align-items: flex-start; gap: 1rem; }
    header img { width: 4.5rem; height: 4.5rem; }
    .url code { font-size: .9rem; }
  }
  footer { margin-top: 3rem; padding-top: 1.2rem; border-top: 1px solid var(--border); color: var(--muted); font-size: .9rem; }
</style>
</head>
<body>
<main>
<header>
  <img src="logo.png" alt="">
  <div>
    <h1>vogella Eclipse Themes</h1>
    <p class="tagline">Seven themes for the Eclipse IDE: GitHub Dark, Dracula, One Light, Nord, Gruvbox Material, AI Neon and VS Code Dark.</p>
  </div>
</header>

<h2>Update site</h2>
<div class="url">
  <code id="site-url">$base_url/</code>
  <button type="button" onclick="navigator.clipboard.writeText(document.getElementById('site-url').textContent).then(() => { this.textContent = 'Copied'; })">Copy</button>
</div>

<h2>Install</h2>
<ol>
  <li>In Eclipse, open <em>Help &gt; Install New Software...</em></li>
  <li>Paste the update site URL into <em>Work with</em> and press Enter.</li>
  <li>Select <em>vogella Eclipse Themes</em>, finish the wizard and restart.</li>
  <li>Pick a theme in <em>Window &gt; Preferences &gt; General &gt; Appearance</em>.</li>
</ol>

<h2>Current build</h2>
<p><span class="version">$latest</span></p>
<p class="note">This site carries the newest build and nothing else. The previous build is dropped when a new one is published, so update rather than pin.</p>

<footer>Sources and documentation: <a href="https://github.com/vogellacompany/eclipse-themes">github.com/vogellacompany/eclipse-themes</a></footer>
</main>
</body>
</html>
EOF
} > "$site/index.html"

echo "Composite site updated with ${#versions[@]} release(s): ${versions[*]}"
