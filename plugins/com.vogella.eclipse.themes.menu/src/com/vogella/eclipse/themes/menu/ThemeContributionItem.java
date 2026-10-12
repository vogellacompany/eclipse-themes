package com.vogella.eclipse.themes.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Lists the installed themes and applies the one the user picks, on releases whose
 * Window > Appearance menu has no Themes submenu yet.
 */
public class ThemeContributionItem extends CompoundContributionItem {

	private static final String RENDERER_QUALIFIER = "org.eclipse.e4.ui.workbench.renderers.swt";
	private static final String THEME_ENABLED_KEY = "themeEnabled";

	@Override
	protected IContributionItem[] getContributionItems() {
		// High contrast mode pins the engine to the high contrast theme, so offer no choice.
		if (PlatformUI.getWorkbench().getDisplay().getHighContrast()) {
			return new IContributionItem[0];
		}
		IThemeEngine engine = PlatformUI.getWorkbench().getService(IThemeEngine.class);
		if (engine == null) {
			IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(RENDERER_QUALIFIER);
			// No engine although theming is enabled means -cssTheme none, nothing to offer.
			if (prefs.getBoolean(THEME_ENABLED_KEY, true)) {
				return new IContributionItem[0];
			}
			return new IContributionItem[] { createEnableThemingItem(prefs) };
		}
		List<ITheme> themes = new ArrayList<>(engine.getThemes());
		themes.sort(Comparator.comparing(ITheme::getLabel, String.CASE_INSENSITIVE_ORDER));
		return themes.stream().map(theme -> createItem(engine, theme)).toArray(IContributionItem[]::new);
	}

	private static IContributionItem createEnableThemingItem(IEclipsePreferences prefs) {
		return new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.PUSH, index);
				item.setText(Messages.enableTheming);
				item.addListener(SWT.Selection, event -> {
					prefs.putBoolean(THEME_ENABLED_KEY, true);
					try {
						prefs.flush();
					} catch (BackingStoreException e) {
						ILog.of(ThemeContributionItem.class).error("Failed to enable theming", e);
					}
					offerRestart();
				});
			}
		};
	}

	private static IContributionItem createItem(IThemeEngine engine, ITheme theme) {
		return new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.RADIO, index);
				item.setText(theme.getLabel());
				ITheme activeTheme = engine.getActiveTheme();
				item.setSelection(activeTheme != null && activeTheme.getId().equals(theme.getId()));
				item.addListener(SWT.Selection, event -> {
					if (!((MenuItem) event.widget).getSelection()) {
						return;
					}
					ITheme previousTheme = engine.getActiveTheme();
					// Clicking the checked entry fires a selection too.
					if (previousTheme != null && previousTheme.getId().equals(theme.getId())) {
						return;
					}
					engine.setTheme(theme, true);
					// These releases restyle a live switch only partially, as their preference page says.
					offerRestart();
				});
			}
		};
	}

	private static void offerRestart() {
		MessageDialog dialog = new MessageDialog(null, Messages.restartTitle, null, Messages.restartMessage,
				MessageDialog.NONE, 1, Messages.restart, Messages.dontRestart);
		if (dialog.open() == 0) {
			Display.getDefault().asyncExec(() -> PlatformUI.getWorkbench().restart());
		}
	}
}
