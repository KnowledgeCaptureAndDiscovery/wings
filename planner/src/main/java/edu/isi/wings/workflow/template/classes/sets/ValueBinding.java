package edu.isi.wings.workflow.template.classes.sets;

public class ValueBinding extends Binding {
	private static final long serialVersionUID = 1L;

	private String datatype;
	
	public ValueBinding() {
	}

	public ValueBinding(Object value) {
		this.setValue(value);
	}

	public ValueBinding(Object value, String datatype) {
		this.setValue(value);
		this.setDatatype(datatype);
	}
	
	public ValueBinding(ValueBinding b) {
		super(b);
	}

	public ValueBinding(Object[] values) {
		for (Object val : values) {
			this.add(new ValueBinding(val));
		}
	}

	public String getDatatype() {
		return datatype;
	}

	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}
}
