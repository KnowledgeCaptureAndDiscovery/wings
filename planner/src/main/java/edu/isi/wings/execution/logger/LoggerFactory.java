package edu.isi.wings.execution.logger;

import java.util.Properties;

import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.logger.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.logger.api.impl.kb.RunKB;

public class LoggerFactory {
	
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
}
