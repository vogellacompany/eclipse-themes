#!/usr/bin/env bash
#
# Regenerates the p2 composite metadata at the root of the update site from the
# directories present under <site>/releases, optionally dropping older releases
# first.
#
# Usage: releng/update-composite-site.sh [--keep <n>] [--only <version>] <site-directory>

set -euo pipefail

usage="Usage: $0 [--keep <n>] [--only <version>] <site-directory>"
keep=
only=

while [ $# -gt 0 ]; do
	case $1 in
	--keep)
		keep=${2:?$usage}
		case $keep in
		'' | *[!0-9]* | 0) echo "--keep needs a positive number, got '$keep'" >&2; exit 1 ;;
		esac
		shift 2
		;;
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

# oldest first, so that the newest release is the last child p2 sees
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

if [ -n "$keep" ] && [ ${#versions[@]} -gt "$keep" ]; then
	drop=$(( ${#versions[@]} - keep ))
	for version in "${versions[@]:0:$drop}"; do
		echo "Dropping release $version"
		rm -rf "${releases_dir:?}/$version"
	done
	versions=("${versions[@]:$drop}")
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
{
	cat <<EOF
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>vogella Eclipse Themes Update Site</title>
<style>
  body { font-family: system-ui, sans-serif; max-width: 46rem; margin: 3rem auto; padding: 0 1rem; line-height: 1.5; }
  code { background: #8881; padding: 0.1rem 0.3rem; border-radius: 0.2rem; }
</style>
</head>
<body>
<h1>vogella Eclipse Themes</h1>
<p>Seven themes for the Eclipse IDE: GitHub Dark, Dracula, One Light, Nord, Gruvbox Material, AI Neon and VS Code Dark.</p>
<p>Install them in Eclipse with <em>Help &gt; Install New Software</em>, using the update site</p>
<p><code>$base_url/</code></p>
<p>One feature installs all seven themes.</p>
<p>This site carries the newest build and nothing else. Older versions are not supported: the previous build is dropped when a new one is published, so update rather than pin.</p>
<p>Currently published:</p>
<ul>
EOF
	for version in "${versions[@]}"; do
		printf '  <li><code>%s</code></li>\n' "$version"
	done
	cat <<EOF
</ul>
<p>Sources and documentation: <a href="https://github.com/vogellacompany/eclipse.themes">github.com/vogellacompany/eclipse.themes</a></p>
</body>
</html>
EOF
} > "$site/index.html"

echo "Composite site updated with ${#versions[@]} release(s): ${versions[*]}"
