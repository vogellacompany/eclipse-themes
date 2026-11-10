#!/usr/bin/env python3
"""Writes each theme's TextMate theme for TM4E editors from its own Java colours.

TM4E reads no Eclipse colour preference, so this is the only way an editor built
on TM4E (the vogella AsciiDoc editor, YAML, TypeScript and more) follows the theme.
Run it after changing a theme's *_jdt.css or editor colours in *_preferences.css.
The result lands in the com.vogella.eclipse.themes.tm4e fragment.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# Each scope selector and the Java role it borrows its colour from, plus a style.
RULES = [
    # Any TM4E grammar, and the source blocks embedded in AsciiDoc.
    ('.comment', 'comment', ''),
    ('.string', 'string', ''),
    ('.constant.numeric', 'number', ''),
    ('.constant.language', 'keyword', ''),
    ('.constant.character', 'number', ''),
    ('.keyword', 'keyword', ''),
    ('.keyword.operator', 'operator', ''),
    ('.storage', 'keyword', ''),
    ('.entity.name.function', 'method', ''),
    ('.support.function', 'method', ''),
    ('.entity.name.type', 'type', ''),
    ('.entity.name.class', 'type', ''),
    ('.support.class', 'type', ''),
    ('.support.type', 'type', ''),
    ('.entity.name.tag', 'keyword', ''),
    ('.entity.other.attribute-name', 'field', ''),
    ('.support.type.property-name', 'field', ''),
    ('.variable', 'default', ''),
    ('.meta.annotation', 'annotation', ''),
    ('.storage.type.annotation', 'annotation', ''),
    ('.punctuation.definition.tag', 'operator', ''),
    ('.invalid', 'error', ''),
    ('.markup.inserted', 'string', ''),
    ('.markup.deleted', 'error', ''),
    ('.markup.changed', 'keyword', ''),
    # AsciiDoc, mapped like the WikiText block of *_preferences.css.
    ('.markup.heading', 'keyword', 'font-weight: bold;'),
    ('.markup.heading.block-attribute', 'annotation', ''),
    ('.markup.meta.attribute-list', 'annotation', ''),
    ('.markup.admonition', 'annotation', 'font-weight: bold;'),
    ('.markup.bold', 'default', 'font-weight: bold;'),
    ('.markup.strong', 'default', 'font-weight: bold;'),
    ('.markup.italic', 'default', 'font-style: italic;'),
    ('.markup.emphasis', 'default', 'font-style: italic;'),
    ('.markup.raw', 'string', ''),
    ('.markup.monospace', 'string', ''),
    ('.markup.block.listing', 'string', ''),
    ('.markup.block.literal', 'string', ''),
    ('.markup.block.passthrough', 'string', ''),
    ('.markup.block.front-matter', 'comment', ''),
    ('.markup.link', 'link', ''),
    ('.markup.other.url', 'link', ''),
    ('.markup.reference.xref', 'link', ''),
    ('.markup.other.anchor', 'type', ''),
    ('.markup.blockid', 'type', ''),
    ('.markup.other.footnote', 'type', ''),
    ('.markup.biblioref', 'type', ''),
    ('.markup.macro', 'method', ''),
    ('.markup.substitution.attribute-reference', 'field', ''),
    ('.support.constant.attribute-name', 'field', ''),
    ('.string.unquoted.attribute-value', 'string', ''),
    ('.markup.list.bullet', 'operator', ''),
    ('.markup.table.delimiter', 'operator', ''),
    ('.markup.table.cell.delimiter', 'operator', ''),
    ('.constant.other.symbol', 'operator', ''),
    ('.markup.character-reference', 'number', ''),
    ('.markup.mark', 'number', ''),
    ('.markup.highlight', 'number', ''),
    ('.callout', 'number', ''),
    ('.markup.todo', 'task', 'font-weight: bold;'),
]

ROLES = {
    'default': ('preferences', 'AbstractTextEditor.Color.Foreground'),
    'background': ('preferences', 'AbstractTextEditor.Color.Background'),
    'selection': ('preferences', 'AbstractTextEditor.Color.SelectionBackground'),
    'line': ('preferences', 'currentLineColor'),
    'comment': ('jdt', 'java_single_line_comment'),
    'string': ('jdt', 'java_string'),
    'keyword': ('jdt', 'java_keyword'),
    'operator': ('jdt', 'java_operator'),
    'task': ('jdt', 'java_comment_task_tag'),
    'link': ('jdt', 'java_doc_link'),
    'number': ('jdt', 'semanticHighlighting.number.color'),
    'method': ('jdt', 'semanticHighlighting.method.color'),
    'type': ('jdt', 'semanticHighlighting.class.color'),
    'field': ('jdt', 'semanticHighlighting.field.color'),
    'annotation': ('jdt', 'semanticHighlighting.annotation.color'),
    'error': ('palette', 'ERROR'),
}


def preferences(sheet):
    values = {}
    for key, value in re.findall(r"'([^'=]+)=(\d+,\d+,\d+)'", sheet.read_text()):
        values.setdefault(key, value)
    return values


def palette(sheet):
    values = {}
    for name, hexcolour in re.findall(
            r'ColorDefinition#com-vogella-themes-(\w+)\s*\{\s*color:\s*#([0-9A-Fa-f]{6})', sheet.read_text()):
        values[name] = ','.join(str(int(hexcolour[i:i + 2], 16)) for i in (0, 2, 4))
    return values


def generate(jdt):
    css = jdt.parent
    theme = jdt.name.removesuffix('_jdt.css')
    sources = {
        'jdt': preferences(jdt),
        'preferences': preferences(css / f'{theme}_preferences.css'),
        'palette': palette(css / f'{theme}_palette.css'),
    }

    def rgb(role):
        kind, key = ROLES[role]
        if key not in sources[kind]:
            sys.exit(f'{jdt}: no {key} in the {kind} sheet for role {role}')
        return 'rgb({})'.format(', '.join(sources[kind][key].split(',')))

    lines = [
        '/* Generated by releng/generate-tm4e-themes.py from this theme\'s Java colours.',
        ' * Edit the generator or the *_jdt.css and *_preferences.css sheets, not this file. */',
        '',
    ]
    for selector, role, style in RULES:
        lines.append(' '.join([selector, '{', f'color: {rgb(role)};', style, '}']).replace('  ', ' '))
    lines += [
        '',
        f".editor {{ background-color: {rgb('background')}; color: {rgb('default')}; }}",
        f".editor.lineHighlight {{ background-color: {rgb('line')}; }}",
        f".editor.selection {{ background-color: {rgb('selection')}; }}",
        '',
    ]
    target = ROOT / 'plugins/com.vogella.eclipse.themes.tm4e/themes' / f'{theme}.css'
    target.write_text('\n'.join(lines))
    print(target.relative_to(ROOT))


for jdt in sorted(ROOT.glob('plugins/*/css/*_jdt.css')):
    generate(jdt)
