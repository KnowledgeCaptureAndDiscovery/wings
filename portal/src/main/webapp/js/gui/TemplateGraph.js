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

/**
 * <p>TemplateGraphPanel is an extension of the default Panel. It allows creation of a Template
 * 
 * @author <a href="mailto:varunratnakar@google.com">Varun Ratnakar</a>
 * @class Ext.ux.TemplateGraphPanel
 * @extends Ext.Panel
 * @constructor
 * @component
 * @version 1.0
 * @license GPL
 * 
 */

Ext.Ajax.timeout = 120000;

Ext.namespace("Ext.ux");
/**
 * Graph Panel
 * (Includes toolbar + Template Graph component)
 */
Ext.ux.TemplateGraphPanel = Ext
		.extend(
				Ext.Panel,
				{
					zoomFactor : 1.2,
					editor : null,
					editable : false,
					initComponent : function(config) {
						Ext.apply(this, config);
						var me = this;
						this.editor = new Ext.ux.TemplateGraph({
							region : 'center',
							guid : this.guid,
							store : this.store,
							cmap : this.cmap,
							url : this.url,
							template_id : this.template_id,
							editable : this.editable,
							editor_mode : this.editor_mode,
							infoPanel : this.infoPanel,
							gridPanel : this.gridPanel,
							tellMePanel : this.templatePanel.tellMePanel,
							browser : this.browser
						});

						// Create the toolbar
						var tbar = [];
						if (this.editable) {
							tbar
									.push({
										text : 'Add Component',
										iconCls : 'icon-add fa fa-green',
										handler : function() {
											Ext.MessageBox
													.show({
														title : 'Add Component',
														modal : false,
														msg : 'To add a component, drag a component from the <b>"Components Tab"</b> on the left to the Graph below',
														icon : Ext.MessageBox.INFO,
														buttons : Ext.MessageBox.OK
													});
										}
									});
							tbar.push('-');
							tbar.push({
								text : 'Delete Selected',
								iconCls : 'icon-del fa fa-red',
								handler : function() {
								},
								handler : function() {
									me.editor.deleteSelected();
								}
							});
						}
						tbar.push({
							xtype : 'tbfill'
						});
						if (this.editable) {
							if (this.editor_mode == 'tellme') {
								/*tbar.push({text:'TOP', iconCls:'TOPIcon',
											handler:function() {me.editor.findErrors();}
										});*/
							}
						}
						tbar.push({
							text : 'Layout',
							iconCls : 'icon-wflow fa-small fa-blue',
							handler : function() {
								me.editor.layout(true);
							}
						});
						tbar.push({
							// text : 'Zoom In',
							iconCls : 'icon-zoomIn fa-small fa-blue',
							handler : function() {
								me.editor.zoom(1.2);
								me.doLayout();
							}
						});
						tbar.push({
							// text : 'Zoom Out',
							iconCls : 'icon-zoomOut fa-small fa-blue',
							handler : function() {
								me.editor.zoom(1 / 1.2);
							}
						});
						tbar.push('-');
						tbar.push({
							text : 'Grab Image',
							iconCls : 'icon-snapshot fa-small fa-blue',
							handler : function() {
								me.editor.saveImage();
							}
						});

						// Apply toolbar and other styles to the panel
						Ext.applyIf(this, {
							autoScroll : true,
							bodyCls : (me.editable ? 'editableCanvas' : 'disabledCanvas'),
							layout : 'border',
							tbar : tbar
						});
						this.items = [
							this.editor
						];
						Ext.ux.TemplateGraphPanel.superclass.initComponent.apply(this, arguments);
					},

					reloadGraph : function(store) {
						this.editor.template.setData(store);
						this.editor.template.draw();
					}
				});

/**
 * Template Graph Container
 */
Ext.ux.TemplateGraph = Ext.extend(Ext.Component, {
	tb : new Ext.Toolbar(),
	graphPadding : 20,
	template_id : null,
	template : null,
	editable : false,
	autoresize : true,
	border : false,
	gridSize : 10,
	snapSize : 10,
	showGrid : true,
	scale : 1.0,
	gridColor : "rgba(230,230,230,1)",
	selBoxColor : "rgba(0,0,0,0.2)",
	selBoxBorderColor : "rgba(0,0,0,0.2)",

	initComponent : function(config) {
		Ext.apply(this, config);
		this.addEvents('graphloaded');
		Ext.ux.TemplateGraph.superclass.initComponent.apply(this, arguments);
	},

	needsLayout : function() {
		var noLayout = 0;
		for(var i=0; i<this.template.variables.length; i++) {
			var variable = this.template.variables[i];
			if(variable.x < 40 && variable.y < 40)
				noLayout++;
		}
		if(noLayout == this.template.variables.length)
			return true;
		return false;
	},
	
	onBoxReady: function(w, h) {
		Ext.ux.TemplateGraph.superclass.onBoxReady.call(this, w, h);
		
        this.template = new Template(this.template_id, this.store, this);
        this.template.setEditable(this.editable);
        this.template.draw(this.el.dom);
        this.graphLayout = new GraphLayout();
		this.initDropZone(this.el.dom, this.guid + "_ComponentTree");
		this.fireEvent('graphloaded');
		this.setEventHandlers();
	},
	
	setEventHandlers: function() {
		var me = this;
		this.template.events.dispatch
		.on("select", function(items) {
			me.autoresize = false;
			if (Object.keys(items).length == 1) {
				var item = items[Object.keys(items)[0]];
				me.showSelectedItemInfo(item);
			}
			else {
				me.hideSelectedItemInfo();
			}
		});
	},
	
	onResize: function(w, h) {
		if(this.autoresize)
			this.template.resizePanel();
	},

	refreshConstraints : function() {
		if (this.gridPanel && this.template.store.constraints)
			this.gridPanel.getStore().loadData(cloneObj(this.template.store.constraints));
	},

	initDropZone : function(item, group, callback) {
		var me = this;
		this.dropZone = new Ext.dd.DropTarget(item, {
			ddGroup : group,
			curNode : null,
			notifyEnter : function(ddSource, e, data) {
				if (data.records.length) {
					var comp = data.records[0].data.component;
					if(comp)
						data.ddel.dom.innerHTML = "Drop to add <b>" + getLocalName(comp.id) + "</b>";
					else
						data.ddel.dom.innerHTML = "Cannot drop this item";
				}
				return this.overClass;
			},
			notifyOut : function(ddSource, e, data) {
				data.ddel.dom.innerHTML = ddSource.dragText;
			},
			notifyDrop : function(ddSource, e, data) {
				if (data.records.length) {
					var comp = data.records[0].data.component;
					if(!comp)
						return false;
					var coords = me.template.transformEventCoordinates(e.getPageX(), e.getPageY());
					var node = me.template.addNode(comp);
					node.setCoords(coords);
					node.draw();
					me.template.drawLinks();
					me.template.markErrors();
					me.template.resizeViewport();
					return true;
				}
				return false;
			}
		});
	},

	zoom : function(value) {
		this.autoresize = false;
		this.template.zoom(value, true);
	},

	findErrors : function(rec) {
		var msgTarget = Ext.get(this.getId());
		msgTarget.mask('Connecting to TOP to find issues...', 'x-mask-loading');
		saveTemplateStore(this.template, this.gridPanel);
		var hpTree = this.tellMePanel.history.getComponent('tellmeHistoryTreePanel');
		var hpDetail = this.tellMePanel.history.getComponent('tellmeHistoryDetailPanel');
		runTopAlgorithm(this.template, hpTree, hpDetail, msgTarget, this.redrawCanvas, this, this.url);
	},

	layout : function(animate, domnode) {
		var msgTarget = Ext.get(this.getId());
		msgTarget.mask('Designing Layout...', 'x-mask-loading');
		//this.graphLayout.layoutVizDot(msgTarget, this.template, animate, domnode);
		this.graphLayout.layoutDot(msgTarget, this.template, animate, domnode, this.url);
	},

	saveImage : function() {
		// var scale = this.canvas.getScale();
		var scale = 1;
		var img = this.template.getImage();
		var win = window.open();
		win.document.body.appendChild(img);
	},

	getSelectedVariable : function() {
		var items = this.template.events.selectedItems;
		for ( var id in items) {
			var item = this.template.variables[id];
			if (item)
				return item;
		}
		return null;
	},

	getSelectedNode : function() {
		var items = this.template.events.selectedItems;
		for ( var id in items) {
			var item = this.template.nodes[id];
			if (item)
				return item;
		}
		return null;
	},

	selectItem : function(item, scrollTo) {
		if (typeof (item) == "string") {
			var itemid = this.template.ns + item;
			var item = this.template.variables[itemid];
			if (!item)
				item = this.template.nodes[itemid];
		}
		if (item) {
			this.template.events.selectItem(item);
			if (scrollTo) {
				this.scrollTo(item.getX(), item.getY());
			}
			this.showSelectedItemInfo(item);
		}
	},

	hideSelectedItemInfo : function() {
		this.clearVariableConstraintFilter();
		this.hideInfoPanel();
	},

	clearVariableConstraintFilter : function() {
		if (this.gridPanel) {
			this.gridPanel.store.clearFilter();
			// this.gridPanel.setTitle('Constraints: All');
		}
	},

	setVariableConstraintFilter : function(id) {
		if (this.gridPanel) {
			this.gridPanel.store.filter('subject', new RegExp("^" + id + "$"));
			// this.gridPanel.setTitle('Constraints: ' + getLocalName(id));
		}
	},

	hideInfoPanel : function() {
		if (this.infoPanel) {
			/*if (this.editable && this.infoPanel.dataGrid) {
							var gridEditor = this.infoPanel.dataGrid.getPlugin();
							gridEditor.cancelEdit();
						}*/
			this.infoPanel.hide();
		}
	},

	deleteSelected : function() {
		var items = this.template.events.selections;
		var numselections = 0;
		for ( var id in items) {
			var item = items[id];
			if (item.getInputLinks) {
				this.template.removeNode(item);
			}
			else {
				this.template.removeVariable(item);
			}
			numselections++;
		}
		if (!numselections) {
			showError('Select an Item to Delete first !');
			return;
		}

		this.refreshConstraints();
		this.updateGridVariables();
		this.clearVariableConstraintFilter();
		this.template.drawLinks();
		this.template.markErrors();
	},

	updateGridVariables : function() {
		if (this.gridPanel && this.gridPanel.variableStore && this.editable) {
			var vars = [];
			var dataVars = [];
			for ( var id in this.template.variables) {
				vars.push(id);
				if (!this.template.variables[id].isParam)
					dataVars.push(id);
			}
			vars.sort();
			var This = this;
			var values = Ext.Array.map(vars, function(varid) {
				return {
					id : varid,
					text : getPrefixedUrl(varid, This.browser.nsmap)
				};
			});
			var dValues = Ext.Array.map(dataVars, function(varid) {
				return {
					id : varid,
					text : getPrefixedUrl(varid, This.browser.nsmap)
				};
			});
			this.gridPanel.variableStore.loadData(values);
			this.gridPanel.dataVariableStore.loadData(dValues);
		}
	},
 
	showSelectedItemInfo : function(item) { 
		// if(window.console) window.console.log(item.id);
		var html = "";
		var isVariable = (item instanceof GraphVariable);

		if (this.infoPanel) {
			// Make the information panel
			if (this.editable) {
				// For Template Editor, this will be a Property Grid
				this.infoPanel.hide();
				this.infoPanel.removeAll();
				var dataGrid = this.getInfoPanelEditor(item, isVariable);
				this.infoPanel.dataGrid = dataGrid;
				this.infoPanel.add(dataGrid);
			}
			else {
				this.infoPanel.hide();
				this.infoPanel.removeAll();
				var html = this.getInfoPanelViewerHtml(item, isVariable);
				this.infoPanel.add(new Ext.Component({
					html : html
				}));
			}
			var errors = item.errors;
			if (errors && errors.length) {
				this.infoPanel.add({
					border : false,
					padding : 5,
					html : "<div style='color:red;font-size:10px;font-style:italic'>" + errors + "</div>"
				});
			}
			this.infoPanel.show();
			this.infoPanel.doLayout();
			//this.infoPanel.alignTo(this, 'b-b?');			
		}

		if (this.gridPanel) {
			this.clearVariableConstraintFilter();
			if (isVariable) {
				this.setVariableConstraintFilter(item.id);
			}
			else {
				this.clearVariableConstraintFilter();
			}
		}
	},

	getInfoPanelViewerHtml : function(item, isVariable) {
		var html = "";
		if (isVariable) {
			html += "<b>Variable: " + item.getName() + "</b>";
			html += "<div><b>" + (item.isInput ? "Input " : (item.isOutput ? "Output " : "Intermediate "));
			html += (item.isParam ? "Parameter" : "Data") + "</b></div>";
			if (item.dim)
				html += "<div><b>Dimensionality:</b> " + item.dimesionality + "-D Collection</div>";
			if (item.autofill)
				html += "<div><b>Autofill</b> values, don't ask user</div>";
		}
		else {
			var items = this.cmap ? this.cmap[item.binding.id] : null;
			if (items && items.length && (!item.isConcrete || item.binding.id)) {
				var me = this;
				var copts = {};
				copts = setURLComboListOptions(copts, items, item.binding.id, 
						'Select ' + getLocalName(item.binding.id) + ' Binding ..', 
						false, false);
				var formitem = new Ext.form.field.ComboBox(copts);
				this.infoPanel.add(formitem);
				formitem.on('select', function() {
					var val = formitem.getValue();
					if (val) {
						item.setBinding({
							id : val,
							type : 'uri'
						});
						item.isConcrete = true;
						item.draw();
						console.log(item);
					}
				});
			}
			else {
				html += "<b>Node: " + item.getName() + "</b>";
			}
			
			var port_role_map = {};
			for ( var pid in item.inputPorts) {
				var port = item.inputPorts[pid];
				port_role_map[port.id] = port.role.roleid;
			}
			
			if (item.prule.type == 'WTYPE')
				html += "<div><b>Creating multiple workflows</b> for input data collection</div>";

			if (item.prule.expr.args.length) {
				html += "<div><b>Input Data Combination: </b></div>";
				html += "<i>"+this.getExpressionText(item.prule.expr, port_role_map)+"</i>";
			}
			if(item.machineIds && item.machineIds.length)
				html += "<div><b>Can run on: </b> " + 
					Ext.Array.map(item.machineIds, function(item) {
						return getLocalName(item);
					}).join(",") + 
					"</div>";
		}
		return html;
	},
	
	getExpressionText : function(expr, port_role_map) {
		var text = "";
		if (expr && typeof expr == "object") {
			text += "<span class='prule_op'>"+expr.op + "</span> (";
			for ( var i = 0; i < expr.args.length; i++) {
				if (i > 0)
					text += ", ";
				text += this.getExpressionText(expr.args[i], port_role_map);
			}
			text += ") ";
		}
		else if (expr) {
			text = port_role_map[expr];
		}
		return text;
	},

	getInfoPanelEditor : function(item, isVariable) {
		var title = '';
		var source = {};
		source['id'] = item.id;
		var showPrule = false, showCrule = false;
		if (isVariable) {
			title = (item.isInput ? 'Input ' : (item.isOutput ? 'Output ' : 'Intermediate '))
					+ (item.isParam ? 'Parameter' : 'Data') + ' Variable: ' + item.text;
			source['a_varName'] = item.getName();
			if (item.isInput && !item.isParam) {
				source['b_varColl'] = (item.dimensionality != 0);
			}
			if (!item.isInput && !item.isParam) {
				source['b_breakpoint'] = (item.breakpoint ? true : false);
			}
			if (item.isInput)
				source['c_varAutoFill'] = (item.autofill ? true : false);
		}
		else {
			var links = this.template.getLinksToNode(item);
			for(var i=0; i<links.length; i++) {
				if(links[i].variable.dimensionality > 0) {
					showPrule = true;
					break;
				}
			}
			if(!item.isConcrete)
				showCrule = true;
			
			title = 'Node: ' + getLocalName(item.id);
			if(showPrule)
				source['a_pruleS'] = (item.prule.type == 'STYPE');
			if (showCrule)
				source['c_cruleS'] = (item.crule.type == 'STYPE');
		}
		var dataGrid = new Ext.grid.PropertyGrid({
			title : title,
			source : source,
			bodyCls : 'multi-line-grid',
			hideHeaders : true,
			nameColumnWidth : '80%',
			autoHeight : true,
			border : false,
			propertyNames : {
				'a_varName' : 'Variable Name',
				'b_varColl' : 'Input Data should be a Collection',
				'b_breakpoint' : 'Set Breakpoint for fetching metadata',
				'c_varAutoFill' : 'Automatically Select Value (Don\'t ask user)',
				'a_pruleS' : 'Use all Input Data in the same workflow',
				'c_cruleS' : 'Use all Components of this Type in the same workflow'
			}
		});
		var me = this;
		var myStore = dataGrid.getStore();
		// hide variable id from the property grid
		myStore.filterBy(function(rec) {
			if (rec.data.name == 'id')
				return false;
			else
				return true;
		});
		
		myStore.on('update', function(st, rec, op, fields) {
			if(fields == null)
				return;
			var data = rec.getData();
			var needsSweep = false;
			if (isVariable) {
				if(data.name == "a_varName") {
					var name = data.value;
					var id = getNamespace(item.id) + getRDFID(name);
					if (me.template.variables[id]) {
						Ext.MessageBox.show({
							msg : 'Another variable with name ' + name + ' already exists',
							modal : false
						});
					}
					else {
						me.template.renameVariable(item, id);
						me.refreshConstraints();
						me.updateGridVariables();
						me.clearVariableConstraintFilter();
						me.setVariableConstraintFilter(id);
					}
				}
				else if(data.name == "b_varColl") {
					var dim = data.value ? 1 : 0;
					if(item.dimensionality != dim) {
						item.setDimensionality(dim);
						me.template.forwardSweep();
					}
				}
				else if(data.name == "b_breakpoint") {
					item.setBreakpoint(data.value);
				}
				else if(data.name == "c_varAutoFill") {
					item.setAutofill(data.value);
				}
			}
			else {
				if(data.name == "a_pruleS") {
					item.setPortRule({
						type : (data.value==false) ? 'WTYPE' : 'STYPE',
						expr : item.prule.expr
					});
					me.template.forwardSweep();
				}
				else if(data.name == "c_cruleS") {
					item.setComponentRule({
						type : (data.value==true) ? 'STYPE' : 'WTYPE',
						expr : item.prule.expr
					});
					item.draw();
					me.template.forwardSweep();
				}
			}
		});
		
		var items = [
			dataGrid
		];
		if (!isVariable) {
			if(showPrule)
				items.push(this.getAdvancedRuleEditor(item));
			else if(!showCrule)
				return {
					html: title,
					bodyStyle: 'padding:5px'
				};
		}
		return {
			xtype : 'tabpanel',
			border : false,
			// plain: true,
			items : items
		};
		// return dataGrid;
	},

	getAdvancedRuleEditor : function(node) {
		var ips = node.getInputPorts();
		// var dataips = [];
		// for (var i = 0; i < ips.length; i++)
		// if (ips[i].type == 'DATA')
		// dataips.push(ips[i]);

		var ops = [
				'XPRODUCT', 'NWISE', 'INCREASEDIM', 'REDUCEDIM', 'SHIFT'
		];
		return {
			xtype : 'portruleeditor',
			title : 'Input Data Combination',
			border : false,
			ops : ops,
			expr : node.prule.expr,
			inputs : ips,
			listeners : {
				'changed' : function(expr) {
					node.prule.expr = expr;
				}
			}
		};
	},

	scrollTo : function(x, y) {
		if (this.canvasDom.parentNode.parentNode) {
			var cmp = Ext.get(this.canvasDom.parentNode.parentNode.id);
			this.canvasDom.parentNode.parentNode.scrollLeft = this.canvas.scale * x - this.graphPadding;
			cmp.scrollTo('top', this.canvas.scale * y - this.graphPadding, false);
		}
	},

	drawGrid : function() {
		if (!this.showGrid || !this.editable)
			return;
		var ctx = this.canvas.getCtx();
		ctx.beginPath();
		for ( var i = this.gridSize; i < this.template_layer.height; i += this.gridSize) {
			ctx.moveTo(0.5, i + 0.5);
			ctx.lineTo(this.template_layer.width + 0.5, i + 0.5);
		}
		for ( var i = this.gridSize; i < this.template_layer.width; i += this.gridSize) {
			ctx.moveTo(i + 0.5, 0.5);
			ctx.lineTo(i + 0.5, this.template_layer.height + 0.5);
		}
		ctx.strokeStyle = this.gridColor;
		ctx.lineWidth = 1;
		ctx.stroke();
	},

	drawSelectionBox : function() {
		var ctx = this.canvas.getCtx();
		ctx.beginPath();
		var lyr = this.template_layer;
		ctx.rect(lyr.selBoxStart.x, lyr.selBoxStart.y, lyr.selBoxEnd.x - lyr.selBoxStart.x, lyr.selBoxEnd.y
				- lyr.selBoxStart.y);
		ctx.strokeStyle = this.selBoxBorderColor;
		ctx.fillStyle = this.selBoxColor;
		ctx.lineWidth = 2;
		ctx.stroke();
		ctx.fill();
	},

	clearCanvas : function() {
		//FIXME: this.template.clear();
	}
});

Ext.define('Test.view.PortRuleEditor', {
	extend : 'Ext.panel.Panel',
	alias : 'widget.portruleeditor',

	layout : {
		type : 'hbox',
		align : 'stretch'
	},

	initComponent : function() {
		// Expects the following to be set by parent
		// - this.ops
		// - this.expr
		// - this.inputs

		this.port_role_map = {};
		this.role_port_map = {};
		this.args = [];

		for ( var i = 0; i < this.inputs.length; i++) {
			var port = this.inputs[i];
			this.port_role_map[port.id] = port.role.roleid;
			this.role_port_map[port.role.roleid] = port.id;
			this.args.push(port.role.roleid);
		}
		var This = this;
		var opmenus = [];
		var argmenus = [];
		for ( var i = 0; i < this.ops.length; i++)
			opmenus.push({
				plain : true,
				text : this.ops[i],
				handler : Ext.bind(This.opAdd, This)
			});
		for ( var i = 0; i < this.args.length; i++)
			argmenus.push({
				plain : true,
				text : this.args[i],
				handler : Ext.bind(This.argAdd, This)
			});

		this.items = this.getExpressionTextItems(this.expr);

		this.dockedItems = {
			dock : 'top',
			xtype : 'toolbar',
			items : [
					{
						text : 'Set Op',
						disabled : true,
						itemId : 'addop',
						menu : {
							items : opmenus,
							plain : true,
						}
					}, {
						text : 'Set Role',
						itemId : 'addrole',
						disabled : true,
						menu : {
							plain : true,
							items : argmenus,
						}
					}, {
						text : 'Delete',
						itemId : 'delete',
						disabled : true,
						handler : Ext.bind(This.menuDel, This)
					}
			]
		};
		this.callParent();
	},

	defaults : {
		padding : 4,
		xtype : 'text',
		listeners : {
			'click' : function(e) {
				var txt = this.text;
				if (txt == "(" || txt == ")" || txt == ",")
					return false;

				var panel = this.up('panel');
				if (panel.selection)
					panel.selection.removeCls('prule_highlighted');
				this.addCls('prule_highlighted');
				panel.selection = this;

				var isFirst = this.previousSibling() ? false : true;

				if (txt == "_") {
					panel.down('#delete').setDisabled(true);
					panel.down('#addop').setDisabled(false);
					panel.down('#addrole').setDisabled(isFirst);
				}
				else {
					panel.down('#delete').setDisabled(false);
					panel.down('#addop').setDisabled(!this.isOp);
					panel.down('#addrole').setDisabled(!this.isArg);
				}
			}
		},
	},

	opAdd : function(item) {
		this.setSelection(item.text, true, false);
	},

	argAdd : function(item) {
		this.setSelection(item.text, false, true);
	},

	menuDel : function(item) {
		this.setSelection('_', false, false);
	},

	setSelection : function(text, isOp, isArg) {
		if (this.selection) {
			this.selection.setText(text);
			this.selection.isOp = isOp;
			this.selection.isArg = isArg;
			this.expr = this.getPanelExpression();
			this.fireEvent('changed', this.expr);

			var textitems = this.getExpressionTextItems(this.expr);
			this.removeAll();
			for ( var i = 0; i < textitems.length; i++)
				this.add(textitems[i]);
			this.selection = null;
		}
	},

	getPanelExpression : function() {
		var expr = {};
		var curexpr = expr;
		var items = this.query('text');
		for ( var i = 0; i < items.length; i++) {
			var item = items[i];
			if (item.isOp) {
				if (curexpr.op) {
					// item is an operation and we are already in an operation
					// this means a sub-expression
					var newexpr = {
						parent : curexpr
					};
					curexpr.args.push(newexpr);
					curexpr = newexpr;
				}
				curexpr.op = item.text;
				curexpr.args = [];
			}
			else if (item.isArg) {
				if (!curexpr.args)
					return null;
				var port = this.role_port_map[item.text];
				// FIXME CHECK: Not allowing duplicate arguments in the same arg
				// list
				if (!Ext.Array.contains(curexpr.args, port))
					curexpr.args.push(port);
			}
			else if (item.text == ")" && curexpr.parent) {
				var oldexpr = curexpr.parent;
				curexpr.parent = null;
				curexpr = oldexpr;
			}
		}
		return expr;
	},

	getExpressionTextItems : function(expr) {
		var items = [];
		if (expr && typeof expr == "object") {
			items.push({
				text : expr.op,
				cls : 'prule_op',
				isOp : true,
				isArg : false
			});
			items.push({
				text : '('
			});
			for ( var i = 0; i < expr.args.length; i++) {
				if (i > 0)
					items.push({
						text : ','
					});
				items = items.concat(this.getExpressionTextItems(expr.args[i]));
			}
			items.push({
				text : '_',
				cls : 'prule_selectable'
			});
			items.push({
				text : ')'
			});
		}
		else if (expr) {
			items.push({
				text : this.port_role_map[expr],
				isOp : false,
				isArg : true
			});
		}
		if (!expr)
			items.push({
				text : '_',
				cls : 'prule_selectable'
			});
		return items;
	}

});
