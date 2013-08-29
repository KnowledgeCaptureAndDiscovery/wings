package edu.isi.wings.workflow.template.classes.sets;

import java.util.ArrayList;

public class WingsSet extends ArrayList<WingsSet> {
	private static final long serialVersionUID = 1L;
	protected Object obj;

	public WingsSet() {
		super();
	}

	public WingsSet(WingsSet s) {
		this.add(s);
	}

	public WingsSet(Object obj) {
		this.obj = obj;
	}

	public WingsSet(Object[] objs) {
		for (Object t : objs) {
			this.add(new WingsSet(t));
		}
	}

	public int getMaxDimension() {
		if (!isSet())
			return 0;
		int dimension = 0;
		for (WingsSet i : this) {
			int idim = i.getMaxDimension() + 1;
			if (dimension < idim)
				dimension = idim;
		}
		return dimension;
	}

	public int getMinDimension() {
		if (!isSet())
			return 0;
		int dimension = 9999;
		for (WingsSet i : this) {
			int idim = i.getMinDimension() + 1;
			if (dimension > idim)
				dimension = idim;
		}
		return dimension;
	}

	public void increaseMinDimensionTo(int toDimension) {
		int increase = toDimension - getMinDimension();
		increaseDimensionBy(increase);
	}

	public void increaseMaxDimensionTo(int toDimension) {
		int increase = toDimension - getMaxDimension();
		increaseDimensionBy(increase);
	}

	public void increaseDimensionBy(int increase) {
		for (int i = 0; i < increase; i++) {
			WingsSet child = (WingsSet) this.clone();
			this.clear();
			this.add(child);
		}
	}

	public void shift() {
		if (!isSet())
			return;
		this.remove(0);
	}

	public int getSize() {
		return this.size();
	}

	public boolean isSet() {
		if (!isEmpty())
			return true;
		return false;
	}

	public Object getValue() {
		if (!isSet())
			return obj;
		return null;
	}

	/*
	 * Overrides
	 */
	public boolean add(WingsSet s) {
		if (!this.contains(s))
			return super.add(s);
		return false;
	}

	public int hashCode() {
		if (isSet())
			return super.hashCode();
		else if (obj != null)
			return obj.hashCode();
		return 0;
	}

	public boolean equals(Object s) {
		if (hashCode() == s.hashCode())
			return true;
		return false;
	}

	public String toString() {
		if (isSet())
			return super.toString();
		else
			return getValue().toString();
	}
}
