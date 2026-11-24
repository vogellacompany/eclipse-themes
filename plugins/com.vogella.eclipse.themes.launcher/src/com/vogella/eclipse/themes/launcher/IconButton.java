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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A 36 px icon-only button with one image per palette, since an SVG icon carries its colour.
 */
class IconButton extends Canvas {

	private final Image light;
	private final Image dark;
	private Palette palette;
	private boolean hovered;
	private boolean focusByMouse;

	IconButton(Composite parent, String accessibleName, Image light, Image dark, Palette palette) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.light = light;
		this.dark = dark;
		this.palette = palette;
		setToolTipText(accessibleName);
		setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		Accessibility.describe(this, () -> accessibleName, ACC.ROLE_PUSHBUTTON);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseEnter, e -> {
			hovered = true;
			redraw();
		});
		addListener(SWT.MouseExit, e -> {
			hovered = false;
			redraw();
		});
		addListener(SWT.MouseDown, e -> focusByMouse = true);
		addListener(SWT.MouseUp, e -> {
			if (getClientArea().contains(e.x, e.y)) {
				notifyListeners(SWT.Selection, new Event());
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
				// a focused button takes Enter instead of the dialog's Launch
				e.doit = false;
				e.detail = SWT.TRAVERSE_NONE;
				notifyListeners(SWT.Selection, new Event());
			}
		});
		addListener(SWT.KeyDown, e -> {
			if (e.character == ' ') {
				notifyListeners(SWT.Selection, new Event());
			}
		});
	}

	void setPalette(Palette palette) {
		this.palette = palette;
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(36, 36);
	}

	private void paint(Event e) {
		GC gc = e.gc;
		Rectangle area = getClientArea();
		gc.setAntialias(SWT.ON);
		gc.setBackground(getParent().getBackground());
		gc.fillRectangle(area);
		if (hovered) {
			gc.setBackground(palette.hover());
			gc.fillRoundRectangle(0, 0, area.width, area.height, 14, 14);
		}
		if (isFocusControl() && !focusByMouse) {
			gc.setForeground(palette.accent());
			gc.setLineWidth(2);
			gc.drawRoundRectangle(1, 1, area.width - 3, area.height - 3, 12, 12);
		}
		Image image = palette.dark() ? dark : light;
		if (image != null) {
			Rectangle b = image.getBounds();
			gc.drawImage(image, (area.width - b.width) / 2, (area.height - b.height) / 2);
		}
	}
}
