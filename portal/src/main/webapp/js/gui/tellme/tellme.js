function TellMe(guid, tid, tname, tabPanel, mainPanel, opts, store, 
		run_url, op_url, nsmap) {
	this.guid = guid;
	this.tid = tid;
	this.tname = tname;
	this.tabPanel = tabPanel;
	this.mainPanel = mainPanel;
	this.opts = opts;
	this.store = store;
	this.run_url = run_url;
	this.op_url = op_url;
	this.nsmap = nsmap;
	
	this.mainPanel = null;
	this.tellmeCombo = null;
	this.historyPanel = null;
	this.beamer = null;
	this.complist = {};
	this.datalist = {};
	this.dtparents = {};
	
	this.cutoff = 10;
}

TellMe.prototype.initialize = function() {
	this.beamer = new TodoListParser(this.store.beamer_paraphrases, 
			this.store.beamer_mappings);
	this.complist = this.flattenComponents(this.store.components.tree);
	this.datalist = this.flattenData(this.store.data.tree);
	this.dtparents = this.getDatatypeParentMap(this.store.data.tree);

	var comboStore = new Ext.data.Store({fields:['name']});
	this.tellmeCombo = new Ext.form.ComboBox({
		region:'north', 
		border:false, 
		displayField:'name', 
		store:comboStore, 
		enableKeyEvents:true, 
		hideTrigger:true, 
		queryMode: 'local',
		listeners: {
			blur: function() {
				//this.focus();
			},
			specialkey: function(cb, e) {
				if(!This.beamer) return;
				if(e.getKey() == e.DOWN) {
					var text = this.getRawValue();
					if(This.beamer.curtext != text) {
						This.loadBeamerPossibleSentences(text, this.store);
					}
				}
			},
			keypress: function(cb, e) {
				if(!This.beamer) return;
				if(e.getKey() == e.ENTER) {
					This.parseTellMeInstruction(e); 
				}
			},
			change: function(cb, newval, oldval) {
				if(!This.beamer) return;
				if(!oldval || newval[newval.length-1] == ' ') {
					This.loadBeamerPossibleSentences(newval, this.store);
				}
			}
		}
	});
	
	var This = this;
	this.historyPanel = new Ext.Panel({
		layout:'card', 
		region: 'center', 
		border:false, 
		layoutOnCardChange:true, 
		tellme: This
	});
	
	this.mainPanel = new Ext.Panel({
		title: 'TellMe',
		border: false,
		iconCls: 'icon-chat fa-title fa-blue',
		layout: 'border',
		items: [ This.tellmeCombo, This.historyPanel ]
	});
	return this.mainPanel;
}

TellMe.prototype.clear = function() {
	var hp = this.historyPanel.getLayout().activeItem;
	var hpTree = hp.getComponent('tellmeHistoryTreePanel');
	var hpDetail = hp.getComponent('tellmeHistoryDetailPanel');
	while(hpTree.root.firstChild) {
		hpTree.root.removeChild(hpTree.root.firstChild);
	}
	hpDetail.updateDetail({teacher:'', student:'', log:''});
}

TellMe.prototype.typeSubsumesType = function(type1, type2) {
	type1 = getLocalName(type1);
	type2 = getLocalName(type2);
	if(type1==type2) return true;
	var parents = this.dtparents[type2];
	if(!parents) return false;
	for(var i=0; i<parents.length; i++) {
		if(parents[i] == type1) return true;
	}
	return false;
}

TellMe.prototype.matchComponentRoles = function(rs, roles, isInput) {
	var tmp = roles.concat();
	for(var i=0; i<rs.length; i++) {
		var r = rs[i];
		var match = false;
		var tmp2 = [];
		for(var j=0; j<tmp.length; j++) {
			tmp2.push(tmp[j]);
			var type = tmp[j].type;
			if(!match) {
				var typeMatch = true;
				if(isInput && !this.typeSubsumesType(type, r)) 
					typeMatch = false;
				else if(!isInput && !this.typeSubsumesType(r, type)) 
					typeMatch = false;
				match = typeMatch;
				if(match) tmp2.pop();
			}
		}
		tmp = tmp2;
		if(!match) {
			return false;
		}
	}
	return true;
}

TellMe.prototype.findMatchingComponents = function(comps, inputs, outputs) {
	var clist = [];
	for(var cid in comps) {
		var c = comps[cid];
		if(!this.matchComponentRoles(inputs, c.inputs, true)) continue;
		if(!this.matchComponentRoles(outputs, c.outputs, false)) continue;
		clist.push(cid);
	}
	return clist;
}

TellMe.prototype.checkComponentIO = function(comps, cid, inputs, outputs) {
	var c = comps[cid];
	var rank = 0;
	if(c) {
		if(!this.matchComponentRoles(inputs, c.inputs, true)) return {rank:0, comps:[]};
		if(!this.matchComponentRoles(outputs, c.outputs, false)) return {rank:0, comps:[]};
		rank = 1;
	}
	return {rank:rank, comps:[cid]};
}

TellMe.prototype.fillDataDetails = function(matches, data) {
	for(var i=0; i<matches.length; i++) {
		var m = matches[i];
		for(var j=0; j<m.tasks.length; j++) {
			var t = m.tasks[j];
			for(var arg in t._databindings) {
				var b = t._databindings[arg];
				var d = data[b.name];
				b.role = d ? d.type : 'Plain';
				if(b.role != 'Plain') m.rank++;
				t._databindings[arg] = b;
			}
		}
	}
	return matches;
}

TellMe.prototype.fillMissingComponents = function(matches, comps) {
	var nmatches = [];
	for(var i=0; i<matches.length; i++) {
		var m = matches[i];
		var invalid = false;
		for(var j=0; j<m.tasks.length; j++) {
			var t = cloneObj(m.tasks[j]);
			if(!t._taskname.match(/^C_/)) continue;
			var inputs = []; var outputs = [];
			for(var b in t._databindings) {
				var db = t._databindings[b];
				if(db && db.role == 'Datatype') {
					if(b.match(/^ip/)) inputs.push(db.name);
					if(b.match(/^op/)) outputs.push(db.name);
				}
			}
			if(!t._component) {
				t._component = this.findMatchingComponents(comps, inputs, outputs);
				m.rank++;
			}
			else {
				var c = this.checkComponentIO(comps, t._component, inputs, outputs);
				m.rank += c.rank;
				t._component = c.comps;
			}
			if(!t._component.length) {
				invalid = true; break;
			}
			m.tasks[j] = t;
		}
		if(!invalid) nmatches.push(m);
	}
	return nmatches;
}

TellMe.prototype.flattenComponents = function(comptree) {
	var comps = {};
	var tmp = comptree.children.concat();
	while(tmp.length) {
		var c = tmp.pop();
		if(c.cls.component)
			comps[getLocalName(c.cls.component.id)] = c.cls.component;
		if(c.children)
			for(var i=0; i<c.children.length; i++)
				tmp.push(c.children[i]);
	}
	return comps;
}

TellMe.prototype.flattenData = function(data_root) {
	var data = {};
	var tmp = [data_root];
	while(tmp.length) {
		var d = tmp.pop();
		if(d.item) {
			data[getLocalName(d.item.id)] = {name:d.item.id, type:(d.item.type == 1 ? 'Datatype':'Data')};
		}
		if(d.children)
			for(var i=0; i<d.children.length; i++)
				tmp.push(d.children[i]);
	}
	return data;
}


TellMe.prototype.getDatatypeParentMap = function(data_root, parents) {
	var parentmap = {};
	var id = getLocalName(data_root.item.id);
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
}

TellMe.prototype.loadBeamerPossibleSentences = function(text, comboStore) {
	var spindex = text.lastIndexOf(' ') + 1;
	if(spindex) text = text.substr(0, spindex);
	this.beamer.curtext = text;
	var values = this.beamer.getPossibleSentences(text, this.cutoff);

	var opts = [];
	for(var i=0; i<values.length; i++) opts[i] = {name:values[i]};
	comboStore.loadData(opts);
}

TellMe.prototype.parseTellMeInstruction = function(e) {
	var tp = this.historyPanel.getLayout().activeItem;
	var done = false;
	var code = e.getCharCode();
	var c = String.fromCharCode(code);
	var otext = this.tellmeCombo.getRawValue();
	var text = this.tellmeCombo.getRawValue() + c;
	var gp = tp.templatePanel.mainTab.graphPanel;
	text = this.tellmeCombo.getRawValue();
	if(!text) return;
	
	// Mix in current template variables
	var tdatalist = {};
	for(var d in this.datalist) tdatalist[d] = this.datalist[d];
	for(var v in gp.editor.template.variables) 
		tdatalist[getLocalName(v)] = {name:v, type:'Variable'};

	this.mainPanel.getEl().mask(
			'Trying to understand what you said ...', 'x-mask-loading');
	
	var This = this;
	Ext.Function.defer(function() {
		if(This.beamer.isEnd(text)) {
			This.elaborateTemplate(text, gp.editor);
			This.mainPanel.getEl().unmask();
			return;
		}
		var matches = This.beamer.findMatches(text);
		// Fill details of the data from data symbols (variables, datatypes, data instances)
		matches = This.fillDataDetails(matches, tdatalist);
		// If steps are missing components, then get matching components
		matches = This.fillMissingComponents(matches, This.complist);
		// Normalize any multiple component matches that may have been found in the previous step
		matches = This.beamer.normalizeComponentMatches(matches);
		// Apply the matches
		This.applyBeamerMatches(text, matches, This.complist, tdatalist, gp, gp.editor);
		This.mainPanel.getEl().unmask();
		This.tellmeCombo.focus();
		//if(window.console) window.console.log(matches);
	}, 100, this);

	// TODO: 
	// - If components or datatypes don't exist - add them as is
	// - Check for Template Issues
}


TellMe.prototype.getTellMeHistory = function(panel) {
	var hp = this.historyPanel.getLayout().activeItem;
	var hpTree = hp.getComponent('tellmeHistoryTreePanel');
	var root = hpTree.getRootNode();
	var froot = this.filterTree(root);

	var sels = hpTree.getSelectionModel().getSelection();
	froot.selected = (sels && sels.length) ? sels[0].data.id : null;

	var str = Ext.JSON.encode(froot);
	return str;
}

TellMe.prototype.filterTree = function(tn) {
	var node = {};
	var data = tn.data.template ? tn.data : tn.raw;
	node.id = data.id;
	node.cls = data.cls;
	node.iconCls = data.iconCls;
	if(node.iconCls == 'icon-chat fa fa-blue') {
		node.status = data.status;
		node.student = data.student;
		node.teacher = data.teacher;
		node.text = data.text;
	}
	if(node.iconCls == 'icon-wflow-alt fa fa-blue' && data.template) {
		var t = data.template.createCopy();
		node.template = cloneObj(t.store);
	}
	if(tn.childNodes.length) {
		node.children = [];
		for(var i=0; i<tn.childNodes.length; i++) {
			node.children.push(this.filterTree(tn.childNodes[i]));
		}
	} else {
		node.leaf = true;
	}
	return node;
}

TellMe.prototype.getTellMeRoot = function(panel) {
	var hp = this.historyPanel.getLayout().activeItem;
	var hpTree = hp.getComponent('tellmeHistoryTreePanel');
	return hpTree.getRootNode();
}

TellMe.prototype.setTellMeRoot = function(panel, root) {
	var hp = this.historyPanel.getLayout().activeItem;
	var hpTree = hp.getComponent('tellmeHistoryTreePanel');
	hpTree.setRootNode(root);
	hpTree.render();
	root.expand();
}

TellMe.prototype.loadTellMeHistory = function(tree) {
	if(!tree) tree = {};
	var hp = this.historyPanel.getLayout().activeItem;
	var hpTree = hp.getComponent('tellmeHistoryTreePanel');
	var root = hpTree.getStore().getRootNode();
	while(root.firstChild) {
		root.removeChild(root.firstChild);
	}
	var gp = hp.templatePanel.mainTab.graphPanel;
	this.loadTellMeTree(tree, null, root, gp.editor);
	root.expand();

	if(tree.selected) {
		var rec = hpTree.getStore().getNodeById(tree.selected);
		if(rec) hpTree.getSelectionModel().select(rec, false, true);
	}
}


TellMe.prototype.loadTellMeTree = function(tn, p, pn, ed) {
	var node;
	var nodeObj;
	if(p) {
		if(tn.template) {
			var t = null;
			if(p) t = new Template(tn.id, tn.template, ed);

			nodeObj = {teacher:p?p.teacher:'', student:p?p.student:'', log:'', 
						status:p?p.status:'', text:getLocalName(tn.id), id:tn.id, 
						template:t, 
						leaf:tn.children?true:false, iconCls: tn.iconCls, expanded:true};
		}
		else {
			nodeObj = {id:tn.id, iconCls:tn.iconCls, 
						cls: tn.cls, text:tn.text, teacher:tn.teacher, student:tn.student, log:'', 
						status:tn.status, expanded:true};
		}
		if(nodeObj) {
			pn.data.leaf = false;
			node = pn.appendChild(nodeObj);
		}
	}
	else node = pn;

	if(node && tn.children) {
		for(var i=0; i<tn.children.length; i++) {
			var cn = tn.children[i];
			this.loadTellMeTree(cn, tn, node, ed);
		}
	}
}


TellMe.prototype.addToHistory = function(hpTree, hpDetail, curT, teacher, student, log, templates, status, icon) {
	var rn = hpTree.getRootNode();
	var curNode = rn.data.id==curT.txid ? rn : rn.findChild('id', curT.txid, true);
	var curTxid = curT.txid + "." + (curNode.childNodes.length+1);

	var curText = curNode.data.text;
	var utObj = {id:curTxid+".utterance", iconCls: icon?icon:'icon-chat fa fa-blue', 
							cls: 'tellmestudent_'+status,
							text:teacher, teacher:teacher, student:student, log:log, 
							status:status, expanded:true};

	curNode.data.leaf = false;
	var utNode = curNode.appendChild(utObj);

	var tNode, t;
	var firstNode, firstTemplate;
	if(templates && templates.length) {
		utNode.data.leaf = false;
		for(var i=1; i<=templates.length; i++) {
			t = templates[i-1];
			var txid = curTxid+"."+i;
			var tObj = {teacher:teacher, student:student, log:log, status:status,
								text:curText+"."+i, id:txid, template:t, leaf:true, 
								iconCls: 'icon-wflow-alt fa fa-blue'};
			t.txid = txid;
			tNode = utNode.appendChild(tObj);
			if(i==1) {
				firstNode = tNode;
				firstTemplate = t;
			}
		}
	}
	if(tNode && t) {
		hpTree.selectPath(getTreePath(firstNode, 'text'), 'text');
		var data = firstNode.data.template ? firstNode.data : firstNode.raw;
		hpDetail.updateDetail(data);
		this.showTemplate(firstTemplate, true);
	}
	else {
		hpTree.selectPath(getTreePath(utNode, 'text'), 'text');
		var data = utNode.data.template ? utNode.data : utNode.raw;
		hpDetail.updateDetail(data);
	}
	curNode.expand();
	return utNode;
}

TellMe.prototype.copyCompVarBindings = function(t1, t2) {
	t2.compBindings = {};
	t2.varBindings = {};
	t2.editorActions = [];
	for(var i in t1.compBindings) t2.compBindings[i] = t2.nodes[t1.compBindings[i]];
	for(var i in t1.varBindings) t2.varBindings[i] = t2.variables[t1.varBindings[i]];
	for(var i in t1.editorActions) t2.editorActions[i] = t1.editorActions[i];
}

TellMe.prototype.getPortNameForDatatype = function(datatype, roles) {
	for(var i=0; i<roles.length; i++) {
		var r = roles[i];
		if(r.param) continue;
		if(this.typeSubsumesType(r.type, datatype)) 
			return r.role;
	}
	return null;
};

TellMe.prototype.getPortDatatype = function(portname, roles) {
	for(var i=0; i<roles.length; i++) {
		var r = roles[i];
		if(r.param) continue;
		if(r.role == portname) return r.type;
	}
	return null;
};

TellMe.prototype.getDataPortByIndex = function(roles, index) {
	var count = 0;
	for(var i=0; i<roles.length; i++) {
		var r = roles[i];
		if(!r.param) {
			if(count == index) return r.role;
			count++;
		}
	}
	return null;
}

TellMe.prototype.searchForMatchingInputVariables = function(n, opvars, ipvars, template, dtype) {
	var vs = {};
	var vlist = [];
	for (var i=opvars.length-1; i>=0; i--) {
		var v = template.variables[opvars[i]];
		if(!v) continue;
		var vtype = template.getVariableType(v);
		if(!vs[v.id] && (!dtype || this.typeSubsumesType(dtype, vtype))) {
			vs[v.id] = true;
			vlist.push(v);
		}
	}
	for (var i=ipvars.length-1; i>=0; i--) {
		var v = template.variables[ipvars[i]];
		if(!v) continue;
		var vtype = template.getVariableType(v);
		if(!vs[v.id] && (!dtype || this.typeSubsumesType(dtype, vtype))) {
			vs[v.id] = true;
			vlist.push(v);
		}
	}
	var tvars = template.variables;
	for (var vid in tvars) {
		var v = tvars[vid];
		if(v.type != "DATA") continue;
		var vtype = template.getVariableType(v);
		if(!vs[v.id] && (!dtype || this.typeSubsumesType(dtype, vtype))) {
			vs[v.id] = true;
			vlist.push(v);
		}
	}
	return vlist;
}

TellMe.prototype.addNodeIOToTemplate = function(n, data, isInput, template, cbyid, datalist, ipvars, opvars, seln, selv, ind) {
	var port;
	var c = cbyid[getLocalName(n.binding.id)];
	if(!c) return null;

	var roles = isInput ? c.inputs : c.outputs;

	var v;
	var dtype;

	if(data.role == 'Datatype' || data.role == 'Variable' || data.type == 'Selected') {
		dtype = data.name;
		if(data.role == 'Variable' || data.type == 'Selected') {
			if (data.type == "Selected" && selv) v = template.variables[selv.id];
			else if(data.role == 'Variable') v = template.variables[data.name];
			if(!v) return null;
			dtype = template.getVariableType(v);
		}
		if(!dtype) return null;
		var pname = this.getPortNameForDatatype(dtype, roles);
		if(!pname) return null;
		port = isInput ? n.getInputPortByName(pname) : n.getOutputPortByName(pname);
	} else if(data.role == 'Plain') {
		port = isInput ? n.getInputPortByName(data.name) : n.getOutputPortByName(data.name);
	}
	if(!port && data.role == 'Plain') {
		ind = ind ? ind: 0;
		//if(window.console) window.console.log("Warning: Underspecified data "+data.name+". Trying to use port "+ind);
		var pname = this.getDataPortByIndex(roles, ind);
		if(!pname) return null;
		port = isInput ? n.getInputPortByName(pname) : n.getOutputPortByName(pname);
	}
	if(!port) return null;

	var newvariable = false;
	if(data.role == 'Variable') {
		template.editorActions.push("Using existing variable "+v.id);
	} 
	else if (isInput && data.type.match(/^Specified/)) {
		var vlist = this.searchForMatchingInputVariables(n, opvars, ipvars, template, dtype);
		if(vlist.length) {
			v = vlist[0];
			// FIXME: Just picking the first match for now ! Should create template copies here
			template.editorActions.push("Using previously created variable "+v.id);
		}
		else
			return null;
	}
	else if (data.type == "Selected") {
		template.editorActions.push("Using user selected variable "+v.id);
	}

	if(!v && data.name) {
		var vname = data.name.replace(/\s+/g,'_');
		v = template.addVariable(data.role=='Plain' ? vname : port.name);

		if(data.role=='Plain') v.setIsUnknown(true);

		template.editorActions.push("Creating new variable "+getLocalName(v.id));
		if(data.type.match(/Collection/)) {
			v.setDimensionality(1);
			template.editorActions.push("Marking the variable as a collection");
		}
		v.type = port.type;
		newvariable = true;
	}

	if(!v) return null;

	if(data.prop && data.val) {
		var propid = this.nsmap['dcdom'] + data.prop;
		template.setVariablePropertyValue(v, propid, data.val);
	}

	if(newvariable) {
		isInput ? template.addInputLink(port, v) : template.addOutputLink(port, v);
		if(data.role == 'Datatype') 
			template.setVariableType(v, datalist[data.name].name);
		else 
			template.setVariableType(v, this.getPortDatatype(port.name, roles));
	} else {
		var vtype = template.getVariableType(v);
		var ptype = this.getPortDatatype(port.name, roles);
		if(ptype && isInput && !this.typeSubsumesType(ptype, vtype))
			return null;
		if(ptype && !isInput && !this.typeSubsumesType(vtype, ptype))
			return null;

		var ls = template.getLinksWithVariable(v);
		for(var i=0; i<ls.length; i++) {
			var l = ls[i];
			if(!l.toNode && isInput) template.changeLinkDestination(l, port);
			else if(!l.fromNode && !isInput) template.changeLinkOrigin(l, port);
			else if(isInput) template.addInputLink(port, v);
			else template.addOutputLink(port, v);
		}
	}
	return v;
}

TellMe.prototype.addNodeInputToTemplate = function(n, ip, template, cbyid, datalist, ipvars, opvars, seln, selv, ind) {
	return this.addNodeIOToTemplate(n, ip, true, template, cbyid, datalist, ipvars, opvars, seln, selv, ind); 
}

TellMe.prototype.addNodeOutputToTemplate = function(n, op, template, cbyid, datalist, ipvars, opvars, seln, selv, ind) {
	return this.addNodeIOToTemplate(n, op, false, template, cbyid, datalist, ipvars, opvars, seln, selv, ind); 
}

TellMe.prototype.removeItemsFromTemplate = function(xitem, template, seln, selv) {
	var name = xitem.name;
	if (xitem.type == "Selected") {
		if(selv) name = selv.id;
		if(seln) name = seln.id;
	}
	//window.console.log(name);
	var x = template.variables[name];
	if(x) {
		template.removeVariable(x);
		return true;
	}
	x = template.nodes[name];
	if(x) {
		template.removeNode(x);
		return true;
	}
	return false;
}

TellMe.prototype.mergeVariablesInTemplate = function(xv1, xv2, template, cbyid, datalist, seln, selv) {
	var v1 = template.variables[xv1.name];
	var v2 = template.variables[xv2.name];
	if (!v1 && xv1.type == "Selected" && selv) v1 = template.variables[selv.id];
	if (!v2 && xv2.type == "Selected" && selv) v2 = template.variables[selv.id];

	if(!v1 || !v2) return null;
	if(v1.unknown) {
		var tmp = v1; v1 = v2; v2 = tmp;
	}
	if(v1.type != v2.type) return null;
	if(v1.type == "DATA") {
		var v1type = template.getVariableType(v1);
		var v2type = template.getVariableType(v2);
		if(!this.typeSubsumesType(v1type,v2type) && 
			!this.typeSubsumesType(v2type,v1type))
			 return null;

		// Keep this more specific variable
		if(v1type != v2type && this.typeSubsumesType(v2type, v1type)) {
			var tmp = v1; v1 = v2; v2 = tmp;
		}
	}

	var ls1 = template.getLinksWithVariable(v1);
	for(var i=0; i<ls1.length; i++) {
		var l1 = ls1[i];
		if(l1.fromPort) 
			template.addLinkToVariable(l1.fromPort, v2.getInputPort());
		if(l1.toPort)
			template.addLinkToVariable(v2.getOutputPort(), l1.toPort);
	}

	for(var i=0; i<template.store.constraints.length; i++) {
		var cons = template.store.constraints[i];
		if(cons.s == v1.id || cons.o == v1.id) {
			Ext.Array.remove(template.store.constraints, cons);
			i--;
		}
	}
	return v1;
}

TellMe.prototype.applyBeamerActionsToTemplate = function(template, tasks, cbyid, datalist, seln, selv, index, ipvars, opvars) {
	var opvars = ipvars ? ipvars : [];
	var ipvars = opvars ? opvars : [];
	if(!index) index=0;
	var templateQ = [template];
	for(var i=index; i<tasks.length; i++) {
		var task = tasks[i];
		var args = task._databindings;
		var comp = task._component;
		var fn = task._taskname.replace(/\s+.+/,'');
		var n = null;
		//window.console.log(fn);

		if(fn == "C_AddNode") {
			var c = cbyid[comp];
			if(!c) return null;
			n = template.addNode(c);
			n.setPortRule({type:'STYPE', expr: n.prule.expr}); // S type for TellMe Nodes
			template.editorActions.push("Creating new node: "+comp);
		}
		else if(fn == "C_AddNodeWithInput" || fn == "C_AddNodeWithOutput" || 
				fn == "C_AddNodeWithInputOutput" || fn == "C_AddNodeWith2Inputs") {
			var c = cbyid[comp];
			if(!c) return null;
			n = template.addNode(c);
			n.setPortRule({type:'STYPE', expr: n.prule.expr}); // S type for TellMe Nodes

			template.editorActions.push("Creating new node: "+comp);
			if(fn == "C_AddNodeWithInput" || fn == "C_AddNodeWithInputOutput") {
				var ip = args['ip'];
				if(!ip) return null;
				var v = this.addNodeInputToTemplate(
						n, ip, template, cbyid, datalist, ipvars, opvars, seln, selv);
				if(!v) return null;
				//ipvars.push(v);
			}
			if(fn == "C_AddNodeWithOutput" || fn == "C_AddNodeWithInputOutput") {
				var op = args['op'];
				if(!op) return null;
				var v = this.addNodeOutputToTemplate(
						n, op, template, cbyid, datalist, ipvars, opvars, seln, selv);
				if(!v) return null;
				//opvars.push(v);
			}
			if(fn == "C_AddNodeWith2Inputs") {
				var ip1 = args['ip1'];
				var ip2 = args['ip2'];
				if(!ip1 || !ip2) return null;
				var v1 = this.addNodeInputToTemplate(
						n, ip1, template, cbyid, datalist, ipvars, opvars, seln, selv);
				var v2 = this.addNodeInputToTemplate(
						n, ip2, template, cbyid, datalist, ipvars, opvars, seln, selv, 1);
				if(!v1 || !v2) return null;
				//ipvars.push(v1);
				//ipvars.push(v2);
			}
		}
		if(fn == "V_MergeVariables") {
			var v1 = args['v1'];
			var v2 = args['v2'];
			if(!v1 || !v2) return null;
			var v = this.mergeVariablesInTemplate(
					v1, v2, template, cbyid, datalist, seln, selv);
			if(!v) return null;
			template.editorActions.push("Merging variables "+v1.name+" and "+v2.name);
		}
		if(fn == "X_RemoveItem") {
			var x = args['x'];
			if(!x) return null;
			var ok = this.removeItemsFromTemplate(x, template, seln, selv);
			if(!ok) return null;
			template.editorActions.push("Removing "+x.name);
		}
		if(n) {
			template.bindUnboundNodeIO(n, cbyid[getLocalName(n.binding.id)]);
			var ilinks = n.getInputLinks();
			var olinks = n.getOutputLinks();
			for(var j=0; j<ilinks.length; j++) ipvars.push(ilinks[j].variable.id);
			for(var j=0; j<olinks.length; j++) opvars.push(olinks[j].variable.id);

    		var tightQ = this.getTightenedTemplates(template, n, 
    				cbyid, datalist);
			for(var j=0; j<tightQ.length; j++) {
				// Apply rest of the beamer action tasks on these tightened templates (notice index is now i+1)
				var tQ = this.applyBeamerActionsToTemplate(tightQ[j], tasks, 
						cbyid, datalist, seln, selv, i+1, ipvars, opvars);
				if(!tQ) continue;
				templateQ = tQ.concat(templateQ);
			}
		}
	}
	/*for(var nid in template.nodes) {
		var n = template.nodes[nid];
		template.bindUnboundNodeIO(n, cbyid[n.binding.id]);
	}*/

	var ntQ = [];
	for(var i=0; i<templateQ.length; i++) {
		templateQ[i].markErrors();
		var num_errors = 0;
		for(var errid in templateQ[i].errors) num_errors++;
		if(!num_errors) {
			templateQ[i].forwardSweep();
			ntQ.push(templateQ[i]);
		}
	}

	return ntQ;
}

TellMe.prototype.findMatchingVariablesInTemplateForType = function(t, type, nid, vid, isInput) {
	var vars=[];

	var vdone = {};
	for(var lid in t.links) {
		var l = t.links[lid];
		var vt = l.variable;
		if(!vt || vt.id==vid || vdone[vt.id] || vt.type=="PARAM") continue;
		vdone[vt.id] = 1;

		var vtype = t.getVariableType(vt);
		if(!vtype) continue;

		if(isInput && l.fromNode && l.fromNode.id != nid && 
				this.typeSubsumesType(type, vtype)) 
			vars.push(vt.id);

		if(isInput && l.toNode && l.toNode.id != nid && !l.fromNode && type == vtype) 
			vars.push(vt.id);

		if(!isInput && !l.fromNode && l.toNode && l.toNode.id != nid && 
				this.typeSubsumesType(vtype, type)) 
			vars.push(vt.id);
	}
	return vars;
}

TellMe.prototype.findMatchingVariablesInTemplate = function(t, n, v, isInput) {
	var vars=[];

	var type = t.getVariableType(v);
	if(!type) return vars;

	return this.findMatchingVariablesInTemplateForType(t, type, n.id, v.id, isInput);
}

TellMe.prototype.getTightenedTemplates = function(template, node, cbyid, datalist) {
	var vms = {};
	var cvms = {};
	var vmaps = {};
	var ilinks = node.getInputLinks();
	var olinks = node.getOutputLinks();

	// Check if any input variable can be fed by any other variable in the template
	for(var i=0; i<ilinks.length; i++) {
		var l = ilinks[i];
		var v = l.variable;
		if(!v || vms[v.id] || v.type=="PARAM") continue;
		vms[v.id] = [];
		cvms[v.id] = [];
		if(!l.fromPort) {
			var tmp = this.findMatchingVariablesInTemplate(template, node, v, true);
			for(var j=0; j<tmp.length; j++) {
				var map = v.id+","+tmp[j];
				var rmap = tmp[j]+","+v.id;
				if(vmaps[rmap]) continue;
				vmaps[map] = true;
				vms[v.id].push(tmp[j]);
			}

			// If there are no direct variable matches, see if we can add a component to create a match
			if(!tmp.length) {
				var type = template.getVariableType(v);
				// Find components that can be fed to the variable type
				var cmps = this.findMatchingComponents(cbyid, datalist, [], [type]);

				for(var j=0; j<cmps.length; j++) {
					var c = cbyid[cmps[j]];
					// Get matching coutrole
					var coutrole = null;
					for(var k=0; k<c.outputs.length; k++) {
						var couttype = c.outputs[k].type;
						if(this.typeSubsumesType(type, couttype))
							coutrole = c.outputs[k].role;
					}
					if(!coutrole) continue;

					for(var k=0; k<c.inputs.length; k++) {
						var cin = c.inputs[k];
						var cintype = cin.type;

						// Check if this component's input can be linked from an existing output
						var tmp = this.findMatchingVariablesInTemplateForType(template, cintype, node.id, v.id, true);
						//console.log(tmp);

						// Add a task to add the component and then add the link
						for(var l=0; l<tmp.length; l++) {
							var map = v.id+","+c.id+","+cin.role+","+coutrole+","+tmp[l];
							var rmap = tmp[l]+","+coutrole+","+cin.role+","+c.id+","+v.id;
							if(vmaps[rmap]) continue;
							vmaps[map] = true;
							cvms[v.id].push({c:c, crole:cin.role, coutrole:coutrole, vid:tmp[l]});
						}
					}
				}
			}
		}
	}

	// Check if any output variable can be fed to any other variable in the template
	for(var i=0; i<olinks.length; i++) {
		var l = olinks[i];
		var v = l.variable;
		if(!v || vms[v.id] || v.type=="PARAM") continue;
		vms[v.id] = [];
		if(!l.toPort) {
			var tmp = this.findMatchingVariablesInTemplate(template, node, v, false, datalist);
			for(var j=0; j<tmp.length; j++) {
				var map = v.id+","+tmp[j];
				var rmap = tmp[j]+","+v.id;
				if(vmaps[rmap]) continue;
				vmaps[map] = true;
				vms[v.id].push(tmp[j]);
			}
		}
	}

	//if(window.console) window.console.log(vms);

	var tQ = [template];
	var nochange = true;

	// Look at variable merges
	for(vid in vms) {
		var mvids = vms[vid];
		if(!mvids.length) continue;

		var ntQ = [];
		var len = tQ.length;
		for(var j=0; j<len; j++) {
			var tmp = tQ[j];
			for(var i=0; i<mvids.length; i++) {
				var tmp2 = tmp.createCopy();
				this.copyCompVarBindings(tmp, tmp2);
				var v = this.mergeVariablesInTemplate({name:vid}, {name:mvids[i]}, tmp2, null, datalist, null, null);
				if(v) {
					tQ.push(tmp2);
					nochange = false;
				}
			}
		}
	}

	// Look at component additions + variable merges
	for(vid in cvms) {
		var cmvids = cvms[vid];
		if(!cmvids.length) continue;

		var ntQ = [];
		var len = tQ.length;
		for(var j=0; j<len; j++) {
			var tmp = tQ[j];
			for(var i=0; i<cmvids.length; i++) {
				var xc = cmvids[i];
				var tmp2 = tmp.createCopy();
				this.copyCompVarBindings(tmp, tmp2);

				var tmpn = tmp2.addNode(xc.c);
				tmpn.setPortRule('S', tmpn.pruleOp); // S type for TellMe Nodes
				tmp2.editorActions.push("Creating new -connecting- node: "+xc.c.id);
				tmp2.bindUnboundNodeIO(tmpn, xc.c);

				var mv = null;
				var ilinks = tmpn.getInputLinks();
				for(var j=0; j<ilinks.length; j++) {
					if(ilinks[j].toPort.name == xc.crole) 
						mv = ilinks[j].variable;
				}
				if(!mv) continue;

				var v = this.mergeVariablesInTemplate({name:mv.id}, {name:xc.vid}, tmp2, null, datalist, null, null);
				if(!v) continue;

				var mv = null;
				var olinks = tmpn.getOutputLinks();
				for(var j=0; j<olinks.length; j++) {
					if(olinks[j].fromPort.name == xc.coutrole) 
						mv = olinks[j].variable;
				}
				if(!mv) continue;
				var v = this.mergeVariablesInTemplate({name:mv.id}, {name:vid}, tmp2, null, datalist, null, null);
				if(!v) continue;

				tQ.push(tmp2);
				nochange = false;
			}
		}
	}

	tQ.splice(0,1);
	return tQ;
}

TellMe.prototype.elaborateTemplate = function(text, ed) {
	var hp = this.historyPanel.getLayout().activeItem;
	var hpTree = hp.getComponent('tellmeHistoryTreePanel');
	var hpDetail = hp.getComponent('tellmeHistoryDetailPanel');

	this.tellmeCombo.setValue('');

	var t = ed.template.createCopy();
	t.txid = ed.template.txid ? ed.template.txid : ed.template.id;
	this.addToHistory(hpTree, hpDetail, t, text, "Template elaboration", '', null, 'ok');

	this.inferElaboratedTemplate();
}

TellMe.prototype.getUniqueTemplates = function(tQ) {
	var temp = [];
	for(var i=0;i<tQ.length;i++){
		if(!this.templateArrContains(temp, tQ[i])) {
			temp.push(tQ[i]);
		}
	}
	return temp;
}

TellMe.prototype.templateArrContains = function(arr, t) {
	for(var j=0;j<arr.length;j++)
		if(arr[j].equals(t)) return true;
	return false;
}

TellMe.prototype.applyBeamerMatches = function(text, matches, 
		complist, tdatalist, gp, ed) {
	var hp = this.historyPanel.getLayout().activeItem;
	var hpTree = hp.getComponent('tellmeHistoryTreePanel');
	var hpDetail = hp.getComponent('tellmeHistoryDetailPanel');
	
	this.tellmeCombo.setValue('');
	//_console(matches);

	var tQ = [];
	var num = 0;
	_console("Found "+matches.length+" possibilities");

	var t = ed.template.createCopy();
	t.txid = ed.template.txid ? ed.template.txid : ed.template.id;

	var root = hpTree.getStore().getRootNode();
	if(!root.data.template) root.data.template = t;

	for(var i=0; i<matches.length; i++) {
		_console("- Exploring match "+i);
		var m = matches[i];
		//if(window.console) window.console.log(m);
		var t = ed.template.createCopy();
		t.txid = ed.template.txid ? ed.template.txid : ed.template.id;
		t.editorActions = [];

		var br = "\n";
		var tab = "  ";
		var log = "Looking for matching patterns...";

		var tasks = m.tasks;
		if(!tasks.length) {
			log += br+"ERROR: Cannot Understand";
			this.addToHistory(hpTree, hpDetail, t, text, 
					"Cannot Understand", log, null, 'error');
			return;
		}
		var seln = ed.getSelectedNode();
		var selv = ed.getSelectedVariable();
		var ts = this.applyBeamerActionsToTemplate(t, tasks, 
				complist, tdatalist, seln, selv);
		if(ts && ts.length) {
			num += ts.length;
			tQ = tQ.concat(ts);
		}
		if(num > 20) break;
		if(i > 100) break;
	}
	tQ = this.getUniqueTemplates(tQ);
	tQ = tQ.splice(0,20);
	tQ.sort(function(a,b) {return a.getNumLinks() - b.getNumLinks();});

	log += br+"Found "+tQ.length+" alternative templates";
	for(var i=0; i<tQ.length; i++) {
		var t2 = tQ[i];
		if(t2.editorActions && t2.editorActions.length) {
			log += br+tab+"- Template "+(i+1)+" matched instruction to these editor actions:";
			for(var j=0; j<t2.editorActions.length; j++) {
				log += br+tab+tab+(j+1)+". "+t2.editorActions[j];
			}
		}
	}
	if(!tQ.length) {
		log += br+"ERROR: No matches found";
		this.addToHistory(hpTree, hpDetail, t, text, "No alternatives", log, null, 'error');
		return;
	}
	var msg = "OK";
	if(tQ.length > 1) msg+= " ("+tQ.length+" alternatives)";

	log += br+"Applying actions to Workflow Sketch...";
	var trec = this.addToHistory(hpTree, hpDetail, t, text, msg, log, tQ, 'ok');

	//var selTemplate = tQ[0];
	/*var selTemplate = tQ[tQ.length-1];
	showTemplate(selTemplate);
	ed.clearCanvas();
	ed.findErrors(trec);*/

	Ext.get(hp.getId()).highlight('#fffebb', {block:true});
}

TellMe.prototype.showTemplate = function(t, runTOP) {
	var ed = t.editor;
	ed.template = t;
  	ed.initLayerItems();
	ed.refreshConstraints();
	ed.template.markErrors();
	ed.redrawCanvas(ed.panelWidth, ed.panelHeight);
	if(runTOP && !t.layouted) {
		ed.clearCanvas();
		ed.layout();
		t.layouted = true;
		//ed.findErrors(null);
	}
}

TellMe.prototype.runTopAlgorithm = function(template, hpTree, hpDetail, msgTarget, callbackfn, ed, op_url) {
	var This = this;
	var url = op_url+"/runTopAlgorithm?template_id="+template.id;
	Ext.Ajax.request({ 
		url: url,
		params: {json: Ext.encode(template.store)},
		success: function(response) {
			//msgTarget.unmask();
			//if(window.console) window.console.log(response.responseText);

			var curNode = hpTree.getRootNode().findChild('id', template.txid, true);
			var log = curNode ? curNode.data.log : '';
			var br = "\n";
			var tab = "  ";

			var data;
			try {
				data = Ext.decode(response.responseText);
			}
			catch (e) {
				log += response.responseText;
			}
			
			if(data) {
				log += br+"Performing initial assessment...";
				if(data.issues1 && data.issues1.length) {
					var issues = br+tab+"Found "+data.issues1.length+" issues:";
					Ext.each(data.issues1, function(item) { issues += br+tab+tab+item; });
					log += issues;
				} else log += br+tab+"Found No Issues";

				log += br+"Performing secondary assessment...";
				if(data.issues2 && data.issues2.length) {
					var issues = br+tab+"Found "+data.issues2.length+" issues:";
					Ext.each(data.issues2, function(item) { issues += br+tab+tab+item; });
					log += issues;
				} else log += br+tab+"Found No Issues";


				var top_template = new Template(template.template_id, data.template, ed);

				log += br+"Displaying Workflow Sketch...";

				if(curNode) {
					This.addToHistory(hpTree, hpDetail, template, "[Run TOP]", "OK", log, [top_template], 'ok', 'TOPIcon');
				}
				else This.showTemplate(top_template, true);

			}
			//msgTarget.mask('Using Graphviz for Layout...', 'x-mask-loading');
			//ed.graphLayout.layoutDot(msgTarget, callbackfn, ed, op_url, ed.template);
			//ed.layout();
		},
		failure: function(response) {
			if(window.console) window.console.log(response.responseText);
		}
	});
}
