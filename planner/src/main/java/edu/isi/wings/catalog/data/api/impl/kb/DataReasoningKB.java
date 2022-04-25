/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import edu.isi.kcap.ontapi.KBObject;
import edu.isi.kcap.ontapi.KBTriple;
import edu.isi.kcap.ontapi.SparqlQuery;
import edu.isi.kcap.ontapi.SparqlQuerySolution;
import edu.isi.wings.catalog.data.api.DataReasoningAPI;
import edu.isi.wings.catalog.data.classes.VariableBindings;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.metrics.Metric;
import edu.isi.wings.catalog.data.classes.metrics.Metrics;
import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.common.logging.LoggerHelper;
import edu.isi.wings.workflow.plan.classes.ExecutionFile;

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
		super(props, false, false);
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
	public ArrayList<VariableBindingsList> findDataSources(ArrayList<KBTriple> dods, ArrayList<String> variableNamespaces) {
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

		SparqlQuery sq = sparqlFactory.makeSparqlQueryFromDataObjectDescriptions(dods, variableNamespaces);
		HashMap<String, KBObject> variableMap = sq.getVariableMap();
		String query = sq.getQuery();

		this.start_read();
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
		this.end();
		
		return result;
	}

	/**
	 * Q4.1 and Q8.2
	 * <p/>
	 * Given dataObjectId this function returns a Metrics object that represents
	 * all the data metrics (and charateristics) of a the data source.
	 * 
	 * @param dataObjectId
	 *            the (unique) id of the dataObject
	 * @return the Metrics object
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

		this.start_read();
		
		KBObject dataObject = this.dataObjectForDataObjectNameOrId(dataObjectId);
		if(dataObject == null)
		  return result;

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
		
		this.end();
		
		return result;
	}
	  
	 /**
   * Given dataObjectId this function Fetches Metrics from the .met file
   * 
   * @param dataObjectId
   *            the (unique) id of the dataObject
   * @return the Metrics object
   */
  @Override
  public Metrics fetchDataMetricsForDataObject(String dataObjectId) {
    Metrics metrics = new Metrics();
    ExecutionFile file = new ExecutionFile(dataObjectId);
    String loc = this.getDataLocation(dataObjectId);
    if (loc == null)
      loc = this.getDefaultDataLocation(dataObjectId);
    file.setLocation(loc);
    file.loadMetadataFromLocation();
    
    this.start_read();
    for (Object key : file.getMetadata().keySet()) {
      KBObject mprop = this.dataPropMap.get(key);
      String valstr = file.getMetadata().get(key).toString();
      if (mprop != null) {
        KBObject range = this.kb.getPropertyRange(mprop);
        KBObject valobj = this.kb.createXSDLiteral(valstr, range.getID());
        Object val = valobj.getValue();
        metrics.addMetric(mprop.getID(), new Metric(Metric.LITERAL, val));
      } else {
        mprop = this.objPropMap.get(key);
        if (mprop != null) {
          metrics.addMetric(mprop.getID(), new Metric(Metric.URI, valstr));
        } else {
          logger.debug(key + " is not a valid metadata property");
        }
      }
    }
    this.end();
    
    return metrics;
  }
	/**
	 * <p>
	 * Check if first class subsumes the second class
	 */
	public boolean checkDatatypeSubsumption(String subsumer, String subsumee) {
	  try {
  	  this.start_read();
  		KBObject class1 = kb.getConcept(subsumer);
  		KBObject class2 = kb.getConcept(subsumee);
  		if (kb.hasSubClass(class1, class2))
  			return true;
  		return false;
	  }
	  finally {
	    this.end();
	  }
	}

	/**
	 * Create a DataID by transposing metric values onto the name format for the
	 * DataType
	 */
	public String createDataIDFromMetrics(String id, String type, Metrics metrics) {
		String nameformat = this.conceptNameFormat.get(type);
		if (nameformat != null && metrics != null) {
			HashMap<String, ArrayList<Metric>> propValMap = metrics.getMetrics();
			Pattern pat = Pattern.compile("\\[(.+?)\\]");
			Matcher m = pat.matcher(nameformat);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String key = m.group(1);
				if (key.equals("__ID")) {
					m.appendReplacement(sb, id);
				} else {
				  if(propValMap.containsKey(this.dcdomns + key)) {
  					for(Metric tmp : propValMap.get(this.dcdomns + key)) {
    					if (tmp != null && tmp.getValue() != null)
    						m.appendReplacement(sb, tmp.getValueAsString());
    					else
    						m.appendReplacement(sb, "");
  					}
				  }
				  else {
				    m.appendReplacement(sb, "");
				  }
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
