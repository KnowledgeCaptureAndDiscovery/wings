package edu.isi.wings.ontapi.rules;

import java.util.ArrayList;

public interface KBRuleFunctor {

	String getName();
	
	ArrayList<KBRuleObject> getArguments();
	
	void setName(String name);
	
	void addArgument(KBRuleObject item);
	
	void setArguments(ArrayList<KBRuleObject> args);
	
	Object getInternalFunctor();
}
