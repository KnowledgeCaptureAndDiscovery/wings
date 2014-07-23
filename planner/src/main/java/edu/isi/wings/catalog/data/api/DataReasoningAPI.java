package edu.isi.wings.catalog.data.api;

import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.metrics.Metrics;
import edu.isi.wings.ontapi.KBTriple;

import java.util.ArrayList;

/**
 * The interface used by the Workflow system to assist in Planning and
 * Generating workflows
 */
public interface DataReasoningAPI {
	// API to help in Workflow Planning and Generation

	ArrayList<VariableBindingsList> findDataSources(ArrayList<KBTriple> dods);

	Metrics findDataMetricsForDataObject(String dataObjectId);
	
	Metrics fetchDataMetricsForDataObject(String dataObjectId);

	String getDataLocation(String dataid);
	
	String getDefaultDataLocation(String dataid);

	String createDataIDFromKey(String hashkey, String prefix);

	String createDataIDFromMetrics(String id, String type, Metrics metrics);

	boolean checkDatatypeSubsumption(String dtypeid_subsumer, String dtypeid_subsumee);
	
}
