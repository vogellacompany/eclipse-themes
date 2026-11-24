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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

/** Derived fonts of the dialog, kept in the JFace font registry so they are disposed with the display. */
final class Fonts {

	private Fonts() {
	}

	static Font bold() {
		return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
	}

	static Font title() {
		String key = Fonts.class.getName() + ".title";
		if (!JFaceResources.getFontRegistry().hasValueFor(key)) {
			FontData[] data = JFaceResources.getDefaultFont().getFontData();
			for (FontData d : data) {
				d.setHeight(d.getHeight() + 5);
			}
			JFaceResources.getFontRegistry().put(key, data);
		}
		return JFaceResources.getFontRegistry().getBold(key);
	}
}
