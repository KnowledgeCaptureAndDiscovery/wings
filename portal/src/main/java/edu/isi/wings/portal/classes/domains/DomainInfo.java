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

package edu.isi.wings.portal.classes.domains;

import java.util.ArrayList;

public class DomainInfo {
	String name;
	String engine;
	String directory;
	String url;
	boolean isLegacy;
	ArrayList<Permission> permissions;

	public DomainInfo(Domain dom) {
		this.name = dom.getDomainName();
		this.directory = dom.getDomainDirectory();
		this.url = dom.getDomainUrl();
		this.isLegacy = dom.isLegacy();
		this.engine = dom.getPlanEngine();
		this.permissions = dom.getPermissions();
	}
	
	public DomainInfo(String name, String engine, 
	    String directory, String url, boolean isLegacy, 
	    ArrayList<Permission> permissions) {
		super();
		this.name = name;
		this.engine = engine;
		this.directory = directory;
		this.url = url;
		this.isLegacy = isLegacy;
		this.permissions = permissions;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isLegacy() {
		return isLegacy;
	}

	public void setLegacy(boolean isLegacy) {
		this.isLegacy = isLegacy;
	}

  public String getEngine() {
    return engine;
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }
}
