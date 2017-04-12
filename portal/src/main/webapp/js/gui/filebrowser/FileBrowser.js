Ext.define('Wings.fb.FileBrowser',{
    extend: 'Ext.panel.Panel',
    xtype: 'filebrowser',
    layout: 'border',
    items: [{
        region: 'west', 
        xtype: 'fileTreePanel'
    },{
        region: 'center',
        xtype: 'tabpanel',
        margins: '5 5 5 0',
        enableTabScroll: true,
        activeTab: 0,
        resizeTabs: true,
        plain: true,
        border: false,
        
        items:[{
            title: 'Files',
            layout: 'fit',
            //id: 'tab-files',
            saved: true,
            border: true,
            bodyStyle: "border-width: 0px 0px 1px 0px",
            html: '<div class="x-toolbar x-toolbar-default highlightIcon" ' + 
            	'style="padding:10px;font-size:1.5em;border-width:0px 1px 1px 1px">' +
            	'Edit Component Code</div><div style="padding:5px; line-height:1.5em">' + 
            	'With this interface, you can: <ul>' + 
            	'<li>View/Edit Files by clicking on the links in the tree on the left</li></ul></div>'
        }],
        listeners: {
            beforetabchange: function(tabPanel, newCard, oldCard, eOpts){
                if(oldCard == null){
                    return true;
                }
                if(oldCard.saved == false){
                    Ext.Msg.confirm('Save', 'Save changes?', function (button) {
                        oldCard.saved = true;
                        var btn = oldCard.down('button[text=save]');
                        btn.disable();
                        if (button == 'yes') {
                            btn.fireEvent('click',btn);
                        }
                        tabPanel.setActiveTab(newCard);
                    });
                    return false;
                }
            }
        }
    }],

    initComponent: function(){
        this.items[0].address = this.address;
        this.items[0].cid = this.cid;
        this.cname = this.cid.replace(/.*#/, '');
        this.callParent(arguments);

        this.down('fileTreePanel').on('itemclick',this.onItemclick);
    },

    languageName: function(filename){
        if(filename.match(/\.js$/)){
            return "text/javascript";
        }else if(filename.match(/\.cpp$/i)){
            return "text/x-c++src";
        }else if(filename.match(/\.css$/)){
            return "text/css";
        }else if(filename.match(/\.c$/i)){
            return "text/x-csrc";
        }else if(filename.match(/\.java$/)){
            return "text/x-java";
        }else if(filename.match(/\.html$/i)){
            return "text/html";
        }else if(filename.match(/\.py$/i)){
            return "text/x-python";
        }else if(filename.match(/\.sh$/i)){
            return "text/x-sh";
        }else if(filename.match(/^\/?run$/)){
            return "text/x-sh";
        }else if(filename.match(/\.php$/i)){
            return "text/x-php";
        }else if(filename.match(/\.pl$/i)){
            return "text/x-perl";
        }else if(filename.match(/\.R/i)){
            return "text/x-rsrc";
        }else if(filename.match(/\.xml$/i)){
            return "text/xml";
        }else if(filename.match(/\.owl/i)){
            return "text/xml";
        }
    },
    
    getPath: function(item) {
    	if(!item || item.isRoot())
    		return "";
        return item.raw.path || item.data.path;    
    },

    onItemclick: function(view, selectedItem){
    	if(!selectedItem.isLeaf())
    		return;
    	
        var tabs = this.up('filebrowser').down('tabpanel');
        var urlSave = this.up('filebrowser').address.urlSave;
        var urlViewFile = this.up('filebrowser').address.urlViewFile;
        var path = this.up('filebrowser').getPath(selectedItem);
        var cid = this.cid;

        var tabId = 'tab-'+path;
        tabId = tabId.replace(/[ \/\.]/g,'-');
        var tab = tabs.getComponent(tabId);
        if(!tab){
        	var language = this.up('filebrowser').languageName(path);
        	var codearea = new Ext.ux.form.field.CodeMirror({
        		showToolbar: false,
        		urlSave: urlSave,
        		layout: 'fit'
        	});
        	
        	var savebtn = new Ext.button.Button({
        		text: 'Save',
        		iconCls: 'icon-save fa fa-blue',
        		disabled: true,
        		handler: function() {
        			var value = codearea.getValue();
            		var paneltab = this.up('panel');   			
     			
                    Ext.Ajax.request({
                        url: urlSave,
                        method: 'POST',
                        params: {cid: cid, path: path, filedata: value},
                        success: function (response, options) {
                            savebtn.disable();
                            paneltab.setTitle(paneltab.title.replace(/^\*/, ''));  
                            paneltab.origValue = value;
                        },
                        failure: function (response, options) {
                            alert('fail');
                        }
                    });
        		}
        	});
        	
        	codearea.on("change", function(item, val) {
        		var paneltab = item.up('panel');
        		if(!codearea.initialize) {
        			codearea.initialize=true;
        		}
        		else {
        			if(val != paneltab.origValue) {
        				paneltab.setTitle("*" + paneltab.title.replace(/^\*/, ''));
        				savebtn.enable();
        			}
        			else {
        				paneltab.setTitle(paneltab.title.replace(/^\*/, ''));
        				savebtn.disable();
        			}
        		}
        	});
        	
        	var tbar = new Ext.toolbar.Toolbar({
        		style: 'border-top:0px',
        		border: true,
        		items: [savebtn]
        	});
        	
        	var ntab = new Ext.panel.Panel({
        		tbar: tbar,
        		id: tabId,
        		layout: 'fit',
        		saved: true,
        		closable: true, 
        		autoWidth: true,
        		autoHeight: true,
        		autoScroll: true,
        		active: true,        		
        		border: false,
        		title: selectedItem.data.text,
        		items: codearea
        	});
        	
        	Ext.Ajax.request({
        		url: urlViewFile,
        		params: {
        			cid: cid,
        			path: path
        		},
        		method: 'GET',
        		success: function (response, options) {
        			codearea.initialize=false;
        			codearea.setValue(response.responseText);
        			tab = tabs.add(ntab);
        			tab.origValue = response.responseText;
        			tabs.setActiveTab(tab);
        			if(language != null){
        				codearea.setMode(language);
        			}
        		},
        		failure: function (response, options) {
        		}
        	});
        }
            
        tabs.setActiveTab(tab);
    }
});