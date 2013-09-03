package edu.isi.wings.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.WriteLock;
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
		PrintWriter out = response.getWriter();
		Config config = new Config(request);
		
		String path = request.getPathInfo();
		if (path == null)
			path = "/";
		String[] args = path.split("\\/");
		String op = args.length > 1 ? args[1] : null;

		if (op != null && op.equals("intro")) {
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
//		String libname = request.getParameter("libname");
		
		if (op == null || op.equals("")) {
			response.setContentType("text/html");
			dc.show(out);
			return;
		}
		
		if(op.equals("getDomainDetails")) {
			out.println(dc.getDomainJSON(domain));
		}
		else if(op.equals("selectDomain")) {
			if(dc.selectDomain(domain))
				out.print("OK");
		}
		
		synchronized (WriteLock.Lock) {
			if (op.equals("deleteDomain")) {
				if (dc.deleteDomain(domain))
					out.print("OK");
			} else if (op.equals("importDomain")) {
				String location = request.getParameter("location");
				out.println(dc.importDomain(domain, location));
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
