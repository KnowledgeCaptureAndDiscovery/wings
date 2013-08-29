Ext.ns('TellMe');


/**
 * TellMe.HistoryStore
 * @extends Ext.data.Store
 * Record defintion:
 *  - teacher
 *  - student
 *  - student_detail
 *  - templates
 *  - status
 */
if(!Ext.ModelManager.isRegistered('TellMe.HistoryRecord')) {
	Ext.define('TellMe.HistoryRecord', {
		extend:'Ext.data.Model', 
		fields:[
			{name:'teacher', mapping:'teacher'},
			{name:'student', mapping:'student'},
			{name:'log', mapping:'log'},
			{name:'templates', mapping:'templates'},
			{name:'status', mapping:'status'}
		]
	});
}

/**
 * TellMe.HistoryTree
 * @extends Ext.grid.TreePanel
 * This is a custom grid which will displays TellMe History. It is tied to
 * a specific record definition by the dataIndex properties.
 */
TellMe.HistoryTree = Ext.extend(Ext.tree.Panel, {
	initComponent : function() {
		Ext.apply(this, {
			root: {id:this.rootId, text:this.rootText, iconCls:'wflowIcon', leaf:true},
			border:false,
			autoScroll: true,
			bodyCssClass:'hrefTree'
		});
		TellMe.HistoryTree.superclass.initComponent.call(this);
	}
});

/**
 * TellMe.HistoryDetail
 * @extends Ext.Panel
 * This is a specialized Panel which is used to show information about tellme history.
 */
TellMe.HistoryDetail = Ext.extend(Ext.Panel, {
	// add tplMarkup as a new property
	tplMarkup1: [
		'<div class="tellmeteacher">&gt; {teacher}</div>',
		'<div class="tellmestudent tellmestudent_{status}">&lt; {student}</div>',
		'<pre class="tellmelog"><h4>Log:</h4>{log}</pre>'
	],
	// startingMarup as a new property
	//startingMarkup: 'Select a history item from above to see additional details',

	// override initComponent to create and compile the template
	// apply styles to the body of the panel and initialize
	// html to startingMarkup
	initComponent: function() {
		this.tpl1 = new Ext.Template(this.tplMarkup1);
		Ext.apply(this, {
			padding:0
			//html: this.startingMarkup
		});
		// call the superclass's initComponent implementation
		TellMe.HistoryDetail.superclass.initComponent.call(this);
	},
	// add a method which updates the details
	updateDetail: function(data) {
		this.tpl1.overwrite(this.body, data);
	}
});


/**
 * TellMe.HistoryPanel
 * @extends Ext.Panel
 *
 * This is a specialized panel which is composed of both a tellmeHistoryTree
 * and a tellmeHistoryPanel panel. It provides the glue between the two
 * components to allow them to communicate. You could consider this
 * the actual application.
 *
 */
TellMe.HistoryPanel = Ext.extend(Ext.Panel, {
	// override initComponent
	initComponent: function() {
		// used applyIf rather than apply so user could
		// override the defaults
		Ext.applyIf(this, {
			//frame: true,
			title: 'History',
			width: 540,
			height: 400,
			layout: 'border',
			border:false,
			items: [
				new TellMe.HistoryTree({
					itemId: 'tellmeHistoryTreePanel',
					rootText: this.tname,
					rootId: this.tid,
					region: 'center',
					border: false
				}),
				new TellMe.HistoryDetail({
					itemId: 'tellmeHistoryDetailPanel',
					region: 'south',
					border: false,
					height: 250,
					autoScroll:true,
					split: true
				})
			]
		});
		// call the superclass's initComponent implementation
		TellMe.HistoryPanel.superclass.initComponent.apply(this, arguments);
	},
	// override initEvents
	initEvents: function() {
		// call the superclass's initEvents implementation
		TellMe.HistoryPanel.superclass.initEvents.call(this);

		// now add application specific events
		// notice we use the selectionmodel's rowselect event rather
		// than a click event from the grid to provide key navigation
		// as well as mouse navigation
		var tree = this.getComponent('tellmeHistoryTreePanel');
		/*var gridSm = this.getComponent('tellmeHistoryTreePanel').getSelectionModel();*/
		tree.on('itemclick', this.onNodeSelect, this);
		/*grid.store.on('update', this.onStoreUpdate, this);*/
	},
	// add a method called onRowSelect
	// This matches the method signature as defined by the 'rowselect'
	// event defined in Ext.grid.RowSelectionModel
	onNodeSelect: function(view, rec, item, ind, e) {
		// getComponent will retrieve itemId's or id's. Note that itemId's
		// are scoped locally to this instance of a component to avoid
		// conflicts with the ComponentMgr
		var detailPanel = this.getComponent('tellmeHistoryDetailPanel');
		var data = rec.data.template ? rec.data : rec.raw;
		if(data.template) 
			showTemplate(data.template, true);
		detailPanel.updateDetail(data);
		return true;
	},
	onStoreUpdate: function(st, r, op) {
		var detailPanel = this.getComponent('tellmeHistoryDetailPanel');
		detailPanel.updateDetail(r.data);
	}
});


