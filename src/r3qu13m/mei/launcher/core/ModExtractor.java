package r3qu13m.mei.launcher.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;

import r3qu13m.mei.lib.MeiLogger;
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

	private static void deleteFile(final File target) {
		target.renameTo(new File(target.getAbsolutePath() + ".bak"));
	}

	private static String computeHash(final File target) {
		try {
			return DigestUtils.sha1Hex(new FileInputStream(target));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static File downloadFile(final File baseDir, final DistributeFile distribute) {
		File dest = new File(ModExtractor.getTempDir(baseDir), distribute.getName());
		if (dest.exists()) {
			if (ModExtractor.computeHash(dest).equals(distribute.getHash())) {
				return dest;
			}
			ModExtractor.deleteFile(dest);
		}

		try {
			InputStream is = distribute.getURL().openStream();
			OutputStream os = new FileOutputStream(dest);
			byte[] buf = new byte[256];
			while (is.available() != 0) {
				int readedCount = is.read(buf);
				os.write(buf, 0, readedCount);
			}
			is.close();
			os.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		final String sha1 = ModExtractor.computeHash(dest);

		if (sha1.equals(distribute.getHash())) {
			return dest;
		}

		throw new RuntimeException(String.format("Failed to download: %s (actual sha1: %s, expected %s)",
				distribute.getName(), sha1, distribute.getHash()));
	}

	public static void extract(final File baseDir, final ModPack pack) {
		for (Mod mod : pack.getMods()) {
			for (DistributeFile distribute : mod.getFiles()) {
				File downloaded = ModExtractor.downloadFile(baseDir, distribute);
			}
		}
	}
}
