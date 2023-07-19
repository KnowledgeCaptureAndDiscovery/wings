<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.ConfigLoader"%>
<%@page import="edu.isi.wings.portal.controllers.UserController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Manage Users</title>
    <%
      UserController uc = (UserController) request.getAttribute("controller");
      int guid = 1;
      String thisApi = uc.config.getScriptPath();
      String provApi = uc.config.portalConfig.getCommunityPath() + "/provenance";
      String users = uc.json.toJson(uc.api.getUsers());
    %>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

    <!-- Load Viewer Javascript -->
    <%=JSLoader.getUserViewer(uc.config.getContextRootPath())%>

    <!-- Load Viewer Stylesheets -->
    <%=CSSLoader.getViewerStyles(uc.config.getContextRootPath())%>
  </head>

  <body>
    <script>
      // Global Configuration Variables
      <%=JSLoader.getConfigurationJS(uc.config)%>

      // Viewer data;
      var users = <%=users%>;
      var this_api = "<%=thisApi%>";
      var prov_api = "<%=provApi%>";
      var is_admin = <%=uc.config.isAdminViewer()%>;

      // Initialize viewer
      Ext.onReady(function() {
      	var userViewer = new UserViewer('<%=guid%>', {
      		users: users
      	},
      	this_api, prov_api, is_admin);

        	userViewer.initialize();
      });
    </script>
  </body>
</html>
