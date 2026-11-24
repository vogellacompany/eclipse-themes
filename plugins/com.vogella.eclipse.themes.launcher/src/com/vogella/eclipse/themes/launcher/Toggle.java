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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * An on/off switch; fires {@link SWT#Selection} after the state flipped.
 */
class Toggle extends Canvas {

	private Palette palette;
	private boolean selected;
	private boolean focusByMouse;

	Toggle(Composite parent, String accessibleName, Palette palette) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.palette = palette;
		setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		Accessibility.describe(this, () -> accessibleName, ACC.ROLE_CHECKBUTTON);
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
		return new Point(46, 30);
	}

	private void paint(Event e) {
		GC gc = e.gc;
		gc.setAntialias(SWT.ON);
		gc.setBackground(getParent().getBackground());
		gc.fillRectangle(getClientArea());
		int x = 3;
		int y = 3;
		if (isFocusControl() && !focusByMouse) {
			gc.setForeground(palette.accent());
			gc.setLineWidth(2);
			gc.drawRoundRectangle(1, 1, 43, 27, 28, 28);
		}
		gc.setBackground(selected ? palette.primary() : palette.dark() ? palette.border() : palette.secondary());
		gc.fillRoundRectangle(x, y, 40, 24, 24, 24);
		gc.setBackground(palette.onAccent());
		gc.fillOval(selected ? x + 18 : x + 2, y + 2, 20, 20);
	}
}
