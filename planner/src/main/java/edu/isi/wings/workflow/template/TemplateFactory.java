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

package edu.isi.wings.workflow.template;

import edu.isi.wings.common.kb.PropertiesHelper;
import edu.isi.wings.workflow.template.api.Seed;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.TemplateCreationAPI;
import edu.isi.wings.workflow.template.api.impl.kb.SeedKB;
import edu.isi.wings.workflow.template.api.impl.kb.TemplateCreationKB;
import edu.isi.wings.workflow.template.api.impl.kb.TemplateKB;
import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Properties;

public class TemplateFactory {

  // Legacy factory methods (going to be deprecated soon)

  public static Template getTemplate(Properties props, String templateid) {
    if (props == null) props = createLegacyConfiguration();
    return new TemplateKB(props, templateid);
  }

  public static Template createTemplate(Properties props, String templateid) {
    if (props == null) props = createLegacyConfiguration();
    return new TemplateKB(props, templateid, true);
  }

  public static Seed getSeed(Properties props, String seedid) {
    if (props == null) props = createLegacyConfiguration();
    return new SeedKB(props, seedid);
  }

  public static Seed createSeed(
    Properties props,
    String seedid,
    String templateid
  ) {
    if (props == null) props = createLegacyConfiguration();
    return new SeedKB(props, seedid, templateid);
  }

  /**
   * @param props
   *            The properties should contain: lib.domain.workflow.url, ont.workflow.url, domain.workflows.dir.url
   *            (for file based): lib.domain.workflow.map, domain.workflows.dir.map
   *            (for tdb based): tdb.repository.dir
   */
  public static TemplateCreationAPI getCreationAPI(Properties props) {
    if (props == null) props = createLegacyConfiguration();
    return new TemplateCreationKB(props);
  }

  /**
   * Here, Properties are derived from wings configuration file and with some
   * default values
   */
  public static Properties createLegacyConfiguration() {
    // Create default urls
    Properties props = new Properties();
    props.put(
      "lib.domain.workflow.url",
      PropertiesHelper.getTemplateURL() + "/library.owl"
    );
    props.put("ont.workflow.url", PropertiesHelper.getWorkflowOntologyURL());
    props.put("domain.workflows.dir.url", PropertiesHelper.getTemplateURL());

    props.put(
      "lib.domain.execution.url",
      PropertiesHelper.getTemplateURL() + "/executions/library.owl"
    );
    props.put(
      "ont.execution.url",
      "http://www.wings-workflows.org/ontology/execution.owl"
    );
    props.put(
      "domain.executions.dir.url",
      PropertiesHelper.getTemplateURL() + "/executions"
    );

    props.put(
      "lib.domain.workflow.map",
      "file:" +
      PropertiesHelper.getTemplatesDir() +
      File.separator +
      ".." +
      File.separator +
      "library.owl"
    );
    props.put(
      "domain.workflows.dir.map",
      "file:" + PropertiesHelper.getTemplatesDir()
    );

    props.put(
      "lib.domain.execution.map",
      "file:" +
      PropertiesHelper.getTemplatesDir() +
      File.separator +
      ".." +
      File.separator +
      "executions" +
      File.separator +
      "library.owl"
    );
    props.put(
      "domain.executions.dir.map",
      "file:" +
      PropertiesHelper.getTemplatesDir() +
      File.separator +
      ".." +
      File.separator +
      "executions"
    );

    props.put(
      "ont.workflow.map",
      "file:" +
      PropertiesHelper.getOntologyDir() +
      File.separator +
      PropertiesHelper.getWorkflowOntologyPath()
    );

    createLibraryIfAbsent(props);
    return props;
  }

  private static void createLibraryIfAbsent(Properties props) {
    String libfile = props.getProperty("lib.domain.workflow.map");
    try {
      File libf = new File(new URI(libfile).getRawPath());
      if (libf.exists()) return;
      PrintWriter writer = new PrintWriter(libf);
      String liburl = props.getProperty("lib.domain.workflow.url");
      String onturl = props.getProperty("ont.workflow.url");
      String tplurl = props.getProperty("domain.workflows.dir.url");
      writer.println(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
        "\n" +
        "<rdf:RDF\n" +
        "   xml:base=\"" +
        liburl +
        "\"\n" +
        "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
        "   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n" +
        "   xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n" +
        "   xmlns=\"" +
        liburl +
        "#\"\n" +
        "   xmlns:wflow=\"" +
        onturl +
        "#\">\n" +
        ""
      );
      String wflowdir = props.getProperty("domain.workflows.dir.map");
      File dir = new File(new URI(wflowdir).getRawPath());
      File[] templates = dir.listFiles();
      for (File tpl : templates) {
        String tplfile = tpl.getName();
        if (tplfile.matches(".*\\.owl$")) {
          String url = tplurl + "/" + tplfile;
          String tplname = tplfile.replace(".owl", "");
          writer.println(
            "<wflow:WorkflowTemplate rdf:about=\"" +
            url +
            "#" +
            tplname +
            "\"/>"
          );
        }
      }
      writer.println("</rdf:RDF>");
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
