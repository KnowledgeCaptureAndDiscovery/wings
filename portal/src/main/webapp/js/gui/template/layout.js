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

var Layout = function(template) {
	this.template = template;
};

Layout.prototype.layoutDot = function(msgTarget, ed, op_url) {
	this.template = ed.template;
	ed.template.saveToStore(true);
	var store = ed.template.store;

	var url = op_url + "/layoutTemplate";
	Ext.Ajax.request({
		url : url,
		params : {
			json : Ext.encode(store)
		},
		success : function(response) {
			msgTarget.unmask();
			// Copy over xy positions from new store
			var nstore = Ext.decode(response.responseText);
			for(var i=0; i<nstore.template.Nodes.length; i++) {
				var node = nstore.template.Nodes[i];
				var n = ed.template.nodes[node.id];
				if(n) {
					var xy = ed.template.getXYFromComment(node.comment);
					n.x = parseInt(xy.x) + 0.5;
					n.y = parseInt(xy.y) + 0.5;
					if(xy.center)
						n.centercoords = true;
				}
			}
			for(var i=0; i<nstore.template.Variables.length; i++) {
				var vr = nstore.template.Variables[i];
				var v = ed.template.variables[vr.id];
				if(v) {
					var xy = ed.template.getXYFromComment(vr.comment);
					v.x = parseInt(xy.x) + 0.5;
					v.y = parseInt(xy.y) + 0.5;
					if(xy.center)
						v.centercoords = true;
				}
			}
			// Redraw canvas
			ed.redrawCanvas(ed.panelWidth, ed.panelHeight);
		},
		failure : function(response) {
			if (window.console)
				window.console.log(response.responseText);
		}
	});
};
