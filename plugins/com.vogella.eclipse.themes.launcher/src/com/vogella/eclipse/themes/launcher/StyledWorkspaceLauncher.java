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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;

/**
 * Entry point the woven {@code IDEApplication} calls instead of opening the classic workspace dialog.
 */
public final class StyledWorkspaceLauncher {

	private StyledWorkspaceLauncher() {
	}

	/**
	 * Shows the styled dialog and returns true, or returns false so that the caller opens the classic one,
	 * either on failure or because the user asked for it.
	 */
	public static boolean show(Shell shell, ChooseWorkspaceData launchData, boolean force) {
		try {
			return new StyledChooseWorkspaceDialog(null, launchData).prompt(force);
		} catch (RuntimeException | LinkageError e) {
			ILog.of(StyledWorkspaceLauncher.class).error("Styled workspace dialog failed, using the classic one", e);
			return false;
		}
	}
}
