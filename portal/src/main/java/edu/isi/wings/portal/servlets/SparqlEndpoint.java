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
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.domains.DomainInfo;
import edu.isi.wings.portal.controllers.DomainController;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Servlet implementation class SparqlEndpoint
 */
public class SparqlEndpoint extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private ServletOutputStream out;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SparqlEndpoint() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.out = response.getOutputStream();
		String queryString = request.getParameter("query");
		String updateString = request.getParameter("update");
		if (queryString == null && updateString == null) {
			response.setContentType("text/html");
			out.print("<form>"
					+ "<h1>Wings Portal Sparql endpoint</h1>"
					+ "<h4>Enter select query below</h4>"
					+ "<textarea name='query' rows='20' cols='100'></textarea>"
					+ "<h4>Enter update query below</h4>"
					+ "<textarea name='update' rows='20' cols='100'></textarea>"
					+ "<br/>"
					+ "<input type='submit'/>"
					+ "</form>");
			return;
		}
		
		try {
			if(queryString != null && !queryString.equals(""))
				this.showQueryResults(queryString, request, response);
			else if(updateString != null && !updateString.equals(""))
				this.updateDataset(updateString, request, response);
		}
		catch (Exception e) {
			response.getOutputStream().print(e.getMessage());
		}
		response.getOutputStream().flush();
	}

	private void showQueryResults(String queryString, HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		Query query = QueryFactory.create(queryString);
		if(query.isSelectType()) {
			Config config = new Config(request);
			if(!config.checkDomain(request, response))
				return;
			ResultsFormat fmt = ResultsFormat.lookup(request.getParameter("format"));
			
			Dataset tdbstore = TDBFactory.createDataset(config.getTripleStoreDir());
			QueryExecution qexec = QueryExecutionFactory.create(query, tdbstore);
			qexec.getContext().set(TDB.symUnionDefaultGraph, true);
			ResultSet results = qexec.execSelect();
			if(fmt == null) {
				out.print(queryString+"\n");
				ResultSetFormatter.out(out, results, query);
			}
			else
				ResultSetFormatter.output(out, results, fmt);
		}
		else {
			out.print("Only select queries allowed");
		}
	}
	
	private void updateDataset(String updateString, HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		Config config = new Config(request);
		if(!config.checkDomain(request, response))
			return;
		Dataset tdbstore = TDBFactory.createDataset(config.getTripleStoreDir());
		if(updateString.startsWith("__SERVER_RENAME:")) {
	    String newurl = updateString.substring(updateString.indexOf(":")+1);
	    this.updateServerURL(newurl, tdbstore, config);
		}
		else {
  		UpdateRequest update = UpdateFactory.create(updateString);
  		UpdateAction.execute(update, tdbstore);
		}
		out.print("Updated");
		TDB.sync(tdbstore);
	}
	
	private void updateServerURL(String newurl, Dataset tdbstore, Config config) {
    String cururl = config.getServerUrl();
    try {out.println(cururl +" ==>> "+newurl);}
    catch (Exception e) {System.out.println(cururl +" ==>> "+newurl);}
    
    // Update all graphs in the Dataset
    ArrayList<String> graphnames = new ArrayList<String>();
    try {
      Query query = QueryFactory.create("SELECT DISTINCT ?g { GRAPH ?g { ?s ?p ?o }}");
      QueryExecution qexec = QueryExecutionFactory.create(query, tdbstore);
      qexec.getContext().set(TDB.symUnionDefaultGraph, true);
      ResultSet results = qexec.execSelect();
      while(results.hasNext()) {
        QuerySolution soln = results.next();
        RDFNode graph = soln.get("g");
        if(graph.isURIResource())
          graphnames.add(graph.asResource().getURI());
      }
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    
    for(String graphname : graphnames) {
      if(graphname.startsWith(cururl)) {
        String newname = graphname.replace(cururl, newurl);
        try {out.println(graphname + " -> "+newname);}
        catch (Exception e) {System.out.println(graphname + " -> "+newname);}
        
        try {
          Model m = tdbstore.getNamedModel(graphname);
          Model nm = ModelFactory.createDefaultModel();
          
          for(StmtIterator iter = m.listStatements(); iter.hasNext(); ) {
            Statement st = iter.next();
            Resource subj = st.getSubject();
            Property pred = st.getPredicate();
            RDFNode obj = st.getObject();
            if(subj.isURIResource() && subj.getURI().startsWith(cururl)) {
              String nurl = subj.getURI().replace(cururl, newurl);
              subj = new ResourceImpl(nurl);
            }
            if(pred.getURI().startsWith(cururl)) {
              String nurl = pred.getURI().replace(cururl, newurl);
              pred = new PropertyImpl(nurl);
            }
            if(obj.isURIResource() && obj.asResource().getURI().startsWith(cururl)) {
              String nurl = obj.asResource().getURI().replace(cururl, newurl);
              obj = new ResourceImpl(nurl);
            }
            nm.add(new StatementImpl(subj, pred, obj));
          }
          tdbstore.removeNamedModel(graphname);
          tdbstore.addNamedModel(newname, nm);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    TDB.sync(tdbstore);
    
    // Update all User domains
    for(String userid : config.getUsersList()) {
      config.setUserId(userid);
      config.setViewerId(userid);
      DomainController dc = new DomainController(1, config);
      for(String domname : dc.getDomainsList()) {
        DomainInfo dominfo = dc.getDomainInfo(domname);
        String url = dominfo.getUrl();
        if(url.startsWith(cururl)) {
          try {
            out.println("* changing "+userid+"'s domain url for "+domname);
          }
          catch(Exception e) {
            System.out.println("* changing "+userid+"'s domain url for "+domname);
          }
          url = url.replace(cururl, newurl);
          dc.setDomainURL(domname, url);
        }
      }
    }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
