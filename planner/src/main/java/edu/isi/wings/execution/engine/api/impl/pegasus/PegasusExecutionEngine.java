/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.isi.wings.execution.engine.api.impl.pegasus;

import java.util.Properties;

import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.engine.classes.RuntimePlan;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;

public class PegasusExecutionEngine implements PlanExecutionEngine {
	
	public PegasusExecutionEngine(Properties props) {
		
	}

	@Override
	public void execute(RuntimePlan exe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStepEnd(RuntimePlan exe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void abort(RuntimePlan exe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMaxParallelSteps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxParallelSteps(int num) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStepExecutionEngine(StepExecutionEngine engine) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public StepExecutionEngine getStepExecutionEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExecutionLogger(ExecutionLoggerAPI monitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ExecutionLoggerAPI getExecutionLogger() {
		// TODO Auto-generated method stub
		return null;
	}

  @Override
  public void setExecutionMonitor(ExecutionMonitorAPI monitor) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ExecutionMonitorAPI getExecutionMonitor() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setExecutionResource(ExecutionResourceAPI resource) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ExecutionResourceAPI getExecutionResource() {
    // TODO Auto-generated method stub
    return null;
  }

}
