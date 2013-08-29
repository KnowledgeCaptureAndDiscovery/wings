package edu.isi.wings.workflow.template.api;

import java.util.ArrayList;

import edu.isi.wings.workflow.template.classes.ConstraintProperty;

public interface TemplateCreationAPI {
	// Query
	public ArrayList<String> getTemplateList();
	
	public Template getTemplate(String tplid);
	
	public ArrayList<ConstraintProperty> getAllConstraintProperties();
	
	// Creation
	
	public Template createTemplate(String tplid);
	
	public boolean saveTemplate(Template tpl);
	
	public boolean saveTemplateAs(Template tpl, String newid);
	
	public boolean removeTemplate(Template tpl);

	public void end();
	
	// Copy from another API (Advisable to give the same implementation of the API here)
	void copyFrom(TemplateCreationAPI tc);
	
}
