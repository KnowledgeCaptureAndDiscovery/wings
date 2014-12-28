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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.controllers.DomainController;

/**
 * Servlet implementation class ManageData
 */
public class ManageDomains extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ManageDomains() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Config config = new Config(request);
		if(!config.checkDomain(request, response))
			return;
		
		String[] args = config.getScriptArguments();
		String op = args.length > 0 ? args[0] : null;

		if (op != null && op.equals("intro")) {
			PrintWriter out = response.getWriter();
			out.println("<div class='x-toolbar x-toolbar-default highlightIcon' " + 
					"style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Manage Domain</div>\n" + 
					"<div style='padding:5px; line-height:1.5em'>\n" + 
					"With this interface, you can:\n" + 
					"<ul>\n" + 
					"   <li>View Domains by clicking on domains on the left</li>\n" +
					"   <li>Add new Domains by clicking on the <b>Add Domain</b> button</li>\n" +
					"   <li>Delete Domains by clicking on the <b>Delete</b> button</li>\n" +
					"   <li>After opening a Domain you may:\n" + 
					"   <ul>\n" + 
					"      <li><b>Edit</b> Properties for that Domain</li>\n" +
					//"      <li><b>Export</b> the domain to the shared triple store</li>\n" +
					//"      <li><b>Remove</b> the domain from the shared triple store</li>\n" +
					"   </ul>\n" + 
					"   </li>\n" + 
					"</ul>\n" + 
					"</div>\n");
			return;
		}

		int guid = 1;
		DomainController dc = new DomainController(guid, config);
    String domain = request.getParameter("domain");

    if (op != null && op.equals("downloadDomain")) {
      dc.streamDomain(domain, response, this.getServletContext());
      return;
    }
    
    PrintWriter out = response.getWriter();
    if (op == null || op.equals("")) {
      response.setContentType("text/html");
      dc.show(out);
    }
    else if(op.equals("getDomainDetails")) {
      out.println(dc.getDomainJSON(domain));
    }
    // Domain write functions only for owner viewers
    else if(!config.getViewerId().equals(config.getUserId())) {
      return;
    }
    else if(op.equals("selectDomain")) {
      if(dc.selectDomain(domain))
        out.print("OK");
    }
    else if (op.equals("createDomain")) {
      out.println(dc.createDomain(domain));
    } 
    else if (op.equals("deleteDomain")) {
      if (dc.deleteDomain(domain))
        out.print("OK");
    } 
    else if (op.equals("renameDomain")) {
      String newname = request.getParameter("newname");
      if (dc.renameDomain(domain, newname))
        out.print("OK");
    } 
    else if (op.equals("importDomain")) {
      String location = request.getParameter("location");
      out.println(dc.importDomain(domain, location));
    }
    else if (op.equals("setDomainExecutionEngine")) {
      String engine = request.getParameter("engine");
      if(dc.setDomainExecutionEngine(domain, engine))
        out.print("OK");
    }
    else if (op.equals("setDomainPermissions")) {
      String permissions_json = request.getParameter("permissions_json");
      if(dc.setDomainPermissions(domain, permissions_json))
        out.print("OK");
    }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
