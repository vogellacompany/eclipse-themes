#!/usr/bin/env bash
# Verifies what the build cannot: the '#com-vogella-themes-*' token contract, that
# only the modern bundle uses syntax Batik rejects, that every stylesheet is loaded,
# and that the preference sheets of all themes set the same keys.
# Each of these fails silently at runtime: a missing token renders black, one
# unknown construct drops a whole sheet on the old engine, and a missing key keeps
# the light default or the previous theme's colour.
set -euo pipefail
export LC_ALL=C
cd "$(dirname "$0")/.."

names() { sed -n "s/.*#com-vogella-themes-\([A-Za-z_0-9]*\).*/\1/p" | sort -u; }

palettes=(plugins/*/css/*_palette.css)
# Every stylesheet that is not a palette consumes tokens rather than declaring them.
consumers=$(find plugins -name '*.css' ! -name '*_palette.css' | sort)

used=$(grep -oh "'#com-vogella-themes-[A-Za-z_0-9]*'" $consumers | names)

status=0
for palette in "${palettes[@]}"; do
	theme=${palette#plugins/}
	theme=${theme%%/*}

	defined=$(grep -h "^ColorDefinition#com-vogella-themes-" "$palette" | names)
	# Only tokens listed in ThemesExtension reach the theme engine.
	registered=$(sed -n '/ThemesExtension/,/}/p' "$palette" \
		| grep -o "'#com-vogella-themes-[A-Za-z_0-9]*'" | names)

	report() {
		local label=$1 list=$2
		[ -z "$list" ] && return 0
		echo "$theme: $label" >&2
		printf '  %s\n' $list >&2
		status=1
	}

	report "used by a stylesheet but defined by no ColorDefinition" \
		"$(comm -23 <(printf '%s\n' "$used") <(printf '%s\n' "$defined"))"
	report "defined but not listed in ThemesExtension, so never registered" \
		"$(comm -23 <(printf '%s\n' "$defined") <(printf '%s\n' "$registered"))"
	report "listed in ThemesExtension but defined by no ColorDefinition" \
		"$(comm -13 <(printf '%s\n' "$defined") <(printf '%s\n' "$registered"))"
done

fail() {
	echo "$1" >&2
	printf '  %s\n' "${@:2}" >&2
	status=1
}

strip_comments() { perl -0pe 's{/\*.*?\*/}{}gs' "$1"; }

batik=()
for sheet in $consumers "${palettes[@]}"; do
	case $sheet in plugins/com.vogella.eclipse.themes.modern/*) continue ;; esac
	if strip_comments "$sheet" | grep -qE '@media|@supports|:[a-z-]+\('; then
		batik+=("$sheet")
	fi
done
[ ${#batik[@]} -eq 0 ] ||
	fail "uses @media, @supports or a functional pseudo-class outside the modern bundle" "${batik[@]}"

referenced=$(cat plugins/*/plugin.xml plugins/*/css/*.css \
	| grep -oE '(platform:/plugin/[^/"]+/|uri=")css/[A-Za-z0-9_.-]+\.css' \
	| sed -E 's#^platform:/plugin/([^/]+)/#\1 #; s#^uri="#- #' | sort -u)
orphans=()
for sheet in plugins/*/css/*.css; do
	bundle=${sheet#plugins/}
	bundle=${bundle%%/*}
	file=css/${sheet##*/}
	if ! grep -qxF "$bundle $file" <<<"$referenced" \
		&& ! grep -q "\"$file\"" "plugins/$bundle/plugin.xml" 2>/dev/null; then
		orphans+=("$sheet")
	fi
done
[ ${#orphans[@]} -eq 0 ] || fail "loaded by no plugin.xml and no @import" "${orphans[@]}"

# Prints 'node|key' for every preference a sheet sets.
pref_keys() {
	strip_comments "$1" | awk '
		/^IEclipsePreferences#/ { node = $1; sub(/^IEclipsePreferences#/, "", node); sub(/:.*/, "", node) }
		match($0, /^[ \t]*\x27[^=\x27]+=/) { key = substr($0, RSTART, RLENGTH - 1); sub(/^[ \t]*\x27/, "", key); print node "|" key }'
}

# Keys only some themes set on purpose, as theme:node|key.
optional_keys='neon:org-eclipse-ui-workbench|DECORATIONS_COLOR vscode:org-eclipse-ui-workbench|perspectiveSwitcherSide'

for kind in preferences jdt; do
	sheets=(plugins/*/css/*_"$kind".css)
	union=$(for sheet in "${sheets[@]}"; do pref_keys "$sheet"; done | sort -u)
	for sheet in "${sheets[@]}"; do
		theme=${sheet##*/}
		theme=${theme%_"$kind".css}
		keys=$(pref_keys "$sheet")
		dups=$(sort <<<"$keys" | uniq -d)
		[ -z "$dups" ] || fail "$sheet: key set twice in one node, the later value wins" $dups
		missing=$(comm -23 <(printf '%s\n' "$union") <(sort -u <<<"$keys") \
			| grep -vxF -f <(tr ' ' '\n' <<<"$optional_keys" | sed -n "s/^[a-z]*://p") || true)
		[ -z "$missing" ] || fail "$sheet: set by another theme but not here" $missing
		for entry in $optional_keys; do
			[ "${entry%%:*}" = "$theme" ] && continue
			! grep -qxF "${entry#*:}" <<<"$keys" ||
				fail "$sheet: listed as optional for ${entry%%:*} only" "${entry#*:}"
		done
	done
done

if [ $status -ne 0 ]; then
	exit 1
fi
echo "check-tokens: $(printf '%s\n' "$used" | grep -c .) tokens, consistent across ${#palettes[@]} palettes"
