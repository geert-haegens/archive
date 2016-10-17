package updateJasperSqlParam;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.base.Throwables;

import daos.FileDAO;
import enums.CharValues;
import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRChild;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpressionChunk;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRQuery;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.base.JRBaseSubreport;
import net.sf.jasperreports.engine.util.JRLoader;

public class SQLupdater {

	// TODO : juiste pad in commentaar zetten !
	private static final Path userPath = Paths.get("").toAbsolutePath();

	// private static final Path userPath =
	// Paths.get("D:\\Geert\\PROJECTEN\\JASPER_project\\jasperfiles").toAbsolutePath();
	// private static final Path userPath =
	// Paths.get("C:\\DEV\\Servoy7\\application_server\\server\\webapps\\ROOT\\uploads\\reports").toAbsolutePath();
	//private static final Path userPath = Paths.get("D:\\Geert\\PROJECTEN\\JASPER_project\\demoServer\\test").toAbsolutePath();

	public static final String currentRelativePath = userPath.toString();
	public static final String logPathAndFilename = currentRelativePath + "\\jasperSQLupdater.log";
	public static final String backupPathMain = currentRelativePath + "\\BU_jasperSQLupdater";
	public static String backupPathUsed = backupPathMain;
	public static Map<String, String> log = new HashMap<>();
	private static Set<String> jasperSubFilesNoExt;

	public static void main(String[] args) {

		try {

			JOptionPane pane = new JOptionPane("Processing :", JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.DEFAULT_OPTION, null, new Object[] {}, null);
			pane.setSize(750, 100);
			pane.setMinimumSize(new Dimension(750, 100));
			pane.setPreferredSize(new Dimension(750, 100));
			JDialog dialog = pane.createDialog(null, "SQL updater");
			// pane.setMessage("new message");
			dialog.setModal(false);
			dialog.setResizable(true);
			dialog.setMinimumSize(new Dimension(750, 100));
			dialog.setPreferredSize(new Dimension(750, 100));
			dialog.setVisible(true);
			dialog.setAlwaysOnTop(true);

			String logInfo = "STARTING LOG" + CharValues.CRLF + new Date().toString() + CharValues.CRLF
					+ CharValues.CRLF;
			Files.write(Paths.get(SQLupdater.logPathAndFilename), logInfo.getBytes("utf-8"), StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);

			pane.setMessage("createBuFolders");
			FileDAO.createBuFolders();
			pane.setMessage("move *.jrxml files to BU folder");
			FileDAO.moveFilesToBackupFolder("jrxml");
			pane.setMessage("copy *.jasper files to BU folder");
			FileDAO.copyFilesToBackupFolder("jasper");

			pane.setMessage("Scan *.jasper files and check subreports");
			Set<String> jasperFilesNoExt = FileDAO.scanStructure("jasper", false, true);
			try {
				jasperSubFilesNoExt = getSubReportNamesNoExt(jasperFilesNoExt);
			} catch (RuntimeException e) {
				Files.write(Paths.get(SQLupdater.logPathAndFilename), Throwables.getStackTraceAsString(e).getBytes("utf-8"), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);
				JOptionPane.getRootFrame().dispose();
				JOptionPane.showMessageDialog(null, "logPath" + logPathAndFilename + CharValues.CRLF + Throwables.getStackTraceAsString(e), "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
			
			for (String pathAndFilenameNoExt : jasperFilesNoExt) {

				if (isSubReport(pathAndFilenameNoExt)) {
					// skip
					continue;
				}

				try {
					String jasperFilename = pathAndFilenameNoExt + ".jasper";
					JasperReport report = (JasperReport) JRLoader.loadObjectFromFile(jasperFilename);
					boolean sqlParamPresent = false;
					System.out.println("\n****************\nfile = " + pathAndFilenameNoExt + ".jasper");
					pane.setMessage("Processing :\n" + pathAndFilenameNoExt + ".jasper");

					// CHECK PARAM sql
					JRParameter parameters[] = report.getParameters();
					for (JRParameter parameter : parameters) {
						if (parameter.getName().equals("sql")) {
							sqlParamPresent = true;
							break;
						}
					}
					System.out.println("sqlParamPresent = " + sqlParamPresent);

					// CHECK query

					JRQuery query = report.getQuery();
					String queryText = query.getText();
					boolean queryIsPsql = queryText.equals("$P!{sql}");
					System.out.println("query equals $P!{sql} = " + queryIsPsql);

					if (sqlParamPresent && queryIsPsql) {
						System.out.println("NO MODIF :  " + pathAndFilenameNoExt + ".jasper");
						log.put(pathAndFilenameNoExt + ".jasper", "NO MODIF");
						continue;
					}

					// MODIFY report

					String xmlModif = modifyJRxml(report, sqlParamPresent, queryIsPsql);

					if (xmlModif == null) {
						System.out.println("NO MODIF :  " + pathAndFilenameNoExt + ".jasper\n");
						log.put(pathAndFilenameNoExt + ".jasper", "NO MODIF");
					} else {
						String jrxmlFilename = pathAndFilenameNoExt + ".jrxml";
						Files.write(Paths.get(jrxmlFilename), xmlModif.getBytes("utf-8"), StandardOpenOption.CREATE,
								StandardOpenOption.WRITE);
						FileDAO.writeJasper(pathAndFilenameNoExt);
						System.out.println("MODIFIED :  " + jasperFilename);
						log.put(pathAndFilenameNoExt + ".jasper", "MODIFIED");
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
			System.exit(0);

		} catch (RuntimeException e) {
			JOptionPane.showMessageDialog(null,
					"logPath" + logPathAndFilename + CharValues.CRLF + Throwables.getStackTraceAsString(e), "Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		} catch (Exception e) {
			log.put("UNKNOWN.jasper", CharValues.CRLF + Throwables.getStackTraceAsString(e) + CharValues.CRLF);
			JOptionPane.showMessageDialog(null,
					"logPath" + logPathAndFilename + CharValues.CRLF + Throwables.getStackTraceAsString(e), "Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		} 
	}

	static int i = 1;

	private static String modifyJRxml(JasperReport report, boolean sqlParamPresent, boolean queryIsPsql)
			throws RuntimeException {

		String xmlStr = JasperCompileManager.writeReportToXml(report);
		Document xml = null;

		// CONVERT string to XML doc
		try {
			DocumentBuilderFactory fctr = DocumentBuilderFactory.newInstance();
			DocumentBuilder bldr = fctr.newDocumentBuilder();
			InputSource insrc = new InputSource(new StringReader(xmlStr));
			xml = bldr.parse(insrc);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException | IOException e) {
			throw new RuntimeException(e);
		}

		Element root = xml.getDocumentElement();

		Node query = xml.getElementsByTagName("queryString").item(0);
		if (!queryIsPsql) {
			query.setTextContent("$P!{sql}");
		}

		if (!sqlParamPresent) {
			Element sqlParam = xml.createElement("parameter");
			sqlParam.setAttribute("name", "sql");
			sqlParam.setAttribute("class", "java.lang.String");
			sqlParam.setAttribute("isForPrompting", "false");

			Element parameterDescription = xml.createElement("parameterDescription");
			Node cdataPD = xml.createCDATASection("generated by jasperSQLupdater");
			parameterDescription.appendChild(cdataPD);
			sqlParam.appendChild(parameterDescription);

			Element defaultValueExpression = xml.createElement("defaultValueExpression");
			Node cdataDVE = xml.createCDATASection("\"SELECT 1 as test\"");
			defaultValueExpression.appendChild(cdataDVE);
			sqlParam.appendChild(defaultValueExpression);

			try {
				root.insertBefore(sqlParam, query);
			} catch (DOMException ex) {
				System.out.println(ex.toString());
				throw new RuntimeException("UNEXPECTED EXCEPTION while generating XML\n" + ex.toString());
			}

		}

		// CONVERT XML doc to string
		StringWriter sw = new StringWriter();
		try {
			DOMSource domSource = new DOMSource(xml);
			Transformer transformer;

			transformer = TransformerFactory.newInstance().newTransformer();

			StreamResult sr = new StreamResult(sw);
			transformer.transform(domSource, sr);

		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		return sw.toString();

	}

	private static Set<String> getSubReportNamesNoExt(Set<String> jasperFilesNoExt) throws RuntimeException {

		Set<String> jasperSubFilesNoExt = new HashSet<>();

		// MAIN REPORT OR SUB REPORT ?
		for (String pathAndFilenameNoExt : jasperFilesNoExt) {

			try {
				String jasperFilename = pathAndFilenameNoExt + ".jasper";
				JasperReport report = (JasperReport) JRLoader.loadObjectFromFile(jasperFilename);
				JRParameter parameters[] = report.getParameters();
				for (JRParameter parameter : parameters) {
					if (parameter.getName().equalsIgnoreCase("SUBREPORT_DIR")) {

						// get list of subreports
						StringBuilder subReportError = new StringBuilder();
						subReportError.append("MAIN REPORT = "+ pathAndFilenameNoExt + ".jasper" + CharValues.CRLF);
						subReportError.append("SUB REPORT EXPRESSIONS :" + CharValues.CRLF);
						List<String> subReportExpressions = new ArrayList<>();
						JRBand bands[] = report.getAllBands();
						for (JRBand band : bands) {
							List<JRChild> elements = band.getChildren();
							for (JRChild child : elements) {
								if (child instanceof JRBaseSubreport) {
									JRBaseSubreport subreport = (JRBaseSubreport) child;
									String expression = ""; // Lets find out the
									// expression used
									JRExpressionChunk[] chunks = subreport.getExpression().getChunks();
									for (JRExpressionChunk c : chunks) {
										expression += c.getText();
									}
									subReportExpressions.add(expression);
									subReportError.append("- " + expression + CharValues.CRLF);
								}
							}
						}
						
						for (String expression : subReportExpressions) {
							int subReportStartPos = expression.indexOf("\"");
							int subReportEndPos = expression.toLowerCase().indexOf(".jasper");
							if (subReportStartPos > -1 && subReportEndPos > subReportStartPos) {
								String subReportNameNoExt = expression.substring(subReportStartPos + 1,
										subReportEndPos);
								jasperSubFilesNoExt.add(subReportNameNoExt);
								// System.out.println(pathAndFilenameNoExt
								// + ": " + subReportNameNoExt);
							} else {
								System.err.println(pathAndFilenameNoExt + ": " + expression);
								StringBuilder error = new StringBuilder();
								error.append(CharValues.CRLF);
								error.append(CharValues.CRLF);
								error.append("PROGRAM TERMINATED" + CharValues.CRLF);
								error.append("File :  " + pathAndFilenameNoExt + ".jasper" + CharValues.CRLF);
								error.append("Unable to detect subreport references :  " + expression
										+ CharValues.CRLF);
								error.append(CharValues.CRLF);
								error.append("Please temporarily remove file and its subreports from folder : "
										+ CharValues.CRLF);
								error.append(subReportError);
								error.append("and manually modify the main report." + CharValues.CRLF);
								//log.put(pathAndFilenameNoExt + ".jasper", error.toString() + CharValues.CRLF);
								throw new RuntimeException(error.toString());
							}
						}

					}
					// System.out.println(pathAndFilenameNoExt + ": " +
					// parameter.getName() + " = " +
					// parameter.getDefaultValueExpression().getText());
				}

			} catch (NoClassDefFoundError ex) {
				log.put(pathAndFilenameNoExt + ".jasper",
						CharValues.CRLF + Throwables.getStackTraceAsString(ex) + CharValues.CRLF);
			} catch (JRException e) {
				log.put(pathAndFilenameNoExt + ".jasper",
						CharValues.CRLF + Throwables.getStackTraceAsString(e) + CharValues.CRLF);
			}
			catch (RuntimeException e) {
				log.put(pathAndFilenameNoExt + ".jasper", CharValues.CRLF + Throwables.getStackTraceAsString(e) + CharValues.CRLF);
				throw new RuntimeException(e.getMessage());
			}
		}

		try {
			StringBuilder logInfo = new StringBuilder();
			logInfo.append(CharValues.CRLF + "SUB REPORTS ( = non modified )" + CharValues.CRLF + CharValues.CRLF);
			for (String jasperSubFileNoExt : jasperSubFilesNoExt) {
				logInfo.append(jasperSubFileNoExt + ".jasper" + CharValues.CRLF);
			}
			logInfo.append(CharValues.CRLF);
			Files.write(Paths.get(SQLupdater.logPathAndFilename), logInfo.toString().getBytes("utf-8"),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return jasperSubFilesNoExt;
	}

	private static boolean isSubReport(String pathAndFilenameNoExt) {

		int start = pathAndFilenameNoExt.lastIndexOf("\\");
		String filenameNoExt = pathAndFilenameNoExt.substring(start + 1);
		return jasperSubFilesNoExt.contains(filenameNoExt);

	}

}
