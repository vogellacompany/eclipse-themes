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

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.UserScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * The per OS user switch for the styled workspace dialog, kept in the user scope because it is read before any
 * workspace exists.
 */
public final class LauncherPreferences {

	public static final String QUALIFIER = "com.vogella.eclipse.themes.launcher";
	public static final String ENABLED = "styledWorkspaceDialog";
	/** System property that overrides the preference, as a way out when the IDE cannot reach the page. */
	public static final String OVERRIDE_PROPERTY = QUALIFIER + "." + ENABLED;

	private LauncherPreferences() {
	}

	public static boolean isEnabled() {
		String override = System.getProperty(OVERRIDE_PROPERTY);
		if (override != null) {
			return Boolean.parseBoolean(override);
		}
		return node().getBoolean(ENABLED, true);
	}

	public static void setEnabled(boolean enabled) {
		IEclipsePreferences node = node();
		node.putBoolean(ENABLED, enabled);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			ILog.of(LauncherPreferences.class).error("Could not store " + ENABLED, e);
		}
	}

	static IEclipsePreferences node() {
		return UserScope.INSTANCE.getNode(QUALIFIER);
	}
}
