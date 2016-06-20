package decompileJasper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import com.google.common.base.Throwables;

import daos.FileDAO;
import enums.CharValues;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

public class decompiler {

	public static String logPathAndFilename = System.getProperty("user.dir") + "\\jasperDecompiler.log";

	public static void main(String[] args) {

		try {

			Map<String, String> log = new HashMap<>();
			Set<String> jasperFilesNoExt = FileDAO.scanStructure();

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
