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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

/**
 * Registers the weaving hook for the workspace chooser, but only while the styled dialog is enabled.
 */
public class Activator implements BundleActivator {

	private ServiceRegistration<WeavingHook> registration;

	@Override
	public void start(BundleContext context) {
		if (LauncherPreferences.isEnabled()) {
			registration = context.registerService(WeavingHook.class, new IDEApplicationWeaver(context.getBundle()), null);
		}
	}

	@Override
	public void stop(BundleContext context) {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}
}
