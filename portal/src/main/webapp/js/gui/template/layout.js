/*
 * Layout Class
 */

var Layout = function(template) {
	this.template = template;
	this.iterations = 500;
	this.maxRepulsiveForceDistance = 4;
	this.k = 2;
	this.c = 0.01;
	this.maxVertexMovement = 0.5;
	this.radius = 80;
};

Layout.prototype.cleanID = function(id) {
	id = getLocalName(id);
	id = id.replace(/[^a-zA-Z0-9_]/g, '_');
	if (id.match(/^([0-9]|\.|\-)/))
		id = '_' + id;
	return id;
	// id = id.replace(/[^a-zA-Z0-9_]/g, '_');
	// return id.replace(/(^[0-9])/g, '_$1');
};

Layout.prototype.layoutDot = function(msgTarget, callbackfn, scope, op_url, template) {
	if (template)
		this.template = template;
	var nl = "\n";
	var tab = "\t";
	var dotstr = "digraph test {";
	dotstr += nl + tab + "node [shape=record];";
	dotstr += nl + tab + "nodesep = 0.1;";
	dotstr += nl + tab + "ranksep = 0.5;";
	var idmap = {};
	for ( var i in this.template.nodes) {
		var n = this.template.nodes[i];
		var nid = this.cleanID(n.id);
		var ntext = n.text.split(",").join("\\n");
		var ips = n.getInputPorts();
		var ops = n.getOutputPorts();
		var fsize = parseInt(n.font.replace(/^bold /,'')) + 4;
		
		ips.sort(function(a, b){return a.id.localeCompare(b.id);});
		ops.sort(function(a, b){return a.id.localeCompare(b.id);});
		
		dotstr += nl + tab + nid + "[label=\"{{";
		for ( var i = 0; i < ips.length; i++)
			dotstr += "|<" + this.cleanID(ips[i].id) + ">";
		dotstr += "|}|{" + ntext + "}|{";
		for ( var i = 0; i < ops.length; i++)
			dotstr += "|<" + this.cleanID(ops[i].id) + ">";
		dotstr += "|}}\", fontsize=\"" + fsize + "\"];";
		idmap[nid] = n;
	}
	for ( var i in this.template.variables) {
		var v = this.template.variables[i];
		var fsize = parseInt(v.font.replace(/^bold /,'')) + 4;
		var vid = this.cleanID(v.id);
		var vtext = v.text.split(",").join("\\n");
		dotstr += nl + tab + vid + "[label=\"{{|<ip>|}|{" + vtext + "}|{|<op>|}}\", fontsize=\"" + fsize + "\"];";
		idmap[vid] = v;
	}
	var donelinks = {};
	for ( var i in this.template.links) {
		var l = this.template.links[i];
		if (l.fromPort) {
			var lid = this.cleanID(l.fromPort.partOf.id) + ":" + this.cleanID(l.fromPort.id) + " -> "
						+ this.cleanID(l.variable.id) + ":ip;";
			if(!donelinks[lid]) {
				dotstr += nl + tab + lid;
				donelinks[lid] = true;
			}
		}
		if (l.toPort) {
			var lid = this.cleanID(l.variable.id) + ":op -> " + this.cleanID(l.toPort.partOf.id) + ":"
						+ this.cleanID(l.toPort.id) + ";";
			if(!donelinks[lid]) {
				dotstr += nl + tab + lid;
				donelinks[lid] = true;
			}
		}
	}
	dotstr += nl + "}";

	var url = op_url + "/dotLayout";
	// if(window.console) window.console.log(dotstr);
	Ext.Ajax.request({
		url : url,
		params : {
			dotstr : dotstr
		},
		success : function(response) {
			msgTarget.unmask();
			// if(window.console) window.console.log("Returned:
			// "+response.responseText);
			var lines = response.responseText.split(/\n/);
			for ( var i = 0; i < lines.length; i++) {
				var tmp = lines[i].split(/:/);
				if (tmp.length != 2)
					continue;
				var id = tmp[0];
				var pos = tmp[1].split(/,/);
				var obj = idmap[id];
				if (obj && pos[0] && pos[1]) {
					obj.x = 20 + parseInt(pos[0]) * 1.2 - obj.width / 2;
					obj.y = 20 + parseInt(pos[1]) / 1.5 - obj.height / 2;
					// console.log(obj.id+"="+obj.x+","+obj.y);
				}
			}
			callbackfn.call(scope, scope.panelWidth, scope.panelHeight);
		},
		failure : function(response) {
			if (window.console)
				window.console.log(response.responseText);
		}
	});
};

Layout.prototype.layout = function() {
	// this.setComponentIOPositions();
	this.clearPositions();
	this.setComponentPositions();
};

Layout.prototype.clearPositions = function() {
	for ( var i in this.template.nodes) {
		var n = this.template.nodes[i];
		n.x = 0;
		n.y = 0;
		n._x = 0;
		n._y = 0;
		n._w = 0;
		n._h = 0;
	}
	for ( var i in this.template.variables) {
		var v = this.template.variables[i];
		v.x = 0;
		v.y = 0;
	}
};

Layout.prototype.shiftNodePosition = function(n, xshift, yshift) {
	n._x += xshift;
	n._y += yshift;
	n.x += xshift;
	n.y += yshift;
	var ilinks = n.getInputLinks();
	var olinks = n.getOutputLinks();
	for ( var i = 0; i < ilinks.length; i++) {
		ilinks[i].variable.x += xshift;
		ilinks[i].variable.y += yshift;
	}
	for ( var i = 0; i < olinks.length; i++) {
		olinks[i].variable.x += xshift;
		olinks[i].variable.y += yshift;
	}
};

Layout.prototype.setNodeIOPositions = function(n, template) {
	var ilinks = n.getInputLinks();
	var olinks = n.getOutputLinks();

	n._x = 0;
	n._y = 0;

	var Xpadding = 20;
	var Ydistance = 60;

	var Width = Xpadding;
	var Height = 0;

	// Get Node's Width and Height
	var iX = Xpadding;
	var iY = 0;
	for ( var i = 0; i < ilinks.length; i++) {
		iX += ilinks[i].variable.width + Xpadding;
		Height = iY + ilinks[i].variable.height;
	}
	if (Width < iX)
		Width = iX;

	var nX = n.width + 2 * Xpadding;
	var nY = Height + Ydistance;
	if (Width < nX)
		Width = nX;
	Height = nY + n.height;

	var oX = Xpadding;
	var oY = Height + Ydistance;
	for ( var i = 0; i < olinks.length; i++) {
		oX += olinks[i].variable.width + Xpadding;
		Height = olinks[i].variable.height + oY;
	}
	if (Width < oX)
		Width = oX;
	// Height += Ypadding;

	n._w = Width;
	n._h = Height;

	// Sort the Node's IO Roles
	var iRoleVar = {};
	var iRoles = [];
	for ( var i = 0; i < ilinks.length; i++) {
		iRoleVar[ilinks[i].toPort.name] = ilinks[i].variable;
		iRoles.push(ilinks[i].toPort.name);
	}
	iRoles.sort();

	var oRoleVar = {};
	var oRoles = [];
	for ( var i = 0; i < olinks.length; i++) {
		oRoleVar[olinks[i].fromPort.name] = olinks[i].variable;
		oRoles.push(olinks[i].fromPort.name);
	}
	oRoles.sort();

	// Get Node's initial X and Y from Input Variables
	var curX = (Width - iX) / 2;
	for ( var i = 0; i < iRoles.length; i++) {
		var v = iRoleVar[iRoles[i]];
		if (v.x) {
			if (v.x > curX)
				n._x = v.x - curX;
			else if (v.x < curX) {
				for ( var j = 0; j < v.provenance.length; j++) {
					this.shiftNodePosition(v.provenance[j], curX - v.x, 0);
				}
			}
		}
		if (v.y && n._y < v.y) {
			n._y = v.y;
		}
		curX += v.width + Xpadding;
	}

	// Shift Node's X if it is clashing with another
	for ( var i in template.nodes) {
		var xn = template.nodes[i];
		if (xn != n && xn._w) {
			if ((n._x >= xn._x) && (n.x < xn._x + xn._w))
				n._x = xn._x + xn._w;
		}
	}

	iY += n._y;
	nY += n._y;
	oY += n._y;

	var curX = n._x + (Width - iX) / 2;
	for ( var i = 0; i < iRoles.length; i++) {
		var v = iRoleVar[iRoles[i]];
		v.x = curX;
		v.y = iY;
		curX += v.width + Xpadding;
	}
	n.x = n._x + (Width - nX) / 2;
	n.y = nY;

	curX = n._x + (Width - oX) / 2;
	for ( var i = 0; i < oRoles.length; i++) {
		var v = oRoleVar[oRoles[i]];
		v.x = curX;
		v.y = oY;
		curX += v.width + Xpadding;
	}

};

Layout.prototype.setComponentIOPositions = function() {
	for ( var i in this.template.nodes) {
		var n = this.template.nodes[i];
		this.setNodeIOPositions(n, this.template);
	}
};

Layout.prototype.setComponentPositions = function() {
	var traversedLinks = {};

	// Seed the links queue with input links
	var linksQ = [];
	for ( var i in this.template.links) {
		var l = this.template.links[i];
		if (!l.fromPort)
			linksQ.push(l);
	}

	// Process all links in the linksQ
	while (linksQ.length) {
		var l = linksQ.pop();

		if (traversedLinks[l.id])
			continue;
		traversedLinks[l.id] = 1;

		// Get link's "from node", variable and "to node"
		var fn = l.fromNode;
		var v = l.variable;
		var tn = l.toNode;

		// Set Rankings
		/*var fnr = fn ? fn.rankH : 0;
		if(!v.rankH || v.rankH > fnr + 1) v.rankH = fnr + 1;
		if(tn) {
			if(!tn.rankH || tn.rankH < fnr + 2) tn.rankH = fnr + 2;
			// Add the toNode's output links if all input links have been traversed
			var inlinks = tn.getInputLinks();
			var outlinks = tn.getOutputLinks();
			var allInputsTraversed = true;
			for(var i=0; i<inlinks.length; i++) {
				//inlinks[i].variable.rankH = tn.rankH - 1;
				if(!traversedLinks[inlinks[i].id]) allInputsTraversed = false;
			}
			if(allInputsTraversed) {
				for(var i=0; i<outlinks.length; i++) {
					linksQ.push(outlinks[i]);
				}
			}
		}*/

		if (!v.provenance)
			v.provenance = [];
		if (fn) {
			v.provenance.push(fn);
		}

		// Set Node IO Positions
		if (tn) {
			var inlinks = tn.getInputLinks();
			var outlinks = tn.getOutputLinks();
			var allInputsTraversed = true;
			for ( var i = 0; i < inlinks.length; i++) {
				if (!traversedLinks[inlinks[i].id])
					allInputsTraversed = false;
			}
			if (allInputsTraversed) {
				this.setNodeIOPositions(tn, this.template);
				for ( var i = 0; i < outlinks.length; i++) {
					linksQ.push(outlinks[i]);
				}
			}
		}
	}

	// For orphan nodes and variables (i.e. without any links, rank them at 1)
	/*for(var i in this.template.nodes) {
		var n = this.template.nodes[i];
		if(!n.rankH) n.rankH = 1;
		n.y = n.rankH * n.height + (n.rankH-1)*(n.height + 20);
		//console.log(n.id+":"+n.rankH);
	}
	for(var i in this.template.variables) {
		var v = this.template.variables[i];
		if(!v.rankH) v.rankH = 1;
		v.y = v.rankH * v.height + (v.rankH-1)*(v.height + 20);
		//console.log(v.id+":"+v.rankH);
	}*/
};

Layout.prototype.rankWidth = function() {
};

/*Layout.prototype.layout = function(w,h) {
	this.width = w;
	this.height = h;
	this.templateToGraph();
   this.layoutPrepare();
   for (var i = 0; i < this.iterations; i++) {
       this.layoutIteration();
   }
   this.layoutCalcBounds();
	this.graphToTemplate();
};*/

Layout.prototype.translate = function(n) {
	var factorX = (this.width - n.width) / (this.graph.layoutMaxX - this.graph.layoutMinX);
	var factorY = (this.height - n.height) / (this.graph.layoutMaxY - this.graph.layoutMinY);
	return [
			parseInt((n.layoutPosX - this.graph.layoutMinX) * factorX + n.width),
			parseInt((n.layoutPosY - this.graph.layoutMinY) * factorY + n.height)
	];
};

Layout.prototype.templateToGraph = function() {
	// Convert template to graph
	this.graph = {};
	this.graph.nodes = [];
	this.graph.edges = [];
	for ( var i in this.template.nodes) {
		var n = this.template.nodes[i];
		// n.layoutPosX = n.x; n.layoutPosY = n.y;
		this.graph.nodes.push(n);
	}
	for ( var i in this.template.variables) {
		var n = this.template.variables[i];
		// n.layoutPosX = n.x; n.layoutPosY = n.y;
		this.graph.nodes.push(n);
	}
	for ( var i in this.template.links) {
		var l = this.template.links[i];
		if (l.fromNode)
			this.graph.edges.push({
				source : l.fromNode,
				target : l.variable
			});
		if (l.toNode)
			this.graph.edges.push({
				source : l.variable,
				target : l.toNode
			});
	}
};

Layout.prototype.graphToTemplate = function() {
	// Assign node x,y values
	for ( var i = 0; i < this.graph.nodes.length; i++) {
		var n = this.graph.nodes[i];
		var pos = this.translate(n);
		n.x = pos[0];
		n.y = pos[1];
	}
};

Layout.prototype.layoutPrepare = function() {
	for ( var i = 0; i < this.graph.nodes.length; i++) {
		var node = this.graph.nodes[i];
		node.layoutPosX = 0;
		node.layoutPosY = node.rankH;
		node.layoutForceX = 0;
		node.layoutForceY = 0;
	}
};

Layout.prototype.layoutCalcBounds = function() {
	var minx = Infinity, maxx = -Infinity, miny = Infinity, maxy = -Infinity;
	for ( var i = 0; i < this.graph.nodes.length; i++) {
		var x = this.graph.nodes[i].layoutPosX;
		var y = this.graph.nodes[i].layoutPosY;
		if (x > maxx)
			maxx = x;
		if (x < minx)
			minx = x;
		if (y > maxy)
			maxy = y;
		if (y < miny)
			miny = y;
	}
	this.graph.layoutMinX = minx;
	this.graph.layoutMaxX = maxx;
	this.graph.layoutMinY = miny;
	this.graph.layoutMaxY = maxy;
};

Layout.prototype.layoutIteration = function() {
	// Forces on nodes due to node-node repulsions
	for ( var i = 0; i < this.graph.nodes.length; i++) {
		var node1 = this.graph.nodes[i];
		for ( var j = i + 1; j < this.graph.nodes.length; j++) {
			var node2 = this.graph.nodes[j];
			this.layoutRepulsive(node1, node2);
		}
	}
	// Forces on nodes due to edge attractions
	for ( var i = 0; i < this.graph.edges.length; i++) {
		var edge = this.graph.edges[i];
		this.layoutAttractive(edge);
	}

	// Move by the given force
	for ( var i = 0; i < this.graph.nodes.length; i++) {
		var node = this.graph.nodes[i];
		var xmove = this.c * node.layoutForceX;
		var ymove = this.c * node.layoutForceY;

		var max = this.maxVertexMovement;
		if (xmove > max)
			xmove = max;
		if (xmove < -max)
			xmove = -max;
		if (ymove > max)
			ymove = max;
		if (ymove < -max)
			ymove = -max;

		node.layoutPosX += xmove;
		node.layoutPosY += ymove;
		node.layoutForceX = 0;
		node.layoutForceY = 0;
	}
};

Layout.prototype.layoutRepulsive = function(node1, node2) {
	var dx = node2.layoutPosX - node1.layoutPosX;
	var dy = node2.layoutPosY - node1.layoutPosY;
	var d2 = dx * dx + dy * dy;
	if (d2 < 0.01) {
		dx = 0.1 * Math.random() + 0.1;
		dy = 0.1 * Math.random() + 0.1;
		var d2 = dx * dx + dy * dy;
	}
	var d = Math.sqrt(d2);
	if (d < this.maxRepulsiveForceDistance) {
		var repulsiveForce = this.k * this.k / d;
		node2.layoutForceX += repulsiveForce * dx / d;
		node2.layoutForceY += repulsiveForce * dy / d;
		node1.layoutForceX -= repulsiveForce * dx / d;
		node1.layoutForceY -= repulsiveForce * dy / d;
	}
};

Layout.prototype.layoutAttractive = function(edge) {
	var node1 = edge.source;
	var node2 = edge.target;

	var dx = node2.layoutPosX - node1.layoutPosX;
	var dy = node2.layoutPosY - node1.layoutPosY;
	var d2 = dx * dx + dy * dy;
	if (d2 < 0.01) {
		dx = 0.1 * Math.random() + 0.1;
		dy = 0.1 * Math.random() + 0.1;
		var d2 = dx * dx + dy * dy;
	}
	var d = Math.sqrt(d2);
	if (d > this.maxRepulsiveForceDistance) {
		d = this.maxRepulsiveForceDistance;
		d2 = d * d;
	}
	var attractiveForce = (d2 - this.k * this.k) / this.k;
	if (edge.weight == undefined || edge.weight < 1)
		edge.weight = 1;
	attractiveForce *= Math.log(edge.weight) * 0.5 + 1;

	node2.layoutForceX -= attractiveForce * dx / d;
	node2.layoutForceY -= attractiveForce * dy / d;
	node1.layoutForceX += attractiveForce * dx / d;
	node1.layoutForceY += attractiveForce * dy / d;
};
