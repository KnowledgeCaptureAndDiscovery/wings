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
 * Generic Variable Class
 */

var Variable = function(tpl, id, text, x, y, type) {
	this.tpl = tpl; // template
	this.shadow = false;
	this.initialize(id, text, x, y, 1, 1);
	this.addInputPort(new Port(tpl, this.id + '_ip', text + '_ip', this.color, true));
	this.addOutputPort(new Port(tpl, this.id + '_op', text + '_op', this.color, true));

	this.hideBGColor = "rgba(204,224,224,0.4)";
	this.setType(type);
	this.dim = 0;
	this.unknown = false;
	this.isInput = false;
	this.isOutput = false;
	this.autofill = false;
	this.breakpoint = false;
	this.binding = null;
	// this.font = "Bold 14px Tahoma, Arial";
	// this.font = "bold 14px Optimer";
	this.font = "13px tahoma";
	this.inactive = false;
};
Variable.prototype = new Shape();
Variable.prototype.KAPPA = 1.4 * (Math.sqrt(2) - 1);

Variable.prototype.equals = function(v) {
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

Variable.prototype.getInputPort = function() {
	return this.inputPorts[0];
};

Variable.prototype.getOutputPort = function() {
	return this.outputPorts[0];
};

Variable.prototype.setDefaultColors = function() {
	var alpha = 1;
	if (this.autofill)
		alpha = 0.4;
	
	if (this.unknown) {
		this.color = "rgba(128,5,22," + alpha + ")";
		this.setTextColor("rgba(255,230,230," + alpha + ")");
	}
	else if (this.type == 'PARAM') {
		this.color = "rgba(102,153,51," + alpha + ")";
		this.setTextColor("rgba(230,255,230," + alpha + ")");
		if(this.autofill)
			this.setTextColor("rgba(0,0,0," + alpha + ")");
	}
	else {
		if (this.isInput) {
			this.color = "rgba(51,102,153," + alpha + ")";
			this.setTextColor("rgba(230,230,255," + alpha + ")");
		}
		else {
			if(!this.breakpoint) {
				this.color = "rgba(0,51,102," + alpha + ")";
				this.setTextColor("rgba(220,220,255," + alpha + ")");
			}
			else {
				this.color = "rgba(153,0,0," + alpha + ")";
				this.setTextColor("rgba(255,230,230," + alpha + ")");				
			}
		}
	}
	this.setBackgroundColor(this.color);
	this.getInputPort().color = this.color;
	this.getOutputPort().color = this.color;
};

Variable.prototype.setDimensionality = function(dim) {
	this.dim = dim;
	var dimint = parseInt(dim);
	this.width += dimint*3;
	this.height += dimint*3;
	this.setDefaultColors();
};

Variable.prototype.setIsInput = function(isInput) {
	this.isInput = isInput;
	this.setDefaultColors();
};

Variable.prototype.setIsOutput = function(isOutput) {
	this.isOutput = isOutput;
	this.setDefaultColors();
};

Variable.prototype.setIsUnknown = function(unknown) {
	this.unknown = unknown;
	this.setDefaultColors();
};

Variable.prototype.setAutoFill = function(autofill) {
	this.autofill = autofill;
	this.setDefaultColors();
};

Variable.prototype.setBreakPoint = function(breakpoint) {
	this.breakpoint = breakpoint;
	this.setDefaultColors();
};

Variable.prototype.setType = function(type) {
	this.type = type;
	this.getInputPort().type = type;
	this.getOutputPort().type = type;
	this.setDefaultColors();
};

Variable.prototype.getDimensionality = function() {
	return this.dim;
};

Variable.prototype.setBinding = function(binding) {
	if (binding) {
		this.text = getLocalName(this.id) + " =," + this.getBindingText(binding);
		this.binding = binding;
	}
};

Variable.prototype.getBindingText = function(binding) {
	if(!binding) binding = this.binding;
	if (!binding.length && binding.id)
		return getLocalName(binding.id);
	else if(!binding.length && binding.value)
		return binding.value;
	
	var text = "";
	for ( var i = 0; i < binding.length; i++) {
		if(i > 0) text += ",";
		text += this.getBindingText(binding[i]);
	}
	return "["+text+"]";
};

Variable.prototype.drawShape = function(ctx, x, y, width, height, highlight) {
	// ctx.save();
	ctx.fillStyle = highlight ? this.highlightColor : this.getBackgroundColor();
	ctx.strokeStyle = this.getForegroundColor();
	if (this.dim) {
		var tmpstyle = ctx.fillStyle;
		var len = parseInt(this.dim) + 1;
		this.enableShadow(ctx);
		ctx.lineWidth = 1;
		for ( var i = len; i >= 1; i--) {
			ctx.beginPath();
			this.drawRoundedRectangle(ctx, x + i * 3, y + i * 3, width, height);
			ctx.closePath();
			ctx.fillStyle = "rgba(200,200,200,0.5)";
			if (!highlight)
				ctx.strokeStyle = tmpstyle;
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
	this.drawRoundedRectangle(ctx, x, y, width, height);
	ctx.closePath();
	ctx.stroke();
	ctx.fill();
	// ctx.restore();
};

Variable.prototype.drawRoundedRectangle = function(ctx, x, y, width, height,
		radius) {
	if (typeof radius === "undefined") {
		radius = 8;
	}
	ctx.moveTo(x + radius, y);
	ctx.lineTo(x + width - radius, y);
	ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
	ctx.lineTo(x + width, y + height - radius);
	ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
	ctx.lineTo(x + radius, y + height);
	ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
	ctx.lineTo(x, y + radius);
	ctx.quadraticCurveTo(x, y, x + radius, y);
};

Variable.prototype.drawEllipse = function(ctx, x, y, width, height) {
	// return ctx.rect(x,y,width,height);
	x1 = x;
	x2 = x + width;
	y1 = y;
	y2 = y + height;
	var rx = (x2 - x1) / 2;
	var ry = (y2 - y1) / 2;
	var cx = x1 + rx;
	var cy = y1 + ry;

	ctx.moveTo(cx, cy - ry);
	ctx.bezierCurveTo(cx + (this.KAPPA * rx), cy - ry, cx + rx, cy - (this.KAPPA * ry), cx + rx, cy);
	ctx.bezierCurveTo(cx + rx, cy + (this.KAPPA * ry), cx + (this.KAPPA * rx), cy + ry, cx, cy + ry);
	ctx.bezierCurveTo(cx - (this.KAPPA * rx), cy + ry, cx - rx, cy + (this.KAPPA * ry), cx - rx, cy);
	ctx.bezierCurveTo(cx - rx, cy - (this.KAPPA * ry), cx - (this.KAPPA * rx), cy - ry, cx, cy - ry);
};

/**
 * Helper Classes to simplify variable creation
 */

var DraggerVariable = function(tpl, id, text) {
	var v = new Variable(tpl, id, text, 0, 0);
	v.shadow = false;
	v.isPlaceholder = true;
	v.setTextColor("rgba(255,255,255,1)");
	v.setForegroundColor("rgba(255,255,255,1)");
	v.setBackgroundColor("rgba(0,0,0,0.4)");
	return v;
};
