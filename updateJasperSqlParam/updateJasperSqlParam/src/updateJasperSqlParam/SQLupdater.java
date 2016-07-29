package updateJasperSqlParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import com.google.common.base.Throwables;

import daos.FileDAO;
import enums.CharValues;

public class SQLupdater {
	
	
	// TODO : juiste pad in commentaar zetten !
	//private static final Path userPath = Paths.get("").toAbsolutePath();
	private static final Path userPath = Paths.get("d:\\geert\\temp\\jasperfiles").toAbsolutePath();
	
	public static final String currentRelativePath = userPath.toString();
	public static final String logPathAndFilename = currentRelativePath + "\\jasperSQLupdater.log";
	public static final String backupPathMain = currentRelativePath + "\\BU_jasperSQLupdater";
	public static String backupPathUsed = backupPathMain;
	
	public static void main(String[] args) {

		try {

			Map<String, String> log = new HashMap<>();
			
			String logInfo = "STARTING LOG" + CharValues.CRLF + new Date().toString() + CharValues.CRLF + CharValues.CRLF;
			Files.write(Paths.get(SQLupdater.logPathAndFilename), logInfo.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			
			Set<String> jasperFilesNoExt = FileDAO.scanStructure("jasper", false, true);
			
			FileDAO.createBuFolders();
			FileDAO.moveFilesToBackupFolder("jrxml");
			
			

			/*
			for (String pathAndFilenameNoExt : jasperFilesNoExt) {

				try {
					JasperReport report = (JasperReport) JRLoader.loadObjectFromFile(pathAndFilenameNoExt + ".jasper");
					JRXmlWriter.writeReport(report, pathAndFilenameNoExt + ".jrxml", "UTF-8");
					log.put(pathAndFilenameNoExt + ".jasper", "");
				} catch (NoClassDefFoundError ex) {
					log.put(pathAndFilenameNoExt + ".jasper",
							CharValues.CRLF + Throwables.getStackTraceAsString(ex) + CharValues.CRLF);
				} catch (JRException e) {
					log.put(pathAndFilenameNoExt + ".jasper",
							CharValues.CRLF + Throwables.getStackTraceAsString(e) + CharValues.CRLF);
				}
			}
*/
			FileDAO.writeLog(logPathAndFilename, log);
			System.out.println("Files decompiled.");

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"logPath" + logPathAndFilename + CharValues.CRLF + Throwables.getStackTraceAsString(e), "Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}

}
