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
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import edu.isi.wings.portal.classes.JsonHandler;
import edu.isi.wings.portal.classes.config.Config;

/**
 * Servlet exports graph in TDB
 */
public class ViewConfig extends HttpServlet {
  private static final long serialVersionUID = 1L;
  
  /**
   * @see HttpServlet#HttpServlet()
   */
  public ViewConfig() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    PrintWriter out = response.getWriter();
    Config config = new Config(request, null, null);
    Gson json = JsonHandler.createPrettyGson();
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put("internal_server", config.getServerUrl());
    props.put("storage", config.getStorageDirectory());
    props.put("dotpath", config.getDotFile());
    props.put("ontology", config.getWorkflowOntologyUrl());
    props.put("planner", config.getPlannerConfig());
    out.println(json.toJson(props));
    out.close();
  }

}
