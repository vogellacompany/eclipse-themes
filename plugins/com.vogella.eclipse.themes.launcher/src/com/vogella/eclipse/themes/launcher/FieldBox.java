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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * A rounded field around a borderless text and a trailing button, with a focus ring while the text has focus.
 */
class FieldBox extends Composite {

	private Palette palette;
	private Text text;

	FieldBox(Composite parent, Palette palette) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.palette = palette;
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 4;
		layout.marginHeight = 4;
		layout.marginLeft = 8;
		layout.horizontalSpacing = 8;
		setLayout(layout);
		addListener(SWT.Paint, e -> {
			GC gc = e.gc;
			Rectangle area = getClientArea();
			gc.setAntialias(SWT.ON);
			gc.setBackground(getParent().getBackground());
			gc.fillRectangle(area);
			boolean focused = text != null && text.isFocusControl();
			gc.setBackground(focused ? palette.accent() : palette.dark() ? palette.border() : palette.secondaryBorder());
			gc.fillRoundRectangle(0, 0, area.width, area.height, 16, 16);
			int ring = focused ? 2 : 1;
			gc.setBackground(palette.field());
			gc.fillRoundRectangle(ring, ring, area.width - 2 * ring, area.height - 2 * ring, 14, 14);
		});
	}

	void setText(Text text) {
		this.text = text;
		text.addListener(SWT.FocusIn, e -> redraw());
		text.addListener(SWT.FocusOut, e -> redraw());
	}

	void setPalette(Palette palette) {
		this.palette = palette;
		setBackground(palette.field());
		redraw();
	}
}
