function Reasoner(store) {
	this.complist = this.flattenComponents(store.components.tree);
	this.datalist = this.flattenData(store.data.tree);
	this.dtparents = this.getDatatypeParentMap(store.data.tree);
}

Reasoner.prototype.getComponentRoleType = function(cid, roleid) {
	var c = this.complist[cid];
	if(!c) return null;
	for(var i=0; i<c.inputs.length; i++) {
		if(roleid == c.inputs[i].role)
			return c.inputs[i].type;
	}
	for(var i=0; i<c.outputs.length; i++) {
		if(roleid == c.outputs[i].role)
			return c.outputs[i].type;		
	}
	return null;
};

Reasoner.prototype.flattenComponents = function(comptree) {
	var comps = {};
	var tmp = comptree.children.concat();
	while(tmp.length) {
		var c = tmp.pop();
		if(c.cls.component)
			comps[c.cls.component.id] = c.cls.component;
		if(c.children)
			for(var i=0; i<c.children.length; i++)
				tmp.push(c.children[i]);
	}
	return comps;
};

Reasoner.prototype.flattenData = function(data_root) {
	var data = {};
	var tmp = [data_root];
	while(tmp.length) {
		var d = tmp.pop();
		if(d.item) {
			data[d.item.id] = d.item;
		}
		if(d.children)
			for(var i=0; i<d.children.length; i++)
				tmp.push(d.children[i]);
	}
	return data;
};


Reasoner.prototype.getDatatypeParentMap = function(data_root, parents) {
	var parentmap = {};
	var id = data_root.item.id;
	if(!parents) parents = [];
	parentmap[id] = parents;
	var children = data_root.children;
	if(children) {
		for(var i=0; i<children.length; i++) {
			var cparents = parents.concat(); // duplicate
			cparents.push(id);
			var cmap = this.getDatatypeParentMap(children[i], cparents);
			for(var cid in cmap) parentmap[cid] = cmap[cid];
		}
	}
	return parentmap;
};

Reasoner.prototype.typeSubsumesType = function(type1, type2) {
	if(type1==type2) return true;
	var parents = this.dtparents[type2];
	if(!parents) return false;
	for(var i=0; i<parents.length; i++) {
		if(parents[i] == type1) return true;
	}
	return false;
};

