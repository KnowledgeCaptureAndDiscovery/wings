package edu.isi.wings.execution.tools.api;

import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;

public interface ExecutionLoggerAPI {
	
	void startLogging(RuntimePlan plan);
	
	void updateRuntimeInfo(RuntimePlan plan);
	
	void updateRuntimeInfo(RuntimeStep step);
	
	//Writer Lock
	void setWriterLock(Object lock); 
  
	Object getWriterLock();
  
}
