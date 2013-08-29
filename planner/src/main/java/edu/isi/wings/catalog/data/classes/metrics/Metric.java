package edu.isi.wings.catalog.data.classes.metrics;

import java.io.Serializable;

public class Metric implements Serializable {
	private static final long serialVersionUID = 1L;

	private int type;
	private String datatype;
	private Object value;

	public static int URI = 1;
	public static int LITERAL = 2;

	public Metric(int type, Object value) {
		super();
		this.type = type;
		this.value = value;
	}
	
	public Metric(int type, Object value, String datatype) {
		super();
		this.type = type;
		this.value = value;
		this.datatype = datatype;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getDatatype() {
		return datatype;
	}

	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}

	public String toString() {
		return this.value + "(" + this.type + ")";
	}
}
