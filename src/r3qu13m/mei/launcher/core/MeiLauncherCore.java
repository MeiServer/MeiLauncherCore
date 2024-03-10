package r3qu13m.mei.launcher.core;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

public class MeiLauncherCore {
	public static File getDefaultDirectory() {
		return FileSystemView.getFileSystemView().getDefaultDirectory();
	}

	public static File getBaseDirectory() {
		return new File(MeiLauncherCore.getDefaultDirectory(), "MeiServer");
	}

	public static File getConfigurationFile() {
		return new File(MeiLauncherCore.getBaseDirectory(), "config.yml");
	}
}
