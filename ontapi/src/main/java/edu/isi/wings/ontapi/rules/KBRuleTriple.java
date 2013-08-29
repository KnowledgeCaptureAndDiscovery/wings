package edu.isi.wings.ontapi.rules;

public interface KBRuleTriple {

	KBRuleObject getSubject();
	
	KBRuleObject getPredicate();
	
	KBRuleObject getObject();
	
	void setSubject(KBRuleObject item);
	
	void setPredicate(KBRuleObject item);
	
	void setObject(KBRuleObject item);
	
	Object getInternalTriple();
}
