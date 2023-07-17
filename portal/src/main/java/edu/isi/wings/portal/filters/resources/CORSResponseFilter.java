package edu.isi.wings.portal.filters.resources;

import edu.isi.wings.portal.classes.config.Config;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class CORSResponseFilter implements ContainerResponseFilter {

  @Context
  private HttpServletRequest request;

  public void filter(
    ContainerRequestContext requestContext,
    ContainerResponseContext responseContext
  ) throws IOException {
    Config config = new Config();
    PropertyListConfiguration plist = config.getPortalConfiguration(request);
    String clients = plist.getString("clients");
    if (clients != null) {
      MultivaluedMap<String, Object> headers = responseContext.getHeaders();
      headers.add("Access-Control-Allow-Origin", clients);
      headers.add("Access-Control-Allow-Credentials", "true");
      headers.add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
      headers.add(
        "Access-Control-Allow-Headers",
        "X-Requested-With, Content-Type, Content-Encoding, X-HTTP-Method-Override"
      );
    }
  }
}
