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
    	var rec = This.domainsGrid.getStore().find('name', tab.title);
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
        fields: ['name', 'directory', 'url', 'isLegacy'],
        autoLoad: true,
        data: this.store.list
	});
	
    this.domainsGrid = new Ext.grid.Panel({
        region: 'west',
        width: '25%',
        split: true,
        margins: '5 0 5 5',
        cmargins: '5 5 5 5',
        store: domainStore,
        bodyCls:'multi-line-grid',
        dockedItems: [{
        	xtype: 'toolbar',
        	dock: 'top',
        	items: [ 
        	{
        	    text: 'Add',
        	    iconCls: 'addIcon',
        	    menu:[
        	    {
        	    	itemId: 'createbutton',
        	    	text: 'Add Domain',
        	    	iconCls: 'addIcon'
        	    },
        	    {
        	    	itemId: 'importbutton',
        	    	text: 'Import Domain',
        	    	iconCls: 'importIcon'
        	    }]
        	}, {
        		itemId: 'delbutton',
        		text: 'Delete',
        		iconCls: 'delIcon'
        	}, {
        		itemId: 'selectbutton',
        		text: 'Select',
        		iconCls: 'default_domain'
        	}, {
        		itemId: 'downloadbutton',
        		text: 'Download',
        		iconCls: 'downloadIcon'
        	}]
        }],
        columns: [{
        	dataIndex: 'name',
        	header: 'Domain',
        	flex: 1,
        	renderer: Ext.bind(This.formatDomainName, This)
        }]
    });
};

DomainViewer.prototype.formatDomainName = function(name, item, rec) {
	var cls = this.store.selected == name ? 'default_domain' : 'domain';
	var isLegacy = rec.get('isLegacy');
	return "<div class='"+cls+"'>"+name+"</div>";
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
	var cls = this.store.selected == name ? 'default_domain' : 'domain';
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

DomainViewer.prototype.importDomain = function(domname, loc) {
	var This = this;
    var domname = getRDFID(domname);
    var rec = This.domainsGrid.getStore().find('name', domname);
    if (rec != -1) {
    	console.log(rec);
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
        	Ext.get(This.tabPanel.getId()).unmask();
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
        	 Ext.get(This.domainsGrid.getId()).unmask();
        	 try {
        		 if(response.responseText == "OK") {
        			 This.store.selected = domname; 
            		 This.domainsGrid.reconfigure();
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
        	Ext.get(This.tabPanel.getId()).unmask();
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
        	 try {
        		 if(response.responseText == "OK") {
        			 var rec = This.domainsGrid.getStore().find('name', domname);
        			 This.domainsGrid.getStore().removeAt(rec);
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
        	Ext.get(This.tabPanel.getId()).unmask();
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
                iconCls: 'addIcon',
                handler: function() {
                	var panel = this.up('panel');
                	var loc = this.prev().value;
                	if(!loc) {
                		showError('Please enter the domain location or Upload from below');
                		return;
                	}
                	win.close();
                	var domname = loc.replace(/^.+\//,"");
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
                addButtonCls: 'addIcon',
                deleteButtonCls: 'delIcon',
                uploadButtonCls: 'uploadIcon',
                cancelButtonCls: 'delIcon',
                listeners : {
                	"uploadcomplete" : function(item, files) {
                		// Just check the first file (only one file upload allowed here)
                		var file = files[0];
                		var loc = file.location;
                		win.close();
                		var domname = loc.replace(/^.+\//,"");
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
		if(!sels.length) return;
		var domain = sels[0];
		window.open(This.op_url+"/downloadDomain?domain="+escape(domain.get("name")));
	});
	this.domainsGrid.down('#selectbutton').on("click", function() {
		var sels = This.domainsGrid.getSelectionModel().getSelection();
		if(!sels.length) return;
		var domain = sels[0];
		This.selectDomain(domain.get("name"));
	});
	this.domainsGrid.down('#delbutton').on("click", function() {
		// Delete domain
		var sels = This.domainsGrid.getSelectionModel().getSelection();
		if(!sels.length) return;
		var domain = sels[0];
	    Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + domain.get('name'), function(b) {
	        if (b == "yes") {
	        	This.deleteDomain(domain.get("name"));
	        }
	    });
	});
};

DomainViewer.prototype.createMainPanel = function() {
    this.mainPanel = new Ext.Viewport({
        layout: {
            type: 'border'
        },
        items: [this.domainsGrid, this.tabPanel]
    });
    this.mainPanel.add(getPortalHeader(CONTEXT_ROOT));
};

DomainViewer.prototype.initialize = function() {
    this.createLeftPanel();
    this.createTabPanel();
    this.createMainPanel();
    this.createListeners();
};