<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.Config"%>
<%@page import="edu.isi.wings.portal.controllers.ComponentController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%
ComponentController cc = (ComponentController) request.getAttribute("controller");
String title = "Manage Component" + (cc.loadConcrete ? "s" : " Types");

boolean isSandboxed = cc.config.isSandboxed();

String uploadApi = cc.config.getUserDomainUrl() + "/upload";
String resourceApi = cc.config.getCommunityPath() + "/resources";
String provApi = cc.config.getCommunityPath() + "/provenance";
String thisApi = cc.config.getScriptPath();

int guid = 1;

String tree = cc.json.toJson(cc.cc.getComponentHierarchy(false).getRoot());
String types = cc.json.toJson(cc.dc.getAllDatatypeIds());
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
var advanced_user = <%=!cc.config.isSandboxed()%>;
var load_concrete = <%=cc.loadConcrete%>;
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
		load_concrete, advanced_user
	);
	componentViewer.initialize(); 
});
</script>
</body>
</html>