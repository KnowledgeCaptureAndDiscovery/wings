<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.isi.wings.portal.classes.html.CSSLoader"%>
<%@page import="edu.isi.wings.portal.classes.html.JSLoader"%>
<%@page import="edu.isi.wings.portal.classes.config.Config"%>
<%@page import="edu.isi.wings.portal.controllers.TemplateController"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%
TemplateController tc = (TemplateController) request.getAttribute("controller");
boolean editor = (Boolean) request.getAttribute("editor");
boolean tellme = (Boolean) request.getAttribute("tellme");
String template_name = (String) request.getAttribute("template_name");

boolean lightReasoner = !tc.config.getPlannerConfig().useDataValidation();

String optstr = tc.json.toJson(request.getAttribute("options"));
String title = "Template " + (editor ? "Editor" : "Browser");

int guid = 1;
String wliburl = (String) tc.props.get("domain.workflows.dir.url");
String dcdomns = (String) tc.props.get("ont.domain.data.url") + "#";
String dclibns = (String) tc.props.get("lib.domain.data.url") + "#";
String pcdomns = (String) tc.props.get("ont.domain.component.ns");
String wflowns = (String) tc.props.get("ont.workflow.url") + "#";

String planApi = tc.config.getUserDomainUrl() + "/plan";
String runApi = tc.config.getUserDomainUrl() + "/executions";
String provApi = tc.config.getCommunityPath() + "/provenance";
String thisApi = tc.config.getScriptPath();

String components = tc.json.toJson(tc.cc.getComponentHierarchy(editor).getRoot());
String dataTree = editor ? tc.json.toJson(tc.dc.getDatatypeHierarchy().getRoot()) : null;
String beamerParas = tellme ? tc.getBeamerParaphrasesJSON() : null;
String beamerMaps = tellme ? tc.getBeamerMappingsJSON() : null;
String propVals = tc.json.toJson(tc.getConstraintProperties());


String template_id = null;
if(template_name != null)
	template_id = "'" + wliburl+ "/" + template_name + ".owl#" + template_name + "'";

%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%=title%></title>

<!-- Load Viewer Javascript -->
<%=JSLoader.getTemplateViewer(tc.config.getContextRootPath(), tellme)%>

<!-- Load Viewer Stylesheets -->
<%=CSSLoader.getViewerStyles(tc.config.getContextRootPath())%>
</head>

<body>
<script>
// Global Configuration Variables
<%=JSLoader.getConfigurationJS(tc.config)%>

// Viewer data
var template_id = <%=template_id%>;
var opts = <%=optstr%>;
var templates = <%=tc.getTemplatesListJSON()%>;

var wliburl = "<%=wliburl%>";
var dcdomns = "<%=dcdomns%>";
var dclibns = "<%=dclibns%>";
var pcdomns = "<%=pcdomns%>";
var wflowns = "<%=wflowns%>";

var plan_api = "<%=planApi%>";
var prov_api = "<%=provApi%>";
var run_api = "<%=runApi%>";
var this_api = "<%=thisApi%>";

var components = <%=components%>;
var prop_vals = <%=propVals%>;
var data_tree = <%=dataTree%>;
var beamer_paras = <%=beamerParas%>;
var beamer_maps = <%=beamerMaps%>;

// Initialize viewer
Ext.onReady(function() {
	var tBrowser = new TemplateBrowser('<%=guid%>', 
		opts, 
		{
			tree: templates,
			components: {
				tree: components
			},
			propvals : prop_vals,
			data: {
				tree: data_tree,
			},
			beamer_paraphrases: beamer_paras,
			beamer_mappings: beamer_maps
		},
		<%=editor%>,
		<%=tellme%>,
		this_api, plan_api, run_api, run_api, prov_api,
		wliburl, dcdomns, dclibns, pcdomns, wflowns,
		<%=lightReasoner%>
	);
	tBrowser.initialize(template_id); 
});
</script>
</body>
</html>