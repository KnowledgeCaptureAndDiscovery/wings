package edu.isi.wings.portal.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.catalog.provenance.ProvenanceFactory;
import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.JsonHandler;

/**
 * Servlet implementation class QueryProvenance
 */
public class QueryProvenance extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public QueryProvenance() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Config config = new Config(request);
    if(!config.checkDomain(request, response, false)) {
      response.getWriter().print("[]");
      return;
    }

    String[] args = config.getScriptArguments();
    String op = args.length > 0 ? args[0] : null;
    if(op == null)
      return;
    
    if(op.equals("getItemProvenance")) {
      String id = request.getParameter("id");
      ProvenanceAPI prov = ProvenanceFactory.getAPI(config.getProperties());
      response.getWriter().print(
          JsonHandler.createGson().toJson(prov.getProvenance(id).getActivities()));
    }
    else if(op.equals("getUserActivity")) {
      String userid = request.getParameter("userid");
      ProvenanceAPI prov = ProvenanceFactory.getAPI(config.getProperties());
      response.getWriter().print(
          JsonHandler.createGson().toJson(prov.getAllUserActivities(userid)));
    }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	  doGet(request, response);
	}

}
