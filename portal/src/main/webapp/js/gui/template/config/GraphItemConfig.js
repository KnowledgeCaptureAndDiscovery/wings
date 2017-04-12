function GraphItemConfig() {
	// Configuration
	// TODO: Get from config file
	this.xpad = 4;
	this.ypad = 3;
	this.portsize = 10;
	this.portpad = 10;
	this.stackspacing = 3;
	this.strokewidth = 1;
	this.fontsize = 13;
	this.font = "Tahoma";
	this.fontweight = "bold";
	this.textcolor = "rgba(72,42,3,1)";
	this.strokecolor = "rgba(0,0,0,0.6)";
	this.bgcolor = "rgba(200,200,200,1)";
	this.stackcolor = this.bgcolor;
	this.inactiveopacity = 0.4;
	this.autofillopacity = 0.5;
};

function GraphNodeConfig() {
	GraphItemConfig.call(this);
	this.xpad = 8;
	this.bgcolor = "rgba(255,204,153,1)";
	this.stackcolor = this.bgcolor;
	this.strokewidth = 0.7;
	this.xshift = 8;
};
GraphNodeConfig.prototype = Object.create(GraphItemConfig.prototype);
GraphNodeConfig.prototype.constructor = GraphNodeConfig;

function GraphVariableConfig() {
	GraphItemConfig.call(this);
	this.bgcolor = "rgba(0,51,102,1)";
	this.textcolor = "rgba(220,220,255,1)";
	this.strokecolor = this.bgcolor;
	this.fontweight = "normal";
	this.radius = 6;
};
GraphVariableConfig.prototype = Object.create(GraphItemConfig.prototype);
GraphVariableConfig.prototype.constructor = GraphVariableConfig;

function GraphLinkConfig() {
	GraphItemConfig.call(this);
	this.strokecolor = "rgba(0,0,0,1)";
	this.strokewidth = 0.9
	this.strokeopacity = 0.6;
	this.intersectionpad = 15;
	this.linkstartpad = 15;
	this.interpolation = d3.curveBundle.beta(1);
};
GraphLinkConfig.prototype = Object.create(GraphItemConfig.prototype);
GraphLinkConfig.prototype.constructor = GraphLinkConfig;

function GraphPreviewVariableConfig() {
	GraphVariableConfig.call(this);
	this.textcolor = "rgba(255,255,255,1)";
	this.strokecolor = "rgba(255,255,255,1)";
	this.bgcolor = "rgba(0,0,0,0.4)";
	this.placeholder = true;
};
GraphPreviewVariableConfig.prototype = Object.create(GraphVariableConfig.prototype);
GraphPreviewVariableConfig.prototype.constructor = GraphPreviewVariableConfig;

GraphItemConfig.prototype.getXpad = function() {
	return this.xpad;
};

GraphItemConfig.prototype.setXpad = function(xpad) {
	this.xpad = xpad;
};

GraphItemConfig.prototype.getYpad = function() {
	return this.ypad;
};

GraphItemConfig.prototype.setYpad = function(ypad) {
	this.ypad = ypad;
};

GraphItemConfig.prototype.getPortsize = function() {
	return this.portsize;
};

GraphItemConfig.prototype.setPortsize = function(portsize) {
	this.portsize = portsize;
};

GraphItemConfig.prototype.getPortpad = function() {
	return this.portpad;
};

GraphItemConfig.prototype.setPortpad = function(portpad) {
	this.portpad = portpad;
}

GraphItemConfig.prototype.getStackspacing = function() {
	return this.stackspacing;
};

GraphItemConfig.prototype.setStackspacing = function(stackspacing) {
	this.stackspacing = stackspacing;
}

GraphItemConfig.prototype.getStrokewidth = function() {
	return this.strokewidth;
};

GraphItemConfig.prototype.setStrokewidth = function(strokewidth) {
	this.strokewidth = strokewidth;
}

GraphItemConfig.prototype.getStackcolor = function() {
	return this.stackcolor;
};

GraphItemConfig.prototype.setStackcolor = function(stackcolor) {
	this.stackcolor = stackcolor;
}

GraphItemConfig.prototype.getFontsize = function() {
	return this.fontsize;
};

GraphItemConfig.prototype.setFontsize = function(fontsize) {
	this.fontsize = fontsize;
};

GraphItemConfig.prototype.getFontweight = function() {
	return this.fontweight;
};

GraphItemConfig.prototype.setFontweight = function(fontweight) {
	this.fontweight = fontweight;
};

GraphItemConfig.prototype.getFont = function() {
	return this.font;
};

GraphItemConfig.prototype.setFont = function(font) {
	this.font = font;
};

GraphItemConfig.prototype.getTextcolor = function() {
	return this.textcolor;
};

GraphItemConfig.prototype.setTextcolor = function(textcolor) {
	this.textcolor = textcolor;
};

GraphItemConfig.prototype.getStrokecolor = function() {
	return this.strokecolor;
};

GraphItemConfig.prototype.setStrokecolor = function(strokecolor) {
	this.strokecolor = strokecolor;
};

GraphItemConfig.prototype.getBgcolor = function() {
	return this.bgcolor;
};

GraphItemConfig.prototype.setBgcolor = function(bgcolor) {
	this.bgcolor = bgcolor;
};

