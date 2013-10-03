package edu.isi.wings.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.classes.WriteLock;
import edu.isi.wings.portal.controllers.ComponentController;

/**
 * Servlet implementation class ManageComponents
 */
public class ManageComponents extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ManageComponents() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Config config = new Config(request);
		if(!config.checkDomain(response))
			return;

		boolean loadConcrete = true;
		String path = request.getPathInfo();
		if (path == null) path = "/";
		String[] args = path.split("\\/");
		String op = args.length > 1 ? args[1] : null;
		if(op != null && op.equals("type")) {
			loadConcrete = false;
			config.setScriptPath(config.getScriptPath()+"/"+op);
			op = args.length > 2 ? args[2] : null;
		}
		
		if (op != null && op.equals("intro")) {
			PrintWriter out = response.getWriter();
			String txt = "Component" + (!loadConcrete ? " Types" : "s");
			out.println("<div class='x-toolbar x-toolbar-default highlightIcon' " + 
					"style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Manage " + txt + "</div>\n" + 
					"<div style='padding:5px; line-height:1.5em'>\n" + 
					"With this interface, you can:\n" + 
					"<ul>\n" + 
					"   <li>View/Edit " + txt + " by clicking on the links in the tree on the left</li>\n" + 
					"   <li>After opening a " + txt + " you may edit its input and output descriptions:\n" + 
					"   <ul>\n" + 
					"      <li>Specifiy a <b>Role</b> name for the Input/Output</li>\n" + 
					"      <li>If an Input isn't a file, it is an Input Parameter</li>\n" + 
					"      <li>Specify a <b>Prefix</b> for the Input/Output in the argument string. Example:" +
					"        <ul style='font:12px Courier'><li>-i1 [inputfile1] -p1 [param1] -o1 [output1]</li></ul>\n" +
					"      </li>" +
					"      <li>Specify the <b>Dimensionality</b> of the Input/Output (equal to 1 for a 1-D collection, 0 for a single file)</li>\n" +
					"      <li>Specify the <b>Default Value</b> of Parameters</li>\n" +
					"   </ul>\n" + 
					"   </li>\n" + 
					"   <li>You may also edit Rules by Editing the \"Rules\" Tab</li>\n" + 
					"</ul>\n" + 
					"</div>");
			return;
		}

		int guid = 1;
		ComponentController cv;
		synchronized(WriteLock.Lock) {
			cv = new ComponentController(guid, config, loadConcrete);
		}
		String cid = request.getParameter("cid");
		
		if(op != null && op.equals("fetch")) {
			cv.streamComponent(cid, response, this.getServletContext());
			return;
		}
		
		PrintWriter out = response.getWriter();
		if (op == null || op.equals("")) {
			response.setContentType("text/html");
			cv.show(out);
			return;
		} else if (op.equals("getComponentJSON")) {
			out.print(cv.getComponentJSON(cid));
		}
		
		synchronized(WriteLock.Lock) {
			if (op.equals("saveComponentJSON")) {
				String component_json = request.getParameter("component_json");
				if (!config.isSandboxed())
					if (cv.saveComponentJSON(cid, component_json))
						out.print("OK");
			} else if (op.equals("addComponent")) {
				String pid = request.getParameter("parent_cid");
				String ptype = request.getParameter("parent_type");
				if (!config.isSandboxed())
					if (cv.addComponent(cid, pid, ptype))
						out.print("OK");
			} else if (op.equals("addCategory")) {
				String ptype = request.getParameter("parent_type");
				if (!config.isSandboxed())
					if (cv.addCategory(cid, ptype))
						out.print("OK");
			} else if (op.equals("delComponent")) {
				if (!config.isSandboxed())
					if (cv.delComponent(cid))
						out.print("OK");
			} else if (op.equals("delCategory")) {
				if (!config.isSandboxed())
					if (cv.delCategory(cid))
						out.print("OK");
			} else if (op.equals("moveComponentTo")) {
				// String frompid = request.getParameter("from_parent_cid");
				// String topid = request.getParameter("to_parent_cid");
				// if (!config.isSandboxed())
				// if (cv.moveComponentTo(cid, frompid, topid))
				// out.print("OK");
			} else if (op.equals("setComponentLocation")) {
				String location = request.getParameter("location");
				if(!config.isSandboxed())
					if(cv.setComponentLocation(cid, location))
						out.print("OK");
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
