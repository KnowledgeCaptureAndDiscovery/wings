<%
// Licensed to the Apache Software Foundation (ASF) under one or more contributor
// license agreements.  See the NOTICE.txt file distributed with this work for
// additional information regarding copyright ownership.  The ASF licenses this
// file to you under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy of
// the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
// License for the specific language governing permissions and limitations under
// the License.
%>

<%@page import="java.io.PrintWriter"
%><%@page import="edu.isi.wings.portal.classes.html.CSSLoader"
%><%@page import="edu.isi.wings.portal.classes.config.Config"
%><%@page import="edu.isi.wings.portal.classes.html.JSLoader"
%><%@page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"
%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Wings Portal</title>
<%
Config configx = new Config(request, null, null);
%>

<!-- Load Viewer Javascript -->
<%=JSLoader.getLoginViewer(configx.getContextRootPath())%>

<!-- Load Viewer Stylesheets -->
<%=CSSLoader.getViewerStyles(configx.getContextRootPath())%>
</head>

<body>
<script>
// Global Configuration Variables
<%=JSLoader.getConfigurationJS(configx)%>

// Load home page
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
	            loader: {
	            	url: CONTEXT_ROOT + "/html/home.html", 
	                autoLoad: (nohome == "null") ? true : null
	            },
				listeners: {
					'afterrender': function(comp) {
						if(nohome != "null" && message != "null") {
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
