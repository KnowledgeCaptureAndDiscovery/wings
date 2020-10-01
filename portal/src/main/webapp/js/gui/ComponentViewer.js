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

function ComponentViewer(guid, store, op_url, res_url, 
		upload_url, prov_url, pcdomns, dcdomns, liburl, 
		load_concrete, advanced_user) {
    this.guid = guid;
    this.store = store;
    this.op_url = op_url;
    this.res_url = res_url;
    this.upload_url = upload_url;
    this.prov_url = prov_url;
    this.liburl = liburl;
    this.libname = liburl.replace(/.+\//, '').replace(/\.owl$/, '');
    this.load_concrete = load_concrete;
    this.advanced_user = advanced_user;

    this.ns = {};
    this.ns[''] = pcdomns;
    this.ns['dcdom'] = dcdomns;
    this.ns['xsd'] = "http://www.w3.org/2001/XMLSchema#";

    this.provenanceViewer = new ProvenanceViewer(guid, prov_url);
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
	    useArrows: true,
        viewConfig: {
            plugins: {
                ptype: 'treeviewdragdrop',
                enableDrag: enableDrag,
                ddGroup: This.guid + '_ComponentTree',
                enableDrop: false,
                dragText: 'Drag the Component to the Canvas'
            },
            stripeRows: true
        },
        iconCls: iconCls,
        bodyCls: 'x-docked-noborder-top',
        // bodyCls: 'nonBoldTree',
        title: title,
        store: treeStore,
        url: This.op_url,
        listeners: {
            itemcontextmenu: {
                fn: This.onComponentItemContextMenu,
                scope: this
            }
        }
    });
    return treePanel;
};

ComponentViewer.prototype.getAddMenuItem = function() {
    var This = this;
	return {
        text: 'Add ' + (this.load_concrete ? 'Component': 'Type'),
        iconCls: 'icon-add fa fa-green',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            var parentNode = (nodes && nodes.length) ? nodes[0] : null;
            This.addComponent(parentNode);
        }
    };
};

ComponentViewer.prototype.getAddCategoryMenuItem = function() {
    var This = this;
	return {
        text: 'Add Category',
        iconCls: 'icon-folder-open fa fa-yellow',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            var parentNode = (nodes && nodes.length) ? nodes[0] : null;
            This.addCategory(parentNode);
        }
    };
};

ComponentViewer.prototype.getDuplicateMenuItem = function() {
    var This = this;
    return {
        text: 'Duplicate',
        iconCls: 'icon-docs fa fa-blue',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length)
                return;
            var node = nodes[0];
            This.duplicateComponent(node);
        }
    };
};

ComponentViewer.prototype.getDeleteMenuItem = function() {
    var This = this;
	return {
        text: 'Delete',
        iconCls: 'icon-del fa fa-red',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length)
                return;
            var node = nodes[0];
            This.confirmAndDelete(node);
        }
    };
};

ComponentViewer.prototype.createTreeToolbar = function() {
    var This = this;
    if (this.advanced_user) {
        var items = [];
    	var additem = This.getAddMenuItem();
    	var addcatitem = This.getAddCategoryMenuItem()
    	var delitem = This.getDeleteMenuItem();
    	var duplicate_item = This.getDuplicateMenuItem();
        items.push(additem);
        if (!this.load_concrete) {
            items.push(addcatitem);
        }
        else {
            items.push(duplicate_item);
        }
        items.push(delitem);
        var toolbar = Ext.create('Ext.toolbar.Toolbar', {
            dock: 'top',
            items: items
        });
        This.treePanel.addDocked(toolbar);
        This.treePanel.doComponentLayout();
    }
};

ComponentViewer.prototype.onComponentItemContextMenu = 
		function(view, node, item, index, e, eOpts) {
    var This = this;
    e.stopEvent();
    if (!this.menu && this.advanced_user) {
    	var additem = This.getAddMenuItem();
    	var addcatitem = This.getAddCategoryMenuItem()
    	var delitem = This.getDeleteMenuItem();
    	var duplicate_item = This.getDuplicateMenuItem()
    	additem.iconCls = 'icon-add fa-menu fa-green';
    	addcatitem.iconCls = 'icon-folder-open fa-menu fa-yellow';
    	delitem.iconCls = 'icon-del fa-menu fa-red';
        duplicate_item.iconCls = 'icon-docs fa-menu fa-blue';

        var items = [additem];
        if (!this.load_concrete) {
            items.push(addcatitem);
        } else {
            items.push(duplicate_item);
        }
        items.push(delitem);

        var roitems = [additem];
        this.menu = Ext.create('Ext.menu.Menu', {
            items: items });
        this.readmenu = Ext.create('Ext.menu.Menu', {
            items: roitems });
    }
    if(this.advanced_user) {
	    if (node.data.component.concrete || !this.load_concrete)
	        this.menu.showAt(e.getXY());
	    else
	        this.readmenu.showAt(e.getXY());
    }
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
    var tmp = item.children.slice(0);
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
        			if(compsByIO[type][j] == comp) {
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
    root.leaf = false;
    for(var type in compsByIO) {
    	var comps = compsByIO[type];
    	var typenode = {
    			text: getLocalName(type),
    			id: type,
    			cls: type,
    			leaf: false,
    			iconCls: 'icon-folder fa fa-yellow',
    			expIconCls: 'icon-folder-open fa fa-yellow',
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
            //iconCls: 'ctypeIcon',
            iconCls: 'icon-folder fa fa-yellow',
            expIconCls: 'icon-folder-open fa fa-yellow',
            expanded: true,
            children: []
            };
    }

    // Get the holder's component
    var c = cls.component;

    var tooltip = '<h4>Component: ' + getLocalName(c.id) + '</h4>';
    if (c.inputs && c.outputs) {
        tooltip += "<b>Inputs</b><br/>";
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
        iconCls: 'icon-component fa ' + 
        	(c.type == 2 ? (c.location ? 'fa-orange': 'fa-red') : 'fa-grey'),
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
    return this.getComponentTreePanel(tmp, 'Tree',
    		'icon-component fa-title ' + 
    		(this.load_concrete ? 'fa-orange': 'fa-grey'), enableDrag);
};

ComponentViewer.prototype.getComponentInputsTree = function(enableDrag) {
    var tmp = this.getComponentTreeByIO(this.store.tree, true);
    return this.getComponentTreePanel(tmp, 'Inputs', 'icon-input fa-title fa-blue', enableDrag);
};

ComponentViewer.prototype.getComponentOutputsTree = function(enableDrag) {
    var tmp = this.getComponentTreeByIO(this.store.tree, false);
    return this.getComponentTreePanel(tmp, 'Outputs', 'icon-output fa-title fa-brown', enableDrag);
};

ComponentViewer.prototype.getIOListEditor = function(c, iostore, types, tab, savebtn, editable) {
    var This = this;

    var mainPanel = new Ext.Panel({
        region: 'center',
        title: 'I/O',
        iconCls: 'icon-list fa-title fa-blue',
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
            if(typeof(ip.paramDefaultValue) == "object" && 
            		"lexicalValue" in ip.paramDefaultValue ) {
            	ip.paramDefaultValue = ip.paramDefaultValue["lexicalValue"];
            }
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
        var xsdtypes = [{
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
		}];
        
        if(METAWORKFLOWS) {
	        xsdtypes.push({
	        	id : this.ns['xsd'] + 'anyURI',
	        	type : 'xsd:runid'
	        });
        }
        
        var pTypeEditor = new Ext.form.ComboBox({
            store: {
                model: 'dataPropRangeTypes',
                data: xsdtypes
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
            decimalPrecision: 6
        });
        var boolEditor = new Ext.form.field.ComboBox({
        	editable: false,
        	store: [[true, 'true'], [false, 'false']]
        });
        var dateEditor = new Ext.form.DateField({
        	format: 'Y-m-d'
        });
        
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
                var typestr = getPrefixedUrl(url, This.ns);
                if(typestr == "xsd:anyURI")
                	typestr = "xsd:runid";
                return typestr;
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
                else if (type == xsd + 'date')
                    return dateEditor;                
                else if (type == xsd + 'int')
                    return numEditor;
                else
                    return txtEditor;
            },
            renderer: function(val) {
            	if (val instanceof Date) {
            		val.setHours(1, 0, 0, 0);
            		val = val.toJSON().replace(/T.*$/, '');
                }
        		return val;
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
                iconCls: 'icon-add fa fa-green',
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
                iconCls: 'icon-del fa fa-red',
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

        var titleicon =  "<i class='" + (i == 0 ? 'icon-input fa fa-blue': 
        	(i == 1 ? 'icon-param fa': 
    		'icon-output fa fa-brown')) + "'></i>";
        
        var title = titleicon + ' ' + (i == 0 ? 
        		'Input Data': (i == 1 
        				? 'Input Parameters': 'Output Data')); 
        var gridPanel = new Ext.grid.GridPanel({
            columnLines: true,
            autoHeight: true,
            border: false,
            // forceFit: true,
            title: title,
            columns: columns,
            margin: 5,
            frame: true,
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
        showError('Please select a Component Type');
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
            cTree.getEl().mask("Creating..");
            Ext.Ajax.request({
                url: url,
                params: {
                    parent_cid: parentId,
                    parent_type: parentType,
                    cid: cid,
                    load_concrete: This.load_concrete
                },
                success: function(response) {
                    cTree.getEl().unmask();
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
                    cTree.getEl().unmask();
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
            cTree.getEl().mask("Creating..");
            Ext.Ajax.request({
                url: url,
                params: {
                    parent_type: parentType,
                    cid: cid,
                    load_concrete: This.load_concrete
                },
                success: function(response) {
                    cTree.getEl().unmask();
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
                    cTree.getEl().unmask();
                    if (window.console)
                        window.console.log(response.responseText);
                }
            });
        }
    }, window, false);
};


ComponentViewer.prototype.confirmAndDelete = function(node) {
	var This = this;
    var c = node.data.component;
    var cls = node.data.cls;
    /*if (!c.concrete && This.load_concrete) {
        Ext.MessageBox.show({
            title: 'Cannot delete',
            msg: 'Cannot delete component types from this interface. Go to "Manage Component Types" instead',
            buttons: Ext.Msg.OK
        });
        return;
    }*/
    Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + getLocalName(c.id ? c.id: cls), function(yesno) {
        if (yesno == "yes") {
            var url = This.op_url + '/' + (c.id ? 'delComponent': 'delCategory');
            This.treePanel.getEl().mask("Deleting..");
            Ext.Ajax.request({
                url: url,
                params: {
                    cid: c.id ? c.id: cls
                },
                success: function(response) {
                    This.treePanel.getEl().unmask();
                    if (response.responseText == "OK") {
                        node.parentNode.removeChild(node);
                        if (c.id)
                            This.tabPanel.remove(This.tabPanel.getActiveTab());
                    } else {
                        _console(response.responseText);
                    }
                },
                failure: function(response) {
                    This.treePanel.getEl().unmask();
                    _console(response.responseText);
                }
            });
        }
    });
};


ComponentViewer.prototype.duplicateComponent = function(node) {
    var This = this;
    var cTree = this.treePanel;
    var c = node.data.component;
    var cls = node.data.cls;
    var parentNode = node.parentNode
    var pc = parentNode.data;
    var parentType = pc.cls;
    var parentId = pc.component ? pc.component.id: null;
    var parentIsConcrete = pc.component.concrete;
    // New: Can only add concrete components to existing component types
    if (this.load_concrete && (!parentId || parentIsConcrete)) {
        showError('Please select a Component');
        return;
    }


    Ext.MessageBox.prompt("Duplicate component", "Enter the new name", function(btn, txt) {
        if (btn == "ok" && txt) {
            var new_cid = This.ns[''] + txt;
            var enode = cTree.getStore().getNodeById(new_cid);
            if (enode) {
                showError('Component ' + txt + ' already exists');
                return;
            }
            var url = This.op_url + '/duplicateComponent';
            This.treePanel.getEl().mask("Duplicate..");
            Ext.Ajax.request({
                url: url,
                params: {
                    new_cid: new_cid,
                    parent_cid: parentId,
                    parent_type: parentType,
                    cid: c.id,
                    load_concrete: This.load_concrete
                },
                //TODO: Fix me
                success: function(response) {
                    cTree.getEl().unmask();
                    if (response.responseText == "OK") {
                        var clsid = new_cid + 'Class';
                        // FIXME: Should get the cls from server
                        var tmp = This.getComponentTree({
                            cls: {
                                id: clsid,
                                component: {
                                    id: new_cid,
                                    text: txt,
                                    cls: clsid,
                                    type: (This.load_concrete ? 2: 1),
                                    location: "blank"
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
                    cTree.getEl().unmask();
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
        iconCls: 'icon-save fa fa-blue',
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
                		rec.data.type != This.ns['xsd'] + "anyURI" &&
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
                	
                    if (rec.data.paramDefaultValue instanceof Date) {
                    	rec.data.paramDefaultValue.setHours(1, 0, 0, 0);
                    	rec.data.paramDefaultValue = 
                    		rec.data.paramDefaultValue.toJSON().replace(/T.*$/, '');
                    }                	
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
            This.treePanel.getEl().mask("Saving..");
            Ext.Ajax.request({
                url: url,
                params: {
                	cid: c.id,
                    component_json: Ext.encode(comp),
                    load_concrete: This.load_concrete
                },
                success: function(response) {
                    This.treePanel.getEl().unmask();
                    if (response.responseText == "OK") {
                        mainPanel.iDataGrid.getStore().commitChanges();
                        mainPanel.iParamGrid.getStore().commitChanges();
                        mainPanel.oDataGrid.getStore().commitChanges();
                        savebtn.setDisabled(true);
                        tab.setTitle(tab.title.replace(/^\*/, ''));
                        tab.rulestr = comp.rulesText;
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
                    This.treePanel.getEl().unmask();
                    _console(response.responseText);
                }
            });
        }
    });

    tab.ioEditor = This.getIOListEditor(c, compStore, This.store.types, tab, savebtn, (This.advanced_user && c.concrete == This.load_concrete));
    
    var addcompbtn = {
            xtype: 'button',
            text: 'Upload/Set Path',
            iconCls: 'icon-add fa fa-green',
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
                            iconCls: 'icon-add fa fa-green',
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
                            addButtonCls: 'icon-add fa fa-green',
                            deleteButtonCls: 'icon-del fa fa-red',
                            uploadButtonCls: 'icon-upload fa fa-blue',
                            cancelButtonCls: 'icon-del fa fa-red',
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
		iconCls : 'icon-reload fa fa-green',
		text : 'Reload',
		handler : function() {
			tab.getLoader().load();
			savebtn.setDisabled(true);
			tab.setTitle(tab.title.replace(/^\*/, ''));
		}
	});
	tbar.push('-');
	tbar.push({
		xtype : 'tbfill'
	});
	tbar.push('-');
	if(compStore.version != null)
		tbar.push("v" + compStore.version);
	
	if (This.advanced_user && c.concrete) {
		tbar.push('-');
		tbar.push(addcompbtn);
		tbar.push({
			iconCls : 'icon-download fa fa-blue',
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
    //console.log(compStore);

    var rulesPanel = This.getRulesTab('Component Rules', 'rules', 
    		compStore.rules, compStore, editable, tab, savebtn);
    var inhRulesPanel = This.getRulesTab('Inherited Rules', 'inhrules', 
    		compStore.inheritedRules, compStore, false, tab, savebtn);
    
    mainPanelItems.push({
    	xtype: 'tabpanel',
    	title: 'Rules',
    	border: true,
    	plain: true,
    	padding: 5,
    	//padding: "5 0 0 0",
        iconCls: 'icon-runAlt fa-title fa-blue',
    	items: [rulesPanel, inhRulesPanel]
    });
    mainPanelItems.push(This.getDocumentationTab('doc', compStore.documentation,
     		editable, tab, savebtn));
    if(c.concrete) {
    	mainPanelItems.push(This.getDependenciesTab('Dependencies', 
    		compStore.requirement, editable, tab, savebtn));
        
        mainPanelItems.push(This.getCodeTab(c.id, 'code', editable, tab, savebtn));
    }    	
    
    mainPanelItems.push(This.provenanceViewer.createItemProvenanceGrid(c.id));
    
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
            autoScroll: true,
        	//tbar: editable ? [ savebtn ] : null,
        	items: mainPanelItems
        }
    });
    tab.add(mainPanel);
};

ComponentViewer.prototype.markComponentInitialized = function(cid, tab) {
	var node = this.treePanel.getStore().getNodeById(cid);
    tab.down('#downloadComponent').setDisabled(false);
    tab.setIconCls('icon-component fa-title fa-orange');
    node.set('iconCls', 'icon-component fa fa-orange');
};

ComponentViewer.prototype.setComponentLocation = function(cid, clocation, tab, store, win) {
	var This = this;
    var url = This.op_url + '/setComponentLocation';
    This.tabPanel.getEl().mask("Saving Location..");
    Ext.Ajax.request({
        url: url,
        params: {
            location: clocation,
            cid: cid
        },
        success: function(response) {
            This.tabPanel.getEl().unmask();
            if (response.responseText == "OK") {
                store.location = clocation;
                This.markComponentInitialized(cid, tab);
                win.close();
            } else {
                _console(response.responseText);
            }
        },
        failure: function(response) {
            This.tabPanel.getEl().unmask();
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

        var tab = This.openNewIconTab(tabName, 'icon-component fa-title ' + 
        		(c.concrete ? (c.location ? 'fa-orange': 
        			'fa-red') : 'fa-grey'));
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

ComponentViewer.prototype.getCodeTab = function(cid, textareaid, editable, tab, savebtn) {
    var This = this;
    var serverAddress = this.op_url;
    var fb = new Wings.fb.FileBrowser({    	
    	iconCls: 'icon-runAlt fa-title fa-blue',
    	cid: cid,
    	title: 'Code',
        address: {
            urlGet: serverAddress+'/fb/list',
            urlInitialize: serverAddress+'/fb/initialize',
            urlNewFile: serverAddress+'/fb/addFile',
            urlNewDirectory: serverAddress+'/fb/addDirectory',
            urlRename: serverAddress+'/fb/rename',
            urlDelete: serverAddress+'/fb/delete',
            urlViewFile: serverAddress+'/fb/get',
            urlSave: serverAddress+'/fb/save'
        }    	
    });
    
    fb.on('initialized', function() {
    	This.markComponentInitialized(cid, tab);
    });
    
    return fb;
};


ComponentViewer.prototype.getRulesTab = function(title, textareaid, rules, comp, editable, tab, savebtn) {
    var This = this;

    var rulestr = rules && rules.length ? rules.join("\n\n") : "";
    rulestr = rulestr.replace(/\)\s+/g,")\n");
    rulestr = rulestr.replace(/(\[.+?:)\s+/g,"$1\n");
    rulestr = rulestr.replace(/^([^\[\]])/mg,"  $1");    		
    var rulesArea = new Ext.ux.form.field.CodeMirror({
        disabled: !editable,
        itemId: textareaid,
        value: rulestr,
        showToolbar: false,
        mode: 'application/x-jena',
        pathModes: CONTEXT_ROOT + "/lib/codemirror/mode",
        border: false
    });
    //console.log(new RulesParser(rulestr));
    
    tab.rulestr = rulestr;
    rulesArea.on("change", function(item, val) {
    	if(val != tab.rulestr) {
    		tab.setTitle("*" + tab.title.replace(/^\*/, ''));
    		savebtn.setDisabled(false);
    	}
    	else {
    		tab.setTitle(tab.title.replace(/^\*/, ''));
    		savebtn.setDisabled(true);
    	}
    });
    
    var tbar = [
    	{
	    	text: 'Add Rule',
	    	menu: {
	    		xtype: 'menu',
	    		items: This.initializeMenuItems(
	    			comp, rulesArea,
	    			[
		    			{ name: "Parameter Setting Rule", id: "parameter"},
		    			{ name: "Invalidation Rule", id: "invalidation"},
		    			{ name: "No-Operation Rule", id: "no_op"},
		    			{ name: "Forward Metadata Propagation Rule", id: "forward_meta"},
		    			{ name: "Backward Metadata Propagation Rule", id: "backward_meta"},
		    			{ name: "Collection Size Rule", id: "collection_size"},
	    			]
	    		)
	    	}
    	}
    ];

    return new Ext.Panel({
    	title: 'Rules',
    	layout: 'fit',
    	//border: false,
    	tbar: editable ? tbar : null,
    	items: rulesArea,
    	title: title,
    });
};

ComponentViewer.prototype.initializeMenuItems = function(comp, rulesArea, ruleTypes) {
	var menu = [];
	var me = this;
	for(var i=0; i<ruleTypes.length; i++) {
		var ruleType = ruleTypes[i];
		menu.push({
			id: ruleType.id,
	        text: ruleType.name,
	        handler: function(item) {
	        	var newRule = me.onAddRule(item, comp);
	        	rulesArea.setValue(rulesArea.getValue() + "\n" + newRule);
	        },
	        scope: me
		})
	}
	return menu;
};

ComponentViewer.prototype.getGenericRuleTriples = function(comp) {
	var compname = getLocalName(comp.id);
	var vars = {input: [], output: [], param: []};
	
	var triples = `  (?c rdf:type pcdom:${compname}Class)\n`;
	for(var i=0; i<comp.inputs.length; i++) {
		var input = comp.inputs[i];
		var invar = "?"+input.role;
		triples += `  (?c pc:hasInput ${invar})\n  (${invar} pc:hasArgumentID "${input.role}")\n`;
		if(input.isParam)
			vars.param.push(invar);
		else
			vars.input.push(invar);		
	}
	for(var i=0; i<comp.outputs.length; i++) {
		var output = comp.outputs[i];
		var outvar = "?"+output.role;		
		triples += `  (?c pc:hasOutput ${outvar})\n  (${outvar} pc:hasArgumentID "${output.role}")\n`;
		vars.output.push(outvar);
	}
	return [triples, vars];
};

ComponentViewer.prototype.onAddRule = function(item, comp) {
	var me = this;
	var ruledata = this.getGenericRuleTriples(comp);
	var triples = ruledata[0];
	var vars = ruledata[1];
	var effect = "";
	if(item.id != "parameter") {
		for(var i=0; i<vars.param.length; i++) {
			var v = vars.param[i];
			triples += `  (${v} pc:hasValue ${v}Value)\n`;
		}
	}
	switch(item.id) {
	case "parameter":
		for(var i=0; i<vars.param.length; i++) {
			effect += `  (${vars.param[i]} pc:hasValue "exampleValue"^^xsd:string)\n`;
		}
		break;
	case "invalidation":
		effect += `  (?c pc:isInvalid "true"^^xsd:boolean)\n`;
		break;
	case "no_op":
		effect += `  (?c pc:isNoOperation "true"^^xsd:boolean)\n`;
		break;
	case "forward_meta":
		for(var i=0; i<vars.output.length; i++) {
			var v = vars.output[i];
			effect += `  (${v} dcdom:changeToRealDataProperty ?exampleVariable)\n`;
		}
		break;
	case "backward_meta":
		for(var i=0; i<vars.input.length; i++) {
			var v = vars.input[i];
			effect += `  (${v} dcdom:changeToRealDataProperty ?exampleVariable)\n`;
		}
		break;
	case "collection_size":
		for(var i=0; i<vars.output.length; i++) {
			var v = vars.output[i];
			effect += `  (${v} pc:hasDimensionSizes ?exampleVariable)\n`;
		}
		break;		
	}
	effect += `  print(?c "${item.id} rule fired")`;
	return `[ ${item.id}Rule:
${triples}   ->
${effect}
]`
},

ComponentViewer.prototype.getDocumentationTab = function(id, doc, editable, tab, savebtn) {
	var This = this;
	if(!editable) {
		return new Ext.Panel({
	        title: 'Documentation',
	        iconCls: 'icon-docs fa-title fa-blue',
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
			iconCls: 'icon-docs fa-title fa-blue',
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
		iconCls : 'icon-dropbox fa-title fa-blue',
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
			fieldLabel : 'Requires Software (Minimum Version)',
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
		var num = Ext.Array.intersect(mstore.softwareIds, swversions[sw]);
		if(num.length)
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
    this.createTreeToolbar();

    var leftPanel = new Ext.TabPanel({
        region: 'west',
        width: 250,
        split: true,
		animCollapse : false,
		preventHeader : true,
		collapsible : true,
		collapseMode : 'mini',
        plain: true,
        margins: '5 0 5 5',
		cmargins : '5 0 5 0',        
        activeTab: 0
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
    this.mainPanel.add(getPortalHeader());
    return this.mainPanel;
};
