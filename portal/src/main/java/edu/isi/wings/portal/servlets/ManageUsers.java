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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.isi.wings.portal.classes.Config;
import edu.isi.wings.portal.controllers.UserController;

/**
 * Servlet implementation class ManageUser
 */
public class ManageUsers extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ManageUsers() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Config config = new Config(request);

    String path = request.getPathInfo();
    if (path == null)
      path = "/";
    String[] args = path.split("\\/");
    String op = args.length > 1 ? args[1] : null;

    if (op != null && op.equals("intro")) {
      PrintWriter out = response.getWriter();
      out.println("<div class='x-toolbar x-toolbar-default highlightIcon' "
          + "style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Manage Users</div>\n"
          + "<div style='padding:5px; line-height:1.5em'>\n"
          + "With this interface, you can:\n" + "<ul>\n"
          + "   <li>View User information</li>\n"
          + "   <li>Edit user information</li>\n"
          + "   <li>Add new users</li>\n"
          + "   <li>Remove existing users</li>\n"
          + "</ul>\n" + "</div>\n");
      return;
    }

    int guid = 1;

    UserController uc = new UserController(guid, config);
    String userid = request.getParameter("userid");

    PrintWriter out = response.getWriter();
    // Reader functions
    if (op == null || op.equals("")) {
      response.setContentType("text/html");
      uc.show(out);
      return;
    } else if (op.equals("getUserJSON")) {
      out.println(uc.getUserJSON(userid));
    }

    // Writer functions
    if (op.equals("saveUserJSON")) {
      String uservals_json = request.getParameter("json");
      if (uc.saveUserJSON(userid, uservals_json))
        out.print("OK");
    }
    else if(op.equals("addUser")) {
      if(uc.addUser(userid))
        out.print("OK");
    }
    else if(op.equals("removeUser")) {
      if(uc.removeUser(userid))
        out.print("OK");
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
