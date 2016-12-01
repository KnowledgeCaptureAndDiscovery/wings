package edu.isi.wings.portal.filters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.config.Config;

public class CORSHeaders {

  public static HttpServletResponse addHeaders(HttpServletRequest request,
      HttpServletResponse response) {
    Config config = new Config(request);
    if(config.getClients() != null) {
      response.addHeader("Access-Control-Allow-Origin", config.getClients());
      response.addHeader("Access-Control-Allow-Credentials", "true");
      response
          .addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
      response.addHeader("Access-Control-Allow-Headers",
          "X-Requested-With, Content-Type, X-HTTP-Method-Override");
    }
    return response;
  }
}
