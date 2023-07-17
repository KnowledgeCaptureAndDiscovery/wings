package edu.isi.wings.portal.classes.config;

public class PlannerConfig {

  boolean dataValidation = true;
  boolean specialization = true;
  boolean useRules = true;
  int maxQueueSize = 1000;
  int parallelism = 10;

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

  public int getMaxQueueSize() {
    return maxQueueSize;
  }

  public void setMaxQueueSize(int maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
  }

  public int getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }
}
