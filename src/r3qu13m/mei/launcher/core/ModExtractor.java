package r3qu13m.mei.launcher.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

import r3qu13m.mei.lib.FileUtils;
import r3qu13m.mei.lib.MPVec;
import r3qu13m.mei.lib.MeiServerLib;
import r3qu13m.mei.lib.OperationType;
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
		if (target.exists()) {
			target.renameTo(new File(target.getAbsolutePath() + ".bak"));
		}
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
			FileUtils.downloadFile(distribute.getURL(), dest);
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

	public static void extract(final File baseDir, final ModPack pack, final Optional<ModPack> currentPack) {
		Map<UUID, OperationType> diff;
		if (currentPack.isPresent()) {
			diff = new MPVec(pack, currentPack.get()).getDifference();
		} else {
			diff = new MPVec(pack).getDifference();
		}

		for (UUID key : diff.keySet()) {
			if (diff.get(key) == OperationType.DELETE) {
				ModExtractor.deleteFile(new File(ModExtractor.getTempDir(baseDir),
						MeiServerLib.instance().getDistributeFile(key).getName()));
			}
		}

		for (Mod mod : pack.getMods()) {
			for (DistributeFile distribute : mod.getFiles()) {
				final UUID id = distribute.getID();
				if (diff.containsKey(id) && diff.get(id) == OperationType.ADD) {
					ModExtractor.downloadFile(baseDir, distribute);
				}
			}
		}
	}
}
