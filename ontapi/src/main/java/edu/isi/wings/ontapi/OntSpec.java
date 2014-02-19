package edu.isi.wings.ontapi;

public final class OntSpec {
  private String id;

  private OntSpec(String anID) {
    this.id = anID;
  }

  public String toString() {
    return this.id;
  }

  public static final OntSpec PLAIN = new OntSpec("No Inference Reasoner");

  public static final OntSpec MICRO = new OntSpec("Micro Rules Reasoner");

  public static final OntSpec MINI = new OntSpec("Mini Rules Reasoner");

  public static final OntSpec DL = new OntSpec("DL Reasoner");

  public static final OntSpec FULL = new OntSpec("Full OWL Reasoner");

  public static final OntSpec TRANS = new OntSpec("Transitive Reasoner");

  public static final OntSpec RDFS = new OntSpec("RDFS Reasoner");

  public static final OntSpec PELLET = new OntSpec("Pellet Reasoner");
}
