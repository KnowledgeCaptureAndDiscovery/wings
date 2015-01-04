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

function getWingsDebugOutput(data) {
    if (data.output) {
        return "<pre><b>Debug Output:</b> <br/>\n" + data.output + "</pre>";
    }
    return "";
}

function getWingsExplanation(data) {
	data.explanations = Ext.Array.unique(data.explanations);
    for (var i = 0; i < data.explanations.length; i++) {
        // Replace urls with local names
        data.explanations[i] = data.explanations[i].replace(/http.*?#/g, '');
        data.explanations[i] = data.explanations[i].replace(/\^\^\w+/g, '');
    }

    var exps = Ext.Array.map(data.explanations, function(a) {
        return {
            text: a
        };
    });
    var exp = new Ext.grid.Panel({
        columns: [{
            dataIndex: 'text',
            menuDisabled: true
        }],
        hideHeaders: true,
        region: 'south',
        title: 'Explanation',
        collapsible: false,
        animCollapse: false,
        preventHeader: true,
        hideCollapseTool: true,
        hidden: true,
        forceFit: true,
        viewConfig: {
            getRowClass: function(record, rowIndex, rp, ds) {
                if (record.data.text.match(/ERROR/i)) {
                    return "errorCls";
                }
                if (record.data.text.match(/INFO/i)) {
                    return "infoCls";
                }
                if (record.data.text.match(/Suggest/i)) {
                    return "suggestCls";
                }
            }
        },
        bodyCls: 'multi-line-grid',
        height: 200,
        // border: false,
        split: true,
        autoScroll: true,
        store: new Ext.data.Store({
            fields: ['text'],
            data: exps
        }),
        });
    return exp;
}

function showWingsError(msg, title, result) {
    _console(result);
    var data = {
        error: true
    };
    if (result && result.data) {
        data = result.data;
    } else {
        msg = "Server error. Please contact the server administrator to look in server logs to see what went wrong !";
    }
    var win = new Ext.Window({
        layout: 'border',
        title: title ? title: "Error in Wings..",
        autoScroll: true,
        constrain: true,
        maximizable: true,
        width: 600,
        height: 450,
        items: {
            region: 'north',
            autoHeight: true,
            border: false,
            bodyStyle: 'padding:5px;color:white;background-color:' + (data.error ? 'red': 'darkorange'),
            html: msg
        }
    });

    if (data.explanations && data.explanations.length) {
        var explanationGrid = getWingsExplanation(data);
        Ext.apply(explanationGrid, {
            region: 'center',
            collapsed: false,
            hidden: false
        });
        win.add(explanationGrid);
    } else {
        win.add({
            region: 'center',
            xtype: 'textarea',
            border: false,
            readOnly: true,
            value: data.output
        });
    }

    win.show();
}

function formatDataBindings(value) {
	var s = "";
    if (value.type == "uri") {
    	s = "<div>" + getLocalName(value.id) + "</div>";
    }
    else if(value.length) {
        for (var i = 0; i < value.length; i++)
            s += formatDataBindings(value[i]);
    }
    return s;
}

function formatParameterBindings(value) {
    var s = "";
    if(value.type == "literal") {
    	// Replace the ending timezone "Z" for dates that is used in XSDDateTime
    	if(value.datatype == "http://www.w3.org/2001/XMLSchema#date")
    		value.value = value.value.replace(/Z$/, '');
    	s = "<div>" + value.value + "</div>";
    }
    else if (value.length) {
        for (var i = 0; i < value.length; i++)
            s += formatParameterBindings(value[i]);
    }
    return s;
}

function formatSeconds(value) {
	if (value == null)
		return "No estimate";
	var mins = Math.floor(value / 60);
	var seconds = value - mins * 60;
	return mins + "min, " + seconds.toFixed() + "s";
}

function formatTemplateSummary(value, meta, record, rowind, colind, store) {
    var tpl = record.data.field1.template;
    var time = record.data.field1.time;
    var numJobs = {};
    for (var i = 0; i < tpl.Nodes.length; i++) {
    	if(tpl.Nodes[i].inactive)
    		continue;
        var jobname = getLocalName(tpl.Nodes[i].componentVariable.binding.id);
        if (numJobs[jobname])
            numJobs[jobname]++;
        else
            numJobs[jobname] = 1;
    }

    var s = getLocalName(tpl.id);
    if(time)
    	s += "<br/><br/><div><b>Estimated Time: "+formatSeconds(time)+"</b></div>";
    
    s += "<ul>";
    for (var job in numJobs) {
        var num = numJobs[job];
        s += "<li>" + job + " : " + num + " job" + (num > 1 ? "s": "") + "</li>";
    }
    s += "</ul>";
    //s += "<li>" + tpl.Nodes.length + " jobs</li>";
    //s += "<li>" + tpl.Variables.length + " files</li>";
    //s += "<li>" + tpl.Links.length + " links</li>";
    return s;
}

function showWingsBindings(data, title, formItems, type) {
    // if(window.console) window.console.log(data);
    var bindings = data.bindings;
    if (!bindings || bindings.length == 0) {
        return showWingsError("Wings couldn't find Data for the template", title, data);
    }

    // Get Binding Data
    var cols = [{
        xtype: 'rownumberer',
        width: 30
    }];
    var fields = [];
    for (var i = 0; i < formItems.length; i++) {
        var item = formItems[i];
        // if(item.wtype == type && !item.getValue())
        if (item.wtype == type) {
            var name = item.name;
            //var id = item.uri;
            cols.push({
                dataIndex: name,
                header: name,
                renderer: type == "data" ? formatDataBindings: formatParameterBindings,
                resizable: true,
                sortable: true,
                flex: 1,
                menuDisabled: true
            });
            fields.push({
            	name: name,
            	sortType: function(a) {
            		a = a[0] ? a[0] : a;
            		return type == 'param' ? a.value : a.id;
            	}
            });
        }
    }
    
    // Remove duplicate entries
    var bstrs = {};
    var nbindings = [];
    for(var i=0; i<bindings.length; i++) {
       var nbinding = {};
       var bstr = "";
       for(var j=0; j<fields.length; j++) {
          var field = fields[j];
          var val = bindings[i][field.name];
          nbinding[field.name] = val;
          bstr += (type == 'data' ? formatDataBindings(val) : formatParameterBindings(val))+"|";
       }
       if(!bstrs[bstr]) {
          nbindings.push(nbinding);
          bstrs[bstr] = true;
       }
    }
    
    var myStore = new Ext.data.Store({
    	proxy: {
    		type: 'memory',
            reader: {
                type: 'json',
                useSimpleAccessors: true
            }
    	},
        fields: fields,
        sorters: fields.length > 0 ? [fields[0].name] : [],
        autoLoad: true,
        data: nbindings
    });

    var win = new Ext.Window({
        layout: 'border',
        title: title,
        constrain: true,
        maximizable: true,
        autoScroll: true,
        // autoWidth:true,
        width: 600,
        height: 450
    });

    // Binding Grid Panel
    var bindingsGrid = new Ext.grid.GridPanel({
        columns: cols,
        region: 'center',
        //forceFit : true,
        border: false,
        columnLines: true,
        autoScroll: true,
        bodyCls: 'multi-line-grid',
        store: myStore,
        sm: new Ext.selection.RowModel({
            singleSelect: true
        }),
        tbar: [{
            text: 'Use Selected ' + (type == 'param' ? 'Parameters' : 'Data'),
            iconCls: 'icon-select-alt fa fa-green',
            handler: function() {
                var recs = bindingsGrid.getSelectionModel().getSelection();
                if (!recs || !recs.length) {
                    Ext.Msg.show({
                        msg: 'Select some ' + type + ' combination from below and then press Select',
                        modal: false,
                        buttons: Ext.Msg.OK
                    });
                    return;
                }
                var myRec = recs[0];
                for (var i = 0; i < formItems.length; i++) {
                    var item = formItems[i];
                    var selection = myRec.get(item.name);
                    if (selection == undefined)
                        continue;
                    
                    var val = "";
                    if(selection.type == "uri")
                    	val = selection.id;
                    else if(selection.type == "literal")
                    	val = selection.value;
                    else if(selection.length) {
                    	val = [];
                    	for(var j=0; j<selection.length; j++) {
                    		val[j] = (selection[j].type == "uri") ? selection[j].id : selection[j].value; 
                    	}
                    }
                    item.setValue(val);
                }
                win.close();
            }
        }]
        });

    var explanationsGrid = getWingsExplanation(data);
    explanationsGrid.on("expand", function() {
        explanationsGrid.determineScrollbars();
    });

    win.add({
        region: 'center',
        layout: 'border',
        items: [bindingsGrid, {
            xtype: 'button',
            region: 'south',
            text: 'Show/Hide Explanations',
            handler: function() {
                if (explanationsGrid.isHidden()) {
                    explanationsGrid.expand();
                    explanationsGrid.show();
                } else {
                    explanationsGrid.collapse();
                    explanationsGrid.hide();
                }
            }
        }]
        });
    win.add(explanationsGrid);
    win.show();
}

function showWingsRanMessage(tid, runid, results_url) {
    var msg = "Workflow has been submitted for execution !<br/><br/>" + "You can monitor Workflow Execution from the 'Access Results' page in the Analysis Menu";

    var win = new Ext.Window({
        layout: 'border',
        constrain: true,
        maximizable: true,
        title: "Workflow Submitted",
        autoScroll: true,
        width: 400,
        height: 150,
        items: [{
            region: 'center',
            bodyStyle: 'padding:5px',
            border: false,
            html: msg
        }, {
            xtype: 'button',
            region: 'south',
            text: 'Or CLICK HERE to Monitor Execution',
            iconCls: 'icon-select-alt fa-big fa-up fa-green',
            handler: function() {
                var w = window.open(results_url + '?run_id=' + escape(runid), '_accessResults');
                win.close();
            }
        }]
        });
    win.show();
}

function createEmptyTemplatePanel(tid, browser) {
	   var templatePanel = new Ext.Panel({
	      region: 'center',
	      layout: 'border'
	  });
	  var emptystore = {
	      template: {
	          Variables: [],
	          Nodes: [],
	          Links: []
	          },
	      constraints: []
	      };
	
	  var guid = getLocalName(tid) + '_result';
	  var gridPanel = browser.getConstraintsTableViewer(tid, emptystore);
	  Ext.apply(gridPanel, {
	      region: 'north',
	      itemId: 'grid',
	      split: true
	  });
	  var graph = browser.getTemplateGraph(tid, emptystore, false, templatePanel, gridPanel, guid);
	  Ext.apply(graph, {
		   itemId: 'graph',
	      region: 'center'
	  });
	  Ext.apply(graph.infoPanel, {
		  	itemId: 'info',
	      region: 'south'
	  });
	  templatePanel.add(gridPanel);
	  templatePanel.add(graph);
	  templatePanel.add(graph.infoPanel);
	  return templatePanel;
}

function showWingsAlternatives(tid, data, run_url, results_url, browser) {
    var MAX_LINKS = 500;

    var alternatives = data.templates;
    // if(window.console) window.console.log(alternatives);
    var title = "Select an Executable Workflow to Run..";
    if (!alternatives || alternatives.length == 0) {
        return showWingsError("Wings couldn't find any Executable Templates", title);
    }

    var win = new Ext.Window({
        layout: 'border',
        title: title,
        constrain: true,
        maximizable: true,
        autoScroll: true,
        // autoWidth:true,
        width: '90%',
        height: '90%'
    });

    var templatePanel = createEmptyTemplatePanel(tid, browser);
    
    // Template List Grid
    var alternativesGrid = new Ext.grid.GridPanel({
        columns: [{
            header: 'Template',
            renderer: formatTemplateSummary,
            menuDisabled: true,
            sortable: true,
            doSort: function(state) {
                var ds = this.up('tablepanel').store;
                var field = this.getSortParam();
                ds.sort({
                    property: field,
                    direction: state,
                    sorterFn: function(v1, v2) {
                        v1 = v1.get(field);
                        v2 = v2.get(field);
                        // transform v1 and v2 here
                        return v1 > v2 ? 1: (v1 < v2 ? -1: 0);
                    }
                });
            }
        }],
        region: 'center',
        //width : '20%',
        forceFit: true,
        border: false,
        columnLines: true,
        autoScroll: true,
        bodyCls: 'multi-line-grid',
        store: alternatives,
        sm: new Ext.selection.RowModel({
            singleSelect: true
        }),
        tbar: [{
            text: 'Run Selected Workflow',
            iconCls: 'icon-run fa fa-brown',
            handler: function() {
                var recs = alternativesGrid.getSelectionModel().getSelection();
                if (!recs || !recs.length) {
                    Ext.Msg.show({
                        msg: 'Select a Workflow from below and then press Run',
                        modal: false,
                        buttons: Ext.Msg.OK
                    });
                    return;
                }
                var myRec = recs[0];
                var tstore = myRec.raw[0];
                var url = run_url + '/runWorkflow';
                var msgTarget = Ext.get(win.getId());
                
                msgTarget.mask('Running...', 'x-mask-loading');
                Ext.Ajax.request({
                    url: url,
                    params: {
                    		json: Ext.encode(tstore.template),
                    		constraints_json: Ext.encode(tstore.constraints),
                    		seed_json: Ext.encode(data.seed.template),
                    		seed_constraints_json: Ext.encode(data.seed.constraints),
                    		template_id: tid
                    },
                    success: function(response) {
                        msgTarget.unmask();
                        var runid = response.responseText;
                        if (runid) {
                            //win.close();
                            showWingsRanMessage(tid, runid, results_url);
                        } else 
                        	_console(response.responseText);
                    },
                    failure: function(response) {
                        _console(response.responseText);
                    	msgTarget.unmask();
                    }
                });
            }
        }]
    });

    alternativesGrid.on("select", function(item, rec, index, opts) {
        // Show the template on selection in grid
        var tstore = rec.raw[0];
        var varMaps = [];
        for (var i = 0; i < tstore.template.Variables.length; i++)
            varMaps[tstore.template.Variables[i].id] = 1;
        
        var storetid = tstore.template.id;
        var gridPanel = templatePanel.down('#grid');
        gridPanel.tns = getNamespace(storetid);
        var constraints = browser.replaceConstraintObjects(storetid, tstore.constraints, varMaps, true);
        gridPanel.getStore().loadData(constraints);

        var graph = templatePanel.down('#graph');

        if (tstore.template.Links.length > MAX_LINKS) {
            graph.editor.clearCanvas();
            alert("This graph is too big to display");
            return;
        }

        if (!tstore.tpl)
            tstore.tpl = new Template(storetid, tstore.template, ed);

        var ed = graph.editor;
        ed.template = tstore.tpl;
        ed.initLayerItems();
        ed.deselectAllItems();
        ed.refreshConstraints();
        ed.template.markErrors();
        ed.redrawCanvas(ed.panelWidth, ed.panelHeight);
        if (!tstore.tpl.layouted) {
            ed.clearCanvas();
            ed.layout();
            tstore.tpl.layouted = true;
        }
    });

    var explanationsGrid = getWingsExplanation(data);
    Ext.apply(explanationsGrid, {
        maxHeight: '100%'
    });
    win.add({
        region: 'west',
        width: '25%',
        layout: 'border',
        split: true,
        items: [alternativesGrid, 
        // alternatives on left
        {
            // Explanations button at bottom
            xtype: 'button',
            region: 'south',
            text: 'Show/Hide Explanations',
            handler: function() {
                if (explanationsGrid.isHidden()) {
                    explanationsGrid.expand();
                    explanationsGrid.show();
                } else {
                    explanationsGrid.collapse();
                    explanationsGrid.hide();
                }
            }
        }, explanationsGrid]
        });
    win.add(templatePanel);
    win.show();
}

function showWingsMessage(msg, title, data, w, h) {
    var win = new Ext.Window({
        layout: 'border',
        constrain: true,
        maximizable: true,
        title: title,
        frame: false,
        border: false,
        autoScroll: true,
        width: w ? w : 400,
        height: h ? h : 400
    });

    if (data) {
        var explanationsGrid = getWingsExplanation(data);
        Ext.apply(explanationsGrid, {
            region: 'south',
            hidden: false
        });
        win.add({
            region: 'center',
            layout: 'border',
            items: [{
                region: 'center',
                border: false,
                bodyPadding: 5,
                items: [{
                    autoHeight: true,
                    html: msg,
                    border: false
                }, {
                    margin: 5,
                    xtype: 'button',
                    text: 'OK',
                    align: 'center',
                    handler: function() {
                        win.close();
                    }
                }]
                }, {
                xtype: 'button',
                region: 'south',
                text: 'Show/Hide Explanations',
                handler: function() {
                    if (explanationsGrid.isHidden()) {
                        explanationsGrid.expand();
                        explanationsGrid.show();
                    } else {
                        explanationsGrid.collapse();
                        explanationsGrid.hide();
                    }
                }
            }]
            });
        win.add(explanationsGrid);
    } else {
        win.add({
            region: 'center',
            autoHeight: true,
            bodyStyle: 'padding:5px',
            border: false,
            html: msg
        });
    }
    win.show();
}