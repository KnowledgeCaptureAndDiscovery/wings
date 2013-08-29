package edu.isi.wings.catalog.data;

import java.util.Properties;

import edu.isi.wings.catalog.data.api.*;
import edu.isi.wings.catalog.data.api.impl.kb.*;
import edu.isi.wings.common.kb.PropertiesHelper;

public class DataFactory {
	/**
	 * @param props
	 *            The properties should contain: lib.domain.data.url,
	 *            ont.domain.data.url, ont.data.url tdb.repository.dir
	 *            (optional)
	 */
	public static DataReasoningAPI getReasoningAPI(Properties props) {
		if (props == null)
			props = createLegacyConfiguration();
		return new DataReasoningKB(props);
	}

	/**
	 * @param props
	 *            The properties should contain: lib.domain.data.url,
	 *            ont.domain.data.url, ont.data.url tdb.repository.dir
	 *            (optional)
	 */
	public static DataCreationAPI getCreationAPI(Properties props) {
		if (props == null)
			props = createLegacyConfiguration();
		return new DataCreationKB(props);
	}

	/**
	 * Here, Properties are derived from wings configuration file and with some
	 * default values
	 */
	public static Properties createLegacyConfiguration() {
		String dcurl = PropertiesHelper.getDCURL();
		String dcdomurl = PropertiesHelper.getDCDomainURL();
		String dcdir = PropertiesHelper.getDCDir();
		String dcdomdir = PropertiesHelper.getDCDomainDir();

		// Create default urls
		Properties props = new Properties();
		props.put("lib.domain.data.url", dcdomurl + "/library.owl");
		props.put("ont.domain.data.url", dcdomurl + "/ontology.owl");
		props.put("ont.data.url", dcurl + "/ontology.owl");

		// Add the default data directory
		props.put("lib.domain.data.storage", PropertiesHelper.getDataDirectory());
		
		props.put("lib.domain.data.map", "file:" + dcdomdir + "/library.owl");
		props.put("ont.domain.data.map", "file:" + dcdomdir + "/ontology.owl");
		props.put("ont.data.map", "file:" + dcdir + "/ontology.owl");
		return props;
	}
}
