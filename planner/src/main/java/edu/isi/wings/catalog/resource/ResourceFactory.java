package edu.isi.wings.catalog.resource;

import java.util.Properties;

import edu.isi.wings.catalog.resource.api.ResourceAPI;
import edu.isi.wings.catalog.resource.api.impl.kb.ResourceKB;

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
}
