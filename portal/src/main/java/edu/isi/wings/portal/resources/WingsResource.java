package edu.isi.wings.portal.resources;

import edu.isi.wings.portal.classes.config.ConfigLoader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

public class WingsResource {

  @Context
  HttpServletRequest request;

  @Context
  HttpServletResponse response;

  @Context
  ServletContext context;

  @Context
  UriInfo uriInfo;

  @PathParam("user")
  String user;

  @PathParam("domain")
  String domain;

  ConfigLoader config;

  @PostConstruct
  public void init() {
    this.config = new ConfigLoader(request, this.user, this.domain);
  }

  protected String callViewer(String viewer) {
    return this.getPage("/jsp/viewers/" + viewer + ".jsp");
  }

  protected boolean loadIntroduction(String page) {
    try {
      response.sendRedirect(
        request.getContextPath() + "/html/intros/" + page + ".html"
      );
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  private String getPage(String page) {
    HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(
      response
    ) {
      private final StringWriter sw = new StringWriter();

      @Override
      public PrintWriter getWriter() throws IOException {
        return new PrintWriter(sw);
      }

      @Override
      public String toString() {
        return sw.toString();
      }
    };
    try {
      request.getRequestDispatcher(page).include(request, responseWrapper);
      return responseWrapper.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  protected boolean isPage(String lastsegment) {
    List<PathSegment> segments = uriInfo.getPathSegments();
    if (
      segments.size() > 0 &&
      lastsegment.equals(segments.get(segments.size() - 1).toString())
    ) return true;
    return false;
  }

  protected boolean hasPermissions() {
    return config.checkDomain(request, response);
  }

  protected boolean isOwner() {
    return this.config.getViewerId().equals(this.config.getUserId());
  }
}
