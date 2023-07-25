package edu.isi.wings.portal.classes.config;

import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class PlannerConfig {

  private static final String PLANNER_PARALLELISM_KEY = "planner.parallelism";
  private static final String PLANNER_MAX_QUEUE_SIZE_KEY =
    "planner.max-queue-size";
  private static final String PLANNER_USE_RULES_KEY = "planner.use-rules";
  private static final String PLANNER_SPECIALIZATION_KEY =
    "planner.specialization";
  private static final String PLANNER_DATA_VALIDATION_KEY =
    "planner.data-validation";

  boolean dataValidation = true;
  boolean specialization = true;
  boolean useRules = true;
  int maxQueueSize = 1000;
  int parallelism = 10;

  public PlannerConfig(PropertyListConfiguration serverConfig) {
    if (
      serverConfig.containsKey(MainConfig.MAIN_LIGHT_REASONER_KEY)
    ) this.dataValidation =
      !serverConfig.getBoolean(MainConfig.MAIN_LIGHT_REASONER_KEY);
    if (
      serverConfig.containsKey(PLANNER_DATA_VALIDATION_KEY)
    ) this.dataValidation =
      serverConfig.getBoolean(PLANNER_DATA_VALIDATION_KEY);
    if (
      serverConfig.containsKey(PLANNER_SPECIALIZATION_KEY)
    ) this.specialization = serverConfig.getBoolean(PLANNER_SPECIALIZATION_KEY);
    if (serverConfig.containsKey(PLANNER_USE_RULES_KEY)) this.useRules =
      serverConfig.getBoolean(PLANNER_USE_RULES_KEY);

    if (
      serverConfig.containsKey(PLANNER_MAX_QUEUE_SIZE_KEY)
    ) this.maxQueueSize = serverConfig.getInt(PLANNER_MAX_QUEUE_SIZE_KEY, 1000);
    if (serverConfig.containsKey(PLANNER_PARALLELISM_KEY)) this.parallelism =
      serverConfig.getInt(PLANNER_PARALLELISM_KEY, 10);
  }

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
