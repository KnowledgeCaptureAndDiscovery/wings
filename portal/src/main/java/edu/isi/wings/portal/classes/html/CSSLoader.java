/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.isi.wings.portal.classes.html;

import java.io.PrintWriter;

public class CSSLoader {
	static String theme = "classic";
	static String[] site_css = { "css/menu.css", "css/app.css" 
	  , "css/fontello/css/wings-icons.css" };
	static String[] animation_css = { "css/fontello/css/animation.css" };
	static String[] extjs_css = { "lib/extjs/resources/ext-theme-"+theme+"/ext-theme-"+theme+"-all.css" };

	public static void loadLoginViewer(PrintWriter out, String path) {
		showCssTags(out, path, site_css);
		showCssTags(out, path, extjs_css);
	}
	
	public static void loadDataViewer(PrintWriter out, String path) {
		showCssTags(out, path, site_css);
		showCssTags(out, path, extjs_css);
	}

  public static void loadResourceViewer(PrintWriter out, String path) {
    showCssTags(out, path, site_css);
    showCssTags(out, path, extjs_css);
  }
  
  public static void loadUserViewer(PrintWriter out, String path) {
    showCssTags(out, path, site_css);
    showCssTags(out, path, extjs_css);
  }
	 
	public static void loadComponentViewer(PrintWriter out, String path) {
		showCssTags(out, path, site_css);
		showCssTags(out, path, extjs_css);
	}

	public static void loadDomainViewer(PrintWriter out, String path) {
		showCssTags(out, path, site_css);
		showCssTags(out, path, extjs_css);
	}

	public static void loadRunViewer(PrintWriter out, String path) {
		showCssTags(out, path, site_css);
		showCssTags(out, path, animation_css);
		showCssTags(out, path, extjs_css);
	}

	public static void loadTemplateViewer(PrintWriter out, String path) {
		showCssTags(out, path, site_css);
		showCssTags(out, path, extjs_css);
	}

	private static void showCssTags(PrintWriter out, String path, String[] css) {
		for (String href : css) {
		  String url = path + "/" + href;
		  if(href.matches("http:.*"))
		    url = href;
			out.println("<link rel=\"stylesheet\" href=\"" + url + "\"/>");
		}
	}
}
