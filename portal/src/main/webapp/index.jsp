<%@page import="java.io.PrintWriter"
%><%@page import="edu.isi.wings.portal.classes.html.CSSLoader"
%><%@page import="edu.isi.wings.portal.classes.Config"
%><%@page import="edu.isi.wings.portal.classes.html.JSLoader"
%><%@page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"
%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Wings Portal</title>
<%
  Config configx = new Config(request);
PrintWriter outx = new PrintWriter(out);
JSLoader.loadConfigurationJS(outx, configx);
JSLoader.loadLoginViewer(outx, configx.getContextRootPath());
CSSLoader.loadLoginViewer(outx, configx.getContextRootPath());
%><script>
var message="<%=request.getAttribute("message")%>";
var nohome="<%=request.getAttribute("nohome")%>";
Ext.onReady(function() {
	var mainpanel = new Ext.Viewport({
		layout : 'border',
		items: [
			getPortalHeader(CONTEXT_ROOT),
			{
				xtype: 'panel',
				region: 'center',
				bodyPadding: 10,
				border: false,
				autoScroll: true,
				listeners: {
					'afterrender': function(comp) {
						if(nohome == "null") {
							Ext.Ajax.request({
								url: CONTEXT_ROOT + "/jsp/home.jsp",
								success: function(response) {
									comp.update(response.responseText);
								}
							});
						}
						else if (message != "null") {
							comp.update(message);
						}
					}
				}
			}
		]
	});
});
</script>
</head>
</html>