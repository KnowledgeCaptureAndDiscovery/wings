package edu.isi.wings.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.WriteLock;
import edu.isi.wings.portal.controllers.RunController;

/**
 * Servlet implementation class ManageRuns
 */
public class ManageRuns extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ManageRuns() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		Config config = new Config(request);
		if(!config.checkDomain(response))
			return;
		
		ServletContext context = this.getServletContext();
		
		String[] args = config.getScriptArguments();
		String op = args.length > 0 ? args[0] : null;

		if (op != null && op.equals("intro")) {
			out.println("<div class='x-toolbar x-toolbar-default highlightIcon' " + 
					"style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Manage Runs</div>\n" + 
					"<div style='padding:5px; line-height:1.5em'>\n" + 
					"With this interface, you can:\n" + 
					"<ul>\n" + 
					"   <li>View Runs by clicking on runs on top</li>\n" +
					"   <li>Delete Runs by clicking on the <b>Delete</b> button</li>\n" +
					"   <li>After opening a Run you can:\n" + 
					"   <ul>\n" + 
					"      <li><b>View</b> Execution Plan and other Run Details</li>\n" +
//					"      <li><b>Re-Run</b> the Execution Plan</li>\n" +
					"   </ul>\n" + 
					"   </li>\n" + 
					"</ul>\n" + 
					"</div>\n");
			return;
		}

		int guid = 1;
		RunController rc;
		synchronized(WriteLock.Lock) {
			rc = new RunController(guid, config);
		}
		
		String runid = request.getParameter("run_id");
		if (op == null || op.equals("")) {
			response.setContentType("text/html");
			rc.show(out, runid);
			return;
		}
		synchronized(WriteLock.Lock) {
      if (op.equals("getRunList")) {
        out.println(rc.getRunListJSON());
      } else if (op.equals("getRunDetails")) {
        out.println(rc.getRunJSON(runid));
      } else if (op.equals("runWorkflow")) {
        String origtplid = request.getParameter("template_id");
        String tpljson = request.getParameter("json");
        String consjson = request.getParameter("constraints_json");
        String seedjson = request.getParameter("seed_json");
        String seedconsjson = request.getParameter("seed_constraints_json");
        out.print(rc.runExpandedTemplate(origtplid, tpljson, consjson,
            seedjson, seedconsjson, context));
      } else if (op.equals("deleteRun")) {
        out.println(rc.deleteRun(request.getParameter("json"), context));
      } else if (op.equals("stopRun")) {
        out.println(rc.stopRun(runid, context));
      }
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
