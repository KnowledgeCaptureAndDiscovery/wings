function RunBrowser(guid, runid, op_url, data_url, template_url) {
	this.guid = guid;
	this.runid = runid;
	this.op_url = op_url;
	this.data_url = data_url;
	this.template_url = template_url;
	
	this.tabPanel;
	this.runList;
}

RunBrowser.prototype.formatRunListItem = function(value, meta, record, rowind, colind, store, view) {
	var d = record.data;
	var tid = getLocalName(d.id).replace(/^(.+?)\-[0-9a-f]+\-[0-9a-f]+.*$/, '$1');
	var rstat = d.status;
	
	var icon = 'icon-spin6 fa animate-spin fa-grey'
	if(rstat == 'SUCCESS') 
		icon = 'icon-select-alt fa-big fa-green';
	else if(rstat == 'FAILURE')
		icon = 'icon-cancel-alt fa-big fa-red';

	var tmpl = "<i style='float:left;padding:5px' class='"+icon+"'></i>" +
			"<div>" +
			"<div class='runtitle'>{1}</div>" +
			"<div class='runtime'>Run ID:{2}</div>" +
			"</div>";
	return Ext.String.format(tmpl, rstat, tid, getLocalName(d.id));
};

RunBrowser.prototype.formatProgressBar = function(value, meta, record, rowind, colind, store, view) {
	var d = record.data;
	var tmpl = "";
	tmpl += "<div style='width:{0}px;height:{1}px;background-color:#EEE'>";
	tmpl += "<div style='width:{2}px;height:{1}px;background-color:{5}'>";
	tmpl += "<div style='width:{3}px;height:{1}px;background-color:{6}'>";
	tmpl += "<div style='width:{4}px;height:{1}px;background-color:{7}'>";
	tmpl += "&nbsp;</div></div></div></div>";
	if (d.status == "FAILURE")
		tmpl += "<div class='running'>Failed: {8}</div>";
	else if (d.status == "RUNNING")
		tmpl += "<div class='running'>Running: {9}</div>";
	else if (d.status == "SUCCESS")
		tmpl += "<div class='running'>Completed</div>";
	else if (d.status == "QUEUED")
		tmpl += "<div class='running'>Queued</div>";
	var w = 190;
	if (d.status == "SUCCESS")
		d.percent_done = 100;
	return Ext.String.format(tmpl, w, 10, 
			Math.round(w * (d.percent_done + d.percent_failed + d.percent_running) / 100), 
			Math.round(w * (d.percent_done + d.percent_running) / 100),
			Math.round(w * d.percent_done / 100),
			"#F66", "#999", "#6E6",
			d.failed_jobs, d.running_jobs);
};

RunBrowser.prototype.formatBinding = function(b, meta, record, runid) {
	var str = "";
	var This = this;
	if (b.type == 'literal')
		str += "<b style='color:green'>" + b.value + "</b>";
	else if (b.size != -1 && b.location) {
		var href = This.data_url + "/fetch?data_id=" + escape(b.id);
		str += "<a href='" + href + "' target='_blank'>" + getLocalName(b.id)
				+ "</a> ";
		str += "<span style='color:#888'>(";
		str += This.formatSize(b.size);
		str += ")</span>";
	} else
		str += "<b style='color:red'>Not yet available</b>";
	return str;
};

RunBrowser.prototype.formatSave = function(b, meta, record, runid) {
	var str = "";
	if (record.data.type != "Input") {
		str += "<a class='icon-save fa-small fa-grey' href='#'>Save</a>";
	}	
	return str;
};

RunBrowser.prototype.formatSize = function(bytes, precision) {
	if (!precision)
		precision = 2;
	var units = [
			'B', 'KB', 'MB', 'GB', 'TB'
	];

	bytes = Math.max(bytes, 0);
	var pow = Math.floor((bytes ? Math.log(bytes) : 0) / Math.log(1024));
	pow = Math.min(pow, units.length - 1);

	bytes /= Math.pow(1024, pow);

	return Math.round(bytes, precision) + ' ' + units[pow];
};

RunBrowser.prototype.getIODataGrid = function(data, tstore, runid) {
	var This = this;
	
	// Get information about the file bindings
	var filedata = {};
	var steps = data.plan.steps;
	for(var i=0; i<steps.length; i++) {
		var step = steps[i];
		for(var j=0; j<step.inputFiles.length; j++) {
			var file = step.inputFiles[j];
			filedata[file.id] = file;
		}
		for(var j=0; j<step.outputFiles.length; j++) {
			var file = step.outputFiles[j];
			filedata[file.id] = file;
		}
	}
	// Get mappings of variables to bindings
	var vmaps = {};
	for(var i=0; i<tstore.Variables.length; i++) {
		var v = tstore.Variables[i];
		var binding = filedata[v.id] ? filedata[v.id] : v.binding;
		if(v.binding.id)
			binding.id = v.binding.id;
		vmaps[v.id] = {v: getLocalName(v.id), b: binding};
	}
	for(var i=0; i<tstore.Links.length; i++) {
		var l = tstore.Links[i];
		if(!l.fromPort)
			vmaps[l.variable.id].type = 'Input';
		else if(!l.toPort)
			vmaps[l.variable.id].type = 'Output';
		else 
			vmaps[l.variable.id].type = 'Intermediate';
	}

	// Get variable constraints
	for(var i=0; i<tstore.constraints.length; i++) {
		var cons = tstore.constraints[i];
		var vid = cons.subject;
		if(vmaps[vid] && vmaps[vid].b) {
			var b = vmaps[vid].b;
			var pred = getLocalName(cons.predicate);
			if(!b.metadata)
				b.metadata = {};
			if(!b.metadata[pred])
				b.metadata[pred] = [];
			if(b.metadata[pred].push)
				b.metadata[pred].push(cons.object);
			vmaps[vid].b = b;
		}
	}
	
	// Create binding data
	var bindings = [];
	for(var vid in vmaps)
		bindings.push(vmaps[vid]);
	
	if (!Ext.ModelManager.isRegistered('workflowRunDetails'))
		Ext.define('workflowRunDetails', {
			extend : 'Ext.data.Model',
			fields : [
					'v', 'type', 'b'
			]
		});

	var wRunDStore = new Ext.data.Store({
		model : 'workflowRunDetails',
		proxy : {
			type : 'memory',
			reader : {
				type : 'json'
			}
		},
		sorters : [
			'v'
		],
		groupField : 'type',
		data : bindings
	});
	This.tabPanel.bindings = bindings;

	var groupingFeature = Ext.create('Ext.grid.feature.Grouping', {
		groupHeaderTpl : '{name} ({rows.length} Item{[values.rows.length > 1 ? "s" : ""]})'
	});

	var grid = new Ext.grid.GridPanel({
		store : wRunDStore,
		columns : [
				Ext.create('Ext.grid.RowNumberer', {
					width : 30,
					dataIndex : 'x'
				}), {
					header : 'Variable',
					dataIndex : 'v',
					width : 200,
					sortable : true,
					menuDisabled : true
				}, {
					header : 'Binding',
					dataIndex : 'b',
					flex : 1,
					menuDisabled : true,
					renderer : function(v, m, r) {
						return This.formatBinding(v, m, r, runid);
					}
				}, {
					header : 'Save',
					flex : 1,
					menuDisabled : true,
					renderer : function(v, m, r) {
						return This.formatSave(v, m, r, runid);
					}
				}, {
					header : 'Data',
					hidden : true,
					dataIndex : 'type',
					menuDisabled : true
				}
		],
		
		listeners: {
			cellclick: function(table, td, ci, rec, tr, ri, e, eopts) {
				if(ci != 3) return;
				var clicked_item = e.getTarget();
				// Listen for a click on the "Save" link
				if(clicked_item.tagName == "A") {
					var b = rec.data.b;
					This.registerData(b, runid);
				}
			} 
		},

		// autoExpandColumn: 'binding',
		monitorResize : true,
		features : [
			groupingFeature
		],
		viewConfig : {
			trackOver : false
		},
		bodyCls : 'multi-line-grid',
		border : false
	});

	return grid;
};

RunBrowser.prototype.stopRun = function(rec) {
	var This = this;
	var url = this.op_url + '/stopRun';
	var msgTarget = this.runList.getEl();
	msgTarget.mask('Stopping...', 'x-mask-loading');
	Ext.Ajax.request({
		url : url,
		params : {
			run_id : rec.data.id,
		},
		success : function(response) {
			msgTarget.unmask();
			This.runList.getStore().load();
			
		},
		failure : function(response) {
			_console(response.responseText);
		}
	});
};

RunBrowser.prototype.registerData = function(b, runid) {
	var This = this;
	var dsname = getLocalName(b.id);
	var dsns = getNamespace(b.id);
	Ext.Msg.prompt("Save data..", "Name this dataset for your Data Catalog:", function(btn, text) {
		if (btn == 'ok' && text) {
			var newname = getRDFID(text);
			var url = This.data_url + '/registerData';
			var msgTarget = This.tabPanel.getEl();
			msgTarget.mask('Saving and Registering Data...', 'x-mask-loading');
			Ext.Ajax.request({
				url : url,
				params : {
					data_id : b.id,
					newname : newname,
					metadata_json : Ext.encode(b.metadata)
				},
				success : function(response) {
					msgTarget.unmask();
					if (response.responseText == "OK") {
						// Everything ok
					}
					else {
						showError(response.responseText);
						_console(response.responseText);
					}
				},
				failure : function(response) {
					_console(response.responseText);
				}
			});
		}
	}, window, false, dsname);
	return false;
};

RunBrowser.prototype.getRunReport = function(op_url, runid, tabPanel, type, image) {
	var msgTarget = tabPanel.getEl();
	msgTarget.mask('Generating ' + type + '...', 'x-mask-loading');
	var url = op_url + '/op=getRun' + type;
	Ext.Ajax.request({
		url : url,
		params : {
			run_id : runid,
			run_image : image
		},
		success : function(response) {
			msgTarget.unmask();
			if (response.responseText == "Already Published") {
				alert("This run has already been published");
				return;
			}
			var url = response.responseText;
			var win = new Ext.Window({
				title : type + ' Turtle',
				width : 800,
				height : 600,
				plain : true,
				html : "URL: <a target='_blank' href='" + url + "'>" + url
						+ "</a><br/><iframe style='width:770px; height:550px' src='" + url + "'></iframe>"
			});
			win.show();
		},
		failure : function(response) {
			msgTarget.unmask();
			_console(response.responseText);
		}
	});
};

RunBrowser.prototype.getRunDetailsPanel = function(data, runid) {
	var This = this;
	var tabPanel = Ext.create('Ext.tab.Panel', {
			plain: true,
			margin: 5
	});
	var tid = data.originalTemplateId;
	var xtid = data.expandedTemplateId;
	
	var tBrowser = new TemplateBrowser(this.guid, 
			{ 
				hide_selector: true, hide_form: true, hide_intro: true, 
				hide_documentation: true, hide_title: true 
			}, 
			null, false, false,  This.template_url);
	
	tBrowser.tabPanel = tabPanel;

	var dataPanel = this.getRunDataPanel(tBrowser);
	var logPanel = this.getRunLogPanel(data);

	tabPanel.insert(0, logPanel);
	tabPanel.insert(0, dataPanel);
	
	var tpanel = tBrowser.openTemplate(xtid, 'Expanded Template', null, true);
	tpanel.getLoader().on("load", function() {
		var tstore = tpanel.graph.editor.store;
		dataPanel.add(This.getIODataGrid(data, tstore, runid));
	});
	tabPanel.setActiveTab(0);
	
	return tabPanel;
};

RunBrowser.prototype.getRunLogPanel = function(data) {
	var log = "";
	for(var i=0; i<data.queue.steps.length; i++) {
		var step = data.queue.steps[i];
		if(step.runtimeInfo.status == 'SUCCESS' || step.runtimeInfo.status == 'FAILURE') {
			log += getLocalName(step.id) + "\n----------------------\n";
			log += step.runtimeInfo.log+"\n";
		}
	}
	for(var i=0; i<data.queue.steps.length; i++) {
		var step = data.queue.steps[i];
		if(step.runtimeInfo.status == 'RUNNING') {
			log += getLocalName(step.id) + "\n-----------RUNNING-------------\n";
			log += step.runtimeInfo.log+"\n";
		}
	}
	log += data.runtimeInfo.log;
	return new Ext.Panel({
		title : 'Run Log',
		layout : 'border',
		border : false,
		iconCls : 'icon-run fa-title fa-blue',
		items : {
			region : 'center',
			border : false,
			xtype : 'textarea',
			readOnly : true,
			value : log,
			style : 'font-size:12px;font-family:monospace;'
		},
		autoScroll : true
	});
};

RunBrowser.prototype.getRunDataPanel = function(tBrowser) {
	return new Ext.Panel({
		title : 'Data',
		layout : 'fit',
		border : false,
		iconCls : 'icon-file-alt fa-title fa-blue',
		/*tbar : [
		{
				text : 'Get HTML',
				iconCls : 'docsIcon',
				handler : function() {
					tBrowser.tabPanel.setActiveTab(2);
					var graph = tBrowser.templatePanel.graph.editor;
					var image = graph.getImageData(1, false);
					tBrowser.tabPanel.setActiveTab(0);
					getRunReport(op_url, runid, tBrowser.tabPanel, 'HTML', image);
				}
		},
		{
			text: 'Publish RDF',
			iconCls: 'docsIcon',
			handler: function() {
				Ext.MessageBox.confirm("Confirm", 
					"This might take a while to publish. Please don't close this browser window while upload is going on. Are you sure you want to Continue ?", 
		  			function(b) {
		     			if(b == "yes") {
		     				tBrowser.tabPanel.setActiveTab(2);
							var graph = tBrowser.templatePanel.graph.editor;
							var image = graph.getImageData(1, false);
							tBrowser.tabPanel.setActiveTab(0);
							getRunReport(op_url, runid, tBrowser.tabPanel, 'RDF', image);
						}
					}
				);
			}
		}
		]*/
	});
};

RunBrowser.prototype.refreshOpenRunTabs = function(grid, wRunStore) {
	var tab = this.tabPanel.getActiveTab();
	if (tab && tab.runid) {
		var rec = wRunStore.getById(tab.runid);
		if (rec.data.status != tab.status || rec.data.status == 'RUNNING') {
			this.openRunDetails(tab.runid, rec.data.status, tab);
			tab.status = rec.data.status;
		}
		this.selectRunInList(tab.runid);
	}
};

RunBrowser.prototype.openRunDetails = function(runid, status, tab) {
	var tabName = getLocalName(runid);

	// If a tab is provided, then explicitly refresh
	// Else, check for matching tab and return if already open
	if (!tab) {
		// Check if tab is already open
		var items = this.tabPanel.items.items;
		for ( var i = 0; i < items.length; i++) {
			var tab = items[i];
			if (tab && tab.title.replace(/^\**/, '') == tabName) {
				this.tabPanel.setActiveTab(tab);
				tab.status = status;
				return null;
			}
		}
		var tab = new Ext.Panel({
			layout : 'fit',
			closable : true,
			iconCls : 'icon-run fa-title fa-brown',
			border: false,
			title : tabName,
			items : []
		});
		this.tabPanel.add(tab);
		tab.runid = runid;
		tab.status = status;
		this.tabPanel.setActiveTab(tab);
	}

	var This = this;
	var url = this.op_url + "/getRunDetails";
	Ext.apply(tab, {
		loader : {
			loadMask : true,
			url : url,
			params : {
				run_id : runid
			},
			renderer : function(loader, response, req) {
				var runJson = Ext.decode(response.responseText);
				if (runJson) {
					tab.removeAll();
					var runDetails = This.getRunDetailsPanel(runJson, runid);
					tab.add(runDetails);
					// tab.doLayout(false,true);
				}
				else {
					Ext.get(tab.getId()).update('No Run Details', false);
				}
			}
		}
	});
	tab.getLoader().load();
};

RunBrowser.prototype.getRunList = function() {
	var This = this;
	var fields = [
			'id', 
			{
				name: 'status',
				mapping: 'runtimeInfo["status"]'
			},
			{
				name : 'startTime',
				type: 'date',
				dateFormat:'timestamp',
				mapping : 'runtimeInfo["startTime"]'
			}, 
			{
				name : 'endTime',
				type: 'date',
				dateFormat:'timestamp',
				mapping : 'runtimeInfo["endTime"]'
			},
			'running_jobs', 'failed_jobs', 
			'percent_done', 'percent_running', 'percent_failed'
	];

	/*if (!Ext.ModelManager.isRegistered('workflowRuns'))
		Ext.define('workflowRuns', {
			extend : 'Ext.data.Model',
			fields : fields
		});*/

	var wRunStore = new Ext.data.Store({
		fields : fields,
		proxy : {
			type : 'ajax',
			simpleSortMode : true,
			batchActions : false,
			api : {
				read : this.op_url + "/getRunList",
				destroy : this.op_url + "/deleteRun"
			},
			reader : {
				type : 'json',
				idProperty : 'id'
			},
			writer : {
				type : 'json',
				encode : true,
				root: 'json',
				writeAllFields : false
			},
		},
		sorters : [
			{
				property : 'startTime',
				direction : 'DESC'
			}
		],
		autoSync : true
	});

	var This = this;
	var grid = new Ext.grid.GridPanel({
		columns : [
				{
					header : 'Template',
					dataIndex : 'id',
					flex : 1,
					renderer : This.formatRunListItem,
					sortable : true,
					menuDisabled : true
				}, {
					header : 'Progress',
					dataIndex : 'percent_done',
					sortable : true,
					width : 200,
					renderer : This.formatProgressBar,
					menuDisabled : true
				}, {
					header : 'Start Time',
					dataIndex : 'startTime',
					sortable : true,
					xtype : 'datecolumn',
					format : 'g:i:s a, M d, Y',
					width : 150,
					menuDisabled : true
				}, {
					header : 'End Time',
					dataIndex : 'endTime',
					sortable : true,
					xtype : 'datecolumn',
					format : 'g:i:s a, M d, Y',
					width : 150,
					menuDisabled : true
				}
		],
		title : 'My Runs',
		// autoExpandColumn: 'title',
		bodyCls : 'multi-line-grid',
		border : false,
		split : true,
		tbar : [
				{
					text : 'Delete',
					iconCls : 'icon-del fa fa-red',
					disabled : true,
					id : 'delButton',
					handler : function() {
						var recs = grid.getSelectionModel().getSelection();
						if (recs && recs.length) {
							wRunStore.remove(recs);
							This.tabPanel.remove(This.tabPanel.getActiveTab());
						}
					}
				},
				{
					text : 'Stop',
					iconCls : 'icon-cancel fa fa-red',
					id : 'stopButton',
					disabled : true,
					handler : function() {
						var recs = grid.getSelectionModel().getSelection();
						if (recs && recs.length) {
							This.stopRun(recs[0]);
						}
					}
				}, '-', {
					xtype : 'tbfill'
				}, '-', {
					text : 'Reload',
					iconCls : 'icon-reload fa fa-green',
					handler : function() {
						wRunStore.load();
					}
				}
		],
		store : wRunStore
	});

	grid.getSelectionModel().on("select", function(sm, rec, ind) {
		This.openRunDetails(rec.data.id, rec.data.status);
		grid.down('#delButton').setDisabled(false);
		if(rec.data.status == 'RUNNING')
			grid.down('#stopButton').setDisabled(false);
		else 
			grid.down('#stopButton').setDisabled(true);
		// Ext.EventManager.stopEvent(event);
	}, this);

	if (this.runid) {
		var fn = function() {
			// var rec = wRunStore.getById(runid);
			// openRunDetails(runid, rec.data.status);
			This.selectRunInList(This.runid);
			wRunStore.un('load', fn);
		};
		wRunStore.on('load', fn);
	}

	wRunStore.on('load', function() {
		This.refreshOpenRunTabs(grid, wRunStore);
	});

	var gridListRefresh = {
		run : function() {
			wRunStore.load();
		},
		interval : 30000
		// 30 seconds
	};
	Ext.TaskManager.start(gridListRefresh);

	return grid;
};

RunBrowser.prototype.selectRunInList = function(runid) {
	var This = this;
	this.runList.getSelectionModel().deselectAll();
	this.runList.getStore().each(function(rec) {
		if (rec.data.id == runid)
			This.runList.getSelectionModel().select([
				rec
			]);
	});
};

RunBrowser.prototype.initialize = function() {
	var This = this;
	// Add the result tabPanel in the center
	This.tabPanel = new Ext.TabPanel({
		region : 'center',
		margins : '0 5 5 5',
		enableTabScroll : true,
		resizeTabs : true,
		plain: true,
		border: true,
		// minTabWidth: 175,
		// tabWidth: 175,
		items : [
			{
				layout : 'fit',
				title : 'Run Manager',
				autoLoad : {
					url : This.op_url + '/intro'
				}
			}
		]
	});

	This.runList = This.getRunList();
	Ext.apply(This.runList, {
		region : 'north',
		margins: 5,
		border: true,
		height : '40%',
		split : true
	});

	This.tabPanel.on('tabchange', function(tp, tab) {
		This.selectRunInList(tab.runid);
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
				items : [ This.runList, This.tabPanel ]
			}
		]
	});
	if (!This.runid)
		This.tabPanel.setActiveTab(0);
	return mainpanel;
};
