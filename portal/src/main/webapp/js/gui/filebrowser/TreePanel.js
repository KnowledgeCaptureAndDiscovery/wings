Ext.define('Wings.fb.TreePanel', {
    extend: 'Ext.tree.Panel',

    requires: [
        'Ext.tree.*',
        'Ext.data.*'
    ],
    xtype: 'fileTreePanel',

    title: 'Files',
    tools:[{
        type: 'refresh',
        handler: 'onrefresh'
    }],
    useArrows: true,
    border: true,
    split: true,
    width: '25%',
    margins: '5 0 5 5',
	cmargins : '5 0 5 0',

    initComponent: function() {
    	var me = this;
        this.cname = this.cid.replace(/.*#/, '');
        
        Ext.apply(this, {
            store: Ext.create('Ext.data.TreeStore',{
                proxy: {
                    type: 'ajax',
                    actionMethods: {
                        read: 'GET'
                    },
                    url: me.address.urlGet,
                    reader: {
                        type: 'json'
                    },
                    extraParams:{
                        cid: me.cid
                    },
                    writer: {
                        type: 'json'
                    }
                },
                root: {
                    text: me.cname,
                    path: me.cname,
                    expanded: true,
                    iconCls: 'icon-folder fa fa-yellow',
					expIconCls: 'icon-folder-open fa fa-yellow'
                },
                folderSort: true
            }),

            tbar: [{
            	text: 'Initialize',
            	menu: {
            		xtype: 'menu',
            		items: me.initializeMenuItems([
            		   "Python", "Perl", "PHP", "R", "Java", "Binary", "Generic"
            		])
            	}
            }/*,{
                text: 'Expand All',
                scope: this,
                handler: this.onExpandAllClick
            }, {
                text: 'Collapse All',
                scope: this,
                handler: this.onCollapseAllClick
            }*/],
        });
        
        me.foldermenu = new Ext.menu.Menu({
            items: [{
                text: 'New File',
                handler: this.onNewFile,
                scope: this
            },{
                text: 'New Folder',
                handler: this.onNewFolder,
                scope: this
            },{
                text: 'Rename',
                handler: this.onRename,
                scope: this
            },{
                text: 'Delete',
                handler: this.onDelete,
                scope: this
            }]
        });
        me.filemenu = new Ext.menu.Menu({
            items: [{
                text: 'Rename',
                handler: this.onRename,
                scope: this
            },{
                text: 'Delete',
                handler: this.onDelete,
                scope: this
            }]
        });
        this.on('itemcontextmenu',this.onContextItem);
        this.on('beforeload',this.onBeforeLoad);
        this.on('load', this.onAfterLoad);
        this.callParent();
    },

    onBeforeLoad: function(store, operation, eOpts){
        var path = this.up('filebrowser').getPath(operation.node);
        store.proxy.extraParams.path = path;
    },
    
    onAfterLoad: function(store, node, records) {
    	for(var i=0; i<records.length; i++) {
    		var record = records[i];
    		if(record.isLeaf()) {
    			record.data.iconCls = 'icon-file-alt fa-menu fa-blue'
    		}
    		else {
    			record.data.iconCls = 'icon-folder fa fa-yellow';
    			record.data.expIconCls = 'icon-folder-open fa fa-yellow';
    		}
    	}
    },

    initializeMenuItems: function(languages) {
    	var menu = [];
    	var me = this;
    	for(var i=0; i<languages.length; i++) {
    		var lang = languages[i];
    		menu.push({
    	        text: lang,
    	        handler: me.onInitialize,
    	        scope: me
    		})
    	}
    	return menu;
    },
    
    onInitialize: function(item) {
    	var me = this;
    	var address = this.address;
    	var lang = item.text;
    	// Initialize
    	Ext.Msg.confirm("Initialize", "This will create (and overwrite existing) run and io.sh scripts. Do you want to continue ?", 
    		function(btn) {
    			if(btn == "yes") {
    				me.getEl().mask("Initializing " + lang);
    	            Ext.Ajax.request({
    	                url: address.urlInitialize,
    	                method: 'POST',
    	                params: {cid: me.cid, language: lang},
    	                success: function (response, options) {
    	                	me.fireEvent('initialized');
    	                	me.getEl().unmask();
    	                	me.store.getRootNode().removeAll();
    	                	me.store.load();
    	                },
    	                failure: function (response, options) {
    	                	me.getEl().unmask();
    	                    alert('fail');
    	                }
    	            });    				
    			}
    		}
    	);
    	
    },
    
    onNewFile: function(){
        var address = this.address;
        var record = this.getSelectionModel().getSelection()[0];
        record.expand();
        var path = this.up('filebrowser').getPath(record);
        var me = this;
        Ext.Msg.prompt("New File", "Enter name for the new file:", function(btn, text) {
            if (btn == 'ok' && text) {
            	path += "/" + text;
            	me.getEl().mask("Adding new file");
	            Ext.Ajax.request({
	                url: address.urlNewFile,
	                method: 'POST',
	                params: {cid: me.cid, path: path},
	                success: function (response, options) {
	                	me.getEl().unmask();
	                	if(text == "run")
	                		me.fireEvent('initialized');
	                	record.appendChild({
	                        text: text,
	                        path: path,
	                        leaf: true,
	                        iconCls: 'icon-file-alt fa-menu fa-blue'
	                    });
	                },
	                failure: function (response, options) {
	                	me.getEl().unmask();
	                    alert('fail');
	                }
	            });
            }
        });
    },

    onNewFolder: function(){
        var address = this.address;
        var record = this.getSelectionModel().getSelection()[0];
        record.expand();
        var path = this.up('filebrowser').getPath(record);
        var me = this;
        Ext.Msg.prompt("New Folder", "Enter name for the new folder:", function(btn, text) {
        	if (btn == 'ok' && text) {
        		path += "/" + text;
        		me.getEl().mask("Adding new folder");
        		Ext.Ajax.request({
        			url: address.urlNewDirectory,
        			method: 'POST',
        			params: {cid: me.cid, path: path},
        			success: function (response, options) {
        				me.getEl().unmask();
        				var tmp = record.appendChild({
        					text: text,
        					path: path,
        					leaf: false,
        					expanded: false,
        					iconCls: 'icon-folder fa fa-yellow',
        					expIconCls: 'icon-folder-open fa fa-yellow'
        				});
        				console.log(tmp);
        			},
        			failure: function (response, options) {
        				me.getEl().unmask();
        				alert('fail');
        			}
        		});
            }
        });        
    },

    onRename: function(){ 
        var address = this.address;
        var record = this.getSelectionModel().getSelection()[0];
        if(record.isRoot()) {
        	Ext.Msg.show({
        		title:"Error", 
        		msg:"Cannot rename root folder"
        	});
        	return;
        }
        var curname = record.get('text');
        var path = this.up('filebrowser').getPath(record);
        var me = this;
        Ext.Msg.prompt("New name", "Enter new name:", function(btn, text) {
            if (btn == 'ok' && text) {
            	me.getEl().mask("Renaming");
	            Ext.Ajax.request({
	                url: address.urlRename,
	                method: 'POST',
	                params: {cid: me.cid, path: path, newname: text},
	                success: function (response, options) {
	                	me.getEl().unmask();
	                	record.set('text', text);
	                	record.set('path', path);
	                	record.commit();
	                },
	                failure: function (response, options) {
	                	me.getEl().unmask();
	                    alert('fail');
	                }
	            });
            }
        }, this, false, curname);        
    },

    onDelete: function(){
        var address = this.address;
        var record = this.getSelectionModel().getSelection()[0];
        var path = this.up('filebrowser').getPath(record);
        var item = record.get('text');
        var me = this;
        Ext.Msg.confirm('Delete', 'Are you sure you want to delete ?', function (button) {
        	if (button == 'yes') {
        		me.getEl().mask("Deleting");
        		Ext.Ajax.request({
        			url: address.urlDelete,
        			method: 'POST',
        			params: {cid: me.cid, path: path},
        			success: function (response, options) {
        				me.getEl().unmask();
        				if(record.isRoot()) {
        					me.store.getRootNode().removeAll();
    	                	me.store.reload();
        				}
        				else
        					record.remove(false);
        			},
        			failure: function (response, options) {
        				me.getEl().unmask();
        				alert('fail');
        			}
        		});                    	 
        	}
         });
    },

    onContextItem: function(view, record, item, index, e, eOpts){
        var me = this;
        var menu;
        if(record.data.leaf){
            menu = me.filemenu;
        }else{
            menu = me.foldermenu;
        }
        me.record = record;
        me.HTMLTarget = item;
        e.stopEvent();
        menu.showAt(e.getXY());
        menu.doConstrain();
    },

    onExpandAllClick: function(){
        var me = this,
            toolbar = me.down('toolbar');

        me.getEl().mask('Expanding tree...');
        toolbar.disable();

        this.expandAll(function() {
            me.getEl().unmask();
            toolbar.enable();
        });
    },

    onCollapseAllClick: function(){
        var toolbar = this.down('toolbar');

        toolbar.disable();
        this.collapseAll(function() {
            toolbar.enable();
        });
    }

});
