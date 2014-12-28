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

package edu.isi.wings.workflows.tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.isi.wings.workflows.util.AWGUtil;

public class CollectionTest {
	String domain = "drugome";

	String domdir;

	@Before
	public void setUp() {
		domdir = AWGUtil.initializeTest(domain);
	}

	@After
	public void tearDown() {
		AWGUtil.shutdown();
	}

	@Test
	public void testTemplateElaboration() throws FileNotFoundException, IOException {
		AWGUtil.testTemplateElaboration(domdir, "http://www.isi.edu/drugome/AbstractShortWorkflow.owl#AbstractShortWorkflow");
	}

	@Test
	public void testSeedGeneration() throws IOException {
		AWGUtil.testSeedGeneration(domdir, "http://www.isi.edu/drugome/seeds/DrugomeSeed.owl#DrugomeSeed", 
				new Integer[] { 1, 1, 1, 1, 1, 53 });
	}
}
