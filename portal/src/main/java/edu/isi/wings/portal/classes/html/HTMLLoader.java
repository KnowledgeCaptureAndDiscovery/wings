package edu.isi.wings.portal.classes.html;

import java.io.PrintWriter;

public class HTMLLoader {

	public static void printHeader(PrintWriter out) {
	  out.println("<!DOCTYPE html>");
		out.println("<html>");
	}

	public static void printFooter(PrintWriter out) {
		out.println("</html>");
	}
}
