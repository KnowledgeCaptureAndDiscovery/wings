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
