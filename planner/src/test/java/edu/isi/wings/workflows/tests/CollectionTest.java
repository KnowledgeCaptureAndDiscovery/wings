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
