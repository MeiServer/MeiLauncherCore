package r3qu13m.mei.launcher.core;

import java.io.File;
import java.io.InputStream;

import r3qu13m.mei.lib.structure.DistributeFile;
import r3qu13m.mei.lib.structure.Mod;
import r3qu13m.mei.lib.structure.ModPack;

public class ModExtractor {
	private static ModExtractor _instance = null;

	public static ModExtractor instance() {
		if (ModExtractor._instance == null) {
			ModExtractor._instance = new ModExtractor();
		}
		return ModExtractor._instance;
	}

	private ModExtractor() {

	}
	
	private static File getTempDir(final File baseDir) {
		File ret = new File(baseDir, "temp");
		if (ret.exists()) {
			if (ret.isFile() || !ret.mkdir()) {
				throw new RuntimeException("Can't create temporary directory");
			}
		}
		return ret;
	}
	
	private static File downloadFile(final File baseDir, final DistributeFile distFile) {
		File dest = new File(ModExtractor.getTempDir(baseDir), distFile.getName());
		if (dest.exists()) {
		}
		return null;
	}
	
	public static void extract(final File baseDir, final ModPack pack) {
		for (Mod mod : pack.getMods()) {
			for (DistributeFile file: mod.getFiles()) {
				
			}
		}
	}
}
