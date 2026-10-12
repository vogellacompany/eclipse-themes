package com.vogella.eclipse.themes.menu;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	public static String enableTheming;
	public static String restartTitle;
	public static String restartMessage;
	public static String restart;
	public static String dontRestart;

	static {
		NLS.initializeMessages(Messages.class.getPackageName() + ".messages", Messages.class);
	}

	private Messages() {
	}
}
