package r3qu13m.mei.launcher.core;

import java.io.File;
import java.nio.file.Path;

import javax.swing.filechooser.FileSystemView;

public class MeiLauncherCore {
	public static File getDefaultDirectory() {
		return FileSystemView.getFileSystemView().getDefaultDirectory();
	}

	public static File getBaseDirectory() {
		return new File(getDefaultDirectory(), "MeiServer");
	}

	public static File getConfigurationFile() {
		return new File(getBaseDirectory(), "config.yml");
	}
}
