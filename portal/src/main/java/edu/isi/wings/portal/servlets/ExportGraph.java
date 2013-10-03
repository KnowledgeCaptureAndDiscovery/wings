package edu.isi.wings.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.portal.classes.Config;

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
		Config config = new Config(request);
		if(!config.checkDomain(response))
			return;
		
		String uri = config.getServerUrl() + request.getRequestURI();
		OntFactory tdbfac = new OntFactory(OntFactory.JENA, config.getTripleStoreDir());
		try {
			KBAPI kb = tdbfac.getKB(uri, OntSpec.PLAIN);
			if(kb.getAllTriples().size() > 0) {
				response.setContentType("text/plain");
				out.println(kb.toAbbrevRdf(true));
			}
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
