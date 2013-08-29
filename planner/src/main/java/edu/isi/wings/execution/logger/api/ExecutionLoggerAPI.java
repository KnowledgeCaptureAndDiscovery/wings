package edu.isi.wings.execution.logger.api;

import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;

public interface ExecutionLoggerAPI {
	
	void startLogging(RuntimePlan plan);
	
	void updateRuntimeInfo(RuntimePlan plan);
	
	void updateRuntimeInfo(RuntimeStep step);
	
	void setWriterLock(Object lock); // Writer Lock

}
