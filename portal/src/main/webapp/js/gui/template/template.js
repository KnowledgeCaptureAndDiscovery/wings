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
 * Generic Template Class
 */

var Template = function(id, store, editor) {
	this.id = id;
	this.ns = getNamespace(id);
	this.store = store;
	this.nodes = {};
	this.links = {};
	this.ports = {};
	this.variables = {};
	this.sideEffects = {};
	this.errors = {};
	this.canvasItems = [];
	this.editor = editor;
	
	this.initialize();
	
	this.DBG = false;
};

Template.prototype.getXYFromComment = function(comment) {
	var arr = /x=(.+),y=(.+)/.exec(comment);
	if(!arr) return {x:0,y:0};
	var center = false;
	if(comment.match(/center:/))
		center = true;
	
	return {center: center, x: arr[1], y:arr[2]};
};

Template.prototype.initialize = function() {
	// this.mergeLinks(); // Cleanup
	for ( var i in this.store.Nodes) {
		var node = this.store.Nodes[i];
		if (typeof (node) == "function")
			continue;
		var xy = this.getXYFromComment(node.comment);
		var n = new Node(this, node.id, node.componentVariable, parseInt(xy.x) + 0.5, parseInt(xy.y) + 0.5);
		if(xy.center)
			n.centercoords = true;
		if(node.machineIds)
			n.machineIds = node.machineIds;
		n.setInactive(node.inactive);
		n.setBinding(node.componentVariable.binding);
		n.setConcrete(node.componentVariable.isConcrete);
		n.setComponentRule(node.crule);
		n.setPortRule(node.prule);

		// in case inputPorts/outputPorts are explicitly provided for the nodes
		if (node.inputPorts) {
			for(var portid in node.inputPorts) {
				var p = node.inputPorts[portid];
				this.ports[p.id] = new Port(this, p.id, p.role.roleid);
				this.ports[p.id].dim = parseInt(p.role.dimensionality);
				// FIXME: Temporary -- Don't add port here.
				// Add them while looking at links 
				// - This is done because of a bug in old templates
				//n.addInputPort(this.ports[p.id]);
			}
		}
		if (node.outputPorts) {
			for(var portid in node.outputPorts) {
				var p = node.outputPorts[portid];
				this.ports[p.id] = new Port(this, p.id, p.role.roleid);
				this.ports[p.id].dim = parseInt(p.role.dimensionality);
				// FIXME: Temporary -- Don't add port here.
				// Add them while looking at links 
				// - This is done because of a bug in old templates
				//n.addOutputPort(this.ports[p.id]);
			}
		}
		this.nodes[node.id] = n;
	}
	for ( var i in this.store.Variables) {
		var variable = this.store.Variables[i];
		if (typeof (variable) == "function")
			continue;
		var xy = this.getXYFromComment(variable.comment);
		var type = variable.type == 1 ? 'DATA' : 'PARAM';
		this.variables[variable.id] = new Variable(this, variable.id, getLocalName(variable.id), 
				parseInt(xy.x) + 0.5, parseInt(xy.y) + 0.5, type);
		if(xy.center)
			this.variables[variable.id].centercoords = true;
		this.variables[variable.id].setBinding(variable.binding);
		
		// Set input/output role dimensionality
		var irole = this.store.inputRoles[variable.id];
		if(irole)
			this.variables[variable.id].setDimensionality(parseInt(irole.dimensionality));
		var orole = this.store.outputRoles[variable.id];
		if(orole)
			this.variables[variable.id].setDimensionality(parseInt(orole.dimensionality));

		if (variable.autofill)
			this.variables[variable.id].setAutoFill(variable.autofill);
		if (variable.breakpoint)
			this.variables[variable.id].setBreakPoint(variable.breakpoint);
		
		// Set all variables as inactive for now. 
		// Check links/nodes to show them as active
		this.variables[variable.id].setInactive(true);
		
		//FIXME: unknown isn't currently set in the template variables on server
		if (variable.unknown)
			this.variables[variable.id].setIsUnknown(variable.unknown);
	}
	for ( var i in this.store.Links) {
		var link = this.store.Links[i];
		if (typeof (link) == "function")
			continue;
		var fromPort = null;
		var toPort = null;
		if (link.fromPort && link.fromPort.id) {
			if (!this.ports[link.fromPort.id]) {
				this.ports[link.fromPort.id] = new Port(this, link.fromPort.id, link.fromPort.role.roleid);
				this.ports[link.fromPort.id].dim = parseInt(link.fromPort.role.dimensionality);
			}
			var n = this.nodes[link.fromNode.id];
			n.addOutputPort(this.ports[link.fromPort.id]);
			this.ports[link.fromPort.id].type = this.variables[link.variable.id].type;
				
			var v = this.variables[link.variable.id];
			if (link.fromPort.role.dimensionality)
				v.setDimensionality(parseInt(link.fromPort.role.dimensionality));
			if (!link.toPort)
				v.setIsOutput(true);
			if(!n.isInactive())
				v.setInactive(false);

			fromPort = this.ports[link.fromPort.id];
		}
		if (link.toPort && link.toPort.id) {
			if (!this.ports[link.toPort.id]) {
				this.ports[link.toPort.id] = new Port(this, link.toPort.id, link.toPort.role.roleid);
				this.ports[link.toPort.id].dim = parseInt(link.toPort.role.dimensionality);
			}
			var n = this.nodes[link.toNode.id];
			n.addInputPort(this.ports[link.toPort.id]);
			this.ports[link.toPort.id].type = this.variables[link.variable.id].type;

			var v = this.variables[link.variable.id];
			if (link.toPort.role.dimensionality)
				v.setDimensionality(parseInt(link.toPort.role.dimensionality));
			if (!link.fromPort)
				v.setIsInput(true);
			if(!n.isInactive())
				v.setInactive(false);
			
			toPort = this.ports[link.toPort.id];
		}
		link = new Link(this, fromPort, toPort, this.variables[link.variable.id]);
		
		var ind=1;
		var lid = link.id;
		while(this.links[lid])
			lid = link.id + ind++;
		
		link.id = lid;
		this.links[link.id] = link;
	}
	for ( var i in this.nodes) {
		var node = this.nodes[i];
		if (typeof (node) == "function")
			continue;
		node.setConcrete(node.isConcrete);
		if(!node.prule.expr.args.length)
			node.setDefaultPortRule();
		this.canvasItems = this.canvasItems.concat(this.nodes[node.id].getLayerItems());
	}
	for ( var i in this.variables) {
		var variable = this.variables[i];
		if (typeof (variable) == "function")
			continue;
		this.canvasItems = this.canvasItems.concat(this.variables[variable.id].getLayerItems());
	}

	for ( var i in this.links) {
		var link = this.links[i];
		if (typeof (link) == "function")
			continue;
		this.canvasItems.push(this.links[link.id].getLayerItem());
	}
	this.forwardSweep();
};

Template.prototype.saveToStore = function(showFullPorts) {
	this.store.Nodes = [];
	this.store.Links = [];
	this.store.Variables = [];
	this.store.inputRoles = {};
	this.store.outputRoles = {};
	var cnt = 1;
	var ports = {};
	for ( var i in this.nodes) {
		var n = this.nodes[i];
		var ips = {};
		var ops = {};
		var iports = n.getInputPorts();
		var oports = n.getOutputPorts();
		for ( var j = 0; j < iports.length; j++) {
			var ip = {
				id : iports[j].id,
				role : {
					id : iports[j].id+"_role",
					roleid : iports[j].name,
					dimensionality : iports[j].dim
				}
			};
			ports[ip.id] = ip;
			ips[ip.id] = ip;
		}
		for ( var j = 0; j < oports.length; j++) {
			var op = {
				id : oports[j].id,
				role : {
					id : oports[j].id+"_role",
					roleid : oports[j].name,
					dimensionality : oports[j].dim
				}
			};
			ports[op.id] = op;
			ops[op.id] = op;
		}
		this.store.Nodes.push({
			id : n.id,
			comment : "x="+n.x+",y="+n.y,
			crule : n.crule,
			prule : n.prule,
			componentVariable : {
				id : n.id+"_component",
				isConcrete : n.isConcrete,
				binding : n.binding,
				type : 3
			},
			inputPorts : ips,
			outputPorts : ops,
			machineIds : n.machineIds
		});
		cnt++;
	}
	for ( var i in this.variables) {
		var v = this.variables[i];
		this.store.Variables.push({
			id : v.id,
			comment : "x="+v.x+",y="+v.y,
			type : v.type == 'DATA' ? 1 : 2,
			binding : v.binding,
			//FIXME: unknown isn't currently stored on server
			unknown : v.unknown, 
			autofill : v.autofill,
			breakpoint: v.breakpoint
		});
	}
	for ( var i in this.links) {
		var l = this.links[i];
		var link = {
			id : l.id,
			variable : {
				id: l.variable.id
			}
		};
		if (l.fromNode && l.fromPort) {
			link.fromNode = {
					id: l.fromPort.partOf.id
			};
			link.fromPort = {
					id: l.fromPort.id
			};
			if (showFullPorts)
				link.fromPort = ports[link.fromPort.id];
		}
		else {
			// This is an input link, so set the input role
			this.store.inputRoles[l.variable.id] = {
				type : 	l.variable.type == 'PARAM' ? 1 : 2, // FIXME: variable type numbers are opposite of role type numbers
				roleid : l.toPort.name,
				dimensionality : l.variable.dim,
				id : l.variable.id+"_irole"
			};
		}
		if (l.toPort) {
			link.toNode = {
					id: l.toPort.partOf.id
			};
			link.toPort = {
					id: l.toPort.id
			};
			if (showFullPorts)
				link.toPort = ports[link.toPort.id];
		}
		else {
			// This is an output link, so set the output role
			this.store.outputRoles[l.variable.id] = {
				type : 	l.variable.type == 'PARAM' ? 1 : 2, // FIXME: variable type numbers are opposite of role type numbers
				roleid : l.fromPort.name,
				dimensionality : l.variable.dim,
				id : l.variable.id+"_orole"
			};
		}
		this.store.Links.push(link);
	}

	this.mergeLinks(); // Cleanup
};

Template.prototype.getCanvasItems = function() {
	return this.canvasItems;
};

Template.prototype.convertId = function(id) {
	return this.ns + getLocalName(id);
};

Template.prototype.getFreeVariableId = function(varid) {
	varid = this.convertId(varid);
	if (!this.variables[varid] && (varid != this.id))
		return varid;
	var i = 1;
	while (this.variables[varid + i] || (this.id == varid + i))
		i++;
	return varid + i;
};

Template.prototype.getFreeNodeId = function(nodeid) {
	nodeid = this.convertId(nodeid + 'Node');
	if (!this.nodes[nodeid] && (nodeid != this.id))
		return nodeid;
	var i = 1;
	while (this.nodes[nodeid + i] || (this.id == nodeid + i))
		i++;
	return nodeid + i;
};

Template.prototype.getFreePortId = function(portid) {
	portid = this.convertId('port_' + portid);
	if (!this.ports[portid] && (portid != this.id))
		return portid;
	var i = 1;
	while (this.ports[portid + i] || (this.id == portid + i))
		i++;
	return portid + i;
};

Template.prototype.getNumLinks = function() {
	var num = 0;
	for ( var lid in this.links)
		num++;
	return num;
};

// There can be multiple links with the same variable
Template.prototype.getLinksWithVariable = function(variable) {
	var links = [];
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.variable == variable) {
			links.push(l);
		}
	}
	return links;
};

Template.prototype.getLinksFromNode = function(node) {
	var links = [];
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.fromNode == node) {
			links.push(l);
		}
	}
	return links;
};

Template.prototype.getLinksToNode = function(node) {
	var links = [];
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.toNode == node) {
			links.push(l);
		}
	}
	return links;
};

// We can have multiple links coming out of a port
Template.prototype.getLinksFromPort = function(port) {
	var links = [];
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.fromPort == port) {
			links.push(l);
		}
	}
	return links;
};

// We can have only 1 link going to a port
Template.prototype.getLinkToPort = function(port) {
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.toPort == port) {
			return l;
		}
	}
	return null;
};

Template.prototype.getLinksWithVariable = function(variable) {
	var links = [];
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.variable == variable) {
			links.push(l);
		}
	}
	return links;
};

Template.prototype.getLinksFromPortWithVariable = function(port, variable) {
	var links = [];
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.variable == variable && l.fromPort == port) {
			links.push(l);
		}
	}
	return links;
};

// We can have only 1 link going to a port
Template.prototype.getLinkToPortWithVariable = function(port, variable) {
	var links = [];
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.variable == variable && l.toPort == port) {
			return l;
		}
	}
	return null;
};

Template.prototype.getNodesWithComponent = function(compid) {
	var nodes = [];
	for ( var nid in this.nodes) {
		var n = this.nodes[nid];
		if (n.component == compid)
			nodes.push(n);
	}
	return nodes;
};

/**
 * Add Variable
 */
Template.prototype.addVariable = function(varid) {
	varid = this.getFreeVariableId(varid);
	var v = new Variable(this, varid, getLocalName(varid), 0, 0);
	this.variables[varid] = v;
	this.registerCanvasItem(v);
	return v;
};

/**
 * Add Node
 */
Template.prototype.addNode = function(comp) {
	var nodeid = this.getFreeNodeId(comp.id);
	var node = new Node(this, nodeid, {id:nodeid+"_comp"}, 0, 0);
	node.setBinding({id:comp.id, type:'uri'});
	
	for ( var i = 0; i < comp.inputs.length; i++) {
		var ip = comp.inputs[i];
		var pid = this.getFreePortId(ip.role);
		var port = new Port(this, pid, ip.role);
		port.dim = parseInt(ip.dimensionality);
		port.type = ip.isParam ? 'PARAM' : 'DATA';
		this.ports[pid] = port;
		node.addInputPort(port);
	}
	for ( var i = 0; i < comp.outputs.length; i++) {
		var op = comp.outputs[i];
		var pid = this.getFreePortId(op.role);
		var port = new Port(this, pid, op.role);
		port.dim = parseInt(op.dimensionality);
		port.type = op.isParam ? 'PARAM' : 'DATA';
		this.ports[pid] = port;
		node.addOutputPort(port);
	}
	node.setConcrete(comp.concrete);
	node.setDefaultPortRule();
	
	this.nodes[node.id] = node;
	this.registerCanvasItem(node);
	return node;
};

/*
 * Fill out unbound Node IO automatically
 * - i.e. add input/output links to ports that are currently un-attached
 */
Template.prototype.bindUnboundNodeIO = function(node, c) {
	var itypes = {};
	var otypes = {};
	var iparams = {};
	var iports = node.getInputPorts();
	var oports = node.getOutputPorts();

	for ( var i = 0; i < c.inputs.length; i++) {
		if (!c.inputs[i].isParam)
			itypes[c.inputs[i].role] = c.inputs[i].type;
		else
			iparams[c.inputs[i].role] = true;
	}
	for ( var i = 0; i < c.outputs.length; i++)
		otypes[c.outputs[i].role] = c.outputs[i].type;

	for ( var i = 0; i < iports.length; i++) {
		var ip = iports[i];
		if (!ip.partOfLink) {
			var v = this.addVariable(ip.name);
			// v.setType(ip.type);
			v.setType(iparams[ip.name] ? 'PARAM' : 'DATA');
			this.addInputLink(ip, v);
			if (itypes[ip.name])
				this.setVariableType(v, itypes[ip.name]);
		}
	}
	for ( var i = 0; i < oports.length; i++) {
		var op = oports[i];
		if (!op.partOfLink) {
			var v = this.addVariable(op.name);
			v.setType(op.type);
			this.addOutputLink(op, v);
			if (otypes[op.name])
				this.setVariableType(v, otypes[op.name]);
		}
	}
};

/**
 * Add Link from Component to a Variable
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addLinkToVariable = function(fromPort, toPort, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("-- add Link To Variable");
	// Reorder fromPort/toPort if required
	if (fromPort.isInput) {
		var tmp = fromPort;
		fromPort = toPort;
		toPort = tmp;
	}
	// One of the ports needs to be a variable port
	// - Get the variable included in the link, and fromNodePort, or toNodePort
	var variable;
	var fromNodePort;
	var toNodePort;
	if (fromPort.isVariablePort) {
		variable = fromPort.partOf;
		var ls = this.getLinksWithVariable(variable);
		if (ls.length)
			fromNodePort = ls[0].fromPort;
		toNodePort = toPort;
	}
	else if (toPort.isVariablePort) {
		variable = toPort.partOf;
		var ls = this.getLinksWithVariable(variable);
		toNodePort = new Array();
		for ( var i = 0; i < ls.length; i++)
			toNodePort.push(ls[i].toPort);
		fromNodePort = fromPort;
	}
	if (!variable)
		return false;

	if (toNodePort instanceof Array) {
		for ( var i = 0; i < toNodePort.length; i++) {
			if (!this.addLink(fromNodePort, toNodePort[i], variable, markOnly))
				return false;
		}
	}
	else
		return this.addLink(fromNodePort, toNodePort, variable, markOnly);
};

/**
 * Add Input Link
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addInputLink = function(toNodePort, variable, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("-- add Input Link");
	return this.addLink(null, toNodePort, variable, markOnly);
};

/**
 * Add Output Link
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addOutputLink = function(fromNodePort, variable, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("-- add Output Link");
	return this.addLink(fromNodePort, null, variable, markOnly);
};

/**
 * Add a link from component to new component
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addLink = function(fromNodePort, toNodePort, variable, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("-- add Link from " + (fromNodePort ? fromNodePort.id : null) + " -> "
				+ (toNodePort ? toNodePort.id : null) + " [" + variable.id + "]");

	if (!markOnly && !variable)
		return false;
	// Reorder fromPort/toPort if required
	if (fromNodePort && fromNodePort.isInput) {
		var tmp = fromNodePort;
		fromNodePort = toNodePort;
		toNodePort = tmp;
	}

	var v = variable;
	if (!markOnly && v.isPlaceholder) {
		v = new Variable(this, variable.id, variable.text, variable.x, variable.y);
		if (toNodePort && toNodePort.dim)
			v.setDimensionality(toNodePort.dim);
		if (fromNodePort && fromNodePort.dim)
			v.setDimensionality(fromNodePort.dim);
		if (toNodePort)
			v.setType(toNodePort.type);
		else if (fromNodePort)
			v.setType(fromNodePort.type);
		this.variables[v.id] = v;
		this.registerCanvasItem(v);
	}
	if (!markOnly) {
		v.setIsInput(!fromNodePort);
		v.setIsOutput(!toNodePort);
	}

	// Exit if if the required link already exists
	if (this.duplicateLinkExists(fromNodePort, toNodePort, v))
		return false;

	this.linkAdditionSideEffects(fromNodePort, toNodePort, v, markOnly);
	if (markOnly) {
		this.removeOrphanVariables(markOnly);
		return true;
	}

	var l = new Link(this, fromNodePort, toNodePort, v);
	this.links[l.id] = l;
	this.registerCanvasItem(l);
	if (!markOnly && this.DBG && window.console)
		window.console.log("new link id: " + l.id);

	this.removeOrphanVariables();
	return true;
};

/**
 * Make Link consistency checks
 */
// Check for link duplicates
Template.prototype.duplicateLinkExists = function(fromNodePort, toNodePort, variable) {
	var links = this.getLinksWithVariable(variable);
	for ( var i = 0; i < links.length; i++)
		if (links[i].fromPort == fromNodePort && links[i].toPort == toNodePort)
			return true;
	return false;
};

// Side effects of adding a new link
Template.prototype.linkAdditionSideEffects = function(fromNodePort, toNodePort, variable, markOnly) {
	// If a link already exists to the port that we are creating a link to
	// - then remove that link's destination
	// - Caveat: if we are creating a link to the same variable, then dont remove
	// the link's destination
	if (toNodePort) {
		var link = this.getLinkToPort(toNodePort);
		if (link && (!markOnly || link.variable != variable))
			this.changeLinkDestination(link, null, markOnly);
	}

	if (fromNodePort) {
		// If links already exists from the port that we are creating a link from
		// - then remove the link's origin if they don't contain the same variable
		var links = this.getLinksFromPort(fromNodePort);
		for ( var i = 0; i < links.length; i++) {
			if (links[i].variable != variable)
				this.changeLinkOrigin(links[i], null, markOnly);
		}
	}

	if (variable) {
		// If the variable we are including in the link is already present in some
		// links as the output of a node
		// - then change the origin of those links to the origin of the link we
		// are creating
		var links = this.getLinksWithVariable(variable);
		for ( var i = 0; i < links.length; i++) {
			if (links[i].fromPort && !links[i].toPort && links[i].fromPort != fromNodePort)
				this.changeLinkOrigin(links[i], fromNodePort, markOnly);
			if (links[i].fromPort && !links[i].toPort && links[i].fromPort == fromNodePort)
				this.changeLinkOrigin(links[i], null, markOnly);
		}
	}
};

//
// Cleanup Function to merge pairs of input and output links containing the same
// variable to an InOutLink
//
Template.prototype.mergeLinks = function() {
	var inputLinks = {};
	var outputLinks = {};
	var inOutLinks = {};
	for ( var lid in this.store.links) {
		var l = this.store.links[lid];
		if (typeof (l) == "function")
			continue;
		if (!l.fromPort)
			inputLinks[l.variable] = l;
		else if (!l.toPort)
			outputLinks[l.variable] = l;
		else
			inOutLinks[l.variable] = l;
	}
	for ( var vid in inputLinks) {
		var il = inputLinks[vid];
		var ol = outputLinks[vid];
		var iol = inOutLinks[vid];
		if (ol) {
			il.fromPort = ol.fromPort;
			il.fromNode = ol.fromNode;
			delete this.store.links[ol.id];
		}
		else if (iol) {
			il.fromPort = iol.fromPort;
			il.fromNode = iol.fromNode;
		}
	}
	for ( var vid in outputLinks) {
		var il = inputLinks[vid];
		var ol = outputLinks[vid];
		var iol = inOutLinks[vid];
		if (il) {
			ol.toPort = il.toPort;
			ol.toNode = il.toNode;
			delete this.store.links[il.id];
		}
		else if (iol) {
			ol.toPort = iol.toPort;
			ol.toNode = iol.toNode;
		}
	}
};

/**
 * Remove a Link
 */
// Need to set the partOfLink property of the ports appropriately
Template.prototype.removeLink = function(link, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("removing link: " + link.id);
	if (markOnly) {
		this.addSideEffects(link.id, {
			op : 'remove'
		});
		return;
	}
	if (link.fromNode) {
		link.fromNode.removeOutputLink(link);
		link.fromPort.partOfLink = null;
	}
	else if (link.toNode) {
		link.toNode.removeInputLink(link);
		link.toPort.partOfLink = null;
	}
	delete this.links[link.id];
	this.deRegisterCanvasItem(link);
};

// Need to set the partOfLink property of the ports appropriately
Template.prototype.changeLinkOrigin = function(link, fromPort, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("removing link origin for: " + link.id);
	if (link.toPort == null)
		return this.removeLink(link, markOnly);
	if (markOnly) {
		this.addSideEffects(link.id, {
			op : 'changeFromPort',
			fromPort : fromPort
		});
		return;
	}
	if (!fromPort)
		link.variable.setIsInput(true);
	if (link.fromNode)
		link.fromNode.removeOutputLink(link);
	delete this.links[link.id];
	link.setFromPort(fromPort);
	if (!markOnly && this.DBG && window.console)
		window.console.log("new link id: " + link.id);
	this.links[link.id] = link;
};

Template.prototype.changeLinkDestination = function(link, toPort, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("removing link destination for: " + link.id);
	if (link.fromPort == null)
		return this.removeLink(link, markOnly);
	if (markOnly) {
		this.addSideEffects(link.id, {
			op : 'changeToPort',
			toPort : toPort
		});
		return;
	}
	if (!markOnly && !toPort)
		link.variable.setIsOutput(true);
	if (link.toNode)
		link.toNode.removeInputLink(link);
	delete this.links[link.id];
	link.setToPort(toPort);
	if (!markOnly && this.DBG && window.console)
		window.console.log("new link id: " + link.id);
	this.links[link.id] = link;
};

/**
 * Remove a Variable
 */
Template.prototype.removeVariable = function(variable) {
	delete this.variables[variable.id];
	this.deRegisterCanvasItem(variable);

	// Also Delete all links containing the variable
	var links = this.getLinksWithVariable(variable);
	for ( var i = 0; i < links.length; i++) {
		this.removeLink(links[i]);
	}
};

/**
 * Remove a Node
 */
Template.prototype.removeNode = function(node) {
	delete this.nodes[node.id];
	this.deRegisterCanvasItem(node);

	// Also Delete all input and output links from/to the node
	var iports = node.getInputPorts();
	for ( var i = 0; i < iports.length; i++) {
		var l = this.getLinkToPort(iports[i]);
		if (l)
			this.changeLinkDestination(l, null);
	}
	var oports = node.getOutputPorts();
	for ( var i = 0; i < oports.length; i++) {
		var ls = this.getLinksFromPort(oports[i]);
		for ( var j = 0; j < ls.length; j++) {
			this.changeLinkOrigin(ls[j], null);
		}
	}
	this.removeOrphanVariables();
};

// Remove the variable if it is an orphan (not used by any link)
Template.prototype.removeOrphanVariables = function(markOnly) {
	var variables = {};
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (markOnly && this.sideEffects[lid] && this.sideEffects[lid].op == "remove")
			continue;
		variables[l.variable.id] = true;
	}
	for ( var vid in this.variables) {
		if (!variables[vid]) {
			if (markOnly)
				this.addSideEffects(vid, {
					op : "remove"
				});
			else
				this.removeVariable(this.variables[vid]);
		}
	}
};

/**
 * Canvas Operations
 */
Template.prototype.markLinkAdditionSideEffects = function(fromPort, toPort, skolemVariable) {
	this.clearSideEffects();
	this.addLinkInCanvas(fromPort, toPort, skolemVariable, true);
};

Template.prototype.addLinkInCanvas = function(fromPort, toPort, skolemVariable, markOnly) {
	if (fromPort && toPort) {
		if (!fromPort.isVariablePort && !toPort.isVariablePort && skolemVariable)
			this.addLink(fromPort, toPort, skolemVariable, markOnly);
		else
			this.addLinkToVariable(fromPort, toPort, markOnly);
	}
	else if (fromPort && !fromPort.isVariablePort) {
		if (fromPort.isInput)
			this.addInputLink(fromPort, skolemVariable, markOnly);
		else
			this.addOutputLink(fromPort, skolemVariable, markOnly);
	}
};

Template.prototype.clearSideEffects = function() {
	this.sideEffects = {};
};

Template.prototype.addSideEffects = function(id, effect) {
	if (!this.sideEffects[id])
		this.sideEffects[id] = {};
	for (key in effect)
		this.sideEffects[id][key] = effect[key];
};

Template.prototype.markErrors = function() {
	this.clearErrors();
	var ports = {};
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.toPort)
			ports[l.toPort.id] = true;
		if (l.fromPort)
			ports[l.fromPort.id] = true;
	}
	
	for ( var nid in this.nodes) {
		var node = this.nodes[nid];
		if(this.checkComponentChanged(node)) {
			this.addError(nid, {
				msg : node.text + " used in this node is no longer available"
			});
		}
		var nports = node.getPorts();
		for ( var i = 0; i < nports.length; i++) {
			var port = nports[i];
			if (!ports[port.id]) {
				this.addError(nid, {
					msg : "Missing Links for this node"
				});
				break;
			}
		}
	}
};

Template.prototype.checkComponentChanged = function(node) {
	if(!this.editor || !this.editor.cmap) 
		return false;
	var c = this.editor.cmap[node.getBindingId()];
	if(!c) 
		return true;
	return false;
};

Template.prototype.clearErrors = function() {
	this.errors = {};
};

Template.prototype.addError = function(id, error) {
	if (!this.errors[id])
		this.errors[id] = {};
	for (key in error)
		this.errors[id][key] = error[key];
};

/**
 * Canvas Item registraion/deRegistration
 */
Template.prototype.registerCanvasItem = function(item) {
	var layerItems = [];
	if (item.getLayerItems)
		layerItems = item.getLayerItems();
	else if (item.getLayerItem)
		layerItems.push(item.getLayerItem());

	this.canvasItems = this.canvasItems.concat(layerItems);
	for ( var i = 0; i < layerItems.length; i++) {
		var layerItem = layerItems[i];
		this.editor.template_layer.addItem(layerItem);
	}
};

Template.prototype.deRegisterCanvasItem = function(item) {
	var layerItems = [];
	if (item.getLayerItems)
		layerItems = item.getLayerItems();
	else if (item.getLayerItem)
		layerItems.push(item.getLayerItem());

	if (!layerItems.length)
		return 0;

	// Assuming that the layerItems are all added together into canvasItems (??)
	var index = this.canvasItems.indexOf(layerItems[0]);
	if (index >= 0)
		this.canvasItems.splice(index, layerItems.length);
	for ( var i = 0; i < layerItems.length; i++) {
		var layerItem = layerItems[i];
		this.editor.template_layer.removeItem(layerItem.id);
	}
};

/**
 * Forward sweep through the template to setup variable dimensionalities
 */

Template.prototype.forwardSweep = function() {
	var links = [];
	var doneLinks = {};
	for ( var lid in this.links) {
		if (!this.links[lid].fromNode)
			links.push(this.links[lid]);
	}
	while (links.length > 0) {
		var l = links.pop();
		if (doneLinks[l.id])
			continue;
		doneLinks[l.id] = true;

		l.variable.setIsInput(!l.fromNode);
		l.variable.setIsOutput(!l.toNode);

		var n = l.toNode;
		if (!n)
			continue;

		var extraDim = 0;
		var ok = true;
		for ( var i = 0; i < n.inputLinks.length; i++) {
			var il = n.inputLinks[i];
			if (!doneLinks[il.id]) {
				ok = false;
				break;
			}
			var tmpDim = il.variable.dim - il.toPort.dim;
			if (extraDim < tmpDim)
				extraDim = tmpDim;
		}
		if (!ok) {
			links.push(l);
			continue;
		}

		n.dim = 0;
		for ( var i = 0; i < n.outputLinks.length; i++) {
			var ol = n.outputLinks[i];
			if (n.prule.type == 'STYPE') {
				ol.variable.setDimensionality(ol.fromPort.dim + extraDim);
				// n.dim = ol.fromPort.dim + extraDim;
				n.dim = extraDim;
			}
			else {
				ol.variable.setDimensionality(ol.fromPort.dim);
				// n.dim = ol.fromPort.dim;
			}
			links.push(ol);
		}
	}
};

Template.prototype.getVariablePropertyValue = function(v, prop) {
	for ( var i = 0; i < this.store.constraints.length; i++) {
		var cons = this.store.constraints[i];
		if (cons.subject == v.id && cons.predicate == prop) {
			return cons.object;
		}
	}
};
Template.prototype.setVariablePropertyValue = function(v, prop, value) {
	var updated = false;
	for ( var i = 0; i < this.store.constraints.length; i++) {
		var cons = this.store.constraints[i];
		if (cons.subject == v.id && cons.predicate == prop) {
			cons.object = value;
			updated = true;
		}
		this.store.constraints[i] = cons;
	}
	if (!updated)
		this.store.constraints.push({
			subject : v.id,
			predicate : prop,
			object : value
		});
};

Template.prototype.getVariableType = function(v) {
	var type = this.getVariablePropertyValue(v, 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type');
	return type;
};
Template.prototype.setVariableType = function(v, value) {
	this.setVariablePropertyValue(v, 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type', value);
};

function cloneObj(o) {
	return Ext.clone(o);
	// return jQuery.extend(true, {}, o);
}

Template.prototype.createCopy = function() {
	this.saveToStore(true);
	var store = cloneObj(this.store);
	return new Template(this.id, store, this.editor);
};

Template.prototype.equals = function(t) {
	if (!t)
		return false;
	if (this.isSubsetOf(t) && t.isSubsetOf(this))
		return true;
	return false;
};

Template.prototype.isSubsetOf = function(t) {
	if (!t)
		return false;
	if (!t.store)
		return false;
	if (!this.checkLinkSubset(this.links, t.links))
		return false;
	if (!this.checkConstraintsSubset(this.store.constraints, t.store.constraints))
		return false;
	return true;
};

Template.prototype.checkLinkSubset = function(links1, links2) {
	for ( var lid in links1) {
		var link1 = links1[lid];
		var link2 = links2[lid];
		if (!link2)
			return false;
		if (!link1.equals(link2))
			return false;
	}
	return true;
};

Template.prototype.checkConstraintsSubset = function(constraints1, constraints2) {
	for ( var i = 0; i < constraints1.length; i++) {
		var cons1 = constraints1[i];
		var found = false;
		for ( var j = 0; j < constraints2.length; j++) {
			var cons2 = constraints2[j];
			if (cons1.subject == cons2.subject 
					&& cons1.predicate == cons2.predicate 
					&& cons1.object == cons2.object) {
				found = true;
				break;
			}
		}
		if (!found)
			return false;
	}
	return true;
};
