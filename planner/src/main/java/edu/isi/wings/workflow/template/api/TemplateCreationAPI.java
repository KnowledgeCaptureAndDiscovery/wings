package edu.isi.wings.workflow.template.api;

import java.util.ArrayList;

import edu.isi.wings.workflow.template.classes.ConstraintProperty;

public interface TemplateCreationAPI {
	// Query
	ArrayList<String> getTemplateList();
	
	Template getTemplate(String tplid);
	
	ArrayList<ConstraintProperty> getAllConstraintProperties();
	
	// Creation
	
	Template createTemplate(String tplid);
	
	boolean saveTemplate(Template tpl);
	
	boolean saveTemplateAs(Template tpl, String newid);
	
	boolean removeTemplate(Template tpl);

	void end();
	
	void delete();
	
	// Copy from another API (Advisable to give the same implementation of the API here)
	void copyFrom(TemplateCreationAPI tc);
	
}
