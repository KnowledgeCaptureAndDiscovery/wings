package edu.isi.wings.catalog.component;

import java.io.File;
import java.util.Properties;

import edu.isi.wings.catalog.component.api.ComponentCreationAPI;
import edu.isi.wings.catalog.component.api.ComponentReasoningAPI;
import edu.isi.wings.catalog.component.api.impl.kb.ComponentCreationKB;
import edu.isi.wings.catalog.component.api.impl.kb.ComponentReasoningKB;
import edu.isi.wings.common.kb.PropertiesHelper;

public class ComponentFactory {
	/**
	 * @param libname
	 *            the library file
	 * @param props
	 *            The properties for loading the Catalog (Optional). If props
	 *            are provided, it should contain the following keys:
	 *            lib.concrete.url, lib.concrete.rules.path lib.abstract.url,
	 *            lib.abstract.rules.path ont.domain.component.ns,
	 *            ont.domain.data.url, ont.component.url, ont.data.url,
	 *            ont.workflow.url tdb.repository.dir (optional)
	 * @return a ComponentReasoningAPI Instance
	 */
	public static ComponentReasoningAPI getReasoningAPI(Properties props) {
		if (props == null)
			props = createLegacyConfiguration();
		return new ComponentReasoningKB(props);
	}

	public static ComponentCreationAPI getCreationAPI(Properties props, boolean load_concrete) {
		if (props == null)
			props = createLegacyConfiguration();
		return new ComponentCreationKB(props, load_concrete);
	}

	/**
	 * @param libname
	 *            The library file to use (rest will be derived from wings
	 *            configuration file and using default values)
	 */
	public static Properties createLegacyConfiguration() {
		String libname = "library";
		Properties props = new Properties();
		// Create default urls for the Component Catalog
		String pcurl = PropertiesHelper.getPCURL();
		String pcdir = PropertiesHelper.getPCDir();
		String pcdomurl = PropertiesHelper.getPCDomainURL();
		String pcdomdir = PropertiesHelper.getPCDomainDir();
		props.put("lib.concrete.url", pcdomurl + "/" + libname + ".owl");
		props.put("lib.abstract.url", pcdomurl + "/abstract.owl");
		props.put("ont.domain.component.ns", pcdomurl + "/library.owl#");
		props.put("ont.component.url", pcurl + "/ontology.owl");
		props.put("lib.concrete.map", "file:" + pcdomdir + File.separator + libname + ".owl");
		props.put("lib.abstract.map", "file:" + pcdomdir + File.separator + "abstract.owl");
		props.put("ont.component.map", "file:" + pcdir + File.separator + "ontology.owl");

		// Add paths to the rule files
		props.put("lib.concrete.rules.path", pcdomdir + File.separator + libname + ".rules");
		props.put("lib.abstract.rules.path", pcdomdir + File.separator + "abstract.rules");

		// Add the default code directory
		props.put("lib.domain.code.storage", PropertiesHelper.getCodeDirectory());
		
		// The component catalog uses data properties in component rules
		// and so it needs to be aware of the data ontologies as well
		String dcurl = PropertiesHelper.getDCURL();
		String dcdomurl = PropertiesHelper.getDCDomainURL();
		String dcdir = PropertiesHelper.getDCDir();
		String dcdomdir = PropertiesHelper.getDCDomainDir();
		props.put("ont.domain.data.url", dcdomurl + "/ontology.owl");
		props.put("ont.data.url", dcurl + "/ontology.owl");
		props.put("ont.domain.data.map", "file:" + dcdomdir + File.separator + "ontology.owl");
		props.put("ont.data.map", "file:" + dcdir + File.separator + "ontology.owl");

		// The workflow url is used by the catalog for knowing what namespace it
		// uses so it can used to filter these out and to set send some
		// properties
		// back to the workflow generation/planning system
		String wflowurl = PropertiesHelper.getWorkflowOntologyURL();
		props.put("ont.workflow.url", wflowurl);

		return props;
	}
}
