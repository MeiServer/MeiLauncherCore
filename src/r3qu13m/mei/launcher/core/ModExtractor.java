package r3qu13m.mei.launcher.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
		if (target.exists()) {
			MeiLogger.getLogger().info(String.format("Delete %s", target.getName()));
			target.delete();
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

		MeiLogger.getLogger().info(String.format("Downloading %s...", dest.getAbsoluteFile()));

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

	private static Map<UUID, String> extract(Map<UUID, OperationType> diff, OperationType op) {
		Map<UUID, String> res = new HashMap<>();
		List<UUID> ids = diff.entrySet().stream().filter(entry -> entry.getValue() == op)
				.map(entry -> entry.getKey()).collect(Collectors.toList());
		for (UUID id : ids) {
			DistributeFile df = MeiServerLib.instance().getDistributeFile(id);
			res.put(id, df.getHash());
		}
		return res;
	}

	public static void extract(final File baseDir, final ModPackSequence seq, final Optional<ModPack> currentPack)
			throws IOException {
		boolean doUpdateJar = false;

		Map<UUID, OperationType> diff;
		ModPack latestPack = seq.getLatestPack().get();
		ModPack firstPack = seq.getFirstPack().get();

		if (currentPack.isPresent()) {
			diff = seq.getDifference(currentPack.get(), latestPack).getDifference();
		} else if (!firstPack.equals(latestPack)) {
			diff = seq.getDifference(firstPack, latestPack).getDifference();
		} else {
			diff = new MPVec(latestPack).getDifference();
		}

		Map<UUID, String> diffDeletes = extract(diff, OperationType.DELETE);
		Map<UUID, String> diffAdds = extract(diff, OperationType.ADD);
		diffAdds.putAll(extract(diff, OperationType.IDENTITY));;

		for (final UUID id : diffDeletes.keySet()) {
			DistributeFile df = MeiServerLib.instance().getDistributeFile(id);

			if (df.getType() == DataType.CONFIG) {
				continue;
			}

			df.getType().getDestDir(baseDir).ifPresent(dir -> {
				ModExtractor.deleteFile(new File(dir, String.join(File.separator, df.getName().split("[/\\\\]"))));
			});
		}

		for (final UUID id : diffAdds.keySet()) {
			DistributeFile df = MeiServerLib.instance().getDistributeFile(id);

			File dir = baseDir;
			if (df.getType() != DataType.JAR) {
				dir = df.getType().getDestDir(dir).get();
			} else {
				dir = new File(baseDir, "temp");
			}

			ModExtractor.downloadFile(dir, df);
			doUpdateJar = true;
		}

		// Merge jar-mods into minecraft.jar
		List<File> jarMods = new ArrayList<>();
		Set<String> wroteFiles = new HashSet<>();

		final File tempDir = new File(baseDir, "temp");
		latestPack.getMods().stream().sorted((a, b) -> a.getPriority() - b.getPriority()).forEach(mod -> {
			for (DistributeFile file : mod.getFiles()) {
				if (file.getType() == DataType.JAR) {
					jarMods.add(new File(tempDir, file.getName()));
				}
			}
		});

		File binDir = new File(baseDir, "bin");
		File lockFile = new File(binDir, "minecraft_lock.txt");
		jarMods.add(new File(binDir, "minecraft_vanilla.jar"));
		doUpdateJar |= !lockFile.exists();

		if (doUpdateJar) {
			FileOutputStream fos = new FileOutputStream(new File(new File(baseDir, "bin"), "minecraft.jar"));
			ZipOutputStream dest = new ZipOutputStream(fos);
			for (File file : jarMods) {
				MeiLogger.getLogger().info(String.format("Merging %s...", file.getName()));

				ZipFile zf = new ZipFile(file);
				Enumeration<? extends ZipEntry> entries = zf.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.getName().startsWith("META-INF")) {
						continue;
					}
					if (!wroteFiles.contains(entry.getName())) {
						dest.putNextEntry(new ZipEntry(entry.getName()));

						final InputStream is = zf.getInputStream(entry);
						final byte buf[] = new byte[1024];
						while (true) {
							final int readCount = is.read(buf, 0, 1024);
							if (readCount == -1) {
								break;
							}
							dest.write(buf, 0, readCount);
						}
						dest.flush();
						is.close();
						dest.closeEntry();
						wroteFiles.add(entry.getName());
					}
				}
				zf.close();
			}
			dest.close();
			lockFile.createNewFile();
		}
		MeiLogger.getLogger().info("All done!");
	}
}
