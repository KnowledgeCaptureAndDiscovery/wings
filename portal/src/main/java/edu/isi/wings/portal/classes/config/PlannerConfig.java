package edu.isi.wings.portal.classes.config;

public class PlannerConfig {
  boolean dataValidation = true;
  boolean specialization = true;
  boolean useRules = true;
  
  public boolean useDataValidation() {
    return dataValidation;
  }
  public void setDataValidation(boolean dataValidation) {
    this.dataValidation = dataValidation;
  }
  public boolean useSpecialization() {
    return specialization;
  }
  public void setSpecialization(boolean specialization) {
    this.specialization = specialization;
  }
  public boolean useRules() {
    return useRules;
  }
  public void setUseRules(boolean useRules) {
    this.useRules = useRules;
  }
}
