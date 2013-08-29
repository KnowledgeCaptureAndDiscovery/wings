package edu.isi.wings.ontapi.jena.rules;

import java.util.ArrayList;

import com.hp.hpl.jena.reasoner.rulesys.Rule;

import edu.isi.wings.ontapi.rules.KBRule;
import edu.isi.wings.ontapi.rules.KBRuleList;

public class KBRuleListJena implements KBRuleList {
	ArrayList<KBRule> rules;
	
	public KBRuleListJena() {
		this.rules = new ArrayList<KBRule>();
	}
	
	public KBRuleListJena(String rulesText) {
		this();
		for(Rule rule : Rule.parseRules(rulesText)) {
			rules.add(new KBRuleJena(rule));
		}
	}
	
	@Override
	public ArrayList<KBRule> getRules() {
		return this.rules;
	}

	@Override
	public void addRule(KBRule rule) {
		this.rules.add(rule);
	}

	@Override
	public void setRules(ArrayList<KBRule> rules) {
		this.rules = rules;
	}

	@Override
	public void mergeRules(KBRuleList rulelist) {
		if(rulelist == null)
			return;
		for(KBRule rule : rulelist.getRules())
			this.rules.add(rule);
	}

}
