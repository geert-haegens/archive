package daos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import enums.CharValues;
import updateJasperSqlParam.SQLupdater;

public class FileDAO {

	// CONSTRUCTORS

	public FileDAO() {
	}

	public static Set<String> scanStructure(String ext, boolean includeSubDir, boolean consoleOutput) throws RuntimeException {

		try {

			/*
			JOptionPane.showMessageDialog(null, "logPath = " + SQLupdater.logPathAndFilename + CharValues.CRLF , "Info",
					JOptionPane.INFORMATION_MESSAGE);
			*/
			
			File dir = new File("");
			if (dir.getName().startsWith(".", 0)) {
				return null;
			}
			
			IOFileFilter includeSubDirDFF = null;
			if (includeSubDir) {
				includeSubDirDFF = DirectoryFileFilter.DIRECTORY;
			}

			Set<String> absFiles = new HashSet<>();
			Collection<File> files = FileUtils.listFiles(new File(SQLupdater.currentRelativePath), TrueFileFilter.INSTANCE, includeSubDirDFF
					);
			for (File file : files) {
				if (file.getAbsolutePath().contains("\\.")) {
					continue;
				}
				if (file.isDirectory() && !file.equals(dir)) {
					continue;
				}
				if (!file.isDirectory() && Pattern.matches(".+\\."+ext+"$", file.getName().toLowerCase())) {
					int lenghtNoExt = file.getAbsolutePath().lastIndexOf('.');
					absFiles.add(file.getAbsolutePath().substring(0, lenghtNoExt));
				}
			}
			if (consoleOutput) {
				System.out.println("Scanned [" + SQLupdater.currentRelativePath + "] = " + absFiles.size());
			}
			
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

	public static void createBuFolders() throws RuntimeException {
		
		Date date = new Date();
		String backupPath = SQLupdater.backupPathMain + "\\BU_" + Long.toString(date.getTime());
		try {
			if (!new File(backupPath).mkdirs()) {
				throw new RuntimeException("Unable to create directory " + backupPath);
			};
			SQLupdater.backupPathUsed = backupPath;
			String logInfo = "Created backup folder (original files can be found here)" + CharValues.CRLF + SQLupdater.backupPathUsed + CharValues.CRLF + CharValues.CRLF;
			Files.write(Paths.get(SQLupdater.logPathAndFilename), logInfo.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public static void moveFilesToBackupFolder(String ext) throws RuntimeException {
		
		Set<String> filesToMove = new HashSet<>();
		try {
			filesToMove.addAll(FileDAO.scanStructure(ext, false, false));
			
			for (String sFile : filesToMove) {
				File file = new File(sFile + "." + ext);
				//System.out.println("file to move : "+ file.getAbsolutePath());
				String moveToFileName = SQLupdater.backupPathUsed + "\\" + file.getName();
				if (!file.renameTo(new File(moveToFileName))) {
					throw new RuntimeException("Unable to move file " + file.getAbsolutePath() + " to backup folder !");
				}
				
			}
			
			if (filesToMove.size() > 0) {
				String logInfo = "INFO :  MOVED *.jrxml files to backup folder (" + filesToMove.size() + ")" + CharValues.CRLF + CharValues.CRLF;
				System.out.println(logInfo);
				Files.write(Paths.get(SQLupdater.logPathAndFilename), logInfo.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				
			}
			return;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
public static void copyFilesToBackupFolder(String ext) throws RuntimeException {
		
		Set<String> filesToCopy = new HashSet<>();
		try {
			filesToCopy.addAll(FileDAO.scanStructure(ext, false, false));
			
			for (String sFile : filesToCopy) {
				
				File sourcheFile = new File(sFile + "." + ext);
				Path sourcePath = Paths.get(sourcheFile.toURI());
				//System.out.println("file to copy : "+ sourcheFile.getAbsolutePath());
				File destFile = new File(SQLupdater.backupPathUsed);
				Path destPath = Paths.get(destFile.toURI());
				boolean failure = Files.copy(sourcePath, destPath.resolve(sourcePath.getFileName()), StandardCopyOption.COPY_ATTRIBUTES) == null;
				if (failure) {
					throw new RuntimeException("Unable to copy file " + sourcheFile.getAbsolutePath() + " to backup folder !");
				}
				
			}
			
			if (filesToCopy.size() > 0) {
				String logInfo = "INFO :  COPIED *.jasper files to backup folder (" + filesToCopy.size() + ")" + CharValues.CRLF + CharValues.CRLF;
				System.out.println(logInfo);
				Files.write(Paths.get(SQLupdater.logPathAndFilename), logInfo.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				
			}
			return;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
}
