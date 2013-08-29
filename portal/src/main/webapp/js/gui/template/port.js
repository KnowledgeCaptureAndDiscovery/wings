/*
 * Port Class
 */

Port.prototype.size = 10; // Default Port size in pixels
Port.prototype.zoomSize = 20; // Zoomed Port size in pixels
Port.prototype.spacing = 16;
// Port.prototype.color = "rgba(204,204,250,1)";
Port.prototype.color = "rgba(255,204,153,1)";
// "darkorange";
Port.prototype.highlightColor = "red";
// Port.prototype.linkableColor = "#2EFE2E";
Port.prototype.linkableColor = "#FE2E2E";
// "#2EFE2E";
Port.prototype.tooltipdiv = "__wingstooltip";
Port.prototype.tooltipFontSize = 12;

Port.prototype.zHeight = Math.round(Port.prototype.zoomSize * 4.0 / 5.0);
Port.prototype.zWidth = Math.round(Port.prototype.zoomSize);
Port.prototype.height = Math.round(Port.prototype.size * 4.0 / 5.0);
Port.prototype.width = Math.round(Port.prototype.size);

function Port(tpl, id, role, color, isVariablePort, x, y) {
	this.tpl = tpl; // template
	this.id = id;
	this.name = role;
	this.color = color ? color : Port.prototype.color;
	this.x = x ? x : 0;
	this.y = y ? x : 0;
	this.isVariablePort = isVariablePort;
	this.isInput = false;
	this.isOutput = false;
	this.partOf = null;
	this.partOfLink = null;
	this.isHighlighted = false;
	this.tooltipdiv = this.initializeTooltip();
	this.layerItem = this.createLayerItem(id);
	this.dim = 0;
	this.type = 'DATA';
}

Port.prototype.equals = function(p) {
	if (!p)
		return false;
	// if(this.id != p.id) return false;
	if (this.name != p.name)
		return false;
	return true;
};

Port.prototype.initializeTooltip = function() {
	var div = document.getElementById(this.tooltipdiv);
	if (div)
		return div;
	div = document.createElement('div');
	div.id = this.tooltipdiv;
	div.style.position = "absolute";
	div.style.border = "1px solid #666";
	div.style.backgroundColor = "#FEA";
	div.style.fontSize = this.tooltipFontSize + 'px';
	div.style.fontFamily = "Calibri, Sans";
	div.style.display = "none";
	div.style.cursor = "default";
	div.style.opacity = "0.95";
	div.style.color = "#333";
	document.body.appendChild(div);
	return div;
};

Port.prototype.getSize = function() {
	return this.size;
};

Port.prototype.getLayerItem = function() {
	return this.layerItem;
};

Port.prototype.getComponentPort = function(port) {
	// Translate variable ports to original node ports from whence the variable comes
	if(port.isVariablePort) {
		var v = port.partOf;
		var links = this.tpl.getLinksWithVariable(v);
		if(port.isInput) {
			return links[0].toPort;
		}
		else {
			return links[0].fromPort;
		}
	}
	else {
		return port;
	}
};

/*
 * The "port" is being dragged
 * "this" (i.e. we) are checking if we are compatible
 */
Port.prototype.checkPortCompatibility = function(port) {
	// Inputs can't connect to Inputs
	// Outputs can't connect to Outputs
	if(this.isInput == port.isInput) 
		return false;
	
	// A Variable can't connect to another variable
	if(this.isVariablePort && port.isVariablePort)
		return false;
	
	// Parameters can't be connected to Data
	if(this.type != port.type)
		return false;
	
	// Cannot connect to ourselves
	if(this.partOf == port.partOf)
		return false;

	var port1 = this.getComponentPort(this);
	var port2 = this.getComponentPort(port);
	if(port1 == null || port2 == null)
		return false;
	
	var comp1 = port1.partOf;
	var comp2 = port2.partOf;
	var reasoner = this.tpl.editor.browser.reasoner;
	var type1 = reasoner.getComponentRoleType(comp1.getBindingId(), port1.name);
	var type2 = reasoner.getComponentRoleType(comp2.getBindingId(), port2.name);
	
	if(this.isInput)
		return reasoner.typeSubsumesType(type1, type2);
	else
		return reasoner.typeSubsumesType(type2, type1);
		
};

Port.prototype.createLayerItem = function(id) {
	var port = this;
	return {
		id : id,
		port : port,
		x : 0,
		y : 0,
		width : port.zWidth,
		height : port.zHeight,
		clear : function(ctx) {
			ctx.clearRect(this.x, this.y, this.width, this.height);
		},
		refresh : function(ctx) {
			this.clear(ctx);
			this.draw(ctx);
		},
		// ctx or context, is passed to the item by the layer
		draw : function(ctx) {
			var lyr = this.getLayer ? this.getLayer().parent.LayerManager.findLayer(this.getLayer().id) : null;

			ctx.lineWidth = 1;
			ctx.strokeStyle = 'rgba(0,0,0,1)';

			var w = this.port.isHighlighted ? this.width : this.port.width;
			var h = this.port.isHighlighted ? this.height : this.port.height;
			this.port.drawPath(ctx, this.port.x, this.port.y, w, h);

			// if(this.port.isHighlighted)
			// ctx.fillStyle = this.port.highlightColor;
			
			// If we are compatible with the dragged port, highlight ourselves
			if(lyr && lyr.newLinkFromPort &&
					port.checkPortCompatibility(lyr.newLinkFromPort))
					ctx.fillStyle = this.port.linkableColor;
			else
				ctx.fillStyle = this.port.color;
			
			ctx.fill();

			this.x = this.port.x - this.width / 2.0;
			this.y = this.port.y - (this.port.isInput ? this.height : 0);
		},
		on : {
			"mouseover" : function(event, type, ctx, item) {
				var canvas = this.getLayer().parent;
				var editable = canvas.template.editor.editable;
				if (!editable)
					return;

				var lyr = canvas.LayerManager.findLayer(this.getLayer().id);
				if (lyr.draggedItem)
					return;
				
				// Nothing to do here if we aren't compatible with the dragged port
				if(lyr && lyr.newLinkFromPort &&
					!port.checkPortCompatibility(lyr.newLinkFromPort))
						return;

				if (lyr.newLinkFromPort && lyr.newLinkFromPort != this)
					lyr.newLinkToPort = this.port;

				// Pointer
				canvas.getEl().style.cursor = 'pointer';

				// Highlight
				this.port.isHighlighted = true;
				this.draw(ctx);

				// Check sideEffects only while dragging
				if (lyr.newLinkFromPort) {
					canvas.template.markLinkAdditionSideEffects(lyr.newLinkFromPort, lyr.newLinkToPort, lyr.newVariable);
					// return;
				}

				// Tooltips
				if (this.port.isVariablePort) {
					if (lyr.newLinkFromPort) {
						// this.port.showToolTip(canvas, this.port.partOf.id);
					}
					else if (this.port.isInput)
						this.port.showToolTip(canvas, "Drag from a Component Output port onto this port to create a link");
					else
						this.port.showToolTip(canvas, "Drag from here onto a Component Input port to create a link");
					return;
				}
				var portInfo1 = "<br/>Drag from here to a Variable Output Port, Component Output Port, or drop it in empty space to create new Input Link";
				var portInfo2 = "<br/>Drag from here to a Variable Input Port, Component Input Port, or drop it in empty space to create new Output Link";
				if (lyr.newLinkFromPort)
					this.port.showToolTip(canvas, this.port.name);
				else if (this.port.isInput)
					this.port.showToolTip(canvas, "<b>Input Port: " + this.port.name + "</b>" + portInfo1);
				else
					this.port.showToolTip(canvas, "<b>Output Port: " + this.port.name + "</b>" + portInfo2);
			},
			"mouseout" : function(event, type, ctx, item) {
				var canvas = this.getLayer().parent;
				var lyr = canvas.LayerManager.findLayer(this.getLayer().id);
				lyr.newLinkToPort = null;

				canvas.getEl().style.cursor = 'default';

				lyr.needRefresh = true;
				this.port.isHighlighted = false;
				this.port.hideToolTip();
			},
			"mousedown" : function(event) {
				var canvas = this.getLayer().parent;
				var lyr = canvas.LayerManager.findLayer(this.getLayer().id);
				lyr.draggedItem = null;

				this.port.tooltipdiv.style.display = "none";

				var editable = canvas.template.editor.editable;
				if (editable)
					lyr.newLinkFromPort = this.port;
			},
			"mouseup" : function(event) {
				var canvas = this.getLayer().parent;
				var lyr = canvas.LayerManager.findLayer(this.getLayer().id);
				/*if(lyr.newLinkFromPort && lyr.newLinkFromPort != this) 
					lyr.newLinkToPort = this;*/
			}
		}
	};
};

Port.prototype.showToolTip = function(canvas, message) {
	var scr = canvas.getScrollXY(true);
	var scale = canvas.getScale();
	var oleft = canvas.x;
	var otop = canvas.y;
	this.tooltipdiv.innerHTML = message;

	scale = 1;
	this.tooltipdiv.style.fontSize = parseInt(Port.prototype.tooltipFontSize * scale) + 'px';
	this.tooltipdiv.style.lineHeight = parseInt(Port.prototype.tooltipFontSize * scale) + 'px';
	this.tooltipdiv.style.visibility = "hidden";
	this.tooltipdiv.style.padding = Math.round(2 * scale) + "px";
	this.tooltipdiv.style.maxWidth = Math.round(250 * scale) + "px";
	this.tooltipdiv.style.display = "";
	this.tooltipdiv.style.zIndex = 100;

	scale = canvas.getScale();
	this.tooltipdiv.style.left = parseInt(scale * (this.x) - scr[0] + oleft - this.tooltipdiv.offsetWidth / 2.0) + 'px';
	// this.tooltipdiv.style.top =
	// parseInt(scale*(this.y-(this.isInput?this.zHeight:0)-15) - scr[1] + otop -
	// this.tooltipdiv.offsetHeight) + 'px';
	this.tooltipdiv.style.top = parseInt(scale * (this.y) - (this.isInput ? this.zHeight : 0) - 10 - scr[1] + otop
			- this.tooltipdiv.offsetHeight)
			+ 'px';
	this.tooltipdiv.style.visibility = "visible";
};

Port.prototype.hideToolTip = function() {
	this.tooltipdiv.style.display = "none";
};

Port.prototype.drawPath = function(ctx, x, y, w, h) {
	ctx.beginPath();
	x = x - w / 2.0;
	//h = h - 1;
	if (this.isInput) {
		y = y - h - 0.5;
		ctx.moveTo(x, y + h);
		ctx.lineTo(x, y);
		ctx.lineTo(x + w, y);
		ctx.lineTo(x + w, y + h);
	}
	else {
		y = y + 0.5;
		ctx.moveTo(x, y);
		ctx.lineTo(x, y + h);
		ctx.lineTo(x + w, y + h);
		ctx.lineTo(x + w, y);
	}
	// Stroke ports only for nodes (not for variables)
	// if(!this.partOf.isVariable)
	ctx.stroke();
	
	// ctx.rect(x-size/2.0, y-size/2.0, size, size);
	// ctx.moveTo(x+size/2.0, y+size/2.0);
	// ctx.arc(x,y,size/2.0,0,Math.PI*2,true);
	ctx.closePath();
};
