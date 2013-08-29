package edu.isi.wings.catalog.component.api;

import edu.isi.wings.catalog.component.classes.ComponentInvocation;
import edu.isi.wings.catalog.component.classes.ComponentPacket;

import java.util.ArrayList;

/**
 * The interface for communicating with the Component Catalog during Workflow
 * Planning and generation
 */
public interface ComponentReasoningAPI {
	// Generation API
	ArrayList<ComponentPacket> specializeAndFindDataDetails(ComponentPacket details);

	ComponentPacket findDataDetails(ComponentPacket details);

	ArrayList<ComponentPacket> findOutputDataPredictedDescriptions(ComponentPacket details);

	ComponentInvocation getComponentInvocation(ComponentPacket details);
}
