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

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.osgi.framework.FrameworkUtil;

/**
 * The workspace selection dialog: recent workspaces, a folder field and Launch on one screen, with the startup
 * settings one click away, in the light or dark colours of the default theme.
 */
public class StyledChooseWorkspaceDialog extends Dialog {

	/** Marks a control whose colours follow the palette. */
	private static final String ROLE = StyledChooseWorkspaceDialog.class.getName() + ".role";
	private static final String ROLE_WINDOW = "window";
	private static final String ROLE_SECONDARY = "secondary";
	private static final String ROLE_WARNING = "warning";
	private static final String ROLE_FIELD = "field";
	private static final String ROLE_BORDER = "border";

	/** Return code when the user asked for the platform's own workspace dialog. */
	private static final int CLASSIC = 2;

	private final ChooseWorkspaceData launchData;
	private final List<Image> images = new ArrayList<>();
	/** The dark preference the platform set before this dialog, from the same default theme. */
	private final boolean platformDark = Boolean.TRUE.equals(DefaultTheme.isDark());
	private boolean darkPreferenceChanged;

	private Palette palette;
	private Composite root;
	private StackLayout stackLayout;
	private Composite mainView;
	private Composite settingsView;
	private RowList workspaceList;
	private Text location;
	private Label locationHint;
	private ActionButton launchButton;
	private Combo themeCombo;

	public StyledChooseWorkspaceDialog(Shell parentShell, ChooseWorkspaceData launchData) {
		super(parentShell);
		this.launchData = launchData;
		setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.RESIZE);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		IProduct product = Platform.getProduct();
		shell.setText(product != null && product.getName() != null ? product.getName() : "Eclipse"); //$NON-NLS-1$
		// The platform repaints every shown shell in its own dark grey; put the palette back afterwards.
		shell.addListener(SWT.Show, e -> shell.getDisplay().asyncExec(() -> {
			if (!shell.isDisposed()) {
				applyPalette(shell);
			}
		}));
	}

	@Override
	protected Point getInitialSize() {
		return new Point(720, 500);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Monitor monitor = getShell().getDisplay().getPrimaryMonitor();
		Rectangle bounds = monitor.getClientArea();
		return new Point(bounds.x + (bounds.width - initialSize.x) / 2, bounds.y + (bounds.height - initialSize.y) / 3);
	}

	@Override
	protected Control createContents(Composite parent) {
		palette = Palette.of(prefersDark());
		root = new Composite(parent, SWT.NONE);
		root.setData(ROLE, ROLE_WINDOW);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(root);
		stackLayout = new StackLayout();
		root.setLayout(stackLayout);
		mainView = createMainView(root);
		settingsView = createSettingsView(root);
		stackLayout.topControl = mainView;

		// a filter, because GTK stops Escape at the first control listening for traversal in a shell without parent
		Shell shell = parent.getShell();
		Listener keys = e -> {
			if (!(e.widget instanceof Control control) || control.getShell() != shell) {
				return;
			}
			boolean onMain = stackLayout.topControl == mainView;
			if (e.detail == SWT.TRAVERSE_RETURN && control == location && launchButton.isEnabled()) {
				e.doit = false;
				e.detail = SWT.TRAVERSE_NONE;
				launch();
			} else if (e.detail == SWT.TRAVERSE_ESCAPE) {
				e.doit = false;
				e.detail = SWT.TRAVERSE_NONE;
				if (onMain) {
					setReturnCode(CANCEL);
					close();
				} else {
					showView(mainView);
				}
			}
		};
		applyPalette(root);
		workspaceList.forceFocus();
		shell.getDisplay().addFilter(SWT.Traverse, keys);
		shell.addListener(SWT.Dispose, e -> e.display.removeFilter(SWT.Traverse, keys));
		return root;
	}

	private void showView(Composite view) {
		stackLayout.topControl = view;
		root.layout();
		(view == mainView ? workspaceList : themeCombo).setFocus();
	}

	private Composite createMainView(Composite parent) {
		Composite view = createView(parent);

		Composite header = row(view, 2);
		title(header, "Open a workspace"); //$NON-NLS-1$
		IconButton settings = new IconButton(header, "Settings", loadImage("gear-light.svg"), //$NON-NLS-1$ //$NON-NLS-2$
				loadImage("gear-dark.svg"), palette); //$NON-NLS-1$
		settings.addListener(SWT.Selection, e -> showView(settingsView));

		workspaceList = new RowList(view, loadImage("folder.svg"), palette); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, true).indent(0, 20).applyTo(workspaceList);
		workspaceList.setEmptyText("Pick a folder for your first workspace", //$NON-NLS-1$
				"Eclipse keeps your projects and settings there. The suggested folder works fine; you can switch any time later."); //$NON-NLS-1$
		List<RowList.Row> rows = new ArrayList<>();
		for (String workspace : recentWorkspaces()) {
			rows.add(workspaceRow(workspace));
		}
		workspaceList.setRows(rows);
		workspaceList.addListener(SWT.Selection, e -> {
			RowList.Row row = workspaceList.getSelectedRow();
			if (row != null) {
				location.setText(TextProcessor.process((String) row.data()));
			}
		});
		workspaceList.addListener(SWT.DefaultSelection, e -> launch());

		Label separator = new Label(view, SWT.NONE);
		separator.setData(ROLE, ROLE_BORDER);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 1).indent(0, 20).applyTo(separator);

		Composite actions = row(view, 2);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 16).applyTo(actions);
		FieldBox field = new FieldBox(actions, palette);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 40).applyTo(field);
		location = new Text(field, SWT.SINGLE);
		location.setData(ROLE, ROLE_FIELD);
		Accessibility.describe(location, () -> "Workspace folder", 0); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(location);
		field.setText(location);
		ActionButton browse = new ActionButton(field, "Browse\u2026", false, palette); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(browse);
		browse.addListener(SWT.Selection, e -> browse());
		launchButton = new ActionButton(actions, "Launch", true, palette); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(launchButton);
		launchButton.addListener(SWT.Selection, e -> launch());

		Composite footer = row(view, 2);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 12).applyTo(footer);
		CheckBox remember = new CheckBox(footer, "Always open this workspace", palette); //$NON-NLS-1$
		remember.setSelection(!launchData.getShowDialog());
		remember.addListener(SWT.Selection, e -> launchData.toggleShowDialog());
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(remember);
		locationHint = new Label(footer, SWT.RIGHT);
		locationHint.setData(ROLE, ROLE_SECONDARY);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(locationHint);

		location.addListener(SWT.Modify, e -> validateLocation());
		String initial = rows.isEmpty() ? launchData.getInitialDefault() : (String) rows.get(0).data();
		location.setText(TextProcessor.process(initial == null ? "" : initial)); //$NON-NLS-1$
		if (!rows.isEmpty()) {
			workspaceList.select(0, false);
		}
		return view;
	}

	private RowList.Row workspaceRow(String workspace) {
		File folder = new File(workspace);
		String name = folder.getName().isEmpty() ? workspace : folder.getName();
		boolean missing = !folder.exists();
		String meta = missing ? "Not found" : ago(lastUsed(folder)); //$NON-NLS-1$
		return new RowList.Row(name, shortenPath(workspace), meta, missing, workspace, workspace);
	}

	private void validateLocation() {
		String path = TextProcessor.deprocess(location.getText().trim());
		File folder = new File(path);
		String hint;
		boolean warning = false;
		boolean valid = !path.isEmpty();
		if (!valid) {
			hint = "Enter the workspace folder"; //$NON-NLS-1$
		} else if (folder.isFile()) {
			hint = "This is a file. Choose a folder instead."; //$NON-NLS-1$
			warning = true;
			valid = false;
		} else if (!folder.exists()) {
			hint = "A new workspace is created here"; //$NON-NLS-1$
		} else if (new File(folder, ".metadata").isDirectory()) { //$NON-NLS-1$
			hint = "Existing workspace \u00b7 Enter to launch"; //$NON-NLS-1$
		} else {
			hint = "This folder becomes a new workspace"; //$NON-NLS-1$
		}
		locationHint.setText(hint);
		locationHint.setData(ROLE, warning ? ROLE_WARNING : ROLE_SECONDARY);
		locationHint.setForeground(warning ? palette.warning() : palette.secondary());
		locationHint.getParent().layout();
		launchButton.setEnabled(valid);
		workspaceList.selectData(path);
	}

	private void browse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SHEET);
		dialog.setText("Select Workspace Folder"); //$NON-NLS-1$
		dialog.setMessage("Select the folder to use as workspace."); //$NON-NLS-1$
		File f = new File(TextProcessor.deprocess(location.getText().trim()));
		while (f != null && !f.exists()) {
			f = f.getParentFile();
		}
		if (f != null) {
			dialog.setFilterPath(f.getAbsolutePath());
		}
		String dir = dialog.open();
		if (dir != null) {
			location.setText(TextProcessor.process(dir));
		}
	}

	private void launch() {
		String workspace = TextProcessor.deprocess(location.getText().trim());
		if (!workspace.isEmpty() && !new File(workspace).isFile()) {
			launchData.workspaceSelected(workspace);
			setReturnCode(OK);
			close();
		}
	}

	private Composite createSettingsView(Composite parent) {
		Composite view = createView(parent);

		Composite header = row(view, 2);
		IconButton back = new IconButton(header, "Back to workspaces", loadImage("back-light.svg"), //$NON-NLS-1$ //$NON-NLS-2$
				loadImage("back-dark.svg"), palette); //$NON-NLS-1$
		GridDataFactory.swtDefaults().indent(-8, 0).applyTo(back);
		back.addListener(SWT.Selection, e -> showView(mainView));
		title(header, "Settings"); //$NON-NLS-1$

		Composite themeRow = settingRow(view, "Theme", //$NON-NLS-1$
				"For new workspaces and workspaces without a theme of their own."); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 12).applyTo(themeRow);
		themeCombo = new Combo(themeRow, SWT.READ_ONLY | SWT.DROP_DOWN);
		themeCombo.setEnabled(false);
		GridDataFactory.swtDefaults().hint(200, SWT.DEFAULT).align(SWT.END, SWT.CENTER).applyTo(themeCombo);
		Display display = parent.getDisplay();
		CompletableFuture.supplyAsync(DefaultTheme::available).thenAccept(themes -> display.asyncExec(() -> {
			if (themeCombo.isDisposed()) {
				return;
			}
			String current = DefaultTheme.get();
			themeCombo.add("Follow the operating system"); //$NON-NLS-1$
			int selected = 0;
			for (int i = 0; i < themes.size(); i++) {
				themeCombo.add(themes.get(i).label());
				if (themes.get(i).id().equals(current)) {
					selected = i + 1;
				}
			}
			themeCombo.select(selected);
			themeCombo.setEnabled(true);
			themeCombo.addListener(SWT.Selection, e -> {
				int index = themeCombo.getSelectionIndex();
				DefaultTheme.Theme theme = index > 0 ? themes.get(index - 1) : null;
				DefaultTheme.set(theme);
				setPalette(Palette.of(theme != null ? theme.dark() : Display.isSystemDarkTheme()));
			});
		}));

		Composite startRow = settingRow(view, "Use this launcher at startup", //$NON-NLS-1$
				"Off brings back the classic workspace prompt at the next start."); //$NON-NLS-1$
		Toggle enabled = new Toggle(startRow, "Use this launcher at startup", palette); //$NON-NLS-1$
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).applyTo(enabled);
		enabled.setSelection(LauncherPreferences.isEnabled());
		enabled.addListener(SWT.Selection, e -> LauncherPreferences.setEnabled(enabled.getSelection()));

		Composite classicRow = settingRow(view, "Classic workspace prompt", //$NON-NLS-1$
				"Opens the platform's own dialog instead of this one, for this start."); //$NON-NLS-1$
		ActionButton classic = new ActionButton(classicRow, "Switch to classic", false, palette); //$NON-NLS-1$
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).applyTo(classic);
		classic.addListener(SWT.Selection, e -> {
			setReturnCode(CLASSIC);
			close();
		});

		Label note = new Label(view, SWT.WRAP);
		note.setData(ROLE, ROLE_SECONDARY);
		note.setText("Both settings apply to every Eclipse installation of your user account."); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.END).applyTo(note);
		return view;
	}

	/** A setting: name and description on the left, its control added by the caller on the right, a rule below. */
	private Composite settingRow(Composite view, String name, String description) {
		Composite row = row(view, 2);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(24, 4).margins(0, 16).applyTo(row);
		Composite text = row(row, 1);
		GridLayoutFactory.fillDefaults().spacing(0, 4).applyTo(text);
		Label label = new Label(text, SWT.NONE);
		label.setData(ROLE, ROLE_WINDOW);
		label.setText(name);
		label.setFont(Fonts.bold());
		Label detail = new Label(text, SWT.WRAP);
		detail.setData(ROLE, ROLE_SECONDARY);
		detail.setText(description);
		GridDataFactory.fillDefaults().grab(true, false).hint(410, SWT.DEFAULT).applyTo(detail);
		Label rule = new Label(view, SWT.NONE);
		rule.setData(ROLE, ROLE_BORDER);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 1).applyTo(rule);
		return row;
	}

	private static Composite createView(Composite parent) {
		Composite view = new Composite(parent, SWT.NONE);
		view.setData(ROLE, ROLE_WINDOW);
		GridLayoutFactory.fillDefaults().extendedMargins(32, 32, 24, 22).spacing(0, 0).applyTo(view);
		return view;
	}

	private static Composite row(Composite parent, int columns) {
		Composite row = new Composite(parent, SWT.NONE);
		row.setData(ROLE, ROLE_WINDOW);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(row);
		GridLayoutFactory.fillDefaults().numColumns(columns).spacing(8, 0).applyTo(row);
		return row;
	}

	private static void title(Composite parent, String text) {
		Label title = new Label(parent, SWT.NONE);
		title.setData(ROLE, ROLE_WINDOW);
		title.setText(text);
		title.setFont(Fonts.title());
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(title);
	}

	private void setPalette(Palette newPalette) {
		palette = newPalette;
		Shell shell = getShell();
		darkPreferenceChanged = true;
		shell.getDisplay().setDarkThemePreferred(newPalette.dark());
		applyPalette(shell);
	}

	/** Colours every control by its role; painted controls take the palette itself. */
	private void applyPalette(Control control) {
		if (control instanceof RowList list) {
			list.setPalette(palette);
		} else if (control instanceof ActionButton button) {
			button.setPalette(palette);
		} else if (control instanceof IconButton button) {
			button.setPalette(palette);
		} else if (control instanceof CheckBox check) {
			check.setPalette(palette);
		} else if (control instanceof Toggle toggle) {
			toggle.setPalette(palette);
		} else if (control instanceof FieldBox field) {
			field.setPalette(palette);
		} else if (control.getData(ROLE) != null) {
			Object role = control.getData(ROLE);
			control.setBackground(ROLE_FIELD.equals(role) ? palette.field()
					: ROLE_BORDER.equals(role) ? palette.border() : palette.window());
			control.setForeground(ROLE_SECONDARY.equals(role) ? palette.secondary()
					: ROLE_WARNING.equals(role) ? palette.warning() : palette.text());
		} else if (control instanceof Shell shell) {
			shell.setBackground(palette.window());
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				applyPalette(child);
			}
		}
	}

	private boolean prefersDark() {
		Boolean dark = DefaultTheme.isDark();
		return dark != null ? dark : Display.isSystemDarkTheme();
	}

	private Image loadImage(String name) {
		URL url = FrameworkUtil.getBundle(getClass()).getEntry("icons/launcher/" + name); //$NON-NLS-1$
		if (url == null) {
			return null;
		}
		Image image = ImageDescriptor.createFromURL(url).createImage(false);
		if (image != null) {
			images.add(image);
		}
		return image;
	}

	private List<String> recentWorkspaces() {
		List<String> result = new ArrayList<>();
		String[] workspaces = launchData.getRecentWorkspaces();
		if (workspaces != null) {
			for (String workspace : workspaces) {
				if (workspace != null && !workspace.isBlank()) {
					result.add(workspace);
				}
			}
		}
		return result;
	}

	private static long lastUsed(File workspace) {
		long last = 0;
		for (String marker : new String[] { ".metadata/.lock", ".metadata/.log", //$NON-NLS-1$ //$NON-NLS-2$
				".metadata/.plugins/org.eclipse.e4.workbench/workbench.xmi", ".metadata" }) { //$NON-NLS-1$ //$NON-NLS-2$
			last = Math.max(last, new File(workspace, marker).lastModified());
		}
		return last;
	}

	static String ago(long time) {
		if (time <= 0) {
			return ""; //$NON-NLS-1$
		}
		Duration d = Duration.ofMillis(Math.max(0, System.currentTimeMillis() - time));
		if (d.toMinutes() < 1) {
			return "Just now"; //$NON-NLS-1$
		}
		if (d.toHours() < 1) {
			return d.toMinutes() + " min ago"; //$NON-NLS-1$
		}
		if (d.toDays() < 1) {
			return d.toHours() + (d.toHours() == 1 ? " hour ago" : " hours ago"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (d.toDays() < 2) {
			return "Yesterday"; //$NON-NLS-1$
		}
		if (d.toDays() < 30) {
			return d.toDays() + " days ago"; //$NON-NLS-1$
		}
		long months = d.toDays() / 30;
		return months < 12 ? months + (months == 1 ? " month ago" : " months ago") : "Over a year ago"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static String shortenPath(String path) {
		String trimmed = path.length() > 1 ? path.replaceAll("[/\\\\]+$", "") : path; //$NON-NLS-1$ //$NON-NLS-2$
		String home = System.getProperty("user.home"); //$NON-NLS-1$
		return home != null && trimmed.startsWith(home) ? "~" + trimmed.substring(home.length()) : trimmed; //$NON-NLS-1$
	}

	/**
	 * Shows the dialog, or picks the last workspace without asking when the user switched the prompt off.
	 * Returns false when the user asked for the classic dialog instead.
	 */
	public boolean prompt(boolean force) {
		if (force || launchData.getShowDialog()) {
			open();
			if (getReturnCode() == CLASSIC) {
				return false;
			}
			if (getReturnCode() == CANCEL) {
				launchData.workspaceSelected(null);
			}
			return true;
		}
		String[] recent = launchData.getRecentWorkspaces();
		String workspace = recent != null && recent.length > 0 ? recent[0] : null;
		if (workspace == null || workspace.isEmpty()) {
			workspace = launchData.getInitialDefault();
		}
		launchData.workspaceSelected(TextProcessor.deprocess(workspace));
		return true;
	}

	@Override
	public boolean close() {
		// the platform resets only the dark preference it set itself, so put that one back
		if (darkPreferenceChanged && getShell() != null) {
			getShell().getDisplay().setDarkThemePreferred(platformDark);
		}
		images.forEach(Image::dispose);
		images.clear();
		return super.close();
	}
}
