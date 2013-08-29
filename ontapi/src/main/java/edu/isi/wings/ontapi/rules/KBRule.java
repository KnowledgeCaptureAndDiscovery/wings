package edu.isi.wings.ontapi.rules;

import java.util.ArrayList;

public interface KBRule {

	String getName();
	
	void setName(String name);
	
	ArrayList<KBRuleClause> getRuleBody();
	
	ArrayList<KBRuleClause> getRuleHead();
	
	void setRuleBody(ArrayList<KBRuleClause> body);
	
	void setRuleHead(ArrayList<KBRuleClause> head);
	
	Object getInternalRuleObject();
	
	void resetInternalRuleObject();
}
