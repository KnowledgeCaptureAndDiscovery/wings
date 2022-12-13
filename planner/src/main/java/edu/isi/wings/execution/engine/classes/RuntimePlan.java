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

package edu.isi.wings.execution.engine.classes;

import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

import edu.isi.wings.common.URIEntity;
import edu.isi.wings.common.UuidGen;
import edu.isi.wings.execution.engine.classes.RuntimeInfo.Status;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;

public class RuntimePlan extends URIEntity implements Cloneable {
	private static final long serialVersionUID = 1L;

	ExecutionQueue queue;
	ExecutionPlan plan;
	RuntimeInfo runtimeInfo;
	
	String originalTemplateId;
	String expandedTemplateId;
	String seededTemplateId;
	
	String callbackUrl;
	Cookie[] callbackCookies;
	
	Runnable callbackThread;
	
	boolean replanned = false;
	
	public RuntimePlan() {
	  super();
	}
	
	public RuntimePlan(String id) {
    super(id);
	}
	
	public RuntimePlan(ExecutionPlan plan) {
		super(plan.getID());
		this.setID(UuidGen.generateURIUuid((URIEntity)plan));
		this.plan = plan;
		this.queue = new ExecutionQueue(plan);
		this.runtimeInfo = new RuntimeInfo();
	}
  
	public ExecutionQueue getQueue() {
		return queue;
	}

	public void setQueue(ExecutionQueue queue) {
		this.queue = queue;
	}

	public ExecutionPlan getPlan() {
		return plan;
	}

	public void setPlan(ExecutionPlan plan) {
		this.plan = plan;
	}
	
	public RuntimeInfo getRuntimeInfo() {
		return this.runtimeInfo;
	}
	
	public void setRuntimeInfo(RuntimeInfo info) {
		this.runtimeInfo = info;
	}
	
	public void onStart(ExecutionLoggerAPI logger) {
		logger.startLogging(this);
		this.runtimeInfo.setStatus(RuntimeInfo.Status.RUNNING);
		this.runtimeInfo.setStartTime(new Date());
		logger.updateRuntimeInfo(this);
	}
	
	public void onEnd(ExecutionLoggerAPI logger, RuntimeInfo.Status status, String log) {
		this.runtimeInfo.setStatus(status);
		this.runtimeInfo.addLog(log);
		this.runtimeInfo.setEndTime(new Date());
		this.postCallback();
		logger.updateRuntimeInfo(this);
	}
	
	private void postCallback() {
	  if(this.callbackThread != null) {
	    synchronized(this.callbackThread) {
  	    //System.out.println("Notifying execution thread: " + this.callbackThread);
  	    this.callbackThread.notify();
	    }
	  }
	  
	  if(this.callbackUrl != null && this.runtimeInfo.status == Status.SUCCESS) {
	    try {
        BasicCookieStore cookieStore = new BasicCookieStore();
        if(this.callbackCookies != null) {
          cookieStore.addCookies(callbackCookies);
        }
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();

        // automatically follow redirects
        CloseableHttpClient client = HttpClients.custom()
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .build();
        
        HttpPost httpPost = new HttpPost(this.callbackUrl);
        String planjson = new Gson().toJson(plan);
        HttpEntity stringEntity = new StringEntity(planjson, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        
        HttpResponse response = client.execute(httpPost);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        System.out.println(responseString);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
	  }
	}
	
	public void waitFor() throws InterruptedException {
		while(true) {
			if(this.runtimeInfo.getStatus() != RuntimeInfo.Status.RUNNING)
				break;
			for(RuntimeStep step : queue.getRunningSteps()) {
				if(step.getProcess() != null && !step.getProcess().isDone())
		      Thread.sleep(1000);
			}
			Thread.sleep(500);
		}
	}
	
	public void abort() {
	  this.runtimeInfo.setStatus(Status.FAILURE);
		for(RuntimeStep exe : this.getQueue().getRunningSteps()) {
			exe.abort();
		}
	}
	
	public String getExpandedTemplateID() {
		return this.expandedTemplateId;
	}

	public String getOriginalTemplateID() {
		return this.originalTemplateId;
	}

	public void setExpandedTemplateID(String tid) {
		this.expandedTemplateId = tid;
	}

	public void setOriginalTemplateID(String tid) {
		this.originalTemplateId = tid;
	}

  public String getSeededTemplateID() {
    return seededTemplateId;
  }

  public void setSeededTemplateId(String seededTemplateId) {
    this.seededTemplateId = seededTemplateId;
  }

  public String getCallbackUrl() {
    return callbackUrl;
  }

  public void setCallbackUrl(String callbackUrl) {
    this.callbackUrl = callbackUrl;
  }

  public Cookie[] getCallbackCookies() {
    return callbackCookies;
  }

  public void setCallbackCookies(Cookie[] callbackCookies) {
    this.callbackCookies = callbackCookies;
  }

  public boolean isReplanned() {
    return replanned;
  }

  public void setReplanned(boolean replanned) {
    this.replanned = replanned;
  }
  
  public Runnable getCallbackThread() {
    return callbackThread;
  }

  public void setCallbackThread(Runnable callbackThread) {
    this.callbackThread = callbackThread;
  }

  public Object clone() throws CloneNotSupportedException{  
    return super.clone();  
  }
}
