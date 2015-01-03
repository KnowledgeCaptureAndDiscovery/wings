/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.isi.wings.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.controllers.TemplateController;

/**
 * Servlet implementation class ManageTemplates
 */
public class ManageTemplates extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ManageTemplates() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		Config config = new Config(request);
		if(!config.checkDomain(request, response))
			return;
		
		// Extract template op and editor/tellme mode
		boolean editor = false;
		boolean tellme = false;
		String[] args = config.getScriptArguments();
		String op = args.length > 0 ? args[0] : null;
		if(op != null && op.equals("edit")) {
			editor = true;
			config.setScriptPath(config.getScriptPath()+"/"+op);
			op = args.length > 1 ? args[1] : null;
		}
		else if(op != null && op.equals("tellme")) {
			editor = true;
			tellme = true;
			config.setScriptPath(config.getScriptPath()+"/"+op);
			op = args.length > 1 ? args[1] : null;
		}
		
		if (op != null && op.equals("intro")) {
			if(!editor) {
				out.println("<div class='x-toolbar x-toolbar-default highlightIcon' " + 
						"style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Run Workflows</div>\n" + 
						"<div style='padding:5px; line-height:1.5em'>\n" + 
						"With this interface, you can:\n" + 
						"<ul>" + 
						"<li>View Workflow Templates by clicking a template name from the tree on the left side</li>\n" + 
						"<li>Set inputs to those templates by entering values in the Input form that appears after opening a template. " +
						"<ul>" +
						"   <li>Click on <b>Suggest Data</b> to get Wings to give Suggestions of Data bindings for this workflow. " +
						"       <i>(Note: For collection inputs, Wings will prompt you to use all possible data which isn't ideal. " +
						"       So be cautious about selecting the right data if you don't want to bog down the system)</i>" +
						"   <li>Click on <b>Suggest Parameter</b> after you've selected the data to get Wings to suggest parameter values" +
						"</ul>" +
						"<li>Set concrete components for Abstract components (in grey) by clicking on them and choosing a concrete" +
						"    component from below</li>" +
						"<li>Click on <b>Plan Workflow</b> to get Wings to create one or more expanded and planned workflows. " +
						"    These will be visible in a popup window where you can choose to <b>Run Workflows</b></li>\n" +  
						"</ul>" +
						"</div>");
			}
			else if(editor) {
				out.println("<div class='x-toolbar x-toolbar-default highlightIcon' " + 
						"style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Edit Workflows</div>\n" + 
						"<div style='padding:5px; line-height:1.5em'>\n" + 
						"With this interface, you can:\n" + 
						"<ul>" + 
						"<li>View Workflow Templates by clicking a template name from the tree on the left side</li>\n" +
						(tellme ? 
								"<li>Edit Templates by writing text in the <b>TellMe</b> window on the left side </li>\n" 
								: "") +  
						"<li>Edit Templates by editing the Graph (add/delete node and links). " +
						"<li>Edit Template Constraints by adding/removing/editing items in the Constraints table " +
						"<ul>" +
						"   <li>Click on <b>Elaborate Template</b> to get Wings to automatically fill out the Constraints table</li>" +
						"</ul>" +
						"<li>Save a Template by clicking on the <b>Save</b> or the <b>Save As</b> button</li>\n" +  
						"</ul>" +
						"</div>"); 
			}
			return;
		}
		
		// Get all options
		HashMap<String , Boolean> options = new HashMap<String, Boolean>();
		@SuppressWarnings("unchecked")
		Enumeration<String> pnames = request.getParameterNames();
		while(pnames.hasMoreElements()) {
			String pname = pnames.nextElement();
			if(pname.matches("^hide_.*"))
				options.put(pname, Boolean.parseBoolean(request.getParameter(pname)));
		}

		int guid = 1;
		TemplateController tv = new TemplateController(guid, config);
		
		String template_id = request.getParameter("template_id");
		if (op == null || op.equals("")) {
			response.setContentType("text/html");
			tv.show(out, options, template_id, editor, tellme);
			return;
		}
		else if(op.matches(".*\\.owl")) {
      response.setContentType("text/html");
      String tname = op.replace(".owl", "");
      String tid = tv.getWliburl()+"/"+op+"#"+tname;
      options.put("hide_selector", true);
      tv.show(out, options, tid, editor, tellme);
      return;
		}
		else if(op.equals("dotLayout")) {
			String dotstr = request.getParameter("dotstr");
			out.print(tv.getDotLayout(dotstr));
		}
		else if(op.equals("getViewerJSON")) {
		  out.println(tv.getViewerJSON(template_id));
		}
		else if(op.equals("getEditorJSON")) {
		  out.println(tv.getEditorJSON(template_id));
		}
		else if(op.equals("saveTemplateJSON")) {
		  String tpljson = request.getParameter("json");
		  String consjson = request.getParameter("constraints_json");
		  out.print(tv.saveTemplateJSON(template_id, tpljson, consjson));
		}
		else if(op.equals("deleteTemplate")) {
		  out.print(tv.deleteTemplate(template_id));
		}
		else if(op.equals("newTemplate")) {
		  out.print(tv.newTemplate(template_id));
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
