function _console(msg) {
	if (window.console) {
		window.console.log(msg);
		window.console.trace();
	}	
}

function getRDFID(id) {
	id = id.replace(/[^a-zA-Z0-9_\-\.]/g, '_');
	if (id.match(/^([0-9]|\.|\-)/))
		id = '_' + id;
	return id;
}

function getLocalName(url) {
	if (!url)
		return url;	
	if (url.indexOf('urn:') == 0)
		return url.replace(/^.*:/, '');
	return url.replace(/^.*#/, '');
}

function getNamespace(url) {
	if (!url)
		return url;
	if (url.indexOf('urn:') == 0)
		return url.replace(/:.*$/, ':');
	return url.replace(/#.*$/, '#');
}

function getPrefixedUrl(url, nsmap, default_ns) {
	if (!url)
		return url;
	// If what's passed in isn't a string (i.e. no replace function), just
	// return it as it is
	if (typeof url.replace != "function")
		return url;

	var nurl = url;
	for ( var pfx in nsmap) {
		var nsurl = nsmap[pfx];
		nurl = nurl.replace(nsurl, pfx + ":");
	}
	if (default_ns) {
		nurl = nurl.replace(default_ns, "");
	} else if (nurl == url) {
		nurl = getLocalName(nurl);
	}
	return nurl;
}

function xsdDateTime(date) {
	function pad(n) {
		var s = n.toString();
		return s.length < 2 ? '0' + s : s;
	}
	var yyyy = date.getFullYear();
	var mm1 = pad(date.getMonth() + 1);
	var dd = pad(date.getDate());
	var hh = pad(date.getHours());
	var mm2 = pad(date.getMinutes());
	var ss = pad(date.getSeconds());

	return yyyy + '-' + mm1 + '-' + dd + 'T' + hh + ':' + mm2 + ':' + ss;
}

function getTreePath(node, field, separator) {
	field = field || node.idProperty;
	separator = separator || '/';

	var path = [ node.get(field) ];

	var parent = node.parentNode;
	while (parent) {
		path.unshift(parent.get(field));
		parent = parent.parentNode;
	}
	return separator + path.join(separator);
}

function showError(msg) {
	Ext.Msg.show({
		icon : Ext.MessageBox.ERROR,
		buttons : Ext.MessageBox.OK,
		msg : msg
	});
}

function showInfo(msg) {
	Ext.Msg.show({
		icon : Ext.MessageBox.INFO,
		buttons : Ext.MessageBox.OK,
		msg : msg
	});
}

function showWarning(msg) {
	Ext.Msg.show({
		icon : Ext.MessageBox.WARNING,
		buttons : Ext.MessageBox.OK,
		msg : msg
	});
}

function getPortalHeader() {
	var homepath = CONTEXT_ROOT;
	var userpath = USER_ROOT;
	var userdompath = USERDOM_ROOT;
	var compath = COM_ROOT;
	var html = "<div class=\"menu\"><ul>"
		+ "<li class=\"first active\"><a href=\""+homepath+"\">Home</a></li>"
		+ "<li><a href=\"#\">Analysis</a><ul>\n"
		+ "<li class=\"first\"><a href=\""
		+ userdompath
		+ "/workflows/edit\">Edit Workflows</a></li>\n"
		+ "<li><a href=\""
		+ userdompath
		+ "/workflows\">Run Workflows</a></li>\n"
		+ "<li class=\"last\"><a href=\""
		+ userdompath
		+ "/executions\">Access Runs</a></li>\n"
		+ "</ul></li>\n"
		+ "<li><a href=\"#\">Advanced</a><ul>\n"
		+ "<li class=\"first\"><a href=\""
		+ userdompath
		+ "/data\">Manage Data</a></li>\n"
		+ "<li><a href=\""
		+ userdompath
		+ "/components/type\">Manage Component Types</a></li>\n"
		+ "<li><a href=\""
		+ userdompath
		+ "/components\">Manage Components</a></li>\n"
		+ "<li><a href=\""
		+ compath
		+ "/resources\">Describe Resources</a></li>\n"
		+ "<li class=\"last\"><a href=\""
		+ userpath
		+ "/domains\">Manage Domain</a></li>\n"
		+ "</ul></li>\n" 
		+ (VIEWER_ID != 'null' ? "<li style='float:right'><a href=\""
		+ homepath
		+ "/jsp/logout.jsp\">Logout <span class='user'>"+VIEWER_ID+"</span></a></li>" : '')
		+ (VIEWER_ID != USER_ID ? 
				"<li style='float:right'>"
				+ "<a style='color:pink'><i>Viewing "+USER_ID+" account</i></a></li>" : "") 
		+ "</ul>"
		+ "</div>\n";
	return new Ext.Container(
			{
				id : "app-north",
				region : 'north',
				height : 76,
				layout : {
					type : "vbox",
					align : "stretch"
				},
				items : [
						{
							id : "app-header",
							bodyStyle : "background-color: transparent",
							border : false,
							region : 'north',
							height : 50,
							layout : 'fit',
							items : [ {
								border : false,
								xtype : "component",
								id : "app-header-title",
								html : "<a href=\""+homepath+"\">Wings Portal</a>",
							} ]
						},
						{
							id : "app-menu",
							border : false,
							xtype : "component",
							height : 26,
							html : html
						}]
			});
}

function getPortalFooter() {
	
}

function setURLComboListOptions(copts, data, selection, emptyText, editable,
		multi) {
	copts.emptyText = emptyText;
	var xdata = [];
	if (data) {
		for ( var x = 0; x < data.length; x++) {
			xdata[x] = {
				"uri" : data[x],
				"name" : getLocalName(data[x])
			};
		}
	}
	copts.store = {
		xtype : 'store',
		fields : [ 'uri', 'name' ],
		data : xdata,
		sorters : [ 'name' ]
	};

	if (selection)
		copts.value = selection;
	if (!editable)
		copts.forceSelection = true;
	copts.sorters = [ 'name' ];
	copts.editable = editable;
	copts.queryMode = 'local';
	copts.displayField = 'name';
	copts.valueField = 'uri';
	copts.triggerAction = 'all';
	copts.multiSelect = multi;
	return copts;
}

/*
 * Uncached Cell Editing Allows full flexibility of editors based on record data
 * *WARNING* -- will create a new cell editor for each row !! *WARNING* -- don't
 * use for grids with a large number of cells and records
 */
Ext.define('Ext.grid.plugin.FlexibleCellEditing', {
	extend : Ext.grid.plugin.CellEditing,
	getEditor : function(record, column) {
		var me = this;
		// var editors = me.editors;
		var editorId = column.getItemId();
		// editorId += "-"+getLocalName(record.get('subject'));
		// editorId += "-"+getLocalName(record.get('predicate'));
		/*
		 * for(var key in record.getData()) { editorId += record.get(key)+"|"; }
		 */
		// var editor = editors.getByKey(editorId);
		var editorOwner = me.grid.ownerLockable || me.grid;
		// editor = null;
		// if(!editor) {
		var coleditor = column.getEditor(record);
		if (!coleditor)
			return false;
		editor = new Ext.grid.CellEditor({
			floating : true,
			editorId : editorId,
			field : coleditor
		});
		editorOwner.add(editor);
		editor.on({
			scope : me,
			specialkey : me.onSpecialKey,
			complete : me.onEditComplete,
			canceledit : me.cancelEdit
		});
		column.on('removed', me.cancelActiveEdit, me);
		// editors.add(editor);
		// }
		editor.grid = me.grid;
		editor.editingPlugin = me;
		return editor;
	}
});