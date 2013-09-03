package edu.isi.wings.catalog.component.api;

import edu.isi.wings.catalog.component.classes.Component;
import edu.isi.wings.catalog.component.classes.ComponentTree;

public interface ComponentCreationAPI {
	// Query
	ComponentTree getComponentHierarchy(boolean details);

	Component getComponent(String cid, boolean details);

	String getComponentLocation(String cid);

	String getComponentHolderId(String cid);
	
	// Update
	boolean addComponent(Component comp, String pholderid);
	
	boolean addComponentHolder(String holderid, String pholderid);

	boolean updateComponent(Component newcomp);
	
	boolean renameComponent(String oldid, String newid);

	boolean removeComponent(String cid, boolean remove_holder, boolean unlink);
	
	boolean removeComponentHolder(String holderid);
	
	boolean setComponentLocation(String cid, String location);
	
	// Saving
	boolean save();

	void end();
	
	void delete();
	
	// Copy from another API (Advisable to give the same implementation of the API here)
	void copyFrom(ComponentCreationAPI dc);
}
