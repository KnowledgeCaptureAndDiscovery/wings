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

function ProvenanceViewer(guid, op_url) {
	this.guid = guid;
	this.op_url = op_url;
}

ProvenanceViewer.prototype.createItemProvenanceGrid = function(itemid) {
	var fields = ['id', 'objectId', 'userId', 'time', 'log', 'type'];

	var provStore = new Ext.data.Store({
		fields : fields,
		proxy : {
			type : 'ajax',
			simpleSortMode : true,
			batchActions : false,
			api : {
				read : this.op_url + "/getItemProvenance?id=" + escape(itemid),
			},
			reader : {
				type : 'json',
				idProperty : 'id'
			}
		},
		sorters : [{ property : 'time', direction : 'DESC' }],
		autoSync : true
	});
	provStore.load();

    return new Ext.grid.Panel({
        store: provStore,
    	title: 'Provenance',
    	iconCls: 'icon-list-alt fa-title fa-blue',
        bodyCls:'multi-line-grid',
        columns: [{
        	dataIndex: 'userId',
        	header: 'User',
        	renderer: function(val) {
        		return getLocalName(val);
        	}
        }, {
        	dataIndex: 'type',
        	header: 'Type',
        }, {
        	dataIndex: 'time',
        	header: 'Timestamp',
        	flex: 1,
        	renderer: function(val) {
        		return Ext.Date.format(new Date(val), 'F j Y, g:ia');
        	}
        }, {
        	dataIndex: 'log',
        	header: 'Comments',
        	flex: 1
        }]
    });
}

ProvenanceViewer.prototype.createUserActivityGrid = function(userid) {
	var fields = ['id', 'objectId', 'userId', 'time', 'log', 'type'];

	var provStore = new Ext.data.Store({
		fields : fields,
		proxy : {
			type : 'ajax',
			simpleSortMode : true,
			batchActions : false,
			api : {
				read : this.op_url + "/getUserActivity?userid=" + escape(userid),
			},
			reader : {
				type : 'json',
				idProperty : 'id'
			}
		},
		sorters : [{ property : 'time', direction : 'DESC' }],
		autoSync : true
	});
	provStore.load();

    return new Ext.grid.Panel({
        store: provStore,
    	title: 'Provenance',
    	iconCls: 'icon-list-alt fa-title fa-blue',
        bodyCls:'multi-line-grid',
        columns: [{
        	dataIndex: 'objectId',
        	header: 'Item',
        	flex: 1,
        	renderer: function(val) {
        		return getLocalName(val);
        	}
        }, {
        	dataIndex: 'type',
        	header: 'Type',
        }, {
        	dataIndex: 'time',
        	header: 'Timestamp',
        	flex: 1,
        	renderer: function(val) {
        		return Ext.Date.format(new Date(val), 'F j Y, g:ia');
        	}
        }, {
        	dataIndex: 'log',
        	header: 'Comments',
        	flex: 1
        }]
    });
};
