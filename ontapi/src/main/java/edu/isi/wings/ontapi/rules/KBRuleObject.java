package edu.isi.wings.ontapi.rules;

import edu.isi.wings.ontapi.KBObject;

public interface KBRuleObject {

	boolean isVariable();
	
	String getVariableName();
	
	KBObject getKBObject();
	
	Object getInternalNode();
}
