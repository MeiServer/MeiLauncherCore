package r3qu13m.mei.launcher.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

import r3qu13m.mei.lib.DataType;
import r3qu13m.mei.lib.FileUtils;
import r3qu13m.mei.lib.MPVec;
import r3qu13m.mei.lib.MeiLogger;
import r3qu13m.mei.lib.MeiServerLib;
import r3qu13m.mei.lib.OperationType;
import r3qu13m.mei.lib.structure.DistributeFile;
import r3qu13m.mei.lib.structure.ModPack;
import r3qu13m.mei.lib.structure.ModPackSequence;

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

	private static void deleteFile(final File target) {
		System.err.println(target);
		if (target.exists()) {
			target.renameTo(new File(target.getAbsolutePath() + ".bak"));
		}
	}

	private static String computeHash(final File target) {
		try {
			return DigestUtils.sha1Hex(new FileInputStream(target));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static File downloadFile(final File baseDir, final DistributeFile distribute) {
		final File dest = new File(baseDir, String.join(File.separator, distribute.getName().split("[/\\\\]")));
		if (dest.exists()) {
			if (ModExtractor.computeHash(dest).equals(distribute.getHash())) {
				return dest;
			}
			ModExtractor.deleteFile(dest);
		} else {
			if (!dest.getParentFile().exists()) {
				dest.getParentFile().mkdirs();
			}
		}

		MeiLogger.getLogger().info(String.format("Downloading %s...", distribute.getName()));

		try {
			FileUtils.downloadFile(distribute.getURL(), dest);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		final String sha1 = ModExtractor.computeHash(dest);

		if (sha1.equals(distribute.getHash())) {
			return dest;
		}

		if (dest.exists()) {
			dest.delete();
		}

		throw new RuntimeException(String.format("Failed to download: %s (actual sha1: %s, expected %s)",
				distribute.getName(), sha1, distribute.getHash()));
	}

	public static void extract(final File baseDir, final ModPackSequence seq, final ModPack latestPack,
			final Optional<ModPack> currentPack) {
		Map<UUID, OperationType> diff;
		if (currentPack.isPresent()) {
			diff = seq.getDifference(currentPack.get(), latestPack).getDifference();
		} else {
			diff = new MPVec(latestPack).getDifference();
		}

		for (final UUID key : diff.keySet()) {
			if (diff.get(key) == OperationType.DELETE) {
				DistributeFile df = MeiServerLib.instance().getDistributeFile(key);
				if (df.getType() == DataType.CONFIG || df.getType() == DataType.JAR) {
					continue;
				}

				df.getType().getDestDir(baseDir).ifPresent(dir -> {
					ModExtractor.deleteFile(new File(dir, String.join(File.separator, df.getName().split("[/\\\\]"))));
				});
			}
		}

		for (final DistributeFile distribute : latestPack.getFiles()) {
			final UUID id = distribute.getID();
			if (diff.containsKey(id) && diff.get(id) == OperationType.ADD) {
				File dir = baseDir;
				if (distribute.getType() != DataType.JAR) {
					dir = distribute.getType().getDestDir(dir).get();
				}
				ModExtractor.downloadFile(dir, distribute);
			}
		}
	}
}
