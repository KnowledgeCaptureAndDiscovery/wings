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

function DataViewer(guid, store, 
		op_url, upload_url, prov_url,
		dcns, ontns, libns, 
		advanced_user, use_import_ui, has_external_catalog) {
    this.guid = guid;
    this.store = store;
    this.op_url = op_url;
    this.upload_url = upload_url;
    this.prov_url = prov_url;
    this.advanced_user = advanced_user;
    this.use_import_ui = use_import_ui;
    if(this.use_import_ui)
    	this.advanced_user = false;
    this.has_external_catalog = has_external_catalog;

    this.dcns = dcns;
    this.ontns = ontns;
    this.libns = libns;

    this.provenanceViewer = new ProvenanceViewer(guid, prov_url);
    this.dataTreePanel = null;
    this.metricsTreePanel = null;
    this.tabPanel = null;
    this.leftPanel = null;
    this.mainPanel = null;
    this.dataPropRangesStore = {};
    this.dataPropRangeValuesStores = {};

    this.xsd = "http://www.w3.org/2001/XMLSchema#";
}

DataViewer.prototype.createStoreModels = function() {
    if (!Ext.ModelManager.isRegistered('mTreeRecord'))
        Ext.define('mTreeRecord', {
        extend: 'Ext.data.Model',
        fields: ['text', 'isClass']
        });
    if (!Ext.ModelManager.isRegistered('dataTreeRecord'))
        Ext.define('dataTreeRecord', {
        extend: 'Ext.data.Model',
        fields: ['text', 'isClass']
        });
    if (!Ext.ModelManager.isRegistered('dataPropRangeTypes'))
        Ext.define('dataPropRangeTypes', {
        extend: 'Ext.data.Model',
        fields: ['id', 'type']
        });
    if (!Ext.ModelManager.isRegistered('dataPropRangeValues'))
        Ext.define('dataPropRangeValues', {
        extend: 'Ext.data.Model',
        fields: ['id', 'name']
        });
    if (!Ext.ModelManager.isRegistered('dataPropRange'))
        Ext.define('dataPropRange', {
        extend: 'Ext.data.Model',
        fields: ['prop', 'range']
        });
};

DataViewer.prototype.parseNameFormat = function(fmt) {
    var list = [];
    var re = /\[(.+?)\]/;
    var nfmt = fmt;
    var arr1 = [];
    while (match = re.exec(nfmt)) {
        arr1.push(match[1]);
        nfmt = nfmt.replace(re, '|||');
    }
    var arr2 = nfmt.split('|||');
    for (var i = 0; i < arr2.length; i++) {
        if (arr2[i])
            list.push({
            type: 'string',
            val: arr2[i]
            });
        if (arr1[i])
            list.push({
            type: 'prop',
            val: arr1[i]
            });
    }
};

DataViewer.prototype.getPrefixedDataRange = function(rangeid) {
    if (rangeid.indexOf(this.xsd) == 0)
        return rangeid.replace(this.xsd, "xsd:");
    else
        return getLocalName(rangeid);
};

DataViewer.prototype.createMetricsRangeStores_1 = function(node) {
	if(!node) return null;
    var rangevals = [];
    if (node.item.type == 2) {
        // For metric values return the value node
        rangevals.push({
            id: node.item.id,
            name: getLocalName(node.item.id)
        });
    } else if (node.item.type == 1) {
    	var ns = getNamespace(node.item.id);
    	if(ns != this.ontns && ns != this.dcns)
    		return;
        // For metric types, add the type to store
        this.dataPropRangesStore.add({
            id: node.item.id,
            type: getLocalName(node.item.id)
            });
        // for subtypes, get more metric range values back
        for (var i = 0; i < node.children.length; i++) {
            var child = node.children[i];
            var crangevals = this.createMetricsRangeStores_1(child);
            rangevals = rangevals.concat(crangevals);
        }
        // create a new store with the metric range values
        this.dataPropRangeValuesStores[node.item.id] = Ext.create('Ext.data.Store', {
            model: 'dataPropRangeValues',
            data: rangevals
        });
    }
    return rangevals;
};

DataViewer.prototype.createMetricsRangeStores = function(metricTree) {
    this.dataPropRangesStore = Ext.create('Ext.data.Store', {
        model: 'dataPropRangeTypes',
        data: [{
            id: this.xsd + 'string',
            type: 'xsd:string'
        }, {
            id: this.xsd + 'boolean',
            type: 'xsd:boolean'
        }, {
            id: this.xsd + 'int',
            type: 'xsd:int'
        }, {
            id: this.xsd + 'float',
            type: 'xsd:float'
        }, {
			id : this.xsd + 'date',
			type : 'xsd:date'
		}
        ]
        });
    // Set dataRangeTypes from User defined Metrics
    this.createMetricsRangeStores_1(metricTree);
};

DataViewer.prototype.expandPropertyRecord = function(pinfo) {
    var pname = getRDFID(pinfo.prop);
    var range = pinfo.range;
    if (range.match(/^xsd:/))
        range = range.replace(/^xsd:/, this.xsd);
    else
        range = this.ontns + range;
    return {
        prop: pname,
        pid: this.ontns + pname,
        range: range
    };
};

DataViewer.prototype.importFromExternalCatalog = function(win, dtypeid, xdataid, location, metadata) {
	var This = this;
	var tab = This.tabPanel.getActiveTab();
	var dataname = getLocalName(xdataid);
	var dataid = this.libns + dataname;
	var propvals = {};
	for(var pid in metadata) {
		var mpropid = this.ontns + getLocalName(pid);
		propvals[mpropid] = metadata[pid];
	}

    Ext.get(win.getId()).mask("Importing..");
    var url = this.op_url + "/importFromExternalCatalog";
    Ext.Ajax.request({
        url: url,
        params: {
        	data_id: dataid,
        	data_type: dtypeid,
        	location: location,
        	propvals_json: Ext.encode(propvals)
        },
        success: function(response) {
            Ext.get(win.getId()).unmask();
            if (response.responseText == "OK") {
                var tmp = This.getTree({
                    item: {
                        id: dataid,
                        type: 2
                    },
                    children: []
                });
                
            	var parentNode = This.dataTreePanel.getStore().getNodeById(dtypeid);
                parentNode.data.leaf = false;
                parentNode.data.expanded = true;
                parentNode.appendChild(tmp);
                This.dataTreePanel.getStore().sort('text', 'ASC');
            	
                var tab = This.tabPanel.getActiveTab();
            	if(tab && tab.guifn)
            		tab.getLoader().load();
            	win.close();
            } else 
            	_console(response.responseText);
        },
        failure: function(response) {
        	Ext.get(win.getId()).unmask();
            _console(response.responseText);
        }
    });
};

DataViewer.prototype.openDataTypeEditor = function(args) {
    var tab = args[0];
    var id = args[1];
    var store = args[2];

    var name = getLocalName(id);
    var This = this;

    // DataType's NameFormat field
    var nameFormatField = new Ext.form.field.Text({
        width: '95%',
        //fieldStyle: 'border:1px solid #EEE',
        value: store.name_format
    });

    // Extract inherited properties and own properties
    var myProperties = [];
    var inhProperties = [];
    var pprops = store.properties;

    Ext.iterate(pprops, function(prop) {
        if (!prop.range)
            return true;
        // i.e. continue with loop
        // Convert xsd:integer to xsd:int (to be consistent)
        if (prop.range == This.xsd + 'integer')
            prop.range = This.xsd + 'int';

        var mine = false;
        for (var i = 0; i < prop.domains.length; i++) {
            if (prop.domains[i] == id)
                mine = true;
        }
        if (mine)
            myProperties.push({
            prop: getLocalName(prop.id),
            range: This.getPrefixedDataRange(prop.range)
            });
        else
            inhProperties.push({
            prop: getLocalName(prop.id),
            range: This.getPrefixedDataRange(prop.range)
            });
    });

    // Create a store for the editable properties
    var dataTypeStore = new Ext.data.Store({
        model: 'dataPropRange',
        data: myProperties
    });

    // Create Save Button and it's handler
    var propmods = {addedProperties:{}, deletedProperties:{}, modifiedProperties:{} };
    var savebtn;
    if (This.advanced_user) {
        var savebtn = new Ext.Button({
            text: 'Save',
            iconCls: 'icon-save fa fa-blue',
            disabled: true,
            handler: function() {
                propmods.addedProperties = {};
                propmods.modifiedProperties = {};
                gridPanel.getStore().each(function(rec) {
                    // If this is a new or modified row/record
                    if (rec.dirty) {
                        var sdata = This.expandPropertyRecord(rec.data);
                        rec.set('prop', sdata.prop);
                        var mod = rec.modified;
                        if (!mod.prop && !mod.range) {
                            propmods.addedProperties[This.ontns + sdata.prop] = sdata;
                        } else {
                            var prop = mod.prop ? mod.prop: sdata.prop;
                            propmods.modifiedProperties[This.ontns + prop] = sdata;
                        }
                    }
                });
                var nf = nameFormatField.value;
                This.saveDatatype(id, propmods, nf, gridPanel, savebtn, tab);
            }
        });
    }

    dataTypeStore.on('add', function() {
        tab.setTitle("*" + tab.title.replace(/^\*/, ''));
        savebtn.setDisabled(false);
    });
    dataTypeStore.on('remove', function() {
        tab.setTitle("*" + tab.title.replace(/^\*/, ''));
        savebtn.setDisabled(false);
    });
    dataTypeStore.on('update', function() {
        tab.setTitle("*" + tab.title.replace(/^\*/, ''));
        savebtn.setDisabled(false);
    });
    nameFormatField.on('change', function() {
        tab.setTitle("*" + tab.title.replace(/^\*/, ''));
        savebtn.setDisabled(false);
    });

    var opts = {
        style: 'font-size:11px;padding-top:0px'
    };
    var This = this;
    var rangeEditor = new Ext.form.ComboBox({
        store: This.dataPropRangesStore,
        displayField: 'type',
        queryMode: 'local',
        listStyle: 'font-size:11px',
        forceSelection: true,
        typeAhead: true,
        triggerAction: 'all'
    });
    var propEditor = new Ext.form.TextField(opts);
    Ext.apply(rangeEditor, opts);

    
    var uploadfilesbtn = {
            xtype: 'button',
            text: 'Upload Files ('+getLocalName(id)+')',
            iconCls: 'icon-upload fa fa-blue',
            handler: function() {
                var win = new Ext.Window({
                    xtype: 'panel',
                    title: 'Upload',
                    url: This.upload_url,
				    items : [{
						xtype : 'xuploadpanel',
						url : This.upload_url,
						preventHeader: true,
						border: false,
                    	multipart_params: {type: 'data'},
						multi_selection: true,
	                    width: 500,
	                    height: 300,
						addButtonCls : 'icon-add fa fa-green',
						deleteButtonCls : 'icon-del fa fa-red',
						uploadButtonCls : 'icon-upload fa fa-blue',
						cancelButtonCls : 'icon-del fa fa-red',
                        listeners : {
                        	"uploadcomplete" : function(item, files) {
                        		This.addBatchData(files, id);
                        	}
                        }
				    }]
                });
                win.show();
            }
    };
    
    var mainTbar;
    if(!This.use_import_ui) {
    	mainTbar = [];
        if(This.advanced_user)
        	mainTbar.push(savebtn);
		mainTbar.push({
			iconCls : 'icon-reload fa fa-green',
			text : 'Reload',
			handler : function() {
				tab.getLoader().load();
				savebtn.setDisabled(true);
				tab.setTitle(tab.title.replace(/^\*/, ''));
			}
		});
        mainTbar.push('-');
        mainTbar.push({xtype: 'tbfill'});
        mainTbar.push(uploadfilesbtn);
		if (this.has_external_catalog)
			mainTbar.push({
				iconCls: 'icon-download-cloud fa fa-blue',
				text : 'Import from External Catalog',
				handler: function() {
	                var win = new Ext.Window({
	                    xtype: 'panel',
	                    layout: 'border',
	                    border: false,
	                    modal: true,
	                    title: 'Import '+name+' from External Catalog',
	                    height: '90%',
	                    width: '90%',
	                    listeners : {
	                    	"import" : function(impid, location, metadata) {
	                    		This.importFromExternalCatalog(win, id, impid, location, metadata);
	                    	}
	                    }
	                });
	                win.show();
	                
		            var url = This.op_url + '/external/getDataHierarchyJSON';
		            Ext.get(win.getId()).mask("Loading..");
		            Ext.Ajax.request({
		                url: url,
		                success: function(response) {
		                    Ext.get(win.getId()).unmask();
		                    var tree = Ext.decode(response.responseText);
		                    var store = {tree: tree, metrics: null};
		                    var extdv = new DataViewer(
		                    		This.guid+"_external", store, 
		                    		This.op_url+'/external', This.upload_url, 
		                    		This.prov_url,
		                    		This.dcns, This.ontns, This.libns, 
		                    		This.advanced_user, true, false);
		                    extdv.mainPanel = win;
		                    extdv.initialize();
		                },
		                failure: function(response) {
		                	Ext.get(win.getId()).unmask();
		                    _console(response.responseText);
		                }
		            });
					//window.open(This.op_url+"/external");
				}
			});
    }
    
    var typeTabPanel = new Ext.tab.Panel({
        plain: true,
        margin: 5
    });

    var tbar = null;
    var plugins = [];
    var sm = null;
    
    if (This.advanced_user) {
        var editorPlugin = Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 1
        });
        plugins = [editorPlugin];
        
        sm = Ext.create('Ext.selection.CheckboxModel', {
            checkOnly: true,
            listeners: {
                selectionchange: function(sm, selections) {
                    gridPanel.down('#delProperty' + name).setDisabled(selections.length == 0);
                }
            }
        });

        tbar = [];
        tbar.push({
            text: 'Add Property',
            iconCls: 'icon-add fa fa-green',
            handler: function() {
                var p = new dataPropRange();
                p.set('range', "xsd:string");
                var pos = dataTypeStore.getCount();
                editorPlugin.cancelEdit();
                dataTypeStore.insert(pos, p);
                editorPlugin.startEditByPosition({
                    row: pos,
                    column: 1
                });
            }
        });
        tbar.push('-');
        tbar.push({
            text: 'Delete Property',
            iconCls: 'icon-del fa fa-red',
            id: 'delProperty' + name,
            disabled: true,
            handler: function() {
                editorPlugin.cancelEdit();
                var s = typeTabPanel.gridPanel.getSelectionModel().getSelection();
                for (var i = 0, r; r = s[i]; i++) {
                    var prop = (r.modified && r.modified.hasOwnProperty('prop')) ? r.modified.prop: r.get('prop');
                    if (prop == null) {
                        // This property was just added, don't mark it as a
                        // deletedProperty for the server
                    } else if (prop != "") {
                        propmods.deletedProperties[This.ontns + prop] = 1;
                    }
                    dataTypeStore.remove(r);
                }
            }
        });
    }

    // Show properties
    var gridPanel = new Ext.grid.GridPanel({
        columnLines: true,
        autoHeight: true,
        plugins: plugins,
        //title: 'Metadata Properties for ' + name,
        columns: [{
            dataIndex: 'prop',
            flex: 1,
            header: 'Property',
            editor: propEditor,
            editable: This.advanced_user ? true: false,
            menuDisabled: true
        }, {
            dataIndex: 'range',
            flex: 1,
            header: 'Range',
            editor: rangeEditor,
            editable: This.advanced_user ? true: false,
            menuDisabled: true
        }],
        selModel: sm,
        clicksToEdit: 1,
        store: dataTypeStore,
        border: true,
        tbar: tbar
    });
    
    var typePropsPanel = new Ext.Panel({
        border: false,
        title: 'Metadata Properties',
        defaults: {
            padding: 4
        },
        autoScroll: true,
        items: gridPanel
    });
    
    typeTabPanel.gridPanel = gridPanel;
    
    // Show inherited Properties
    if (inhProperties.length) {
        // Store for the inherited properties
        var inhStore = new Ext.data.Store({
            model: 'dataPropRange',
            data: inhProperties
        });
        var inhGridPanel = new Ext.grid.GridPanel({
            title: "Inherited Metadata Properties",
            margin: "5 0 0 0",
            bodyCls: 'upDownBorder',
            columns: [{
                dataIndex: 'prop',
                header: 'Property',
                flex: 1,
                menuDisabled: true
            }, {
                dataIndex: 'range',
                header: 'Range',
                flex: 1,
                menuDisabled: true
            }],
            autoHeight: true,
            bodyCls: 'inactive-grid',
            columnLines: true,
            border: true,
            store: inhStore
        });
        typePropsPanel.add(inhGridPanel);
    }
    
    typeTabPanel.add(typePropsPanel);
    
    if(!This.use_import_ui)
    	typeTabPanel.add({
    		xtype: 'panel',
    		title: 'Name Format',
    		defaults: {
        		margin: 5,
        		border: false
    		},
            autoScroll: true,
    		items: [nameFormatField, 
    		{
    			html: "Add a NameFormat for files produced of this data type. Examples:<ul>" +
    				"<li>gene_[hasGeneId].txt (hasGeneID would be a Metadata Property " +
    				"for this Datatype)</li>" +
    				"<li>[__ID].txt (__ID is an inbuilt keyword signifying a generated " +
    				"unique id)</li>" +
    				"<li>[hasGeneId]_[__ID].csv (combination of Metadata Property and __ID)</li>" +
    				"</ul>"
    		}]
    	});

    typeTabPanel.add(This.provenanceViewer.createItemProvenanceGrid(id));
    typeTabPanel.setActiveTab(0);
    
    var mainPanel = new Ext.Panel({
        region: 'center',
        border: false,
        layout: 'fit',
        tbar: mainTbar,
        //autoScroll: true,
        items: typeTabPanel
    });
    tab.add(mainPanel);
};

DataViewer.prototype.confirmAndRenameData = function(node) {
    var This = this;
    var dataid = node.data.id;
    var dataName = getLocalName(dataid);

    Ext.Msg.prompt("Rename " + dataName, "Enter new name:", function(btn, text) {
        if (btn == 'ok' && text) {
            var newName = getRDFID(text);
            var newid = This.libns + newName;
            var enode = This.dataTreePanel.getStore().getNodeById(newid);
            if (enode) {
                showError(getRDFID(text) + ' already exists ! Choose a different name.');
                This.confirmAndRenameData(node);
                return;
            }
            var url = This.op_url + '/renameData';
            Ext.get(This.tabPanel.getId()).mask("Renaming..");
            Ext.get(This.dataTreePanel.getId()).mask("Renaming..");
            Ext.Ajax.request({
                url: url,
                params: {
                    data_id: dataid,
                    newid: newid
                },
                success: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    if (response.responseText == "OK") {
                    	node.set('text', newName);
                    	node.set('id', newid);
                    	node.commit();
                    	This.dataTreePanel.getStore().sort('text', 'ASC');
                    } else 
                    	_console(response.responseText);
                },
                failure: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    }, this, false, dataName);
};

DataViewer.prototype.confirmAndRenameDatatype = function(node) {
    var This = this;
    if(!node.parentNode)
    	return;
    
    var dtypeid = node.data.id;
    var dtypeName = getLocalName(dtypeid);

    Ext.Msg.prompt("Rename " + dtypeName, "Enter new name:", function(btn, text) {
        if (btn == 'ok' && text) {
            var newName = getRDFID(text);
            var newid = This.ontns + newName;
            var enode = This.dataTreePanel.getStore().getNodeById(newid);
            if (enode) {
                showError(getRDFID(text) + ' already exists ! Choose a different name.');
                This.confirmAndRenameDatatype(node);
                return;
            }
            var url = This.op_url + '/renameDataType';
            Ext.get(This.tabPanel.getId()).mask("Renaming..");
            Ext.get(This.dataTreePanel.getId()).mask("Renaming..");
            Ext.Ajax.request({
                url: url,
                params: {
                    data_type: dtypeid,
                    newid: newid
                },
                success: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    if (response.responseText == "OK") {
                    	node.set('text', newName);
                    	node.set('id', newid);
                    	node.commit();
                    	This.dataTreePanel.getStore().sort('text', 'ASC');
                    } else 
                    	_console(response.responseText);
                },
                failure: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    }, this, false, dtypeName);
};

DataViewer.prototype.confirmAndDeleteData = function(node) {
    var This = this;
    var dataName = node.data.text;
    var id = node.data.id;

    Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + dataName, function(b) {
        if (b == "yes") {
            var url = This.op_url + '/delData';
            Ext.get(This.tabPanel.getId()).mask("Deleting..");
            Ext.get(This.dataTreePanel.getId()).mask("Deleting..");
            Ext.Ajax.request({
                url: url,
                params: {
                    data_id: id
                },
                success: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    if (response.responseText == "OK") {
                        var node = This.dataTreePanel.getStore().getNodeById(id);
                        node.parentNode.removeChild(node);
                        This.tabPanel.remove(This.tabPanel.getActiveTab());
                    } else {
                        _console(response.responseText);
                    }
                },
                failure: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    });
};

DataViewer.prototype.confirmAndDeleteDatatype = function(node) {
    var This = this;
    var msg = "Are you sure you want to Delete " + node.data.text;
    msg += ".. This will also delete the files under this Datatype";
    Ext.MessageBox.confirm("Confirm Delete", msg, function(b) {
        if (b == "yes") {
            This.deleteDatatype(node, true);
        }
    });
};

DataViewer.prototype.openDataEditor = function(args) {
    var tab = args[0];
    var id = args[1];
    var store = args[2];

    var This = this;

    var customEditors = {};
    var propertyNames = {};
    var propVals = [];

    Ext.iterate(store.props, function(pinfo) {
        prop = pinfo.id;
        var ed = null;
        if (!pinfo.range)
            return true;
        // i.e. skip if the property doesn't have a range
        if (pinfo.range == This.xsd + "int" || pinfo.range == This.xsd + "int") {
            ed = new Ext.form.NumberField({
                allowDecimals: false
            });
        } else if (pinfo.range == This.xsd + "float") {
            ed = new Ext.form.NumberField({
                decimalPrecision: 6
            });
        } else if (pinfo.range == This.xsd + "date") {
            ed = new Ext.form.DateField({
                format: 'Y-m-d'
            });
        } else if (pinfo.range == This.xsd + "boolean") {
            ed = new Ext.form.field.ComboBox({
                editable: false,
                store: [[true, 'true'], [false, 'false']]
                });
        } else if (This.dataPropRangeValuesStores[pinfo.range]) {
            ed = new Ext.form.ComboBox({
                store: This.dataPropRangeValuesStores[pinfo.range],
                queryMode: 'local',
                displayField: 'name',
                //valueField : 'id',
                forceSelection: true,
                typeAhead: true,
                triggerAction: 'all'
            });
        } else {
            ed = new Ext.form.TextField();
        }
        propVals[prop] = '';
        propertyNames[prop] = getLocalName(prop);
        if (ed != null)
            customEditors[prop] = ed;
    });

    var savebtn = new Ext.Button({
        text: 'Save',
        iconCls: 'icon-save fa fa-blue',
        disabled: true,
        handler: function() {
            var data = [];
            gridPanel.getStore().each(function(rec) {
                // data.push(rec.data);
                var name = rec.data.name;
                var val = rec.data.value;
                if (val == undefined)
                    return true;
                if (val instanceof Date) {
                	val.setHours(1, 0, 0, 0);
                	val = val.toJSON().replace(/T.*$/, '');
                }
                // i.e. continue
                data.push({
                    name: name,
                    value: val
                });
            });
            This.saveData(id, data, savebtn, tab);
            // console.log(data),
        }
    });

    var delbtn = new Ext.Button({
        text: 'Delete File',
        iconCls: 'icon-del fa fa-red',
        handler: function() {
            This.confirmAndDeleteFile(id);
        }
    });

    for (var i = 0; i < store.vals.length; i++) {
        pval = store.vals[i];
        // Ignore properties that aren't part of the provided declared properties
        if (!propertyNames[pval.propertyId])
            continue;
        if (pval.value == null)
            pval.value = '';
        propVals[pval.propertyId] = (pval.type == 2 ? pval.value: getLocalName(pval.value));
    }

    var gridPanel = new Ext.grid.property.Grid({
        autoScroll: true,
        border: true,
        source: propVals,
        propertyNames: propertyNames,
        columnLines: true,
        nameColumnWidth: '50%',
        customEditors: customEditors,
        title: 'Metadata', // for ' + getLocalName(id),
        listeners: { 'beforeedit': function (e) { return !This.use_import_ui; } },
        //tbar: This.use_import_ui ? null : [savebtn]
    });
    
    if(this.use_import_ui)
    	gridPanel.getSelectionModel().setLocked(true);
    
    var dataStore = gridPanel.getStore();
    dataStore.on('add', function() {
        tab.setTitle("*" + tab.title.replace(/^\*/, ''));
        savebtn.setDisabled(false);
    });
    dataStore.on('remove', function() {
        tab.setTitle("*" + tab.title.replace(/^\*/, ''));
        savebtn.setDisabled(false);
    });
    dataStore.on('update', function() {
        tab.setTitle("*" + tab.title.replace(/^\*/, ''));
        savebtn.setDisabled(false);
    });

    var addfilebtn = {
        xtype: 'button',
        text: 'Upload/Set Path',
        iconCls: 'icon-add fa fa-green',
        handler: function() {
            var win = new Ext.Window({
                xtype: 'panel',
                layout: {
                    type: 'accordion'
                },
                width: 500,
                items: [{
                    xtype: 'panel',
                    title: 'Set File Path/URL',
                    border: false,
                    layout: {
                        type: 'hbox',
                        defaultMargins: 10
                    },
                    items: [{
                        xtype: 'textfield',
                        flex: 1,
                        value: store.location,
                        emptyText: 'Enter a file path/url or Upload from below'
                    }, {
                        xtype: 'button',
                        text: 'Submit',
                        iconCls: 'icon-add fa fa-green',
                        handler: function() {
                        	var panel = this.up('panel');
                        	var loc = this.prev().value;
                        	if(!loc) {
                        		showError('Please enter the data location or Upload from below');
                        		return;
                        	}
                        	This.setDataLocation(id, loc, tab.down('#downloadFile'), store, win);
                        }
                    }]
                }, {
                    	xtype: 'xuploadpanel',
                    	collapsed: true,
                    	title: 'Upload File',
                    	collapsible: true,
                    	border: false,
                    	multipart_params: {type: 'data'},
                        url: This.upload_url,
                        addButtonCls: 'icon-add fa fa-green',
                        deleteButtonCls: 'icon-del fa fa-red',
                        uploadButtonCls: 'icon-upload fa fa-blue',
                        cancelButtonCls: 'icon-del fa fa-red',
                        listeners : {
                        	"uploadcomplete" : function(item, files) {
                        		// Just check the first file (only one file upload allowed here)
                        		var file = files[0];
                        		var loc = file.location;
                        		This.setDataLocation(id, loc, tab.down('#downloadFile'), store, win);
                        	}
                        }
                }]
            });
            win.show();
        }
    };
    
    var tbar;
    if(!This.use_import_ui) {
    	tbar = [
    	savebtn, 
    	{
	        iconCls: 'icon-reload fa fa-green',
	        text: 'Reload',
	        handler: function() {
	            tab.getLoader().load();
	            savebtn.setDisabled(true);
	            tab.setTitle(tab.title.replace(/^\*/, ''));
	        }
	    }, 
	    '-',
	    { xtype: 'tbfill' },
	    addfilebtn, 
		{
		    iconCls: 'icon-download fa fa-blue',
		    itemId: 'downloadFile',
		    text: 'Download File',
		    disabled: !store.location,
		    handler: function() {
		    	window.open(This.op_url+"/fetch?data_id="+escape(id));
		        //showWingsMessage('Location: '+ store.location, 'Location of '+ getLocalName(id), null, 400, 100);
		    }
	    }];
    }
    else {
    	tbar = [
    	{
    		iconCls: 'icon-download-cloud fa-blue',
    		itemId: 'importFile',
    		text: 'Import into Local Catalog',
    		handler: function() {
    			This.mainPanel.fireEvent("import", id, store.location, gridPanel.getSource());
    			/*console.log(id);
    			console.log(store.location);
    			console.log(gridPanel.getSource());*/
    		}
    	}];
    }
    
    var provGrid = This.provenanceViewer.createItemProvenanceGrid(id);
    
    var mainPanel = new Ext.Panel({
        region: 'center',
        border: false,
        tbar: tbar,
        layout: 'fit',
        items: {
        	xtype: 'tabpanel',
        	plain: true,
        	margin: 5,
        	items: [gridPanel, provGrid]
        }
    });
    tab.add(mainPanel);
};

DataViewer.prototype.refreshInactiveTabs = function() {
    /*var activeTab = this.tabPanel.getActiveTab();
	var items = this.tabPanel.items.items;
	for(var i=0; i<items.length; i++) {
		var tab = items[i];
		if(tab && tab.guifn && tab != activeTab) {
			//tab.getLoader().load({url: tab.url});
			tab.getLoader().load();
			//tab.guifn.call(this, tab.args);
		}
	}*/
    };

DataViewer.prototype.getAddDatatypeMenuItem = function() {
    var This = this;
    return {
        text: 'Add Datatype',
        iconCls: 'icon-folder-open fa-menu fa-yellow',
        handler: function() {
            var nodes = This.dataTreePanel.getSelectionModel().getSelection();
            var node = (nodes && nodes.length) ? nodes[0] : null;
            This.addDatatype(node);
        }
    };
};

DataViewer.prototype.getAddDataMenuItem = function() {
    var This = this;
    return {
        text: 'Add Data',
        iconCls: 'icon-file-alt fa-menu fa-blue',
        handler: function() {
            var nodes = This.dataTreePanel.getSelectionModel().getSelection();
            var node = (nodes && nodes.length) ? nodes[0] : null;
            This.addDataForType(node);
        }
    };
};

DataViewer.prototype.getDeleteMenuItem = function() {
    var This = this;
    return {
        text: 'Delete',
        iconCls: 'icon-del fa fa-red',
        handler: function() {
            var nodes = This.dataTreePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length || !nodes[0].parentNode)
                return;
            var node = nodes[0];
            if (!node.data.isClass) {
                This.confirmAndDeleteData(node);
            } else {
                This.confirmAndDeleteDatatype(node);
            }
        }
    };
};

DataViewer.prototype.getRenameMenuItem = function() {
    var This = this;
    return {
        text: 'Rename',
        iconCls: 'icon-edit fa-menu fa-blue',
        handler: function() {
            var nodes = This.dataTreePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length || !nodes[0].parentNode)
                return;
            var node = nodes[0];
            if (!node.data.isClass) {
                This.confirmAndRenameData(node);
            } else {
                This.confirmAndRenameDatatype(node);
            }
        }
    };
};

DataViewer.prototype.createDataTreeToolbar = function() {
    var This = this;
    var toolbar = Ext.create('Ext.toolbar.Toolbar', {
        dock: 'top',
        items: [{
            text: 'Add',
            iconCls: 'icon-add fa fa-green',
            menu: [This.getAddDatatypeMenuItem(), This.getAddDataMenuItem()]
            }, '-', This.getDeleteMenuItem()]
        });
    This.dataTreePanel.addDocked(toolbar);
    This.dataTreePanel.doComponentLayout();
};

DataViewer.prototype.onDataItemContextMenu = function(dataview, node, item, index, e, eOpts) {
    var This = this;
    e.stopEvent();
    if (!this.menu) {
    	var delitem = This.getDeleteMenuItem();
    	delitem.iconCls = 'icon-del fa-menu fa-red';
        this.menu = Ext.create('Ext.menu.Menu', {
            items: [This.getAddDatatypeMenuItem(), This.getAddDataMenuItem(), '-', 
                    This.getRenameMenuItem(), delitem]
            });
        this.datamenu = Ext.create('Ext.menu.Menu', {
            items: [This.getRenameMenuItem(), delitem]
            });
        this.topmenu = Ext.create('Ext.menu.Menu', {
            items: [This.getAddDatatypeMenuItem(), This.getAddDataMenuItem()]
            });
    }
    if(this.advanced_user) {
	    if (node.parentNode) {
	    	if(node.data.isClass)
	    		this.menu.showAt(e.getXY());
	    	else
	    		this.datamenu.showAt(e.getXY());
	    }
	    else
	        this.topmenu.showAt(e.getXY());
    }
};

DataViewer.prototype.createDataTreePanel = function(dataHierarchy) {
    var This = this;
    this.dataTreeStore = Ext.create('Ext.data.TreeStore', {
        model: 'dataTreeRecord',
        root: This.getTree(dataHierarchy),
        sorters: ['text']
        });

    this.dataTreePanel = new Ext.tree.TreePanel({
        width: '100%',
        border: true,
        autoScroll: true,
        title: 'Data',
        iconCls: 'icon-file-alt fa-title fa-blue',
        containerScroll: true,
        store: This.dataTreeStore,
        url: This.op_url,
	    useArrows: true,
        viewConfig: {
            plugins: {
                ptype: 'treeviewdragdrop',
                enableDrag: This.advanced_user ? true: false,
                ddGroup: This.guid + '_DataTree',
                appendOnly: true,
                dragText: 'Drag Datatype to its new Parent'
            },
            listeners: {
                itemcontextmenu: {
                    fn: This.onDataItemContextMenu,
                    scope: this
                }
            },
            stripeRows: true
        },
        });

    // Create toolbar for advanced users
    if (this.advanced_user) {
        this.createDataTreeToolbar(this.dataTreePanel);
        this.dataTreePanel.doComponentLayout();
    }

    this.dataTreePanel.on("itemclick", Ext.Function.bind(this.handleTreeClick, this));

    this.dataTreePanel.getStore().on('move', function(node, oldp, newp) {
        This.moveDataitemTo(node.data.id, node.data.isClass, oldp.data.id, newp.data.id);
    });
};

DataViewer.prototype.handleTreeClick = function(view, rec, item, ind, event) {
    var id = rec.data.id;
    var tabName = rec.data.text;
    var path = getTreePath(rec, 'text');

    var This = this;

    // Check if tab is already open
    var items = this.tabPanel.items.items;
    for (var i = 0; i < items.length; i++) {
        var tab = items[i];
        if (tab && tab.title.replace(/^\**/, '') == tabName) {
            this.tabPanel.setActiveTab(tab);
            return null;
        }
    }
    // Fetch Data/Datatype details via ajax calls
    if (rec.data.isClass)
        url = this.op_url + '/getDataTypeJSON?data_type=' + escape(id);
    else
        url = this.op_url + '/getDataJSON?data_id=' + escape(id);

    var tab = this.openNewIconTab(tabName, (rec.data.isClass ? 
    		'icon-folder-open fa-title fa-yellow': 
    			'icon-file-alt fa-title fa-blue'));

    Ext.apply(tab, {
        path: path,
        guifn: (rec.data.isClass ? this.openDataTypeEditor: this.openDataEditor),
        args: [tab, id, {}],
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
    this.tabPanel.setActiveTab(tab);
    tab.getLoader().load();
};

DataViewer.prototype.createMetricsTreeToolbar = function() {
    var This = this;
    var toolbar = Ext.create('Ext.toolbar.Toolbar', {
        dock: 'top',
        items: [{
            text: 'Add',
            iconCls: 'icon-add fa fa-green',
            handler: function() {
                var nodes = This.metricsTreePanel.getSelectionModel().getSelection();
                var node = (nodes && nodes.length) ? nodes[0] : null;
                This.addMetric(node);
            }
        }, '-', {
            text: 'Delete',
            iconCls: 'icon-del fa fa-red',
            handler: function() {
                var nodes = This.metricsTreePanel.getSelectionModel().getSelection();
                if (!nodes || !nodes.length) {
                    return;
                }
                var node = nodes[0];
                if (!node.parentNode) {
                    return;
                }
                This.deleteMetric(node);
            }
        }]
        });
    this.metricsTreePanel.addDocked(toolbar);
    this.metricsTreePanel.doComponentLayout();
};

DataViewer.prototype.createMetricsTreePanel = function(metricsHierarchy) {
    var root = this.getTree(metricsHierarchy);
    this.metricsTreePanel = new Ext.tree.TreePanel({
        width: '100%',
        border: false,
        autoScroll: true,
        title: 'Metrics',
        iconCls: 'icon-file-alt fa-title fa-blue',
        // TEMPORARY FIXME
        hidden: true,
        containerScroll: true,
        store: Ext.create('Ext.data.TreeStore', {
            model: 'mTreeRecord',
            root: root
        }),
        url: this.op_url
    });
    // Create toolbar for advanced users
    if (this.advanced_user) {
        this.createMetricsTreeToolbar();
    }
};

DataViewer.prototype.getTree = function(data) {
	if(!data) return null;
	
    var item = data.item;
    var treenode = {
        text: getLocalName(item.id),
        id: item.id,
        isClass: (item.type == 1),
        leaf: (item.type == 1 ? false: true),
        iconCls: (item.type == 1 ? 'icon-folder fa fa-yellow': 
    		'icon-file-alt fa fa-blue'),
        expIconCls: (item.type == 1 ? 'icon-folder-open fa fa-yellow': 
    		'icon-file-alt fa fa-blue'),
        expanded: true,
        draggable: (item.type == 1),
        children: []
        };
    if (data.children) {
        for (var i = 0; i < data.children.length; i++)
            treenode.children.push(this.getTree(data.children[i]));
    }
    return treenode;
};

DataViewer.prototype.openNewIconTab = function(tabname, iconCls) {
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

DataViewer.prototype.addDataForType = function(parentNode) {
    var This = this;
    if (!parentNode) {
        showError('Please select a Datatype from below to add data for');
        return;
    }
    if (!parentNode.data.isClass) {
        showError('Please select a Datatype from below to add data for');
        return;
    }
    var dtype = parentNode.data.id;
    var dtypeName = getLocalName(dtype);

    Ext.Msg.prompt("Add a " + dtypeName, "Enter name for new " + dtypeName + ":", function(btn, text) {
        if (btn == 'ok' && text) {
        	This.addData(text, dtype, parentNode);
        }
    });
};

DataViewer.prototype.addData = function(data_name, dtypeid, parentNode) {
	var This = this;
    var newid = This.libns + getRDFID(data_name);
    var enode = This.dataTreePanel.getStore().getNodeById(newid);
    if (enode) {
        showError(getRDFID(text) + ' already exists ! Choose a different name.');
        return;
    }
    var url = This.op_url + '/addDataForType';
    Ext.get(This.tabPanel.getId()).mask("Adding Data..");
    Ext.Ajax.request({
        url: url,
        params: {
            data_id: newid,
            data_type: dtypeid
        },
        success: function(response) {
        	Ext.get(This.tabPanel.getId()).unmask();
            if (response.responseText == "OK") {
                var tmp = This.getTree({
                    item: {
                        id: newid,
                        type: 2
                    },
                    children: []
                });
                parentNode.data.leaf = false;
                parentNode.data.expanded = true;
                parentNode.appendChild(tmp);
                This.dataTreePanel.getStore().sort('text', 'ASC');
            } else 
            	_console(response.responseText);
        },
        failure: function(response) {
        	Ext.get(This.tabPanel.getId()).unmask();
            _console(response.responseText);
        }
    });
};

DataViewer.prototype.saveData = function(dataid, propvals, savebtn, tab) {
	var This = this;
    var url = This.op_url + '/saveDataJSON';
    Ext.get(This.tabPanel.getId()).mask("Saving..");
    Ext.Ajax.request({
        url: url,
        params: {
            propvals_json: Ext.encode(propvals),
            data_id: dataid
        },
        success: function(response) {
            Ext.get(This.tabPanel.getId()).unmask();
            if (response.responseText == "OK") {
                savebtn.setDisabled(true);
                tab.setTitle(tab.title.replace(/^\*/, ''));
                This.refreshInactiveTabs();
            } else {
                _console(response.responseText);
            }
        },
        failure: function(response) {
            Ext.get(This.tabPanel.getId()).unmask();
            _console(response.responseText);
        }
    });
};

DataViewer.prototype.addBatchData = function(files, dtypeid) {
	var This = this;
	var parentNode = This.dataTreePanel.getStore().getNodeById(dtypeid);

	var dataids = [];
	var datalocs = [];
	var errors = "";
	for(var i=0; i<files.length; i++) {
		var file = files[i];
	    var newid = This.libns + getRDFID(file.name);
	    var enode = This.dataTreePanel.getStore().getNodeById(newid);
	    if (enode) {
	        errors += getRDFID(text) + ' already exists ! Not adding';
	    }
	    dataids.push(newid);
	    datalocs.push(file.location);
	}
	if(errors)
		showError(errors);
	
    var url = This.op_url + '/addBatchData';
    Ext.get(This.tabPanel.getId()).mask("Adding Data..");
    Ext.Ajax.request({
        url: url,
        params: {
            data_type: dtypeid,
            data_ids: Ext.encode(dataids),
            data_locations: Ext.encode(datalocs)
        },
        success: function(response) {
        	Ext.get(This.tabPanel.getId()).unmask();
            if (response.responseText == "OK") {
            	for(var i=0; i<dataids.length; i++) {
	                var tmp = This.getTree({
	                    item: {
	                        id: dataids[i],
	                        type: 2
	                    },
	                    children: []
	                });
	                parentNode.appendChild(tmp);
            	}
                parentNode.data.leaf = false;
                parentNode.data.expanded = true;
                This.dataTreePanel.getStore().sort('text', 'ASC');
            } else 
            	_console(response.responseText);
        },
        failure: function(response) {
        	Ext.get(This.tabPanel.getId()).unmask();
            _console(response.responseText);
        }
    });
};

DataViewer.prototype.setDataLocation = function(dataid, datalocation, viewbtn, store, win) {
	var This = this;
    var url = This.op_url + '/setDataLocation';
    Ext.get(This.tabPanel.getId()).mask("Saving Location..");
    Ext.Ajax.request({
        url: url,
        params: {
            location: datalocation,
            data_id: dataid
        },
        success: function(response) {
            Ext.get(This.tabPanel.getId()).unmask();
            if (response.responseText == "OK") {
                store.location = datalocation;
                viewbtn.setDisabled(false);
                win.close();
            } else {
                _console(response.responseText);
            }
        },
        failure: function(response) {
            Ext.get(This.tabPanel.getId()).unmask();
            _console(response.responseText);
        }
    });
};


DataViewer.prototype.addDatatype = function(parentNode) {
    var This = this;
    if (!parentNode) {
        parentNode = this.dataTreePanel.getRootNode();
    }
    if(!parentNode.data.isClass) {
    	return showError("Select a datatype to add the subtype for");
    }
    var parentType = parentNode.data.id;

    Ext.Msg.prompt("Add Datatype", "Enter name for the new Datatype:", function(btn, text) {
        if (btn == 'ok' && text) {
            var newid = This.ontns + getRDFID(text);
            var enode = This.dataTreePanel.getStore().getNodeById(newid);
            if (enode) {
                showError('Datatype ' + text + ' already exists');
                return;
            }
            var url = This.op_url + '/newDataType';
            Ext.get(This.dataTreePanel.getId()).mask('Adding..');
            Ext.Ajax.request({
                url: url,
                params: {
                    parent_type: parentType,
                    data_type: newid
                },
                success: function(response) {
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    if (response.responseText == "OK") {
                        var tmp = This.getTree({
                            item: {
                                id: newid,
                                type: 1
                            },
                            children: []
                            });
                        parentNode.data.leaf = false;
                        parentNode.data.expanded = true;
                        parentNode.appendChild(tmp);
                        This.dataTreePanel.getStore().sort('text', 'ASC');
                    } else {
                        _console(response.responseText);
                    }
                },
                failure: function(response) {
                    Ext.get(This.dataTreePanel.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    }, window, false);
};

DataViewer.prototype.saveDatatype = function(
		dtypeid, propmods, nameFormat,
		gridPanel, savebtn, tab) {
	var This = this;
    var url = This.op_url + '/saveDataTypeJSON';
    Ext.get(This.tabPanel.getId()).mask("Saving..");
    Ext.Ajax.request({
        url: url,
        params: {
            props_json: Ext.encode({
                format: nameFormat,
                add: propmods.addedProperties,
                del: propmods.deletedProperties,
                mod: propmods.modifiedProperties
            }),
            data_type: dtypeid
        },
        success: function(resp) {
            Ext.get(This.tabPanel.getId()).unmask();

            var data = Ext.decode(resp.responseText);
            if (data.errors.length) {
                Ext.MessageBox.show({
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.MessageBox.OK,
                    msg: "Could not save:<br/>" + data.errors.join('<br/>')
                    });
                _console(data.errors);
                return;
            } else if (data.warnings.length) {
                Ext.MessageBox.show({
                    icon: Ext.MessageBox.WARNING,
                    buttons: Ext.MessageBox.OK,
                    msg: data.warnings.join('<br/>')
                    });
            }
            propmods.deletedProperties = {};
            This.refreshInactiveTabs();
            
            if(gridPanel)
            	gridPanel.getStore().commitChanges();
            if(savebtn)
            	savebtn.setDisabled(true);
            if(tab)
            	tab.setTitle(tab.title.replace(/^\*/, ''));
            // _console(resp.responseText);
            },
        failure: function(response) {
            Ext.get(This.tabPanel.getId()).unmask();
            _console(response.responseText);
        }
    });
};

DataViewer.prototype.deleteDatatype = function(treeNode, delChildren) {
    var node = treeNode;
    var url = this.op_url + '/delDataTypes?del_children=' + delChildren;
    // Cannot delete root node
    if (!treeNode.parentNode)
        return;

    // Get All Child types to be removed as well
    var types = [node.data.id];
    var typesX = {};
    typesX[node.data.text] = true;
    var tmp = [node];
    while (tmp.length > 0) {
        var n = tmp.pop();
        n.eachChild(function(t) {
            typesX[t.data.id] = true;
            if (t.data.isClass) {
                types.push(t.data.id);
                tmp.push(t);
            }
        });
    }

    var This = this;
    Ext.get(this.dataTreePanel.getId()).mask('Deleting..');
    Ext.Ajax.request({
        url: url,
        params: {
            data_type: Ext.encode(types)
            },
        success: function(response) {
            Ext.get(This.dataTreePanel.getId()).unmask();
            if (response.responseText == "OK") {
                node.parentNode.removeChild(node);
                var tabitems = This.tabPanel.items.items;
                for (var i = 0; i < tabitems.length; i++) {
                    var tab = tabitems[i];
                    if (tab && typesX[tab.title])
                        This.tabPanel.remove(tab);
                }
            } else {
                _console(response.responseText);
            }
        },
        failure: function(response) {
            Ext.get(This.dataTreePanel.getId()).unmask();
            _console(response.responseText);
        }
    });
};

DataViewer.prototype.moveDataitemTo = function(ditem, isClass, fromtype, totype) {
    var This = this;
    var url = this.op_url + (isClass ? "/moveDatatypeTo" : "/moveDataTo");
    var params = { 
    		from_parent_type: fromtype,
    		to_parent_type: totype
    };
    if(isClass)
    	params.data_type = ditem;
    else
    	params.data_id = ditem;

    Ext.get(this.dataTreePanel.getId()).mask('Moving..');
    Ext.Ajax.request({
        url: url,
        params: params,
        success: function(response) {
            Ext.get(This.dataTreePanel.getId()).unmask();
        },
        failure: function(response) {
            Ext.get(This.dataTreePanel.getId()).unmask();
        }
    });
};

DataViewer.prototype.createTabPanel = function() {
    var This = this;
    this.tabPanel = new Ext.TabPanel({
        region: 'center',
        margins: '5 5 5 0',
        enableTabScroll: true,
        activeTab: 0,
        plain: true,
        resizeTabs: true,
        // minTabWidth: 135,
        // tabWidth: 135,
        items: [{
            layout: 'fit',
            title: 'Data Manager',
            autoLoad: {
                url: this.op_url + '/intro'
            }
        }]
        });
    this.tabPanel.on('tabchange', function(tp, tab) {
        if (tab.path)
            This.dataTreePanel.selectPath(tab.path, 'text');
        else
            This.dataTreePanel.getSelectionModel().deselectAll();
    });
};

DataViewer.prototype.createLeftPanel = function() {
    this.createDataTreePanel(this.store.tree);
    this.createMetricsTreePanel(this.store.metrics);
    this.createMetricsRangeStores(this.store.metrics);
    // Create an area on the left for the treepanel
    this.leftPanel = new Ext.TabPanel({
        region: 'west',
        // collapsible: true,
        // collapseMode: 'mini',
        width: '25%',
        plain: true,
        split: true,
        margins: '5 0 5 5',
        cmargins: '5 5 5 5',
        items: [this.dataTreePanel, this.metricsTreePanel],
        activeTab: 0
    });
};

DataViewer.prototype.createMainPanel = function() {
	if(!this.mainPanel) {
	    this.mainPanel = new Ext.Viewport({
	        layout: {
	            type: 'border'
	        }
	    });
	}
	this.mainPanel.add(this.leftPanel);
	this.mainPanel.add(this.tabPanel);
	
    if(!this.use_import_ui) {
    	this.mainPanel.add(getPortalHeader());
    }
    else {
    	this.mainPanel.add({
			region : 'north',
    		bodyStyle: 'background-color:#eee; color:#06f; padding-left:5px',
    		html: '<h2>External Data Catalog</h2>'
    	});
    }
};

DataViewer.prototype.initialize = function() {
    this.createStoreModels();
    this.createLeftPanel();
    this.createTabPanel();
    this.createMainPanel();
};
