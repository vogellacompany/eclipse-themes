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

import org.eclipse.core.runtime.preferences.UserScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Switches the styled workspace dialog on or off for every Eclipse installation of this OS user.
 */
public class LauncherPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public LauncherPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		ScopedPreferenceStore store = new ScopedPreferenceStore(UserScope.INSTANCE, LauncherPreferences.QUALIFIER);
		store.setDefault(LauncherPreferences.ENABLED, true);
		setPreferenceStore(store);
		setDescription(Messages.pageDescription);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(LauncherPreferences.ENABLED, Messages.enableStyledDialog,
				getFieldEditorParent()));
	}
}
