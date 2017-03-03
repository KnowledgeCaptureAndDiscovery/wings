var GraphLink = function(parent, id, 
		fromNode, fromPort, toNode, toPort, variable, 
		graphItems, config) {
	this.parent = parent;
	
	this.id = id;
	this.fromNode = fromNode;
	this.fromPort = fromPort;
	this.toNode = toNode;
	this.toPort = toPort;
	this.variable = variable;
	if(!this.id)
		this.id = this.getLinkId();
	
	if(this.fromNode)
		this.fromNode.outputLinks.push(this);
	if(this.toNode)
		this.toNode.inputLinks.push(this);
	
	if(this.variable) {
		if(!this.fromNode)
			this.variable.setIsInput(true)
		else if(!this.toNode)
			this.variable.setIsOutput(true);
		this.variable.variableLinks.push(this);
	}
	
	this.config = config;
	this.inactive = false;
	
	this.graphItems = graphItems; // Graph node & variable items (used to find non-intersecting paths)
	
	// D3 Objects
	this.item = null;
	// D3 Link paths
	this.pathToVariable = null;
	this.pathFromVariable = null;
	// D3 Line interpolation
	this.interpolation = null;

	this.create();
};

GraphLink.prototype.create = function() {
	this.item = this.parent.insert("g", "g").attr("id", this.id);
	
	this.pathToVariable = this.item.append("path")
		.attr("stroke", this.config.strokecolor)
		.attr("fill", "none")
		.attr("stroke-width", this.config.strokewidth)
		.attr("pointer-events", "none")
		.attr("marker-end","url(#arrow)");
	
	this.pathFromVariable = this.item.append("path")
		.attr("stroke", this.config.strokecolor)
		.attr("fill", "none")
		.attr("stroke-width", this.config.strokewidth)
		.attr("pointer-events", "none")
		.attr("marker-end","url(#arrow)");
	
	this.interpolation = d3.line()
		.x(function(d) {return d.x;})
		.y(function(d) {return d.y;})
		.curve(this.config.interpolation);
}

GraphLink.prototype.clear = function() {
	this.pathToVariable.attr("d", null);
	this.pathFromVariable.attr("d", null);
};

GraphLink.prototype.setId = function(id) {
	this.id = id;
	if(this.item)
		this.item.attr("id", this.id);
};

GraphLink.prototype.getName = function() {
	return getLocalName(this.id);
};

GraphLink.prototype.getDescription = function() {
	return getLocalName(this.id) 
		+ (this.variable ? " (" + this.variable.getName() + ")" : ""); 
};

GraphLink.prototype.setInactive = function(inactive) {
	this.inactive = inactive;
	this.variable.setInactive(inactive);
};

GraphLink.prototype.configure = function() {
	if(this.inactive)
		this.item.style("opacity", this.config.inactiveopacity);
	else {
		if(this.variable && this.variable.autofill)
			this.item.style("opacity", this.config.autofillopacity);
		else
			this.item.style("opacity", 1);
	}
};

GraphLink.prototype.draw = function(animate) {
	// Draw link from node to variable
	this.configure();
	this.drawLinkToVariable(animate);
	this.drawLinkFromVariable(animate);
};

GraphLink.prototype.drawLinkToVariable = function(animate) {
	if(this.fromNode && this.fromPort && this.variable) {
		var vport = this.variable.inputPorts[Object.keys(this.variable.inputPorts)[0]];
		var start = this.fromPort.getRealCoords();
		var end = vport.getRealCoords();
		var coords = this.getPathCoordinates(start, end);
		this.drawPartialLink(this.pathToVariable, coords, animate);
	}	
};

GraphLink.prototype.drawLinkFromVariable = function(animate) {
	if(this.toNode && this.toPort && this.variable) {
		var vport = this.variable.outputPorts[Object.keys(this.variable.outputPorts)[0]];
		var start = vport.getRealCoords();
		var end = this.toPort.getRealCoords();		
		var coords = this.getPathCoordinates(start, end);
		this.drawPartialLink(this.pathFromVariable, coords, animate);
	}
};

GraphLink.prototype.getPathCoordinates = function(start, end) {
	var coords = [];
	var c1 = {x: start.x, y: start.y + this.config.portsize/2};
	var c2 = {x: start.x, y: start.y + this.config.portsize/2 + this.config.linkstartpad};
	var e2 = {x: end.x, y: end.y - this.config.portsize/2 - this.config.linkstartpad};
	var e1 = {x: end.x, y: end.y - this.config.portsize/2};
	
	coords.push(c1);	
	//coords.push(c2);
	
	var segments = this.getLineSegments(c1, e1);
	for(var i=0; i<segments.length; i++) {
		coords.push(segments[i]);		
	}

	//coords.push(e2);
	coords.push(e1);
	
	return coords;
};

GraphLink.prototype.drawPartialLink = function(path, coords, animate) {
	if(animate)
		path.transition().attr("d", this.interpolation(coords));
	else
		path.attr("d", this.interpolation(coords));
};

GraphLink.prototype.getLineSegments = function(start, end, index) {
	var segments = [];
	if(!index)
		index = 0;
	
	// TODO: use reverseGraphItems if(start.y > end.y)
	for(var i=index; i<this.graphItems.length; i++) {
		var gitem = this.graphItems[i];
		var coords = gitem.getCoords();
		var bounds = gitem.getBounds();
		
		var pad = this.config.intersectionpad;
		var dimpad = gitem.config.getStackspacing() * gitem.getDimensionality();
		
		var nwidth = bounds.width + pad*2 + dimpad;
		var nheight = bounds.height + dimpad;
		var nx = coords.x + bounds.x - pad;
		var ny = coords.y + bounds.y;

		// If starting point within the box (in case of higher dimensions)
		if(start.x > nx && start.x < nx + nwidth &&
				start.y > ny && start.y < ny + nheight) {
			nheight -= dimpad;
		}
		
		var intersection = this.lineIntersects(start.x, start.y,
				end.x, end.y, nx, ny, nx + nwidth, ny + nheight);

		if (intersection != null) {
			var corners = [];
			corners[0] = {x: nx, y: ny}; // tl
			corners[1] = {x: nx + nwidth, y: ny}; // tr
			corners[2] = {x: nx, y: ny + nheight}; // bl
			corners[3] = {x: nx + nwidth, y: ny + nheight}; // br

			var stpt = this.findClosestCorner(corners, intersection[0]);
			var endpt = this.findClosestCorner(corners, intersection[1]);

			if(stpt != endpt) {
				if (stpt == corners[0] && endpt == corners[3]) {
					segments.push(corners[1]);
				}
				else if (stpt == corners[1] && endpt == corners[2]) {
					segments.push(corners[0]);
				}
				else if (stpt == corners[3] && endpt == corners[0]) {
					segments.push(corners[2]);
				}
				else if (stpt == corners[2] && endpt == corners[1]) {
					segments.push(corners[3]);
				}
				else {
					segments.push(stpt);
				}
			}
			segments.push(endpt);

			/*for(var j=i; j<this.graphItems.length; j++) {
				var nitem = this.graphItems[j];
				if(nitem.getY() > gitem.getY())
					break;
			}*/
			// Get further segments
			var mpoints = this.getLineSegments(endpt, end, i+1);
			
			for (var k=0; k<mpoints.length; k++)
				segments.push(mpoints[k]);

			break;
		}
	}
	return segments;
};

GraphLink.prototype.findClosestCorner = function(corners, point) {
	var current = null;
	var max = 99999;
	for (var j = 0; j < corners.length; j++) {
		var c = corners[j];
		var diff = Math.abs(point.x - c.x) + Math.abs(point.y - c.y);
		if (diff < max) {
			max = diff;
			current = c;
		}
	}
	return current;
};

GraphLink.prototype.lineIntersects = function(x1, y1, x2, y2,
		xmin, ymin, xmax, ymax) {
	var u1 = 0.0;
	var u2 = 1.0;
	var r;
	var deltaX = (x2 - x1);
	var deltaY = (y2 - y1);
	/*
	 * left edge, right edge, bottom edge and top edge checking
	 */
	var pPart = [ -1 * deltaX, deltaX, -1 * deltaY, deltaY ];
	var qPart = [ x1 - xmin, xmax - x1, y1 - ymin, ymax - y1 ];

	var accept = true;
	for (var i = 0; i < 4; i++) {
		var p = pPart[i];
		var q = qPart[i];
		if (p == 0 && q < 0) {
			accept = false;
			break;
		}
		r = q / p;
		if (p < 0)
			u1 = Math.max(u1, r);
		if (p > 0)
			u2 = Math.min(u2, r);

		if (u1 > u2) {
			accept = false;
			break;
		}
	}
	if (accept) {
		if (u2 < 1) {
			x2 = x1 + u2 * deltaX;
			y2 = y1 + u2 * deltaY;
		}
		if (u1 > 0) {
			x1 = x1 + u1 * deltaX;
			y1 = y1 + u1 * deltaY;
		}
		return [ {x:x1, y:y1}, {x:x2, y:y2} ];
	} else {
		return null;
	}
};

GraphLink.prototype.getLinkId = function() {
	var id = "";
	if(this.fromPort) {
		id = this.fromPort.id;
		if(this.toPort)
			id += "_to_" + this.toPort.getName();
		else
			id += "_output";
	}
	else if(this.toPort) {
		id = this.toPort.id + "_input";
	}
	return id;
};

GraphLink.prototype.setFromPort = function(fromPort) {
	// Alter existing fromPort (if any)
	if (this.fromPort)
		this.fromNode.removeOutputLink(this);

	this.fromNode = fromPort ? fromPort.graphItem : null;
	this.fromPort = fromPort;
	if (this.fromNode)
		this.fromNode.addOutputLink(this);

	this.setId(this.getLinkId());
};

GraphLink.prototype.setToPort = function(toPort) {
	// Alter existing toPort (if any)
	if (this.toPort)
		this.toNode.removeInputLink(this);

	this.toNode = toPort ? toPort.graphItem : null;
	this.toPort = toPort;
	if (this.toNode)
		this.toNode.addInputLink(this);

	this.setId(this.getLinkId());
};

GraphLink.prototype.equals = function(l) {
	if (!l)
		return false;

	if (!this.fromNode && l.fromNode)
		return false;
	if (!this.toNode && l.toNode)
		return false;
	if (!this.fromPort && l.fromPort)
		return false;
	if (!this.toPort && l.toPort)
		return false;
	if (!this.variable && l.variable)
		return false;

	if (this.fromNode && !this.fromNode.equals(l.fromNode))
		return false;
	if (this.toNode && !this.toNode.equals(l.toNode))
		return false;
	if (this.fromPort && !this.fromPort.equals(l.fromPort))
		return false;
	if (this.toPort && !this.toPort.equals(l.toPort))
		return false;
	if (this.variable && !this.variable.equals(l.variable))
		return false;

	return true;
};

GraphLink.prototype.setPreview = function(operation) {
	this.preview = operation;
	if(!operation) {
		this.item.style("opacity", 1);
		this.pathToVariable.style("opacity", 1);
		this.pathFromVariable.style("opacity", 1);	
	}
	else if (operation == "remove") {
		this.item.style("opacity", 0.2);
		this.pathToVariable.style("opacity", 1);
		this.pathFromVariable.style("opacity", 1);
		if(this.variable.variableLinks.length == 1)
			this.variable.setPreview("remove");
	}
	else if (operation == "changeFromPort")
		this.pathToVariable.style("opacity", 0.2);
	else if (operation == "changeToPort")
		this.pathFromVariable.style("opacity", 0.2);
};