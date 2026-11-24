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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A text button painted from the palette, because GTK ignores a native button's background colour.
 */
class ActionButton extends Canvas {

	private final String label;
	private final boolean primary;
	private Palette palette;
	private boolean hovered;
	private boolean pressed;
	private boolean focusByMouse;

	ActionButton(Composite parent, String label, boolean primary, Palette palette) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.label = label;
		this.primary = primary;
		this.palette = palette;
		setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		Accessibility.describe(this, () -> label, ACC.ROLE_PUSHBUTTON);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseEnter, e -> {
			hovered = true;
			redraw();
		});
		addListener(SWT.MouseExit, e -> {
			hovered = false;
			pressed = false;
			redraw();
		});
		addListener(SWT.MouseDown, e -> {
			focusByMouse = true;
			pressed = true;
			redraw();
		});
		addListener(SWT.MouseUp, e -> {
			boolean fire = pressed && getClientArea().contains(e.x, e.y);
			pressed = false;
			redraw();
			if (fire) {
				press();
			}
		});
		addListener(SWT.FocusIn, e -> redraw());
		addListener(SWT.FocusOut, e -> {
			focusByMouse = false;
			redraw();
		});
		addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
				e.doit = true;
			} else if (e.detail == SWT.TRAVERSE_RETURN) {
				// a focused button takes Enter, as a native one does, instead of the dialog's Launch
				e.doit = false;
				e.detail = SWT.TRAVERSE_NONE;
				press();
			}
		});
		addListener(SWT.KeyDown, e -> {
			if (e.character == ' ') {
				press();
			}
		});
	}

	void press() {
		if (isEnabled()) {
			notifyListeners(SWT.Selection, new Event());
		}
	}

	void setPalette(Palette palette) {
		this.palette = palette;
		redraw();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		GC gc = new GC(this);
		gc.setFont(primary ? Fonts.bold() : getFont());
		Point extent = gc.textExtent(label);
		gc.dispose();
		return primary ? new Point(Math.max(96, extent.x + 44), 40) : new Point(extent.x + 24, 32);
	}

	private void paint(Event e) {
		GC gc = e.gc;
		Rectangle area = getClientArea();
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);
		gc.setBackground(getParent().getBackground());
		gc.fillRectangle(area);
		gc.setBackground(fill());
		gc.fillRoundRectangle(0, 0, area.width, area.height, primary ? 14 : 10, primary ? 14 : 10);
		if (isFocusControl() && !focusByMouse) {
			gc.setForeground(primary ? palette.onAccent() : palette.accent());
			gc.setLineWidth(primary ? 1 : 2);
			int inset = primary ? 3 : 1;
			gc.drawRoundRectangle(inset, inset, area.width - 2 * inset - 1, area.height - 2 * inset - 1, 8, 8);
		}
		gc.setFont(primary ? Fonts.bold() : getFont());
		gc.setForeground(!isEnabled() ? palette.secondary() : primary ? palette.onAccent() : palette.text());
		Point extent = gc.textExtent(label);
		gc.drawString(label, (area.width - extent.x) / 2, (area.height - extent.y) / 2, true);
	}

	private Color fill() {
		if (!primary) {
			Color rest = palette.dark() ? palette.border() : palette.hover();
			return hovered ? palette.selection() : rest;
		}
		if (!isEnabled()) {
			return palette.border();
		}
		return pressed ? palette.accentPressed() : hovered ? palette.accentHover() : palette.primary();
	}
}
