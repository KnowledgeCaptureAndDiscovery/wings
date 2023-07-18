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

package edu.isi.wings.portal.classes.config;

import edu.isi.wings.execution.engine.ExecutionFactory;
import edu.isi.wings.execution.engine.api.PlanExecutionEngine;
import edu.isi.wings.execution.engine.api.StepExecutionEngine;
import edu.isi.wings.execution.tools.ExecutionToolsFactory;
import edu.isi.wings.execution.tools.api.ExecutionLoggerAPI;
import edu.isi.wings.execution.tools.api.ExecutionMonitorAPI;
import edu.isi.wings.execution.tools.api.ExecutionResourceAPI;
import edu.isi.wings.portal.classes.domains.Domain;
import edu.isi.wings.portal.classes.domains.Permission;
import edu.isi.wings.portal.classes.users.User;
import edu.isi.wings.portal.classes.users.UsersDB;
import edu.isi.wings.portal.controllers.DomainController;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConfigLoader {
  private static final String ONT_DIR_URL = "ont.dir.url";
  private static final String ONT_DIR_MAP = "ont.dir.map";
  public PortalConfig portalConfig = new PortalConfig();
  private ArrayList<String> usersList;
  private boolean isAdminViewer;

  // Some hardcoded values (TODO: override from config file)

  // The following are set from the "request" variable
  private String viewerId;
  private String userId;
  private String sessionId;
  private String contextRootPath;
  private String scriptPath;
  private String[] scriptArguments;

  // This following are user/domain specific properties
  private String userPath;
  private String userDir;

  private Domain domain;
  private String domainId;
  private ArrayList<String> domainsList;
  private String userDomainUrl;
  private String exportUserUrl;

  private UsersDB userapi;


  public ConfigLoader() {}

  public ConfigLoader(HttpServletRequest request, String userid, String domain) {
    // Initialize UserDatabase
    this.contextRootPath = request.getContextPath();
    this.initializeUserDatabase();

    // Initialize portal config
    portalConfig.initializePortalConfig(request);

    // Initialize user config
    this.initializeUserConfig(request, userid, domain);
  }

  private void initializeUserDatabase() {
    this.userapi = new UsersDB();
  }

  private void initializeUserConfig(
    HttpServletRequest request,
    String userid,
    String domainid
  ) {
    this.userId = userid;
    this.domainId = domainid;
    this.viewerId = request.getRemoteUser();

    // Set default script values
    this.scriptPath = request.getRequestURI();

    if (this.domainId != null) this.userDomainUrl =
      this.contextRootPath +
      "/" +
      PortalConfig.USERS_RELATIVE_DIR +
      "/" +
      this.getUserId() +
      "/" +
      this.getDomainId();

    this.sessionId = request.getSession().getId();

    if (this.viewerId == null) return;

    // If no userId specified, then set the viewer as the user
    if (this.userId == null) this.userId = this.viewerId;

    if (!this.checkUser(null)) return;

    this.exportUserUrl =
      portalConfig.serverUrl +
      contextRootPath +
      PortalConfig.EXPORT_SERVLET_PATH +
      "/" +
      PortalConfig.USERS_RELATIVE_DIR+
      "/" +
      userId;
    this.userDir =
      portalConfig.storageDirectory +
      File.separator +
      PortalConfig.USERS_RELATIVE_DIR+
      File.separator +
      userId;

    this.userPath = contextRootPath + "/" + PortalConfig.USERS_RELATIVE_DIR + "/" + userId;

    // Create userDir (if it doesn't exist)
    File uf = new File(this.userDir);
    if (!uf.exists() && !uf.mkdirs()) System.err.println(
      "Cannot create user directory : " + uf.getAbsolutePath()
    );

    // Get domain and user list
    DomainController dc = new DomainController(this);
    this.domainsList = dc.getReadableDomainsList();
    this.usersList = this.userapi.getUsersList();

    // Get user's selected domain
    this.domain = dc.getUserDomain();

    // If the domain isn't a part of the readable domain list,
    // then choose the first one
    if (
      this.domain == null || !domainsList.contains(this.domain.getDomainName())
    ) {
      if (domainsList.size() > 0) this.domain =
        dc.getDomain(domainsList.get(0)); else this.domain = null;
    }

    if (this.domain != null) {
      this.domainId = this.domain.getDomainName();
      this.userDomainUrl =
        this.contextRootPath +
        "/" +
        PortalConfig.USERS_RELATIVE_DIR +
        "/" +
        this.getUserId() +
        "/" +
        this.domain.getDomainName();
    }
  }

  public void getPermissions() {
    // Check domain, user & viewerid
    // Return Permissions (canRead=true/false, canWrite=true/false,
    // canExecute=true/false)
  }

  public boolean checkUser(HttpServletResponse response) {
    /* Check that such a user exists */
    try {
      if (!this.userapi.hasUser(this.userId)) {
        if (response != null) {
          // If userId is not present in server
          response.setContentType("text/html");
          response.getWriter().println("No such user: " + this.userId + " !");
        }
        return false;
      }
      // Get viewer roles
      if (this.viewerId == null) return false;
      User viewer = this.userapi.getUser(this.viewerId);
      this.isAdminViewer = viewer.isAdmin();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public void showError(
    HttpServletRequest request,
    HttpServletResponse response,
    String message
  ) {
    try {
      response.setContentType("text/html");
      request.setAttribute("message", message);
      request.setAttribute("nohome", true);
      request.getRequestDispatcher("/").forward(request, response);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean checkDomain(
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    return this.checkDomain(request, response, true);
  }

  public boolean checkDomain(
    HttpServletRequest request,
    HttpServletResponse response,
    boolean show_error
  ) {
    if (!this.checkUser(response)) return false;

    try {
      // For a non-owner viewer, if there is no domain available,
      // then return a message
      if (this.domain == null && !this.viewerId.equals(this.userId)) {
        if (show_error) this.showError(
            request,
            response,
            "No Domains shared by " + userId + " !"
          );
        return false;
      }

      // Check that a domain is provided in the URL, or a default domain exists
      String redirectUrl = this.getUserPath() + "/domains";
      if (this.domain == null && !this.scriptPath.equals(redirectUrl)) {
        response.setContentType("text/html");
        response.setHeader("Refresh", "5; URL=" + redirectUrl);
        response
          .getWriter()
          .println(
            "No such domain !<br/>" +
            "See list of domains at <a href='" +
            redirectUrl +
            "'>" +
            redirectUrl +
            "</a>. " +
            "Redirecting in 5 seconds"
          );
        return false;
      } else if (
        this.domain != null &&
        !this.scriptPath.equals(redirectUrl) &&
        !this.viewerId.equals(this.userId)
      ) {
        // Check domain permissions
        // TODO: Check read, write & execute permission based on input
        // For now: all or none permissions
        Permission perm = this.domain.getPermissionForUser(this.viewerId);
        if (!perm.canRead()) {
          if (show_error) this.showError(request, response, "No Permission !");
          return false;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return true;
  }


  public String getViewerId() {
    return viewerId;
  }

  public void setViewerId(String viewerId) {
    this.viewerId = viewerId;
  }

  public String[] getScriptArguments() {
    return scriptArguments;
  }

  public void setScriptArguments(String[] scriptArguments) {
    this.scriptArguments = scriptArguments;
  }

  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  public ArrayList<String> getDomainsList() {
    return domainsList;
  }

  public ArrayList<String> getUsersList() {
    return usersList;
  }


  public Properties getProperties() {
    return this.getProperties(this.domain);
  }

  // Return Properties that are currently used by catalogs & planners
  public Properties getProperties(Domain domain) {
    Properties props = new Properties();
    if (domain != null) {
      props = domain.getProperties();
      if (domain.isLegacy()) return props;
      props.setProperty(ONT_DIR_URL, PortalConfig.ONT_DIR_URL);
      if (!domain.getUseSharedTripleStore()) props.setProperty(
        ONT_DIR_MAP,
        "file:" + domain.getDomainDirectory() + File.separator + "ontology"
      );

      props.setProperty("ont.data.url", portalConfig.getDataOntologyUrl());
      props.setProperty(
        "ont.component.url",
        portalConfig.getComponentOntologyUrl()
      );
      props.setProperty(
        "ont.workflow.url",
        portalConfig.getWorkflowOntologyUrl()
      );
      props.setProperty(
        "ont.execution.url",
        portalConfig.getExecutionOntologyUrl()
      );

      if (domain.getUseSharedTripleStore()) props.setProperty(
        "tdb.repository.dir",
        portalConfig.getTdbDirectory()
      );

      HashMap<String, ExeEngine> engines = portalConfig.getEngines();
      ExeEngine pengine = engines.get(domain.getPlanEngine());
      ExeEngine sengine = engines.get(domain.getStepEngine());
      props.putAll(pengine.getProperties());
      props.putAll(sengine.getProperties());
    } else {
      props.setProperty("tdb.repository.dir", portalConfig.getTdbDirectory());
    }
    props.setProperty("logs.dir", portalConfig.getLogsDirectory());
    props.setProperty("dot.path", portalConfig.getDotFile());
    props.setProperty("ont.resource.url", portalConfig.getResourceOntologyUrl());

    props.setProperty(
      "lib.resource.url",
      this.portalConfig.getExportCommunityUrl() + "/resource/library.owl"
    );

    if (domain != null && !domain.getUseSharedTripleStore()) props.setProperty(
      "lib.resource.map",
      "file:" +
      domain.getDomainDirectory() +
      File.separator +
      "ontology" +
      File.separator +
      "resource" +
      File.separator +
      "library.owl"
    );

    props.setProperty(
      "lib.provenance.url",
      this.portalConfig.getExportCommunityUrl() + "/provenance/library.owl"
    );

    if (this.viewerId != null) props.setProperty("viewer.id", this.viewerId);
    if (this.userId != null) props.setProperty("user.id", this.userId);

    props.setProperty(
      "use_rules",
      portalConfig.plannerConfig.useRules() ? "true" : "false"
    );
    return props;
  }

  public PlanExecutionEngine getDomainExecutionEngine() {
    ExeEngine pengine = portalConfig.engines.get(domain.getPlanEngine());
    ExeEngine sengine = portalConfig.engines.get(domain.getStepEngine());
    try {
      pengine.getProperties().putAll(this.getProperties());
      sengine.getProperties().putAll(this.getProperties());
      // TODO: Check if the selected engines are compatible
      // and can be used as plan and step engines respectively
      PlanExecutionEngine pee = ExecutionFactory.createPlanExecutionEngine(
        pengine.getImplementation(),
        pengine.getProperties()
      );
      StepExecutionEngine see = ExecutionFactory.createStepExecutionEngine(
        sengine.getImplementation(),
        sengine.getProperties()
      );
      ExecutionLoggerAPI logger = ExecutionToolsFactory.createLogger(
        this.getProperties()
      );
      ExecutionMonitorAPI monitor = ExecutionToolsFactory.createMonitor(
        this.getProperties()
      );
      ExecutionResourceAPI resource = ExecutionToolsFactory.getResourceAPI(
        this.getProperties()
      );
      resource.setLocalStorageFolder(portalConfig.getStorageDirectory());
      pee.setStepExecutionEngine(see);
      pee.setExecutionLogger(logger);
      pee.setExecutionMonitor(monitor);
      pee.setExecutionResource(resource);
      return pee;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public StepExecutionEngine getDomainStepEngine() {
    return null;
  }

  public ExecutionMonitorAPI getDomainExecutionMonitor() {
    return ExecutionToolsFactory.createMonitor(this.getProperties());
  }


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    if (userDomainUrl != null) this.userDomainUrl =
      this.userDomainUrl.replace(this.userId, userId);
    if (scriptPath != null) this.scriptPath =
      this.scriptPath.replace(this.userId, userId);
    if (exportUserUrl != null) this.exportUserUrl =
      this.exportUserUrl.replace(this.userId, userId);
    if (userPath != null) this.userPath =
      this.userPath.replace(this.userId, userId);
    if (userDir != null) this.userDir =
      this.userDir.replace(this.userId, userId);
    this.userId = userId;
  }

  public String getUserDir() {
    return userDir;
  }

  public void setUserDir(String userDir) {
    this.userDir = userDir;
  }

  public boolean isAdminViewer() {
    return isAdminViewer;
  }

  public void setAdminViewer(boolean isAdminViewer) {
    this.isAdminViewer = isAdminViewer;
  }

  public void setExportUserUrl(String exportUserUrl) {
    this.exportUserUrl = exportUserUrl;
  }

  public String getContextRootPath() {
    return contextRootPath;
  }

  public void setContextRootPath(String root) {
    this.contextRootPath = root;
  }

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public String getScriptPath() {
    return scriptPath;
  }

  public void setScriptPath(String scriptPath) {
    this.scriptPath = scriptPath;
  }

  public String getExportUserUrl() {
    return exportUserUrl;
  }

  public String getUserPath() {
    return userPath;
  }

  public void setUserPath(String userPath) {
    this.userPath = userPath;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getUserDomainUrl() {
    return userDomainUrl;
  }

  public void setUserDomainUrl(String userDomainUrl) {
    this.userDomainUrl = userDomainUrl;
  }

}
