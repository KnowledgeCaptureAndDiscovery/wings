<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.Config"%>
<%@page import="edu.isi.wings.portal.controllers.DomainController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>Manage Domains</title>
<%
DomainController dc = (DomainController) request.getAttribute("controller");

int guid = 1;
String uploadApi = dc.config.getUserDomainUrl() + "/upload";
String thisApi = dc.config.getScriptPath();

String list = dc.getDomainsListJSON();

%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<!-- Load Viewer Javascript -->
<%=JSLoader.getDomainViewer(dc.config.getContextRootPath())%>

<!-- Load Viewer Stylesheets -->
<%=CSSLoader.getViewerStyles(dc.config.getContextRootPath())%>
</head>

<body>
<script>
// Global Configuration Variables
<%=JSLoader.getConfigurationJS(dc.config)%>

// Viewer data;
var list = <%=list%>;
var this_api = "<%=thisApi%>";
var upload_api = "<%=uploadApi%>";

// Initialize viewer
Ext.onReady(function() {
	var domainViewer = new DomainViewer('<%=guid%>', list, this_api, upload_api);
	domainViewer.initialize(); 
});
</script>
</body>
</html>