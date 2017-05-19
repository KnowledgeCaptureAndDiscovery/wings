function TemplateEvents(template) {
	this.template = template;
	this.editable = false;
	
	this.dispatch = d3.dispatch("select", "link");
	
	this.selections = {};
	this.validports = {};
	
	this.fromport = null;
	this.toport = null;

	this.tmplink = null;
	this.tmpvariable = null;
	this.selectbox = null;
};

TemplateEvents.prototype.initialize = function() {
	this.enableSelectEvent();
	this.enableMoveEvent();
	this.setEditable(this.editable);
};

TemplateEvents.prototype.setEditable = function(editable) {
	this.editable = editable;
	if(this.editable)
		this.enableLinkEvent();
	/*else
		this.disableLinkEvent();*/
};

TemplateEvents.prototype.enableSelectEvent = function() {
	var gitems = Object.assign({}, this.template.nodes, this.template.variables);
	var me = this;
	for(var id in gitems) {
		var gitem = gitems[id];
		this.enableSelectEventForItem(gitem);
	}
	var canvas = d3.select(this.template.domnode);
	canvas.on("mousedown", function() {
		me.deselectAllItems();
		me.dispatch.call("select", this, me.selections);
	});
	
	if(!this.selectbox)
		this.selectbox =  me.template.svg.append("rect")
			.attr("fill", "rgba(0,0,0,0.2)")
			.attr("border", "rgba(0,0,0,0.2)");
	
	var drag = d3.drag()
	.on("start", function(d) {
		var xy = d3.mouse(canvas.node());
		var x = xy[0]/me.template.scale;
		var y = xy[1]/me.template.scale;
		me.selectbox.startx = x;
		me.selectbox.starty = y;
		me.selectbox.attr("x", x).attr("y", y).attr("width", 0).attr("height", 0);
		me.selectbox.style("display", "");
	})
	.on("drag", function(d) {
		var xy = d3.mouse(canvas.node());
		var x = xy[0]/me.template.scale;
		var y = xy[1]/me.template.scale;
		var w = x - me.selectbox.startx;
		var h = y - me.selectbox.starty;
		if(w > 0 && h > 0)
			me.selectbox.attr("width", w).attr("height", h);
		
		var gitems = Object.assign({}, me.template.nodes, me.template.variables);
		for(var id in gitems) {
			var gitem = gitems[id];
			var b = gitem.getBounds();
			var c = gitem.getCoords();
			
			var x1 = c.x + b.x;
			var x2 = c.x + b.x + b.width;
			var y1 = c.y + b.y;
			var y2 = c.y + b.y + b.height;
			
			if(x1 > me.selectbox.startx && x2 < x &&
					y1 > me.selectbox.starty && y2 < y) {
				me.selectItem(gitem);
			}
			else if(me.selections[id]) {
				me.deselectItem(gitem);
			}
		}
	})
	.on("end", function() {
		me.selectbox.style("display", "none");
	});
	
	canvas.call(drag);
};

TemplateEvents.prototype.enableMoveEvent = function() {
	var gitems = Object.assign({}, this.template.nodes, this.template.variables);
	for(var id in gitems) {
		var gitem = gitems[id];
		this.enableMoveEventForItem(gitem);
	}
};

TemplateEvents.prototype.enableEventsForItem = function(gitem) {
	this.enableSelectEventForItem(gitem);
	this.enableMoveEventForItem(gitem);
	if(this.editable)
		this.enableLinkEventForItem(gitem);
};

TemplateEvents.prototype.enableSelectEventForItem = function(gitem) {
	var me = this;
	gitem.getItem().style("cursor", "default");
	gitem.getItem().on("mousedown", function() {
		if(!me.selections[gitem.id]) {
			me.deselectAllItems();
			me.selectItem(gitem);
			me.dispatch.call("select", this, me.selections);
		}
		d3.event.stopPropagation();
	})
	.on("dblclick", function() {
		if (gitem.inputLinks) {
			for(var i=0; i<gitem.inputLinks.length; i++)
				me.selectItem(gitem.inputLinks[i].variable)
		}
		if(gitem.outputLinks) {
			for(var i=0; i<gitem.outputLinks.length; i++)
				me.selectItem(gitem.outputLinks[i].variable)
		}
		me.dispatch.call("select", this, me.selections);
		d3.event.stopPropagation();
	});	
};

TemplateEvents.prototype.getSelections = function() {
	return Object.keys(this.selections);
};

TemplateEvents.prototype.selectItem = function(gitem) {
	this.selections[gitem.id] = gitem; 
	gitem.config.setBgcolor("yellow");
	//gitem.config.setStackcolor("yellow");
	gitem.config.setTextcolor("black");
	gitem.configure();	
};

TemplateEvents.prototype.deselectItem = function(gitem) {
	delete this.selections[gitem.id];
	gitem.setDefaultColors();
	gitem.configure();	
};

TemplateEvents.prototype.deselectAllItems = function() {
	// Deselect all earlier selections
	for(var selid in this.selections) {
		var gitem = this.selections[selid];
		gitem.setDefaultColors();
		gitem.configure();
	}
	this.selections = {};
};


TemplateEvents.prototype.enableMoveEventForItem = function(gitem) {
	var me = this;
	var drag = d3.drag().subject(function(d) {
		return {
			x : 0,
			y : 0
		};
	}).on("start", function(d) {
		for(var id in me.selections) {
			var selitem = me.selections[id];
			selitem.dragstart = {x: selitem.coords.x, y: selitem.coords.y }
		}
	}).on("drag", function(d) {
		var dx = d3.event.x;
		var dy = d3.event.y;
		gitem.getItem().style("cursor", "move");
		var links = {};
		for(var id in me.selections) {
			var selitem = me.selections[id];
			selitem.setCoords({
				x: selitem.dragstart.x + dx, 
				y: selitem.dragstart.y + dy
			});
		}
		var linksToRedraw = {};
		for ( var lid in me.template.links) {
			var gl = me.template.links[lid];
			// Redraw only relevant links while dragging
			if ((gl.fromNode != null && me.selections[gl.fromNode.id])
					|| (gl.toNode != null && me.selections[gl.toNode.id])
					|| (gl.variable != null && me.selections[gl.variable.id])) {
				linksToRedraw[gl.id] = gl;
			}
		}
		for(var lid in linksToRedraw)
			linksToRedraw[lid].draw();
		
		me.template.calculateGraphSizeAfterMove(me.selections);
		me.template.setViewport();
		me.template.resizeSVG();
		
	}).on("end", function() {
		// Redraw all links after dragging ended
		gitem.getItem().style("cursor", "default");
		me.template.drawLinks();
		for(var id in me.selections) {
			var selitem = me.selections[id];
			delete selitem.dragstart;
		}
	});
	gitem.getItem().call(drag);	
};

TemplateEvents.prototype.enableLinkEvent = function() {
	var gitems = Object.assign({}, this.template.nodes, this.template.variables);
	var me = this;
	for(var id in gitems) {
		var gitem = gitems[id];
		this.enableLinkEventForItem(gitem);
	}
};

TemplateEvents.prototype.enableLinkEventForItem = function(gitem) {
	var ports = gitem.getInputPorts().concat(gitem.getOutputPorts());
	for(var i=0; i<ports.length; i++) {
		var port = ports[i];
		this.enableEventsForPort(port);
	}
};

TemplateEvents.prototype.enableEventsForPort = function(port) {
	var me = this;
	port.item.style("cursor", "pointer");
	port.item.on("mouseover", function() {
		if(me.validports[port.id]) {
			me.toport = port;
			
			// Show link addition preview
			me.linkDragIntoPort();

			// This is a valid port to connect
			port.item.attr("fill", "yellow");
		}
		else if(!me.fromport) {
			// No dragging - normal behaviour
			port.item.attr("r", port.config.getPortsize());
		}
		me.showTooltip(port);
	});
	port.item.on("mouseout", function() {
		var size = port.config.getPortsize()/2;
		if(me.validports[port.id]) {
			me.toport = null;
			
			// Show link removal preview
			me.linkDragOutOfPort();
			
			for(var pid in me.validports) {
				// Moving out of a valid port
				me.validports[pid].item.attr("fill", "red");
			}
		}
		else if(!me.fromport) { 
			// No dragging - normal behaviour
			port.item.attr("r", port.config.getPortsize()/2);
		}
		// Hide tooltip
		me.template.hideTooltip();
	});

	var drag = d3.drag().subject(function(d) {
		return { x : 0, y : 0 };
	}).on("drag", function() {
		if(!me.fromport) {
			me.fromport = port;
			me.highlightCompatiblePorts();
			me.startLinkDrag();
		}
		else {
			me.linkDrag();
		}
	}).on("end", function() {
		if(me.fromport) {
			me.endLinkDrag();
			
			// Reset port attributes
			for(var pid in me.validports) {
				var oport = me.validports[pid];
				oport.configure();
				oport.draw();
			}
			me.fromport.configure();
			me.fromport.draw();
			
			// Reset state
			me.validports = {};
			me.fromport = null;
			me.toport = null;
		}
	});
	port.item.call(drag);  		
};

TemplateEvents.prototype.initializeDraggerItems = function() {
	if(!this.tmplink) {
		this.tmplink = new GraphLink(this.template.graph, "__dragger_link",
				null, null, null, null, null, 
				[], //this.template.graphItems, 
				new GraphLinkConfig());
	}
	
	if (!this.tmpvariable) {
		this.tmpvariable = new GraphVariable(this.template.graph, 
				{id: 'dummy'}, new GraphVariableConfig());
		this.tmpvariable.isPlaceholder = true;
		this.tmpvariable.config = new GraphPreviewVariableConfig();
		this.tmpvariable.configure();
	}
	else
		this.tmpvariable.item.style("display", "");
	
	if(!this.tmpport) {
		// Create a dummy port
		this.tmpport = {
			coords : {x: 0, y: 0},
			graphItem : {},
			setCoords : function(coords) {
				this.coords = coords;
			},
			getRealCoords : function() {
				return this.coords;
			}
		};
	}
};

TemplateEvents.prototype.drawTemporaryLink = function() {
	this.tmplink.clear();
	
	var fromport = null;
	var toport = null;
	var variable = null;
	// If we Start dragging from a node
	if(this.fromport.graphItem instanceof GraphNode) {
		fromport = this.fromport;
		variable = this.tmpvariable;
		if(this.toport) {
			if(this.toport.graphItem instanceof GraphNode) {
				toport = this.toport;
			}
			else {
				variable = this.toport.graphItem;
			}
		}
	}
	// If we Start dragging from a Variable
	else {
		variable = this.fromport.graphItem;
		fromport = this.tmpport;
		fromport.isInput = !this.fromport.isInput;
	}

	// Invert link if dragging from an input port
	if(fromport.isInput) {
		var tmp = fromport;
		fromport = toport;
		toport = tmp;
	}
	var fromitem = fromport ? fromport.graphItem : null;
	var toitem = toport ? toport.graphItem : null;
	
	// Draw link
	this.tmplink.fromPort = fromport;
	this.tmplink.fromNode = fromitem;
	this.tmplink.toPort = toport;
	this.tmplink.toNode = toitem;
	this.tmplink.variable = variable;
	this.tmplink.draw();
};

TemplateEvents.prototype.startLinkDrag = function() {
	this.initializeDraggerItems();
	
	var pos = this.fromport.getRealCoords();
	
	var variable = null;
	if (this.fromport.graphItem instanceof GraphNode) {
		// Set dragger item details
		var varid = this.template.getFreeVariableId(this.fromport.role.roleid);
		var varname = getLocalName(varid);	
		this.tmpvariable.setId(varid);
		this.tmpvariable.setText(varname, true);
		
		var dims = this.tmpvariable.getBounds()
		if(this.fromport.isInput)
			pos.y = pos.y - dims.height/2;
		else
			pos.y = pos.y + dims.height/2 + this.tmpvariable.config.portsize;
		
		this.tmpvariable.setCoords(pos);
		this.tmpvariable.getItem().style("display", "");
		variable = this.tmpvariable;
	}
	else {
		this.tmpport.setCoords(pos);
	}
	this.drawTemporaryLink();
	this.template.markLinkAdditionPreview(this.fromport, this.toport, variable);
};

TemplateEvents.prototype.linkDragIntoPort = function() {
	if(!this.fromport || !this.toport)
		return;
	
	var variable = null;
	
	// Link dragged from a node to a node
	if((this.fromport.graphItem instanceof GraphNode) &&
			(this.toport.graphItem instanceof GraphNode)) {
		var startpoint = this.fromport.getRealCoords();
		var endpoint = this.toport.getRealCoords();
		var x = Math.round(startpoint.x + (endpoint.x - startpoint.x)/2);
		var y = Math.round(startpoint.y + (endpoint.y - startpoint.y)/2);
		this.tmpvariable.setCoords({x: x, y: y});
		variable = this.tmpvariable;
	}
	else {
		variable = null;
		if(this.tmpvariable)
			this.tmpvariable.item.style("display", "none");
	}
	this.drawTemporaryLink();
	this.template.markLinkAdditionPreview(this.fromport, this.toport, variable);
};

TemplateEvents.prototype.linkDragOutOfPort = function() {
	this.toport = null;
	
	var variable = null;
	// Dragged from Node
	if(this.fromport.graphItem instanceof GraphNode) {
		this.tmpvariable.item.style("display", "");
		variable = this.tmpvariable;
	}
	this.drawTemporaryLink();
	this.template.markLinkAdditionPreview(this.fromport, this.toport, variable);
};

TemplateEvents.prototype.linkDrag = function() {
	// If the toport is set, that should be handled by linkDragIntoPort
	if(this.toport) 
		return;
	
	// Get mouse coordinates
	var xy = d3.mouse(this.template.svg.node());
	var x = xy[0];
	var y = xy[1];

	// Set coordinates of the temporary variable if dragged from a node
	if (this.fromport.graphItem instanceof GraphNode) {
		var dims = this.tmpvariable.getBounds();
		if(this.fromport.isInput)
			y = y - dims.height/2;
		else
			y = y + dims.height/2 + this.tmpvariable.config.portsize;
		
		this.tmpvariable.setCoords({x: x, y: y});	
		this.template.calculateGraphSizeAfterMove([this.tmpvariable]);
		this.template.setViewport();
		this.template.resizeSVG();
	}
	// Set coordinates of temporary port if dragged from a variable
	else {
		this.tmpport.setCoords({x: x, y: y + this.tmpvariable.config.portsize/2});
	}
	// Draw the temporary link
	this.drawTemporaryLink();
};

TemplateEvents.prototype.endLinkDrag = function() {
	this.template.clearPreviews();
	// Add Link
	var link = null;
	if (this.fromport.graphItem instanceof GraphNode)
		link = this.template.addLinkInCanvas(this.fromport, this.toport, this.tmpvariable);
	else
		link = this.template.addLinkInCanvas(this.fromport, this.toport, null);
	
	if(link) {
		// Refresh Graph
		this.template.editor.updateGridVariables();
		this.template.editor.refreshConstraints();
		this.template.drawLinks();
		this.template.markErrors();
		this.template.forwardSweep();
	}
	
	// Reset variables
	if(this.tmpvariable)
		this.tmpvariable.item.style("display", "none");
	if(this.tmplink) {
		this.tmplink.item.remove();
		this.tmplink = null;
	}
};

TemplateEvents.prototype.highlightCompatiblePorts = function() {
	var reasoner = this.template.editor.browser.reasoner;
	// Highlight compatible ports
	this.fromport.item.attr("fill", "yellow");
	var items = Object.assign({}, this.template.nodes, this.template.variables);
	for(var id in items) {
		var ports = Object.assign({}, items[id].inputPorts, items[id].outputPorts);
		for(var pid in ports) {
			var oport = ports[pid];
			if(reasoner.portsCompatible(this.fromport, oport)) {
				this.validports[pid] = oport;
				oport.item
					.attr("r", oport.config.getPortsize())
					.attr("fill", "red");
			}
		}
	}
};

TemplateEvents.prototype.showTooltip = function(port) {
	var c = port.getRealCoords();
	var coords = this.template.transformSVGCoordinates(c.x, c.y);
	
	var message = "";
	if (port.graphItem instanceof GraphVariable) {
		if (this.fromport) {
			// If dragging, no tooltip on a graph variable
		}
		else if (port.isInput)
			message = "Drag from a Component Output port onto this port to create a link";
		else
			message = "Drag from here onto a Component Input port to create a link";
	}
	else {
		var portInfo1 = "<br/>Drag from here to a Variable Output Port, " +
				"Component Output Port, or drop it in empty space to create new Input Link";
		var portInfo2 = "<br/>Drag from here to a Variable Input Port, " +
				"Component Input Port, or drop it in empty space to create new Output Link";
		if (this.fromport) // If dragging,
			message = "" + port.role.roleid;
		else if (port.isInput)
			message = "<b>Input Port: " + port.role.roleid + "</b>" + portInfo1;
		else
			message = "<b>Output Port: " + port.role.roleid + "</b>" + portInfo2;
	}
	if(message)
		this.template.showTooltip(message, coords.x, coords.y);
};
