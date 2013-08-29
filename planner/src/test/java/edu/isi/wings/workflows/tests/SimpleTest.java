package edu.isi.wings.workflows.tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.isi.wings.workflows.util.AWGUtil;

public class SimpleTest {
	String domain = "DMDomain";

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
		AWGUtil.testTemplateElaboration(domdir, "http://www.isi.edu/DMDomain/ModelAndClassify.owl#ModelAndClassify");
	}

	@Test
	public void testSeedGeneration() throws IOException {
		AWGUtil.testSeedGeneration(domdir, "http://www.isi.edu/DMDomain/seeds/Test2Seed.owl#Test2Seed", 
				new Integer[] { 8, 2, 1, 1, 1, 2 });
	}
}
