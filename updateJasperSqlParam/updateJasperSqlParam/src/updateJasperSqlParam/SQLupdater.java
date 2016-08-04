package updateJasperSqlParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import com.google.common.base.Throwables;

import daos.FileDAO;
import enums.CharValues;
import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRChild;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpressionChunk;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.base.JRBaseSubreport;
import net.sf.jasperreports.engine.util.JRLoader;

public class SQLupdater {

	// TODO : juiste pad in commentaar zetten !
	//private static final Path userPath = Paths.get("").toAbsolutePath();
	
	private static final Path userPath = Paths.get("D:\\Geert\\PROJECTEN\\JASPER_project\\jasperfiles").toAbsolutePath();
	// private static final Path userPath = Paths.get("C:\\DEV\\Servoy7\\application_server\\server\\webapps\\ROOT\\uploads\\reports").toAbsolutePath();

	public static final String currentRelativePath = userPath.toString();
	public static final String logPathAndFilename = currentRelativePath + "\\jasperSQLupdater.log";
	public static final String backupPathMain = currentRelativePath + "\\BU_jasperSQLupdater";
	public static String backupPathUsed = backupPathMain;
	public static Map<String, String> log = new HashMap<>();

	public static void main(String[] args) {

		try {

			String logInfo = "STARTING LOG" + CharValues.CRLF + new Date().toString() + CharValues.CRLF
					+ CharValues.CRLF;
			Files.write(Paths.get(SQLupdater.logPathAndFilename), logInfo.getBytes("utf-8"), StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);

			FileDAO.createBuFolders();
			FileDAO.moveFilesToBackupFolder("jrxml");
			FileDAO.copyFilesToBackupFolder("jasper");

			Set<String> jasperFilesNoExt = FileDAO.scanStructure("jasper", false, true);
			Set<String> jasperSubFilesNoExt = getSubReportNamesNoExt(jasperFilesNoExt);
			


			/*
			for (String pathAndFilenameNoExt : jasperFilesNoExt) {

				try {
					String jasperFilename = pathAndFilenameNoExt + ".jasper";
					JasperReport report = (JasperReport) JRLoader.loadObjectFromFile(jasperFilename);
					// JRXmlWriter.writeReport(report, pathAndFilenameNoExt +
					// ".jrxml", "UTF-8");
					String xmlStr = JasperCompileManager.writeReportToXml(report);
					System.out.println("\n****************\nfile = " + pathAndFilenameNoExt + ".jasper");

					String xmlModif = normaliseJRxml(xmlStr);
					if (xmlModif == null) {
						System.out.println("NO MODIF :  " + pathAndFilenameNoExt + ".jasper\n");
					} else {
						String jrxmlFilename = pathAndFilenameNoExt + ".jrxml";
						Files.write(Paths.get(jrxmlFilename), xmlModif.getBytes("utf-8"), StandardOpenOption.CREATE,
								StandardOpenOption.WRITE);
						FileDAO.writeJasper(pathAndFilenameNoExt);
						System.out.println("MODIFIED :  " + jasperFilename);
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
			*/

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
		int stopPosSqlParam = -1;
		String sqlParam = "";
		boolean sqlParamHasDefault = false;
		if (startPosSqlParam >= 0) {
			stopPosSqlParam = xml.indexOf(searchCloseTag, startPosSqlParam) + searchCloseTag.length();
			sqlParam = xml.substring(startPosSqlParam, stopPosSqlParam);
			// "/>" was not the endtag, </parameter> is :
			searchCloseTag = new String("</parameter>");
			if (sqlParam.toLowerCase().indexOf(searchCloseTag) >= 0) {
				stopPosSqlParam = xml.indexOf(searchCloseTag, startPosSqlParam) + searchCloseTag.length();
				sqlParamHasDefault = true;
			}
		}

		boolean sqlParamPresent = (startPosSqlParam > 0 && stopPosSqlParam > startPosSqlParam);
		if (!sqlParamPresent) {
			System.out.println("sqlParam not found :  start=" + startPosSqlParam + " / stop=" + stopPosSqlParam);
		} else {
			System.out.println("sqlParam found :  " + sqlParam);
		}
		
		

		String searchQueryStart = new String("<queryString").toLowerCase();
		String searchQueryEnd = new String("</queryString>").toLowerCase();
		int startPosQueryTag = xmlLC.indexOf(searchQueryStart);
		int stopPosQueryTag = xmlLC.indexOf(searchQueryEnd, startPosQueryTag) + searchQueryEnd.length();
		if (startPosQueryTag < 0 || stopPosQueryTag < 0) {
			throw new RuntimeException("No tag <queryString> not found in file");
		}
		String queryTag = xml.substring(startPosQueryTag, stopPosQueryTag);

		// System.out.println("\nqueryString ("+ (i++) +") :
		// start="+startPosQueryTag+" / stop="+stopPosQueryTag);
		// System.out.println("queryString found : " + queryTag);

		String searchCDATAstart = new String("<![CDATA[").toLowerCase();
		String searchCDATAend = new String("]]>");
		int startPosCDATA = queryTag.toLowerCase().indexOf(searchCDATAstart) + searchCDATAstart.length();
		int stopPosCDATA = queryTag.indexOf(searchCDATAend, startPosCDATA);
		String cdataQueryTag = queryTag.substring(startPosCDATA, stopPosCDATA);

		// System.out.println("\nCDATA ("+ (i++) +") : start="+startPosCDATA+" /
		// stop="+stopPosCDATA);
		System.out.println("CDATA in queryTag found :  " + cdataQueryTag);

		boolean sqlParamPresentInCDATA = cdataQueryTag.equals("$P!{sql}");
		// System.out.println("$P!{sql} = " + sqlParamPresentInCDATA);

		// PARAM sql present PARAM sql has default ("") & sqlParamPresentInCDATA
		if (sqlParamPresent && sqlParamHasDefault && sqlParamPresentInCDATA) {
			// nothing to do
			return null;
		}

		// NEW SQL PARAM
		StringBuilder newSQlParam = new StringBuilder();
		newSQlParam.append(
				"<parameter name=\"sql\" class=\"java.lang.String\" isForPrompting=\"false\">" + CharValues.CRLF);
		newSQlParam.append("<defaultValueExpression><![CDATA[\"");
		if (cdataQueryTag.length() > 0 && !sqlParamPresentInCDATA) {
			newSQlParam.append(convertStringToOneLine(cdataQueryTag));
		} else {
			newSQlParam.append("SELECT 1 as test");
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

	private static String convertStringToOneLine(String str) {

		if (str == null) {
			return "";
		}

		str = str.replaceAll("\"", "'");
		str = str.replaceAll("\\n", " ");
		str = str.replaceAll("\\r", " ");
		str = str.replaceAll("\\t", " ");
		int strLength = str.length();
		int afterReplaceLength = 0;
		while (strLength != afterReplaceLength) {
			strLength = str.length();
			str = str.replaceAll("  ", " ");
			afterReplaceLength = str.length();
		}

		return str;

	}
	
	private static Set<String> getSubReportNamesNoExt(Set<String> jasperFilesNoExt) {
		
		Set<String> jasperSubFilesNoExt = new HashSet<>();
		
		// MAIN REPORT OR SUB REPORT ?
		for (String pathAndFilenameNoExt : jasperFilesNoExt) {

			try {
				String jasperFilename = pathAndFilenameNoExt + ".jasper";
				JasperReport report = (JasperReport) JRLoader.loadObjectFromFile(jasperFilename);
				JRParameter parameters[] = report.getParameters();
				for (JRParameter parameter : parameters) {
					if (parameter.getName().equalsIgnoreCase("SUBREPORT_DIR")) {
						
						JRBand bands[] = report.getAllBands();
						for (JRBand band : bands) {
							List<JRChild> elements = band.getChildren();
							for (JRChild child : elements) {
							    if (child instanceof JRBaseSubreport){
							        JRBaseSubreport subreport = (JRBaseSubreport) child;
							        String expression= ""; //Lets find out the expression used
							        JRExpressionChunk[] chunks = subreport.getExpression().getChunks();
							        for (JRExpressionChunk c : chunks) {
							            expression += c.getText();
							        }
							        int subReportStartPos = expression.indexOf("\"");
							        int subReportEndPos = expression.toLowerCase().indexOf(".jasper");
							        if (subReportStartPos > -1 && subReportEndPos > subReportStartPos) {
							        	String subReportNameNoExt = expression.substring(subReportStartPos + 1, subReportEndPos);
							        	jasperSubFilesNoExt.add(subReportNameNoExt);
							        	System.out.println(pathAndFilenameNoExt + ": " + subReportNameNoExt); 
							        } else {
							        	System.err.println(pathAndFilenameNoExt + ": " + expression); 
							        }
							        
							        
							    }
							}
						}
						
					}
					//System.out.println(pathAndFilenameNoExt + ": " + parameter.getName() + " = " + parameter.getDefaultValueExpression().getText());
				}
				
			} catch (NoClassDefFoundError ex) {
				log.put(pathAndFilenameNoExt + ".jasper",
						CharValues.CRLF + Throwables.getStackTraceAsString(ex) + CharValues.CRLF);
			} catch (JRException e) {
				log.put(pathAndFilenameNoExt + ".jasper",
						CharValues.CRLF + Throwables.getStackTraceAsString(e) + CharValues.CRLF);
			}
		}
		return jasperSubFilesNoExt;
	}

}
