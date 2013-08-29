package edu.isi.wings.catalog.data.classes;

import edu.isi.wings.ontapi.KBObject;

import java.util.ArrayList;
import java.util.HashSet;

public class VariableBindings {

	/**
	 * dataVariable from the workflow namespace
	 */
	public KBObject dataVariable;

	/**
	 * dataObjects from the dc namespace
	 */
	public HashSet<KBObject> dataObjects = new HashSet<KBObject>();

	public VariableBindings() {
	}

	/**
	 * 
	 * @param dataVariable
	 *            data variable from the workflow namespace
	 * @param dataObject
	 *            data object from the dc namespace
	 */
	public VariableBindings(KBObject dataVariable, KBObject dataObject) {
		this.dataVariable = dataVariable;
		this.dataObjects.add(dataObject);
	}

	/**
	 * returns an array list of kbojects <dataVariable dataObjects>
	 * 
	 * @return an array list of kbojects
	 */
	public ArrayList<KBObject> toArrayList() {
		ArrayList<KBObject> result = new ArrayList<KBObject>(2);
		result.add(this.getDataVariable());
		result.addAll(this.getDataObjects());
		return result;
	}

	/**
	 * Getter for property 'dataVariable'.
	 * 
	 * @return Value for property 'dataVariable'.
	 */
	public KBObject getDataVariable() {
		return dataVariable;
	}

	/**
	 * Setter for property 'dataVariable'.
	 * 
	 * @param dataVariable
	 *            Value to set for property 'dataVariable'.
	 */
	public void setDataVariable(KBObject dataVariable) {
		this.dataVariable = dataVariable;
	}

	/**
	 * Getter for property 'dataObjects'.
	 * 
	 * @return Value for property 'dataObjects'.
	 */
	public HashSet<KBObject> getDataObjects() {
		return dataObjects;
	}

	/**
	 * Setter for property 'dataObjects'.
	 * 
	 * @param dataObject
	 *            Value to set for property 'dataObjects'.
	 */
	public void setDataObjects(HashSet<KBObject> dataObjects) {
		this.dataObjects = dataObjects;
	}

	/**
	 * Added for property 'dataObjects'
	 * 
	 * @param dataObject
	 *            Value to add for property 'dataObjects'
	 */
	public void addDataObject(KBObject dataObject) {
		this.dataObjects.add(dataObject);
	}

	public String toString() {
		String str = "";
		str += "( " + dataVariable.shortForm() + " : [ ";
		for (KBObject dataObject : dataObjects)
			str += dataObject.shortForm() + " ";
		str += "] )";
		return str;
	}
}
