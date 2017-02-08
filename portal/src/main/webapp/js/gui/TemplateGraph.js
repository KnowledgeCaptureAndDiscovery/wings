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
								me.editor.layout();
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
						this.editor.initTemplate(this.template_id, store);
						this.editor.redrawCanvas();
					}
				});

/**
 * Template Graph Container
 */
Ext.ux.TemplateGraph = Ext.extend(Ext.Component, {
	tb : new Ext.Toolbar(),
	template_layer : null,
	canvas : null,
	canvasDom : null,
	graphPadding : 20,
	template_id : null,
	template : null,
	editable : false,
	border : false,
	manualZoom : false,
	manualMove : false,
	gridSize : 10,
	snapSize : 10,
	snapToGrid : false,
	showGrid : true,
	gridColor : "rgba(230,230,230,1)",
	selBoxColor : "rgba(0,0,0,0.2)",
	selBoxBorderColor : "rgba(0,0,0,0.2)",

	initComponent : function(config) {
		Ext.apply(this, config);
		Ext.apply(this, {
			xtype : "box"
		});
		this.on('resize', this.onResize, this);
		this.addEvents('graphloaded');
		Ext.ux.TemplateGraph.superclass.initComponent.apply(this, arguments);
	},

	onResize : function(w, h) {
		this.panelWidth = w;
		this.panelHeight = h;
		if (!this.template_layer || !this.template_layer.selectedItems.length) {
			var w = this.manualZoom ? null : this.panelWidth;
			var h = this.manualZoom ? null : this.panelHeight;
			this.redrawCanvas(w, h);
		}
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
		 
	onRender : function(ct, position) {
		Ext.ux.TemplateGraph.superclass.onRender.call(this, ct, position);
		this.initCanvas();
		this.initTemplate(this.template_id, this.store);
		this.initDropZone(this.canvasDom, this.guid + "_ComponentTree");
		this.fireEvent('graphloaded');
	},

	initCanvas : function() {
		this.canvasDom = document.createElement('canvas');
		this.canvasDom.id = '__canvas_' + this.guid + '_' + getLocalName(this.template_id);
		this.el.dom.appendChild(this.canvasDom);
		this.canvas = new Canvas(this.canvasDom);
	},

	initTemplate : function(template_id, store) {
		this.store = store;
		this.template_id = template_id;

		this.template = new Template(this.template_id, this.store, this);
		this.template.markErrors();
		this.canvas.template = this.template;

		this.initTemplateLayer();
		this.initLayerItems();

		this.template_layer.draw();
	},

	refreshConstraints : function() {
		if (this.gridPanel && this.template.store.constraints)
			this.gridPanel.getStore().loadData(cloneObj(this.template.store.constraints));
	},

	initTemplateLayer : function() {
		var editor = this;
		if (this.canvas && this.template_layer)
			this.canvas.LayerManager.removeLayer(this.template_layer.getID());

		this.template_layer = new Canvas.Layer({
			id : "template_" + editor.template_id,
			x : 0,
			y : 0,
			selectedItems : [],
			oldx : -1,
			oldy : -1,
			on : {
				"mousemove" : function(event, type, ctx, item) {
					var mX = event.mouseX, mY = event.mouseY;
					if (editor.snapToGrid) {
						mX = Math.floor(mX / editor.snapSize) * editor.snapSize;
						mY = Math.floor(mY / editor.snapSize) * editor.snapSize;
					}
					if (mX == this.oldx && mY == this.oldy)
						return;
					this.oldx = mX;
					this.oldy = mY;

					if (this.dragging) {
						for ( var i = 0; i < this.selectedItems.length; i++) {
							var item = this.selectedItems[i];
							item.x = parseInt(mX - this.dragging[i].x);
							item.y = parseInt(mY - this.dragging[i].y);
							if (item.x < editor.graphPadding)
								item.x = editor.graphPadding;
							if (item.y < editor.graphPadding)
								item.y = editor.graphPadding;
						}
						editor.canvasDom.style.cursor = 'move';
						editor.redrawCanvas();
						editor.manualMove = true;
						/*Automatic scrolling on drag
								var panel = editor.findParentByType(Ext.ux.TemplateGraphPanel);
								if(panel && panel.view && panel.view.scroller) {
									panel.view.scroller.scrollTo(editor.canvasDom.scrollHeight);
								}*/
					}
					else if (this.newLinkFromPort && editor.editable) {
						this.clear(ctx);
						editor.drawGrid();
						this.draw();

						var port = this.newLinkFromPort;
						var toport = this.newLinkToPort;
						var fromdims = {
							x : port.x,
							y : port.y
						};
						var todims = {
							x : mX,
							y : mY
						};

						if (!port.isVariablePort && (!toport || !toport.isVariablePort)) {
							var varid = editor.template.getFreeVariableId(port.name);
							// if(port.partOfLink) varid =
							// port.partOfLink.variable.id;
							var varname = getLocalName(varid);
							if (!this.newVariable) {
								this.newVariable = new DraggerVariable(editor.template, varid, varname);
							}
							var dims = this.newVariable.getDimensions(ctx, varname);
							var x = Math.round(mX - dims.width / 2.0);
							var y = Math.round(mY);
							if (port.isInput)
								y -= dims.height;

							if (toport) {
								x = Math.round(port.x + (toport.x - port.x) / 2.0 - dims.width / 2.0);
								y = Math.round(port.y + (toport.y - port.y) / 2.0 - dims.height / 2.0);
								todims.x = Math.round(x + dims.width / 2.0);
								todims.y = y + (port.isInput ? dims.height : 0);
							}
							Link.prototype.drawPartialLink(ctx, fromdims, todims, port.isInput);
							this.newVariable.draw(ctx, varname, x, y, false);
							if (toport) {
								fromdims = todims;
								fromdims.y += port.isInput ? -dims.height : dims.height;
								todims = {
									x : mX,
									y : mY
								};
								Link.prototype.drawPartialLink(ctx, fromdims, todims, port.isInput);
							}
						}
						else {
							Link.prototype.drawPartialLink(ctx, fromdims, todims, port.isInput);
						}
						editor.template.markLinkAdditionSideEffects(this.newLinkFromPort, this.newLinkToPort,
								this.newVariable);
					}
					else if (this.selBoxStart) {
						this.selBoxEnd = {};
						this.selBoxEnd.x = event.mouseX, this.selBoxEnd.y = event.mouseY;
						this.selectedItems = [];
						var x1 = this.selBoxStart.x, x2 = this.selBoxEnd.x;
						var y1 = this.selBoxStart.y, y2 = this.selBoxEnd.y;
						if (x1 > x2) {
							var tmp = x1;
							x1 = x2;
							x2 = tmp;
						}
						if (y1 > y2) {
							var tmp = y1;
							y1 = y2;
							y2 = tmp;
						}
						for ( var i = 0; i < this.items.length; i++) {
							var item = this.items[i];
							if (!item.shape)
								continue;
							if (item.x > x1 && item.x + item.width < x2 && item.y > y1 && item.y + item.height < y2) {
								this.selectedItems.push(item.shape);
							}
						}
						this.clear(ctx);
						editor.drawGrid();
						this.draw();
						editor.drawSelectionBox();
					}
					else if (this.needRefresh && editor.editable) {
						this.clear(ctx);
						editor.drawGrid();
						this.draw();
					}
				},
				"mouseup" : function(event, type, ctx, item) {
					editor.template.clearSideEffects();
					editor.template.addLinkInCanvas(this.newLinkFromPort, this.newLinkToPort, this.newVariable);
					editor.updateGridVariables();
					editor.template.markErrors();
					editor.template.forwardSweep();

					if (!this.dragging && !this.selBoxEnd)
						editor.deselectAllItems();
					else {
						editor.showSelectedItemInfo();
					}

					this.newLinkToPort = null;
					this.newLinkFromPort = null;
					this.newVariable = null;
					this.selBoxStart = null;
					this.selBoxEnd = null;
					this.needRefresh = false;
					this.dragging = null;

					this.clear(ctx);
					editor.drawGrid();
					this.draw();

					editor.canvasDom.style.cursor = 'default';
					if (event.preventDefault)
						event.preventDefault();
					event.returnValue = false;
				},
				"mousedown" : function(event, type, ctx, item) {
					this.selBoxStart = {};
					this.selBoxStart.x = event.mouseX, this.selBoxStart.y = event.mouseY;

					if (event.preventDefault)
						event.preventDefault();
					event.returnValue = false;
				}
			},
			clear : function(ctx) {
				ctx.clearRect(this.x, this.y, this.width, this.height);
			}
		}, this.canvas.LayerManager);

		this.graphLayout = new Layout(this.template);
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
					var mouse = me.canvas.translateEventCoordsToCanvasCoords(e.getPageX(), e.getPageY());
					var node = me.template.addNode(comp);
					node.x = mouse.x;
					node.y = mouse.y;
					me.template.markErrors();
					me.redrawCanvas();
					return true;
				}
				return false;
			}
		});
	},

	initLayerItems : function() {
		if (this.template_layer.getCount() > 0)
			this.template_layer.removeAllItems();
		var items = this.template.getCanvasItems();
		for ( var i = 0; i < items.length; i++) {
			this.template_layer.addItem(items[i], false);
		}
	},

	zoom : function(value) {
		this.canvas.changeScale(value);
		this.manualZoom = true;
		this.redrawCanvas();
	},

	findErrors : function(rec) {
		var msgTarget = Ext.get(this.getId());
		msgTarget.mask('Connecting to TOP to find issues...', 'x-mask-loading');
		saveTemplateStore(this.template, this.gridPanel);
		var hpTree = this.tellMePanel.history.getComponent('tellmeHistoryTreePanel');
		var hpDetail = this.tellMePanel.history.getComponent('tellmeHistoryDetailPanel');
		runTopAlgorithm(this.template, hpTree, hpDetail, msgTarget, this.redrawCanvas, this, this.url);
	},

	layout : function() {
		var msgTarget = Ext.get(this.getId());
		msgTarget.mask('Designing Layout...', 'x-mask-loading');
		this.graphLayout.layoutDot(msgTarget, this, this.url);
	},

	saveImage : function() {
		// var scale = this.canvas.getScale();
		var scale = 1;
		var imgdata = this.getImageData(scale, true);
		window.open(imgdata);
	},

	getImageData : function(scale, show_constraints) {
		var MAX_SHOW_CONSTRAINTS_IN_GRAPH = 200;
		var x = 20;
		var y = this.template_layer.itemHeight + 10;
		var ctx = this.canvas.getCtx();

		var w = 12;
		var h = w * 1.5;

		var consHeight = this.gridPanel ? ((this.gridPanel.store.getCount() + 1) * h + 10) : 0;
		if (!show_constraints)
			consHeight = 0;

		var canvasWidth = this.template_layer.itemWidth;
		var constraints = [];
		if (show_constraints && this.gridPanel && this.gridPanel.store.getCount()) {
			var This = this;
			if(this.gridPanel.store.getCount() > MAX_SHOW_CONSTRAINTS_IN_GRAPH) {
				var cons = "Too many constraints to show in image";
				constraints.push(cons);
				var width = (cons.length * w * 0.75) + x + 20;
				if (width > canvasWidth)
					canvasWidth = width;
				consHeight = h*2 + 10;
			}
			else {
				this.gridPanel.store.data.each(function() {
					var cons = getPrefixedUrl(this.data.subject, This.browser.nsmap) + '  '
							+ getPrefixedUrl(this.data.predicate, This.browser.nsmap) + '  '
							+ getPrefixedUrl(this.data.object, This.browser.nsmap);
					constraints.push(cons);
					var width = (cons.length * w * 0.75) + x + 20;
					if (width > canvasWidth)
						canvasWidth = width;
				});
			}
		}

		this.canvasDom.height = (this.template_layer.itemHeight + consHeight) * scale;
		this.canvasDom.width = canvasWidth * scale;

		this.updateCanvasForRetina();
		
		ctx.scale(scale, scale);
		this.template_layer.clear(ctx);
		this.template_layer.draw();

		if (show_constraints && this.gridPanel && this.gridPanel.store.getCount()) {
			ctx.save();
			ctx.fillStyle = "rgba(40,40,40,1)";
			ctx.textAlign = "left";
			ctx.textBaseline = "middle";
			ctx.font = "bold 14px courier";
			var selitem = this.template_layer.selectedItems.length == 1 ? this.template_layer.selectedItems[0] : null;
			if (selitem && !selitem.getInputLinks)
				ctx.fillText("Constraints for " + selitem.text + ":", x, y);
			else
				ctx.fillText("All Constraints:", x, y);

			ctx.font = w + "px courier";
			for ( var i = 0; i < constraints.length; i++) {
				y += h;
				ctx.fillText(constraints[i], x + 10, y);
			}
			ctx.restore();
		}
		var imgdata = this.canvasDom.toDataURL();
		this.redrawCanvas();
		return imgdata;
	},

	getSelectedVariable : function() {
		var items = this.template_layer.selectedItems;
		for ( var i = 0; i < items.length; i++) {
			var item = this.template.variables[items[i].id];
			if (item)
				return item;
		}
		return null;
	},

	getSelectedNode : function() {
		var items = this.template_layer.selectedItems;
		for ( var i = 0; i < items.length; i++) {
			var item = this.template.nodes[items[i].id];
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
			this.template_layer.selectedItems = [
				item
			];
			if (scrollTo) {
				this.scrollTo(item.x - 
						(this.panelWidth/2 - item.width)/this.canvas.scale, 
						item.y - 20 );
			}
			this.redrawCanvas();
			this.showSelectedItemInfo();
		}
	},

	deselectAllItems : function() {
		this.template_layer.selectedItems = [];
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
		var items = this.template_layer.selectedItems;
		if (!items.length) {
			showError('Select an Item to Delete first !');
			return;
		}
		for ( var i = 0; i < items.length; i++) {
			var item = items[i];
			var isVariable = true;
			if (item.getInputLinks) {
				this.template.removeNode(item);
			}
			else {
				this.template.removeVariable(item);
			}
		}
		this.updateGridVariables();
		this.template.markErrors();
		this.redrawCanvas();
	},

	updateGridVariables : function() {
		if (this.gridPanel && this.gridPanel.variableStore && this.editable) {
			var vars = [];
			var dataVars = [];
			for ( var id in this.template.variables) {
				vars.push(id);
				if (this.template.variables[id].type == "DATA")
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
			// window.console.log(vars);
			this.gridPanel.variableStore.loadData(values);
			this.gridPanel.dataVariableStore.loadData(dValues);
		}
	},

	showSelectedItemInfo : function() {
		var items = this.template_layer.selectedItems;
		if (items.length == 1) {
			// if(window.console) window.console.log(item.id);
			var html = "";
			var isVariable = true;
			var item = items[0];
			if (item.getInputLinks)
				isVariable = false;

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
				var error = this.template.errors[item.id];
				if (error != null) {
					this.infoPanel.add({
						border : false,
						padding : 5,
						html : "<div style='color:red;font-size:10px;font-style:italic'>" + error.msg + "</div>"
					});
				}
				this.infoPanel.show();
				this.infoPanel.doLayout();
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
		}
		else {
			this.hideInfoPanel();
			this.clearVariableConstraintFilter();
		}
	},

	getInfoPanelViewerHtml : function(item, isVariable) {
		var html = "";
		if (isVariable) {
			html += "<b>Variable: " + getLocalName(item.id) + "</b>";
			html += "<div><b>" + (item.isInput ? "Input " : (item.isOutput ? "Output " : "Intermediate "));
			html += (item.type == "PARAM" ? "Parameter" : "Data") + "</b></div>";
			if (item.dim)
				html += "<div><b>Dimensionality:</b> " + item.dim + "-D Collection</div>";
			if (item.autofill)
				html += "<div><b>Autofill</b> values, don't ask user</div>";
		}
		else {
			var items = this.cmap ? this.cmap[item.component.binding.id] : null;
			if (items && items.length && (!item.isConcrete || item.binding.id)) {
				var me = this;
				var copts = {
					fieldLabel : getLocalName(item.component.binding.id) + ' Binding',
					labelWidth : '50%'
				};
				copts = setURLComboListOptions(copts, items, item.binding.id, 'Select Component ..', false, false);
				var formitem = new Ext.form.field.ComboBox(copts);
				this.infoPanel.add(formitem);
				formitem.on('select', function() {
					var val = formitem.getValue();
					if (val) {
						item.setBinding({
							id : val,
							type : 'uri'
						});
						item.setConcrete(true);
						me.template.forwardSweep();
						me.redrawCanvas();
					}
				});
			}
			else {
				html += "<b>Node: " + getLocalName(item.id) + "</b>";
			}
			
			var port_role_map = {};
			//html += "<div><b>Inputs: </b>";
			for ( var i = 0; i < item.inputPorts.length; i++) {
				var port = item.inputPorts[i];
				//html += (i ? ", " : "") + port.name;
				port_role_map[port.id] = port.name;
			}
			//html += "</div>";
			/*(html += "<div><b>Outputs: </b>";
			for ( var i = 0; i < item.outputPorts.length; i++) {
				html += (i ? ", " : "") + item.outputPorts[i].name;
			}
			html += "</div>";*/
			
			if (item.prule.type == 'WTYPE')
				html += "<div><b>Creating multiple workflows</b> for input data collection</div>";
			/*else
				html += "<div><b>Using all input data</b> in the same workflow</div>";*/
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
					+ (item.type == "DATA" ? 'Data' : 'Parameter') + ' Variable: ' + item.text;
			source['a_varName'] = item.text;
			if (item.isInput && item.type == "DATA") {
				source['b_varColl'] = (item.dim != 0);
			}
			if (!item.isInput && item.type == "DATA") {
				source['b_breakpoint'] = (item.breakpoint ? true : false);
			}
			if (item.isInput)
				source['c_varAutoFill'] = (item.autofill ? true : false);
		}
		else {
			var links = this.template.getLinksToNode(item);
			for(var i=0; i<links.length; i++) {
				if(links[i].variable.dim > 0) {
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
		myStore.on('update', function() {
			var mySource = dataGrid.getSource();
			if (isVariable) {
				var name = mySource['a_varName'];
				var id = getNamespace(mySource['id']) + getRDFID(name);
				if (id && id != item.id) {
					if (me.template.variables[id]) {
						Ext.MessageBox.show({
							msg : 'Another variable with name ' + name + ' already exists',
							modal : false
						});
					}
					else {
						delete me.template.variables[item.id];
						item.id = id;
						item.text = name;
						me.template.variables[item.id] = item;
						mySource['id'] = id;
					}
				}
				item.setDimensionality(mySource['b_varColl'] ? 1 : 0);
				item.setBreakPoint(mySource['b_breakpoint'] ? true : false);
				item.setAutoFill(mySource['c_varAutoFill'] ? true : false);
			}
			else {
				item.setPortRule({
					type : (mySource['a_pruleS']==false) ? 'WTYPE' : 'STYPE',
					expr : item.prule.expr
				});
				item.setComponentRule({
					type : (mySource['c_cruleS']==true) ? 'STYPE' : 'WTYPE'
				});
			}
			me.template.forwardSweep();
			me.redrawCanvas();
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
		this.template_layer.clear(this.canvas.getCtx());
	},

	updateCanvasForRetina : function() {
		if(window.devicePixelRatio == 2) {
			var ow = this.canvasDom.width;
			var oh = this.canvasDom.height;
			this.canvasDom.width = ow*2;
			this.canvasDom.height = oh*2;
			this.canvasDom.style.width = ow + 'px';
			this.canvasDom.style.height = oh + 'px';
			this.canvas.getCtx().scale(2, 2);
		}
	},
	
	redrawCanvas : function(preferredWidth, preferredHeight) {
		var minX = 9999999;
		var minY = 9999999;
		var maxX = this.graphPadding;
		var maxY = this.graphPadding;

		if (preferredWidth || preferredHeight)
			this.template_layer.draw();

		// Get required canvas dimensions
		for ( var i = 0; i < this.template_layer.items.length; i++) {
			var item = this.template_layer.items[i];
			var x = Math.round(item.x + item.width) - 0.5;
			var y = Math.round(item.y + item.height) - 0.5;
			if (x > maxX)
				maxX = x;
			if (y > maxY)
				maxY = y;
			if (item.link)
				continue;
			if (item.x < minX)
				minX = parseInt(item.x);
			if (item.y < minY)
				minY = parseInt(item.y);
		}
		if (minX == 9999999 || !preferredWidth)
			minX = this.graphPadding;
		if (minY == 9999999 || !preferredHeight)
			minY = this.graphPadding;
		for ( var i = 0; i < this.template_layer.items.length; i++) {
			var item = this.template_layer.items[i];
			if (item.link || this.manualMove)
				continue;
			var xitem;
			if (item.port)
				xitem = item.port;
			else if (item.shape)
				xitem = item.shape;
			xitem.x -= minX - this.graphPadding;
			xitem.y -= minY - this.graphPadding;
		}

		var w = (maxX + this.graphPadding) - (minX - this.graphPadding);
		var h = (maxY + this.graphPadding) - (minY - this.graphPadding);

		var scale = this.canvas.getScale();
		if (preferredWidth) {
			scale = preferredWidth / w;
			if (scale > 1)
				scale = 1;
		}
		if (preferredHeight) {
			var hscale = preferredHeight / h;
			if (hscale < scale)
				scale = hscale;
		}

		if (Ext.isIE)
			this.canvas.getCtx().scale(1 / this.canvas.scale, 1 / this.canvas.scale);

		// The following resets the Canvas (setting canvas width, and
		// height)
		this.canvasDom.width = this.panelWidth > scale * w ? this.panelWidth : scale * w;
		this.canvasDom.height = this.panelHeight > scale * h ? this.panelHeight : scale * h;
		// console.log(scale+","+w+","+this.canvasDom.width);
		// Setting some template layer variables
		this.template_layer.width = this.canvasDom.width / scale;
		this.template_layer.height = this.canvasDom.height / scale;

		// The following are used in the TemplateGraph Class only
		this.template_layer.itemWidth = w;
		this.template_layer.itemHeight = h;
		
		this.updateCanvasForRetina();
		
		// if(window.console) window.console.log(scale);
		// Resetting the scale to previous value
		this.canvas.scale = 1.0;
		this.canvas.changeScale(scale);

		// Redrawing the canvas
		this.template_layer.clear(this.canvas.getCtx());
		this.drawGrid();
		this.template_layer.draw();
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
			this.port_role_map[port.id] = port.name;
			this.role_port_map[port.name] = port.id;
			this.args.push(port.name);
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
