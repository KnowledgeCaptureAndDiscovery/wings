function Template(id, store, editor) {
	this.id = id;
	this.editor = editor;

	// Different sizes to handle scaling properly
	this.scale = 1;
	this.panelsize = {width: 0, height: 0};
	this.svgsize = {width: 0, height: 0};
	this.graphsize = {width: 0, height: 0};
	
	// SVG's width and height
	this.width = 0;
	this.height = 0;
	
	// D3 items
	this.svg = null;
	this.defs = null
	this.graph = null;
	this.tooltip = null;

	// Individual D3 Items
	this.nodes = {};
	this.links = {};
	this.variables = {};
		
	// sorted graph items
	this.graphItems = [];
	
	this.editable = false;
	this.initDrawingSurface();
	
	this.store = null;
	this.events = new TemplateEvents(this);
	
	this.DBG = 0;
	
	this.setData(store);
};

Template.prototype.isEditable = function() {
	return this.editable;
};

Template.prototype.setEditable = function(editable) {
	this.events.setEditable(editable);
};

Template.prototype.resizePanel = function(animate) {
	this.panelsize = {
		width: this.domnode.offsetWidth, 
		height: this.domnode.offsetHeight
	};
	this.resizeViewport(true, animate);
};

Template.prototype.setScale = function(scale, animate) {
	this.scale = scale;
	this.resizeSVG(animate);
};

Template.prototype.setSVGSize = function(w, h, animate) {
	this.svgsize = {width: w, height: h};
	if(animate)
		this.svg.transition().attr("width", w).attr("height", h);
	else
		this.svg.attr("width", w).attr("height", h);
};

Template.prototype.setGraphSize = function(w, h) {
	this.graphsize = {width: w, height: h};
};

Template.prototype.resizeSVG = function(animate) {	
	var w = this.scale*this.graphsize.width;
	var h = this.scale*this.graphsize.height
	if(w && h)
		this.setSVGSize(w, h, animate);
};

Template.prototype.setData = function(store) {
	this.store = store;
	this.graph.id = getLocalName(store.id);
	this.graph.node().innerHTML = "";
	
	this.nodes = {};
	this.variables = {};
	this.links = {};

	// Create nodes
	for(var nodeid in store.Nodes) {
		var nodedata = store.Nodes[nodeid];
		var gnode = new GraphNode(this.graph, nodedata, new GraphNodeConfig());
		gnode.setCoords(this.getItemCoordinates(nodedata));
		this.nodes[nodeid] = gnode;
	}
	// Create variables
	for(var varid in store.Variables) {
		var vardata = store.Variables[varid];
		var gvar = new GraphVariable(this.graph, vardata, new GraphVariableConfig());
		gvar.setCoords(this.getItemCoordinates(vardata));
		this.variables[varid] = gvar;
	}	
	// Create Links
	for(var linkid in store.Links) {
		var linkdata = store.Links[linkid];
		var fromNode = null,
			fromPort = null,
			toNode = null,
			toPort = null,
			variable = null;
		
		if(linkdata.fromNode) {
			fromNode = this.nodes[linkdata.fromNode.id];
			fromPort = fromNode.outputPorts[linkdata.fromPort.id];
		}
		if(linkdata.toNode) {
			toNode = this.nodes[linkdata.toNode.id];
			toPort = toNode.inputPorts[linkdata.toPort.id];
		}
		variable = this.variables[linkdata.variable.id];	
		
		var glink = new GraphLink(this.graph, linkdata.id,
				fromNode, fromPort, toNode, toPort, variable,
				this.graphItems, new GraphLinkConfig());
		
		glink.setInactive(linkdata.inactive);
		
		if(fromNode) {
			glink.setInactive(fromNode.inactive);
			variable.setInactive(fromNode.inactive);
		}
		else if(toNode) {
			variable.setInactive(toNode.inactive);
		}
		this.links[linkid] = glink;
	}
	
	// Set input role dimensionality
	for(var varid in store.inputRoles) {
		var gvar = this.variables[varid];
		gvar.setDimensionality(store.inputRoles[varid].dimensionality);
	}

	// Set appropriate dimensionality
	this.forwardSweep();
};

Template.prototype.drawLinks = function(animate) {
	// Register new set of graph items to links (to do path avoidance)
	this.refreshGraphItems();
	for ( var lid in this.links) {
		var l = this.links[lid];
		l.draw(animate);
	}
};

Template.prototype.draw = function(domnode) {
	if(domnode) {
		this.domnode = domnode;
		domnode.innerHTML = '';
		domnode.appendChild(this.svg.node());
		this.panelsize = {
			width: this.domnode.offsetWidth, 
			height: this.domnode.offsetHeight
		};
	}
	
	// Draw nodes
	for(var nid in this.nodes) {
		var n = this.nodes[nid];
		n.draw();
		if(n.getCoords().legacy) {
			n.setLegacyCoords(n.getCoords());
			n.getCoords().legacy = false;
		}
	}
	
	// Draw variables
	for(var vid in this.variables) {
		var v = this.variables[vid];
		v.draw();
		if(v.getCoords().legacy) {
			v.setLegacyCoords(v.getCoords());
			v.getCoords().legacy = false;
		}
	}
	
	// Draw links
	this.drawLinks();

	this.resizeViewport(true);
	this.events.initialize();
};

Template.prototype.resizeViewport = function(fitclient, animate) {
	// Adjust viewport to see the whole graph
	this.calculateGraphSize();
	this.setViewport();
	if(fitclient) {
		if(this.graphsize.width && this.graphsize.height) {
			var scalex = this.panelsize.width/this.graphsize.width;
			var scaley = this.panelsize.height/this.graphsize.height;
			this.scale = scalex < scaley ? scalex : scaley;
		}
		else
			this.scale = 1;
	}
	this.resizeSVG(animate);
};

Template.prototype.calculateGraphSize = function() {
	var maxx = 0, maxy = 0;
	var xpad = 20; ypad = 20;
	var items = Object.assign({}, this.nodes, this.variables);
	for(var id in items) {
		var item = items[id];
		var bounds = item.getBounds();
		var coords = item.getCoords();
		var itemx = coords.x + bounds.x + bounds.width + xpad;
		var itemy = coords.y + bounds.y + bounds.height + ypad;
		if(maxx < itemx) 
			maxx = itemx;
		if(maxy < itemy)
			maxy = itemy;
	}
	if(!maxx && !maxy) {
		maxx = this.panelsize.width;
		maxy = this.panelsize.height;
	}
	this.setGraphSize(maxx, maxy);
};

Template.prototype.calculateGraphSizeAfterMove = function(moveditems) {
	var maxx = this.graphsize.width, maxy = this.graphsize.height;
	var xpad = 20; ypad = 20;
	for(var id in moveditems) {
		var item = moveditems[id];
		var bounds = item.getBounds();
		var coords = item.getCoords();
		var itemx = coords.x + bounds.x + bounds.width + xpad;
		var itemy = coords.y + bounds.y + bounds.height + ypad;
		if(maxx < itemx) 
			maxx = itemx;
		if(maxy < itemy)
			maxy = itemy;
	}
	this.setGraphSize(maxx, maxy);
};

Template.prototype.setViewport = function(animate) {
	if(animate)
		this.svg.transition().
			attr("viewBox", "0 0 "+this.graphsize.width + " "+this.graphsize.height);
	else
		this.svg.attr("viewBox", "0 0 "+this.graphsize.width + " "+this.graphsize.height);
};

Template.prototype.getItemCoordinates = function(item) {
	var arr = /x=(.+),y=(.+)/.exec(item.comment);
	if(!arr) return {x:0,y:0};
	var legacy = true;
	if(item.comment.match(/center:/))
		legacy = false;
	return {legacy: legacy, 
		x: parseFloat(arr[1]), 
		y: parseFloat(arr[2])
	};
};

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

		var n = l.toNode;
		if (!n)
			continue;
	
		var ok = true;	
		for ( var i = 0; i < n.inputLinks.length; i++) {
			var il = n.inputLinks[i];
			if (!doneLinks[il.id]) {
				ok = false;
				break;
			}
		}
		if (!ok) {
			links.push(l);
			continue;
		}
		
		var pdim = 0;
		if(n.prule.type == 'STYPE') {
			var portDims = this.getPortDimensionAdjustment(n.prule.expr);
			for ( var i = 0; i < n.inputLinks.length; i++) {
				var il = n.inputLinks[i];
				var dimadj = portDims[il.toPort.id] || 0;
				var tmpDim = il.variable.dimensionality - il.toPort.role.dimensionality + dimadj;
				if (pdim < tmpDim)
					pdim = tmpDim;		
			}
		}
		var cdim = 0;
		if(n.crule.type == 'STYPE') {
			cdim = 1;
		}		
		n.setDimensionality(pdim + cdim);
		
		for ( var i = 0; i < n.outputLinks.length; i++) {
			var ol = n.outputLinks[i];
			ol.variable.setDimensionality(ol.fromPort.role.dimensionality + pdim + cdim);
			links.push(ol);
		}
	}
};

Template.prototype.refreshGraphItems = function() {
	// Get graph items sorted by y-position
	this.graphItems.length = 0;
	for(var nid in this.nodes)
		this.graphItems.push(this.nodes[nid]);
	for(var vid in this.variables)
		this.graphItems.push(this.variables[vid]);
	this.graphItems.sort(function(p1, p2) {
		if (p1.getY() < p2.getY()) return -1;
		if (p1.getY() > p2.getY()) return 1;
		return 0;
	});
};

Template.prototype.getPortDimensionAdjustment = function(expr) {
	var portdims = {};
	var dim = 0;
	if(typeof expr === 'object') {
		if(expr.op == "REDUCEDIM")
			dim = -1;
		if(expr.op == "INCREASEDIM")
			dim = 1;
		for(var i=0; i<expr.args.length; i++) {
			var cpdims = this.getPortDimensionAdjustment(expr.args[i]);
			for(var port in cpdims) {
				portdims[port] = cpdims[port] + dim;
			}
		}
	}
	else {
		portdims[expr] = 0;
	}
	return portdims;
};

Template.prototype.initDrawingSurface = function() {
	this.svg = d3.select(document.createElementNS(d3.namespaces.svg, "svg"))
		.attr("preserveAspectRatio", "xMinYMin");

	var tname = getLocalName(this.id);
	
	// arrow template
	this.svg.append("svg:defs")
		.append("svg:marker")
		.attr("id", "arrow_"+tname)
		.attr("viewBox", "0 0 10 8")
		.attr("refX", 10)
		.attr("refY", 4)
		.attr("markerUnits", "strokeWidth")
		.attr("markerWidth", 10)
		.attr("markerHeight", 8)
		.attr("orient", "auto")
		.append("svg:path")
		.attr("d", "M 0 0 L 10 4 L 0 8 z"); 
	
	// Graph group
	this.graph = this.svg.append("g");
	
	// A dummy SVGPoint for coordinates translation
	this.pt = this.svg.node().createSVGPoint();	
	
	// Define the div for the tooltip
	this.tooltip = d3.select("body").append("div")	
	    	.attr("class", "tooltip")				
	    	.style("opacity", 0);
	
	this.createGrid();
}

Template.prototype.zoom = function(value, animate) {
	this.setScale(this.scale * value, animate);
};

Template.prototype.getImage = function() {
    var xml  = new XMLSerializer().serializeToString(this.svg.node());
    var data = "data:image/svg+xml;base64," + btoa(xml);
    var img  = new Image();
    img.setAttribute('src', data);
    return img;
};

Template.prototype.createGrid = function() {
	this.grid = this.svg.append("g");
};

Template.prototype.showTooltip = function(message, x, y) {	
    this.tooltip.html(message);
    var bbox = this.tooltip.node().getBoundingClientRect();
    this.tooltip
    	.style("left", (x - bbox.width/2) + "px")		
    	.style("top", (y - bbox.height - 15) + "px");
    
    this.tooltip.transition().delay(200)		
		.style("opacity", .9);	
};

Template.prototype.hideTooltip = function() {		
	this.tooltip.transition()	
    	.style("opacity", 0);	
};

Template.prototype.getData = function(showFullPorts) {
	var data = {
		id: this.id,
		ns: getNamespace(this.id),
		props: this.store.props
	};
	
	data.Nodes = {};
	data.Links = {};
	data.Variables = {};
	data.inputRoles = {};
	data.outputRoles = {};
	
	var cnt = 1;
	var ports = {};
	for ( var nid in this.nodes) {
		var n = this.nodes[nid];
		var ips = {};
		var ops = {};
		var iports = n.getInputPorts();
		var oports = n.getOutputPorts();
		var coords = n.getCoords();
		for ( var j = 0; j < iports.length; j++) {
			var ip = {
				id : iports[j].id,
				role : iports[j].role
			};
			ports[ip.id] = ip;
			ips[ip.id] = ip;
		}
		for ( var j = 0; j < oports.length; j++) {
			var op = {
				id : oports[j].id,
				role : oports[j].role
			};
			ports[op.id] = op;
			ops[op.id] = op;
		}
		data.Nodes[n.id] = {
			id : n.id,
			comment : "center:x="+coords.x+",y="+coords.y,
			crule : n.crule,
			prule : n.prule,
			inactive : n.inactive,
			derivedFrom : n.derivedFrom,
			componentVariable : {
				id : n.componentid,
				isConcrete : n.isConcrete,
				binding : n.binding,
				type : 3
			},
			inputPorts : ips,
			outputPorts : ops,
			machineIds : n.machineIds
		};
		cnt++;
	}
	for ( var vid in this.variables) {
		var v = this.variables[vid];
		var coords = v.getCoords();
		data.Variables[v.id] = {
			id : v.id,
			comment : "center:x="+coords.x+",y="+coords.y,
			type : v.isParam ? 2 : 1,
			binding : v.binding,
			inactive : v.inactive,
			derivedFrom : v.derivedFrom,
			//FIXME: unknown isn't currently datad on server
			unknown : v.unknown, 
			autofill : v.autofill,
			breakpoint: v.breakpoint
		};
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
				id: l.fromNode.id
			};
			link.fromPort = {
				id: l.fromPort.id
			};
			if (showFullPorts)
				link.fromPort = ports[link.fromPort.id];
		}
		else {
			// This is an input link, so set the input role
			data.inputRoles[l.variable.id] = {
				type : 	l.variable.isParam ? 2 : 1,
				roleid : l.toPort.getName(),
				dimensionality : l.variable.getDimensionality(),
				id : l.variable.id+"_irole"
			};
		}
		if (l.toPort) {
			link.toNode = {
				id: l.toNode.id
			};
			link.toPort = {
				id: l.toPort.id
			};
			if (showFullPorts)
				link.toPort = ports[link.toPort.id];
		}
		else {
			// This is an output link, so set the output role
			data.outputRoles[l.variable.id] = {
				type : 	l.variable.isParam ? 2 : 1,
				roleid : l.fromPort.getName(),
				dimensionality : l.variable.getDimensionality(),
				id : l.variable.id+"_orole"
			};
		}
		data.Links[l.id] = link;
	}
	return data;
	//this.mergeLinks(); // Cleanup
};


// Template Query functions
Template.prototype.getNumLinks = function() {
	var num = 0;
	for ( var lid in this.links)
		num++;
	return num;
};

// There can be multiple links with the same variable
Template.prototype.getLinksWithVariable = function(variable) {
	return variable.variableLinks.slice(0);
};

Template.prototype.getLinksFromNode = function(node) {
	return node.outputLinks.slice(0);
};

Template.prototype.getLinksToNode = function(node) {
	return node.inputLinks.slice(0);
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

Template.prototype.getLinksFromPortWithVariable = function(port, variable) {
	var links = [];
	for ( var i=0; i<variable.variableLinks; i++) {
		var l = variable.variableLinks[i];
		if (l.fromPort == port) {
			links.push(l);
		}
	}
	return links;
};

// We can have only 1 link going to a port
Template.prototype.getLinkToPortWithVariable = function(port, variable) {
	var links = [];
	for ( var i=0; i<variable.variableLinks; i++) {
		var l = variable.variableLinks[i];
		if (l.toPort == port) {
			links.push(l);
		}
	}
	return links;
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

// Template Editing functions

/**
 * Add Variable
 */
Template.prototype.addVariable = function(varid, isParam) {
	varid = this.getFreeVariableId(varid);
	var vardata = {
		id: varid,
		type: isParam ? 2 : 1
	};
	var v = new GraphVariable(this.graph, vardata, new GraphVariableConfig());
	this.variables[varid] = v;
	this.refreshGraphItems();
	this.events.enableEventsForItem(v);
	return v;
};

/**
 * Add Node
 */
Template.prototype.addNode = function(comp) {
	var nodeid = this.getFreeNodeId(comp.id);
	var componentVariable = {
		id: nodeid + "_comp",
		isConcrete: comp.concrete,
		binding: {
			id: comp.id,
			type: 'uri'
		}
	};
	var inputPorts = {};
	var outputPorts = {};
	var args = [];
	for ( var i = 0; i < comp.inputs.length; i++) {
		var ip = comp.inputs[i];
		var pid = nodeid + "_" + ip.role;
		var roledata = {
			id: pid + "_role",
			roleid: ip.role,
			dimensionality: ip.dimensionality,
			type: ip.isParam ? 2 : 1
		};
		args.push(pid);
		inputPorts[pid] = {id: pid, role: roledata};
	}
	for ( var i = 0; i < comp.outputs.length; i++) {
		var op = comp.outputs[i];
		var pid = nodeid + "_" + op.role;
		var roledata = {
			id: pid + "_role",
			roleid: op.role,
			dimensionality: op.dimensionality,
			type: op.isParam ? 2 : 1
		};
		outputPorts[pid] = {id: pid, role: roledata};
	}
	var nodedata = {
		id: nodeid,
		componentVariable: componentVariable,
		inputPorts : inputPorts,
		outputPorts : outputPorts,
		crule : {type: 'WTYPE'},
		prule : {type: 'STYPE', expr: {op: "XPRODUCT", args: args}}
	};
	var node = new GraphNode(this.graph, nodedata, new GraphNodeConfig());
	this.nodes[node.id] = node;
	this.refreshGraphItems();
	this.events.enableEventsForItem(node);
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
	
	// Figure out which ports don't have links
	var portsfilled = {};
	for(var i=0; i<node.getInputLinks().length; i++) {
		var l = node.getInputLinks()[i]
		portsfilled[l.toPort.id] = true;
	}
	for(var i=0; i<node.getOutputLinks().length; i++) {
		var l = node.getOutputLinks()[i]
		portsfilled[l.fromPort.id] = true;
	}

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
		if (!portsfilled[ip.id]) { 
			var isParam = iparams[ip.role.roleid];
			var v = this.addVariable(ip.role.roleid, isParam);
			this.addInputLink(ip, v);
			if (itypes[ip.role.roleid])
				this.setVariableType(v, itypes[ip.role.roleid]);
		}
	}
	for ( var i = 0; i < oports.length; i++) {
		var op = oports[i];
		if (!portsfilled[op.id]) {
			var v = this.addVariable(op.role.roleid, op.isParam);
			this.addOutputLink(op, v);
			if (otypes[op.role.roleid])
				this.setVariableType(v, otypes[op.role.roleid]);
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
	if (fromPort.graphItem instanceof GraphVariable) {
		// If the connection is from a variable to a node
		variable = fromPort.graphItem;
		var ls = this.getLinksWithVariable(variable);
		if(ls.length) {
			if(!ls[0].toPort) {
				// If existing link is an Output Link, then make it an IO link			
				this.changeLinkDestination(ls[0], toPort, markOnly);
				this.linkAdditionSideEffects(ls[0].fromPort, ls[0].toPort, ls[0].variable, markOnly);
				return ls[0];
			}
			else {
				// Otherwise, just add another link with this variable
				return this.addLink(ls[0].fromPort, toPort, variable, markOnly);
			}
		}
	}
	else if (toPort.graphItem instanceof GraphVariable) {
		// If the connection is from a node to a variable
		variable = toPort.graphItem;
		var ls = this.getLinksWithVariable(variable);
		for ( var i = 0; i < ls.length; i++) {
			// Change link origins for all
			this.changeLinkOrigin(ls[i], fromPort, markOnly);
			this.linkAdditionSideEffects(ls[i].fromPort, ls[i].toPort, ls[i].variable, markOnly);
		}
		return ls;
	}
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
Template.prototype.addLink = function(fromPort, toPort, variable, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("-- add Link from " + (fromPort ? fromPort.getName() : null) + " -> "
				+ (toPort ? toPort.getName() : null) + " [" + variable.getName() + "]");

	if (!markOnly && !variable)
		return null;
	// Reorder fromPort/toPort if required
	if (fromPort && fromPort.isInput) {
		var tmp = fromPort;
		fromPort = toPort;
		toPort = tmp;
	}

	var v = variable;
	if (!markOnly && v.config.placeholder) {
		v = this.addVariable(variable.id, (toPort && toPort.role.type == 2))
		if (fromPort && fromPort.dimensionality)
			v.setDimensionality(fromPort.getDimensionality());
		else if (toPort && toPort.dimensionality)
			v.setDimensionality(toPort.getDimensionality());
		v.setCoords(variable.getCoords());
		v.draw();
	}
	if (!markOnly) {
		v.setIsInput(!fromPort);
		v.setIsOutput(!toPort);
	}

	// Exit if the required link already exists
	if (this.duplicateLinkExists(fromPort, toPort, v))
		return null;

	this.linkAdditionSideEffects(fromPort, toPort, v, markOnly);
	if (markOnly) {
		this.markOrphanVariables();
		return null;
	}
	
	var fromNode = fromPort ? fromPort.graphItem : null;
	var toNode = toPort ? toPort.graphItem : null;
	var l = new GraphLink(this.graph, null,
			fromNode, fromPort, toNode, toPort, v,
			this.graphItems, new GraphLinkConfig());
	l.draw();
	
	this.links[l.id] = l;
	if (!markOnly && this.DBG && window.console)
		window.console.log("new link id: " + l.getDescription());

	return l;
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

// Preview side effects of adding a new link
Template.prototype.linkAdditionSideEffects = function(fromNodePort, toNodePort, variable, markOnly) {
	// If a link already exists to the port that we are creating a link to
	// - then remove that link's destination
	// - Caveat: if we are creating a link to the same variable, then dont remove
	// the link's destination
	if (toNodePort) {
		var link = this.getLinkToPort(toNodePort);
		if (link && link.variable != variable) {
			if(link.variable.variableLinks.length == 1)
				this.changeLinkDestination(link, null, markOnly);
			else
				this.removeLink(link, markOnly);
		}
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
Template.prototype.removeLink = function(link, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("removing link: " + link.getDescription());
	if (markOnly) {
		link.setPreview('remove');
		return;
	}
	if (link.fromNode) {
		link.fromNode.removeOutputLink(link);
	}
	else if (link.toNode) {
		link.toNode.removeInputLink(link);
	}
	if(link.variable) {
		var index = link.variable.variableLinks.indexOf(link);
		link.variable.variableLinks.splice(index, 1);
		if(link.variable.variableLinks.length == 0)
			this.removeVariable(link.variable);
	}
	delete this.links[link.id];
	link.item.remove();
	//FIXME ? this.events.disableEventsForItem(link);
};

Template.prototype.changeLinkOrigin = function(link, fromPort, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("change link origin for: " + link.getDescription()
				+ " to " + fromPort);
	if (!fromPort && !link.toPort)
		return this.removeLink(link, markOnly);
	if (markOnly) {
		link.setPreview('changeFromPort');
		return;
	}
	if (!fromPort) 
		link.variable.setIsInput(true);

	delete this.links[link.id];
	link.clear();
	link.setFromPort(fromPort); // Creates a new link id
	link.draw();
	
	if (!markOnly && this.DBG && window.console)
		window.console.log("new link id: " + link.getDescription());
	this.links[link.id] = link;
};

Template.prototype.changeLinkDestination = function(link, toPort, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("change link destination for: " + link.getDescription() 
				+ " to " + toPort);
	if (!toPort && !link.fromPort)
		return this.removeLink(link, markOnly);
	if (markOnly) {
		link.setPreview('changeToPort');
		return;
	}
	if (!toPort)
		link.variable.setIsOutput(true);
	
	delete this.links[link.id];
	link.clear();
	link.setToPort(toPort); // Creates a new link id
	link.draw();
	
	if (!markOnly && this.DBG && window.console)
		window.console.log("new link id: " + link.getDescription());
	
	this.links[link.id] = link;
};

/**
 * Remove a Variable
 */
Template.prototype.removeVariable = function(variable, markOnly) {
	if (!markOnly && this.DBG && window.console)
		window.console.log("removing variable: " + variable.getName());
	
	if (markOnly) {
		variable.setPreview('remove');
		return;
	}
	
	delete this.variables[variable.id];
	variable.item.remove();

	// Also Delete all links containing the variable
	var links = this.getLinksWithVariable(variable);
	for ( var i = 0; i < links.length; i++) {
		this.removeLink(links[i]);
	}
	
	// Delete all constraints with the variable
	var newcons = [];
	for(var i=0; i<this.store.constraints.length; i++) { 
		var cons = this.store.constraints[i];
		if(cons.subject != variable.id &&
				cons.object != variable.id) {
			newcons.push(cons);
		}
	}
	this.store.constraints = newcons;
	this.refreshGraphItems();
};

/**
 * Rename a Variable
 */
Template.prototype.renameVariable = function(variable, newid) {
	var oldid = variable.id;
	delete this.variables[variable.id];
	variable.setId(newid);
	variable.setText(variable.getBindingText(), true);
	this.variables[variable.id] = variable;
	
	// Rename all constraints with the variable
	var newcons = [];
	for(var i=0; i<this.store.constraints.length; i++) { 
		var cons = this.store.constraints[i];
		if(cons.subject == oldid)
			cons.subject = newid;
		if(cons.object == oldid)
			cons.object = newid;
		newcons.push(cons);
	}
	this.store.constraints = newcons;
};

/**
 * Remove a Node
 */
Template.prototype.removeNode = function(node) {
	delete this.nodes[node.id];
	node.item.remove();

	// Also Delete all input and output links from/to the node
	var iports = node.getInputPorts();
	for ( var i = 0; i < iports.length; i++) {
		var l = this.getLinkToPort(iports[i]);
		if (l)
			this.removeLink(l);
			//this.changeLinkDestination(l, null);
	}
	var oports = node.getOutputPorts();
	for ( var i = 0; i < oports.length; i++) {
		var ls = this.getLinksFromPort(oports[i]);
		for ( var j = 0; j < ls.length; j++) {
			this.removeLink(ls[j]);
			//this.changeLinkOrigin(ls[j], null);
		}
	}
	this.refreshGraphItems();
};

// Mark the variable to remove if it is an orphan (not used by any link)
Template.prototype.markOrphanVariables = function() {
	var variables = {};
	for ( var lid in this.links) {
		var l = this.links[lid];
		if (l.preview != "remove")
			variables[l.variable.id] = true;
	}
	for ( var vid in this.variables) {
		if (!variables[vid]) {
			this.variables[vid].setPreview("remove");
		}
	}
};

/**
 * Canvas Operations
 */
Template.prototype.markLinkAdditionPreview = function(fromPort, toPort, skolemVariable) {
	this.clearPreviews();
	return this.addLinkInCanvas(fromPort, toPort, skolemVariable, true);
};

Template.prototype.addLinkInCanvas = function(fromPort, toPort, skolemVariable, markOnly) {
	if (fromPort && toPort) {
		if ((fromPort.graphItem instanceof GraphNode) && 
				(toPort.graphItem instanceof GraphNode) && 
				skolemVariable)
			return this.addLink(fromPort, toPort, skolemVariable, markOnly);
		else
			return this.addLinkToVariable(fromPort, toPort, markOnly);
	}
	else if (fromPort && (fromPort.graphItem instanceof GraphNode)) {
		if (fromPort.isInput)
			return this.addInputLink(fromPort, skolemVariable, markOnly);
		else
			return this.addOutputLink(fromPort, skolemVariable, markOnly);
	}
};

// Error Handling

Template.prototype.clearPreviews = function() {
	for(var vid in this.variables)
		this.variables[vid].setPreview(null);
	for(var lid in this.links)
		this.links[lid].setPreview(null);
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
		var errors = [];
		if(this.checkComponentChanged(node)) {
			errors.push(node.text + " used in this node is no longer available");
		}
		var nports = node.getInputPorts().concat(node.getOutputPorts());
		for ( var i = 0; i < nports.length; i++) {
			var port = nports[i];
			if (!ports[port.id]) {
				errors.push("Missing Links for this node");
				break;
			}
		}
		if(errors.length) {
			node.setErrors(errors);
		}
	}
};

// Utility functions
Template.prototype.convertId = function(id) {
	return getNamespace(this.id) + getLocalName(id);
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
	nodeid = this.convertId(nodeid + '_node');
	if (!this.nodes[nodeid] && (nodeid != this.id))
		return nodeid;
	var i = 1;
	while (this.nodes[nodeid + i] || (this.id == nodeid + i))
		i++;
	return nodeid + i;
};

Template.prototype.checkComponentChanged = function(node) {
	if(!this.editor || !this.editor.cmap) 
		return false;
	var c = this.editor.cmap[node.binding.id];
	if(!c) 
		return true;
	return false;
};

Template.prototype.clearErrors = function() {
	for(var nid in this.nodes) {
		var node = this.nodes[nid];
		if(node.errors.length > 0) {
			node.setErrors([]);
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
	//this.saveToStore(true);
	var store = this.getData();
	store.constraints = cloneObj(this.store.constraints);
	var newt = new Template(this.id, store, this.editor);
	return newt;
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


Template.prototype.transformEventCoordinates = function(x, y){
	this.pt.x = x; this.pt.y = y;
	return this.pt.matrixTransform(this.svg.node().getScreenCTM().inverse());
};

Template.prototype.transformSVGCoordinates = function(x, y){
	this.pt.x = x; this.pt.y = y;
	return this.pt.matrixTransform(this.svg.node().getScreenCTM());
};