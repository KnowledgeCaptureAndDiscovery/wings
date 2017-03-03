function GraphPort(parent, graphItem, id, role, isInput, config) {
	this.parent = parent;
	this.graphItem = graphItem;
	
	this.id = id;
	this.role = role;
	this.isInput = isInput;
	this.config = config;
	
	this.coords = {x: 0, y:0};

	// D3
	this.item = null;
	
	this.create();
	this.configure();
};

GraphPort.prototype.create = function() {
	this.item = this.parent.append('circle');
};

GraphPort.prototype.configure = function() {
	this.item.attr("id", this.id)
		.attr("stroke", this.config.getStrokecolor())
		.attr("stroke-width", this.config.getStrokewidth())
		.attr("fill", this.config.getBgcolor());
	this.item.attr("pointer-events", this.config.placeholder ? "none" : "all");
};

GraphPort.prototype.draw = function() {
	this.item.attr("r", this.config.getPortsize() / 2.0);
};

GraphPort.prototype.getId = function() {
	return this.id;
};

GraphPort.prototype.setId = function(id) {
	this.id = id;
	if(this.item)
		this.item.attr("id", this.id);
};

GraphPort.prototype.getName = function() {
	return getLocalName(this.id);
};

GraphPort.prototype.getItem = function() {
	return this.item;
};

GraphPort.prototype.getRole = function() {
	return this.role;
};

GraphPort.prototype.setRole = function(role) {
	this.role = role;
};

GraphPort.prototype.isInput = function() {
	return this.isInput;
};

GraphPort.prototype.setInput = function(isInput) {
	this.isInput = isInput;
};

GraphPort.prototype.getConfig = function() {
	return this.config;
};

GraphPort.prototype.setConfig = function(config) {
	this.config = config;
	this.configureItem();
};

GraphPort.prototype.getDuration = function() {
	return this.duration;
};

GraphPort.prototype.setDuration = function(duration) {
	this.duration = duration;
};

GraphPort.prototype.getCoords = function() {
	return this.coords;
};

GraphPort.prototype.getRealCoords = function() {
	return {
		x: this.coords.x + this.graphItem.coords.x,
		y: this.coords.y + this.graphItem.coords.y
	};
};

GraphPort.prototype.setCoords = function(coords) {
	this.coords = coords;
	this.item.attr("transform", "translate(" + coords.x + "," + coords.y + ")");
};

GraphPort.prototype.equals = function(p) {
	if (!p)
		return false;
	// if(this.id != p.id) return false;
	if (this.role.roleid != p.role.roleid)
		return false;
	return true;
};

//Translate variable ports to original node ports from where the variable comes
GraphPort.prototype.getComponentPort = function() {
	if(this.graphItem instanceof GraphVariable) {
		var v = this.graphItem;
		var links = this.graphItem.variableLinks;
		if(this.isInput) {
			if(links[0].toPort)
				return links[0].toPort;
			else
				return links[0].fromPort;
		}
		else {
			if(links[0].fromPort)
				return links[0].fromPort;
			else
				return links[0].toPort;
		}
	}
	else {
		return this;
	}
};
