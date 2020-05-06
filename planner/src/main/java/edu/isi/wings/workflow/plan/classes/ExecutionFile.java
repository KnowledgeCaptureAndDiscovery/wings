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

package edu.isi.wings.workflow.plan.classes;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import edu.isi.wings.common.URIEntity;

public class ExecutionFile extends URIEntity {
	private static final long serialVersionUID = 1L;

	String location;
	String bindingId;
	
	transient String metaExtension = ".met";
	Properties metadata;
	long size;
	
	public ExecutionFile() {
	  super();
	}
	
	public ExecutionFile(String id) {
		super(id);
		metadata = new Properties();
		size = -1;
	}
	 
  public void loadMetadataFromLocation() {
    File f = new File(location);
    File metaf = new File(location + metaExtension);
    if(f.exists() && f.isFile()) {
      size = f.length();
      if(metaf.exists() && metaf.isFile()) {
        try {
          metadata.load(new FileInputStream(metaf));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  public void loadMetadataFromFileContents() {
    File metaf = new File(location);
    if(metaf.exists() && metaf.isFile()) {
      try {
        metadata.load(new FileInputStream(metaf));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void removeMetadataFile() {
    File metaf = new File(location + metaExtension);
    if(metaf.exists() && metaf.isFile()) {
      metaf.delete();
    }
  }
  
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getBinding() {
		return bindingId;
	}
	public void setBinding(String bindingId) {
		this.bindingId = bindingId;
	}
  public Properties getMetadata() {
    return metadata;
  }
  public void setMetadata(Properties metadata) {
    this.metadata = metadata;
  }
  public long getSize() {
    return size;
  }
  public void setSize(long size) {
    this.size = size;
  }

  public String getBindingId() {
    return bindingId;
  }

  public void setBindingId(String bindingId) {
    this.bindingId = bindingId;
  }
}
