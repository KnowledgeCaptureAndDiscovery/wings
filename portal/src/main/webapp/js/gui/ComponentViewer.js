function ComponentViewer(guid, store, op_url, res_url, 
		upload_url, pcdomns, dcdomns, liburl, 
		load_concrete, advanced_user) {
    this.guid = guid;
    this.store = store;
    this.op_url = op_url;
    this.res_url = res_url;
    this.upload_url = upload_url;
    this.liburl = liburl;
    this.libname = liburl.replace(/.+\//, '').replace(/\.owl$/, '');
    this.load_concrete = load_concrete;
    this.advanced_user = advanced_user;

    this.ns = {};
    this.ns[''] = pcdomns;
    this.ns['dcdom'] = dcdomns;
    this.ns['xsd'] = "http://www.w3.org/2001/XMLSchema#";

    this.treePanel = null;
    this.tabPanel = null;
    this.leftPanel = null;
    this.mainPanel = null;
};

ComponentViewer.prototype.getComponentTreePanel = function(root, title, iconCls, enableDrag) {
    var This = this;
    if (!Ext.ModelManager.isRegistered('compTreeRecord'))
        Ext.define('compTreeRecord', {
        extend: 'Ext.data.Model',
        fields: ['text', 'component']
        });

    var treeStore = Ext.create('Ext.data.TreeStore', {
        model: 'compTreeRecord',
        root: root,
        sorters: ['text']
    });
    var treePanel = new Ext.tree.TreePanel({
        width: '100%',
        border: false,
        autoScroll: true,
        hideHeaders: true,
        rootVisible: false,
        viewConfig: {
            plugins: {
                ptype: 'treeviewdragdrop',
                enableDrag: enableDrag,
                ddGroup: This.guid + '_ComponentTree',
                enableDrop: false,
                dragText: 'Drag the Component to the Canvas'
            }
        },
        iconCls: iconCls,
        bodyCls: 'x-docked-noborder-top',
        // bodyCls: 'nonBoldTree',
        title: title,
        store: treeStore,
        url: This.op_url
    });
    return treePanel;
};

ComponentViewer.prototype.getComponentTree = function(item) {
    var tmp = [];
    var cnode = this.getComponentTreeNode(item, true);
    if (item.children) {
        for (var i = 0; i < item.children.length; i++) {
            cnode.children.push(this.getComponentTree(item.children[i]));
        }
    }
    return cnode;
};

ComponentViewer.prototype.getComponentTreeByIO = function(item, isInput) {
    var tmp = item.children;
    var compsByIO = {};
    while(tmp.length > 0) {
    	var comp = tmp.pop();
        var cls = comp.cls;
        if (cls.component) {
        	var ios = isInput ? cls.component.inputs : cls.component.outputs; 
        	for(var i=0; i<ios.length; i++) {
        		var type = ios[i].type;
        		if(!compsByIO[type]) compsByIO[type] = [];
        		var isIncluded = false;
        		for(var j=0; j<compsByIO[type].length; j++) {
        			if(compsByIO[type][j] == type) {
        				isIncluded = true;
        				break;
        			}
        		}
        		if(!isIncluded)
        			compsByIO[type].push(comp);
        	}
        }
        tmp = tmp.concat(comp.children);
    }
    var root = this.getComponentTreeNode(item, false);
    for(var type in compsByIO) {
    	var comps = compsByIO[type];
    	var typenode = {
    			text: getLocalName(type),
    			id: type,
    			cls: type,
    			leaf: false,
    			iconCls: 'dtypeIcon',
    			expanded: true,
    			children: []
    	};
    	for(var i=0; i<comps.length; i++) {
    		typenode.children.push(this.getComponentTreeNode(comps[i], false));
    	}
    	root.children.push(typenode);
    }
    return root;
};

ComponentViewer.prototype.getComponentTreeNode = function(item, setid) {
    // Get the component holder class
    var cls = item.cls;

    if (!cls.component) {
        // A category folder (no components)
        return {
            text: getLocalName(cls.id),
            id: (setid ? cls.id: null),
            cls: cls.id,
            leaf: (item.children && item.children.length ? false: true),
            iconCls: 'ctypeIcon',
            expanded: true,
            children: []
            };
    }

    // Get the holder's component
    var c = cls.component;

    var tooltip = '<h4>Component: ' + getLocalName(c.id) + '</h4>';
    if (c.inputs && c.outputs) {
        tooltip += "<br/><b>Inputs</b><br/>";
        for (var i = 0; i < c.inputs.length; i++)
            tooltip += " - " + c.inputs[i].role + " (" + getPrefixedUrl(c.inputs[i].type, this.ns) + ')<br/>';
        tooltip += "<br/><b>Outputs</b><br/>";
        for (var i = 0; i < c.outputs.length; i++)
            tooltip += " - " + c.outputs[i].role + " (" + getPrefixedUrl(c.outputs[i].type, this.ns) + ')<br/>';
        tooltip += '<br/>Drag this to the template graph on the right to add to the template';
    }
    return {
        text: getLocalName(c.id),
        id: (setid ? c.id: null),
        cls: cls.id,
        leaf: (item.children && item.children.length ? false: true),
        iconCls: (c.type == 2 ? (c.location ? 'compIcon': 'noCompIcon') : 'absCompIcon'),
        component: {
            id: c.id,
            cls: cls.id,
            inputs: c.inputs,
            outputs: c.outputs,
            concrete: (c.type == 2),
            location: c.location
        },
        draggable: false,
        qtip: tooltip,
        expanded: true,
        children: []
        };
};

ComponentViewer.prototype.getComponentListTree = function(enableDrag) {
    var tmp = this.getComponentTree(this.store.tree);
    return this.getComponentTreePanel(tmp, 'Tree', (this.load_concrete ? 'compIcon': 'absCompIcon'), enableDrag);
};

ComponentViewer.prototype.getComponentInputsTree = function(enableDrag) {
    var tmp = this.getComponentTreeByIO(this.store.tree, true);
    return this.getComponentTreePanel(tmp, 'Inputs', 'inputIcon', enableDrag);
};

ComponentViewer.prototype.getComponentOutputsTree = function(enableDrag) {
    var tmp = this.getComponentTreeByIO(this.store.tree, false);
    return this.getComponentTreePanel(tmp, 'Outputs', 'outputIcon', enableDrag);
};

ComponentViewer.prototype.getIOListEditor = function(c, iostore, types, tab, savebtn, editable) {
    var This = this;

    var mainPanel = new Ext.Panel({
        region: 'center',
        title: 'I/O',
        iconCls: 'paramIcon',
        border: false,
        defaults: {
            border: false,
            padding: 0
        },
        autoScroll: true
    });

    var inputs = [];
    var params = [];
    for (var i = 0; i < iostore.inputs.length; i++) {
        var ip = iostore.inputs[i];
        if (ip.isParam) {
            // Convert xsd:integer to xsd:int (to be consistent)
            if (ip.type == this.ns['xsd'] + 'integer')
                ip.type = this.ns['xsd'] + 'int';
            params.push(ip);
        } else {
            inputs.push(ip);
        }
    }

    // Register store models
    if (!Ext.ModelManager.isRegistered('DataRole'))
        Ext.define('DataRole', {
        extend: 'Ext.data.Model',
        fields: ['role', 'type', 'prefix', 'dimensionality']
        });
    if (!Ext.ModelManager.isRegistered('ParamRole'))
        Ext.define('ParamRole', {
        extend: 'Ext.data.Model',
        fields: ['role', 'type', 'prefix', 'paramDefaultValue']
        });
    if (!Ext.ModelManager.isRegistered('dataPropRangeTypes'))
        Ext.define('dataPropRangeTypes', {
        extend: 'Ext.data.Model',
        fields: ['id', 'type']
        });

    // Create stores for Inputs, Params and Outputs
    var ipStore = new Ext.data.Store({
        model: 'DataRole',
        data: inputs
    });
    var paramStore = new Ext.data.Store({
        model: 'ParamRole',
        data: params
    });
    var opStore = new Ext.data.Store({
        model: 'DataRole',
        data: iostore.outputs
    });

    var iDataGrid,
    iParamGrid,
    oDataGrid;

    // Create editors
    for (var i = 0; i < 3; i++) {
        var typeEditor = new Ext.form.ComboBox({
            store: {
                model: 'dataPropRangeTypes',
                data: Ext.Array.map(types, function(typeid) {
                    return {
                        id: typeid,
                        type: getPrefixedUrl(typeid, This.ns)
                        };
                })
                },
            displayField: 'type',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            forceSelection: true,
            allowBlank: false
        });
        var pTypeEditor = new Ext.form.ComboBox({
            store: {
                model: 'dataPropRangeTypes',
                data: [{
                    id: this.ns['xsd'] + 'string',
                    type: 'xsd:string'
                }, {
                    id: this.ns['xsd'] + 'boolean',
                    type: 'xsd:boolean'
                }, {
                    id: this.ns['xsd'] + 'int',
                    type: 'xsd:int'
                }, {
                    id: this.ns['xsd'] + 'float',
                    type: 'xsd:float'
                }, {
					id : this.ns['xsd'] + 'date',
					type : 'xsd:date'
				}
                ]
                },
            displayField: 'type',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            forceSelection: true,
            allowBlank: false
        });
        var txtEditor = new Ext.form.field.Text({
            allowBlank: false
        });
        var numEditor = new Ext.form.field.Number({
            allowDecimals: false
        });
        var floatEditor = new Ext.form.field.Number({
            allowDecimals: true
        });
        var boolEditor = new Ext.form.field.Checkbox();

        var columns = [{
            dataIndex: 'role',
            header: 'Name',
            flex: 1,
            editor: txtEditor,
            menuDisabled: true
        }, {
            dataIndex: 'type',
            header: 'Type',
            flex: 1,
            renderer: function(url) {
                return getPrefixedUrl(url, This.ns);
            },
            editor: (i == 1 ? pTypeEditor: typeEditor),
            menuDisabled: true
        }, {
            dataIndex: 'prefix',
            header: 'Prefix',
            //width : 80,
            editor: txtEditor,
            menuDisabled: true
        }, ];
        if (i != 1)
            columns.push({
            dataIndex: 'dimensionality',
            header: 'Dimensionality',
            //width : 80,
            editor: numEditor,
            menuDisabled: true
        });
        else
            columns.push({
            dataIndex: 'paramDefaultValue',
            header: 'Default Value',
            //width : 80,
            getEditor: function(rec) {
                var type = rec.get('type');
                var xsd = This.ns['xsd'];
                if (type == xsd + 'boolean')
                    return boolEditor;
                else if (type == xsd + 'float')
                    return floatEditor;
                else if (type == xsd + 'int')
                    return numEditor;
                else
                    return txtEditor;
            },
            menuDisabled: true
        });

        var sm = editable ? Ext.create('Ext.selection.CheckboxModel', {
            checkOnly: true,
            }) : Ext.create('Ext.selection.RowModel');

        var editorPlugin = Ext.create('Ext.grid.plugin.FlexibleCellEditing', {
            clicksToEdit: 1
        });

        var plugins = editable ? [editorPlugin] : [];
        var bodycls = editable ? '': 'inactive-grid';

        var gridStore = (i == 0 ? ipStore: (i == 1 ? paramStore: opStore));
        var tbar = null;
        if (editable) {
            tbar = [{
                text: 'Add',
                iconCls: 'addIcon',
                roletype: i,
                handler: function() {
                    var role;
                    var i = this.roletype;
                    var panel = (i == 0 ? iDataGrid: (i == 1 ? iParamGrid: oDataGrid));
                    var gridStore = panel.getStore();
                    var pos = gridStore.getCount();
                    var sm = panel.getSelectionModel();
                    var pfx = (i == 0 ? '-i': (i == 1 ? '-p': '-o')) + (pos + 1);

                    if (i != 1)
                        role = new DataRole({
                        	prefix: pfx,
                        	dimensionality: 0
                        });
                    else
                        role = new ParamRole({
                        	prefix: pfx
                        });
                    panel.editorPlugin.cancelEdit();
                    gridStore.insert(pos, role);
                    panel.editorPlugin.startEditByPosition({
                    	row:pos,
                    	column:1
                    });
                }
            }, {
                iconCls: 'delIcon',
                text: 'Delete',
                roletype: i,
                handler: function() {
                    var i = this.roletype;
                    var panel = (i == 0 ? iDataGrid: (i == 1 ? iParamGrid: oDataGrid));
                    var gridStore = panel.getStore();
                    panel.editorPlugin.cancelEdit();
                    var s = panel.getSelectionModel().getSelection();
                    for (var i = 0, r; r = s[i]; i++) {
                        gridStore.remove(r);
                    }
                    // mainPanel.doLayout();
                }
            }];
        }

        var gridPanel = new Ext.grid.GridPanel({
            columnLines: true,
            autoHeight: true,
            border: false,
            // forceFit: true,
            title: (i == 0 ? 'Input Data': (i == 1 ? 'Input Parameters': 'Output Data')),
            iconCls: (i == 0 ? 'inputIcon': (i == 1 ? 'paramIcon': 'outputIcon')),
            columns: columns,
            selModel: sm,
            selType: 'cellmodel',
            plugins: plugins,
            bodyCls: bodycls,
            store: gridStore,
            tbar: tbar
        });
        gridPanel.editorPlugin = editorPlugin;

        if (i == 0)
            iDataGrid = gridPanel;
        if (i == 1)
            iParamGrid = gridPanel;
        if (i == 2)
            oDataGrid = gridPanel;
        gridStore.on('add', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('remove', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('update', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
    }
    /*var colPanel = new Ext.Panel({
		layout:'column',
		autoHeight: true,
		border: false,
		items: [iDataGrid, iParamGrid]
	});
	mainPanel.add(colPanel);*/

    mainPanel.add(iDataGrid);
    mainPanel.add(iParamGrid);
    mainPanel.add(oDataGrid);

    mainPanel.iDataGrid = iDataGrid;
    mainPanel.iParamGrid = iParamGrid;
    mainPanel.oDataGrid = oDataGrid;
    return mainPanel;
};

ComponentViewer.prototype.addComponent = function(parentNode) {
    var This = this;
    var cTree = this.treePanel;
    if (parentNode == null)
        parentNode = cTree.getRootNode();

    var pc = parentNode.data;
    var parentType = pc.cls;
    var parentId = pc.component ? pc.component.id: null;
    var parentIsConcrete = pc.component.concrete;

    // New: Can only add concrete components to existing component types
    if (this.load_concrete && (!parentId || parentIsConcrete)) {
        showError('Please select a Component Type first');
        return;
    }

    Ext.Msg.prompt("Add Component", "Enter name for the new Component:", function(btn, text) {
        if (btn == 'ok' && text) {
            text = getRDFID(text);
            var cid = This.ns[''] + text;
            var enode = cTree.getStore().getNodeById(cid);
            if (enode) {
                showError('Component ' + text + ' already exists');
                return;
            }
            var url = This.op_url + '/addComponent';
            Ext.get(cTree.getId()).mask("Creating..");
            Ext.Ajax.request({
                url: url,
                params: {
                    parent_cid: parentId,
                    parent_type: parentType,
                    cid: cid,
                    load_concrete: This.load_concrete
                },
                success: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    if (response.responseText == "OK") {
                        var clsid = cid + 'Class';
                        // FIXME: Should get the cls from server
                        var tmp = This.getComponentTree({
                            cls: {
                                id: clsid,
                                component: {
                                    id: cid,
                                    text: text,
                                    cls: clsid,
                                    type: (This.load_concrete ? 2: 1)
                                    }
                            }
                        });
                        if (tmp) {
                            parentNode.data.leaf = false;
                            parentNode.data.expanded = true;
                            parentNode.appendChild(tmp);
                            This.treePanel.getStore().sort('text', 'ASC');
                        }
                    } else {
                        if (window.console)
                            window.console.log(response.responseText);
                    }
                },
                failure: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    if (window.console)
                        window.console.log(response.responseText);
                }
            });
        }
    }, window, false);
};

ComponentViewer.prototype.addCategory = function(parentNode) {
    var This = this;
    var cTree = this.treePanel;
    if (parentNode == null)
        parentNode = cTree.getRootNode();
    var parentType = parentNode.data.cls;

    Ext.Msg.prompt("Add Category", "Enter name for the new Category:", function(btn, text) {
        if (btn == 'ok' && text) {
            text = getRDFID(text);
            var cid = This.ns[''] + text;
            var enode = cTree.getStore().getNodeById(cid);
            if (enode) {
                showError('Category ' + text + ' already exists');
                return;
            }
            var url = This.op_url + '/addCategory';
            Ext.get(cTree.getId()).mask("Creating..");
            Ext.Ajax.request({
                url: url,
                params: {
                    parent_type: parentType,
                    cid: cid,
                    load_concrete: This.load_concrete
                },
                success: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    if (response.responseText == "OK") {
                        var tmp = This.getComponentTree({
                            cls: {
                                id: cid
                            }
                        });
                        if (tmp) {
                            parentNode.data.leaf = false;
                            parentNode.data.expanded = true;
                            parentNode.appendChild(tmp);
                            This.treePanel.getStore().sort('text', 'ASC');
                        }
                    } else {
                        if (window.console)
                            window.console.log(response.responseText);
                    }
                },
                failure: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    if (window.console)
                        window.console.log(response.responseText);
                }
            });
        }
    }, window, false);
};

ComponentViewer.prototype.refreshInactiveTabs = function() {
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

ComponentViewer.prototype.prepareRoleRecord = function(arg, isParam) {
    var narg = {};
    for (var key in arg)
        narg[key] = arg[key];
    narg.isParam = isParam;
    return narg;
};

ComponentViewer.prototype.openComponentEditor = function(args) {
    var tab = args[0];
    var c = args[1];
    var compStore = args[2];
    var mainPanel;
    var This = this;

    var savebtn = new Ext.Button({
        text: 'Save',
        iconCls: 'saveIcon',
        disabled: true,
        handler: function() {
            var mainPanel = tab.ioEditor;
            var comp = {
                id: c.id,
                type: (c.concrete ? 2: 1),
                inputs: [],
                outputs: [],
                rulesText: "",
                documentation: "",
                location: c.location
            };
            var notok = false;
            var message = "";
            var names = {};
            comp.rulesText = tab.down("#rules").getValue();
            comp.documentation = tab.down("#doc").getValue();
            
        	var form = tab.down('form');
        	if(form) {
	        	var fields = form.getForm().getFields();
	        	comp.requirement = {};
	        	fields.each(function(field) {
	        		comp.requirement[field.getName()] = field.getValue();
	        	});
        	}
            
            mainPanel.iDataGrid.getStore().each(function(rec) {
                if (!rec.data.role || !rec.data.type || !rec.data.prefix) {
                	if(!rec.data.role) message += "Input Name not specified.. ";
                	if(!rec.data.type) message += "Input Type not specified.. ";
                	if(!rec.data.prefix) message += "Input Prefix not specified.. ";
                    notok = true;
                }
                else if(names[rec.data.role]) {
                	message += "Duplicate role name found: "+rec.data.role;
                	notok = true;
                }
                else {
                	names[rec.data.role] = 1;
                	comp.inputs.push(This.prepareRoleRecord(rec.data, false));
                }
            });
            mainPanel.iParamGrid.getStore().each(function(rec) {
                if (!rec.data.role || !rec.data.type || !rec.data.prefix) {
                	if(!rec.data.role) message += "Input Parameter Name not specified.. ";
                	if(!rec.data.type) message += "Input Parameter Type not specified.. ";
                	if(!rec.data.prefix) message += "Input Parameter Prefix not specified.. ";
                    notok = true;
                }
                else if(rec.data.type != This.ns['xsd'] + "string" && 
               		 (rec.data.paramDefaultValue+"") == "") {
                	message += "Input Parameter Default Value not specified.. ";
                	notok = true;
                }
                else if(names[rec.data.role]) {
                	message += "Duplicate role name found: "+rec.data.role;
                	notok = true;
                }
                else {
                	names[rec.data.role] = 1;
                	comp.inputs.push(This.prepareRoleRecord(rec.data, true));
                }
            });
            mainPanel.oDataGrid.getStore().each(function(rec) {
                if (!rec.data.role || !rec.data.type || !rec.data.prefix) {
                	if(!rec.data.role) message += "Output Name not specified.. ";
                	if(!rec.data.type) message += "Output Type not specified.. ";
                	if(!rec.data.prefix) message += "Output Prefix not specified.. ";
                    notok = true;
                }
                else if(names[rec.data.role]) {
                	message += "Duplicate role name found: "+rec.data.role;
                	notok = true;
                }
                else {
                	names[rec.data.role] = 1;
                	comp.outputs.push(This.prepareRoleRecord(rec.data, false));
                }
            });
            if (notok) {
                Ext.MessageBox.show({
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.MessageBox.OK,
                    msg: message
                });
                return;
            }
            var url = This.op_url + '/saveComponentJSON';
            Ext.get(This.treePanel.getId()).mask("Saving..");
            Ext.Ajax.request({
                url: url,
                params: {
                    component_json: Ext.encode(comp),
                    load_concrete: This.load_concrete
                },
                success: function(response) {
                    Ext.get(This.treePanel.getId()).unmask();
                    if (response.responseText == "OK") {
                        mainPanel.iDataGrid.getStore().commitChanges();
                        mainPanel.iParamGrid.getStore().commitChanges();
                        mainPanel.oDataGrid.getStore().commitChanges();
                        savebtn.setDisabled(true);
                        tab.setTitle(tab.title.replace(/^\*/, ''));
                        This.refreshInactiveTabs(This.tabPanel);
                    } else {
                        Ext.MessageBox.show({
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.MessageBox.OK,
                            msg: "Could not save:<br/>" + response.responseText.replace(/\n/, '<br/>')
                        });
                    }
                },
                failure: function(response) {
                    Ext.get(This.treePanel.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    });

    tab.ioEditor = This.getIOListEditor(c, compStore, This.store.types, tab, savebtn, (This.advanced_user && c.concrete == This.load_concrete));
    
    var addcompbtn = {
            xtype: 'button',
            text: 'Upload/Set Path',
            iconCls: 'addIcon',
            handler: function() {
                var win = new Ext.Window({
                    xtype: 'panel',
                    layout: {
                        type: 'accordion'
                    },
                    url: This.upload_url,
                    width: 500,
                    items: [{
                        xtype: 'panel',
                        title: 'Set Path to Component',
                        border: false,
                        layout: {
                            type: 'hbox',
                            defaultMargins: 10
                        },
                        items: [{
                            xtype: 'textfield',
                            flex: 1,
                            value: c.location,
                            emptyText: 'Enter path to component or Upload from below'
                        }, {
                            xtype: 'button',
                            text: 'Submit',
                            iconCls: 'addIcon',
                            handler: function() {
                            	var panel = this.up('panel');
                            	var loc = this.prev().value;
                            	if(!loc) {
                            		showError('Please enter the component location or Upload from below');
                            		return;
                            	}
                            	This.setComponentLocation(c.id, loc, tab, c, win);
                            }
                        }]
                    }, {
                        	xtype: 'xuploadpanel',
                        	collapsed: true,
                        	title: 'Upload Component',
                        	collapsible: true,
                        	border: false,
                        	multipart_params: {type: 'component', id: c.id},
                            url: This.upload_url,
                            addButtonCls: 'addIcon',
                            deleteButtonCls: 'delIcon',
                            uploadButtonCls: 'uploadIcon',
                            cancelButtonCls: 'delIcon',
                            listeners : {
                            	"uploadcomplete" : function(item, files) {
                            		// Just check the first file (only one file upload allowed here)
                            		var file = files[0];
                            		var loc = file.location;
                            		This.setComponentLocation(c.id, loc, tab, c, win);
                            	}
                            }
                    }]
                });
                win.show();
            }
        };

	        
	var editable = (This.advanced_user && This.load_concrete == c.concrete);
	
    var tbar = [];
    if(editable)
    	tbar.push(savebtn);
	tbar.push({
		iconCls : 'reloadIcon',
		text : 'Reload',
		handler : function() {
			tab.getLoader().load();
			savebtn.setDisabled(true);
			tab.setTitle(tab.title.replace(/^\*/, ''));
		}
	});
	
	if (This.advanced_user && c.concrete) {
		tbar.push('-');
		tbar.push({
			xtype : 'tbfill'
		});
		tbar.push(addcompbtn);
		tbar.push({
			iconCls : 'downloadIcon',
			itemId : 'downloadComponent',
			text : 'Download',
			disabled : !c.location,
			handler : function() {
				window.open(This.op_url+"/fetch?cid="+escape(c.id));
//				showWingsMessage('Location: ' + compStore.location,
//						'Location of ' + getLocalName(c.id), null, 400, 100);
			}
		});
	}


    var mainPanelItems = [ tab.ioEditor ];
    mainPanelItems.push(This.getRulesTab('Rules', 'rules', compStore.rules, 
    		editable, tab, savebtn));
    mainPanelItems.push(This.getRulesTab('Inherited Rules', 'inhrules', 
    		compStore.inheritedRules, false, tab, savebtn));
    mainPanelItems.push(This.getDocumentationTab('doc', compStore.documentation,
     		editable, tab, savebtn));
    if(c.concrete)
    	mainPanelItems.push(This.getDependenciesTab('Dependencies', 
    		compStore.requirement, editable, tab, savebtn));
    
    var mainPanel = new Ext.Panel({
        region: 'center',
        border: false,
        layout: 'fit',
        tbar: tbar,
        bodyStyle: editable ? '' : 'background-color:#ddd',
        items: {
        	xtype: 'tabpanel',
        	margin: 5,
        	plain: true,
        	//tbar: editable ? [ savebtn ] : null,
        	items: mainPanelItems
        }
    });
    tab.add(mainPanel);
};

ComponentViewer.prototype.setComponentLocation = function(cid, clocation, tab, store, win) {
	var This = this;
    var url = This.op_url + '/setComponentLocation';
    Ext.get(This.tabPanel.getId()).mask("Saving Location..");
    Ext.Ajax.request({
        url: url,
        params: {
            location: clocation,
            cid: cid
        },
        success: function(response) {
            Ext.get(This.tabPanel.getId()).unmask();
            if (response.responseText == "OK") {
                store.location = clocation;
                var node = This.treePanel.getStore().getNodeById(cid);
                tab.down('#downloadComponent').setDisabled(false);
                tab.setIconCls('compIcon');
                node.set('iconCls', 'compIcon');
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

ComponentViewer.prototype.openNewIconTab = function(tabname, iconCls) {
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

ComponentViewer.prototype.initComponentTreePanelEvents = function() {
    var This = this;
    This.treePanel.on("itemclick", function(view, rec, item, ind, event) {
        if (!rec.parentNode)
            return false;
        var id = rec.data.text;
        var path = getTreePath(rec, 'text');
        var tabName = id;
        var c = rec.data.component;
        if (!c.id)
            return;

        // Check if tab is already open
        var items = This.tabPanel.items.items;
        for (var i = 0; i < items.length; i++) {
            var tab = items[i];
            if (tab && tab.title.replace(/^\**/, '') == tabName) {
                This.tabPanel.setActiveTab(tab);
                return null;
            }
        }

        // Fetch Store via Ajax
        var url = This.op_url + '/getComponentJSON?cid=' + escape(c.id);

        var tab = This.openNewIconTab(tabName, (c.concrete ? (c.location ? 'compIcon': 'noCompIcon') : 'absCompIcon'));
        Ext.apply(tab, {
            path: path,
            guifn: This.openComponentEditor,
            args: [tab, c, {}]
            });
        This.tabPanel.setActiveTab(tab);
        
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
                        // tab.doLayout(false,true);
                    }
                }
            }
        });
        tab.getLoader().load();
    });

    /*treePanel.getStore().on('move', function(node, oldp, newp) {
		moveComponentTo(node.id, !oldp.parentNode?null:oldp.id, !newp.parentNode?null:newp.id, op_url);
		//window.console.log(node.id+" from "+oldp.id+" -> "+newp.id);
	});*/

    This.tabPanel.on('tabchange', function(tp, tab) {
        if (tab.path)
            This.treePanel.selectPath(tab.path, 'text');
        else
            This.treePanel.getSelectionModel().deselectAll();
    });

    return This.treePanel;
};

ComponentViewer.prototype.getRulesTab = function(title, textareaid, rules, editable, tab, savebtn) {
    var This = this;

    var rulestr = rules && rules.length ? rules.join("\n\n") : "";
    rulestr = rulestr.replace(/\)\s+/g,")\n");
    rulestr = rulestr.replace(/(\[.+?:)\s+/g,"$1\n");
    //rulestr = rulestr.replace(/\\s+\\]/g,"]");
    var rulesArea = new Ext.form.TextArea({
        itemId: textareaid,
        enableKeyEvents: editable,
        disabled: !editable,
        value: rulestr,
        border: false
    });
    
    var keyfn = function(obj, e) {
       var key = e.getKey();
       if (key >= 33 && key <= 40)
           return;
       if (key >= 16 && key <= 18)
           return;
       if (key >= 112 && key <= 123)
           return;
       if (key >= 90 && key <= 91)
           return;
       if (key == 27)
           return;
       tab.setTitle("*" + tab.title.replace(/^\*/, ''));
       savebtn.setDisabled(false);
       //item.un('keyup', keyfn);
   };
   if(editable)
   	rulesArea.on('keyup', keyfn);
    
   return new Ext.Panel({
        title: 'Rules',
        iconCls: 'inferIcon',
        layout: 'fit',
        border: false,
        items: rulesArea,
        title: title,
   });
};

ComponentViewer.prototype.getDocumentationTab = function(id, doc, editable, tab, savebtn) {
	var This = this;
	if(!editable) {
		return new Ext.Panel({
	        title: 'Documentation',
	        iconCls: 'docsIcon',
	        layout: 'fit',
	        border: false,
	        bodyPadding: 10,
	        autoScroll: true,
	        bodyCls: 'docsDiv',
	        html: doc
	    });
	}
	else {
		var docArea = new Ext.form.HtmlEditor({
			itemId: id,
			title: 'Documentation',
			iconCls: 'docsIcon',
			layout: 'fit',
			region : 'center',
			border : false,
			enableFont : false,
			bodyCls : 'docsDiv',
			value : doc
		});
		docArea.on('sync', function() {
			tab.setTitle("*" + tab.title.replace(/^\*/, ''));
	      savebtn.setDisabled(false);
		});
		return docArea;
	}
};

ComponentViewer.prototype.getDependenciesTab = 
		function(title, mstore, editable, tab, savebtn) {
	var This = this;
	
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
				url : This.res_url + '/getAllSoftwareVersions'
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
				url : This.res_url + '/getAllSoftwareEnvironment'
			},
			groupers : 'softwareGroupId',
			autoLoad : true
		});
	}
	this.environment_store.on('load', function() {
		This.showEnvironmentVariables(tab, mstore);
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

	var form = {
		xtype : 'form',
		title : title,
		iconCls : 'softwareIcon',
		bodyStyle : 'padding:5px',
		autoScroll : true,
		defaults : {
			xtype : 'textfield',
			flex : 1,
			anchor : '100%',
			disabled : !editable
		},
		items : [{
			xtype: 'combo',
			name : 'softwareIds',
			fieldLabel : 'Requires Software',
			labelWidth : 150,
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
					mstore.softwareIds = item.value;
					This.showEnvironmentVariables(tab, mstore);
				}
			}
		}, {
			xtype : 'panel',
			border: false,
			frame: true,
			type: 'evarpanel',
			margin: 5,
			html: '',
			listeners: {
				afterrender: function() {
					This.showEnvironmentVariables(tab, mstore);
				}
			}
		}, {
			name : 'memoryGB',
			fieldLabel : 'Requires Memory (GB)',
			labelWidth : 150,
			value : mstore.memoryGB
		}, {
			name : 'storageGB',
			fieldLabel : 'Requires Storage (GB)',
			labelWidth : 150,
			value : mstore.storageGB
		}, {
			name : 'needs64bit',
			xtype: 'checkbox',
			fieldLabel : 'Needs 64-bit machine',
			labelWidth : 150,
			checked : mstore.needs64bit
		} ],
		listeners : {
			dirtychange : function(item, dirty, opts) {
				if (dirty) {
					savebtn.setDisabled(false);
					tab.setTitle("*" + tab.title.replace(/^\*/, ''));
				}
			}
		}
	};
	return form;
};

ComponentViewer.prototype.showEnvironmentVariables = function(tab, mstore) {
	var text = 'Following Environment Variables are available for the component:<br/>';
	var swversions = {};
	this.version_store.each(function(field) {
		var sw = field.get('softwareGroupId');
		if(!swversions[sw]) 
			swversions[sw] = [];
		swversions[sw].push(field.get('id'));
	});
	var ptext = tab.down('panel[type="evarpanel"]');
	if(!ptext || !this.environment_store || !this.version_store)
		return;
	var evars = [];
	this.environment_store.each(function(field) {
		var sw = field.get('softwareGroupId');
		var vr = field.get('variable');
		if(!swversions[sw] || !mstore.softwareIds) return;
		var int = Ext.Array.intersect(mstore.softwareIds, swversions[sw]);
		if(int.length)
			evars.push("$"+vr);
	});
	if(evars.length)
		text += evars.join(", ");
	else
		text = "No Environment Variables are available for the component";
	Ext.getCmp(ptext.getId()).update(text);
};

ComponentViewer.prototype.initialize = function() {
    // Add the template tabPanel in the center
    var This = this;
    this.tabPanel = new Ext.TabPanel({
        region: 'center',
        margins: '5 5 5 0',
        enableTabScroll: true,
        activeTab: 0,
        resizeTabs: true,
        plain: true,
        // resizeTabs: true,
        // minTabWidth: 50,
        // tabWidth: 135,
        items: [{
            layout: 'fit',
            title: 'Component Manager',
            autoLoad: {
                url: this.op_url + '/intro'
            }
        }]
        });

    this.treePanel = this.getComponentListTree();
    
    var This = this;
    var delbtn = new Ext.Button({
        text: 'Delete',
        iconCls: 'delIcon',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length)
                return;
            var node = nodes[0];
            var c = node.data.component;
            var cls = node.data.cls;
            if (!c.concrete && This.load_concrete) {
                Ext.MessageBox.show({
                    title: 'Cannot delete',
                    msg: 'Cannot delete component types from this interface. Go to "Manage Component Types" instead',
                    buttons: Ext.Msg.OK
                });
                return;
            }
            Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + getLocalName(c.id ? c.id: cls), function(yesno) {
                if (yesno == "yes") {
                    var url = This.op_url + '/' + (c.id ? 'delComponent': 'delCategory');
                    Ext.get(This.treePanel.getId()).mask("Deleting..");
                    Ext.Ajax.request({
                        url: url,
                        params: {
                            cid: c.id ? c.id: cls
                        },
                        success: function(response) {
                            Ext.get(This.treePanel.getId()).unmask();
                            if (response.responseText == "OK") {
                                node.parentNode.removeChild(node);
                                if (c.id)
                                    This.tabPanel.remove(This.tabPanel.getActiveTab());
                            } else {
                                if (window.console)
                                    window.console.log(response.responseText);
                            }
                        },
                        failure: function(response) {
                            Ext.get(This.treePanel.getId()).unmask();
                            if (window.console)
                                window.console.log(response.responseText);
                        }
                    });
                }
            });
        }
    });

    var tbar = null;
    if (this.advanced_user) {
        tbar = [{
            text: 'Add ' + (this.load_concrete ? 'Component': 'Type'),
            iconCls: 'addIcon',
            handler: function() {
                var nodes = This.treePanel.getSelectionModel().getSelection();
                var parentNode = (nodes && nodes.length) ? nodes[0] : null;
                This.addComponent(parentNode);
            }
        }];
        if (!this.load_concrete) {
            tbar.push({
                text: 'Add Category',
                iconCls: 'dtypeIcon',
                handler: function() {
                    var nodes = This.treePanel.getSelectionModel().getSelection();
                    var parentNode = (nodes && nodes.length) ? nodes[0] : null;
                    This.addCategory(parentNode);
                }
            });
        }
        tbar.push(delbtn);
    }

    var leftPanel = new Ext.TabPanel({
        region: 'west',
        width: 250,
        split: true,
        plain: true,
        margins: '5 0 5 5',
        activeTab: 0,
        tbar: tbar
    });
    var libname = (this.libname == "library" ? "Default": this.libname);
    Ext.apply(this.treePanel, {
        title: 'Component' + (!This.load_concrete ? ' Types': 's: ' + libname)
        });

    this.store.types.sort();
    this.initComponentTreePanelEvents();

    leftPanel.add(this.treePanel);
    leftPanel.setActiveTab(0);

    this.mainPanel = new Ext.Viewport({
        layout: {
            type: 'border'
        },
        items: [leftPanel, this.tabPanel]
    });
    this.mainPanel.add(getPortalHeader(CONTEXT_ROOT));
    return this.mainPanel;
};
