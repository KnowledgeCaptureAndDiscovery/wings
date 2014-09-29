function TemplateBrowser(guid, opts, store, editor, tellme, op_url, plan_url,
		run_url, results_url, prov_url, wliburl, dcdomns, dclibns, pcdomns, wflowns) {
	this.guid = guid;
	this.opts = opts;
	this.store = store;
	this.op_url = op_url;
	this.plan_url = plan_url;
	this.run_url = run_url;
	this.results_url = results_url;
	this.prov_url = prov_url;

	if (editor)
		this.reasoner = new Reasoner(store);

	this.wliburl = wliburl;
	this.nsmap = {
		dcdom : dcdomns,
		dclib : dclibns,
		pcdom : pcdomns,
		wflow : wflowns,
		xsd : "http://www.w3.org/2001/XMLSchema#",
		rdf : "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	};
	this.editor_mode = editor ? (tellme ? "tellme" : "editor") : "";

	this.provenanceViewer = new ProvenanceViewer(guid, prov_url);
	this.mainPanel = null;
	this.leftPanel = null;
	this.tabPanel = null;
	this.treePanel = null;
	
	this.cmap = null;
	this.tellMe = null;
}

TemplateBrowser.prototype.getTemplateGraph = function(template_id, tstore,
		editable, templatePanel, gridPanel, guid) {
	var This = this;
	if (!guid)
		guid = This.guid;
	// Sending constraints over to the template store
	tstore.template.constraints = tstore.constraints;
	var infoPanel = new Ext.Panel({
		region : 'south',
		border : false,
		autoHeight : true,
		bodyCls : 'transparent-panel',
		preventHeader : true,
		animCollapse : false,
		bodyPadding : editable ? 0 : 5,
		hidden : true
	});
	return new Ext.ux.TemplateGraphPanel({
		width : '100%',
		guid : guid,
		store : tstore.template,
		cmap : This.cmap,
		gridPanel : gridPanel,
		border : false,
		editable : editable,
		editor_mode : This.editor_mode,
		templatePanel : templatePanel,
		url : This.op_url,
		infoPanel : infoPanel,
		template_id : template_id,
		browser : This
	});
};

TemplateBrowser.prototype.getSeedForm = function(template_id, formdata,
		templatePanel) {
	var This = this;
	return new Ext.ux.form.SeedForm({
		store : formdata,
		split : true,
		// margins:'5 5 0 5',
		// border:true,
		split : true,
		preventHeader : true,
		collapsible : true,
		collapseMode : 'mini',
		border : false,
		autoHeight : true,
		op_url : This.op_url,
		plan_url : This.plan_url,
		run_url : This.run_url,
		results_url : This.results_url,
		templatePanel : templatePanel,
		template_id : template_id,
		browser : This
	});
};

TemplateBrowser.prototype.getChildComponentsMap = function(comptree) {
	return this.getConcreteComponents(comptree, {});
};

TemplateBrowser.prototype.getConcreteComponents = function(c, map) {
	if (c.children) {
		var cid = c.cls.component ? c.cls.component.id : c.cls.id;
		map[cid] = [];
		for ( var i = 0; i < c.children.length; i++) {
			var child = c.children[i];
			var childid = child.cls.component ? child.cls.component.id
					: child.cls.id;
			if (child.cls.component && child.cls.component.type == 2)
				map[cid].push(childid);
			var childmap = this.getConcreteComponents(child, map);
			if (childmap[childid])
				map[cid] = map[cid].concat(childmap[childid]);
		}
	}
	return map;
};

TemplateBrowser.prototype.getTemplateText = function(templateID) {
	return templateID.replace(/_/g, ' ');
};

TemplateBrowser.prototype.createNewTemplate = function() {
	var This = this;
	Ext.Msg.prompt("New Template..", "Enter name for the template:", function(
			btn, text) {
		if (btn == 'ok' && text) {
			var tname = getRDFID(text);
			var tid = This.getTemplateID(tname);
            var enode = This.treePanel.getStore().getNodeById(tid);
            if (enode) {
                showError('Template "' + tname + '" already exists');
                return;
            }
			var url = This.op_url + '/newTemplate?template_id=' + escape(tid);
			var msgTarget = Ext.get(This.treePanel.getId());
			msgTarget.mask('Creating...', 'x-mask-loading');
			Ext.Ajax.request({
				url : url,
				success : function(response) {
					msgTarget.unmask();
					if (response.responseText == "OK") {
						var tn = {
							id : tid,
							text : tname,
							iconCls : 'icon-wflow-alt fa fa-blue',
							leaf : true
						};
						var tnode = This.treePanel.getRootNode()
								.appendChild(tn);
						var path = getTreePath(tnode, 'text');
						This.openTemplate(tid, tname, path);
						This.treePanel.getStore().sort('text', 'ASC');
					} else
						_console(response.responseText);
				},
				failure : function(response) {
					_console(response.responseText);
				}
			});
		}
	}, window, false);
};

TemplateBrowser.prototype.deleteTemplate = function(tid, tname) {
	var This = this;
	var url = This.op_url + '/deleteTemplate?template_id=' + escape(tid);
	var msgTarget = Ext.get(This.treePanel.getId());
	msgTarget.mask('Deleting...', 'x-mask-loading');
	Ext.Ajax.request({
		url : url,
		success : function(response) {
			msgTarget.unmask();
			_console(response.responseText);
			if (response.responseText == "OK") {
				var tn = This.treePanel.getStore().getNodeById(tid);
				tn.parentNode.removeChild(tn);

				var items = This.tabPanel.items.items;
				for ( var i = 0; i < items.length; i++) {
					var tab = items[i];
					if (tab && tab.title == tname) {
						This.tabPanel.remove(tab);
					}
				}
			}
		},
		failure : function(response) {
			_console(response.responseText);
		}
	});
};

TemplateBrowser.prototype.renderTemplate = function(tab, tstore, tid, tname) {
	if (!this.editor_mode)
		this.renderTemplateViewer(tab, tstore, tid, tname);
	else
		this.renderTemplateEditor(tab, tstore, tid, tname);
};

/**
 * Template renderer (after fetching it from server)
 * 
 * @param tab :
 *            tab to render it under
 * @param tid :
 *            template id
 * @param tname :
 *            template name
 */
TemplateBrowser.prototype.setupTemplateRenderer = function(tab, tid, tname) {
	var This = this;
	Ext.apply(tab, {
		loader : {
			loadMask : true,
			renderer : function(loader, response, req) {
				var tstore = Ext.decode(response.responseText);
				if (tstore) {
					tab.removeAll();
					This.guessUnknownNamespaces(tstore.template);
					This.renderTemplate(tab, tstore, tid, tname);
				}
			}
		}
	});
};

TemplateBrowser.prototype.guessUnknownNamespaces = function(tstore) {
	if (!this.nsmap['dcdom'] && tstore.props)
		this.nsmap['dcdom'] = tstore.props['ont.domain.data.url'] + "#";
	if (!this.nsmap['dclib'] && tstore.props)
		this.nsmap['dclib'] = tstore.props['lib.domain.data.url'] + "#";
	if (!this.nsmap['pcdom'] && tstore.props)
		this.nsmap['pcdom'] = tstore.props['lib.domain.component.ns'];
	if (!this.nsmap['wflow'] && tstore.props)
		this.nsmap['wflow'] = tstore.props['ont.workflow.url'] + "#";
};

/**
 * Open a new template in the system tabPanel
 * 
 * @param tid
 *            template id
 * @param tname
 *            template name
 * @param path
 *            tree path in the system treePanel that opened this
 */
TemplateBrowser.prototype.openTemplate = function(tid, tname, path, doLayout) {
	var This = this;

	// Check if tab is already open
	var items = This.tabPanel.items.items;
	for ( var i = 0; i < items.length; i++) {
		var tab = items[i];
		if (tab && tab.title == tname) {
			This.tabPanel.setActiveTab(tab);
			return null;
		}
	}
	var tpanel = this.getTemplatePanel(tid, tname, path);

	This.tabPanel.setActiveTab(tpanel.mainTab);
	this.setupTemplateRenderer(tpanel, tid, tname);

	// Fetch Template via Ajax
	var fetchOp = This.editor_mode ? 'getEditorJSON' : 'getViewerJSON';
	var url = This.op_url + '/' + fetchOp;
	tpanel.getLoader().load({
		url : url,
		params : {
			template_id : tid
		}
	});

	if (doLayout) {
		var layoutfn = function() {
			tpanel.graph.editor.layout();
			tpanel.mainTab.un("activate", layoutfn);
		};
		tpanel.mainTab.on("activate", layoutfn);
	}
	return tpanel;
};

/**
 * Get TreePanel with a list of templates
 * 
 * @param templateList
 *            the template list store
 */
TemplateBrowser.prototype.createTemplatesListTree = function(templateList) {
	var This = this;

	// Create a Tree Store
	if (!Ext.ModelManager.isRegistered('treeRecord'))
		Ext.define('treeRecord', {
			extend : 'Ext.data.Model',
			fields : [ 'text' ]
		});

	var tmp = [];
	for ( var i = 0; i < templateList.length; i++) {
		tmp.push({
			id : templateList[i],
			text : getLocalName(templateList[i]),
			iconCls : 'icon-wflow-alt fa fa-blue',
			leaf : true
		});
	}
	var treeStore = Ext.create('Ext.data.TreeStore', {
		model : 'treeRecord',
		root : {
			text : 'Templates',
			expanded : true,
			children : tmp,
			//iconCls : 'dtypeIcon'
	        iconCls: 'icon-folder fa fa-orange',
	        expIconCls: 'icon-folder-open fa fa-orange'
		},
		folderSort : true,
		sorters : [ 'text' ]
	});

	// Create the toolbar
	var toolbar = null;
	if (This.editor_mode) {
		toolbar = Ext.create('Ext.toolbar.Toolbar', {
			dock : 'top',
			items : [
					{
						text : 'New Template',
						iconCls : 'icon-add fa fa-green',
						handler : function() {
							This.createNewTemplate();
						}
					},
					'-',
					{
						text : 'Delete Template',
						iconCls : 'icon-del fa fa-red',
						handler : function() {
							var nodes = This.treePanel.getSelectionModel()
									.getSelection();
							if (!nodes || !nodes.length) {
								return;
							}
							var node = nodes[0];
							if (!node) {
								return;
							}
							var tname = node.data.text;
							var tid = node.data.id;
							Ext.MessageBox.confirm("Confirm Delete",
									"Are you sure you want to Delete " + tname,
									function(b) {
										if (b == "yes") {
											This.deleteTemplate(tid, tname);
										}
									});
						}
					} ]
		});
	}

	// Create the tree panel
	This.treePanel = new Ext.tree.TreePanel({
		width : '100%',
		header : false,
		border : false,
		autoScroll : true,
		rootVisible : false,
		bodyCls : !This.editor_mode ? 'x-docked-noborder-top' : '',
		iconCls : 'icon-wflow-alt fa-title fa-blue',
		containerScroll : true,
		preventHeader : true,
		store : treeStore,
		url : This.op_url,
	    useArrows: true,
        viewConfig:{stripeRows:true},
	});

	if (toolbar != null) {
		This.treePanel.addDocked(toolbar);
	}

	// Onclick of the tree panel : Open a template
	This.treePanel.on("itemclick", function(view, rec, item, ind, event) {
		if (!rec.parentNode)
			return false;
		var tname = rec.data.text;
		var tid = rec.data.id;
		var path = getTreePath(rec, 'text');
		This.openTemplate(tid, tname, path);
	});

	This.tabPanel.on('tabchange', function(tp, tab) {
		if (tab.path)
			This.treePanel.selectPath(tab.path, 'text');
		else
			This.treePanel.getSelectionModel().deselectAll();

		if (This.tellMe) {
			var layout = This.tellMe.historyPanel.getLayout();
			if (tab.tellMeHistory)
				layout.setActiveItem(tab.tellMeHistory.getId());
		}
	});

	This.tabPanel.on('remove', function(tp, tab) {
		if (This.tellMe && tab.tellMeHistory) {
			This.tellMe.historyPanel.remove(tab.tellMeHistory);
		}
	});
};

TemplateBrowser.prototype.renderTemplateViewer = function(templatePanel,
		tstore, tid, tname) {
	var This = this;

	templatePanel.removeAll();
	var graph;
	var seedForm;
	var gridPanel;

	var mainPanel = new Ext.Panel({
		layout : 'border',
		border : false
	});

	if (!This.opts.hide_constraints && tstore.constraints.length) {
		gridPanel = this.getConstraintsTableViewer(tid, tstore);
		Ext.apply(gridPanel, {
			region : 'north',
			bodyCls : 'inactive-grid'
		});
	}
	if (!This.opts.hide_form) {
		seedForm = this.getSeedForm(tid, tstore.inputs, templatePanel);
		var region = !This.opts.hide_graph ? 'north' : 'center';
		Ext.apply(seedForm, {
			region : region
		});
		mainPanel.add(seedForm);
	}
	if (!This.opts.hide_graph) {
		graph = this.getTemplateGraph(tid, tstore, false, templatePanel,
				gridPanel);
		Ext.apply(graph, {
			region : 'center'
		});

		var graphPanel = new Ext.Panel(
				{
					title : (This.opts.hide_form && !This.opts.hide_title) ? 'Workflow: '
							+ tname
							: '',
					border : false,
					region : 'center',
					layout : 'border'
				});
		if (gridPanel)
			graphPanel.add(gridPanel);
		if (graph)
			graphPanel.add(graph);

		mainPanel.add(graphPanel);
		mainPanel.add(graph.infoPanel);
	}
	if (graph) {
		templatePanel.graph = graph;
		if (seedForm)
			seedForm.graph = graph;
	}

	if (!This.opts.hide_documentation && tstore.template.metadata) {
		var meta = tstore.template.metadata;
		var docViewer = new Ext.Panel();
		var docid = This.guid + "_" + tid + "_doc";
		templatePanel.mainTab.remove(docid);

		var docViewerPanel = new Ext.Panel(
				{
					title : 'Documentation',
					border : false,
					layout : 'border',
					id : docid,
					iconCls : 'icon-docs fa-title fa-blue',
					items : [
							/*{
								region : 'north',
								border : true,
								bodyStyle : 'border-left:0px;border-right:0px;border-top:0px',
								bodyPadding : 10,
								autoHeight : true,
								html : ("<b>Author:</b> " + meta.contributors
										+ "<br/><b>Last Updated:</b> " + This
										.parseXSDDateString(meta.lastUpdate))
							},*/ {
								region : 'center',
								border : false,
								bodyPadding : 10,
								autoScroll : true,
								bodyCls : 'docsDiv',
								html : meta.documentation
							} ]
				});
		templatePanel.mainTab.add(docViewerPanel);
	}
	if (!This.opts.hide_documentation) {
		var provid = This.guid + "_" + tid + "_prov";
		templatePanel.mainTab.remove(provid);
		var provgrid = This.provenanceViewer.createItemProvenanceGrid(tid);
		Ext.apply(provgrid, {id: provid, border: false});
		templatePanel.mainTab.add(provgrid);
	}
	templatePanel.add(mainPanel);
};

TemplateBrowser.prototype.parseXSDDateString = function(date) {
	if (!date)
		return null;
	var re = /(\d{4})-(\d{2})-(\d{2})(T(\d{2}):(\d{2}):(\d{2}))Z/;
	return eval(date.replace(re,
			'new Date(new Date(\'$2/$3/$1 $5:$6:$7 GMT\').getTime())'));
};

TemplateBrowser.prototype.getConstraintsTableViewer = function(tid, tstore) {
	var This = this;
	var tns = getNamespace(tid);
	var varMaps = [];
	for ( var i = 0; i < tstore.template.Variables.length; i++) {
		varMaps[tstore.template.Variables[i].id] = 1;
	}
	tstore.constraints = this.replaceConstraintObjects(tid, tstore.constraints,
			varMaps, true);

	if (!Ext.ModelManager.isRegistered('Triple'))
		Ext.define('Triple', {
			extend : 'Ext.data.Model',
			fields : [ 'subject', 'predicate', 'object' ]
		});
	var viewerTripleStore = new Ext.data.Store({
		model : 'Triple',
		reader : {
			type : 'array'
		},
		sorters : [ {
			field : 'subject',
			direction : 'ASC'
		} ]
	});
	viewerTripleStore.loadData(tstore.constraints);

	var gridPanel = new Ext.grid.Panel({
		tns : tns,
		store : viewerTripleStore,
		columns : [ {
			dataIndex : 'subject',
			header : 'Variable',
			menuDisabled : true,
			renderer : function(url) {
				return getPrefixedUrl(url, This.nsmap, this.tns);
			}
		}, {
			dataIndex : 'predicate',
			header : 'Constraint',
			menuDisabled : true,
			renderer : function(url) {
				return getPrefixedUrl(url, This.nsmap, this.tns);
			}
		}, {
			dataIndex : 'object',
			header : 'Value',
			menuDisabled : true,
			renderer : function(val) {
				if (typeof val == "string")
					return getPrefixedUrl(val, This.nsmap, this.tns);
				else
					return val;
			}
		} ],
		forceFit : true,
		border : false,
		autoScroll : true,
		columnLines : true,
		split : (tstore.constraints && tstore.constraints.length) ? true
				: false,
		animCollapse : false,
		preventHeader : true,
		collapsible : true,
		collapseMode : 'mini',
		// autoHeight : true,
		// title : 'Constraints: All',
		height : This.opts.hide_graph ? '100%' : '30%',
		maxHeight: 150
	});
	return gridPanel;
};

TemplateBrowser.prototype.replaceConstraintObjects = function(tid, constraints,
		varMaps, isViewer) {
	var tns = getNamespace(tid);

	var newconstraints = [];
	for ( var i = 0; i < constraints.length; i++) {
		var cons = constraints[i];
		var newcons = {};
		newcons.subject = cons.subject.id;
		newcons.predicate = cons.predicate.id;
		newcons.object = cons.object.isLiteral ? cons.object.value
				: cons.object.id;

		if (cons.object.isLiteral) {
			// Do something with the literal here ?
		} else if (!varMaps[cons.object.id]
				&& (getNamespace(cons.object.id) == tns)) {
			// if this isn't a variable & belongs to the current namespace
			newcons.object = "?" + newcons.object;
		}
		if (!varMaps[cons.subject.id] && (getNamespace(cons.subject.id) == tns)) {
			// if the subject isn't a variable & belongs to the current
			// namespace
			newcons.subject = "?" + newcons.subject;
		}

		/*
		 * if (isViewer) { // For viewer, remove all prefixes newcons.subject =
		 * newcons.subject.replace(/.+#/, ''); newcons.predicate =
		 * newcons.predicate.replace(/.+#/, ''); if(typeof newcons.object ==
		 * "string") newcons.object = newcons.object.replace(/.+#/, ''); }
		 */
		newconstraints[i] = newcons;
	}
	return newconstraints;
};

TemplateBrowser.prototype.getDatatypeParents = function(dtype, ptype, map) {
	if (!map)
		map = {};
	if (!map[dtype.item.id])
		map[dtype.item.id] = [];
	if (ptype) {
		map[dtype.item.id].push(ptype.item.id);
		map[dtype.item.id] = map[dtype.item.id].concat(map[ptype.item.id]);
	}
	for ( var i = 0; i < dtype.children.length; i++) {
		map = this.getDatatypeParents(dtype.children[i], dtype, map);
	}
	return map;
};

TemplateBrowser.prototype.getDatatypeChildren = function(dtype, ptype, map) {
	if (!map)
		map = {};
	if (ptype) {
		if (!map[ptype.item.id])
			map[ptype.item.id] = [];
		map[ptype.item.id].push(dtype.item.id);
	}
	for ( var i = 0; i < dtype.children.length; i++) {
		var cdtype = dtype.children[i];
		map = this.getDatatypeChildren(cdtype, dtype, map);
		if (map[cdtype.item.id])
			map[dtype.item.id] = map[dtype.item.id].concat(map[cdtype.item.id]);
	}
	return map;
};

// TODO: Move Constraints Table code in another class
// -- This is becoming too big
TemplateBrowser.prototype.getConstraintsTable = function(tid, tstore) {
	var This = this;
	var tns = getNamespace(tid);
	var propmap = {};

	var parentTypes = This.getDatatypeParents(This.store.data.tree);
	var childTypes = This.getDatatypeChildren(This.store.data.tree);

	// Create type : properties map
	var typeProps = {};
	var propvals = This.store.propvals;
	for ( var i = 0; i < propvals.length; i++) {
		var prop = propvals[i];
		var pitem = {
			id : prop.id,
			text : getPrefixedUrl(prop.id, This.nsmap, tns)
		};
		for ( var j = 0; j < prop.domains.length; j++) {
			var domid = prop.domains[j];
			if (!typeProps[domid])
				typeProps[domid] = [];
			typeProps[domid].push(pitem);
		}
		propmap[prop.id] = propvals[i];
	}

	var vars = [];
	var dataVars = [];
	var varMaps = [];
	for ( var i = 0; i < tstore.template.Variables.length; i++) {
		var v = tstore.template.Variables[i];
		varMaps[v.id] = 1;
		var vitem = {
			id : v.id,
			text : getPrefixedUrl(v.id, This.nsmap, tns)
		};
		vars.push(vitem);
		if (v.type == 1)
			dataVars.push(vitem);
	}

	// Do some cleanup/annotation on the constraints
	tstore.constraints = this.replaceConstraintObjects(tid, tstore.constraints,
			varMaps);

	if (!Ext.ModelManager.isRegistered('Triple'))
		Ext.define('Triple', {
			extend : 'Ext.data.Model',
			fields : [ 'subject', 'predicate', 'object' ]
		});

	/*
	 * Stores
	 */
	var editorTripleStore = new Ext.data.Store({
		model : 'Triple'
	});
	editorTripleStore.loadData(tstore.constraints);

	var varStore = new Ext.data.Store({
		fields : [ 'id', 'text' ],
		sorters : [ 'text' ]
	});
	varStore.loadData(vars);

	var dataVarStore = new Ext.data.Store({
		fields : [ 'id', 'text' ],
		sorters : [ 'text' ]
	});
	dataVarStore.loadData(dataVars);

	var propsStore = new Ext.data.Store({
		fields : [ 'id', 'text' ],
		sorters : [ 'text' ]
	});

	var valsStore = Ext.create('Ext.data.Store', {
		fields : [ 'id', 'text' ],
		sorters : [ 'text' ]
	});

	/*
	 * Editors
	 */
	var varEditor = new Ext.form.ComboBox({
		store : varStore,
		displayField : 'text',
		valueField : 'id',
		queryMode : 'local'
	});
	var varObjEditor = new Ext.form.ComboBox({
		store : dataVarStore,
		displayField : 'text',
		valueField : 'id',
		queryMode : 'local'
	});
	var propsEditor = new Ext.form.ComboBox({
		store : propsStore,
		displayField : 'text',
		valueField : 'id',
		queryMode : 'local'
	});
	var valsEditor = new Ext.form.ComboBox({
		store : valsStore,
		displayField : 'text',
		valueField : 'id',
		queryMode : 'local'
	});
	var txtEditor = new Ext.form.TextField({
		allowBlank : false
	});
	var numEditor = new Ext.form.NumberField({
		allowDecimals : false,
		allowBlank : false
	});
	var floatEditor = new Ext.form.NumberField({
		allowBlank : false,
		decimalPrecision : 6
	});
	var boolEditor = new Ext.form.field.Checkbox({
		allowBlank : false
	});

	var edopts = {
		triggerAction : 'all',
		typeAhead : true,
		lazyRender : true,
		lazyInit : true,
		forceSelection : true,
		allowBlank : false
	};
	Ext.apply(varEditor, edopts);
	Ext.apply(varObjEditor, edopts);
	Ext.apply(propsEditor, edopts);
	Ext.apply(valsEditor, edopts);

	var mapfn = function(val) {
		return {
			id : val,
			text : getPrefixedUrl(val, This.nsmap, tns)
		};
	};

	var getVariableType = function(varid, gridPanel) {
		if (varid && gridPanel.graph) {
			var tpl = gridPanel.graph.editor.template;
			var v = tpl.variables[varid];

			// Get the first type
			var links = tpl.getLinksWithVariable(v);
			for ( var i = 0; i < links.length; i++) {
				var link = links[i];
				var roleid = link.toPort ? link.toPort.name
						: link.fromPort.name;
				var node = link.toNode ? link.toNode : link.fromNode;
				var cbindingstr = node.getBindingId(node.binding);
				var cbindings = cbindingstr.split(',');
				for ( var j = 0; j < cbindings.length; j++) {
					var cid = cbindings[j];
					var vtype = This.reasoner.getComponentRoleType(cid, roleid);
					if (vtype)
						return vtype;
				}
			}
		}
		return null;
	};

	var getTypeProperties = function(typeid) {
		var props = [];
		if (typeProps[typeid])
			props = props.concat(typeProps[typeid]);
		// Get properties of parent types too
		var ptypeids = parentTypes[typeid];
		if (ptypeids) {
			for ( var i = 0; i < ptypeids.length; i++) {
				var ptypeid = ptypeids[i];
				if (typeProps[ptypeid])
					props = props.concat(typeProps[ptypeid]);
			}
		}
		return props;
	};

	var valueEditorFn = function(rec, gridPanel) {
		if (!rec)
			return false;
		// Need to have filled out the subject and predicate
		// to get a value editor
		var subjvarid = rec.get('subject');
		var pred = rec.get('predicate');
		if (!subjvarid || !pred)
			return true;

		var val = propmap[pred];
		if (val) {
			var rangeid = val.range ? val.range : getVariableType(subjvarid,
					gridPanel);
			var range = rangeid ? getPrefixedUrl(rangeid, This.nsmap) : "";
			if (val.type == 1) {
				if (range == "xsd:float" || range == "xsd:double")
					return floatEditor;
				if (range == "xsd:int" || range == "xsd:integer"
						|| range == "xsd:number")
					return numEditor;
				if (range == "xsd:boolean" || range == "xsd:bool")
					return boolEditor;
				// editorTripleStore.getAt(rowIndex).set('object', null);
				return txtEditor;
			} else if (range == "wflow:DataVariable") {
				return varObjEditor;
			} else if (pred == This.nsmap['rdf'] + 'type') {
				if (rangeid) {
					var types = [ rangeid ];
					types = types.concat(childTypes[rangeid]);
					var values = Ext.Array.map(types, mapfn);
					valsStore.loadData(values);
					return valsEditor;
				}
			} else if (val.values) {
				var values = Ext.Array.map(val.values, mapfn);
				valsStore.loadData(values);
				return valsEditor;
			}
		}
		return txtEditor;
	};

	var propertyEditorFn = function(rec, gridPanel) {
		// Just change the property store here
		if (!rec)
			return false;
		var subjvarid = rec.get('subject');
		if (!subjvarid)
			return true;

		if (gridPanel.graph) {
			var tpl = gridPanel.graph.editor.template;
			var v = tpl.variables[subjvarid];

			// Get all possible variable properties
			var allprops = [];

			// Get an intersection of all variable type properties
			var links = tpl.getLinksWithVariable(v);
			for ( var i = 0; i < links.length; i++) {
				var link = links[i];
				var roleid = link.toPort ? link.toPort.name
						: link.fromPort.name;
				var node = link.toNode ? link.toNode : link.fromNode;
				var cbindingstr = node.getBindingId(node.binding);
				var cbindings = cbindingstr.split(',');
				for ( var j = 0; j < cbindings.length; j++) {
					var cid = cbindings[j];
					var vtype = This.reasoner.getComponentRoleType(cid, roleid);
					if (vtype) {
						var vtypeprops = getTypeProperties(vtype);
						if (!allprops.length)
							allprops = vtypeprops;
						else {
							// Get an intersection
							allprops = allprops.filter(function(n) {
								return vtypeprops.indexOf(n) != -1;
							});
						}
					}
				}
			}
			var wflow = This.nsmap['wflow'];
			if (v.type == "PARAM")
				allprops = allprops.concat(typeProps[wflow
						+ 'ParameterVariable']);
			else
				allprops = allprops.concat(typeProps[wflow + 'DataVariable']);
			propsEditor.getStore().removeAll();
			propsEditor.getStore().loadData(allprops);
		}
		return propsEditor;
	};

	var selModel = Ext.create('Ext.selection.CheckboxModel', {
		checkOnly : true,
		listeners : {
			selectionchange : function(sm, selections) {
				gridPanel.down('#delConstraint').setDisabled(
						selections.length == 0);
			}
		}
	});

	var editorPlugin = Ext.create('Ext.grid.plugin.FlexibleCellEditing', {
		clicksToEdit : 1
	});

	var gridPanel = Ext.create('Ext.grid.Panel', {
		store : editorTripleStore,
		selModel : selModel,
		columns : [ {
			dataIndex : 'subject',
			header : 'Variable',
			flex : 1,
			renderer : function(url) {
				return getPrefixedUrl(url, This.nsmap, tns);
			},
			editor : varEditor,
			menuDisabled : true
		}, {
			dataIndex : 'predicate',
			header : 'Constraint',
			flex : 1,
			renderer : function(url) {
				return getPrefixedUrl(url, This.nsmap, tns);
			},
			getEditor : function(rec) {
				return propertyEditorFn(rec, gridPanel);
			},
			menuDisabled : true
		}, {
			dataIndex : 'object',
			header : 'Value',
			editable : true,
			flex : 1,
			renderer : function(val) {
				if (typeof val == "string")
					return getPrefixedUrl(val, This.nsmap, tns);
				else
					return val;
			},
			getEditor : function(rec) {
				return valueEditorFn(rec, gridPanel);
			},
			menuDisabled : true
		} ],
		border : false,
		autoScroll : true,
		columnLines : true,
		split : true,
		animCollapse : false,
		preventHeader : true,
		collapsible : true,
		collapseMode : 'mini',
		// title : 'Constraints: All',
		height : This.opts.hide_graph ? '100%' : '30%',
		maxHeight: 150,
		propmap : propmap,
		plugins : [ editorPlugin ],
		tbar : [ {
			text : 'Add Constraint',
			iconCls : 'icon-add fa fa-green',
			handler : function() {
				// access the Record constructor through the grid's store
				var p = new Triple({
					subject : '',
					predicate : '',
					object : ''
				});
				var pos = editorTripleStore.getCount();
				editorPlugin.cancelEdit();
				editorTripleStore.insert(pos, p);
				editorPlugin.startEditByPosition({
					row : pos,
					column : 1
				});
			}
		}, '-', {
			text : 'Delete Constraint',
			itemId : 'delConstraint',
			iconCls : 'icon-del fa fa-red',
			disabled : true,
			handler : function() {
				editorPlugin.cancelEdit();
				var s = gridPanel.getSelectionModel().getSelection();
				for ( var i = 0, r; r = s[i]; i++)
					editorTripleStore.remove(r);
			}
		} ]
	});
	gridPanel.variableStore = varStore;
	gridPanel.dataVariableStore = dataVarStore;
	return gridPanel;
};

TemplateBrowser.prototype.renderTemplateEditor = function(templatePanel,
		tstore, tid, tname) {
	var This = this;
	templatePanel.removeAll();
	var graph;
	var gridPanel;

	var graphPanel = new Ext.Panel({
		title : This.opts.hide_constraints ? 'Workflow: ' + tname : '',
		region : 'center',
		layout : 'border',
		border : false
	});

	if (!This.opts.hide_constraints) {
		gridPanel = This.getConstraintsTable(tid, tstore);
		Ext.apply(gridPanel, {
			region : (This.opts.hide_graph ? 'center' : 'north')
		});
		graphPanel.add(gridPanel);
	}

	if (!This.opts.hide_graph) {
		graph = This.getTemplateGraph(tid, tstore, true, templatePanel,
				gridPanel);
		Ext.apply(graph, {
			region : 'center'
		});
		if (graph)
			graphPanel.add(graph);

		if (gridPanel && graph)
			gridPanel.graph = graph;

		graphPanel.add(graph.infoPanel);

		templatePanel.mainTab.graphPanel = graph;
		templatePanel.mainTab.constraintsTable = gridPanel;
	}
	templatePanel.add(graphPanel);

	if (!This.opts.hide_documentation) {
		var docEditor = new Ext.form.HtmlEditor({
			region : 'center',
			border : false,
			// margins : '5 5 5 5',
			enableFont : false,
			bodyCls : 'docsDiv',
			value : tstore.template.metadata.documentation
		});

		var docid = This.guid + "_" + tid + "_doc_ed";
		templatePanel.mainTab.remove(docid);

		var docEditorPanel = new Ext.Panel({
			title : 'Documentation',
			id : docid,
			layout : 'border',
			iconCls : 'icon-docs fa-title fa-blue',
			border : false,
			items : docEditor
		});
		templatePanel.mainTab.add(docEditorPanel);
		templatePanel.mainTab.doc = docEditor;
		Ext.apply(templatePanel.mainTab.doc, {
			border : false
		});
	}

	if (This.editor_mode == "tellme") {
		var tellme = tstore.template.metadata.tellme;
		if (!This.tellMe)
			return;
		This.leftPanel.setActiveTab(This.tellMe.mainPanel);

		var layout = This.tellMe.historyPanel.getLayout();
		if (typeof (layout) == "string")
			return;
		var history = new TellMe.HistoryPanel({
			region : 'center',
			border : false,
			tid : tid,
			tname : tname,
			tellme : This.tellMe
		});
		history.templatePanel = templatePanel;

		This.tellMe.historyPanel.add(history);
		layout.setActiveItem(history.getId());

		templatePanel.mainTab.tellMeHistory = history;

		if (tellme && This.tellMe) {
			var tree = Ext.decode(tellme);
			This.tellMe.loadTellMeHistory(tree);
		}
	}
};

TemplateBrowser.prototype.saveActiveTemplateAs = function() {
	var This = this;
	var tab = This.tabPanel.getActiveTab();
	var tname = tab.title;
	Ext.Msg.prompt("Save As..", "Enter name for the template:", function(btn,
			text) {
		if (btn == 'ok') {
			if (text)
				This.saveActiveTemplate(text);
			else
				Ext.Msg.show({
					msg : "Give a name please"
				});
		}
	}, window, false);
};

TemplateBrowser.prototype.saveTemplateStore = function(template, consTable) {
	template.saveToStore();
	var constraints = [];
	consTable.store.clearFilter();
	var This = this;
	consTable.store.data.each(function() {
		if (this.data.object != undefined && this.data.subject
				&& this.data.predicate) {
			var prop = consTable.propmap[this.data.predicate];
			var object = {
				id : this.data.object
			};
			// Replace the temporary "?" prefix on non-variable constraint
			// objects. The prefix is just cosmetic to differentiate
			// variables from non-variables in the template namespace
			if ((typeof object.id == "string") && object.id.match(/^\?/))
				object.id = object.id.replace(/^\?/, '');
			else if (prop.type == 1)
				object = {
					isLiteral : true,
					value : this.data.object,
					type : prop.range
				// ? prop.range : This.nsmap['xsd'] + 'string'
				};

			var triple = {
				subject : {
					id : this.data.subject
				},
				predicate : {
					id : this.data.predicate
				},
				object : object
			};
			constraints.push(triple);
		}
	});
	template.store.constraints = constraints;
};

TemplateBrowser.prototype.getTemplateStoreForTab = function(tab) {
	var template = tab.graphPanel.editor.template;
	if (!template)
		return null;

	template = template.createCopy();
	var consTable = tab.constraintsTable;
	var numErrors = 0;
	for (err in template.errors)
		numErrors++;
	if (numErrors) {
		showError("There are errors in the current template that are marked in red. Please fix them before continuing");
		_console(template.errors);
		return null;
	}
	this.saveTemplateStore(template, consTable);
	if (!template.store.metadata)
		template.store.metadata = {};
	if (tab.doc) {
		template.store.metadata.documentation = tab.doc.getValue();
	}
	template.store.metadata.lastUpdate = xsdDateTime(new Date());
	return template.store;
};

TemplateBrowser.prototype.getTemplateID = function(tname) {
	var stname = getRDFID(tname);
	return this.wliburl + "/" + stname + ".owl#" + stname;
};

TemplateBrowser.prototype.saveActiveTemplate = function(tname) {
	var This = this;
	var tab = This.tabPanel.getActiveTab();
	if (!tname) {
		tname = tab.title;
	} else {
		tname = getRDFID(tname);
	}
	if (tname == 'TellMeTemplate') {
		showError("Cannot overwrite 'TellMeTemplate'. Try and <b>Save As</b> another template");
		return false;
	}

	var tid = this.getTemplateID(tname);

	var store = this.getTemplateStoreForTab(tab);
	if (!store)
		return;

	if (This.tellMe) {
		if (store.metadata)
			store.metadata.tellme = this.tellMe.getTellMeHistory();
		else
			store.tellme = this.tellMe.getTellMeHistory();
	}

	// Move constraints out of store
	var constraints = store.constraints;
	store.constraints = null;

	var imagedata = tab.graphPanel.editor.getImageData(1, false);

	var url = This.op_url + '/saveTemplateJSON?template_id=' + escape(tid);
	var msgTarget = Ext.get(tab.getId());
	msgTarget.mask('Saving...', 'x-mask-loading');
	Ext.Ajax.request({
		url : url,
		params : {
			json : Ext.encode(store),
			constraints_json : Ext.encode(constraints),
		// imagedata: imagedata
		},
		success : function(response) {
			msgTarget.unmask();
			if (response.responseText != "OK") {
				showError(response.responseText);
			} else {
				if (tname == tab.title) {
					tab.constraintsTable.getStore().commitChanges();
				}
				var node = This.treePanel.getStore().getNodeById(tid);
				if (!node) {
					var ptab = This.treePanel.findParentByType('tabpanel');
					ptab.setActiveTab(This.treePanel);
					var node = {
						id : tid,
						text : tname,
						iconCls : 'icon-wflow-alt fa fa-blue',
						leaf : true
					};
					This.treePanel.getRootNode().appendChild(node);
				}
			}
		},
		failure : function(response) {
			msgTarget.unmask();
			showError(response.responseText);
		}
	});
};

TemplateBrowser.prototype.inferElaboratedTemplate = function(store) {
	var This = this;
	var tab = This.tabPanel.tabBar ? This.tabPanel.getActiveTab()
			: This.tabPanel.templatePanel.mainTab;
	var tname = tab.title;
	var tid = this.getTemplateID(tname);

	if (!store)
		store = this.getTemplateStoreForTab(tab);
	if (!store)
		return;

	// Move constraints out of store
	var constraints = store.constraints;
	store.constraints = null;

	var url = This.plan_url + '/elaborateTemplateJSON?__template_id='
			+ escape(tid);
	var msgTarget = Ext.get(tab.getId());
	msgTarget.mask('Elaborating...', 'x-mask-loading');
	Ext.Ajax
			.request({
				url : url,
				params : {
					json : Ext.encode(store),
					constraints_json : Ext.encode(constraints)
				},
				success : function(response) {
					msgTarget.unmask();
					// try {
					var store = Ext.decode(response.responseText);
					if (!store) {
						_console(response.responseText);
					} else if (store.error) {
						_console(store);
						showWingsError(
								"Wings couldn't elaborate the template: "
										+ tname, "Error in " + tname, store);
					} else {
						var varMaps = [];
						for ( var i = 0; i < store.template.Variables.length; i++) {
							varMaps[store.template.Variables[i].id] = 1;
						}
						store.constraints = This.replaceConstraintObjects(tid,
								store.constraints, varMaps);
						tab.constraintsTable.store.loadData(store.constraints);
						tab.graphPanel.reloadGraph(store.template);
						showWingsMessage(
								"Wings has elaborated your template. Please Save to make your changes persistent",
								tname, store);
					}
					/*
					 * } catch (e) { _console(response.responseText);
					 * showWingsError(response.responseText, "Error in " +
					 * tname, { explanations : [] }); }
					 */
				},
				failure : function(response) {
					msgTarget.unmask();
					showError(response.responseText);
				}
			});
};

TemplateBrowser.prototype.getTemplatePanel = function(tid, tabname, path) {
	var This = this;
	var toolbar = null;

	if (This.editor_mode) {
		// Create the toolbar
		toolbar = Ext.create('Ext.toolbar.Toolbar', {
			dock : 'top',
			items : [
					{
						text : 'Save',
						iconCls : 'icon-save fa fa-blue',
						cls : 'highlightIcon',
						handler : function() {
							This.saveActiveTemplate();
						}
					},
					{
						text : 'Save As',
						iconCls : 'icon-save fa fa-blue',
						handler : function() {
							This.saveActiveTemplateAs();
						}
					},
					'-',
					{
						text : 'Elaborate Template',
						iconCls : 'icon-run fa fa-brown',
						handler : function() {
							This.inferElaboratedTemplate();
						}
					},
					'-',
					{
						xtype : 'tbfill'
					},
					{
						iconCls : 'icon-reload fa fa-green',
						text : 'Reload',
						handler : function() {
							var fetchOp = This.editor_mode ? 'getEditorJSON'
									: 'getViewerJSON';
							var url = This.op_url + '/' + fetchOp
									+ '?template_id=' + escape(tid);
							var tpanel = this.up('panel');
							if(!This.opts.hide_documentation) tpanel = tpanel.down('panel');
							tpanel.getLoader().load({
								url : url
							});
							if (This.tellMe)
								This.tellMe.clear();
						}
					} ]
		});
	}

	var templatePanel;
	if (!This.opts.hide_documentation) {
		// If documentation needs to be shown as well, then
		// - create a tab panel
		// - move the template panel into it

		// Create the panel
		templatePanel = new Ext.Panel({
			title : 'Template',
			region : 'center',
			layout : 'fit'
		});
		var lowerTabs = new Ext.TabPanel({
			plain : true,
			border : true,
			iconCls : 'icon-wflow-alt fa-title fa-blue',
			closable : !This.opts.hide_selector,
			region : 'center',
			title : tabname,
			tabPosition : 'top',
			margin : 5,
			activeTab : 0,
			path : path,
			items : [ templatePanel ]
		});
		if (toolbar != null)
			lowerTabs.addDocked(toolbar);

		This.tabPanel.add(lowerTabs);
		templatePanel.mainTab = lowerTabs;
		Ext.apply(templatePanel.mainTab, {
			border : true
		});
	} else {
		// If no documentation
		// - create a new template panel
		templatePanel = new Ext.Panel({
			border : false,
			layout : 'fit',
			region : 'center',
			title : tabname,
			iconCls : 'icon-wflow-alt fa-title fa-blue',
			closable : !This.opts.hide_selector
		});
		if (toolbar != null)
			templatePanel.addDocked(toolbar);

		This.tabPanel.add(templatePanel);
		templatePanel.mainTab = templatePanel;
	}
	if (this.tellMe) {
		templatePanel.tellMePanel = this.tellMe.mainPanel;
	}
	return templatePanel;
};

// Must be called within Ext.onReady
TemplateBrowser.prototype.initialize = function(tid) {
	var This = this;

	// this.mainPanel = new Ext.Panel({
	this.mainPanel = new Ext.Viewport({
		layout : {
			type : 'border'
		},
		items : []
	});

	this.mainPanel.add(getPortalHeader());

	if (this.store.components)
		this.cmap = this.getChildComponentsMap(this.store.components.tree);

	// Create a tabbed on the left for the template and component trees
	this.leftPanel = new Ext.TabPanel({
		plain : true,
		region : 'west',
		collapsible : true,
		collapseMode : 'mini',
		// border: false,
		margins : '5 0 5 5',
		cmargins : '5 0 5 0',
		preventHeader : true,
		width : (This.editor_mode == 'tellme' ? 400 : This.editor_mode ? 280
				: 220),
		split : true,
		activeTab : 0
	});

	// Add the template tabPanel in the center
	this.tabPanel = new Ext.TabPanel({
		plain : true,
		margins : this.opts.hide_selector ? 5 : '5 5 5 0',
		region : 'center',
		// border: false,
		enableTabScroll : true,
	});

	if (!This.opts.hide_intro) {
		this.tabPanel.add({
			// margin: 10,
			// border: false,
			title : This.editor_mode ? 'Edit Workflows' : 'Run Workflows',
			autoLoad : {
				url : This.op_url + '/' + 'intro'
			}
		});
		this.tabPanel.setActiveTab(0);
	}
	// Add the TellMe Panel
	if (this.editor_mode == 'tellme') {
		this.tellMe = new TellMe(this.guid, this.tid, this.tname,
				this.tabPanel, this.mainPanel, this.opts, this.store,
				this.plan_url, this.op_url, this.nsmap);
		this.tellMe.initialize();
		this.leftPanel.add(this.tellMe.mainPanel);
	}

	// Add the template tree list to the leftPanel
	if (this.store.tree) {
		// Sets this.treePanel
		this.createTemplatesListTree(this.store.tree);
		this.treePanel.setTitle('Templates');
		this.leftPanel.add(this.treePanel);
		this.leftPanel.setActiveTab(0);
	}

	if (this.editor_mode) {
		var cv = new ComponentViewer(this.guid, this.store.components,
				this.op_url, null, null, this.prov_url, 
				this.nsmap.pcdom, this.nsmap.dcdom,
				this.nsmap.dclib, true, false);
		var cTree = cv.getComponentListTree(true);
	    var cInputsTree = cv.getComponentInputsTree(true);
	    var cOutputsTree = cv.getComponentOutputsTree(true);
		var cPanel = {
			xtype: 'tabpanel',
			title : 'Components',
			plain: true,
			padding: '3 0 0 0',
			items: [cTree, cInputsTree, cOutputsTree]
		};
		this.leftPanel.add(cPanel);
	}

	this.mainPanel.add(this.leftPanel);
	this.mainPanel.add(this.tabPanel);

	if (This.opts.hide_selector)
		This.leftPanel.hide();

	// Preload a template if provided
	if (tid) {
		var tnode = this.treePanel.getStore().getNodeById(tid);
		if (tnode) {
			var path = getTreePath(tnode, 'text');
			This.openTemplate(tid, getLocalName(tid), path);
		}
	}
	
	// Initialize tooltips
	Ext.tip.QuickTipManager.init();
	Ext.apply(Ext.tip.QuickTipManager.getQuickTip(), {
	    maxWidth: 400,
	    minWidth: 250,
	});
};