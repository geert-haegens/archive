package daos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import decompileJasper.decompiler;
import enums.CharValues;

public class FileDAO {

	// CONSTRUCTORS

	public FileDAO() {
	}

	public static Set<String> scanStructure() throws RuntimeException {

		Path currentRelativePath = Paths.get("");

		// TODO : in commentaar zetten !
		currentRelativePath = Paths.get("d:\\geert\\temp");
		String path = currentRelativePath.toAbsolutePath().toString();
		decompiler.logPathAndFilename = path + "\\jasperDecompiler.log";

		try {

			/*
			JOptionPane.showMessageDialog(null, "logPath = " + decompiler.logPathAndFilename + CharValues.CRLF , "Info",
					JOptionPane.INFORMATION_MESSAGE);
			*/
			
			String logInfo = "STARTING LOG" + CharValues.CRLF + new Date().toString() + CharValues.CRLF + CharValues.CRLF;
			Files.write(Paths.get(decompiler.logPathAndFilename), logInfo.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			
			File dir = new File("");
			if (dir.getName().startsWith(".", 0)) {
				return null;
			}

			Set<String> absFiles = new HashSet<>();
			Collection<File> files = FileUtils.listFiles(new File(path), TrueFileFilter.INSTANCE,
					DirectoryFileFilter.DIRECTORY);
			for (File file : files) {
				if (file.getAbsolutePath().contains("\\.")) {
					continue;
				}
				if (file.isDirectory() && !file.equals(dir)) {
					continue;
				}
				if (!file.isDirectory() && Pattern.matches(".+\\.jasper$", file.getName().toLowerCase())) {
					int lenghtNoExt = file.getAbsolutePath().lastIndexOf('.');
					absFiles.add(file.getAbsolutePath().substring(0, lenghtNoExt));
				}
			}
			System.out.println("Scanned [" + path + "] = " + absFiles.size());
			return absFiles;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void writeLog(String logPathAndFilename, Map<String, String> log) throws RuntimeException {

		int errors = 0;
		int correct = 0;
		for (Entry<String, String> entry : log.entrySet()) {
			if (entry.getValue().length() == 0) {
				correct++;
			} else {
				errors++;
			}
		}

		StringBuilder builder = new StringBuilder();
		builder.append("ERRORS : " + errors + CharValues.CRLF);
		for (Entry<String, String> entry : log.entrySet()) {
			if (entry.getValue().length() > 0) {
				builder.append(entry.getKey()).append(": ").append(entry.getValue()).append(CharValues.CRLF);
			}
		}
		builder.append(CharValues.CRLF);
		builder.append("DECOMPILED : " + correct + CharValues.CRLF);
		for (Entry<String, String> entry : log.entrySet()) {
			if (entry.getValue().length() == 0) {
				builder.append(entry.getKey() + CharValues.CRLF);
			}
		}

		try {
			/*
			if (Files.exists(Paths.get(logPathAndFilename))) {
				throw new RuntimeException("File [" + logPathAndFilename + "] already exists !\n");
			}
			*/
			Files.write(Paths.get(logPathAndFilename), builder.toString().getBytes("utf-8"), StandardOpenOption.APPEND);
			System.out.println("Log :  " + logPathAndFilename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
