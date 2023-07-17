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

package edu.isi.wings.portal.servlets;

import edu.isi.kcap.ontapi.util.SparqlAPI;
import edu.isi.wings.portal.classes.config.Config;
import edu.isi.wings.portal.classes.domains.DomainInfo;
import edu.isi.wings.portal.controllers.DomainController;
import java.io.IOException;
import java.io.PrintStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SparqlEndpoint
 */
public class SparqlEndpoint extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private PrintStream out;
  private SparqlAPI api;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public SparqlEndpoint() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(
    HttpServletRequest request,
    HttpServletResponse response
  ) throws ServletException, IOException {
    this.out = new PrintStream(response.getOutputStream());

    Config config = new Config(request, null, null);
    String queryString = request.getParameter("query");
    String updateString = request.getParameter("update");
    if (queryString == null && updateString == null) {
      response.setContentType("text/html");
      out.println("<script>USER_ID=\"" + config.getViewerId() + "\";</script>");
      out.println(
        "<form>" +
        "<h1>Wings Portal Sparql endpoint</h1>" +
        "<h4>Enter select query below</h4>" +
        "<textarea name='query' rows='20' cols='100'></textarea>" +
        "<h4>Enter update query below</h4>" +
        "<textarea name='update' rows='20' cols='100'></textarea>" +
        "<br/>" +
        "<input type='submit'/>" +
        "</form>"
      );
      return;
    }

    try {
      if (queryString != null && !queryString.equals("")) this.showQueryResults(
          queryString,
          request,
          response
        ); else if (
        updateString != null && !updateString.equals("")
      ) this.updateDataset(updateString, request, response);
    } catch (Exception e) {
      response.getOutputStream().print(e.getMessage());
    }
    response.getOutputStream().flush();
  }

  private void showQueryResults(
    String queryString,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws IOException {
    Config config = new Config(request, null, null);
    String tdbdir = config.getTripleStoreDir();
    String format = request.getParameter("format");

    this.api = new SparqlAPI(tdbdir);
    this.api.showQueryResults(queryString, format, out);
  }

  private void updateDataset(
    String updateString,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws IOException {
    Config config = new Config(request, null, null);
    if (!config.checkDomain(request, response)) return;

    String tdbdir = config.getTripleStoreDir();
    this.api = new SparqlAPI(tdbdir);

    if (updateString.startsWith("__SERVER_RENAME:")) {
      String newurl = updateString.substring(updateString.indexOf(":") + 1);
      this.updateServerURL(newurl, config);
    } else {
      this.api.executeUpdateQuery(updateString, out);
    }
  }

  private void updateServerURL(String newurl, Config config) {
    String cururl = config.getServerUrl();

    // Update all graphs in the triple store
    this.api.updateGraphURLs(cururl, newurl, out);

    // Update all User domains
    for (String userid : config.getUsersList()) {
      config.setUserId(userid);
      config.setViewerId(userid);
      DomainController dc = new DomainController(config);
      for (String domname : dc.getDomainsList()) {
        DomainInfo dominfo = dc.getDomainInfo(domname);
        String url = dominfo.getUrl();
        if (url.startsWith(cururl)) {
          try {
            out.println(
              "* changing " + userid + "'s domain url for " + domname
            );
          } catch (Exception e) {
            System.out.println(
              "* changing " + userid + "'s domain url for " + domname
            );
          }
          url = url.replace(cururl, newurl);
          dc.setDomainURL(domname, url);
        }
      }
    }
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(
    HttpServletRequest request,
    HttpServletResponse response
  ) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }
}
