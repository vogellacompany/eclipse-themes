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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * The launcher's colours, one light and one dark set, picked from the default theme so the dialog matches the IDE
 * it opens.
 */
record Palette(boolean dark, Color window, Color hover, Color selection,
		Color text, Color secondary, Color accent, Color onAccent, Color primary, Color accentHover, Color accentPressed,
		Color border, Color warning, Color field, Color secondaryBorder) {

	static Palette of(boolean dark) {
		return dark ? new Palette(true, c(32, 33, 38), c(42, 44, 51), c(36, 52, 77),
				c(232, 234, 237), c(160, 167, 178), c(102, 163, 245), c(255, 255, 255), c(40, 98, 180),
				c(47, 110, 198), c(33, 84, 158), c(52, 55, 63), c(232, 179, 92), c(40, 42, 48), c(52, 55, 63))
				: new Palette(false, c(255, 255, 255), c(240, 242, 245),
						c(226, 236, 252), c(31, 35, 40), c(87, 96, 106), c(9, 105, 218), c(255, 255, 255),
						c(9, 105, 218), c(20, 116, 228), c(8, 88, 184), c(222, 225, 230), c(154, 103, 0), c(255, 255, 255), c(201, 206, 214));
	}

	private static Color c(int r, int g, int b) {
		return new Color(new RGB(r, g, b));
	}
}
