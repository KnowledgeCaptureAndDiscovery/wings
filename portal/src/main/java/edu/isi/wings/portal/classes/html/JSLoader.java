package edu.isi.wings.portal.classes.html;

import java.io.PrintWriter;
import java.util.HashMap;

import edu.isi.wings.portal.classes.Config;

public class JSLoader {
	static String[] common_scripts = { "lib/extjs/ext-all.js", "js/util/common.js",
			"js/gui/WingsMessages.js" };
	static String[] component_scripts = { "js/gui/ComponentViewer.js" };
	static String[] domain_scripts = { "js/gui/DomainViewer.js" };
	static String[] run_scripts = { "js/gui/RunBrowser.js" };
	static String[] data_scripts = { "js/gui/DataViewer.js" };
	static String[] template_scripts = { "js/gui/SeedForm.js", "js/gui/template/Canvas.js",
			"js/gui/template/port.js", "js/gui/template/shape.js", "js/gui/template/link.js",
			"js/gui/template/node.js", "js/gui/template/variable.js", "js/gui/template/template.js",
			"js/gui/template/layout.js", "js/gui/template/reasoner.js",
			"js/gui/TemplateBrowser.js", "js/gui/TemplateGraph.js", };
	static String[] rule_scripts = { "js/gui/jenarules/RulesParser.js",
			"js/gui/jenarules/RuleFunctionType.js", "gui/jenarules/RuleFunction.js",
			"js/gui/jenarules/Rule.js", "js/gui/jenarules/Constants.js", "gui/jenarules/Triple.js",
			"js/gui/jesnarules/LocalClasses.js" };
	static String[] tellme_scripts = { "js/gui/tellme/tellme.js",
			"js/gui/tellme/tellme_history.js", "js/beamer/ControlList.js", "js/beamer/MatchedTask.js",
			"js/beamer/Paraphrase.js", "js/beamer/Task.js", "js/beamer/TodoItemMatches.js",
			"js/beamer/TodoListParser.js", "js/beamer/Token.js", "js/beamer/Trie.js" };

	static String[] plupload_scripts = { "js/util/pluploadPanel.js", "lib/plupload/plupload.full.js" };

	public static void setContextInformation(PrintWriter out, Config config) {
		HashMap<String, Object> jsvars = new HashMap<String, Object>();
		jsvars.put("CONTEXT_ROOT", "'" + config.getContextRootPath() + "'");
		jsvars.put("USER_ID", "'" + config.getUserId() + "'");
		JSLoader.showScriptKeyVals(out, jsvars);
	}

	public static void loadLoginViewer(PrintWriter out, String path) {
		showScriptTags(out, path, common_scripts);		
	}
		
	public static void loadDataViewer(PrintWriter out, String path) {
		showScriptTags(out, path, common_scripts);
		showScriptTags(out, path, data_scripts);
		showScriptTags(out, path, plupload_scripts);
	}

	public static void loadComponentViewer(PrintWriter out, String path) {
		showScriptTags(out, path, common_scripts);
		showScriptTags(out, path, component_scripts);
		// showScriptTags(out, path, rule_scripts);
		showScriptTags(out, path, plupload_scripts);
	}

	public static void loadDomainViewer(PrintWriter out, String path) {
		showScriptTags(out, path, common_scripts);
		showScriptTags(out, path, domain_scripts);
		showScriptTags(out, path, plupload_scripts);
	}

	public static void loadRunViewer(PrintWriter out, String path) {
		showScriptTags(out, path, common_scripts);
		showScriptTags(out, path, run_scripts);
		showScriptTags(out, path, template_scripts);
	}

	public static void loadTemplateViewer(PrintWriter out, String path, boolean loadtellme) {
		showScriptTags(out, path, common_scripts);
		showScriptTags(out, path, template_scripts);
		showScriptTags(out, path, component_scripts);
		if (loadtellme)
			showScriptTags(out, path, tellme_scripts);
	}

	private static void showScriptTags(PrintWriter out, String path, String[] scripts) {
		for (String script : scripts) {
			out.println("<script src=\"" + path + "/" + script + "\"></script>");
		}
	}

	public static void showScriptKeyVals(PrintWriter out, HashMap<String, Object> map) {
		out.println("<script>");
		for (String key : map.keySet()) {
			Object val = map.get(key);
			out.println("var " + key + " = " + val + ";");
		}
		out.println("</script>");
	}
}
