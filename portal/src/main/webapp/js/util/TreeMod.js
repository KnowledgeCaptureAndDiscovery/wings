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

Ext.tree.Column.prototype.cellTpl =  [
        '<tpl for="lines">',
            '<img src="{parent.blankUrl}" class="{parent.childCls} {parent.elbowCls}-img ',
            '{parent.elbowCls}-<tpl if=".">line<tpl else>empty</tpl>"/>',
        '</tpl>',
        '<img src="{blankUrl}" class="{childCls} {elbowCls}-img {elbowCls}',
            '<tpl if="isLast">-end</tpl><tpl if="expandable">-plus {expanderCls}</tpl>"/>',
        '<tpl if="checked !== null">',
            '<input type="button" role="checkbox" <tpl if="checked">aria-checked="true" </tpl>',
                'class="{childCls} {checkboxCls}<tpl if="checked"> {checkboxCls}-checked</tpl>"/>',
        '</tpl>',
        '<i class="{textCls} {childCls} ',
        '<tpl if="isExpanded && expIconCls">',
        	'{expIconCls}',
        '<tpl else>',
        	'{iconCls}',
        '</tpl>"></i>',
        '<tpl if="href">',
            '<a href="{href}" target="{hrefTarget}" class="{textCls} {childCls}">{value}</a>',
        '<tpl else>',
            '<span class="{textCls} {childCls}">{value}</span>',
        '</tpl>'
    ];

Ext.tree.Column.prototype.treeRenderer =
	function(value, metaData, record, rowIdx, colIdx, store, view){
		var me = this,
			cls = record.get('cls'),
			renderer = me.origRenderer,
			data = record.data,
			parent = record.parentNode,
			rootVisible = view.rootVisible,
			lines = [],
			parentData;
	
		if (cls) {
			metaData.tdCls += ' ' + cls;
		}
	
		while (parent && (rootVisible || parent.data.depth > 0)) {
			parentData = parent.data;
			lines[rootVisible ? parentData.depth : parentData.depth - 1] =
				parentData.isLast ? 0 : 1;
			parent = parent.parentNode;
		}
	
		return me.getTpl('cellTpl').apply({
			record: record,
			baseIconCls: me.iconCls,
			iconCls: data.iconCls,
			expIconCls: record.raw.expIconCls,
			icon: data.icon,
			checkboxCls: me.checkboxCls,
			checked: data.checked,
			elbowCls: me.elbowCls,
			expanderCls: me.expanderCls,
			textCls: me.textCls,
			leaf: data.leaf,
			expandable: record.isExpandable(),
			isExpanded: record.isExpanded(),
			isLast: data.isLast,
			blankUrl: Ext.BLANK_IMAGE_URL,
			href: data.href,
			hrefTarget: data.hrefTarget,
			lines: lines,
			metaData: metaData,
			// subclasses or overrides can implement a getChildCls() method, which can
			// return an extra class to add to all of the cell's child elements (icon,
			// expander, elbow, checkbox).  This is used by the rtl override to add the
			// "x-rtl" class to these elements.
			childCls: me.getChildCls ? me.getChildCls() + ' ' : '',
					value: renderer ? renderer.apply(me.origScope, arguments) : value
		});
};
