package edu.isi.wings.portal.classes.html;

import java.io.PrintWriter;

public class HTMLLoader {

	public static void loadHeader(PrintWriter out) {
		out.println("<html>");
	}

	public static void loadFooter(PrintWriter out) {
		out.println("</html>");
	}
}
