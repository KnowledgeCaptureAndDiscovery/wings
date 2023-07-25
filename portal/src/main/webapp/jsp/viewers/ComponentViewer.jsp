<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.ConfigLoader"%>
<%@page import="edu.isi.wings.portal.controllers.ComponentController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%
      ComponentController cc = (ComponentController) request.getAttribute("controller");
      String title = "Manage Components";
      boolean isSandboxed = cc.config.portalConfig.isSandboxed();
      String uploadApi = cc.config.getUserDomainUrl() + "/upload";
      String resourceApi = cc.config.portalConfig.mainConfig.getCommunityPath() + "/resources";
      String provApi = cc.config.portalConfig.mainConfig.getCommunityPath() + "/provenance";
      String thisApi = cc.config.getScriptPath();
      int guid = 1;
      String tree = cc.getComponentHierarchyJSON();
      String types = cc.getDatatypeIdsJSON();
    %>
    <title><%=title%></title>

    <!-- Load Viewer Javascript -->
    <%=JSLoader.getComponentViewer(cc.config.getContextRootPath())%>

    <!-- Load Viewer Stylesheets -->
    <%=CSSLoader.getViewerStyles(cc.config.getContextRootPath())%>
  </head>

  <body>
    <script>
      // Global Configuration Variables
      <%=JSLoader.getConfigurationJS(cc.config)%>

      // Viewer data
      var tree = <%=tree%>;
      var types = <%=types%>;
      var this_api = "<%=thisApi%>";
      var resource_api = "<%=resourceApi%>";
      var prov_api = "<%=provApi%>";
      var upload_api = "<%=uploadApi%>";
      var advanced_user = <%=!cc.config.portalConfig.isSandboxed()%>;
      var pcdomns = "<%=cc.pcdomns%>";
      var dcdomns = "<%=cc.dcdomns%>";
      var liburl = "<%=cc.liburl%>";

      // Initialize viewer
      Ext.onReady(function() {
      	var componentViewer = new ComponentViewer('<%=guid%>',
      		{
      			tree: tree,
      			types: types
      		},
      		this_api, resource_api, upload_api, prov_api,
      		pcdomns, dcdomns, liburl,
      		advanced_user
      	);
      	componentViewer.initialize();
      });
    </script>
  </body>
</html>
