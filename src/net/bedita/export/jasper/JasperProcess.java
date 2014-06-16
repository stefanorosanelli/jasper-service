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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Document;

public class JasperProcess {


	/**
	 * @param args
	 * @throws JRException 
	 * @throws IOException 
	 * @throws ParseException 
	 */
    public static void main(String[] args) throws JRException, IOException, ParseException {

        // create the parser
        CommandLineParser parser = new BasicParser();
        Options options = initOptions();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
        	HelpFormatter formatter = new HelpFormatter();
        	formatter.printHelp("jasper-service", options, true);
        }
        
        String report = cmd.getOptionValue("r");
        String sub = cmd.getOptionValue("s");
        String[] subReports = {};
        if (sub != null) {
        	subReports = sub.split(",");
        }
        String xmlFile = cmd.getOptionValue("d");
        String destFile = cmd.getOptionValue("o");
        
//        String reportPath = args[0];
//        String sep = File.separator;
//        String report = reportPath + sep + "CustomersReport.jasper";
//        String[] subReports = {reportPath + sep + "OrdersReport.jasper"};
//        String xmlFile = reportPath + sep + "northwind.xml";
//        String destFile = reportPath + sep + "customers.pdf";

        JasperProcess jasperProc = new JasperProcess();
        jasperProc.compileReports(report, subReports);
        Map<String, Object> params = jasperProc.readDataFile(xmlFile);
        jasperProc.pdf(report, params, destFile);
    }

    /**
     * Init options, parse arguments
     */
    static private Options initOptions() {
        Options options = new Options();
        options.addOption("r", "report", true, "jasper report file path (.jasper file), absolute or relative");
        options.addOption("d", "data-file", true, "data file path (i.e. .xml data file");
        options.addOption("o", "output", true, "output file path");
        options.addOption("s", "sub-reports", false, "comma saparated list of jasper subreports");
        options.addOption("h", "help", false, "help message");
        return options;
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
