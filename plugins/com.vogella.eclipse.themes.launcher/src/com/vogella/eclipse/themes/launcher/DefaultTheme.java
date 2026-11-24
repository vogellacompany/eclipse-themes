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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.UserScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * The platform's default theme for new workspaces, stored per product in the user scope with the same keys as the
 * workbench's own default theme setting.
 */
final class DefaultTheme {

	private static final String THEME_PLUGIN_ID = "org.eclipse.e4.ui.css.swt.theme";
	private static final String THEMEID_KEY = "themeid";
	private static final String THEME_IS_DARK_KEY_PREFIX = "themeIsDark.";

	record Theme(String id, String label, boolean dark) {
	}

	private DefaultTheme() {
	}

	/** Themes registered for this operating system, sorted by label. */
	static List<Theme> available() {
		Map<String, Theme> themes = new LinkedHashMap<>();
		String os = Platform.getOS();
		for (IConfigurationElement element : Platform.getExtensionRegistry()
				.getConfigurationElementsFor(THEME_PLUGIN_ID)) {
			if (!"theme".equals(element.getName())) {
				continue;
			}
			String id = element.getAttribute("id");
			String label = element.getAttribute("label");
			String themeOs = element.getAttribute("os");
			if (id != null && label != null && (themeOs == null || themeOs.isEmpty() || themeOs.equals(os))) {
				String dark = element.getAttribute("isDarkTheme");
				themes.putIfAbsent(id, new Theme(id, label, dark != null ? Boolean.parseBoolean(dark) : id.contains("dark")));
			}
		}
		return themes.values().stream().sorted(Comparator.comparing(Theme::label, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	static String get() {
		return node().get(THEMEID_KEY, null);
	}

	/** Whether the default theme is dark, or {@code null} when there is no default theme. */
	static Boolean isDark() {
		String id = get();
		return id == null ? null : node().getBoolean(THEME_IS_DARK_KEY_PREFIX + id, id.contains("dark"));
	}

	/** Sets the default theme, or removes it for {@code null} so the platform follows the operating system again. */
	static void set(Theme theme) {
		IEclipsePreferences node = node();
		String previous = node.get(THEMEID_KEY, null);
		if (previous != null) {
			node.remove(THEME_IS_DARK_KEY_PREFIX + previous);
		}
		if (theme == null) {
			node.remove(THEMEID_KEY);
		} else {
			node.put(THEMEID_KEY, theme.id());
			node.putBoolean(THEME_IS_DARK_KEY_PREFIX + theme.id(), theme.dark());
		}
		try {
			node.flush();
		} catch (BackingStoreException e) {
			ILog.of(DefaultTheme.class).error("Could not store the default theme", e);
		}
	}

	private static IEclipsePreferences node() {
		IEclipsePreferences base = UserScope.INSTANCE.getNode(THEME_PLUGIN_ID);
		IProduct product = Platform.getProduct();
		String scope = product != null ? product.getId() : System.getProperty("eclipse.application");
		return scope != null ? (IEclipsePreferences) base.node(scope) : base;
	}
}
