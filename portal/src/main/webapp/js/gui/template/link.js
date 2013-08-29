/*
 * Generic Link Class
 */

var Link = function(tpl, fromPort, toPort, variable) {
	this.tpl = tpl;
	this.fromNode = null;
	this.fromPort = null;
	this.toNode = null;
	this.toPort = null;
	this.setFromPort(fromPort);
	this.setToPort(toPort);
	this.variable = variable;
	this.color = 'rgba(50,50,50,0.9)';
	this.hideLinkColor = 'rgba(0,0,0,0.2)';
	this.layerItem = this.createLayerItem(this.id);
};

Link.prototype.arrowSize = 8;

Link.prototype.equals = function(l) {
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

Link.prototype.getLayerItem = function() {
	return this.layerItem;
};

Link.prototype.setId = function() {
	var ns = "";
	var name = "";
	if(this.fromNode && this.fromPort) {
		ns = getNamespace(this.fromNode.id);
		name = getLocalName(this.fromPort.id);
	}
	else {
		name += "ip";
	}
	name += "_";
	if(this.toNode && this.toPort) {
		ns = getNamespace(this.toNode.id);
		name += getLocalName(this.toPort.id);
	}
	else {
		name += "op";
	}
	this.id = ns + name;
};

Link.prototype.setFromPort = function(fromPort) {
	// Alter existing fromPort (if any)
	if (this.fromPort)
		this.fromNode.removeOutputLink(this);

	this.fromNode = fromPort ? fromPort.partOf : null;
	this.fromPort = fromPort;
	if (this.fromPort) 
		this.fromPort.partOfLink = this;
	if (this.fromNode)
		this.fromNode.addOutputLink(this);

	this.setId();
};

Link.prototype.setToPort = function(toPort) {
	// Alter existing toPort (if any)
	if (this.toPort)
		this.toNode.removeInputLink(this);

	this.toNode = toPort ? toPort.partOf : null;
	this.toPort = toPort;
	if (this.toPort)
		this.toPort.partOfLink = this;
	if (this.toNode)
		this.toNode.addInputLink(this);

	this.setId();
};

Link.prototype.createLayerItem = function(id) {
	var link = this;
	return {
		id : id,
		link : link,
		clear : function(ctx) {
			// How to clear a link ?
			// How to figure out intersecting objects underneath
		},
		refresh : function(ctx) {
			this.clear(ctx);
			this.draw(ctx);
		},
		// ctx or context, is passed to the item by the layer
		draw : function(ctx) {
			// ctx.save();
			var canvas = this.getLayer().parent;
			var lyr = canvas.LayerManager.findLayer(this.getLayer().id);
			ctx.lineWidth = 1;
			
			var xitems = [];
			for(var i=0; i<lyr.items.length; i++) {
				if(lyr.items[i].shape) {
					xitems.push(lyr.items[i]);
				}
			}
			xitems.sort(function(a, b) {
				return a.y - b.y;
			});
			
			this.link.draw(ctx, canvas.template.sideEffects[this.link.id], xitems);
			// ctx.restore();
		},
		on : {}
	};
};

// Link drawing should only happen after the node has been drawn and its ports
// have been updated !!!
Link.prototype.draw = function(ctx, sideEffects, items) {
	var color = this.color;
	
	if (sideEffects && sideEffects.op == "remove")
		this.color = this.hideLinkColor;
	if (this.fromPort) {
		if (sideEffects && sideEffects.op == "changeFromPort") {
			this.color = this.hideLinkColor;
		}
		// Draw Path from fromNode/fromOutputPort -> Variable/inputPort
		this.drawPartialLink(ctx, this.fromPort, this.variable.getInputPort(), false, true, items);
		this.color = color;
	}
	if (this.toPort) {
		if (sideEffects && sideEffects.op == "changeToPort") {
			this.color = this.hideLinkColor;
		}
		this.drawPartialLink(ctx, this.variable.getOutputPort(), this.toPort, false, true, items);
		this.color = color;
	}
	this.color = color;
};

Link.prototype.drawPartialLink = function(ctx, port1, port2, inverted, straight, items) {
	if (inverted) {
		var tmp = port1;
		port1 = port2;
		port2 = tmp;
	}
	x1 = port1.x;
	y1 = port1.y + Port.prototype.height + 2;
	x2 = port2.x;
	y2 = port2.y - Port.prototype.height - 2;
	
	var pstart = new Point(x1, y1);
	var pend = new Point(x2, y2);
	var points = this.getLineSegments(pstart, pend, items);
	points.unshift(pstart);
	points.push(pend);
	
	ctx.beginPath();
	ctx.moveTo(points[0].x, points[0].y);
	var i=0;
	if (points.length > 2) {
		for (i = 1; i < points.length - 2; i++) {
			var xc = (points[i].x + points[i + 1].x) / 2;
			var yc = (points[i].y + points[i + 1].y) / 2;
			ctx.quadraticCurveTo(points[i].x, points[i].y, xc, yc);
		}
		ctx.quadraticCurveTo(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y);
	} else {
		ctx.lineTo(points[points.length-1].x, points[points.length-1].y);
	}
	ctx.strokeStyle = ctx.fillStyle = this.color ? this.color : "rgba(0,0,0,1)";
	ctx.lineWidth = 1;
	ctx.stroke();
	this.drawArrow(ctx, points[i].x, points[i].y, points[i+1].x, points[i+1].y);
};

Link.prototype.drawArrow = function(ctx, x0, y0, x1, y1) {
	var len = this.arrowSize * ctx.lineWidth;
	var an = an ? an : 0.4;

	var d = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
	var ah = Math.acos((x1 - x0) / d);
	if (y1 < y0)
		ah = -ah;
	var at = ah + Math.PI;

	var x1a = x1 - len * Math.cos(ah + an);
	var y1a = y1 - len * Math.sin(ah + an);
	var x1b = x1 - len * Math.cos(ah - an);
	var y1b = y1 - len * Math.sin(ah - an);

	var cx1 = x1a + (x1b - x1a) / 2;
	var cy1 = y1a + (y1b - y1a) / 2;
	
	ctx.beginPath();
	ctx.moveTo(x1, y1);
	ctx.lineTo(x1a, y1a);
	ctx.lineTo(x1b, y1b);
	ctx.closePath();
	ctx.stroke();
	ctx.fill();
};

Link.prototype.getLineSegments = function(pstart, pend, items) {
	if (!items || !items.length) {
		return [];
	}

	var nitems = [];
	for(var i=0; i<items.length; i++)
		nitems[i] = items[i];
	
	var npoints = [];
	while(nitems.length) {
		var n = nitems.shift();
		if (!n.width || !n.height || !n.shape)
			continue;
		
		var intersection = this.lineIntersects(pstart.x, pstart.y, pend.x, pend.y, 
				n.x, n.y, n.x + n.width, n.y + n.height);
		if (intersection) {
			var corners = [];
			var dx = n.width/10;
			var dy = n.height/10;
			corners[0] = new Point(n.x-dx, n.y-dy); // tl
			corners[1] = new Point(n.x+n.width+dx, n.y-dy); // tr
			corners[2] = new Point(n.x-dx, n.y+n.height+dy); // bl
			corners[3] = new Point(n.x+n.width+dx, n.y+n.height+dy); // br

			var stpt = this.findClosestCorner(corners, intersection[0]);
			var endpt = this.findClosestCorner(corners, intersection[1]);
			if(stpt == endpt)
				npoints.push(stpt);
			else {
				if((stpt==corners[0] && endpt==corners[3]) ||
						(stpt == corners[1] && endpt==corners[3])) {
					npoints.push(corners[1]);
					npoints.push(corners[3]);
				}
				else if((stpt==corners[1] && endpt==corners[2]) ||
						(stpt == corners[0] && endpt==corners[2])) {
					npoints.push(corners[0]);
					npoints.push(corners[2]);
				}
				else if(stpt==corners[3] && endpt==corners[0]) {
					npoints.push(corners[2]);
					npoints.push(corners[0]);
				}
				else if(stpt==corners[2] && endpt==corners[1]) {
					npoints.push(corners[3]);
					npoints.push(corners[1]);
				}
			}
			var mpoints = this.getLineSegments(endpt, pend, nitems);
			for(var j=0; j<mpoints.length; j++) {
				npoints.push(mpoints[j]);
			}
			break;
		}
	}

	return npoints;
};

Link.prototype.findClosestCorner = function(corners, point) {
	var current;
	var max = 99999;
	for(var j=0; j<corners.length; j++) {
		var c = corners[j];
		var diff = Math.abs(point.x-c.x) + Math.abs(point.y-c.y);
		if(diff < max) {
			max = diff;
			current = c;
		}
	}
	return current;
};

Link.prototype.lineIntersects = function(x1, y1, x2, y2, xmin, ymin, xmax, ymax) {
	var u1 = 0.0;
	var u2 = 1.0;
	var r;

	var deltaX = (x2 - x1);
	var deltaY = (y2 - y1);

	/*
	 * left edge, right edge, bottom edge and top edge checking
	 */
	var pPart = [
			-1 * deltaX, deltaX, -1 * deltaY, deltaY
	];
	var qPart = [
			x1 - xmin, xmax - x1, y1 - ymin, ymax - y1
	];

	var accept = true;

	for ( var i = 0; i < 4; i++) {
		p = pPart[i];
		q = qPart[i];

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
			x2 = parseInt(x1 + u2 * deltaX);
			y2 = parseInt(y1 + u2 * deltaY);
		}
		if (u1 > 0) {
			x1 = parseInt(x1 + u1 * deltaX);
			y1 = parseInt(y1 + u1 * deltaY);
		}
		return [
				new Point(x1, y1), new Point(x2, y2)
		];
	}
	else {
		return null;
	}
};

var Point = function(x, y) {
	this.x = x;
	this.y = y;
};

var Line = function(start, end) {
	this.start = start;
	this.end = end;
};

/**
 * Helper Classes to simplify link creation
 */

var InputLink = function(toPort, variable) {
	return new Link(null, toPort, variable);
};

var OutputLink = function(fromPort, variable) {
	return new Link(fromPort, null, variable);
};
