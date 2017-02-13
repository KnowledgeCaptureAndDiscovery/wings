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
<title>Login to Wings</title>
<%
Config configx = new Config(request, null, null); //(Config) request.getAttribute("config");
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

// Initialize Login Box
var message="<%=request.getParameter("message")%>";
Ext.onReady(function() {
	var mainpanel = new Ext.Viewport({
		layout : 'border',
		items: [
			getPortalHeader(CONTEXT_ROOT),
			{
				region: 'center',
				xtype: 'panel',
				layout: {
					type: 'vbox',
					align: 'center'
				},
				bodyCls: 'disabledCanvas',
				frame: false,
				bodyPadding: 50,
				items: [{
					xtype: 'form',
					layout:'form',
					url: '<%=configx.getContextRootPath()%>/j_security_check',
					title: 'Login to the Portal',
					frame: true,
					bodyPadding: 10,
					width: 400,
					fieldDefaults: {
						labelWidth: 75
					},
					defaultType: 'textfield',
					items: [
					{
						xtype: 'text',
						style: 'font-style:italic',
						text: (message != "null" ? message : '')
					},
					{
						fieldLabel: 'Username',
						name: 'j_username',
						allowBlank: false
					}, {
						fieldLabel: 'Password',
						name: 'j_password',
						inputType: 'password',
						allowBlank: false,
			            listeners: {
			                specialkey: function(field, e){
			                    if (e.getKey() == e.ENTER) {
			                    	this.up('form').getForm().standardSubmit=true;
									this.up('form').getForm().submit();
			                    }
			                }
			            },
					}],
					
					buttons: [{
						text: 'Login',
						handler: function() {
							this.up('form').getForm().standardSubmit=true;
							this.up('form').getForm().submit();
						}
					}],
				}]
			}
		]
	});
});
</script>
</head>
<body>

</body>
</html>
