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

package edu.isi.wings.catalog.resource;

import java.io.File;
import java.util.Properties;

import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.catalog.resource.api.impl.kb.ResourceKB;
import edu.isi.wings.common.kb.PropertiesHelper;

public class ResourceFactory {
  /**
   * @param props
   *            The properties should contain: lib.resource.url,
   *            ont.resource.url
   *            (optional)
   */
  public static ResourceAPI getAPI(Properties props) {
    return new ResourceKB(props);
  }
  
  public static Properties createLegacyConfiguration() {
    Properties props = new Properties();
    String ontdir = PropertiesHelper.getOntologyDir();
    props.put("lib.resource.url",  PropertiesHelper.getOntologyURL() 
        + "/resource/lib/resource.owl");
    props.put("lib.resource.map", "file:" + ontdir 
        + File.separator + "resource/lib/resource.owl");
    props.put("ont.resource.url", 
        PropertiesHelper.getOntologyURL() + "/resource/resource.owl");
    props.put("ont.resource.map", "file:" + ontdir 
        + File.separator + "resource/resource.owl");
    return props;
  }
}
