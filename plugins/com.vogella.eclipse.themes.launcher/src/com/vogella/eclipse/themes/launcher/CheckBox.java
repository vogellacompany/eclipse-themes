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

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A check box with a label, painted so its box stays visible on the dark palette.
 */
class CheckBox extends Canvas {

	private static final int BOX = 16;

	private final String label;
	private Palette palette;
	private boolean selected;
	private boolean focusByMouse;

	CheckBox(Composite parent, String label, Palette palette) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.label = label;
		this.palette = palette;
		setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		Accessibility.describe(this, () -> label, ACC.ROLE_CHECKBUTTON);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseDown, e -> focusByMouse = true);
		addListener(SWT.MouseUp, e -> flip());
		addListener(SWT.FocusIn, e -> redraw());
		addListener(SWT.FocusOut, e -> {
			focusByMouse = false;
			redraw();
		});
		addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
				e.doit = true;
			}
		});
		addListener(SWT.KeyDown, e -> {
			if (e.character == ' ') {
				flip();
			}
		});
	}

	private void flip() {
		selected = !selected;
		redraw();
		notifyListeners(SWT.Selection, new Event());
	}

	boolean getSelection() {
		return selected;
	}

	void setSelection(boolean selected) {
		this.selected = selected;
		redraw();
	}

	void setPalette(Palette palette) {
		this.palette = palette;
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		GC gc = new GC(this);
		Point extent = gc.textExtent(label);
		gc.dispose();
		return new Point(BOX + 10 + extent.x + 4, Math.max(BOX, extent.y) + 6);
	}

	private void paint(Event e) {
		GC gc = e.gc;
		Rectangle area = getClientArea();
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);
		gc.setBackground(getParent().getBackground());
		gc.fillRectangle(area);
		int y = (area.height - BOX) / 2;
		if (selected) {
			gc.setBackground(palette.primary());
			gc.fillRoundRectangle(0, y, BOX, BOX, 8, 8);
			gc.setForeground(palette.onAccent());
			gc.setLineWidth(2);
			gc.setLineCap(SWT.CAP_ROUND);
			gc.setLineJoin(SWT.JOIN_ROUND);
			Path check = new Path(getDisplay());
			check.moveTo(4, y + 8.5f);
			check.lineTo(7, y + 11.5f);
			check.lineTo(12, y + 5f);
			gc.drawPath(check);
			check.dispose();
		} else {
			gc.setBackground(palette.secondary());
			gc.fillRoundRectangle(0, y, BOX, BOX, 8, 8);
			gc.setBackground(palette.window());
			gc.fillRoundRectangle(1, y + 1, BOX - 2, BOX - 2, 6, 6);
		}
		if (isFocusControl() && !focusByMouse) {
			gc.setForeground(palette.accent());
			gc.setLineWidth(2);
			gc.drawRoundRectangle(-1 + 1, y - 2 + 1, BOX + 2, BOX + 2, 10, 10);
		}
		gc.setForeground(palette.secondary());
		Point extent = gc.textExtent(label);
		gc.drawString(label, BOX + 10, (area.height - extent.y) / 2, true);
	}
}
