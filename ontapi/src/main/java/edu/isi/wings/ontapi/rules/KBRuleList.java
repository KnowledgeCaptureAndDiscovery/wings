package edu.isi.wings.ontapi.rules;

import java.util.ArrayList;

public interface KBRuleList {
	
	ArrayList<KBRule> getRules();
	
	void addRule(KBRule rule);
	
	void mergeRules(KBRuleList rulelist);
	
	void setRules(ArrayList<KBRule> rules);
}
