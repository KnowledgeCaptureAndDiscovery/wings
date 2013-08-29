package edu.isi.wings.execution.logger.api.impl.derby;

import java.util.Properties;

import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.engine.classes.RuntimeStep;
import edu.isi.wings.execution.logger.api.ExecutionLoggerAPI;

public class DerbyLogger implements ExecutionLoggerAPI {
	Properties props;
	DerbyDatabase db;
	
	public DerbyLogger(Properties props) {
		this.props = props;
		this.db = new DerbyDatabase(props);
	}
	
	@Override
	public void updateRuntimeInfo(RuntimePlan plan) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRuntimeInfo(RuntimeStep step) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startLogging(RuntimePlan plan) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setWriterLock(Object lock) {
		// TODO Auto-generated method stub
		
	}
}
