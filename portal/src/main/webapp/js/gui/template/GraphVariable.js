function GraphVariable(parent, vardata, config) {
	GraphItem.call(this, parent, vardata.id, config);
	
	// Variable specific defaults
	this.isInput = false;
	this.isOutput = false;
	this.isParam = false;
	this.autofill = false;
	this.unknown = false;
	this.breakpoint = false;
	
	this.variableLinks = [];
	
	// Override defaults with vardata
	this.processData(vardata);
	
	// Add variable "ports"
	var roletype = this.isParam ? 2 : 1;
	this.addInputPort(this.id+"_ip", {id: this.id+"_ip_role", type: roletype});
	this.addOutputPort(this.id+"_op", {id: this.id+"_op_role", type: roletype});
};

GraphVariable.prototype = Object.create(GraphItem.prototype);
GraphVariable.prototype.constructor = GraphVariable;

GraphVariable.prototype.createBackgroundItem = function(parent) {
	return parent.insert('rect');
};

GraphVariable.prototype.drawBackgroundItem = function(bgitem, bounds) {
	// Rounded rectangle
	bgitem
		.attr("x", bounds.x)
		.attr("y", bounds.y)
		.attr("rx", this.config.radius)
		.attr("ry", this.config.radius)
		.attr("width", bounds.width)
		.attr("height", bounds.height);
};


GraphVariable.prototype.processData = function(data) {
	this.autofill = data.autofill;
	this.isParam = (data.type == 2);
	this.breakpoint = data.breakpoint;
	this.unknown = data.unknown;
	this.inactive = data.inactive;
	this.derivedFrom = data.derivedFrom;
	this.setBinding(data.binding);
	this.setDefaultColors();
};

GraphVariable.prototype.setIsInput = function(isInput) {
	this.isInput = isInput;
	this.setDefaultColors();
	this.configure();
}

GraphVariable.prototype.setIsOutput = function(isOutput) {
	this.isOutput = isOutput;
	this.setDefaultColors();
	this.configure();
}

GraphVariable.prototype.setParam = function(isParam) {
	this.isParam = isParam;
	
	for(var id in this.inputPorts)
		this.inputPorts[id].role.type = isParam ? 2 : 1;
	for(var id in this.outputPorts)
		this.outputPorts[id].role.type = isParam ? 2 : 1;
	
	this.setDefaultColors();
	this.configure();
};

GraphVariable.prototype.setAutofill = function(autofill) {
	this.autofill = autofill;
	this.setDefaultColors();
	this.configure();
};

GraphVariable.prototype.setUnknown = function(unknown) {
	this.unknown = unknown;
	this.setDefaultColors();
	this.configure();
};

GraphVariable.prototype.setBreakpoint = function(breakpoint) {
	this.breakpoint = breakpoint;
	this.setDefaultColors();
	this.configure();
};

GraphVariable.prototype.getInputPort = function() {
	return this.getInputPorts()[0];
};

GraphVariable.prototype.getOutputPort = function() {
	return this.getOutputPorts()[0];
};

GraphVariable.prototype.setDefaultColors = function() {
	var alpha = 1;
	if (this.autofill)
		alpha = this.config.autofillopacity;

	if (this.unknown) {
		this.config.setBgcolor("rgba(128,5,22," + alpha + ")");
		this.config.setTextcolor("rgba(255,230,230,1)");
	}
	else if (this.isParam) {
		this.config.setBgcolor("rgba(102,153,51," + alpha + ")");
		this.config.setTextcolor("rgba(230,255,230,1)");
	}
	else {
		if (this.isInput) {
			this.config.setBgcolor("rgba(51,102,153," + alpha + ")");
			this.config.setTextcolor("rgba(230,230,255,1)");
		}
		else {
			if(!this.breakpoint) {
				this.config.setBgcolor("rgba(0,51,102," + alpha + ")");
				this.config.setTextcolor("rgba(220,220,255,1)");
			}
			else {
				this.config.setBgcolor("rgba(153,0,0," + alpha + ")");
				this.config.setTextcolor("rgba(255,230,230,1)");
				//this.config.setStrokecolor("rgba(70,140,210,0.8)");
			}
		}
	}
	this.config.setStrokecolor(this.config.getBgcolor());
};

GraphVariable.prototype.getBindingText = function(binding) {
	var text = "";
	if(!binding) {
		binding = this.binding;
		var text = getLocalName(this.id);
		if(!binding) return text;
		text += "\n";
	}
	if (!binding.length && binding.id)
		return text + getLocalName(binding.id);
	else if(!binding.length && binding.value)
		return text + binding.value;
	
	for ( var i = 0; i < binding.length; i++) {
		text += this.getBindingText(binding[i]) + "\n";
	}
	return text;
};

GraphVariable.prototype.equals = function(v) {
	if (!v)
		return false;
	if (this.id != v.id)
		return false;
	if (this.type != v.type)
		return false;
	if (this.dim != v.dim)
		return false;
	return true;
};
