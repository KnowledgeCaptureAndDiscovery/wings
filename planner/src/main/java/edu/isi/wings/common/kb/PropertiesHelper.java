package edu.isi.wings.common.kb;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import edu.isi.wings.common.logging.LoggerHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesHelper {
	public final static String WINGS_PROPERTIES_FILE = "wings.properties";

	private static Properties conf;
	private static String properties_dir;

	private static HashMap<String, String> pcnsmap = null;
	private static HashMap<String, String> dcnsmap = null;

	private static HashMap<String, Logger> classLoggers = new HashMap<String, Logger>();

	private static String ontdir, logdir, resdir, opdir;

	private static boolean dont_use_logging = false;

	public static boolean createDir(String dir) {
		if (dir == null)
			return false;
		File f = new File(dir);
		if (f.exists() && !f.isDirectory()) {
			Logger.getLogger(PropertiesHelper.class).error(
					"Error: '" + f.getAbsolutePath() + "' is not a directory !");
			return false;
		}
		if (!f.exists() && !f.mkdir()) {
			Logger.getLogger(PropertiesHelper.class).error(
					"Error: Cannot create directory '" + f.getAbsolutePath() + "'");
			return false;
		}
		return true;
	}

	public static void disableLogging() {
		dont_use_logging = true;
	}

	public static boolean getLoggingStatus() {
		return dont_use_logging;
	}

	public static File getDir(String dir) {
		if (dir == null)
			return null;
		File f = new File(dir);
		if (f.exists() && f.isDirectory()) {
			return f;
		}
		return null;
	}

	public static String getProposedLogFileName(String id) {
		if (!createDir(PropertiesHelper.getLogDir())) {
			String tmpdir = System.getProperty("java.io.tmpdir");
			Logger.getLogger(PropertiesHelper.class).error(
					"Using temporary directory for logging: " + tmpdir);
			PropertiesHelper.setLogDir(tmpdir);
		}
		return PropertiesHelper.getLogDir() + "/" + id + ".log";
	}

	private static void addFileAppender(Logger logger, String id) {
		if (logger.getAppender(id) != null)
			return;

		String filename = getProposedLogFileName(id);
		try {
			FileAppender fileappender = new FileAppender(new SimpleLayout(), filename);
			fileappender.setAppend(false);
			fileappender.setName(id);
			logger.addAppender(fileappender);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Logger getLogger(String classname, String id) {
		Logger logger = classLoggers.get(classname);
		if (logger == null) {
			logger = Logger.getLogger(classname);
			classLoggers.put(classname, logger);

			if (id != null && !dont_use_logging)
				addFileAppender(logger, id);
		}
		return logger;
	}

	public static void removeLogger(String classname) {
		Logger logger = classLoggers.get(classname);
		if (logger != null) {
			classLoggers.remove(classname);
			logger.removeAllAppenders();
		}
	}

	public static Properties loadWingsProperties() {
		return loadWingsProperties(null);
	}

	/**
	 * resets the Properities conf
	 */
	public static void resetProperties() {
		ontdir = logdir = resdir = opdir = null;
		conf = null;
		pcnsmap = dcnsmap = null;
		classLoggers.clear();
		dont_use_logging = false;
	}

	public static Properties loadWingsProperties(String properties_file) {
		if (conf != null)
			return conf;

		if (properties_file == null)
			properties_file = LoggerHelper.getPathToProperties(WINGS_PROPERTIES_FILE);

		conf = loadProperties(properties_file);

		// Store properties file dir (for relative path resolution)
		properties_dir = new File(properties_file).getParentFile().getAbsolutePath();
		return conf;
	}

	private static Properties loadProperties(String properties_file) {
		if (properties_file == null)
			return null;
		Properties conf = new Properties();
		try {
			// System.out.println("Loading config from " + properties_file);
			conf.load(new FileInputStream(properties_file));
		} catch (IOException e) {
			Logger.getLogger(PropertiesHelper.class).error(
					"Error loading config file: " + properties_file + ", Cannot open");
			return null;
		}
		return conf;
	}

	public static String getPCDomain() {
		// Get DC and PC Domain from Properties File
		loadWingsProperties();
		String PCDomain = conf.getProperty("pc.domain");
		return PCDomain;
	}

	public static String getDCDomain() {
		loadWingsProperties();
		String DCDomain = conf.getProperty("dc.domain");
		return DCDomain;
	}

	public static String getTemplateDomain() {
		loadWingsProperties();
		String TemplateDomain = conf.getProperty("template.domain");
		return TemplateDomain;
	}

	public static String getGraphvizPath() {
		loadWingsProperties();
		return abspath(conf.getProperty("graphviz.path"));
	}

	public static void setLogDir(String dir) {
		loadWingsProperties();
		logdir = dir;
		conf.setProperty("logs.dir", dir);
	}

	public static String getLogDir() {
		loadWingsProperties();
		if (logdir != null)
			return logdir;

		logdir = abspath(conf.getProperty("logs.dir"));
		return logdir;
	}

	public static void setOutputDir(String dir) {
		loadWingsProperties();
		opdir = dir;
		conf.setProperty("output.dir", dir);
	}

	public static String getOutputDir() {
		loadWingsProperties();
		if (opdir != null)
			return opdir;
		opdir = abspath(conf.getProperty("output.dir"));
		return opdir;
	}

	public static void setResourceDir(String dir) {
		loadWingsProperties();
		resdir = dir;
		conf.setProperty("resource.dir", dir);
	}

	public static String getResourceDir() {
		loadWingsProperties();
		if (resdir != null)
			return resdir;

		resdir = abspath(conf.getProperty("resource.dir"));
		return resdir;
	}

	public static String getOntologyDir() {
		loadWingsProperties();
		if (ontdir != null)
			return ontdir;

		ontdir = abspath(conf.getProperty("ontology.root.dir"));
		return ontdir;
	}

	public static void setOntologyDir(String dir) {
		loadWingsProperties();
		ontdir = dir;
		conf.setProperty("ontology.root.dir", dir);
	}

	public static String getOntologyURL() {
		loadWingsProperties();
		return conf.getProperty("ontology.root.url");
	}

	public static String getWorkflowOntologyPath() {
		loadWingsProperties();
		return conf.getProperty("ontology.wflow.path");
	}

	public static String getWorkflowOntologyURL() {
		return getOntologyURL() + "/" + getWorkflowOntologyPath();
	}

	public static boolean getProvenanceFlag() {
		loadWingsProperties();
		try {
			return Boolean.parseBoolean(conf.getProperty("storeprovenance"));
		} catch (Exception e) {
			return false;
		}
	}

	public static int getTrimmingNumber() {
		loadWingsProperties();
		try {
			return Integer.parseInt(conf.getProperty("trimming.numworkflows"));
		} catch (Exception e) {
			return 0;
		}
	}

	public static String getOutputFormat() {
		loadWingsProperties();
		String oformat = conf.getProperty("output.format");
		if (oformat == null)
			oformat = "xml";
		return oformat;
	}

	private static HashMap<String, String> getKeyValueMap(String pref, String suf) {
		HashMap<String, String> map = new HashMap<String, String>();
		String fac = conf.getProperty(pref + ".factory");
		String dom = conf.getProperty(pref + ".domain");
		String pattern = pref + "\\." + fac + "\\.([^\\.]+)\\." + suf + "\\.(.+)";
		Pattern pat = Pattern.compile(pattern);
		for (Object o : conf.keySet()) {
			String s = (String) o;
			Matcher m = pat.matcher(s);
			if (m.find()) {
				String mdom = m.group(1);
				String mkey = m.group(2);
				String value = conf.getProperty(s);
				if (mdom.equals(dom))
					map.put(mkey, value);
				else if (mdom.equals("*") || !map.containsKey(mkey))
					map.put(mkey, value);
			}
		}
		return map;
	}

	public static HashMap<String, String> getDCPrefixNSMap() {
		loadWingsProperties();
		if (dcnsmap == null)
			dcnsmap = getKeyValueMap("dc", "ns");
		return dcnsmap;
	}

	public static HashMap<String, String> getPCPrefixNSMap() {
		loadWingsProperties();
		if (pcnsmap == null)
			pcnsmap = getKeyValueMap("pc", "ns");
		return pcnsmap;
	}

	private static String getPropertyForCurrentDomain(String pref, String prop) {
		loadWingsProperties();
		String fac = conf.getProperty(pref + ".factory");
		String dom = conf.getProperty(pref + ".domain");
		String tmp = conf.getProperty(pref + "." + fac + "." + dom + "." + prop);
		if (tmp == null)
			tmp = conf.getProperty(pref + "." + fac + ".*." + prop);
		return tmp;
	}

	public static String getDCPropertyForCurrentDomain(String prop) {
		String value = getPropertyForCurrentDomain("dc", prop);
		if(prop.endsWith(".dir"))
			return abspath(value);
		return value;
	}

	public static String getPCPropertyForCurrentDomain(String prop) {
		String value = getPropertyForCurrentDomain("pc", prop);
		if(value != null && prop.endsWith(".dir"))
			return abspath(value);
		return value;
	}

	public static String getCodeDirectory() {
		String val = getPCPropertyForCurrentDomain("components.dir");
		if(val == null)
			return abspath("code");
		return val;
	}
	
	public static String getDataDirectory() {
		String val = getPCPropertyForCurrentDomain("data.dir");
		if(val == null)
			return abspath("data");
		return val;
	}
	
	public static String getPCDomainDir() {
		String dirstr = PropertiesHelper.getPCPropertyForCurrentDomain("directory");
		if (dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getPCDir() + "/" + getPCDomain();
	}

	public static String getDCDomainDir() {
		String dirstr = PropertiesHelper.getDCPropertyForCurrentDomain("directory");
		if (dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getDCDir() + "/" + getDCDomain();
	}

	public static String getTemplatesDir() {
		String dirstr = conf.getProperty("template." + getTemplateDomain() + ".directory");
		if (dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getOntologyDir() + "/" + getTemplateDomain();
	}

	public static String getSeedsDir() {
		String dirstr = conf.getProperty("seed." + getTemplateDomain() + ".directory");
		if (dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getOntologyDir() + "/" + getTemplateDomain() + "/seeds";
	}

	public static String getPCDir() {
		return getOntologyDir() + "/ac";
	}

	public static String getDCDir() {
		return getOntologyDir() + "/dc";
	}

	public static String getPCURL() {
		return getOntologyURL() + "/ac";
	}

	public static String getDCURL() {
		return getOntologyURL() + "/dc";
	}

	public static String getPCDomainURL() {
		return getPCURL() + "/" + getPCDomain();
	}

	public static String getDCDomainURL() {
		return getDCURL() + "/" + getDCDomain();
	}

	public static String getTemplateURL() {
		return getOntologyURL() + "/" + getTemplateDomain();
	}

	public static String getSeedURL() {
		return getTemplateURL() + "/seeds";
	}

	public static String getPCNewComponentPrefix() {
		return getPCPropertyForCurrentDomain("componentns");
	}

	public static String getDCNewDataPrefix() {
		return getDCPropertyForCurrentDomain("datans");
	}

	public static String getQueryNamespace() {
		loadWingsProperties();
		return conf.getProperty("query.ns.wfq");
	}

	public static String getQueryVariablesNamespace() {
		loadWingsProperties();
		return conf.getProperty("query.ns.wfqv");
	}

	private static String abspath(String path) {
		if (path.startsWith("/"))
			return path;
		else
			return properties_dir + "/" + path;
	}

	/**
	 * Set domain paths dynamically from a given directory
	 */
	public static boolean setDomainDir(String dirstr) {
		File dir = getDir(dirstr);
		if (dir == null) {
			Logger.getLogger(PropertiesHelper.class).error(
					"Error: Domain Directory '" + dirstr + "' does not Exist !");
			return false;
		}
		dirstr = dir.getAbsolutePath();
		Properties props = loadProperties(dirstr + "/domain.properties");
		if (props == null) {
			Logger.getLogger(PropertiesHelper.class).error(
					"Domain isn't configured correctly. domain.properties file is missing");
			return false;
		}
		loadWingsProperties();

		// Override Wings Properties
		for (Object prop : props.keySet()) {
			if (prop != null)
				conf.setProperty(prop.toString(), props.getProperty(prop.toString()));
		}
		conf.setProperty("pc.internal." + getPCDomain() + ".directory", dirstr
				+ "/component_catalog");
		conf.setProperty("dc.internal." + getDCDomain() + ".directory", dirstr + "/data_catalog");
		conf.setProperty("pc.internal." + getPCDomain() + ".components.dir", dirstr
				+ "/component_catalog/bin");
		conf.setProperty("dc.internal." + getDCDomain() + ".data.dir", dirstr
				+ "/data_catalog/data");
		conf.setProperty("template." + getTemplateDomain() + ".directory", dirstr + "/templates");
		conf.setProperty("seed." + getTemplateDomain() + ".directory", dirstr + "/templates/seeds");
		// Re-initialize namespace maps
		pcnsmap = null;
		dcnsmap = null;
		return true;
	}
}
