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
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.query.JRXPathQueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

public class JasperProcess {

    protected final Log log = LogFactory.getLog(getClass());
    static protected Options options;
    
	/**
	 * @param args
	 * @throws JRException 
	 * @throws IOException 
	 * @throws ParseException 
	 */
    public static void main(String[] args) throws JRException, IOException {

		try {
			// create the parser
			CommandLineParser parser = new BasicParser();
			Options options = initOptions();
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				usage();
				return;
			}

			String report = cmd.getOptionValue("r");
			String sub = cmd.getOptionValue("s");
			String[] subReports = {};
			if (sub != null) {
				subReports = sub.split(",");
			}
			String dataFile = cmd.getOptionValue("d");
			String destFile = cmd.getOptionValue("o");
			String userParamsStr = cmd.getOptionValue("p");
			Map<String, String> userParams = new HashMap<String, String>();
			if (userParamsStr != null) {
				String[] p = {};
				p = userParamsStr.split(",");
				for (String kv : p) {
					String[] keyVal = kv.split("=");
					if (keyVal.length != 2) {
						throw new ParseException("Bad parameter: " + kv);
					}
					userParams.put(keyVal[0], keyVal[1]);
				}
			}
			
			JasperProcess jasperProc = new JasperProcess();
			jasperProc.generate(report, subReports, dataFile, destFile, userParams);
			System.out.println("output file created: " + destFile);

		} catch (ParseException ex) {
			System.out.println("jasper-service stopped with errors");			
			System.err.println("error parsing commandline: " + ex.getMessage());
			usage();
		}
    }


    static public void usage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("jasper-service", options, true);    	
    }
    
    public void generate(String report, String[] subReports, String dataFile, 
    		String destFile, Map<String, String> userParams) throws IOException, JRException {
    	// compile if needed
    	compileReports(report, subReports);
    	// prepare params
        Map<String, Object> params = readDataFile(dataFile);
        for (String k : userParams.keySet()) {
        	params.put(k, userParams.get(k));
        }
    	log.debug("generating print data");
        JasperPrint print = JasperFillManager.fillReport(report, params);

        log.debug("exporting to file: " + destFile);
        String extension = destFile.substring(destFile.lastIndexOf('.')+1).toLowerCase();
        if("pdf".equals(extension)) {
        	log.debug("using pdf format");
            pdf(print, destFile);
        } else if("docx".equals(extension)) {
        	log.debug("using docx format");
            docx(print, destFile);
        } else if("rtf".equals(extension)) {
        	log.debug("using rtf format");
            rtf(print, destFile);
        } else if("odt".equals(extension)) {
        	log.debug("using odt format");
            odt(print, destFile);
        }  else {
        	log.warn("unsupported format for file: " + destFile);
        	System.out.println("unsupported format for file: " + destFile);
        	System.out.println("supported formats: pdf, docx, rtf and odt");        	
        }
    }
    
    /**
     * Init options, parse arguments
     */
    static private Options initOptions() {
        options = new Options();
        // report option
        Option r = new Option("r", "report", true, "jasper report file path (.jasper file), absolute or relative");
        r.setRequired(true);
        options.addOption(r);
        // data file option
        Option d = new Option("d", "data-file", true, "data file path (i.e. .xml data file");
        d.setRequired(true);
        options.addOption(d);
        // param option
        Option p = new Option("p", "param", true, "comma separated list of params in this form: name1=value1,name2=value2");
        options.addOption(p);
        // output file option
        Option o = new Option("o", "output", true, "output file path");
        o.setRequired(true);
        options.addOption(o);
        // subreport files option
        Option s = new Option("s", "sub-reports", true, "comma saparated list of jasper subreports file paths");
        options.addOption(s);

        options.addOption("h", "help", false, "this help message");
        return options;
    }
    
    protected void compileReports(String report, String[] subReports) throws IOException, JRException {
    	log.info("compiling report: " + report);
    	compileReport(report);
    	for (String subRep : subReports) {
        	log.info("compiling subreport: " + subRep);
    		compileReport(subRep);
    	}
    }

    protected Map<String, Object> readDataFile(String xmlDataFile) throws JRException {
        Document document = JRXmlUtils.parse(JRLoader.getLocationInputStream(xmlDataFile));
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JRXPathQueryExecuterFactory.PARAMETER_XML_DATA_DOCUMENT, document);
        params.put(JRXPathQueryExecuterFactory.XML_DATE_PATTERN, "yyyy-MM-dd");
        params.put(JRXPathQueryExecuterFactory.XML_NUMBER_PATTERN, "#,##0.##");
        params.put(JRXPathQueryExecuterFactory.XML_LOCALE, Locale.ENGLISH);
        params.put(JRParameter.REPORT_LOCALE, Locale.US);
        return params;
    }

    protected void pdf(JasperPrint print, String destFile) throws JRException, IOException {
    	log.debug("exporting to pdf file: " + destFile);
        JasperExportManager.exportReportToPdfFile(print, destFile);
    }
	
    protected void docx(JasperPrint print, String destFile) throws JRException, IOException {
    	log.debug("exporting to docx file: " + destFile);
    	JRDocxExporter exporter = new JRDocxExporter();
		exporter.setExporterInput(new SimpleExporterInput(print));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));		
		exporter.exportReport();
    }
	
    protected void odt(JasperPrint print, String destFile) throws JRException, IOException {
    	log.debug("exporting to odt file: " + destFile);
    	JROdtExporter exporter = new JROdtExporter();
		exporter.setExporterInput(new SimpleExporterInput(print));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
		exporter.exportReport();
   }
    
    protected void rtf(JasperPrint print, String destFile) throws JRException, IOException {
    	log.debug("exporting to rtf file: " + destFile);
    	JRRtfExporter exporter = new JRRtfExporter();
		exporter.setExporterInput(new SimpleExporterInput(print));
		exporter.setExporterOutput(new SimpleWriterExporterOutput(destFile));
		exporter.exportReport();
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
        	log.debug("compiling source report: " + sourceFileName);
            JasperCompileManager.compileReportToFile(sourceFileName, jasperFileName);
        } else {
        	log.debug("source report compilation not needed");        	
        }
    }

}
