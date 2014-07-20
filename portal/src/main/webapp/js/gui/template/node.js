/*
 * Generic Node Class
 */

var Node = function(tpl, id, component, x, y) {
	this.tpl = tpl; // template
	this.component = component;
	this.initialize(id, getLocalName(id), x, y, 1, 1);
	this.setBackgroundColor("rgba(255,204,153,1)");
	this.setTextColor("rgba(72,42,3,1)");
	this.inputLinks = new Array();
	this.outputLinks = new Array();
	this.isConcrete = true;
	this.binding = null;
	
	this.crule = {type:'WTYPE'};
	this.prule = {type:'STYPE', expr:{'op' : 'XPRODUCT', args: []}};

	this.font = "bold 13px tahoma";
	this.dim = 0;
	this.machineIds = [];
	this.inactive = false;
};
Node.prototype = new Shape();

Node.prototype.equals = function(n) {
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

Node.prototype.getDimensions = function(ctx, text) {
	var dims = Shape.prototype.getDimensions.call(this, ctx, text);

	var psize = Port.prototype.size;
	var pspacing = Port.prototype.spacing;
	var ipwidth = pspacing + (psize + pspacing) * this.inputPorts.length;
	var opwidth = pspacing + (psize + pspacing) * this.outputPorts.length;

	dims.width = dims.width > ipwidth ? dims.width : ipwidth;
	dims.width = dims.width > opwidth ? dims.width : opwidth;
	return dims;
};

Node.prototype.setComponentRule = function(crule) {
	if(!crule) return;
	this.crule = crule;
	if (this.crule.type == 'STYPE')
		this.text = "{" + this.getBindingText(this.binding) + "}";
	else
		this.text = this.getBindingText(this.binding);
};

Node.prototype.setPortRule = function(prule) {
	if(!prule) return;
	this.prule = prule;
};

Node.prototype.setDefaultPortRule = function() {
	this.prule = {type:'STYPE', expr:{'op' : 'XPRODUCT', 'args': []}};
	var ips = this.getInputPorts();
	for(var i=0; i<ips.length; i++) {
		this.prule.expr.args.push(ips[i].id);
	}
};

Node.prototype.setConcrete = function(isConcrete) {
	this.isConcrete = isConcrete;
	if (!this.isConcrete) {
		this.color = "rgba(204,204,204,1)";
		this.setBackgroundColor(this.color);
		var ports = this.getPorts();
		for ( var i = 0; i < ports.length; i++)
			ports[i].color = this.color;
	}
	else {
		this.color = "rgba(255,204,153,1)";
		this.setBackgroundColor(this.color);
		var ports = this.getPorts();
		for ( var i = 0; i < ports.length; i++)
			ports[i].color = this.color;
	}
};

Node.prototype.setBinding = function(binding) {
	if (binding) {
		//this.setConcrete(true);
		this.dim = this.getBindingDimensionality(binding);
		this.text = this.getBindingText(binding);
		this.binding = binding;
	}
};

Node.prototype.getBindingDimensionality = function(binding) {
	if (typeof (binding) == "string")
		return 0;
	var max = 0;
	for ( var i = 0; i < binding.length; i++) {
		var m = this.getBindingDimensionality(binding[i]) + 1;
		if (m > max)
			max = m;
	}
	if (binding.length == 1)
		return max - 1;
	return max;
};

Node.prototype.getMachinesText = function() {
	if(this.inactive)
		return "";
	if(this.machineIds.length == 1)
		return getLocalName(this.machineIds[0]);
	else if(this.machineIds.length > 1)
		return "1 of " + this.machineIds.length + " machines";
	else
		return "";
};


Node.prototype.getBindingText = function(binding) {
	if(!binding) binding = this.binding;
	var sfx = this.getMachinesText();
	if(sfx) 
		sfx = ",[Run on " + sfx + "]";
	
	if (!binding.length)
		return getLocalName(binding.id) + sfx;
	var text = "";
	for ( var i = 0; i < binding.length; i++) {
		if(i > 0) text += ",";
		text += this.getBindingText(binding[i]);
	}
	return text + sfx;
};

Node.prototype.getBindingId = function(binding) {
	if(!binding) binding = this.binding;
	if (!binding.length)
		return binding.id;
	var text = "";
	for ( var i = 0; i < binding.length; i++) {
		if(i > 0) text += ",";
		text += this.getBindingId(binding[i]);
	}
	if(binding.length == 1)
		return text;
	return text;
};

Node.prototype.drawShape = function(ctx, x, y, width, height, highlight) {
	// ctx.save();
	ctx.fillStyle = highlight ? this.highlightColor : this.getBackgroundColor();
	ctx.strokeStyle = this.getForegroundColor();
	if (this.dim) {
		var tmpstyle = ctx.fillStyle;
		var len = 2;
		this.enableShadow(ctx);
		ctx.lineWidth = 1;
		for ( var i = len; i >= 1; i--) {
			ctx.beginPath();
			this.drawParallelogram(ctx, x + i * 3, y + i * 3, width, height, 4);
			ctx.closePath();
			// ctx.fillStyle = "rgba(255,255,255,0.7)";
			ctx.stroke();
			ctx.fill();
			this.disableShadow(ctx);
		}
		ctx.fillStyle = tmpstyle;
	}
	else {
		ctx.lineWidth = 1;
		this.enableShadow(ctx);
	}
	ctx.beginPath();
	var len = (this.text.split(",")).length;
	//this.drawRectangle(ctx, x, y, width, height);
	this.drawParallelogram(ctx, x, y, width + (len-1)*4, height, 4);
	ctx.closePath();
	ctx.stroke();
	ctx.fill();
	// ctx.restore();
};

Node.prototype.drawRectangle = function(ctx, x, y, width, height) {
	ctx.beginPath();
	ctx.rect(x, y, width, height);
	ctx.closePath();
};

Node.prototype.drawParallelogram = function(ctx, x, y, width, height, shift) {
	ctx.beginPath();
	ctx.moveTo(x-shift, y+height);
	ctx.lineTo(x+shift, y);
	ctx.lineTo(x+shift+width, y);
	ctx.lineTo(x-shift+width, y+height);
	ctx.lineTo(x-shift, y+height);
	ctx.closePath();
};

Node.prototype.addInputLink = function(link) {
	this.inputLinks.push(link);
};

Node.prototype.addOutputLink = function(link) {
	this.outputLinks.push(link);
};

Node.prototype.removeInputLink = function(link) {
	var index = this.inputLinks.indexOf(link);
	if (index >= 0)
		return this.inputLinks.splice(index, 1);
};

Node.prototype.removeOutputLink = function(link) {
	var index = this.outputLinks.indexOf(link);
	if (index >= 0)
		return this.outputLinks.splice(index, 1);
};

Node.prototype.getInputLinks = function() {
	return this.inputLinks;
};

Node.prototype.getOutputLinks = function() {
	return this.outputLinks;
};

Node.prototype.getLinks = function() {
	return this.inputLinks.concat(this.outputLinks);
};
