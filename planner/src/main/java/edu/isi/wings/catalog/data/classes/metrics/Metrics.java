package edu.isi.wings.catalog.data.classes.metrics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Metrics implements Serializable {
	private static final long serialVersionUID = 1L;

	HashMap<String, ArrayList<Metric>> metrics;

	public Metrics() {
		metrics = new HashMap<String, ArrayList<Metric>>();
	}

	public Metrics(Metrics m) {
		metrics = new HashMap<String, ArrayList<Metric>>();
		HashMap<String, ArrayList<Metric>> om = m.getMetrics();
		for (String oprop : om.keySet()) {
			for(Metric omm : om.get(oprop)) {
  			Metric nmm = new Metric(omm.getType(), omm.getValue());
  			nmm.setDatatype(omm.getDatatype());
  			this.addMetric(oprop, nmm);
			}
		}
	}

	public HashMap<String, ArrayList<Metric>> getMetrics() {
		return this.metrics;
	}

	public void addMetric(String prop, Metric m) {
	  ArrayList<Metric> pms = metrics.get(prop);
	  if(pms == null)
	    pms = new ArrayList<Metric>();
	  pms.add(m);
		metrics.put(prop, pms);
	}

	public String toString() {
		return metrics.toString();
	}
}
