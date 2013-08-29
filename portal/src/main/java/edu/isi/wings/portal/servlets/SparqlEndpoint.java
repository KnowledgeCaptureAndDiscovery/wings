package edu.isi.wings.portal.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.isi.wings.portal.classes.Config;

/**
 * Servlet implementation class SparqlEndpoint
 */
public class SparqlEndpoint extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
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
		ServletOutputStream out = response.getOutputStream();
		String queryString = request.getParameter("query");
		if (queryString == null) {
			response.setContentType("text/html");
			out.print("<form>"
					+ "<h1>Wings Portal Sparql endpoint</h1>"
					+ "<h4>Enter select query below</h4>"
					+ "<textarea name='query' rows='20' cols='100'></textarea>"
					+ "<br/>"
					+ "<input type='submit'/>"
					+ "</form>");
			return;
		}
		
		try {
			Query query = QueryFactory.create(queryString);
			if(query.isSelectType()) {
				Config config = new Config(request);
				Dataset tdbstore = TDBFactory.createDataset(config.getTripleStoreDir());
				QueryExecution qexec = QueryExecutionFactory.create(query, tdbstore);
				qexec.getContext().set(TDB.symUnionDefaultGraph, true);
				ResultSet results = qexec.execSelect();
				ResultSetFormatter.out(response.getOutputStream(), results, query);
			}
			else {
				response.getOutputStream().print("Only select queries allowed");
			}
		}
		catch (Exception e) {
			response.getOutputStream().print(e.getMessage());
		}
		response.getOutputStream().flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
