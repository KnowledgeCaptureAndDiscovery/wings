<%@page import="edu.isi.wings.portal.controllers.MetaworkflowController"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.ConfigLoader"%>
<%@page import="edu.isi.wings.portal.controllers.ResourceController"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Comparison &amp; Benchmarking Workflows</title>
    <%
      MetaworkflowController mc = (MetaworkflowController) request.getAttribute("controller");
      int guid = 1;
      String thisApi = mc.config.getScriptPath();
      String planApi = mc.config.getUserDomainUrl() + "/plan";
      String runApi = mc.config.getUserDomainUrl() + "/executions";
    %>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

    <!-- Load Viewer Javascript -->
    <%=JSLoader.getMetaworkflowViewer(mc.config.getContextRootPath())%>

    <!-- Load Viewer Stylesheets -->
    <%=CSSLoader.getViewerStyles(mc.config.getContextRootPath())%>
  </head>

  <body>
    <script>
      // Global Configuration Variables
      <%=JSLoader.getConfigurationJS(mc.config)%>

      // Viewer data;
      var this_api = "<%=thisApi%>";
      var workflows = <%=mc.getMetaworkflowsListJSON()%>;
      var runs = <%=mc.getWorkflowRunsJSON()%>;
      var template_url = "<%=mc.templateUrl%>";
      var plan_api = "<%=planApi%>";
      var run_api = "<%=runApi%>";

      // Initialize viewer
      Ext.onReady(function() {
      	var mwViewer = new MetaworkflowBrowser('<%=guid%>',
      		this_api, template_url, plan_api, run_api
      	);
      	mwViewer.initialize();
      });
    </script>
  </body>
</html>
