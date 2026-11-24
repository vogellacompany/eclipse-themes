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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	public static String pageDescription;
	public static String enableStyledDialog;

	static {
		NLS.initializeMessages(Messages.class.getPackageName() + ".messages", Messages.class);
	}

	private Messages() {
	}
}
