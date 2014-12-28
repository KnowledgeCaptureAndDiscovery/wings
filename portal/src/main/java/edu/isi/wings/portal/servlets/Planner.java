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
import edu.isi.wings.portal.controllers.PlanController;

/**
 * Servlet implementation class Planner
 */
public class Planner extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Planner() {
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

		String op = request.getParameter("op");
		String[] args = config.getScriptArguments();
		if (op == null) {
			op = args.length > 0 ? args[0] : null;
		}
		
		PlanController wp = new PlanController(config, out);;
		
		String tplid = request.getParameter("__template_id");
		if(op.equals("getExpansions")) {
			wp.printExpandedTemplatesJSON(tplid, request.getParameterMap());
		}
		else if(op.equals("getData")) {
			wp.printSuggestedDataJSON(tplid, request.getParameterMap());
		}
		else if(op.equals("getParameters")) {
			wp.printSuggestedParametersJSON(tplid, request.getParameterMap());
		}
		else if(op.equals("elaborateTemplateJSON")) {
			String tpljson = request.getParameter("json");
			String consjson = request.getParameter("constraints_json");
			wp.printElaboratedTemplateJSON(tplid, tpljson, consjson);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
