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

package edu.isi.wings.portal.classes;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import edu.isi.wings.common.kb.KBUtils;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.api.impl.kb.TemplateKB;
import edu.isi.wings.workflow.template.classes.Link;
import edu.isi.wings.workflow.template.classes.Node;
import edu.isi.wings.workflow.template.classes.Port;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.SetExpression;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.sets.WingsSet;
import edu.isi.wings.workflow.template.classes.sets.SetExpression.SetOperator;
import edu.isi.wings.workflow.template.classes.variables.Variable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JsonHandler {
	public static Gson createGson() {
		return new GsonBuilder().disableHtmlEscaping().create();
	}
	
	public static Gson createPrettyGson() {
		return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	}

	public static Gson createRunGson() {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Date.class, new DateSerializer());
		gson.registerTypeAdapter(Binding.class, new BindingSerializer());		
		gson.registerTypeAdapter(ValueBinding.class, new BindingSerializer());		
		return gson.disableHtmlEscaping().setPrettyPrinting().create();
	}

  public static Gson createDataGson() {
    GsonBuilder gson = new GsonBuilder();
    gson.setDateFormat("yyyy-MM-dd");
    return gson.disableHtmlEscaping().setPrettyPrinting().create();
  }
  
  public static Gson createComponentJson() {
    GsonBuilder gson = new GsonBuilder();
    gson.setDateFormat("yyyy-MM-dd");
    return gson.disableHtmlEscaping().setPrettyPrinting().create();
  }
  
	public static Gson createTemplateGson() {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Link.class, new LinkSerializer());
//		gson.registerTypeAdapter(Node.class, new NodeSerializer());
		gson.registerTypeAdapter(Binding.class, new BindingSerializer());
		gson.registerTypeAdapter(Binding.class, new BindingDeserializer());
		gson.registerTypeAdapter(ValueBinding.class, new BindingSerializer());
		gson.registerTypeAdapter(ValueBinding.class, new BindingDeserializer());
		gson.registerTypeAdapter(SetExpression.class, new SetExpressionSerializer());
		gson.registerTypeAdapter(SetExpression.class, new SetExpressionDeserializer());
		gson.setDateFormat("yyyy-MM-dd");
		gson.disableHtmlEscaping();
		gson.setPrettyPrinting();
		return gson.create();
	}
	
	public static String getTemplateJSON(Gson json, Template tpl, HashMap<String, Object> extra) {
		ArrayList<String> varids = new ArrayList<String>();
		for(Variable v : tpl.getVariables()) varids.add(v.getID());
		HashMap<String, Object> items = new HashMap<String, Object>();
		items.put("template",  tpl);
		items.put("constraints",  tpl.getConstraintEngine().getConstraints(varids));
		if(extra != null)
			items.putAll(extra);
		return json.toJson(items);
	}
	
	public static Template getTemplateFromJSON(Gson json, String tpljson, String consjson) {
		// FIXME: Template class (TemplateKB) should be provided to the UI and then passed back from the UI
		Template tpl = json.fromJson(tpljson, TemplateKB.class);
		
		// Only node and variable ids were serialized in Links. Expand the nodes and variables now
		fillTemplateLinks(tpl);
		// Reset internal KB structures according to interface structures (links, nodes, etc)
		tpl.resetInternalRepresentation();
		// Add constraints to the constraint engine based on consjson
		addConstraints(tpl, consjson);
		
		return tpl;
	}
	
	private static void fillTemplateLinks(Template tpl) {
		for(Link l : tpl.getLinks()) {
			if(l.getOriginNode() != null) {
				l.setOriginNode(tpl.getNode(l.getOriginNode().getID()));
			}
			if(l.getDestinationNode() != null) {
				l.setDestinationNode(tpl.getNode(l.getDestinationNode().getID()));
			}
			l.setVariable(tpl.getVariable(l.getVariable().getID()));
		}
	}
	
	private static void addConstraints(Template tpl, String json) {
		JsonElement el = new JsonParser().parse(json);
		for(JsonElement tel : el.getAsJsonArray()) {
			JsonObject triple = tel.getAsJsonObject();
			String subj = triple.getAsJsonObject("subject").get("id").getAsString();
			String pred = triple.getAsJsonObject("predicate").get("id").getAsString();
			JsonObject objitem = triple.getAsJsonObject("object");
			if(objitem.get("value") != null && objitem.get("isLiteral").getAsBoolean()) {
				JsonPrimitive obj = objitem.get("value").getAsJsonPrimitive();
				String objtype = objitem.get("type") != null ? objitem.get("type").getAsString() : null;
				tpl.getConstraintEngine().createNewDataConstraint(subj, pred, 
						obj.isString() ? obj.getAsString() : obj.toString(), 
								objtype);
			}
			else {
				String obj = objitem.get("id").getAsString();
				tpl.getConstraintEngine().createNewConstraint(subj, pred, obj); 
			}
		}
	}
}


/**
 * Serializers and Deserializers
 */

/**
 * Link Serializer
 * -- Nodes and Variables references in Links are converted to String ids (to avoid repeating same information)
 */
class LinkSerializer implements JsonSerializer<Link>{
	public JsonElement serialize(Link link, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", link.getID());
		if(link.getOriginNode() != null) {
			JsonObject nodeobj = new JsonObject();
			nodeobj.addProperty("id", link.getOriginNode().getID());
			obj.add("fromNode", nodeobj);
		}
		if(link.getDestinationNode() != null) {
			JsonObject nodeobj = new JsonObject();
			nodeobj.addProperty("id", link.getDestinationNode().getID());
			obj.add("toNode", nodeobj);
		}
    if(link.getVariable() != null) {
      JsonObject varobj = new JsonObject();
      varobj.addProperty("id", link.getVariable().getID());
      obj.add("variable", varobj);      
    }		
		if(link.getOriginPort() != null)
			obj.add("fromPort", context.serialize(link.getOriginPort()));
		if(link.getDestinationPort() != null)
			obj.add("toPort", context.serialize(link.getDestinationPort()));
		return obj;
	}
}
/**
 * Node Serializer
 * -- inputPorts and outputPorts information isn't provided
 * -- the UI uses information from the links
 */
class NodeSerializer implements JsonSerializer<Node>{
	public JsonElement serialize(Node node, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("comment", node.getComment());
		obj.addProperty("id", node.getID());
		obj.add("componentVariable", context.serialize(node.getComponentVariable()));
		obj.add("prule", context.serialize(node.getPortSetRule()));
		obj.add("crule", context.serialize(node.getComponentSetRule()));
		return obj;
	}
}
/**
 * Binding Serializer
 * -- Bindings are array lists with extra information. Need to return that extra information
 */
class BindingSerializer implements JsonSerializer<Binding>{
	public JsonElement serialize(Binding binding, Type typeOfSrc, JsonSerializationContext context) {
		if(binding.isSet()) {
			JsonArray arr = new JsonArray();
			for(WingsSet s : binding)
				arr.add(context.serialize((Binding)s, typeOfSrc));
			return arr;
		}
		else {
			JsonObject obj = new JsonObject();
			if (binding.isURIBinding()) {
				obj.add("id", new JsonPrimitive(binding.getID()));
				obj.add("type", new JsonPrimitive("uri"));
			} else {
				ValueBinding vb = (ValueBinding) binding;
				String datatype = vb.getDatatype();
				if(datatype== null)
					datatype = KBUtils.XSD + "string";
				obj.add("value", new JsonPrimitive(vb.getValueAsString()));
				obj.add("datatype", new JsonPrimitive(datatype));
				obj.add("type", new JsonPrimitive("literal"));
			}
			return obj;
		}
	}
}
/**
 * Binding Deserializer
 */
class BindingDeserializer implements JsonDeserializer<Binding>{
	public Binding deserialize(JsonElement el, Type typeOfSrc, JsonDeserializationContext context) {
		if(el.isJsonArray()) {
			Binding b = new Binding();
			for(JsonElement cel : el.getAsJsonArray()) {
				b.add((Binding) context.deserialize(cel, Binding.class));
			}
			return b;
		}
		else {
			JsonObject obj = (JsonObject) el;
			if(obj.get("type") == null)
				return null;
			String type = obj.get("type").getAsString();
			if("uri".equals(type))
				return new Binding(obj.get("id").getAsString());
			else if("literal".equals(type))
				return new ValueBinding(obj.get("value").getAsString(), obj.get("datatype").getAsString());
		}
		return null;
	}
}
/**
 * SetExpression Serializer
 * -- SetExpressions are array lists with extra information. Need to return that extra information
 */
class SetExpressionSerializer implements JsonSerializer<SetExpression>{
	public JsonElement serialize(SetExpression expr, Type typeOfSrc, JsonSerializationContext context) {
		if(expr.isSet()) {
			JsonObject obj = new JsonObject();
			obj.add("op", context.serialize(expr.getOperator()));
			JsonArray arr = new JsonArray();
			for(SetExpression s : expr) {
				arr.add(context.serialize(s));
			}
			obj.add("args", arr);
			return obj;
		}
		else {
		  if(expr.getPort() != null)
		    return context.serialize(expr.getPort().getID());
		  else
		    return null;
		}
	}
}
/**
 * SetExpression Deserializer
 */
class SetExpressionDeserializer implements JsonDeserializer<SetExpression>{
	public SetExpression deserialize(JsonElement el, Type typeOfSrc, JsonDeserializationContext context) {
		if(el.isJsonObject()) {
			JsonObject obj = el.getAsJsonObject();
			SetOperator op = context.deserialize(obj.get("op"), SetOperator.class);
			SetExpression expr = new SetExpression(op);
			for(JsonElement arg : obj.getAsJsonArray("args")) {
				expr.add((SetExpression)context.deserialize(arg, SetExpression.class));
			}
			return expr;
		}
		else {
			String portid = el.getAsString();
			Port port = new Port(portid);
			return new SetExpression(SetOperator.XPRODUCT, port);
		}
	}
}
/**
 * Date Serializer
 * -- convert to timestamp (long) 
 */
class DateSerializer implements JsonSerializer<Date> {
  public JsonElement serialize(Date date, Type typeOfSrc,
      JsonSerializationContext context) {
    return context.serialize(date.getTime()/1000);
  }
}
