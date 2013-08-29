package edu.isi.wings.workflow.plan.classes;

public class ExecutionCode extends ExecutionFile {
	private static final long serialVersionUID = 1L;

	String codeDirectory;
	
	public ExecutionCode(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}
	public String getCodeDirectory() {
		return codeDirectory;
	}

	public void setCodeDirectory(String codeDirectory) {
		this.codeDirectory = codeDirectory;
	}
}
