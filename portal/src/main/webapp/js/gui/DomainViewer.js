function DomainViewer(guid, store, op_url, upload_url) {
	this.guid = guid;
	this.store = store;
	this.op_url = op_url;
	this.upload_url = upload_url;
	
	this.domainsGrid;
	this.tabPanel;
}


DomainViewer.prototype.createTabPanel = function() {
    var This = this;
    this.tabPanel = new Ext.TabPanel({
        region: 'center',
        margins: '5 5 5 0',
        enableTabScroll: true,
        activeTab: 0,
        plain: true,
        resizeTabs: true,
        items: [{
            layout: 'fit',
            title: 'Domain Manager',
            autoLoad: {
                url: this.op_url + '/intro'
            }
        }]
        });
    this.tabPanel.on('tabchange', function(tp, tab) {
    	var rec = This.domainsGrid.getStore().findExact('name', tab.title);
    	if(rec < 0)
            This.domainsGrid.getSelectionModel().deselectAll(true);
    	else
    		This.domainsGrid.getSelectionModel().select(rec, true);
    });
};

DomainViewer.prototype.createLeftPanel = function() {
	var This = this;
	var domainStore = Ext.create('Ext.data.Store', {
        proxy: {
            type: 'memory',
            reader: {
                type: 'json'
            },
            sorters: ['name']
        },
        fields: ['name', 'engine', 'permissions', 'directory', 'url', 'isLegacy'],
        autoLoad: true,
        data: this.store.list,
        sorters: ['name']
	});
	
	var execMenu = [];
	for(var i=0; i<this.store.engines.length; i++) {
		var engine = this.store.engines[i];
		execMenu.push({
	    	text: engine,
	    	iconCls: 'icon-run fa-menu fa-brown',
	    	handler: function() {
	    		var sels = This.domainsGrid.getSelectionModel().getSelection();
	    		if(!sels.length) {
	    			showError("Select a domain first");
	    			return;
	    		}
	    		var domain = sels[0];
	    		This.setDomainExecutionEngine(domain, this.text);
	    	}
		})
	}

    this.domainsGrid = new Ext.grid.Panel({
        region: 'west',
        width: '25%',
        split: true,
        /*margins: '5 0 5 5',
        cmargins: '5 5 5 5',*/
        margins: 5,
        store: domainStore,
        bodyCls:'multi-line-grid',
        dockedItems: [{
        	xtype: 'toolbar',
        	dock: 'top',
        	items: [ 
        	{
        	    text: 'Add',
        	    iconCls: 'icon-add fa fa-green',
        	    menu:[
        	    {
        	    	itemId: 'createbutton',
        	    	text: 'Add Domain',
        	    	iconCls: 'icon-add fa-menu fa-green'
        	    },
        	    {
        	    	itemId: 'importbutton',
        	    	text: 'Import Domain',
        	    	iconCls: 'icon-download-cloud fa-menu fa-blue'
        	    }]
        	}, {
        		itemId: 'selectbutton',
        		text: 'Set Default',
        		iconCls: 'icon-select fa fa-green'
        	}, {
        	    text: 'Use Execution Engine',
        	    iconCls: 'icon-run fa fa-brown',
        	    menu: execMenu
        	}, {
        		itemId: 'delbutton',
        		text: 'Delete',
        		iconCls: 'icon-del fa fa-red'
        	}, {
        		itemId: 'renamebutton',
        		text: 'Rename',
        		iconCls: 'icon-edit fa fa-blue'
        	}, {
        		itemId: 'downloadbutton',
        		text: 'Download',
        		iconCls: 'icon-download fa fa-blue'
        	}]
        }],
        columns: [{
        	dataIndex: 'name',
        	header: 'Domain',
        	flex: 1,
        	renderer: Ext.bind(This.formatDomainName, This)
        }, {
        	dataIndex: 'engine',
        	header: 'Execution Engine',
        	flex: 1
        }, {
        	dataIndex: 'permissions',
        	header: 'Permissions',
        	flex: 1,
        	renderer: Ext.bind(This.formatPermissions, This)
        }]
    });
};

DomainViewer.prototype.formatDomainName = function(name, item, rec) {
	var extra = '';
	var color = 'fa-grey';
	if(this.store.selected == name) { 
		extra = '<i class="icon-select-alt fa-green"></i>';
		color = 'fa-green';
		name = '<b>' + name + '</b>';
	}
	return "<i class='icon-box fa "+color+"'></i> " 
		+ name + " " + extra;
};

DomainViewer.prototype.formatPermissions = function(name, item, rec) {
	var perms = rec.get('permissions');
	var users = [];
	for(var i=0; i<perms.length; i++) {
		var perm = perms[i];
		var userid = perm.userid;
		if(userid=="*") userid = "All";
		users.push(userid);
	}
	if(users.length) {
		var userstr = users.join(", ");
		return "<i class='icon-unlockAlt fa-green'></i>"
			+ " Unlocked for "+userstr;
	}
	else
		return "<i class='icon-lock fa-maroon'></i>"
			+ " Locked";
};

DomainViewer.prototype.getDomainDetails = function(store) {
	var detailpanel = {
		xtype: 'propertygrid',
		border: false,
		source: store
	};
	return detailpanel;
};

DomainViewer.prototype.openDomain = function(name) {
	var This = this;
	var cls = this.store.selected == name ? 'icon-box fa fa-green' : 
		'icon-box fa fa-grey';
	var items = this.tabPanel.items.items;
	for(var i=0; i<items.length; i++) {
		if(items[i].title == name) {
			this.tabPanel.setActiveTab(items[i]);
			return items[i];
		}
	}
	
	var tab = new Ext.Panel({
		layout : 'fit',
		closable : true,
		iconCls : cls,
		border: false,
		title : name,
		items : []
	});
    this.tabPanel.add(tab);
    Ext.apply(tab, {
        loader: {
            loadMask: true,
            url: This.op_url+"/getDomainDetails?domain="+name,
            renderer: function(loader, response, req) {
                var store = Ext.decode(response.responseText);
                if (store) {
                    tab.removeAll();
                    tab.add(This.getDomainDetails(store));
                }
            }
        }
    });
    this.tabPanel.setActiveTab(tab);
    tab.getLoader().load();
    
    return tab;
};

DomainViewer.prototype.createDomain = function(domname) {
	var This = this;
    var domname = getRDFID(domname);
    var rec = This.domainsGrid.getStore().findExact('name', domname);
    if (rec != -1) {
        showError(domname + ' already exists ! Choose a different name.');
        return;
    }
    var url = This.op_url + '/createDomain';
    Ext.get(This.domainsGrid.getId()).mask("Creating Domain..");
    Ext.Ajax.request({
        url: url,
        params: {
        	domain: domname
        },
        success: function(response) {
        	 Ext.get(This.domainsGrid.getId()).unmask();
        	 try {
        		 var ret = Ext.decode(response.responseText);
        		 if(!ret.error && ret.name) {
        			 This.domainsGrid.getStore().add(ret);
        		 }
        		 else {
        			 _console(ret);
        			 showError(ret.error);
        		 }
        	 }
        	 catch (e) {
        		 _console(e.message);
        	 }
        },
        failure: function(response) {
        	Ext.get(This.domainsGrid.getId()).unmask();
        	_console(response.responseText);
        }
        
    });
};

DomainViewer.prototype.importDomain = function(domname, loc) {
	var This = this;
    var domname = getRDFID(domname);
    var rec = This.domainsGrid.getStore().findExact('name', domname);
    if (rec != -1) {
        showError(domname + ' already exists ! Choose a different name.');
        return;
    }
    var url = This.op_url + '/importDomain';
    Ext.get(This.domainsGrid.getId()).mask("Importing Domain..");
    Ext.Ajax.request({
        url: url,
        params: {
        	domain: domname,
            location: loc,
        },
        success: function(response) {
        	 Ext.get(This.domainsGrid.getId()).unmask();
        	 try {
        		 var ret = Ext.decode(response.responseText);
        		 if(!ret.error && ret.name) {
        			 This.domainsGrid.getStore().add(ret);
        		 }
        		 else {
        			 _console(ret);
        			 showError(ret.error);
        		 }
        	 }
        	 catch (e) {
        		 _console(e.message);
        	 }
        },
        failure: function(response) {
        	Ext.get(This.domainsGrid.getId()).unmask();
        	_console(response.responseText);
        }
        
    });
};

DomainViewer.prototype.selectDomain = function(domname) {
	var This = this;
    var url = This.op_url + '/selectDomain';
    Ext.get(This.domainsGrid.getId()).mask("Selecting Domain..");
    Ext.Ajax.request({
        url: url,
        params: {
        	domain: domname
        },
        success: function(response) {
        	 //Ext.get(This.domainsGrid.getId()).unmask();
        	 try {
        		 if(response.responseText == "OK") {
        			 window.location.reload();
        			 //This.store.selected = domname; 
            		 //This.domainsGrid.reconfigure();
        		 }
        		 else {
        			 _console(response.responseText);
        		 }
        	 }
        	 catch (e) {
        		 _console(e.message);
        	 }
        },
        failure: function(response) {
        	Ext.get(This.domainsGrid.getId()).unmask();
        	_console(response.responseText);
        }
        
    });
};

DomainViewer.prototype.deleteDomain = function(domname) {
	var This = this;
    var url = This.op_url + '/deleteDomain';
    Ext.get(This.domainsGrid.getId()).mask("Deleting Domain..");
    Ext.Ajax.request({
        url: url,
        params: {
        	domain: domname
        },
        success: function(response) {
        	 Ext.get(This.domainsGrid.getId()).unmask();
			 if (response.responseText == "OK") {
				var rec = This.domainsGrid.getStore().findExact('name', domname);
				This.domainsGrid.getStore().removeAt(rec);
			 } else {
				_console(response.responseText);
			 }
        },
        failure: function(response) {
        	Ext.get(This.domainsGrid.getId()).unmask();
        	_console(response.responseText);
        }
        
    });
};

DomainViewer.prototype.renameDomain = function(domname, newname) {
	var This = this;
    var newname = getRDFID(newname);
    var rec = This.domainsGrid.getStore().findExact('name', newname);
    if (rec != -1) {
        showError(newname + ' already exists ! Choose a different name.');
        return;
    }
    var url = This.op_url + '/renameDomain';
    Ext.get(This.domainsGrid.getId()).mask("Renaming Domain..");
    Ext.Ajax.request({
        url: url,
        params: {
        	domain: domname,
        	newname: newname
        },
        success: function(response) {
        	 Ext.get(This.domainsGrid.getId()).unmask();
			 if (response.responseText == "OK") {
				var rec = This.domainsGrid.getStore().findRecord('name', domname);
				rec.set("name", newname);
				if(This.store.selected == domname) {
					This.store.selected = newname;
					window.location.reload();
				}
				This.domainsGrid.reconfigure();
			} else {
				_console(response.responseText);
			}
        },
        failure: function(response) {
        	Ext.get(This.domainsGrid.getId()).unmask();
        	_console(response.responseText);
        }
        
    });
};

DomainViewer.prototype.setDomainExecutionEngine = function(domrec, engine) {
	var This = this;
	var curengine = domrec.get('engine');
	if(curengine == engine) {
		showError('The domain engine is already set to '+engine);
		return;
	}
	var domname = domrec.get('name');
	
    var url = This.op_url + '/setDomainExecutionEngine';
    Ext.get(This.domainsGrid.getId()).mask("Setting Execution Engine..");
    Ext.Ajax.request({
        url: url,
        params: {
        	domain: domname,
        	engine: engine
        },
        success: function(response) {
        	 Ext.get(This.domainsGrid.getId()).unmask();
			 if (response.responseText == "OK") {
				domrec.set("engine", engine);
				This.domainsGrid.reconfigure();
			} else {
				_console(response.responseText);
			}
        },
        failure: function(response) {
        	Ext.get(This.domainsGrid.getId()).unmask();
        	_console(response.responseText);
        }
        
    });
};

DomainViewer.prototype.showImportDomainDialog = function() {
	var This = this;
    var win = new Ext.Window({
        xtype: 'panel',
        layout: {
            type: 'accordion'
        },
        width: 500,
        items: [
        {
            xtype: 'panel',
            title: 'Enter Domain Location',
            border: false,
            layout: {
                type: 'hbox',
                defaultMargins: 10
            },
            items: [{
                xtype: 'textfield',
                flex: 1,
                emptyText: 'Enter domain location or Upload from below'
            }, {
                xtype: 'button',
                text: 'Submit',
                iconCls: 'icon-add fa fa-green',
                handler: function() {
                	var panel = this.up('panel');
                	var loc = this.prev().value;
                	if(!loc) {
                		showError('Please enter the domain location or Upload from below');
                		return;
                	}
                	win.close();
                	var domname = loc.replace(/^.+\//,"");
                	domname = domname.replace(/^.+\\/,"");
                	This.importDomain(domname, loc);
                }
            }]
        }, {
            	xtype: 'xuploadpanel',
            	collapsed: true,
            	title: 'Upload Domain',
            	collapsible: true,
            	border: false,
            	multipart_params: {type: 'domain'},
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
                		win.close();
                		var domname = loc.replace(/^.+\//,"");
                		domname = domname.replace(/^.+\\/,"");
                		This.importDomain(domname, loc);
                	}
                }
        }]
    });
    win.show();
};

DomainViewer.prototype.createListeners = function() {
	var This = this;
	this.domainsGrid.on("select", function(item, rec) {
		if(This.tabPanel)
			This.openDomain(rec.get('name'));
	});
	this.domainsGrid.down('#createbutton').on("click", function() {
	    Ext.Msg.prompt("Create Domain", "Enter name for new domain", function(btn, text) {
	        if (btn == 'ok' && text) {
	        	This.createDomain(text);
	        }
	    });
	});
	this.domainsGrid.down('#importbutton').on("click", function() {
		This.showImportDomainDialog();
	});
	this.domainsGrid.down('#downloadbutton').on("click", function() {
		var sels = This.domainsGrid.getSelectionModel().getSelection();
		if(!sels.length) {
			showError("Click on a domain first");
			return;
		}
		var domain = sels[0];
		window.open(This.op_url+"/downloadDomain?domain="+escape(domain.get("name")));
	});
	this.domainsGrid.down('#selectbutton').on("click", function() {
		var sels = This.domainsGrid.getSelectionModel().getSelection();
		if(!sels.length) {
			showError("Click on a domain first");
			return;
		}
		var domain = sels[0];
		This.selectDomain(domain.get("name"));
	});
	this.domainsGrid.down('#delbutton').on("click", function() {
		// Delete domain
		var sels = This.domainsGrid.getSelectionModel().getSelection();
		if(!sels.length) {
			showError("Click on a domain first");
			return;
		}
		var domain = sels[0];
	    Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + domain.get('name'), function(b) {
	        if (b == "yes") {
	        	This.deleteDomain(domain.get("name"));
	        }
	    });
	});
	this.domainsGrid.down('#renamebutton').on("click", function() {
		// Delete domain
		var sels = This.domainsGrid.getSelectionModel().getSelection();
		if(!sels.length) {
			showError("Click on a domain first");
			return;
		}
		var domain = sels[0];
	    Ext.MessageBox.prompt("Rename domain", "Enter the new name", function(btn, txt) {
	        if (btn == "ok" && txt) {
	        	This.renameDomain(domain.get("name"), txt);
	        }
	    }, This, false, domain.get('name'));
	});
};

DomainViewer.prototype.createMainPanel = function() {
    this.mainPanel = new Ext.Viewport({
        layout: {
            type: 'border'
        },
        items: [this.domainsGrid, this.tabPanel]
    });
    this.mainPanel.add(getPortalHeader());
};

DomainViewer.prototype.initialize_bak = function() {
    this.createLeftPanel();
    this.createTabPanel();
    this.createMainPanel();
    this.createListeners();
};

DomainViewer.prototype.initialize = function() {
    this.createLeftPanel();
    Ext.apply(this.domainsGrid, {
    	region: 'center'
    });
    this.mainPanel = new Ext.Viewport({
        layout: 'border',
        items: [getPortalHeader(), this.domainsGrid]
    });
    this.createListeners();
};