package edu.isi.wings.catalog.data.api.impl.kb;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.catalog.data.classes.VariableBindings;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.metrics.Metric;
import edu.isi.wings.catalog.data.classes.metrics.Metrics;
import edu.isi.wings.catalog.data.classes.metrics.MetricsXMLUtil;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.common.logging.LoggerHelper;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.ontapi.SparqlQuery;
import edu.isi.wings.ontapi.SparqlQuerySolution;

public class DataReasoningKB extends DataKB implements DataReasoningAPI {
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Constructor
	 * 
	 * @param props
	 *            The properties should contain: lib.domain.data.url,
	 *            ont.domain.data.url, ont.data.url tdb.repository.dir
	 *            (optional)
	 */
	public DataReasoningKB(Properties props) {
		super(props, false);
	}

	/**
	 * <p/>
	 * Returns a list of data variables mapped to data source ids from the dc
	 * namespace. The data object descriptions explain constraints on and
	 * between data variables from a particular specialized template.
	 * <p/>
	 * 
	 * @param dods
	 *            data object descriptions from the dc namespaces mapped to data
	 *            variables in the sr namespace
	 * @param returnPartialBindings
	 *            if true, will return [dataVariableN null] if no mapping can be
	 *            found for a data variable, otherwise returns an empty array
	 *            list.
	 * @return data variables from the sr namespace mapped to data source ids
	 *         from the dc namespace
	 */
	@Override
	public ArrayList<VariableBindingsList> findDataSources(ArrayList<KBTriple> dods) {
		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(2);
			argumentMap.put("dods", dods);
			String arguments = LoggerHelper
					.getArgumentString("<findDataSources> q3.1", argumentMap);
			logger.debug(arguments);
		}

		if (dods.size() == 0)
			return null;

		ArrayList<VariableBindingsList> result;

		result = new ArrayList<VariableBindingsList>();

		SparqlQuery sq = sparqlFactory.makeSparqlQueryFromDataObjectDescriptions(dods);
		HashMap<String, KBObject> variableMap = sq.getVariableMap();
		String query = sq.getQuery();

		//System.out.println(query);
		ArrayList<ArrayList<SparqlQuerySolution>> queryResults = kb.sparqlQuery(query);
		for (ArrayList<SparqlQuerySolution> queryResult : queryResults) {
			VariableBindingsList listOfBindings = new VariableBindingsList();
			for (SparqlQuerySolution sparqlQuerySolution : queryResult) {
				String variableName = sparqlQuerySolution.getVariable();
				KBObject kboVariable = variableMap.get(variableName);
				KBObject dataObject = sparqlQuerySolution.getObject();
				VariableBindings dvdob = new VariableBindings(kboVariable, dataObject);
				listOfBindings.add(dvdob);
			}
			result.add(listOfBindings);
		}

		if (logger.isInfoEnabled()) {
			String returnString = LoggerHelper.getReturnString("<findDataSources> q3.1", result);
			logger.debug(returnString);
		}
		return result;
	}

	/**
	 * Q4.1 and Q8.2
	 * <p/>
	 * If metricsOrCharacteristics ArrayList is null or empty, then for the
	 * given dataSourceId this function returns an XML string that represents
	 * all the data metrics (and charateristics) of a the data source.
	 * 
	 * @param dataObjectId
	 *            the (unique) id of the dataObject
	 * @return a string of xml
	 */
	@Override
	public Metrics findDataMetricsForDataObject(String dataObjectId) {
		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(1);
			argumentMap.put("dataObjectId", dataObjectId);
			String arguments = LoggerHelper.getArgumentString(
					"<findDataMetricsForDataObject> q4.1", argumentMap);
			logger.debug(arguments);
		}

		Metrics result = new Metrics();

		KBObject dataObject = this.dataObjectForDataObjectNameOrId(dataObjectId);

		HashMap<String, KBObject> opmap = this.objPropMap;
		HashMap<String, KBObject> dpmap = this.dataPropMap;

		for (KBObject prop : kb.getSubPropertiesOf(opmap.get("hasMetrics"), false)) {
			KBObject val = kb.getPropertyValue(dataObject, prop);
			if (val != null) {
				result.addMetric(prop.getID(), new Metric(Metric.URI, val.getID()));
			}
		}
		for (KBObject prop : kb.getSubPropertiesOf(dpmap.get("hasDataMetrics"), false)) {
			KBObject val = kb.getDatatypePropertyValue(dataObject, prop);
			if (val != null && val.getValue() != null) {
				result.addMetric(prop.getID(), new Metric(Metric.LITERAL, val.getValue(), val.getDataType()));
			}
		}
		KBObject val = kb.getClassOfInstance(dataObject);
		if (val != null) {
			result.addMetric(KBUtils.RDF + "type", new Metric(Metric.URI, val.getID()));
		}

		if (logger.isInfoEnabled()) {
			String resultValue = LoggerHelper.getReturnString(
					"<findDataMetricsForDataObject> q4.1", "<some xml>");
			logger.debug(resultValue);
		}
		return result;
	}

	/**
	 * <p>
	 * Check if first class subsumes the second class
	 */
	public boolean checkDatatypeSubsumption(String subsumer, String subsumee) {
		KBObject class1 = kb.getConcept(subsumer);
		KBObject class2 = kb.getConcept(subsumee);
		if (kb.hasSubClass(class1, class2))
			return true;
		return false;
	}

	/**
	 * Create a DataID by transposing metric values onto the name format for the
	 * DataType
	 */
	public String createDataIDFromMetrics(String id, String type, Metrics metrics) {
		String nameformat = this.conceptNameFormat.get(type);
		if (nameformat != null && metrics != null) {
			HashMap<String, Metric> propValMap = metrics.getMetrics();
			Pattern pat = Pattern.compile("\\[(.+?)\\]");
			Matcher m = pat.matcher(nameformat);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String key = m.group(1);
				if (key.equals("__ID")) {
					m.appendReplacement(sb, id);
				} else {
					Metric tmp = propValMap.get(key);
					if (tmp != null && tmp.getValue() != null)
						m.appendReplacement(sb, MetricsXMLUtil.getValueString(tmp.getValue()));
					else
						m.appendReplacement(sb, "");
				}
			}
			m.appendTail(sb);
			return KBUtils.sanitizeID(sb.toString());
		}

		return null;
	}

	/**
	 * Create a DataID from the creation path
	 */
	public String createDataIDFromKey(String key, String prefix) {
		// Just returning a MD5 hash of the path
		if (key == null)
			return null;
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		m.update(key.getBytes(), 0, key.length());
		return KBUtils.sanitizeID(prefix + "-" + new BigInteger(1, m.digest()).toString(Character.MAX_RADIX));
		// return UuidGen.generateAUuid("");
	}
	
	private KBObject dataObjectForDataObjectNameOrId(String dataObjectNameOrId) {
		KBObject dataObject;
		if ((dataObject = kb.getIndividual(dataObjectNameOrId)) != null) {
			return dataObject;
		} else {
			dataObject = kb.getIndividual(this.dclibns + dataObjectNameOrId);
			return dataObject;
		}
	}

}