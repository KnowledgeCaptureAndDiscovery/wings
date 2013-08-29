package edu.isi.wings.execution.logger.api.impl.derby;

import java.util.ArrayList;

import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.logger.api.ExecutionMonitorAPI;

public class DerbyMonitor implements ExecutionMonitorAPI {

	@Override
	public ArrayList<RuntimePlan> getRunList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RuntimePlan getRunDetails(String runid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteRun(String runid) {
	    // TODO Auto-generated method stub
	    return false;
	}

	@Override
	public boolean runExists(String runid) {
		// TODO Auto-generated method stub
		return false;
	}

}
