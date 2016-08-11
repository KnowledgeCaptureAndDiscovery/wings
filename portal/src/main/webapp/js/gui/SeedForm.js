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
 * <p>SeedForm is an extension of the default FormPanel. It allows creation of a Seeded Template
 * 
 * @author <a href="mailto:varunratnakar@google.com">Varun Ratnakar</a>
 * @class Ext.ux.SeedForm
 * @extends Ext.FormPanel
 * @constructor
 * @component
 * @version 1.0
 * @license GPL
 * 
 */

Ext.Ajax.timeout = 90000;

Ext.namespace("Ext.ux");
Ext.ux.form.SeedForm = Ext.extend(Ext.FormPanel, {
    url: null,
    template_id: null,
    graph: null,

    initComponent: function(config) {
        Ext.apply(this, config);
        Ext.apply(this, {
            autoHeight: true,
            autoScroll: true,
            layout: 'hbox',
            defaults: {
                border: false,
                flex: 1,
                layout: 'anchor',
                padding: 4
            },
            fieldDefaults: {
                anchor: '-10',
                labelAlign: 'right',
                labelWidth: 140,
                labelStyle: 'text-overflow:ellipsis'
            },
            items: []
            });

        this.flatItems = [];
        var tmp = {
            items: []
            };

        var dtypes = {};

        for (var i = 0; i < this.store.length; i++) {
            if (i == Math.ceil(this.store.length / 2.0)) {
                this.items.push(tmp);
                tmp = {
                    items: []
                    };
            }
            var item = this.store[i];
            var formitem;
            var copts = {
                uri: item.id,
                name: item.name,
                wtype: item.type,
                fieldLabel: Ext.util.Format.ellipsis(item.name, 25)
                };
            if (item.type == "data") {
                var emptyText = 'Select ' + (item.dim ? 'multiple files': 'a file') + '...';
                copts = setURLComboListOptions(copts, item.options, item.binding, emptyText, !item.dim, item.dim);
                var formitem = new Ext.form.field.ComboBox(copts);
                // Allow shift selection etc
                formitem.getPicker().getSelectionModel().setSelectionMode('MULTI');
            } else if (item.type == "param") {
                dtypes[item.name] = item.dtype;
                // Store datatype for this param to be sent to server
                var dtype = getLocalName(item.dtype);
                copts.emptyText = 'Enter a ' + dtype + ' value...';
                if (item.binding)
                    copts.value = item.binding;

                if (dtype == "boolean") {
                    formitem = new Ext.form.field.ComboBox({
                        editable: false,
                        store: [[true, 'true'], [false, 'false']]
                        });
                    Ext.apply(formitem, copts);
                } else if (dtype == "float") {
                    copts['decimalPrecision'] = 6;
                    formitem = new Ext.form.NumberField(copts);
                } else if (dtype == "int" || dtype == "integer") {
                    copts.allowDecimals = false;
                    formitem = new Ext.form.NumberField(copts);
                } else if (dtype == "date") {
                	copts.format = 'Y-m-d';
                	formitem = new Ext.form.DateField(copts);
                } else
                    formitem = new Ext.form.TextField(copts);
            }
            if (formitem) {
                formitem.on('focus', function(item) {
                    if (this.graph && this.graph.editor)
                        this.graph.editor.selectItem(item.name, true);
                }, this);
                tmp.items.push(formitem);
                this.flatItems.push(formitem);
            }
        }
        this.items.push(tmp);

        var me = this;
        
        var tbar = [];
        if(!this.light_reasoner) {
        	tbar = [{
                text: 'Suggest Data',
                iconCls: 'icon-help fa fa-blue',
                cls: 'highlightIcon',
                handler: function() {
                    op = 'getData';
                    if (!me.checkValidity(op, ''))
                        return false;
                    var cbindings = me.getComponentBindings();
                    var title = 'Suggested Data for ' + me.template_id;
                    me.getForm().submit({
                        url: me.plan_url + "/" + op,
                        params: {
                            __template_id: me.template_id,
                            __cbindings: cbindings,
                            __paramdtypes: Ext.encode(dtypes)
                            },
                        submitEmptyText: false,
                        waitMsg: 'Getting Suggested Data...',
                        timeout: Ext.Ajax.timeout,
                        success: function(form, action) {
                            if (action.result && action.result.data)
                                showWingsBindings(action.result.data, title, me.flatItems, "data");
                            else
                                showWingsError("Wings couldn't find Data for the template: " 
                                		+ getLocalName(me.template_id), title, {});
                        },
                        failure: function(form, action) {
                            // console.log(action);
                            showWingsError("Wings couldn't find Data for the template " 
                            		+ getLocalName(me.template_id), title, action.result);
                        }
                    });
                }
            }, '-', {
                text: 'Suggest Parameters',
                iconCls: 'icon-help fa fa-blue',
                cls: 'highlightIcon',
                handler: function() {
                    op = 'getParameters';
                    if (!me.checkValidity(op, 'Please fill in all Data fields before querying for Parameters'))
                        return false;
                    var cbindings = me.getComponentBindings();
                    var title = 'Suggested Parameters for ' + me.template_id;
                    me.getForm().submit({
                        url: me.plan_url + "/" + op,
                        params: {
                            __template_id: me.template_id,
                            __cbindings: cbindings,
                            __paramdtypes: Ext.encode(dtypes)
                            },
                        submitEmptyText: false,
                        waitMsg: 'Getting Suggested Parameters...',
                        timeout: Ext.Ajax.timeout,
                        success: function(form, action) {
                            showWingsBindings(action.result.data, title, me.flatItems, "param");
                        },
                        failure: function(form, action) {
                            showWingsError("No parameter values are compatible with your datasets for the template " 
                            		+ getLocalName(me.template_id) + " !", title, action.result);
                        }
                    });
                }
            }, '-' ];
        	/*{
			text: 'Suggest Components',
			iconCls: 'compIcon',
			handler: function() {
				op = 'getComponents';
				var cbindings = me.getComponentBindings();
				if(!me.checkValidity(op,'Please fill in all fields before querying for Components')) return false;
				var title = 'Suggested Components for '+me.template_id;
				me.getForm().submit(
						{
							submitEmptyText: false,
							url : me.plan_url + "/" + op,
							params : {
								__template_id : me.template_id,
								__cbindings : cbindings,
								__paramdtypes : Ext.encode(dtypes)
							},
							submitEmptyText: false,
							waitMsg:'Getting Suggested Components...',
							timeout:Ext.Ajax.timeout,
							success: function(form, action) { 
								showWingsBindings(action.result.data, title, me.flatItems, "param"); 
							}, 
							failure: function(form, action) { 
								showWingsError("No parameter values are compatible with your datasets for the template: "
									+ me.template_id + " !", title, action.result); 
							}
						});
					}
				} */
        }
        tbar.push ({
                text: 'Plan Workflow',
                iconCls: 'icon-run fa fa-brown',
                cls: 'highlightIcon',
                handler: function() {
                    op = 'getExpansions';
                    if (!me.checkValidity(op, 'Please fill in all fields before running the Workflow'))
                        return false;
                    var cbindings = me.getComponentBindings();
                    me.getForm().submit({
                        url: me.plan_url + "/" + op,
                        params: {
                            __template_id: me.template_id,
                            __cbindings: cbindings,
                            __paramdtypes: Ext.encode(dtypes)
                            },
                        submitEmptyText: false,
                        waitMsg: 'Preparing Workflow Execution...',
                        timeout: Ext.Ajax.timeout,
                        success: function(form, action) {
                            var data = action.result.data;
                            if (data.templates) {
                                showWingsAlternatives(me.template_id, data, me.run_url, me.results_url, me.browser);
                            } else {
                                showWingsError("Wings couldn't generate any executable workflows for the template " 
                                		+ getLocalName(me.template_id) + ' based on what was submitted', 
                                		'No workflows created', action.result);
                             }
                        },
                        failure: function(form, action) {
                            showWingsError("Wings couldn't plan any workflows for the template " 
                            		+ getLocalName(me.template_id) + ' based on what was submitted', 
                            		'No workflows created', action.result);
                        }
                    });
                }
            });
        tbar.push({ xtype: 'tbfill'});
        tbar.push({
                text: 'Clear',
                iconCls: 'icon-trash fa fa-grey',
                handler: function() {
                    for (var i = 0; i < me.flatItems.length; i++) {
                        var item = me.flatItems[i];
                        item.setValue('');
                        item.allowBlank = true;
                    }
                }
            });
        tbar.push({
                text: 'Reload',
                iconCls: 'icon-reload fa fa-green',
                handler: function() {
                    var fetchOp = 'getViewerJSON';
                    var url = me.op_url + '/' + fetchOp + '?template_id=' + me.template_id;
                    me.templatePanel.getLoader().load({
                        url: url
                    });
                }
            });
        Ext.apply(this, { tbar: tbar });
        Ext.ux.form.SeedForm.superclass.initComponent.apply(this, config);
    },

    getComponentBindings: function() {
        var compbindings = {};
        if (!this.graph)
            return compbindings;

        var template = this.graph.editor.template;
        template.saveToStore();
        for (var i = 0; i < template.store.Nodes.length; i++) {
            var n = template.store.Nodes[i];
            if (n.componentVariable && n.componentVariable.binding)
                compbindings[n.componentVariable.id] = n.componentVariable.binding.id;
        }
        return Ext.encode(compbindings);
    },

    checkValidity: function(op, errmsg) {
        for (var i = 0; i < this.flatItems.length; i++) {
            var item = this.flatItems[i];
            if (!op || op == "getExpansions")
                item.allowBlank = false;
            else if (op == "getData")
                item.allowBlank = true;
            else if (op == "getParameters") {
                if (item.wtype == "data")
                    item.allowBlank = false;
                if (item.wtype == "param")
                    item.allowBlank = true;
            }
        }
        if (!this.getForm().isValid()) {
            Ext.Msg.show({
                title: 'Error',
                msg: errmsg,
                animEl: this,
                icon: Ext.Msg.ERROR,
                buttons: Ext.Msg.OK,
                modal: false
            });
            return false;
        }
        return true;
    },

    afterRender: function() {
        this.getForm().waitMsgTarget = this.getEl();
        Ext.ux.form.SeedForm.superclass.afterRender.apply(this, arguments);
    }
});
