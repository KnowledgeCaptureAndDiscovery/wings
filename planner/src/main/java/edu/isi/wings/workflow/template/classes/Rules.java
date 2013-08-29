package edu.isi.wings.workflow.template.classes;

import java.io.Serializable;

public class Rules implements Serializable {
	private static final long serialVersionUID = 1L;
	String rules;

	public Rules() {
	}

	public Rules(String rules) {
		this.rules = rules;
	}

	public void setRulesText(String rules) {
		this.rules = rules;
	}

	public String getRulesText() {
		return this.rules;
	}

	public void addRules(Rules ruleset) {
		if (this.rules != null && ruleset.getRulesText() != null)
			this.rules += "\n" + ruleset.getRulesText();
	}

}
