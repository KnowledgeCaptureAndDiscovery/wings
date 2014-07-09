package edu.isi.wings.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.WriteLock;
import edu.isi.wings.portal.controllers.ResourceController;

/**
 * Servlet implementation class ManageResources
 */
public class DescribeResources extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DescribeResources() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Config config = new Config(request);

    String path = request.getPathInfo();
    if (path == null)
      path = "/";
    String[] args = path.split("\\/");
    String op = args.length > 1 ? args[1] : null;
    
    if (op != null && op.equals("intro")) {
      PrintWriter out = response.getWriter();
      out.println("<div class='x-toolbar x-toolbar-default highlightIcon' "
          + "style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Resources</div>\n"
          + "<div style='padding:5px; line-height:1.5em'>\n"
          + "With this interface, you can:\n"
          + "<ul>\n"
          + "   <li>View Resources</li>\n"
          + "   <li>Add new Resources</li>\n"
          + "</ul>\n" 
          + "</div>\n");
      return;
    }
    
    int guid = 1;

    synchronized (WriteLock.Lock) {
      ResourceController rc = new ResourceController(guid, config);
      String resid = request.getParameter("resid");
      PrintWriter out = response.getWriter();
      
      // Reader functions
      if (op == null || op.equals("")) {
        response.setContentType("text/html");
        rc.show(out);
        return;
      } 
      else if (op.equals("getMachineJSON")) {
        out.println(rc.getMachineJSON(resid));
      }
      else if (op.equals("getSoftwareJSON")) {
        out.println(rc.getSoftwareJSON(resid));
      }
      else if (op.equals("getSoftwareVersionJSON")) {
        out.println(rc.getSoftwareVersionJSON(resid));
      }
      else if (op.equals("getAllSoftwareVersions")) {
        out.println(rc.getAllSoftwareVersions());
      }
      else if (op.equals("getAllSoftwareEnvironment")) {
        out.println(rc.getAllSoftwareEnvironment());
      }
      else if (op.equals("checkMachine")) {
        out.println(rc.checkMachine(resid));
      }
      
      // Writer functions
      if (op.equals("addMachine")) {
        if (rc.addMachine(resid))
          out.print("OK");
      }
      else if (op.equals("addSoftware")) {
        if (rc.addSoftware(resid))
          out.print("OK");
      }
      else if (op.equals("addSoftwareVersion")) {
        String softwareid = request.getParameter("softwareid");
        if (rc.addSoftwareVersion(resid, softwareid))
          out.print("OK");
      }
      else if (op.equals("saveMachineJSON")) {
        String resvals_json = request.getParameter("json");
        if (rc.saveMachineJSON(resid, resvals_json))
          out.print("OK");
      }
      else if (op.equals("saveSoftwareJSON")) {
        String resvals_json = request.getParameter("json");
        if (rc.saveSoftwareJSON(resid, resvals_json))
          out.print("OK");
      }
      else if (op.equals("saveSoftwareVersionJSON")) {
        String resvals_json = request.getParameter("json");
        if (rc.saveSoftwareVersionJSON(resid, resvals_json))
          out.print("OK");
      }
      else if (op.equals("removeMachine")) {
        if (rc.removeMachine(resid))
          out.print("OK");
      }
      else if (op.equals("removeSoftwareVersion")) {
        if (rc.removeSoftwareVersion(resid))
          out.print("OK");
      }
      else if (op.equals("removeSoftware")) {
        if (rc.removeSoftware(resid))
          out.print("OK");
      }
    }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	  doGet(request, response);
	}

}
