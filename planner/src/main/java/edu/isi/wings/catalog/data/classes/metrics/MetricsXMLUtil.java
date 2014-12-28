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

package edu.isi.wings.catalog.data.classes.metrics;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

public class MetricsXMLUtil {
	public static String HEADER = "<MetricResults xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
			+ " xsi:noNamespaceSchemaLocation=\"dc_results_draft.xsd\">\n";

	public static String FOOTER = "</MetricResults>";

	private static Object convertStringToPossibleJavaObject(String str, String type) {
		if ("String".equals(type)) {
			return str;
		} else if ("Boolean".equals(type)) {
			return Boolean.parseBoolean(str);
		} else if ("Integer".equals(type)) {
			return Integer.parseInt(str);
		} else if ("Float".equals(type)) {
			return Float.parseFloat(str);
		} else if ("XSDDateTime".equals(type)) {
			try {
				Calendar cal = DatatypeConverter.parseDate(str);
				cal.setTimeZone(TimeZone.getTimeZone("UTC"));
				XSDDateTime dt = new XSDDateTime(cal);
				dt.narrowType(XSDDatatype.XSDdate);
				return dt;
			} catch (Exception e) {
			}
		}

		return guessPossibleJavaObject(str);
	}

	private static Object guessPossibleJavaObject(String str) {
		if (str.toLowerCase().equals("false") || (str.toLowerCase().equals("true"))) {
			return Boolean.parseBoolean(str);
		}
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
		}
		try {
			return Float.parseFloat(str);
		} catch (Exception e) {
		}
		try {
			Calendar cal = DatatypeConverter.parseDate(str);
			cal.setTimeZone(TimeZone.getTimeZone("UTC"));
			XSDDateTime dt = new XSDDateTime(cal);
			dt.narrowType(XSDDatatype.XSDdate);
			return dt;
			// SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			// Calendar cal = Calendar.getInstance();
			// cal.setTime(formatter.parse(str));
			// return cal;
		} catch (Exception e) {
		}
		return str;
	}

	public static HashMap<String, Metric> parseMetricsXML(String metrics) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Convert ArrayList here to a Metric Class
		HashMap<String, Metric> propValMap = new HashMap<String, Metric>();

		String expression = "//MetricResults/Metric";
		if (metrics == null || metrics.equals("")) {
			return propValMap;
		}

		InputSource inputSource = new InputSource(new ByteArrayInputStream(metrics.getBytes()));
		try {
			NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource,
					XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				// Get element
				Element elem = (Element) nodes.item(i);
				String metricName = elem.getAttribute("name");
				String propName = null;
				Object propVal = null;
				for (int j = 0; j < elem.getChildNodes().getLength(); j++) {
					Node dim = (Node) elem.getChildNodes().item(j);
					if (dim.getNodeName().equals("Dimension")) {
						propName = ((Element) dim).getAttribute("name");
						for (int k = 0; k < dim.getChildNodes().getLength(); k++) {
							Node tmp = (Node) dim.getChildNodes().item(k);
							if (tmp.getNodeName().equals("Value")) {
								String valueType = ((Element) tmp).getAttribute("type");
								propVal = convertStringToPossibleJavaObject(tmp.getNodeValue(),
										valueType);
								break;
							}
						}
						break;
					}
				}
				if (propName != null && propVal != null) {
					int metricType = (metricName.equals("uri") ? Metric.URI : Metric.LITERAL);
					Metric m = new Metric(metricType, propVal);
					propValMap.put(propName, m);
				}
			}
		} catch (Exception e) {
			System.out.println(metrics);
			e.printStackTrace();
		}
		return propValMap;
	}

	public static String getMetricXML(Metrics metrics) {
		StringBuilder result = new StringBuilder();
		result.append(HEADER);

		HashMap<String, ArrayList<Metric>> mmap = metrics.getMetrics();
		for (String prop : mmap.keySet()) {
			for(Metric mm : mmap.get(prop)) {
  			String valdtype = null;
  			Object val = mm.getValue();
  			if (mm.getType() == Metric.LITERAL && val != null) {
  				if(mm.getDatatype() != null)
  					valdtype = mm.getDatatype();
  				else
  					valdtype = val.getClass().getSimpleName();
  			}
  			result.append(getMetricXML(prop, val.toString(), mm.getType(), valdtype));
			}
		}
		result.append(FOOTER);
		return result.toString();
	}

	public static String getMetricXML(String key, Object value, int typeint, String dtype) {
		StringBuilder result = new StringBuilder();

		String newLineChar = System.getProperty("line.separator");
		String metricBegin = "  <Metric name=\"";
		String metricEnd = "\">" + newLineChar;
		String metricBeginAlt = "  <Metric>";
		String metricClose = "  </Metric>" + newLineChar;
		String dimensionBegin = "    <Dimension name=\"";
		String dimensionEnd = "\">" + newLineChar;
		String dimensionClose = "    </Dimension>" + newLineChar;
		String valueBegin = "      <Value>";
		String valueClose = "</Value>" + newLineChar;
		String typedValueBegin = "      <Value type=\"";
		String typedValueEnd = "\">";

		String type = typeint == Metric.URI ? "uri"
				: (typeint == Metric.LITERAL ? "literal" : null);

		if (type != null) {
			result.append(metricBegin);
			result.append(type);
			result.append(metricEnd);
		} else {
			result.append(metricBeginAlt);
		}
		result.append(dimensionBegin);
		result.append(key);
		result.append(dimensionEnd);
		if (dtype != null) {
			result.append(typedValueBegin);
			result.append(dtype);
			result.append(typedValueEnd);
		} else
			result.append(valueBegin);

		result.append(getValueString(value));

		result.append(valueClose);
		result.append(dimensionClose);
		result.append(metricClose);

		return result.toString();
	}

	public static String getValueString(Object value) {
		return value.toString();
	}
}
