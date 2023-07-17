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

package edu.isi.wings.workflows.util;

import edu.isi.wings.common.kb.PropertiesHelper;
import edu.isi.wings.planner.cli.Wings;
import edu.isi.wings.workflow.plan.api.ExecutionPlan;
import edu.isi.wings.workflow.template.api.Template;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.apache.jena.util.FileUtils;
import org.junit.Assert;

public class AWGUtil {

  private static String DOMAINS_PATH = "/domains/";

  public static String initializeTest(String dom) {
    String domdir = AWGUtil.class.getResource(DOMAINS_PATH + dom).getPath();
    initializeWithDirectory(domdir);
    return domdir;
  }

  public static void initializeWithDirectory(String domdir) {
    String conf_path = domdir + "/wings.properties";
    PropertiesHelper.resetProperties();
    PropertiesHelper.loadWingsProperties(conf_path);
    PropertiesHelper.setOntologyDir(domdir + "/ontology");
    PropertiesHelper.setLogDir(domdir + "/logs");
    PropertiesHelper.setOutputDir(domdir + "/output");
  }

  public static void shutdown() {
    PropertiesHelper.resetProperties();
  }

  public static void testTemplateElaboration(String domdir, String template)
    throws FileNotFoundException, IOException {
    String conf_path = domdir + "/wings.properties";
    String requestid = UUID.randomUUID().toString();
    Wings wings = new Wings(template, requestid, conf_path, true);

    wings.initializePC();
    wings.initializeRC();
    wings.initializeWorkflowGenerator();
    wings.initializeDC();
    wings.setDC(wings.dc, wings.dcc);
    wings.initializeItem();

    Template it = wings.getWG().getInferredTemplate(wings.getTemplate());
    Assert.assertNotNull(it);

    String[] arr1 = FileUtils
      .readWholeFileAsUTF8(domdir + "/test1.output")
      .split("\\s*[\\[,\\]]\\s*");
    String[] arr2 = it
      .getConstraintEngine()
      .getConstraints()
      .toString()
      .split("\\s*[\\[,\\]]\\s*");
    Assert.assertEquals(arr1.length, arr2.length);

    java.util.Arrays.sort(arr1);
    java.util.Arrays.sort(arr2);
    Assert.assertArrayEquals(arr1, arr2);
  }

  public static void testSeedGeneration(
    String domdir,
    String seed,
    Integer[] num
  ) throws IOException {
    String conf_path = domdir + "/wings.properties";
    String requestid = UUID.randomUUID().toString();
    Wings wings = new Wings(seed, requestid, conf_path);

    wings.initializePC();
    wings.initializeRC();
    wings.initializeWorkflowGenerator();
    wings.initializeDC();
    wings.setDC(wings.dc, wings.dcc);
    wings.initializeItem();

    ArrayList<Template> candidates = wings.backwardSweep(wings.getSeed());
    Assert.assertEquals(num[0].intValue(), candidates.size());

    ArrayList<Template> bindings = wings.selectInputData(candidates);
    Assert.assertEquals(num[1].intValue(), bindings.size());

    wings.getDataMetricsForInputData(bindings);

    ArrayList<Template> configurations = wings.forwardSweep(bindings);
    Assert.assertEquals(num[2].intValue(), configurations.size());

    ArrayList<Template> expansions = wings.getExpandedTemplates(configurations);
    Assert.assertEquals(num[3].intValue(), expansions.size());

    ArrayList<ExecutionPlan> plans = wings.getExecutionPlans(expansions);
    Assert.assertEquals(num[4].intValue(), plans.size());

    int numjobs = plans.get(0).getAllExecutionSteps().size();
    Assert.assertEquals(num[5].intValue(), numjobs);
    //wings.runPlan(plans.get(0), true);
  }
}
