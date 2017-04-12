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

/*
 * Layout Class
 */

var GraphLayout = function() {
	this.graph = null;
};
var layoutWorker = undefined;

GraphLayout.prototype.layoutDot = function(msgTarget, graph, animate, domnode, op_url) {
	this.graph = graph;
	var data = this.graph.getData();

	var me = this;
	
	var url = op_url + "/layoutTemplate";
	// Gzipped Request
	Ext.Ajax.requestGZ({
		url : url,
		rawData : Ext.encode(data),
		success : function(response) {
			msgTarget.unmask();
			// Copy over xy positions from new store
			var nstore = Ext.decode(response.responseText);
			for(var i in nstore.template.Nodes) {
				var node = nstore.template.Nodes[i];
				var n = me.graph.nodes[node.id];
				if(n) {
					var pos = me.graph.getItemCoordinates(node);
					n.setCoords(pos, animate);
				}
			}
			for(var i in nstore.template.Variables) {
				var vr = nstore.template.Variables[i];
				var v = me.graph.variables[vr.id];
				if(v) {
					var pos = me.graph.getItemCoordinates(vr);
					v.setCoords(pos, animate);
				}
			}
	    	if(domnode)
	    		me.graph.draw(domnode);
	    	else {
	        	// Redraw links
	        	me.graph.drawLinks(animate);
	    		me.graph.resizeViewport(false, true);
	    	}			
		},
		failure : function(response) {
			if (window.console)
				window.console.log(response);
		}
	});
};

GraphLayout.prototype.layoutVizDot = function(msgTarget, graph, animate, domnode) {
	this.graph = graph;
	var DPI = 72;
    var MAX_LINKS = 500;
    
    var numlinks = 0;
    for(var i in this.graph.links)
    	numlinks++;
    
    if(numlinks > 500)
    	return;
    //TODO: Instead do a simpler layout (no ports, no text)
    
    var nl = "\n";
    var tab = "\t";
    var dotstr = "digraph test {";
    dotstr += nl + tab + "node [shape=record];";
    dotstr += nl + tab + "nodesep = 0.1;";
    dotstr += nl + tab + "ranksep = 0.6;";
    
    var idmap = {};
    
    for (var nid in this.graph.nodes) {
    	var n = this.graph.nodes[nid];
    	var text = this.cleanText(n.getText(), n.getDimensionality());
    	var id = this.cleanID(n.getName());
    	/*var machine_text = n.getMachinesText();
    	if(machine_text)
    		text += "\n" + machine_text;*/
    	
    	var ips = n.getInputPorts();
    	var ops = n.getOutputPorts();

    	var fsize = n.config.getFontsize();
    	var fname = n.config.getFont() + " " + n.config.getFontweight();
    	
    	dotstr += nl + tab + id + "[label=\"{{";
    	for ( var i = 0; i < ips.length; i++)
    		dotstr += "|<" + this.cleanID(ips[i].getName()) + ">";
    	dotstr += "|}|{" + text + "}|{";
    	for ( var i = 0; i < ops.length; i++)
    		dotstr += "|<" + this.cleanID(ops[i].getName()) + ">";
    	dotstr += "|}}\", fontname=\"" + fname + "\" fontsize=\"" + fsize + "\"];";
    	idmap[id] = n;
    }

    for (var vid in this.graph.variables) {
    	var v = this.graph.variables[vid];
    	var text = this.cleanText(v.getText(), v.getDimensionality());
    	var id = this.cleanID(v.getName());

    	var fsize = v.config.getFontsize();
    	var fname = v.config.getFont() + " " + v.config.getFontweight();
    	
    	var fsize = 13;       
    	dotstr += nl + tab + id + 
    		"[label=\"{{|<ip>|}|{" + text + "}|{|<op>|}}\", " +
    				"fontname=\"" + fname + "\", fontsize=\"" + fsize + "\"];";

    	idmap[id] = v;
    }

    var donelinks = {};
    for (var lid in this.graph.links) {
    	var l = this.graph.links[lid];
    	if (l.fromPort) {
    		var linkid = this.cleanID(l.fromNode.getName()) + ":" 
              	+ this.cleanID(l.fromPort.getName()) + " -> "
              	+ this.cleanID(l.variable.getName()) + ":ip;";
    		if(!donelinks[linkid]) {
    			dotstr += nl + tab + linkid;
    			donelinks[linkid] = true;
    		}
    	}
    	if (l.toPort) {
    		var linkid = this.cleanID(l.variable.getName()) + ":op -> "
            	+ this.cleanID(l.toNode.getName()) + ":" 
            	+ this.cleanID(l.toPort.getName()) + ";";
    		if(!donelinks[linkid]) {
    			dotstr += nl + tab + linkid;
    			donelinks[linkid] = true;
    		}
    	}
    }
    dotstr += nl + "}";
    //console.log(dotstr);
    
    if(layoutWorker == undefined) {
    	layoutWorker = new Worker(CONTEXT_ROOT + "/js/workers/layout.js");
    	//console.log("creating new worker");
    }
    
    var me = this;
    layoutWorker.postMessage(dotstr);
    layoutWorker.onmessage = function(e) {
    	msgTarget.unmask();
    	var layout = e.data;
    	//console.log(layout);
    	var lines = layout.split(/\n/);
    	
    	var graph = lines[0].split(/\s+/);
    	var gw = parseFloat(graph[2]);
    	var gh = parseFloat(graph[3]);
    	var curline = "";
    	for ( var i = 1; i < lines.length; i++) {
    		var line = lines[i];
    		if(line.match(/\\$/)) {
    			curline += line.substring(0, line.length-1);
    			continue;
    		}
    		if(curline) {
    			line = curline + line;
    			curline = "";
    		}
    		var tmp = line.split(/\s+/);
    		if (tmp.length < 4)
    			continue;
    		if(tmp[0] != "node")
    			continue;
    		var id = tmp[1];
    		if(idmap[id]) {
    			var item = idmap[id];
    			var w = parseFloat(tmp[4]);
    			var h = parseFloat(tmp[5]);
        		var x = parseFloat(tmp[2])*1.1;
        		var y = (gh - h/2 - parseFloat(tmp[3]))/1.5;
    			//console.log("x="+x+", y="+y+", w="+w+", h="+h);
    			var itemx = 10 + DPI*x; // - (item.bounds.width - item.textbounds.width)/2;
    			var itemy = 30 + DPI*y;
        		//console.log(line);
        		//console.log(itemx + "," + itemy);
    			item.setCoords({x: itemx, y: itemy}, animate);
    		}
    	}    	
    	if(domnode)
    		me.graph.draw(domnode);
    	else {
        	// Redraw links
        	me.graph.drawLinks(animate);
    		me.graph.resizeViewport(false, true);
    	}
    }
};

GraphLayout.prototype.cleanID = function(id) {
    id = id.replace(/^.*#/g, "");
    id = id.replace(/[^a-zA-Z0-9_]/g, "_");
    if (id.match(/^([0-9]|\.|\-)/))
      id = '_' + id;
    return id;
};

GraphLayout.prototype.cleanText = function(text, dim) {
	var suffix = ""
	for(var i=0; i<dim; i++)
		suffix += "D";
	
	text = text.replace(/\n/g, suffix + '\\n');
	text = text + suffix;
	text = text.replace(/([{}])/g, "1");
	return text;
}
