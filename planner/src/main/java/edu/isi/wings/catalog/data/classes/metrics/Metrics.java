package edu.isi.wings.catalog.data.classes.metrics;

import java.io.Serializable;
import java.util.HashMap;

public class Metrics implements Serializable {
	private static final long serialVersionUID = 1L;

	HashMap<String, Metric> metrics;

	public Metrics() {
		metrics = new HashMap<String, Metric>();
	}

	public Metrics(Metrics m) {
		metrics = new HashMap<String, Metric>();
		HashMap<String, Metric> om = m.getMetrics();
		for (String oprop : om.keySet()) {
			Metric omm = om.get(oprop);
			Metric nmm = new Metric(omm.getType(), omm.getValue());
			nmm.setDatatype(omm.getDatatype());
			this.addMetric(oprop, nmm);
		}
	}

	public HashMap<String, Metric> getMetrics() {
		return this.metrics;
	}

	public void addMetric(String prop, Metric m) {
		metrics.put(prop, m);
	}

	public String toString() {
		return metrics.toString();
	}
}
