<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.ConfigLoader"%>
<%@page import="edu.isi.wings.portal.controllers.DataController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Manage Data</title>
    <%
      DataController dc = (DataController) request.getAttribute("controller");
      boolean isSandboxed = dc.config.portalConfig.isSandboxed();
      String uploadApi = dc.config.getUserDomainUrl() + "/upload";
      String provApi = dc.config.portalConfig.getCommunityPath() + "/provenance";
      String runApi = dc.config.getUserDomainUrl() + "/executions";
      String thisApi = dc.config.getScriptPath();
      int guid = 1;
      String tree = dc.getDataHierarchyJSON();
      String metrics = dc.getMetricsHierarchyJSON();
      String sensors = dc.getSensorComponentsListJSON();
      boolean hasExternalCatalog = (dc.dc != null && dc.dc.getExternalCatalog() != null);
    %>

    <!-- Load Viewer Javascript -->
    <%=JSLoader.getDataViewer(dc.config.getContextRootPath())%>

    <!-- Load Viewer Stylesheets -->
    <%=CSSLoader.getViewerStyles(dc.config.getContextRootPath())%>
  </head>

  <body>
    <script>
      // Global Configuration Variables
      <%=JSLoader.getConfigurationJS(dc.config)%>

      // Viewer data
      var tree = <%=tree%>;
      var metrics = <%=metrics%>;
      var sensors = <%=sensors%>;
      var this_api = "<%=thisApi%>";
      var prov_api = "<%=provApi%>";
      var results_api = "<%=runApi%>";
      var upload_api = "<%=uploadApi%>";
      var advanced_user = <%=!dc.config.portalConfig.isSandboxed()%>;
      var use_import_ui = false;
      var has_external_catalog = <%=hasExternalCatalog%>;
      var dcns = "<%=dc.dcns%>";
      var domns = "<%=dc.domns%>";
      var libns = "<%=dc.libns%>";

      // Initialize viewer
      Ext.onReady(function() {
      	var dataViewer = new DataViewer('<%=guid%>',
      		{
      			tree: tree,
      			metrics: metrics,
      			sensors: sensors
      		},
      		this_api, upload_api, prov_api, results_api,
      		dcns, domns, libns,
      		advanced_user, use_import_ui, has_external_catalog
      	);
      	dataViewer.initialize();
      });
    </script>
  </body>
</html>
