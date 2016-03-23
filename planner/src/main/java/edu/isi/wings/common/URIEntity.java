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

package edu.isi.wings.common;

import java.io.Serializable;
import java.net.URI;

public class URIEntity implements Serializable, Comparable<URIEntity> {
	private static final long serialVersionUID = 1L;
	private URI id;

	public URIEntity(String id) {
		setID(id);
	}

	public String getID() {
		if (id != null)
			return id.toString();
		else
			return null;
	}

	public void setID(String id) {
		try {
			this.id = new URI(id).normalize();
		} catch (Exception e) {
			System.err.println(id + " Not a URI. Only URIs allowed for IDs");
		}
	}
	
	public String getURL() {
		return this.getNamespace().replaceAll("#$", "");
	}

	public String getName() {
		if (id != null)
			return id.getFragment();
		else
			return null;
	}

	public String getNamespace() {
		if (id != null)
			return id.getScheme() + ":" + id.getSchemeSpecificPart() + "#";
		else
			return null;
	}

	public String toString() {
		return getName();
	}

	public int hashCode() {
		return id.hashCode();
	}

  @Override
  public int compareTo(URIEntity o) {
    if(o != null)
      return this.getID().compareTo(o.getID());
    return 1;
  }

}
