function GraphItem(parent, id, config) {
	this.parent = parent;

	this.id = id;
	this.text = getLocalName(id);
	this.config = config;
	
	this.binding = null;
	this.dimensionality = 0;
	this.inactive = false;
	this.derivedFrom = null;
	
	this.textbounds = {x: 0, y: 0, width: 0, height: 0};
	this.bounds = {x: 0, y: 0, width: 0, height: 0}
	this.coords = {x: 0, y: 0}
	
	// D3 Objects
	this.item = null; // Main group
	this.textitem = null; // Text
	this.bgitem = null; // Background
	this.bgstack = null; // Background Stack
	
	this.inputPorts = {}; // Input ports
	this.outputPorts = {}; // Output ports
	
	this.create();
};

GraphItem.prototype.create = function() {
	this.item = this.parent.append('g').attr("id", this.id);
	this.bgstack = this.item.append('g');
	this.bgitem = this.createBackgroundItem(this.item);
	this.textitem = this.item.append('text');
};

GraphItem.prototype.configure = function() {
	this.textitem
		.attr("pointer-events", "none").style("text-anchor", "middle")
		.attr("dy", 0)
		.style("font-size", this.config.getFontsize() + "px")
		.style("font-family", this.config.getFont())
		.style("font-weight",this.config.getFontweight())
		.style("fill", this.config.getTextcolor());
	
	if(this.bgitem) {
		this.bgitem.attr("stroke", this.config.getStrokecolor())
			.attr("stroke-width", this.config.getStrokewidth())
			.attr("fill", this.config.getBgcolor());
		this.bgitem.attr("pointer-events", this.config.placeholder ? "none" : null);
	}
	
	for(var portid in this.inputPorts) {
		this.inputPorts[portid].config = this.config;
		this.inputPorts[portid].configure();
	}
	for(var portid in this.outputPorts) {
		this.outputPorts[portid].config = this.config;
		this.outputPorts[portid].configure();
	}
	
	if(this.skip)
		this.textitem.style("text-decoration", "line-through");
		
	if(this.inactive || this.skip)
		this.item.style("opacity", this.config.inactiveopacity);
	else
		this.item.style("opacity", 1);
};

// ** OVERRIDE in Subclases **
GraphItem.prototype.createBackgroundItem = function(parent) {
	//return parent.append('circle');
};
// ** OVERRIDE in Subclases **
GraphItem.prototype.drawBackgroundItem = function(bgitem, bounds) {
	//this.bgitem.x(bounds.x).y(bounds.y).rx(bounds.width/2).ry(bounds.width/2);
};
//** OVERRIDE in Subclases **
GraphItem.prototype.setDefaultColors = function() {
	//this.config.setBgcolor("red");
};

GraphItem.prototype.drawStack = function() {
	// Clear existing stack
	this.bgstack.node().innerHTML = '';
	
	// Draw a stack
	var dim = this.dimensionality;
	for (i = 0; i < dim; i++) {
		var stackitem = d3.select(this.bgitem.node().cloneNode());
		var spacing = (dim - i) * this.config.getStackspacing();
		var opacity = 1 - (dim - i) * 0.15;
		stackitem.style("opacity", opacity);
		stackitem.style("fill", this.config.getStackcolor());
		stackitem.attr("transform", "translate(" + spacing + "," + spacing + ")");
		this.bgstack.node().appendChild(stackitem.node());
	}	
};

GraphItem.prototype.drawPorts = function() {
	var inputPorts = this.getInputPorts();
	var outputPorts = this.getOutputPorts();
	
	var portsize = this.config.getPortsize();
	var portpad = this.config.getPortpad();

	var ipsize = inputPorts.length * (portsize + portpad) + portpad;
	var opsize = outputPorts.length * (portsize + portpad) + portpad;

	var w = this.bounds.width;
	var h = this.bounds.height;
	var x = this.bounds.x;
	var y = this.bounds.y;
	
	// Input ports
	var ipstart = portpad + (w - ipsize) / 2;
	for ( var i=0; i<inputPorts.length; i++) {
		var gport = inputPorts[i];
		var px = x + ipstart + i * (portsize + portpad) + portsize / 2;
		var py = y - portsize / 2;
		gport.setCoords({x: px, y: py});
		gport.draw();
	}

	// Output ports
	var opstart = portpad + (w - opsize) / 2;
	for ( var i=0; i<outputPorts.length; i++ ) {
		var gport = outputPorts[i];
		var px = x + opstart + i * (portsize + portpad) + portsize / 2;
		var py = y + h + portsize / 2;
		gport.setCoords({x: px, y: py});
		gport.draw();
	}
};

GraphItem.prototype.calculateBoundingBox = function() {
	// Get text bounding box
	var bbox = this.textitem.node().getBBox();
	// Calculate item's top-left corner from text size (to pass to
	// drawBackground)
	this.textbounds.x = bbox.x - this.config.getXpad();
	this.textbounds.y = bbox.y - this.config.getYpad();

	// Calculate item's width and height
	this.textbounds.width = bbox.width + this.config.getXpad() * 2;
	this.textbounds.height = bbox.height + this.config.getYpad() * 2;
	
	this.bounds = {
		x: this.textbounds.x,
		y: this.textbounds.y,
		width: this.textbounds.width,
		height: this.textbounds.height
	}
	// Check if ports can fit inside
	// - else increase width and fix top-left corner
	var pwidth = this.getPortsWidth();
	if (pwidth > this.bounds.width) {
		this.bounds.x = this.bounds.x - (pwidth - this.bounds.width) / 2;
		this.bounds.width = pwidth;
	}
};

GraphItem.prototype.draw = function(redraw) {
	this.configure();
	this.drawText();
	this.calculateBoundingBox();
	this.drawPorts();
	this.drawBackgroundItem(this.bgitem, this.bounds);
	this.drawStack();
	if(redraw) {
		// Draw links again if redrawing
		// - We don't do this on initial draw to avoid drawing the same link multiple times
		this.redrawLinks();
	}
};

GraphItem.prototype.redrawLinks = function() {
	if(this instanceof GraphVariable)
		for(var i=0; i<this.variableLinks.length; i++)
			this.variableLinks[i].draw();
	if(this instanceof GraphNode) {
		for(var i=0; i<this.inputLinks.length; i++)
			this.inputLinks[i].draw();
		for(var i=0; i<this.outputLinks.length; i++)
			this.outputLinks[i].draw();		
	}
};

GraphItem.prototype.getItem = function() {
	return this.item;
};

GraphItem.prototype.getBackground = function() {
	return this.bgitem;
};

GraphItem.prototype.getBackgroundStack = function() {
	return this.bgstack;
};

GraphItem.prototype.getTextItem = function() {
	return this.textitem;
};

GraphItem.prototype.getId = function() {
	return this.id;
};

GraphItem.prototype.setId = function(id) {
	this.id = id;
	this.item.attr("id", id);
};

GraphItem.prototype.getName = function() {
	return getLocalName(this.id);
};

GraphItem.prototype.getText = function() {
	return this.text;
}

GraphItem.prototype.setText = function(text, redraw) {
	this.text = text;
	if(redraw) {
		this.draw(true);
	}
}

GraphItem.prototype.drawText = function() {
	this.textitem.node().innerHTML = "";
	var lines = this.text.split(/\n/);
	for(var i=0; i<lines.length; i++) {
		if(!lines[i])
			continue;
		var fontsize = this.config.getFontsize()*(i > 0 ? 0.65 : 1); 
		var dysize = i == 1 ? fontsize*1.5 : fontsize;
		this.textitem.append('tspan').text(lines[i])
			.style("font-size", fontsize+"px")
			.attr("x", 0).attr("dy", dysize);
	}
}

GraphItem.prototype.getDimensionality = function() {
	return this.dimensionality;
}

GraphItem.prototype.setDimensionality = function(dimensionality) {
	this.dimensionality = dimensionality;
	if(this.bgitem != null)
		this.drawStack();
};

GraphItem.prototype.getX = function() {
	return this.coords.x;
}

GraphItem.prototype.setX = function(x) {
	this.setCoords({
		x : x,
		y : this.getY()
	});
}

GraphItem.prototype.getY = function() {
	return this.coords.y;
}

GraphItem.prototype.setY = function(y) {
	this.setCoords({
		x : this.getX(),
		y : y
	});
}

GraphItem.prototype.getCoords = function() {
	return this.coords;
}

GraphItem.prototype.setCoords = function(coords, animate) {
	if(coords.legacy)
		return this.setLegacyCoords(coords);
	
	if (coords.x < this.bounds.width/2 + 1)
		coords.x = this.bounds.width/2 + 1;
	if (coords.y < 11 + this.config.portsize/4) {
		coords.y = 11 + this.config.portsize/4;
	}
	if(animate)
		this.item.transition().attr("transform", "translate(" + coords.x + "," + coords.y + ")");
	else
		this.item.attr("transform", "translate(" + coords.x + "," + coords.y + ")");
	this.coords = coords;
}

GraphItem.prototype.setLegacyCoords = function(coords) {
	coords.x = coords.x + this.bounds.width / 2;
	coords.y = coords.y + this.bounds.height / 2;
	this.item.attr("transform", "translate(" + coords.x + "," + coords.y + ")");
	this.coords = coords;
}

GraphItem.prototype.getInputPorts = function() {
	var ports = [];
	var sortedIds = Object.keys(this.inputPorts).sort();
	for(var i=0; i<sortedIds.length; i++)
		ports.push(this.inputPorts[sortedIds[i]]);
	return ports;
}

GraphItem.prototype.setInputPorts = function(inputPorts) {
	// TODO: Check whether replacing the variable has any effect
	this.inputPorts = inputPorts;
}

GraphItem.prototype.addInputPort = function(portid, role) {
	this.inputPorts[portid] = new GraphPort(this.item, this, portid, role, true, this.config);
}

GraphItem.prototype.getOutputPorts = function() {
	var ports = [];
	var sortedIds = Object.keys(this.outputPorts).sort();
	for(var i=0; i<sortedIds.length; i++)
		ports.push(this.outputPorts[sortedIds[i]]);
	return ports;
}

GraphItem.prototype.setOutputPorts = function(outputPorts) {
	this.outputPorts = outputPorts;
}

GraphItem.prototype.addOutputPort = function(portid, role) {
	this.outputPorts[portid] = new GraphPort(this.item, this, portid, role, false, this.config);
}

GraphItem.prototype.getBounds = function() {
	return this.bounds;
};

GraphItem.prototype.getWidth = function() {
	return this.bounds.width;
}

GraphItem.prototype.getHeight = function() {
	return this.bounds.height;
}

// Override 
GraphItem.prototype.getBindingText = function(binding) {
	if(!binding)
		return this.binding;
	return binding;
};

GraphItem.prototype.getBinding = function() {
	return this.binding;
};

GraphItem.prototype.setBinding = function(binding, redraw) {
	this.binding = binding;
	this.setText(this.getBindingText(), redraw);
};

GraphItem.prototype.getConfig = function() {
	return this.config;
};

GraphItem.prototype.setConfig = function(config) {
	this.config = config;
};

GraphItem.prototype.setInactive = function(inactive) {
	this.inactive = inactive;
};

GraphItem.prototype.getPortsWidth = function() {
	var portsize = this.config.getPortsize();
	var portpad = this.config.getPortpad();
	var ipsize = Object.keys(this.inputPorts).length * (portsize + portpad) + portpad;
	var opsize = Object.keys(this.outputPorts).length * (portsize + portpad) + portpad;
	var psize = ipsize > opsize ? ipsize : opsize;
	return psize;
};

GraphItem.prototype.setPreview = function(operation) {
	this.preview = operation;
	if (operation == "remove") {
		if(!this.origconfig) {
			this.origconfig = this.config;
			this.config = new GraphPreviewVariableConfig();
			this.config.placeholder = false;
			this.configure();
		}
	}
	else if(this.origconfig) {
		this.config = this.origconfig;
		this.origconfig = null;
		this.configure();
	}
};
