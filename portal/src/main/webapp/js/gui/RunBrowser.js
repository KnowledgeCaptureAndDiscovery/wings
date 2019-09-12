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

function RunBrowser(guid, runid, op_url, data_url, template_url, can_publish) {
	this.guid = guid;
	this.runid = runid;
	this.op_url = op_url;
	this.data_url = data_url;
	this.template_url = template_url;
	this.can_publish = can_publish;
	
	this.tabPanel;
	this.runList;
	this.tBrowser = this.setupTemplateBrowser();
}

PAGESIZE = 100;

RunBrowser.prototype.setupTemplateBrowser = function() {
	return new TemplateBrowser(this.guid, 
			{ 
				hide_selector: true, hide_form: true, hide_intro: true, 
				hide_documentation: true, hide_title: true 
			}, 
			null, false, false,  this.template_url);
};

RunBrowser.prototype.formatRunListItem = function(value, meta, record, rowind, colind, store, view) {
	var d = record.data;
	var tid = getLocalName(d.template_id);
	if(!tid)
		tid = getLocalName(d.id).replace(/^(.+?)\-[0-9a-f]+\-[0-9a-f]+.*$/, '$1');
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
		str += "<b style='color:red'>Not available</b>";
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

RunBrowser.prototype.getIODataGrid = function(data, runid) {
	var This = this;
	var bindings = this.getVariableBindingData(data);
	
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

RunBrowser.prototype.getVariableBindingData = function(data) {
	var exec = data.execution;

	// Get information about the file bindings
	var filedata = {};
	var steps = exec.plan.steps;
	for(var i=0; i<steps.length; i++) {
		var step = steps[i];
		for(var j=0; j<step.inputFiles.length; j++) {
			var file = step.inputFiles[j];
			for(var key in file.metadata)
				file.metadata[key] = [file.metadata[key]];
			filedata[file.id] = file;
		}
		for(var j=0; j<step.outputFiles.length; j++) {
			var file = step.outputFiles[j];
			for(var key in file.metadata)
				file.metadata[key] = [file.metadata[key]];
			filedata[file.id] = file;
		}
	}
	
	var vmaps = {};
	var varmap = function(variables, type) {
		for(var i=0; i<variables.length; i++) {
			var v = variables[i];
			var binding = filedata[v.id] ? filedata[v.id] : v.binding;
			if(v.binding.id)
				binding.id = v.binding.id;
			vmaps[v.id] = {v: getLocalName(v.id), b: binding, type: type};
		}		
	}
	// Get mappings of variables to bindings
	varmap(data.variables.input, "Input");
	varmap(data.variables.output, "Output");
	varmap(data.variables.intermediate, "Intermediate");

	// Get variable constraints
	for(var vid in data.constraints) {
		if(vmaps[vid] && vmaps[vid].b) {
			var constraints = data.constraints[vid];
			var b = vmaps[vid].b;
			if(!b.metadata)
				b.metadata = {};
			for(var i=0; i<constraints.length; i++) {
				var cons = constraints[i];
				var pred = cons.p;
				if(!b.metadata[pred])
					b.metadata[pred] = [];
				if(b.metadata[pred].push && 
						(pred == "type" || b.metadata[pred].length == 0))
					b.metadata[pred].push(cons.o.isLiteral ? cons.o.value : cons.o.id);				
			}
			vmaps[vid].b = b;			
		}
	}
	
	// Create binding data
	var bindings = [];
	for(var vid in vmaps)
		bindings.push(vmaps[vid]);	

	return bindings;
};

RunBrowser.prototype.stopRun = function(rec) {
    var This = this;
    var url = this.op_url + '/stopRun';
    var msgTarget = this.runList.getEl();
    msgTarget.mask('Stopping...', 'x-mask-loading');
    Ext.Ajax.request({
        url: url,
        params: {
            run_id: rec.data.id,
        },
        success: function (response) {
            msgTarget.unmask();
            This.runList.getStore().load();

        },
        failure: function (response) {
            _console(response.responseText);
        }
    });
}

RunBrowser.prototype.reRun = function(rec) {
    var This = this;
    var url = this.op_url + '/reRunWorkflow';
    var msgTarget = this.runList.getEl();
    msgTarget.mask('Creating the new workflow...', 'x-mask-loading');
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

RunBrowser.prototype.publishRun = function(runid, tabPanel) {
	var msgTarget = tabPanel.getEl();
	msgTarget.mask('Publishing RDF ...', 'x-mask-loading');
	var url = this.op_url + '/publishRun';
	Ext.Ajax.request({
		url : url,
		params : {
			run_id : runid
		},
		success : function(response) {
			msgTarget.unmask();
			var resp = Ext.decode(response.responseText);
			if(resp.url) {
				var url = resp.url;
				var win = new Ext.Window({
					title : 'Pubby',
					width : '70%',
					height : '70%',
					plain : true,
					maximizable : true,
					html : "<div style='padding:5px'>URL: <a target='_blank' href='" + url + "'>" + url
							+ "</a></div>"
							+ "<iframe style='border: 1px solid #999; width:100%; height:94%' src='" 
							+ url + "'></iframe>"
				});
				win.show();
			}
			if(resp.error)
				showError(resp.error);
		},
		failure : function(response) {
			msgTarget.unmask();
			_console(response.responseText);
		}
	});
};


RunBrowser.prototype.prePublishList = function() {
	var This = this;
	Ext.Msg.prompt("Publishing executions" , "Please, enter a pattern", function(btn, text) {
		if (btn == 'ok' && text)
			This.publishList(text);
		else{
			showError('Please enter a pattern');
			return;
		}
	});
};

RunBrowser.prototype.publishList = function(pattern) {
	var msgTarget = this.tabPanel.getEl();
	msgTarget.mask('Publishing RDF ...', 'x-mask-loading');
	var url = this.op_url + '/publishList';
	Ext.Ajax.request({
		url: url,
		params: {
			pattern: pattern
		},
		success: function (response) {
			msgTarget.unmask();
			var resp = Ext.decode(response.responseText);
			if (resp.error)
				showError(resp.error);
		},
		failure: function (response) {
			msgTarget.unmask();
			_console(response.responseText);
		}
	});
}



RunBrowser.prototype.getRunDetailsPanel = function(data, runid) {
	var This = this;
	var tbar = null;
	/*var tbar = [];
	tbar.push({
		text : 'Get HTML',
		iconCls : 'icon-docs fa fa-blue',
		handler : function() {
			This.getRunReport(runid, this.up('panel'), 'HTML');
		}
	});*/
	
	if(this.can_publish && data.execution.runtimeInfo.status == "SUCCESS") {
		if(!tbar)
			tbar = [];
		
		if(data.published_url) {
			var url = data.published_url;
			tbar.push({
				text: 'View Published RDF',
				iconCls: 'icon-network fa fa-blue',
				handler: function() {
					var me = this;
					var win = new Ext.Window({
						title : 'Pubby',
						width : '70%',
						height : '70%',
						plain : true,
						maximizable : true,
						html : "<div style='padding:5px'>URL: <a target='_blank' href='" + url + "'>" + url
								+ "</a></div>"
								+ "<iframe style='border: 1px solid #999; width:100%; height:94%' src='" 
								+ url + "'></iframe>"
					});
					win.show();
				}
			});
		}
		else {
			tbar.push({
				text: 'Publish RDF',
				iconCls: 'icon-network fa fa-blue',
				handler: function() {
					var me = this;
					Ext.MessageBox.confirm("Confirm", 
							"This might take a while to publish. Please don't close this browser window while upload is going on. Are you sure you want to Continue ?", 
							function(b) {
						if(b == "yes") {
							var panel = me.up('panel');
							var tab = panel.up('panel');
							This.publishRun(runid, panel);
							tab.getLoader().load();							
						}
					});
				}
			});
		}
	}
	
	var tabPanel = Ext.create('Ext.tab.Panel', {
			plain: true,
			margin: 5,
			tbar: tbar,
			items: []
	});
	
	var exec = data.execution;	
	var tid = exec.seededTemplateId;
	var xtid = exec.expandedTemplateId;

	var dataPanel = this.getRunDataPanel(data, runid);
	var logPanel = this.getRunLogPanel(exec);
	var tPanel = this.getTemplatePanel(tid, 'Template');
	var xtPanel = this.getTemplatePanel(xtid, 'Executable Workflow');	
	
	tabPanel.add(dataPanel);	
	tabPanel.add(logPanel);
	tabPanel.add(tPanel);
	tabPanel.add(xtPanel);
	
	tabPanel.dataPanel = dataPanel;
	tabPanel.logPanel = logPanel;
	tabPanel.tPanel = tPanel;
	tabPanel.xtPanel = xtPanel;
	
	tabPanel.setActiveTab(0);
	
	var loadxfn = function() {
		xtPanel.getLoader().on("load", function(event, response) {
			var xdata = Ext.decode(response.responseText);
			tpl = xdata.template;
			// Layout if current layout data missing			
			for(var i=0; i<tpl.Variables.length; i++) {
				var variable = tpl.Variables[i];
				if(!variable.comment) {
					xtPanel.graph.editor.layout();
					break;
				}
			}
			This.setGraphRuntimeInfo(xtPanel.graph, data);
		});
		xtPanel.getLoader().load({method: 'get'});
		xtPanel.un("activate", loadxfn);
	};
	xtPanel.on("activate", loadxfn);
	
	// Seeded Workflow
	var loadtfn = function() {
		tPanel.getLoader().on("load", function(event, response) {
			tPanel.graph.editor.layout();
		});
		tPanel.getLoader().load({method : 'get'});
		tPanel.un("activate", loadtfn);
	};	
	tPanel.on("activate", loadtfn);
	
	return tabPanel;
}

RunBrowser.prototype.setGraphRuntimeInfo = function(graph, data) {
	if(!graph || !graph.editor || !graph.editor.template)
		return;
	
	// Set node runtime infos
	var tpl = graph.editor.template;
	for(var i=0; i<data.execution.queue.steps.length; i++) {
		var step = data.execution.queue.steps[i];
		var node = tpl.nodes[step.id];
		if(node != null) {
			node.setRuntimeInfo(step.runtimeInfo);
		}
	}	
};

RunBrowser.prototype.getTemplatePanel = function(xtid, name) {
	return this.tBrowser.createViewerPanel(xtid, name);
};

RunBrowser.prototype.getRunLog = function(exec) {
	var log = "";
	
	exec.queue.steps.sort(function (a, b) {
		if(a.runtimeInfo.startTime != b.runtimeInfo.startTime) {
			return a.runtimeInfo.startTime > b.runtimeInfo.startTime ? 1 : -1;
		}
		else if(a.runtimeInfo.endTime == null)
			return 1;
		else if(b.runtimeInfo.endTime == null)
			return -1;
		else return a.runtimeInfo.endTime > b.runtimeInfo.endTime ? 1
				: a.runtimeInfo.endTime < b.runtimeInfo.endTime ? -1 : 0;
	});
	for(var i=0; i<exec.queue.steps.length; i++) {
		var step = exec.queue.steps[i];
		if(step.runtimeInfo.status != 'WAITING' &&
				step.runtimeInfo.status != 'QUEUED') {
			log += "=====================================\n";
			log += "[ JOB: " + getLocalName(step.id) + " ]";
			log += "\n[ STARTED: " + new Date(step.runtimeInfo.startTime*1000) + " ]";
			if(step.runtimeInfo.endTime) {
				log += "\n[ ENDED: " + new Date(step.runtimeInfo.endTime*1000) + " ]";				
			}
			log += "\n[ STATUS: " + step.runtimeInfo.status + " ]\n";
			log += "=====================================\n";
			log += step.runtimeInfo.log+"\n";
		}
	}
	log += exec.runtimeInfo.log;
	return log;
};

RunBrowser.prototype.getRunLogPanel = function(exec) {
	var log = this.getRunLog(exec);
	
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

RunBrowser.prototype.getRunDataPanel = function(data, runid) {
	var me = this;
	var dataPanel = new Ext.Panel({
		title : 'Data',
		layout : 'fit',
		border : false,
		iconCls : 'icon-file-alt fa-title fa-blue'
	});
	dataPanel.grid = this.getIODataGrid(data, runid)
	dataPanel.add(dataPanel.grid);
	return dataPanel;
};

RunBrowser.prototype.refreshOpenRunTabs = function(grid, wRunStore) {
	var selectedTab = this.tabPanel.getActiveTab();
	
	var items = this.tabPanel.items.items;
	for ( var i = 0; i < items.length; i++) {
		var tab = items[i];
		if (tab && tab.runid) {
			var rec = wRunStore.getById(tab.runid);
			if (rec.data.status != tab.status || rec.data.status == 'RUNNING') {
				tab.getLoader().load();
				tab.status = rec.data.status;
			}
		}
	}
	if (selectedTab && selectedTab.runid) 
		this.selectRunInList(selectedTab.runid);
};

RunBrowser.prototype.openRunDetails = function(runid, status) {
	var tabName = getLocalName(runid);

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
	
	// Create a new tab
	var tab = new Ext.Panel({
		layout : 'fit',
		closable : true,
		iconCls : 'icon-run fa-title fa-brown',
		border: false,
		title : tabName,
		items : []
	});
	tab.runid = runid;
	tab.status = status;
	
	// Add tab to tab panel 
	this.tabPanel.add(tab);
	this.tabPanel.setActiveTab(tab);

	// Set loader 
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
				var rundata = Ext.decode(response.responseText);
				if (rundata) {
					if(tab.initialized) {
						// If already initialized, only update content
						var bindings = This.getVariableBindingData(rundata);
						var log = This.getRunLog(rundata.execution);
						var graph = tab.content.xtPanel.graph;
						
						tab.content.dataPanel.grid.getStore().loadData(bindings);
						tab.content.logPanel.items.items[0].setValue(log);
						This.setGraphRuntimeInfo(graph, rundata);
					}
					else {
						// Create new content
						tab.content = This.getRunDetailsPanel(rundata, runid);
						tab.add(tab.content);
						// tab.doLayout(false,true);
						tab.initialized = true;
					}
				}
				else {
					tab.getEl().update('No Run Details', false);
				}
			}
		}
	});
	tab.getLoader().load();
};

RunBrowser.prototype.getRunList = function() {
	var This = this;
	var fields = [
			'id', 'template_id',
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
			timeout : 60000, // Timeout of 60 seconds
			api : {
				read : this.op_url + "/getRunList",
				destroy : this.op_url + "/deleteRun"
			},
			reader : {
				type : 'json',
	            root: 'rows',
	            totalProperty: 'results',
				idProperty : 'id'
			},
			writer : {
				type : 'json',
				encode : true,
				root: 'json',
				writeAllFields : false
			},
		},
		pageSize: PAGESIZE,
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
	    multiSelect: true, 
	    allowDeselect: true,
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
					text : 'Re-run',
					iconCls : 'icon-reload fa fa-green',
					disabled : false,
					id : 'rerunButton',
					handler : function() {
						var recs = grid.getSelectionModel().getSelection();
						if (recs && recs.length) {
							This.reRun(recs[0]);
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
				}, '-', 
				{
					xtype: 'textfield',
					id: 'searchText',
				},
				{
					text: 'Search',
					handler : function() {
						wRunStore.load({
					        start: 0,
					        limit: PAGESIZE,
					        params: {
					        	pattern: grid.down('#searchText').value
					        }
						});
					}
				},
				'-', 
				{
					xtype : 'tbfill'
				}, '-', {
					text : 'Reload',
					iconCls : 'icon-reload fa fa-green',
					handler : function() {
						wRunStore.load({
					        start: 0,
					        limit: PAGESIZE,
					        params: {
					        	pattern: grid.down('#searchText').value
					        }
						});
					}
				}
		],
		store : wRunStore,
	    dockedItems: [{
	        xtype: 'pagingtoolbar',
	        store: wRunStore,   // same store GridPanel is using
	        dock: 'bottom',
	        displayInfo: true
	    }],
	});

	grid.getSelectionModel().on("focuschange", function(item, oldrec, rec, opts) {
		if(rec) {
			This.openRunDetails(rec.data.id, rec.data.status);
			grid.down('#delButton').setDisabled(false);
			if(rec.data.status == 'RUNNING')
				grid.down('#stopButton').setDisabled(false);
			else 
				grid.down('#stopButton').setDisabled(true);
			// Ext.EventManager.stopEvent(event);
		}
	}, this);

	if (this.runid) {
		var fn = function() {
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
			wRunStore.load({
		        start: 0,
		        limit: PAGESIZE,
		        params: {
		        	pattern: grid.down('#searchText').value
		        }
			});
		},
		interval : 120000
		// 120 seconds
	};
	Ext.TaskManager.start(gridListRefresh);

	return grid;
};

RunBrowser.prototype.selectRunInList = function(runid) {
	var This = this;
	if(!runid)
		this.runList.getSelectionModel().deselectAll();
	this.runList.getStore().each(function(rec) {
		if (rec.data.id == runid)
			This.runList.getSelectionModel().setLastFocused(rec);
			/*
			This.runList.getSelectionModel().select([
				rec
			]);*/
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
