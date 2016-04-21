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
 * Generic Shape Class
 */

var Shape = function(id, text, x, y, wmul, hmul) {
	if (arguments)
		this.initialize(id, text, x, y, wmul, hmul);
};

Shape.prototype.initialize = function(id, text, x, y, wmul, hmul) {
	this.id = id;
	this.text = text;
	this.x = x;
	this.y = y;
	this.centercoords = false;
	this.width = 0;
	this.height = 0;
	this.padding = 8;
	this.lineWidth = 1;
	this.font = "13px Courier";
	this.wmul = wmul ? wmul : 1;
	this.hmul = hmul ? hmul : 1;
	this.foregroundColor = "rgba(0,0,0,0.6)";
	this.textColor = "rgba(0,0,0,1)";
	this.backgroundColor = "rgba(200,200,200,1)";
	this.highlightColor = "rgba(250,240,100,1)";
	this.highlightTextColor = "rgba(0,0,0,1)";
	this.hideBGColor = "rgba(200,200,200,1)";
	this.hideTextColor = "rgba(0,0,0,0.4)";
	this.errorColor = "rgba(255,0,0,1)";
	this.layerItem = this.createLayerItem(id, text, x, y);
	this.inputPorts = new Array();
	this.outputPorts = new Array();
	this.canvas = null;
	this.shadow = false;
	this.inactive = false;
};

Shape.prototype.getInputPorts = function() {
	return this.inputPorts;
};

Shape.prototype.getOutputPorts = function() {
	return this.outputPorts;
};

Shape.prototype.getPorts = function() {
	return this.inputPorts.concat(this.outputPorts);
};

Shape.prototype.addInputPort = function(port) {
	for(var i=0; i<this.inputPorts.length; i++)
		if(this.inputPorts[i].id == port.id)
			return;
	this.inputPorts.push(port);
	this.inputPorts.sort(function(a, b) {
		return a.id.localeCompare(b.id);
	});
	port.partOf = this;
	port.isInput = true;
};

Shape.prototype.addOutputPort = function(port) {
	for(var i=0; i<this.outputPorts.length; i++)
		if(this.outputPorts[i].id == port.id)
			return;
	this.outputPorts.push(port);
	this.outputPorts.sort(function(a, b) {
		return a.id.localeCompare(b.id);
	});
	port.partOf = this;
	port.isOutput = true;
};

Shape.prototype.getInputPortById = function(id) {
	for ( var i = 0; i < this.inputPorts.length; i++) {
		if (this.inputPorts[i].id == id)
			return this.inputPorts[i];
	}
	return null;
};

Shape.prototype.getOutputPortById = function(id) {
	for ( var i = 0; i < this.outputPorts.length; i++) {
		if (this.outputPorts[i].id == id)
			return this.outputPorts[i];
	}
	return null;
};

Shape.prototype.getInputPortByName = function(name) {
	for ( var i = 0; i < this.inputPorts.length; i++) {
		if (this.inputPorts[i].name == name)
			return this.inputPorts[i];
	}
	return null;
};

Shape.prototype.getOutputPortByName = function(name) {
	for ( var i = 0; i < this.outputPorts.length; i++) {
		if (this.outputPorts[i].name == name)
			return this.outputPorts[i];
	}
	return null;
};

Shape.prototype.getLayerItems = function() {
	var layerItems = [];
	layerItems.push(this.layerItem);
	var ports = this.getPorts();
	for ( var i = 0; i < ports.length; i++) {
		layerItems.push(ports[i].getLayerItem());
	}
	return layerItems;
};

Shape.prototype.createLayerItem = function(lid, ltext, lx, ly) {
	var lshape = this;
	return {
		id : lid,
		x : lx,
		y : ly,
		shape : lshape,
		clear : function(ctx) {
			var ports = this.shape.getPorts();
			for ( var i = 0; i < ports.length; i++) {
				ports[i].getLayerItem().clear(ctx);
			}
			ctx.save();
			this.shape.drawPath(ctx, this.x - 1, this.y - 1, this.width + 2, this.height + 2);
			ctx.clip();
			ctx.clearRect(this.x, this.y, this.width, this.height);
			ctx.restore();
		},
		refresh : function(ctx) {
			this.clear(ctx);
			this.draw(ctx);
		},
		// ctx or context, is passed to the item by the layer
		draw : function(ctx) {
			if (!this.getLayer)
				return;
			this.shape.canvas = this.getLayer().parent;
			var lyr = this.shape.canvas.LayerManager.findLayer(this.getLayer().id);
			var highlight = (lyr.selectedItems.indexOf(this.shape) >= 0);
			this.shape.draw(ctx, this.shape.text, this.shape.x, this.shape.y, highlight);
			this.x = this.shape.x;
			this.y = this.shape.y;
			this.width = this.shape.width;
			this.height = this.shape.height;
			this.shape.resetPortPositions(this.shape.inputPorts, 'input');
			this.shape.resetPortPositions(this.shape.outputPorts, 'output');
			// this.shape.drawPorts(ctx);
		},
		on : {
			"mouseup" : function() {
				// var lyr =
				// this.getLayer().parent.LayerManager.findLayer(this.getLayer().id);
			},
			"mousedown" : function(event) {
				var lyr = this.shape.canvas.LayerManager.findLayer(this.getLayer().id);
				if (!lyr.newLinkFromPort) {
					// If the shape being clicked is not currently selected,
					// then remove all currently selected shapes and just select this
					// shape
					if (lyr.selectedItems.indexOf(this.shape) == -1)
						lyr.selectedItems = [
							this.shape
						];
					lyr.dragging = [];
					for ( var i = 0; i < lyr.selectedItems.length; i++) {
						lyr.dragging.push({
							x : event.mouseX - lyr.selectedItems[i].x,
							y : event.mouseY - lyr.selectedItems[i].y
						});
					}
				}
			}
		/*"mouseover" : function(event, type, ctx, item) {
			this.draw(ctx, true);
		},
		"mouseout" : function(event, type, ctx, item) {
			this.draw(ctx);
		},
		"dblclick" : function() {
			alert('Edit window called here');
		}*/
		}
	};
};

Shape.prototype.resetPortPositions = function(ports, type) {
	var spacing = Port.prototype.spacing;
	var size = Port.prototype.size;
	var portSpace = ports.length * size + (ports.length - 1) * spacing;
	var startX = this.x + size / 2.0 + (this.width - portSpace) / 2.0;
	// Center justify ports
	for ( var i = 0; i < ports.length; i++) {
		ports[i].x = startX + i * (size + spacing);
		ports[i].y = type == 'input' ? this.y : this.y + this.height;
	}
};

Shape.prototype.drawPorts = function(ctx) {
	var ports = this.getPorts();
	for ( var i = 0; i < ports.length; i++) {
		ports[i].getLayerItem().draw(ctx);
	}
};

Shape.prototype.getDimensions = function(ctx, text) {
	var maxwidth = 0;
	var fullheight = 0;
	var lineheight = 0;
	var lines = text.split(',');
	wmul = this.wmul;
	hmul = this.hmul;
	for(var i=0; i<lines.length; i++) {
		var line = lines[i];
		ctx.font = this.font;
		var dim = ctx.measureText(line);
		var fontsize = this.font.replace(/[^\d]/g, '');
		dim.height = parseInt(fontsize);
		var width = parseInt(dim.width);
		var height = parseInt(dim.height);
		if(width > maxwidth) maxwidth = width;
		if(height > lineheight) lineheight = height;
		fullheight += height;
	}
	return {
		width : wmul * maxwidth + this.padding,
		height : hmul * fullheight + this.padding,
		lineheight : lineheight
	};
};

Shape.prototype.draw = function(ctx, text, x, y, highlight) {
	var dim = this.getDimensions(ctx, text);
	this.x = x;
	this.y = y;
	this.text = text;
	this.width = dim.width;
	this.height = dim.height;
	this.lineheight = dim.lineheight;
	
	if(this.centercoords) {
		this.x = this.x - dim.width/2;
		this.y = this.y - dim.height/2;
		this.centercoords = false;
	}

	// Check if this shape has to be "hidden" or if there are any errors with it
	var color = this.backgroundColor;
	var fgcolor = this.foregroundColor;
	var tcolor = this.textColor;
	var shadow = this.shadow;
	if (this.canvas) {
		var sideEffects = this.canvas.template.sideEffects[this.id];
		var errors = this.canvas.template.errors[this.id];
		if (errors) {
			this.foreGroundColor = this.errorColor;
			this.textColor = this.errorColor;
		}
		if (sideEffects && sideEffects.op == "remove") {
			this.backgroundColor = this.hideBGColor;
			this.textColor = this.hideTextColor;
			this.shadow = false;
		}
	}

	if(this.isInactive())
		ctx.globalAlpha=0.3;
	this.drawShape(ctx, this.x, this.y, this.width, this.height, highlight);
	this.drawText(ctx, text, this.x, this.y, this.width, this.height, highlight);
	ctx.globalAlpha=1.0;

	// Restore original colors
	this.backgroundColor = color;
	this.foregroundColor = fgcolor;
	this.textColor = tcolor;
	this.shadow = shadow;
};

Shape.prototype.drawText = function(ctx, text, x, y, width, height, highlight) {
	ctx.fillStyle = highlight ? this.highlightTextColor : this.getTextColor();
	ctx.font = this.font;
	ctx.textAlign = "center";
	ctx.textBaseline = "middle";
	
	var lines = text.split(',');
	var toppadding = (this.height - this.lineheight * lines.length)/2.0;
	for(var i=0; i<lines.length; i++) {
		var line = lines[i];
		ctx.fillText(line, this.x + this.width / 2.0, this.y + this.lineheight / 2.0 + toppadding + i*this.lineheight);
	}
};

Shape.prototype.setBackgroundColor = function(color) {
	this.backgroundColor = color;
};

Shape.prototype.setForegroundColor = function(color) {
	this.foregroundColor = color;
};

Shape.prototype.setTextColor = function(color) {
	this.textColor = color;
};

Shape.prototype.getBackgroundColor = function() {
	return this.backgroundColor;
};

Shape.prototype.getForegroundColor = function() {
	return this.foregroundColor;
};

Shape.prototype.getTextColor = function() {
	return this.textColor;
};

Shape.prototype.isInactive = function() {
	return this.inactive;
};

Shape.prototype.setInactive = function(inactive) {
	this.inactive = inactive;
};

Shape.prototype.drawPath = function(ctx, x, y, width, height) {
	// Override this in subclasses
};

// Can also override drawShape in subclasses for more flexibility
Shape.prototype.drawShape = function(ctx, x, y, width, height, highlight) {
	ctx.save();
	ctx.fillStyle = highlight ? this.highlightColor : this.getBackgroundColor();
	ctx.strokeStyle = this.getForegroundColor();
	ctx.lineWidth = this.lineWidth;
	if (this.shadow) {
		ctx.shadowOffsetX = 2 * (this.canvas ? this.canvas.scale : 1);
		ctx.shadowOffsetY = 2 * (this.canvas ? this.canvas.scale : 1);
		// ctx.shadowOffsetX = 2;
		// ctx.shadowOffsetY = 2;
		ctx.shadowBlur = 0;
		ctx.shadowColor = 'rgba(0, 0, 0, 0.4)';
	}
	this.drawPath(ctx, x, y, width, height);
	// ctx.clip();
	// ctx.stroke();
	ctx.fill();
	ctx.restore();
};

Shape.prototype.enableShadow = function(ctx) {
	if (this.shadow) {
		ctx.shadowOffsetX = 2 * (this.canvas ? this.canvas.scale : 1);
		ctx.shadowOffsetY = 2 * (this.canvas ? this.canvas.scale : 1);
		ctx.shadowBlur = 4;
		ctx.shadowColor = 'rgba(0,0,0,0.4)';
	}
};

Shape.prototype.disableShadow = function(ctx) {
	ctx.shadowOffsetX = 0;
	ctx.shadowOffsetY = 0;
	ctx.shadowBlur = 0;
	ctx.shadowColor = 'rgba(0,0,0,0)';
};
