<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.Config"%>
<%@page import="edu.isi.wings.portal.controllers.ResourceController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>Manage Software &amp; Machine Resources</title>
<%
ResourceController rc = (ResourceController) request.getAttribute("controller");

int guid = 1;
String thisApi = rc.config.getScriptPath();
String machineIds = rc.json.toJson(rc.api.getMachineIds());
String softwareIds = rc.json.toJson(rc.api.getSoftwareIds());
rc.api.end();

%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<!-- Load Viewer Javascript -->
<%=JSLoader.getResourceViewer(rc.config.getContextRootPath())%>

<!-- Load Viewer Stylesheets -->
<%=CSSLoader.getViewerStyles(rc.config.getContextRootPath())%>
</head>

<body>
<script>
// Global Configuration Variables
<%=JSLoader.getConfigurationJS(rc.config)%>

// Viewer data;
var this_api = "<%=thisApi%>";
var machine_ids = <%=machineIds%>;
var software_ids = <%=softwareIds%>;
var resns = "<%=rc.rns%>";
var libns = "<%=rc.libns%>";
var advanced_user = <%=!rc.config.isSandboxed()%>;

// Initialize viewer
Ext.onReady(function() {
	var resViewer = new ResourceViewer('<%=guid%>', {
			machines: machine_ids,
			softwares: software_ids
		},
		this_api, resns, libns,
		advanced_user
	);
	resViewer.initialize(); 
});
</script>
</body>
</html>