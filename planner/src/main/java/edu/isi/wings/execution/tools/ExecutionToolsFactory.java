package edu.isi.wings.execution.tools;

import java.util.Properties;

import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.execution.tools.api.impl.kb.ExecutionResourceKB;
import edu.isi.wings.execution.tools.api.impl.kb.RunKB;

public class ExecutionToolsFactory {
	
	public static ExecutionLoggerAPI createLogger(String impl, Properties props) 
			throws Exception {
		Class<?> classz = Class.forName(impl);
		return (ExecutionLoggerAPI) classz.getDeclaredConstructor(Properties.class).newInstance(props);
	}
	
	public static ExecutionMonitorAPI createMonitor(String impl, Properties props) 
			throws Exception {
		Class<?> classz = Class.forName(impl);
		return (ExecutionMonitorAPI) classz.getDeclaredConstructor(Properties.class).newInstance(props);
	}
	
	public static ExecutionLoggerAPI createLogger(Properties props) {
		return new RunKB(props);
	}
	
	public static ExecutionMonitorAPI createMonitor(Properties props) {
		return new RunKB(props);
	}
	
	 public static ExecutionResourceAPI getResourceAPI(Properties props) {
	    return new ExecutionResourceKB(props);
	  }
}
