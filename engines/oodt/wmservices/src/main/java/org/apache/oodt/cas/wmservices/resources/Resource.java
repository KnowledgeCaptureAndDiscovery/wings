package org.apache.oodt.cas.wmservices.resources;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.oodt.cas.wmservices.servlets.WmServicesServlet;
import org.apache.oodt.cas.workflow.system.XmlRpcWorkflowManagerClient;

public abstract class Resource {
  private static final Logger LOGGER = Logger.getLogger(Resource.class
      .getName());

  // Servlet context
  @Context
  private ServletContext context;

  /**
   * Gets the packaged repository directory from servlet context.
   * @return the packaged repository directory
   * @throws Exception
   *           if an object cannot be retrieved from the context attribute
   */
  public File getContextPkgReposDir() throws Exception {
    Object repositoryDirObject = context
        .getAttribute(WmServicesServlet.ATTR_NAME_PKG_REPO_DIR);
    if (repositoryDirObject != null
        && repositoryDirObject instanceof File) {
      return (File) repositoryDirObject;
    }
    String message = "Unable to retrieve packaged repository directory from the servlet context.";
    LOGGER.log(Level.WARNING, message);
    throw new Exception(message);
  }
  
  /**
   * Gets the servlet's workflow manager client instance from the servlet
   * context.
   * @return the workflow manager client instance from the servlet context
   *         attribute
   * @throws Exception
   *           if an object cannot be retrieved from the context attribute
   */
  public XmlRpcWorkflowManagerClient getContextClient() throws Exception {
    // Get the workflow manager client from the servlet context.
    Object clientObject = context
        .getAttribute(WmServicesServlet.ATTR_NAME_CLIENT);
    if (clientObject != null
        && clientObject instanceof XmlRpcWorkflowManagerClient) {
      return (XmlRpcWorkflowManagerClient) clientObject;
    }

    String message = "Unable to retrieve workflow manager client from the "
        + "servlet context.";
    LOGGER.log(Level.WARNING, message);
    throw new Exception(message);
  }

  
  /**
   * Sets the servlet context.
   * @param context
   *          the servlet context to set.
   */
  public void setServletContext(ServletContext context) {
    this.context = context;
  }
}
