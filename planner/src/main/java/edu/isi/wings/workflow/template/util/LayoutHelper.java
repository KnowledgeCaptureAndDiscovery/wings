package edu.isi.wings.workflow.template.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LayoutHelper {
	static public double[] parseCommentString(String s) {
		Pattern pat = Pattern.compile("x=([\\d\\.]+),y=([\\d\\.]+)");
		Matcher m = pat.matcher(s);
		if (m.find()) {
			double[] pos = new double[2];
			try {
				pos[0] = Double.parseDouble(m.group(1));
				pos[1] = Double.parseDouble(m.group(2));
				return pos;
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	static public String createCommentString(double x, double y) {
		return "x=" + x + ",y=" + y;
	}
}
