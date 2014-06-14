package net.bedita.export.jasper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.query.JRXPathQueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRXmlUtils;

import org.w3c.dom.Document;

public class JasperProcess {


	/**
	 * @param args
	 * @throws JRException 
	 * @throws IOException 
	 */
    public static void main(String[] args) throws JRException, IOException {
        // read params from
        String reportPath = args[0];
        String sep = File.separator;
        String report = reportPath + sep + "CustomersReport.jasper";
        String[] subReports = {reportPath + sep + "OrdersReport.jasper"};
        String xmlFile = reportPath + sep + "northwind.xml";
        String destFile = reportPath + sep + "customers.pdf";

        JasperProcess jasperProc = new JasperProcess();
        jasperProc.compileReports(report, subReports);
        Map<String, Object> params = jasperProc.readDataFile(xmlFile);
        jasperProc.pdf(report, params, destFile);
	}
    
    public void compileReports(String report, String[] subReports) throws IOException, JRException {
    	compileReport(report);
    	for (String subRep : subReports) {
    		compileReport(subRep);
    	}
    }

    public Map<String, Object> readDataFile(String xmlDataFile) throws JRException {
        Document document = JRXmlUtils.parse(JRLoader.getLocationInputStream(xmlDataFile));
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JRXPathQueryExecuterFactory.PARAMETER_XML_DATA_DOCUMENT, document);
        params.put(JRXPathQueryExecuterFactory.XML_DATE_PATTERN, "yyyy-MM-dd");
        params.put(JRXPathQueryExecuterFactory.XML_NUMBER_PATTERN, "#,##0.##");
        params.put(JRXPathQueryExecuterFactory.XML_LOCALE, Locale.ENGLISH);
        params.put(JRParameter.REPORT_LOCALE, Locale.US);
        return params;
    }
    
    public void pdf(String reportFile, Map<String, Object> params, String destFile) throws JRException, IOException {
        JasperPrint print = JasperFillManager.fillReport(reportFile, params);
        JasperExportManager.exportReportToPdfFile(print, destFile);
    }
	
    /**
     * Compile jasper .jrxml source file if compiled .jasper is missing or older than source
     * 
     * @param filename
     * @return JasperReport
     * @throws IOException
     * @throws JRException
     */
    private void compileReport(String jasperFileName) throws IOException, JRException {
        if (!jasperFileName.endsWith(".jasper")) {
        	throw new JRException("Bad file extension, should be .jasper: " + jasperFileName);
        }
        String sourceFileName = jasperFileName.substring(0, jasperFileName.length()-7) + ".jrxml";
        File source = new File(sourceFileName);
        File compiled = new File(jasperFileName);
        if (!source.exists()) {
            throw new IOException("Missing source JRXML file " + sourceFileName);
        }
        
        if(!compiled.exists() || (compiled.lastModified() < source.lastModified())) {
            JasperCompileManager.compileReportToFile(sourceFileName, jasperFileName);
        }
    }

}
