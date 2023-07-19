<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.ConfigLoader"%>
<%@page import="edu.isi.wings.portal.controllers.RunController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Access Run Results</title>
    <%
      RunController rc = (RunController) request.getAttribute("controller");
      String run_id = (String) request.getAttribute("run_id");
      if(run_id != null)
        run_id = "'" + run_id + "'";
      int guid = 1; String thisApi =
      rc.config.getScriptPath(); boolean canPublish =
      (rc.config.portalConfig.getPublisher() != null);
    %>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

    <!-- Load Viewer Javascript -->
    <%=JSLoader.getRunViewer(rc.config.getContextRootPath())%>

    <!-- Load Viewer Stylesheets -->
    <%=CSSLoader.getViewerStyles(rc.config.getContextRootPath())%>
  </head>

  <body>
    <script>
      // Global Configuration Variables
      <%=JSLoader.getConfigurationJS(rc.config)%>

      // Viewer data;
      var run_id = <%=run_id%>;
      var this_api = "<%=thisApi%>";
      var data_url = "<%=rc.dataUrl%>";
      var template_url = "<%=rc.templateUrl%>";
      var can_publish = <%=canPublish%>;

      // Initialize viewer
      Ext.onReady(function() {
      	var runViewer = new RunBrowser('<%=guid%>', run_id, this_api,
      			data_url, template_url, can_publish);
          runViewer.initialize();
      });
    </script>
  </body>
</html>
