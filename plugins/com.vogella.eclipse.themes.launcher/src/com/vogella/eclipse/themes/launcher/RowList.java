/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.vogella.eclipse.themes.launcher;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A keyboard driven list of two line rows. Selection fires {@link SWT#Selection}, a double click or Enter fires
 * {@link SWT#DefaultSelection}.
 */
class RowList extends Canvas {

	record Row(String title, String detail, String meta, boolean metaIsWarning, String tooltip, Object data) {
	}

	private static final int ROW_HEIGHT = 50;
	private static final int GAP = 2;

	private List<Row> rows = List.of();
	private final Image emptyIcon;
	private Palette palette;
	private String emptyTitle = "";
	private String emptyDetail = "";
	private int selection = -1;
	private int hover = -1;
	private int scroll;

	RowList(Composite parent, Image emptyIcon, Palette palette) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.emptyIcon = emptyIcon;
		this.palette = palette;
		Accessibility.describe(this, () -> selection >= 0 ? rows.get(selection).title() + ", " + rows.get(selection).detail() //$NON-NLS-1$
				: emptyTitle, ACC.ROLE_LIST);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.Resize, e -> updateScrollBar());
		addListener(SWT.MouseMove, e -> setHover(rowAt(e.y)));
		addListener(SWT.MouseExit, e -> setHover(-1));
		addListener(SWT.MouseDown, e -> {
			forceFocus();
			int index = rowAt(e.y);
			if (index >= 0) {
				select(index, true);
			}
		});
		addListener(SWT.MouseDoubleClick, e -> {
			if (rowAt(e.y) >= 0) {
				notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		addListener(SWT.MouseVerticalWheel, e -> scrollBy(-e.count * ROW_HEIGHT / 3));
		addListener(SWT.FocusIn, e -> redraw());
		addListener(SWT.FocusOut, e -> redraw());
		addListener(SWT.Traverse, e -> {
			switch (e.detail) {
			case SWT.TRAVERSE_TAB_NEXT, SWT.TRAVERSE_TAB_PREVIOUS -> e.doit = true;
			case SWT.TRAVERSE_RETURN -> {
				if (selection >= 0) {
					e.doit = false;
					e.detail = SWT.TRAVERSE_NONE;
					notifyListeners(SWT.DefaultSelection, new Event());
				}
			}
			default -> {
				// keep the platform behaviour
			}
			}
		});
		addListener(SWT.KeyDown, e -> {
			switch (e.keyCode) {
			case SWT.ARROW_DOWN -> select(Math.min(rows.size() - 1, selection + 1), true);
			case SWT.ARROW_UP -> select(Math.max(0, selection - 1), true);
			case SWT.HOME -> select(0, true);
			case SWT.END -> select(rows.size() - 1, true);
			default -> {
				// other keys are not ours
			}
			}
		});
	}

	void setRows(List<Row> rows) {
		this.rows = List.copyOf(rows);
		selection = Math.min(selection, rows.size() - 1);
		updateScrollBar();
		redraw();
	}

	void setEmptyText(String title, String detail) {
		emptyTitle = title;
		emptyDetail = detail;
		redraw();
	}

	void setPalette(Palette palette) {
		this.palette = palette;
		redraw();
	}

	Row getSelectedRow() {
		return selection >= 0 && selection < rows.size() ? rows.get(selection) : null;
	}

	void select(int index, boolean notify) {
		if (index < 0 || index >= rows.size()) {
			return;
		}
		selection = index;
		reveal(index);
		redraw();
		if (notify) {
			notifyListeners(SWT.Selection, new Event());
		}
	}

	/** Moves the selection to the row holding {@code data}, without notifying. */
	void selectData(Object data) {
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).data().equals(data)) {
				select(i, false);
				return;
			}
		}
		selection = -1;
		redraw();
	}

	private void setHover(int index) {
		if (hover != index) {
			hover = index;
			setToolTipText(index >= 0 ? rows.get(index).tooltip() : null);
			redraw();
		}
	}

	private int rowAt(int y) {
		int index = (y + scroll) / (ROW_HEIGHT + GAP);
		return index >= 0 && index < rows.size() && (y + scroll) % (ROW_HEIGHT + GAP) < ROW_HEIGHT ? index : -1;
	}

	private void reveal(int index) {
		int top = index * (ROW_HEIGHT + GAP);
		int height = getClientArea().height;
		if (top < scroll) {
			scrollTo(top);
		} else if (top + ROW_HEIGHT > scroll + height) {
			scrollTo(top + ROW_HEIGHT - height);
		}
	}

	private void scrollBy(int delta) {
		scrollTo(scroll + delta);
	}

	private void scrollTo(int value) {
		int max = Math.max(0, contentHeight() - getClientArea().height);
		scroll = Math.max(0, Math.min(max, value));
		redraw();
	}

	private int contentHeight() {
		return rows.size() * (ROW_HEIGHT + GAP) - GAP;
	}

	private void updateScrollBar() {
		scrollTo(scroll);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(wHint == SWT.DEFAULT ? 400 : wHint, hHint == SWT.DEFAULT ? 3 * ROW_HEIGHT : hHint);
	}

	private void paint(Event e) {
		GC gc = e.gc;
		Rectangle area = getClientArea();
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);
		gc.setBackground(palette.window());
		gc.fillRectangle(area);
		if (rows.isEmpty()) {
			paintEmpty(gc, area);
			return;
		}
		int content = contentHeight();
		int rowWidth = content > area.height ? area.width - 8 : area.width;
		for (int i = 0; i < rows.size(); i++) {
			int y = i * (ROW_HEIGHT + GAP) - scroll;
			if (y + ROW_HEIGHT < 0 || y > area.height) {
				continue;
			}
			paintRow(gc, rows.get(i), y, rowWidth, i == selection, i == hover);
		}
		if (content > area.height) {
			// thin overlay thumb instead of a native scroll bar, whose gutter the palette cannot colour
			int thumb = Math.max(24, area.height * area.height / content);
			int top = (area.height - thumb) * scroll / (content - area.height);
			gc.setBackground(palette.border());
			gc.fillRoundRectangle(area.width - 4, top, 4, thumb, 4, 4);
		}
	}

	private void paintRow(GC gc, Row row, int y, int width, boolean selected, boolean hovered) {
		if (selected || hovered) {
			gc.setBackground(selected ? palette.selection() : palette.hover());
			gc.fillRoundRectangle(0, y, width, ROW_HEIGHT, 14, 14);
		}
		if (selected && isFocusControl()) {
			gc.setForeground(palette.accent());
			gc.setLineWidth(2);
			gc.drawRoundRectangle(1, y + 1, width - 2, ROW_HEIGHT - 2, 14, 14);
		}
		int x = 14;
		int right = width - 16;
		if (row.meta() != null && !row.meta().isEmpty()) {
			gc.setFont(getFont());
			Point meta = gc.textExtent(row.meta());
			gc.setForeground(row.metaIsWarning() ? palette.warning() : palette.secondary());
			gc.drawString(row.meta(), right - meta.x, y + (ROW_HEIGHT - meta.y) / 2, true);
			right -= meta.x + 20;
		}
		int textWidth = Math.max(0, right - x);
		gc.setFont(Fonts.bold());
		Point titleExtent = gc.textExtent("Ag");
		gc.setFont(getFont());
		Point detailExtent = gc.textExtent("Ag");
		int top = y + (ROW_HEIGHT - titleExtent.y - detailExtent.y - 2) / 2;
		gc.setFont(Fonts.bold());
		gc.setForeground(palette.text());
		gc.drawString(fit(gc, row.title(), textWidth, false), x, top, true);
		gc.setFont(getFont());
		gc.setForeground(palette.secondary());
		gc.drawString(fit(gc, row.detail(), textWidth, true), x, top + titleExtent.y + 2, true);
	}

	private void paintEmpty(GC gc, Rectangle area) {
		List<String> lines = wrap(gc, emptyDetail, Math.min(380, area.width - 32));
		gc.setFont(Fonts.bold());
		Point title = gc.textExtent(emptyTitle);
		gc.setFont(getFont());
		int lineHeight = gc.textExtent("Ag").y + 3;
		int iconHeight = emptyIcon != null ? emptyIcon.getBounds().height + 14 : 0;
		int y = Math.max(8, (area.height - iconHeight - title.y - 8 - lines.size() * lineHeight) / 2);
		if (emptyIcon != null) {
			Rectangle b = emptyIcon.getBounds();
			gc.drawImage(emptyIcon, (area.width - b.width) / 2, y);
			y += iconHeight;
		}
		gc.setFont(Fonts.bold());
		gc.setForeground(palette.text());
		gc.drawString(emptyTitle, (area.width - title.x) / 2, y, true);
		y += title.y + 8;
		gc.setFont(getFont());
		gc.setForeground(palette.secondary());
		for (String line : lines) {
			gc.drawString(line, (area.width - gc.textExtent(line).x) / 2, y, true);
			y += lineHeight;
		}
	}

	private static List<String> wrap(GC gc, String text, int width) {
		List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && gc.textExtent(candidate).x > width) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}

	/** Shortens text to the width, eliding in the middle for paths so both ends stay readable. */
	static String fit(GC gc, String text, int width, boolean middle) {
		if (text == null || gc.textExtent(text).x <= width) {
			return text == null ? "" : text;
		}
		String ellipsis = "…";
		int keep = text.length();
		while (keep > 1) {
			keep--;
			String candidate = middle
					? text.substring(0, (keep + 1) / 2) + ellipsis + text.substring(text.length() - keep / 2)
					: text.substring(0, keep) + ellipsis;
			if (gc.textExtent(candidate).x <= width) {
				return candidate;
			}
		}
		return ellipsis;
	}
}
