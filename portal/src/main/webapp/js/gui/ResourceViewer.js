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

function ResourceViewer(guid, store, op_url, ontns, libns, is_advanced) {
	this.guid = guid;
	this.store = store;
	this.op_url = op_url;
	this.ontns = ontns;
	this.libns = libns;
	this.is_advanced = is_advanced;

	this.treePanel = null;
}

ResourceViewer.prototype.createStoreModels = function() {
	if (!Ext.ModelManager.isRegistered('ResourceRecord'))
		Ext.define('ResourceRecord', {
			extend : 'Ext.data.Model',
			fields : [ 'text' ]
		});
};

ResourceViewer.prototype.refreshEnvironmentVariables = function(tab, mstore) {
	var source = {};
	var evals = {};
	for(var i=0; i<mstore.environmentValues.length; i++) {
		var eval = mstore.environmentValues[i];
		evals[eval.variable] = eval.value;
	}
	var swversions = {};
	this.version_store.each(function(field) {
		var sw = field.get('softwareGroupId');
		if(!swversions[sw]) 
			swversions[sw] = [];
		swversions[sw].push(field.get('id'));
	});
	var pgrid = tab.down('propertygrid');
	if(!pgrid || !this.environment_store || !this.version_store)
		return;
	this.environment_store.each(function(field) {
		var sw = field.get('softwareGroupId');
		var vr = field.get('variable');
		if(!swversions[sw] || !mstore.softwareIds) return;
		var num = Ext.Array.intersect(mstore.softwareIds, swversions[sw]);
		if(num.length)
			source[vr] = evals[vr] ? evals[vr] : '';
	});
	pgrid.setSource(source);
};

ResourceViewer.prototype.getGB = function(bytes) {
	return (bytes/1073741824).toFixed(2) + " GB";
};

ResourceViewer.prototype.openMachineEditor = function(args) {
    var tab = args[0];
    var id = args[1];
    var mstore = args[2];
    var mainPanel;
    var This = this;
    
    var savebtn = new Ext.Button({
        text: 'Save',
        iconCls: 'icon-save fa fa-blue',
        disabled: true,
        handler: function(btn) {
        	var form = tab.down('form');
        	var fields = form.getForm().getFields();
        	var machine = {id: id};
        	fields.each(function(field) {
        		machine[field.getName()] = field.getValue();
        	});
        	var swpanel = form.down('panel[type=machineSoftware]');
        	machine = This.getMachineSoftware(swpanel, machine);
        	
        	This.tabPanel.getEl().mask("Saving..");
        	Ext.Ajax.request({
                url: This.op_url + '/saveMachineJSON',
                params: {
                    resid: id,
                    json: Ext.encode(machine)
                },
                success: function(response) {
                    This.tabPanel.getEl().unmask();
                    if (response.responseText == "OK") {
						// Reset dirty bit
						form.getForm().getFields().each(function(field) {
							field.resetOriginalValue();
						});
                        savebtn.setDisabled(true);
                        tab.setTitle(tab.title.replace(/^\*/, ''));
                    } else {
                        Ext.MessageBox.show({
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.MessageBox.OK,
                            msg: "Could not save:<br/>" + response.responseText.replace(/\n/, '<br/>')
                        });
                    }
                },
                failure: function(response) {
                    This.tabPanel.getEl().unmask();
                    _console(response.responseText);
                }
            });
        }
    });
    
    Ext.Ajax.timeout = 300000;
    var checkbtn = new Ext.Button({
        text: 'Get Machine Details',
        iconCls: 'icon-run fa fa-brown',
        handler: function(btn) {
        	This.tabPanel.getEl().mask("Connecting and checking..<br/>"+
        			"This may take some time for first-time connections");
        	Ext.Ajax.request({
                url: This.op_url + '/checkMachine',
                params: {
                    resid: id
                },
                timeout: 300000,
                success: function(response) {
                    This.tabPanel.getEl().unmask();
                    try {
                    	var store = Ext.decode(response.responseText);
						var win = new Ext.Window({
							layout : 'border',
							constrain : true,
							maximizable : true,
							title : getLocalName(id),
							frame : false,
							border : false,
							autoScroll : true,
							width : 450,
							height : 350,
							items: [{
								xtype: 'propertygrid',
								region: 'center',
								source: store,
								customRenderers:{
									storageRootMax:function(v) {
										return This.getGB(v);},
									storageRootFree:function(v) {
										return This.getGB(v);},
									memoryFree:function(v) {
										return This.getGB(v);},
									memoryMax:function(v) {
										return This.getGB(v);},
									connect: function(v) {
										return "<div class='connect_"+v+"'>"+
											v+"</div>";
									},
									systemLoad: function(v) {
										return v.toFixed(2);
									}
								},
							    listeners: {
							        'beforeedit': {
							            fn: function () {
							                return false;
							            }
							        }
							    }
							}, {
								region: 'south',
								title: 'Errors',
								bodyStyle: 'padding:5px;font:tahoma 11px',
								html: store.errors.join("<br/>")
							}]
						});
                    	win.show();
                    }
                    catch (e) {
                    	console.log(e);
                        Ext.MessageBox.show({
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.MessageBox.OK,
                            msg: "Got invalid response:<br/>" 
                            	+ response.responseText.replace(/\n/, '<br/>')
                        });
                    }
                },
                failure: function(response) {
                    This.tabPanel.getEl().unmask();
                    _console(response.responseText);
                }
            });
        }
    });
    
	var form = {
		xtype : 'form',
		fieldDefaults : {
			msgTarget : 'side',
			labelWidth : 150
		},
		frame : false,
		border : false,
		layout : 'border',
		items : [],
		listeners : {
			dirtychange : function(item, dirty, opts) {
				if (dirty) {
					savebtn.setDisabled(false);
					tab.setTitle("*" + tab.title.replace(/^\*/, ''));
				}
			}
		},
		tbar: This.is_advanced ? [ savebtn ] : null
	};
	form.tbar.push(checkbtn);

	var tabPanel = {
		xtype : 'tabpanel',
		region : 'center',
		border : false,
		plain : true,
		margins : '5 0 0 0',
		activetab : 0,
		items : []
	};
	form.items.push(tabPanel);
	
	var infoPanel = {
		xtype : 'panel',
		title : 'Information',
		frame : true,
		layout : 'form',
		bodyStyle : 'padding:5px',
		margin : 5,
		autoScroll : true,
		defaults: {
			xtype: 'textfield',
			flex: 1,
			anchor: '100%'
		},
		items : [ {
			name : 'hostIP',
			fieldLabel : 'IP Address',
			value: mstore.hostIP
		},{
			name : 'hostName',
			fieldLabel : 'Host Name',
			value: mstore.hostName
		},{
			name : 'userId',
			fieldLabel : 'Userid',
			value: mstore.userId
		},{
			name : 'userKey',
			fieldLabel : 'User Private Key (path)',
			value: mstore.userKey
		},{
			xtype : 'checkbox',
			name : 'isHealthy',
			fieldLabel : 'Is Healthy ?',
			checked : mstore.isHealthy
		},{
			name : 'memoryGB',
			fieldLabel : 'System Memory (GB)',
			value: mstore.memoryGB
		},{
			name : 'storageGB',
			fieldLabel : 'System Storage (GB)',
			value: mstore.storageGB
		},{
			xtype : 'checkbox',
			name : 'is64Bit',
			fieldLabel : 'Is 64-bit ?',
			checked : mstore.is64Bit
		},{
			name : 'storageFolder',
			fieldLabel : 'Wings Storage Folder',
			value: mstore.storageFolder
		},{
			name : 'executionFolder',
			fieldLabel : 'Wings Execution Folder',
			value: mstore.executionFolder
		}
		]
	};
	tabPanel.items.push(infoPanel);
	
	if(!this.version_store) {
		this.version_store = Ext.create('Ext.data.Store', {
			fields : [ 'id', 'softwareGroupId', 'versionNumber', 'versionText', 
			           {name:'versionDisplay',
						convert: function (val, model) {
							return getLocalName(model.get('softwareGroupId'))
									+ " " + model.get('versionText');
						}},
			           {name:'groupDisplay',
						convert: function (val, model) {
							return getLocalName(model.get('softwareGroupId'));
						}}],
			proxy : {
				type : 'ajax',
				url : This.op_url + '/getAllSoftwareVersions'
			},
			groupers : 'softwareGroupId',
			sorters : ['softwareGroupId', 'versionNumber'],
			autoLoad : true
		});
	}
	if(!this.environment_store) {
		this.environment_store = Ext.create('Ext.data.Store', {
			fields : [ 'variable', 'softwareGroupId' ],
			proxy : {
				type : 'ajax',
				url : This.op_url + '/getAllSoftwareEnvironment'
			},
			groupers : 'softwareGroupId',
			autoLoad : true
		});
	}
	this.environment_store.on('load', function() {
		This.refreshEnvironmentVariables(tab, mstore);
	});

	var tpl = new Ext.XTemplate(
			'<tpl for=".">',
			'<tpl if="this.softwareGroupId != values.softwareGroupId">',
				'<tpl exec="this.softwareGroupId = values.softwareGroupId"></tpl>',
				'<div class="x-panel-header-default x-panel-header-text-container ' 
				+ 'x-panel-header-text x-panel-header-text-default">'
				+ '{groupDisplay}</div>',
			'</tpl>',
			'<div class="x-boundlist-item">{versionDisplay}</div>',
			'</tpl>'
	);
	
	var swPanel = {
		xtype : 'panel',
		title : 'Software',
		type : 'machineSoftware',
		layout : 'form',
		frame : true,
		margin : 5,
		bodyStyle : 'padding:5px',
		autoScroll : true,
		defaults : {
			xtype : 'textfield',
			flex : 1,
			anchor : '100%'
		},
		items : [ /*{
			xtype: 'combo',
			name : 'osid',
			fieldLabel : 'Operating System',
			value : mstore.osid
		}, */
		{
			xtype: 'combo',
			name : 'softwareIds',
			fieldLabel : 'Installed Software',
			value : mstore.softwareIds,
			multiSelect : true,
			store: This.version_store,
			queryMode : 'local',
			displayField: 'versionDisplay',
			groupField: 'softwareGroupId',
			valueField: 'id',
			tpl: tpl,
			listeners: {
				select: function(item) {
					mstore = This.getMachineSoftware(item.up('panel'), mstore);
					This.refreshEnvironmentVariables(tab, mstore);
				}
			}
		}, {
			xtype : 'propertygrid',
			margin : '5 0 0 0',
			title : 'Environment Variables',
			listeners: {
				afterrender: function() {
					This.refreshEnvironmentVariables(tab, mstore);
				},
				propertychange: function() {
					This.flagModified(tab, savebtn);
				}
			}
		} ]
	};
	
	tabPanel.items.push(swPanel);
    tab.add(form);
};
   
ResourceViewer.prototype.getMachineSoftware = function(panel, mstore) {
	mstore.softwareIds = panel.down('combo').value;
	var evars = panel.down('propertygrid').getSource();
	mstore.environmentValues = [];
	for(var v in evars) {
		mstore.environmentValues.push({
			variable: v,
			value: evars[v]
		});
	}
	return mstore;
};

ResourceViewer.prototype.flagModified = function(tab, savebtn) {
	tab.setTitle("*" + tab.title.replace(/^\*/, ''));
	savebtn.setDisabled(false);
};

ResourceViewer.prototype.prepareRoleRecord = function(arg) {
    var narg = {};
    for (var key in arg)
        narg[key] = arg[key];
    return narg;
};

ResourceViewer.prototype.openSoftwareEditor = function(args) {
    var tab = args[0];
    var id = args[1];
    var mstore = args[2];
    var mainPanel;
    var This = this;
    
    var savebtn = new Ext.Button({
        text: 'Save',
        iconCls: 'icon-save fa fa-blue',
        disabled: true,
        handler: function(btn) {
        	var software = {id: id, environmentVariables: [],
        			versions: []};
        	var grids = tab.query('grid[type="environment"]');
        	for(var i=0; i<grids.length; i++) {
        		var grid = grids[i];
	        	grid.getStore().each(function(field) {
	        		software.environmentVariables.push(field.get('name'));
	        	});
        	}
        	var grids = tab.query('grid[type="versions"]');
        	for(var i=0; i<grids.length; i++) {
        		var grid = grids[i];
	        	grid.getStore().each(function(field) {
	        		software.versions.push(This.prepareRoleRecord(field.data));
	        	});
        	}
        	This.tabPanel.getEl().mask("Saving..");
        	Ext.Ajax.request({
                url: This.op_url + '/saveSoftwareJSON',
                params: {
                	resid: id,
                    json: Ext.encode(software)
                },
                success: function(response) {
                    This.tabPanel.getEl().unmask();
                    if (response.responseText == "OK") {
			            grid.getStore().commitChanges();
                        savebtn.setDisabled(true);
                        tab.setTitle(tab.title.replace(/^\*/, ''));
                        if(This.version_store)
                        	This.version_store.reload();
                        if(This.environment_store)
                        	This.environment_store.reload();
                    } else {
                        Ext.MessageBox.show({
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.MessageBox.OK,
                            msg: "Could not save:<br/>" + response.responseText.replace(/\n/, '<br/>')
                        });
                    }
                },
                failure: function(response) {
                    This.tabPanel.getEl().unmask();
                    _console(response.responseText);
                }
            });
        }
    });

    var evarStore = Ext.create('Ext.data.Store', {
    	fields: ['name'],
    	data: mstore.environmentVariables.map(function(a){return {name:a};})
    });
    evarStore.on('add', function() {This.flagModified(tab, savebtn);});
    evarStore.on('remove', function() {This.flagModified(tab, savebtn);});
    evarStore.on('update', function() {This.flagModified(tab, savebtn);});

    var verStore = Ext.create('Ext.data.Store', {
    	fields: ['id', 'versionNumber', 'versionText'],
    	data: mstore.versions,
    	sorters: ['versionNumber']
    });
    verStore.on('add', function() {This.flagModified(tab, savebtn);});
    verStore.on('remove', function() {This.flagModified(tab, savebtn);});
    verStore.on('update', function() {This.flagModified(tab, savebtn);});
    
	var tbar1=null, tbar2 = null;
    var plugins1=[], plugins2 = [];
    var sm1=null, sm2 = null;
    if (This.is_advanced) {
        plugins1 = [{ptype: 'cellediting', clicksToEdit: 1}];
        plugins2 = [{ptype: 'cellediting', clicksToEdit: 1}];
        sm1 = Ext.create('Ext.selection.CheckboxModel', {checkOnly:true});
        sm2 = Ext.create('Ext.selection.CheckboxModel', {
        	mode:'SINGLE', allowDeselect: true});
        tbar1 = [{
	            text: 'Add',
	            iconCls: 'icon-add fa fa-green',
	            handler: function(item) {
	            	var grid = item.up('grid');
	                var pos = grid.getStore().getCount();
	                var plugin = grid.findPlugin('cellediting');
	                plugin.cancelEdit();
	                grid.getStore().insert(pos, {name:''});
	                plugin.startEditByPosition({
	                    row: pos,
	                    column: 1
	                });
	            }
	        },
	        {
	            text: 'Delete',
	            iconCls: 'icon-del fa fa-red',
	            handler: function(item) {
	                var grid = item.up('grid');
	                var plugin = grid.findPlugin('cellediting');
	                plugin.cancelEdit();
	                var s = grid.getSelectionModel().getSelection();
	                for (var i = 0, r; r = s[i]; i++) {
	                    grid.getStore().remove(r);
	                }
	            }
	        }
	    ];
        tbar2 = [{
	            text: 'Add',
	            iconCls: 'icon-add fa fa-green',
	            handler: function(item) {
	            	var grid = item.up('grid');
	                var pos = grid.getStore().getCount();
	                var plugin = grid.findPlugin('cellediting');
	                plugin.cancelEdit();
	                grid.getStore().insert(pos, {versionNumber:pos});
	                plugin.startEditByPosition({
	                    row: pos,
	                    column: 1
	                });
	            }
	        },
	        {
	            text: 'Delete',
	            iconCls: 'icon-del fa fa-red',
	            handler: function(item) {
	                var grid = item.up('grid');
	                var plugin = grid.findPlugin('cellediting');
	                plugin.cancelEdit();
	                var s = grid.getSelectionModel().getSelection();
	                for (var i = 0, r; r = s[i]; i++) {
	                    grid.getStore().remove(r);
	                }
	                // Reset indices
	                var ind = 0;
	                grid.getStore().each(function(rec) {
	                	rec.set('versionNumber', ind++);
	                });
	            }
	        },
	        '-',
	        {
	            text: 'Up',
	            iconCls: 'icon-up fa fa-blue',
	            handler: function(item) {
	                var grid = item.up('grid');
	                var plugin = grid.findPlugin('cellediting');
	                plugin.cancelEdit();
	                var s = grid.getSelectionModel().getSelection();
	                if(!s || s.length == 0) return;
	                var rec = s[0];
	                var vernum = rec.data.versionNumber;
	                if(vernum == 0) return;
	                rec.set('versionNumber', vernum - 1);
	                var rec2 = grid.getStore().getAt(vernum - 1);
	                rec2.set('versionNumber', vernum);
	                grid.getStore().sort();
	            }
	        },
	        {
	            text: 'Down',
	            iconCls: 'icon-down fa fa-blue',
	            handler: function(item) {
	                var grid = item.up('grid');
	                var plugin = grid.findPlugin('cellediting');
	                plugin.cancelEdit();
	                var s = grid.getSelectionModel().getSelection();
	                if(!s || s.length == 0) return;
	                var rec = s[0];
	                var vernum = rec.data.versionNumber;
	                var total = grid.getStore().getCount();
	                if(vernum == (total-1)) return;
	                rec.set('versionNumber', vernum + 1);
	                var rec2 = grid.getStore().getAt(vernum + 1);
	                rec2.set('versionNumber', vernum);
	                grid.getStore().sort();
	            }
	        }
	    ];
    }
		
	var envgrid = {
		xtype : 'grid',
		title : 'Environment Variables',
		type : 'environment',
		margin: 5,
		//frame: true,
		autoScroll : true,
		columns: [{
            dataIndex: 'name',
            flex: 1,
            header: 'Variable',
            editor: true,
            editable: This.is_advanced ? true: false,
            menuDisabled: true
        }],
        selModel: sm1,
        tbar: tbar1,
        plugins: plugins1,
        store: evarStore
	};
		
	var vergrid = {
		xtype : 'grid',
		title : 'Versions',
		type : 'versions',
		margin : 5,
		// frame: true,
		autoScroll : true,
		sortableColumns : false,
		columns : [ {
			dataIndex : 'versionNumber',
			header : 'Ver#',
			hidden : true,
			menuDisabled : true
		},
		{
			dataIndex : 'versionText',
			flex : 1,
			header : 'Version (Earlier versions on top)',
			editor : true,
			editable : This.is_advanced ? true : false,
			menuDisabled : true
		}],
		selModel: sm2,
		tbar : tbar2,
		plugins : plugins2,
		store : verStore
	};

	var panel = {
		xtype : 'panel',
		border : false,
		layout : 'border',
		tbar : (This.is_advanced ? [ savebtn ] : null),
		items : [ {
			xtype : 'tabpanel',
			region : 'center',
			border : false,
			plain : true,
			margins : '5 0 0 0',
			activetab : 0,
			items : [vergrid, envgrid]
		} ]
	};

	tab.add(panel);
};

ResourceViewer.prototype.addResource = function(type, parentNode) {
    var This = this;
    if(type == "Machine")
    	parentNode = this.treePanel.getStore().getNodeById("_machines");
    else if(type == "Software")
    	parentNode = this.treePanel.getStore().getNodeById("_software");
    else {
		showError('Cannot recognize type '+type);
		return;
	}

    Ext.Msg.prompt("Add a " + type, "Enter name for new " + type + ":", function(btn, name) {
        if (btn == 'ok' && name) {
            var newid = This.libns + getRDFID(name);
            var enode = This.treePanel.getStore().getNodeById(newid);
            if (enode) {
                showError(getRDFID(name) + ' already exists ! Choose a different name.');
                return;
            }
            var url = This.op_url + '/add'+type;
            This.tabPanel.getEl().mask("Adding "+type+"..");
            var params = {resid: newid};
	        var iconCls = (type == 'Machine' ? 'icon-machine fa fa-blue' :
        		'icon-dropbox fa fa-blue');
            Ext.Ajax.request({
                url: url,
                params: params,
                success: function(response) {
                	This.tabPanel.getEl().unmask();
                    if (response.responseText == "OK") {
                        var tmp = {
                            id: newid,
                            text: getLocalName(newid),
                            type: type,
                            iconCls: iconCls,
                            children: []
                        };
                        parentNode.data.leaf = false;
                        parentNode.data.expanded = true;
                        parentNode.appendChild(tmp);
                        This.treePanel.getStore().sort('text', 'ASC');
                    } else 
                    	_console(response.responseText);
                },
                failure: function(response) {
                	This.tabPanel.getEl().unmask();
                    _console(response.responseText);
                }
            });
        }
    });
};

ResourceViewer.prototype.confirmAndDelete = function(node) {
    var This = this;
    var name = node.data.text;
    var id = node.data.id;

    Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + name, function(b) {
        if (b == "yes") {
            var url = This.op_url + '/remove' + node.raw.type;
            This.tabPanel.getEl().mask("Deleting..");
            This.treePanel.getEl().mask("Deleting..");
            Ext.Ajax.request({
                url: url,
                params: {
                    resid: id
                },
                success: function(response) {
                    This.tabPanel.getEl().unmask();
                    This.treePanel.getEl().unmask();
                    if (response.responseText == "OK") {
                        var node = This.treePanel.getStore().getNodeById(id);
                        node.parentNode.removeChild(node);
                        This.tabPanel.remove(This.tabPanel.getActiveTab());
                    } else {
                        _console(response.responseText);
                    }
                },
                failure: function(response) {
                    This.tabPanel.getEl().unmask();
                    This.treePanel.getEl().unmask();
                    _console(response.responseText);
                }
            });
        }
    });
};


ResourceViewer.prototype.getAddMachineMenuItem = function() {
    var This = this;
    return {
        text: 'Add Machine',
        iconCls: 'icon-machine fa-menu fa-blue',
        handler: function() {
            This.addResource("Machine");
        }
    };
};

ResourceViewer.prototype.getAddSoftwareMenuItem = function() {
    var This = this;
    return {
        text: 'Add Software',
        iconCls: 'icon-dropbox fa-menu fa-blue',
        handler: function() {
            This.addResource("Software");
        }
    };
};

ResourceViewer.prototype.getDeleteMenuItem = function() {
    var This = this;
    return {
        text: 'Delete',
        iconCls: 'icon-del fa-menu fa-red',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length || !nodes[0].parentNode)
                return;
            var node = nodes[0];
            if (!node.data.id[0] != "_") {
                This.confirmAndDelete(node);
            } else {
                showError("Cannot delete this item");
            }
        }
    };
};

/*ResourceViewer.prototype.getRenameMenuItem = function() {
    var This = this;
    return {
        text: 'Rename',
        iconCls: 'docsIcon',
        //FIXME: Get an "Edit" icon for this
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length || !nodes[0].parentNode)
                return;
            var node = nodes[0];
            if (!node.data.id[0] != "_") {
                This.confirmAndRename(node);
            } else {
                showError("Cannot rename this item");
            }
        }
    };
};*/

ResourceViewer.prototype.createResourceTreeToolbar = function() {
	var This = this;
	var delItem = This.getDeleteMenuItem();
	delItem.iconCls = 'icon-del fa fa-red';
    return {
        dock: 'top',
        items: [{
            text: 'Add',
            iconCls: 'icon-add fa fa-green',
            menu: [This.getAddMachineMenuItem(), This.getAddSoftwareMenuItem()]
        }, 
        '-', 
        delItem ]
    };
};

ResourceViewer.prototype.onResourceItemContextMenu = function(view, node, item, index, e, eOpts) {
    var This = this;
    e.stopEvent();
    if (!this.topmcmenu) {
        this.topmcmenu = Ext.create('Ext.menu.Menu', {
            items: [This.getAddMachineMenuItem()]
            });
        this.topswmenu = Ext.create('Ext.menu.Menu', {
            items: [This.getAddSoftwareMenuItem()]
            });
        this.menu = Ext.create('Ext.menu.Menu', {
            items: [This.getDeleteMenuItem()]
            });
    }
    if(node.data.id == "_machines")
    	this.topmcmenu.showAt(e.getXY());
    else if(node.data.id == "_software")
    	this.topswmenu.showAt(e.getXY());
    else 
        this.menu.showAt(e.getXY());
};

ResourceViewer.prototype.createLeftPanel = function() {
	var This = this;

	this.treePanel = {
		xtype : 'treepanel',
		width : '20%',
		region : 'west',
		split : true,
		margins : '5 0 5 5',
		cmargins : '5 5 5 5',
        hideHeaders: true,
        rootVisible: false,
        bodyCls: 'x-docked-noborder-top',
		border : true,
		autoScroll : true,
		title : 'Resources',
		//iconCls: 'userIcon',
		url : This.op_url,
		store: Ext.create('Ext.data.TreeStore', {
	        model: 'ResourceRecord',
	        root: This.getTree(This.store),
	        sorters: ['text']
	    }),
	    useArrows: true,
        viewConfig:{stripeRows:true},
		listeners : {
            itemcontextmenu: {
                fn: This.onResourceItemContextMenu,
                scope: this
            },
			itemclick : function(view, rec, item, ind, event) {
				var id = rec.data.id;
		        var type = rec.raw.type;
		        if(!type)
		        	return;
		        
		        var path = getTreePath(rec, 'text');
		        var tabName = getLocalName(id);

		        var tabPanel = this.up('viewport').down('tabpanel');
		        
		        // Check if tab is already open
		        var items = tabPanel.items.items;
		        for (var i = 0; i < items.length; i++) {
		            var tab = items[i];
		            if (tab && tab.title.replace(/^\**/, '') == tabName) {
		                This.tabPanel.setActiveTab(tab);
		                return null;
		            }
		        }

		        // Fetch Store via Ajax
		        var url = This.op_url + '/get'+type+'JSON?resid=' + escape(id);
		        var icon = (type == 'Machine' ? 'icon-machine fa-title fa-blue' :
		        	'icon-dropbox fa-title fa-blue');
		        var guifn = (type == 'Machine' ? 
		        		This.openMachineEditor :
		        		This.openSoftwareEditor);
		        var tab = This.openNewIconTab(tabName, icon);
		        Ext.apply(tab, {
		            path: path,
		            guifn: guifn,
		            args: [tab, id, {}]
		        });
		        tabPanel.setActiveTab(tab);
		        
		        Ext.apply(tab, {
		            loader: {
		                loadMask: true,
		                url: url,
		                renderer: function(loader, response, req) {
		                    var store = Ext.decode(response.responseText);
		                    if (store) {
		                        tab.removeAll();
		                        tab.args[2] = store;
		                        tab.guifn.call(This, tab.args);
		                    }
		                }
		            }
		        });
		        tab.getLoader().load();
		    },
		    
		    tabchange: function(tp, tab) {
		        if (tab.path)
		            this.up('treepanel').selectPath(tab.path, 'text');
		        else
		            this.up('treepanel').getSelectionModel().deselectAll();
		    },
		    
		    render: function() {
		    	This.treePanel = this;
		    }
		}
	};
	
	if(this.is_advanced)
		this.treePanel.tbar = this.createResourceTreeToolbar();
};

ResourceViewer.prototype.openNewIconTab = function(tabname, iconCls) {
    var tab = new Ext.Panel({
        layout: 'fit',
        closable: true,
        iconCls: iconCls,
        title: tabname,
        items: []
        });
    this.tabPanel.add(tab);
    return tab;
};

ResourceViewer.prototype.getTree = function(store) {
	var root = {
	        text: "Resources",
	        id: "_resources",
			iconCls: 'icon-folder fa fa-yellow',
			expIconCls: 'icon-folder-open fa fa-yellow',
	        expanded: true,
	        children: []
	};
	var machines = {
			text: "Machines",
			id: "_machines",
			iconCls: 'icon-folder fa fa-yellow',
			expIconCls: 'icon-folder-open fa fa-yellow',
			//iconCls: "machineIcon",
			expanded: true,
			children: []
	};
	var softwares = {
			text: "Software",
			id: "_software",
			iconCls: 'icon-folder fa fa-yellow',
			expIconCls: 'icon-folder-open fa fa-yellow',
			//iconCls: "softwareIcon",
			expanded: true,
			children: []
	};
	
	for(var i=0; i<store.machines.length; i++) {
		var machineId = store.machines[i];
		machines.children.push({
			text: getLocalName(machineId),
			id: machineId,
			type: "Machine",
			iconCls: 'icon-machine fa fa-blue',
			leaf: true
		});
	}
	
	for(var i=0; i<store.softwares.length; i++) {
		var softwareId = store.softwares[i];
		var snode = {
				text: getLocalName(softwareId),
				id: softwareId,
				type: "Software",
				iconCls: 'icon-dropbox fa fa-blue',
				expanded: true,
				children: []
		};
		softwares.children.push(snode);
	}
	
	root.children = [machines, softwares];
	return root;
};

ResourceViewer.prototype.createTabPanel = function() {
	var This = this;
	this.tabPanel = new Ext.TabPanel({
		region : 'center',
		margins : '5 5 5 0',
		enableTabScroll : true,
		activeTab : 0,
		plain : true,
		resizeTabs : true,
		// minTabWidth: 135,
		// tabWidth: 135,
		items : [ {
			layout : 'fit',
			title : 'Describe Resources',
			autoLoad : {
				url : this.op_url + '/intro'
			}
		} ]
	});
	this.tabPanel.on('tabchange', function(tp, tab) {
		var treePanel = this.up('viewport').down('treepanel');
		if (tab.path)
			treePanel.selectPath(tab.path, 'text');
		else
			treePanel.getSelectionModel().deselectAll();
	});
};

ResourceViewer.prototype.createMainPanel = function() {
	this.mainPanel = Ext.create('Ext.Viewport',
			{
				layout : {
					type : 'border'
				},
				items : [ this.treePanel, this.tabPanel,
						getPortalHeader() ]
			});
};

ResourceViewer.prototype.initialize = function() {
	this.createStoreModels();
	this.createLeftPanel();
	this.createTabPanel();
	this.createMainPanel();
};