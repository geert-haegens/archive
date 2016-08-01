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
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

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
			
			FileDAO.createBuFolders();
			FileDAO.moveFilesToBackupFolder("jrxml");
			FileDAO.copyFilesToBackupFolder("jasper");
			
			Set<String> jasperFilesNoExt = FileDAO.scanStructure("jasper", false, true);
			
			for (String pathAndFilenameNoExt : jasperFilesNoExt) {

				try {
					JasperReport report = (JasperReport) JRLoader.loadObjectFromFile(pathAndFilenameNoExt + ".jasper");
					//JRXmlWriter.writeReport(report, pathAndFilenameNoExt + ".jrxml", "UTF-8");
					String xmlStr = JasperCompileManager.writeReportToXml(report);
					System.out.println("\n****************\nfile = " + pathAndFilenameNoExt + ".jasper");
					
					String xmlModif = normaliseJRxml(xmlStr);
					if (xmlModif == null) {
						System.out.println("NO MODIF :  " + pathAndFilenameNoExt + ".jasper\n");
					} else {
						String pxmlFilename = pathAndFilenameNoExt + ".pxml";
						Files.write(Paths.get(pxmlFilename), xmlModif.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
						System.out.println("MODIFIED :  " + pxmlFilename);
					}
					
					log.put(pathAndFilenameNoExt + ".jasper", "");
				} catch (NoClassDefFoundError ex) {
					log.put(pathAndFilenameNoExt + ".jasper",
							CharValues.CRLF + Throwables.getStackTraceAsString(ex) + CharValues.CRLF);
				} catch (JRException e) {
					log.put(pathAndFilenameNoExt + ".jasper",
							CharValues.CRLF + Throwables.getStackTraceAsString(e) + CharValues.CRLF);
				}
			}

			FileDAO.writeLog(logPathAndFilename, log);
			System.out.println("Files have been altered for correct $SQL param.");

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"logPath" + logPathAndFilename + CharValues.CRLF + Throwables.getStackTraceAsString(e), "Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
	
	static int i = 1;
	private static String normaliseJRxml(String xml) throws RuntimeException {
		
		String xmlLC = xml.toLowerCase();
		
		String searchSqlParam = new String("<parameter name=\"sql\"");
		String searchCloseTag = new String("/>");
		int startPosSqlParam = xmlLC.indexOf(searchSqlParam);
		int stopPosSqlParam = xml.indexOf(searchCloseTag, startPosSqlParam) + searchCloseTag.length();
		
		boolean sqlParamPresent = (startPosSqlParam > 0 && stopPosSqlParam > startPosSqlParam);
		if (!sqlParamPresent) {
			System.out.println("sqlParam not found :  start="+startPosSqlParam+" / stop="+stopPosSqlParam);
		}
		
		String searchQueryStart = new String("<queryString").toLowerCase();
		String searchQueryEnd = new String("</queryString>").toLowerCase();
		int startPosQueryTag = xmlLC.indexOf(searchQueryStart);
		int stopPosQueryTag = xmlLC.indexOf(searchQueryEnd, startPosQueryTag) + searchQueryEnd.length();
		if (startPosQueryTag < 0 || stopPosQueryTag < 0) {
			throw new RuntimeException("No tag <queryString> not found in file");
		}
		String queryTag = xml.substring(startPosQueryTag, stopPosQueryTag);
		
		//System.out.println("\nqueryString ("+ (i++) +") :  start="+startPosQueryTag+" / stop="+stopPosQueryTag);
		//System.out.println("queryString found :  " + queryTag);
		
		String searchCDATAstart = new String("<![CDATA[").toLowerCase();
		String searchCDATAend = new String("]]>");
		int startPosCDATA = queryTag.toLowerCase().indexOf(searchCDATAstart) + searchCDATAstart.length();
		int stopPosCDATA = queryTag.indexOf(searchCDATAend, startPosCDATA);
		String cdataQueryTag = queryTag.substring(startPosCDATA, stopPosCDATA);
		
		//System.out.println("\nCDATA ("+ (i++) +") :  start="+startPosCDATA+" / stop="+stopPosCDATA);
		System.out.println("CDATA found :  " + cdataQueryTag);
		
		boolean sqlParamPresentInCDATA = cdataQueryTag.equals("$P!{sql}");
		//System.out.println("$P!{sql} = " + sqlParamPresentInCDATA);
		
		/*
		// JASPER REPORT IS OK
		if (sqlParamPresent && sqlParamPresentInCDATA) {
			return null;
		}
		
		// JASPER REPORT SqlParam missing
		if (!sqlParamPresent) {
			StringBuilder bxml = new StringBuilder();
			bxml.append(xml.substring(0, startPosQueryTag));
			bxml.append("<parameter name=\"sql\" class=\"java.lang.String\"/>" + CharValues.CRLF);
			bxml.append(xml.substring(startPosQueryTag));
			xml = bxml.toString();
		}
		*/
		
		// NEW SQL PARAM
		StringBuilder newSQlParam = new StringBuilder();
		newSQlParam.append("<parameter name=\"sql\" class=\"java.lang.String\">" + CharValues.CRLF);
		newSQlParam.append("<defaultValueExpression><![CDATA[\"");
		if (cdataQueryTag.length() > 0 && !sqlParamPresentInCDATA) {
			newSQlParam.append(cdataQueryTag);
		} 
		newSQlParam.append("\"]]></defaultValueExpression>" + CharValues.CRLF);
		newSQlParam.append("</parameter>" + CharValues.CRLF);
		
		// GENERATE CORRECT JASPER REPORT
		
		StringBuilder bxml = new StringBuilder();
		
		// sql param
		if (sqlParamPresent) {
			bxml.append(xml.substring(0, startPosSqlParam));
			bxml.append(newSQlParam);
			bxml.append(xml.substring(stopPosSqlParam, startPosQueryTag));
		} else {
			bxml.append(xml.substring(0, startPosQueryTag));
			bxml.append(newSQlParam);
		}
		
		// <queryString>
		bxml.append("<queryString>" + CharValues.CRLF);
		bxml.append("<![CDATA[$P!{sql}]]>" + CharValues.CRLF);
		bxml.append("</queryString>" + CharValues.CRLF);

		// xml after </queryString>
		bxml.append(xml.substring(stopPosQueryTag));
		
		return bxml.toString();
		
	}

}
