package edu.isi.wings.workflow.template.api;

import java.io.Serializable;

import edu.isi.wings.workflow.template.classes.Metadata;
import edu.isi.wings.workflow.template.classes.Rules;

public interface Seed extends Template, Serializable {

	public String getID();

	public void setID(String seedId);

	public String getInternalRepresentation();

	public String deriveTemplateRepresentation();

	// Constraint Queries
	public ConstraintEngine getSeedConstraintEngine();

	public ConstraintEngine getTemplateConstraintEngine();

	public String getName();

	public Metadata getSeedMetadata();

	public Rules getSeedRules();

	public void reloadSeedFromEngine();
}
