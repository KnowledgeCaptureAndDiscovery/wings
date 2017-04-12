function GraphNode(parent, nodedata, config) {
	GraphItem.call(this, parent, nodedata.id, config);
	
	// GraphNode specific defaults
	this.isConcrete = false;
	this.crule = {type:'WTYPE'};
	this.prule = {type:'STYPE', expr:{'op' : 'XPRODUCT', args: []}};
	this.inputLinks = [];
	this.outputLinks = [];
	this.machineIds = [];
	this.runtimeInfo = {};
	this.componentid = null;
	
	this.errors = [];
	
	// Override defaults with node data
	this.processData(nodedata);
	
	// Add node ports
	var ports = nodedata.inputPorts;
	for(var portid in ports) {
		var port = ports[portid];
		this.addInputPort(port.id, port.role);
	}
	ports = nodedata.outputPorts;
	for(var portid in ports) {
		var port = ports[portid];
		this.addOutputPort(port.id, port.role);
	}
};

GraphNode.prototype = Object.create(GraphItem.prototype);
GraphNode.prototype.constructor = GraphNode;

GraphNode.prototype.createBackgroundItem = function(parent) {
	return parent.insert('polygon');
};

GraphNode.prototype.drawBackgroundItem = function(bgitem, bounds) {
	// Parallelogram
	var xshift = this.config.xshift;
	return bgitem
		.attr("points", 
			(bounds.x + xshift) + "," + bounds.y + " " +
			(bounds.x + bounds.width) + "," + bounds.y + " " +
			(bounds.x + bounds.width - xshift) + "," 
				+ (bounds.y + bounds.height) + " " +
			(bounds.x + "," + (bounds.y + bounds.height))
		);
};

GraphNode.prototype.processData = function(data) {
	this.isConcrete = data.componentVariable.isConcrete ? true : false;
	if(data.machineIds)
		this.machineIds = data.machineIds;
	if(data.crule)
		this.crule = data.crule;
	if(data.prule)
		this.prule = data.prule;
	this.componentid = data.componentVariable.id; 
	this.inactive = data.inactive;
	this.derivedFrom = data.derivedFrom;
	this.setBinding(data.componentVariable.binding);
	this.setDefaultColors();
};

GraphNode.prototype.getBindingText = function(binding) {
	if(!binding)
		binding = this.binding;
	if(!binding)
		return null;
	
	var sfx = this.getMachinesText();
	if(sfx) 
		sfx = "\n[Run on " + sfx + "]";
	
	var text = "";
	if (!binding.length)
		text = getLocalName(binding.id);
	
	for ( var i = 0; i < binding.length; i++) {
		if(i > 0) text += ",";
		text += this.getBindingText(binding[i]);
	}
	if(this.crule.type == 'STYPE')
		text = "{" + text + "}";
	return text + sfx;
};

GraphNode.prototype.getMachinesText = function() {
	if(this.inactive)
		return "";
	if(this.machineIds.length == 1)
		return getLocalName(this.machineIds[0]);
	else if(this.machineIds.length > 1)
		return "1 of " + this.machineIds.length + " machines";
	else
		return "";
};

GraphNode.prototype.setBinding = function(binding) {
	this.binding = binding;
	this.setText(this.getBindingText(binding));
};

GraphNode.prototype.setConcrete = function(isConcrete) {
	this.isConcrete = isConcrete;
	this.setDefaultColors();
	this.configure();
};

GraphNode.prototype.setComponentRule = function(crule) {
	if(!crule) return;
	this.crule = crule;
	this.setText(this.getBindingText(this.binding));
};

GraphNode.prototype.setPortRule = function(prule) {
	if(!prule) return;
	this.prule = prule;
};

GraphNode.prototype.setConcrete = function(isConcrete) {
	this.isConcrete = isConcrete;
	this.setDefaultColors();
	this.configure();
};

GraphNode.prototype.setErrors = function(errors) {
	this.errors = errors;
	this.setDefaultColors();
	this.configure();
};

GraphNode.prototype.setDefaultColors = function() {
	if (this.isConcrete)
		this.config.setBgcolor("rgba(255,204,153,1)");
	else
		this.config.setBgcolor("rgba(204,204,204,1)");
	
	if(this.runtimeInfo && this.runtimeInfo.status) {
		if(this.runtimeInfo.status == "SUCCESS") {
			this.config.setStrokecolor("rgba(0,200,0,1)");
			this.config.setTextcolor("rgba(0,160,0,1)");
		}
		else if(this.runtimeInfo.status == "FAILURE") {
			this.config.setStrokecolor("rgba(200,0,0,1)");
			this.config.setTextcolor("rgba(160,0,0,1)");
		}
		this.config.setStrokewidth(2);
	}
	if(this.errors.length > 0)
		this.config.setTextcolor("rgba(255,0,0,1)");
	else
		this.config.setTextcolor("rgba(72,42,3,1)");
	
	this.config.stackcolor = this.config.getBgcolor();
};

GraphNode.prototype.setRuntimeInfo = function(runtimeInfo) {
	this.runtimeInfo = runtimeInfo;
	this.setDefaultColors();
	this.configure();
};

GraphNode.prototype.equals = function(n) {
	if (!n)
		return false;
	// if(this.id ! n.id) return false;
	if (this.component != n.component)
		return false;
	if (this.prule.type != n.prule.type)
		return false;
	if (this.crule.type != n.crule.type)
		return false;
	if (this.prule.expr.op != n.prule.expr.op)
		return false;
	return true;
};

GraphNode.prototype.addInputLink = function(link) {
	this.inputLinks.push(link);
};

GraphNode.prototype.addOutputLink = function(link) {
	this.outputLinks.push(link);
};

GraphNode.prototype.removeInputLink = function(link) {
	var index = this.inputLinks.indexOf(link);
	if (index >= 0)
		return this.inputLinks.splice(index, 1);
};

GraphNode.prototype.removeOutputLink = function(link) {
	var index = this.outputLinks.indexOf(link);
	if (index >= 0)
		return this.outputLinks.splice(index, 1);
};

GraphNode.prototype.getInputPortByName = function(name) {
	for(var portid in this.inputPorts) {
		var port = this.inputPorts[portid];
		if(port.role && port.role.roleid == name)
			return port;
	}
	return null;
};

GraphNode.prototype.getOutputPortByName = function(name) {
	for(var portid in this.outputPorts) {
		var port = this.outputPorts[portid];
		if(port.role && port.role.roleid == name)
			return port;
	}
	return null;
};

GraphNode.prototype.getInputLinks = function() {
	return this.inputLinks;
};

GraphNode.prototype.getOutputLinks = function() {
	return this.outputLinks;
};

GraphNode.prototype.getLinks = function() {
	return this.inputLinks.concat(this.outputLinks);
};