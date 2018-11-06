function MetaworkflowBrowser(guid, op_url, template_url, plan_api, run_api) {
	this.guid = guid;
	this.op_url = op_url;
	this.template_url = template_url;
	this.plan_api = plan_api;
	this.run_api = run_api;
	this.tBrowser = this.setupTemplateBrowser();
	this.treePanel = null;
	this.tabPanel = null;
}

MetaworkflowBrowser.prototype.setupTemplateBrowser = function() {
	return new TemplateBrowser(this.guid, 
			{ 
				hide_selector: true, hide_form: false, hide_intro: true, 
				hide_documentation: true, hide_title: true 
			}, 
			null, false, false,  this.template_url, this.plan_api, this.run_api, this.run_api);
};

MetaworkflowBrowser.prototype.openTemplate = function(tid, tname, path, doLayout) {
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
	Ext.apply(tpanel, { closable: true });
	
	This.tabPanel.add(tpanel);
	
	tpanel.getLoader().on("load", function(event, response) {
		var data = Ext.decode(response.responseText);
		var inputs = data.inputs;
		console.log(inputs);
	});
	
	tpanel.getLoader().load({method : 'get'});

	This.tabPanel.setActiveTab(tpanel);
	return tpanel;
};

MetaworkflowBrowser.prototype.getTemplatePanel = function(tid, tabname, path) {
	return this.tBrowser.createViewerPanel(tid, tabname);
};

MetaworkflowBrowser.prototype.createMetaworkflowsListTree = function(list) {
	var This = this;

	// Create a Tree Store
	if (!Ext.ModelManager.isRegistered('treeRecord'))
		Ext.define('treeRecord', {
			extend : 'Ext.data.Model',
			fields : [ 'text' ]
		});

	var tmp = [];
	for ( var i = 0; i < list.length; i++) {
		tmp.push({
			id : list[i],
			text : getLocalName(list[i]),
			iconCls : 'icon-wflow-alt fa fa-blue',
			leaf : true
		});
	}
	var treeStore = Ext.create('Ext.data.TreeStore', {
		model : 'treeRecord',
		root : {
			text : 'Meta Workflows',
			expanded : true,
			children : tmp,
			//iconCls : 'dtypeIcon'
	        iconCls: 'icon-folder fa fa-orange',
	        expIconCls: 'icon-folder-open fa fa-orange'
		},
		folderSort : true,
		sorters : [ 'text' ]
	});

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
	});

	This.tabPanel.on('remove', function(tp, tab) {
		if (This.tellMe && tab.tellMeHistory) {
			This.tellMe.historyPanel.remove(tab.tellMeHistory);
		}
	});
};

MetaworkflowBrowser.prototype.initialize = function() {
	var This = this;
	// Add the result tabPanel in the center
	This.tabPanel = new Ext.TabPanel({
		region : 'center',
        margins: '5 5 5 0',		
		enableTabScroll : true,
		resizeTabs : true,
		plain: true,
		border: true,
		// minTabWidth: 175,
		// tabWidth: 175,
		items : [
			{
				layout : 'fit',
				title : 'Metaworkflow Browser',
				autoLoad : {
					url : This.op_url + '/intro'
				}
			}
		]
	});

	this.createMetaworkflowsListTree(workflows);
	this.treePanel.setTitle('Meta Workflows');
	
	This.metaList = new Ext.TabPanel({
		region : 'west',
        margins: '5 0 5 5',
        cmargins: '5 5 5 5',
        plain: true,
		width : '20%',
		split : true,
		items: [ this.treePanel ],
        activeTab: 0
	});

	var mainpanel = new Ext.Viewport({
		layout : 'border',
		items: [
			getPortalHeader(),
			{
				xtype:'panel',
				region:'center',
				layout:'border',
				border: false,
				items : [ This.metaList, This.tabPanel ]
			}
		]
	});
	This.tabPanel.setActiveTab(0);
	return mainpanel;
};