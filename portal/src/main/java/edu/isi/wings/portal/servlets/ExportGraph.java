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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.kcap.ontapi.KBAPI;
import edu.isi.kcap.ontapi.OntFactory;
import edu.isi.kcap.ontapi.OntSpec;
import edu.isi.wings.portal.classes.config.Config;

/**
 * Servlet exports graph in TDB
 */
public class ExportGraph extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ExportGraph() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		Config config = new Config(request, null, null);
		//if(!config.checkDomain(request, response))
		//	return;
		
		response.addHeader("Access-Control-Allow-Origin", config.getClients());
		response.addHeader("Access-Control-Allow-Credentials", "true");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
		response.addHeader("Access-Control-Allow-Headers",
        "X-Requested-With, Content-Type, X-HTTP-Method-Override");		
		
    String format = request.getParameter("format");
		String uri = config.getServerUrl() + request.getRequestURI();
		OntFactory tdbfac = new OntFactory(OntFactory.JENA, config.getTripleStoreDir());
		try {
		  Pattern complibpat = Pattern.compile(".+\\/components\\/(.+)\\.owl");
			KBAPI kb = tdbfac.getKB(uri, OntSpec.PLAIN);
			
			Matcher mat = complibpat.matcher(uri);
			if(mat.find()) {
			  String abslib = "abstract";
			  String complib = mat.group(1);
			  if(!complib.equals(abslib)) {
			    String absuri = uri.replace(complib, abslib);
			    tdbfac.start_write_transaction();
			    kb.copyFrom(tdbfac.getKB(absuri, OntSpec.PLAIN));
			    tdbfac.end_transaction();
			  }
			}
			
	    tdbfac.start_read_transaction();
			if(kb.getAllTriples().size() > 0) {
				response.setContentType("application/rdf+xml");
				if(format != null) {
				  if(format.equals("json"))
				    out.println(kb.toJson());
				  else if(format.equals("n3"))
				    out.println(kb.toN3());
				}
				else
				  out.println(kb.toAbbrevRdf(true));
			}
			tdbfac.end_transaction();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}

}
